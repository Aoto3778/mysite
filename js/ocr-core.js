// OCRパイプラインの純粋ロジック（ブラウザ / Node 共用）
//
// - preprocessForOcr: 画像下部の字幕領域をクロップ → 正規化 → 二値化
//   字幕の事前知識「画面下部・中央寄せ・明るい文字＋暗い縁取り」を利用する。
// - analyzeOcrResult: Tesseract の行情報から字幕行のみを残し、
//   プレイヤーUI（シークバー時刻・小さな文字・端寄りのテキスト）を除外する。
//
// DOM / Tesseract には依存しない。受け入れテスト (dev/test-ocr.mjs) から
// そのまま import して、ブラウザ実装と同一の処理を検証できる。

// ---- チューニング定数 ----------------------------------------------------

// 読み取り対象: 画像の下からこの割合（字幕は画面下部に出る）
export const CROP_RATIO = 0.42;
// 前処理後の画像幅（Tesseractが読みやすいサイズに正規化）
export const TARGET_WIDTH = 1300;
// ページセグメンテーションモード（3 = 自動。字幕+UI混在クロップに最も安定）
export const OCR_PSM = "3";

// 二値化: クロップ内のOtsu（判別分析）閾値。プレイヤーUIで減光された
// 灰色字幕（輝度~105）と通常の白字幕の両方に追従する
const THRESHOLD_MIN = 60;
const THRESHOLD_MAX = 200;
// アウトラインゲート: 明るい画素でも、近傍にこの輝度未満の「縁取り/影」が
// 無ければ背景（空など）とみなして捨てる
const OUTLINE_DARK = 90;

// 行フィルタ
const MIN_LINE_CONFIDENCE = 35; // これ未満はノイズとして無視（hasChromeに数えない）
const BOTTOM_EDGE_RATIO = 0.94; // 画像最下端ゾーン（シークバー域）
const CENTER_DEV_RATIO = 0.25;  // 中央からの水平ずれ許容（字幕は中央寄せ）
const SMALL_HEIGHT_RATIO = 0.5; // 主要行より大幅に小さい文字はUI要素

// 文末判定: この記号で終わっていれば「文末」、それ以外は「次へ続く」
const SENTENCE_END_RE = /[.!?…"”'’」』)\]]\s*$/;

// ---- 前処理 ---------------------------------------------------------------

/**
 * RGBA画像から字幕領域を切り出し、OCR向けの二値画像（黒文字/白背景）を作る。
 * @param {{data: Uint8ClampedArray|Uint8Array, width: number, height: number}} raw
 * @returns {{data: Uint8ClampedArray, width: number, height: number,
 *            geometry: {cropY0: number, scale: number, srcWidth: number, srcHeight: number}}}
 */
export function preprocessForOcr(raw) {
  const { data, width, height } = raw;
  const cropY0 = Math.max(0, Math.floor(height * (1 - CROP_RATIO)));
  const cropH = height - cropY0;

  // グレースケール化（クロップ領域のみ）
  const gray = new Uint8ClampedArray(width * cropH);
  for (let y = 0; y < cropH; y++) {
    let si = ((cropY0 + y) * width) * 4;
    let di = y * width;
    for (let x = 0; x < width; x++, si += 4, di++) {
      gray[di] = (data[si] * 299 + data[si + 1] * 587 + data[si + 2] * 114) / 1000;
    }
  }

  // 正規化リサイズ（拡大は2倍まで）
  const scale = Math.min(2, TARGET_WIDTH / width);
  const dw = Math.max(1, Math.round(width * scale));
  const dh = Math.max(1, Math.round(cropH * scale));
  const resized = resizeGrayBilinear(gray, width, cropH, dw, dh);

  // 適応閾値（Otsu）: 暗い背景と明るい文字を統計的に分離
  const thr = Math.min(THRESHOLD_MAX, Math.max(THRESHOLD_MIN, otsuThreshold(resized)));

  // 二値化 + アウトラインゲート → RGBA（黒文字 / 白背景）
  const out = new Uint8ClampedArray(dw * dh * 4);
  const r1 = Math.max(3, Math.round(dh * 0.012));
  const r2 = r1 * 2;
  for (let y = 0; y < dh; y++) {
    for (let x = 0; x < dw; x++) {
      const i = y * dw + x;
      let isText = false;
      if (resized[i] >= thr) {
        isText = hasDarkNeighbor(resized, dw, dh, x, y, r1) ||
                 hasDarkNeighbor(resized, dw, dh, x, y, r2);
      }
      const v = isText ? 0 : 255;
      const o = i * 4;
      out[o] = out[o + 1] = out[o + 2] = v;
      out[o + 3] = 255;
    }
  }

  return {
    data: out,
    width: dw,
    height: dh,
    geometry: { cropY0, scale, srcWidth: width, srcHeight: height },
  };
}

function resizeGrayBilinear(src, sw, sh, dw, dh) {
  if (sw === dw && sh === dh) return src;
  const out = new Uint8ClampedArray(dw * dh);
  const xr = sw / dw;
  const yr = sh / dh;
  for (let y = 0; y < dh; y++) {
    const sy = Math.min(sh - 1.001, y * yr);
    const y0 = sy | 0;
    const fy = sy - y0;
    const y1 = Math.min(sh - 1, y0 + 1);
    for (let x = 0; x < dw; x++) {
      const sx = Math.min(sw - 1.001, x * xr);
      const x0 = sx | 0;
      const fx = sx - x0;
      const x1 = Math.min(sw - 1, x0 + 1);
      const a = src[y0 * sw + x0];
      const b = src[y0 * sw + x1];
      const c = src[y1 * sw + x0];
      const d = src[y1 * sw + x1];
      out[y * dw + x] = a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
    }
  }
  return out;
}

function otsuThreshold(gray) {
  const hist = new Uint32Array(256);
  for (let i = 0; i < gray.length; i++) hist[gray[i]]++;
  const total = gray.length;
  let sum = 0;
  for (let v = 0; v < 256; v++) sum += v * hist[v];
  let sumB = 0;
  let wB = 0;
  let best = 127;
  let bestVar = -1;
  for (let v = 0; v < 256; v++) {
    wB += hist[v];
    if (wB === 0) continue;
    const wF = total - wB;
    if (wF === 0) break;
    sumB += v * hist[v];
    const mB = sumB / wB;
    const mF = (sum - sumB) / wF;
    const between = wB * wF * (mB - mF) * (mB - mF);
    if (between > bestVar) {
      bestVar = between;
      best = v;
    }
  }
  return best + 1;
}

function hasDarkNeighbor(gray, w, h, x, y, r) {
  for (let dy = -1; dy <= 1; dy++) {
    for (let dx = -1; dx <= 1; dx++) {
      if (!dx && !dy) continue;
      const nx = x + dx * r;
      const ny = y + dy * r;
      if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
      if (gray[ny * w + nx] < OUTLINE_DARK) return true;
    }
  }
  return false;
}

// ---- OCR結果のフィルタリング ----------------------------------------------

/**
 * Tesseract の blocks 出力から字幕行のみを抽出する。
 * @param {Array} blocks - recognize(..., {blocks: true}) の data.blocks
 * @param {{cropY0: number, scale: number, srcWidth: number, srcHeight: number}} geometry
 * @returns {{lines: string[], endsComplete: boolean, hasChrome: boolean}}
 */
export function analyzeOcrResult(blocks, geometry) {
  const { cropY0, scale, srcWidth, srcHeight } = geometry;
  const rawLines = collectLines(blocks);

  const candidates = [];
  let hasChrome = false;

  for (const line of rawLines) {
    const text = cleanLineText(line.text);
    if (!text) continue;

    // 元画像座標へ変換
    const x0 = line.bbox.x0 / scale;
    const x1 = line.bbox.x1 / scale;
    const y1 = cropY0 + line.bbox.y1 / scale;
    const heightOrig = (line.bbox.y1 - line.bbox.y0) / scale;
    const centerDev = Math.abs((x0 + x1) / 2 - srcWidth / 2);

    const letters = (text.match(/[A-Za-z]/g) || []).length;
    const dashes = (text.match(/[-–—_=~]/g) || []).length;
    const isTimeLike =
      /^[\s\d:.,/|\\\-–—]*\d{1,2}:\d{2}[\s\d:.,/|\\\-–—]*$/.test(text);
    // シークバー: 画面下部の幅広いダッシュ主体の「線」（誤認識文字でも形で分かる）
    const isSeekbarLike =
      y1 > srcHeight * 0.85 &&
      dashes / text.length >= 0.4 &&
      (x1 - x0) > srcWidth * 0.25;
    const isPunctOnly = letters === 0 && !/^[.…!?"']+$/.test(text);

    // UI要素の強いシグナルは信頼度に関係なく除外対象として数える
    if (isTimeLike || isSeekbarLike) {
      hasChrome = true;
      continue;
    }
    if (line.confidence < MIN_LINE_CONFIDENCE) continue; // ノイズ

    if (y1 > srcHeight * BOTTOM_EDGE_RATIO || // 画面最下端（シークバー域）
        centerDev > srcWidth * CENTER_DEV_RATIO) { // 端寄り = UI要素
      hasChrome = true;
      continue;
    }
    if (isPunctOnly) continue; // 記号のみはノイズ

    candidates.push({ text, top: cropY0 + line.bbox.y0 / scale, height: heightOrig });
  }

  // 主要（最大）行高より大幅に小さい行はプレイヤーUIの小さな文字とみなす
  let kept = candidates;
  if (candidates.length > 1) {
    const maxH = Math.max(...candidates.map((c) => c.height));
    kept = candidates.filter((c) => {
      const ok = c.height >= maxH * SMALL_HEIGHT_RATIO;
      if (!ok) hasChrome = true;
      return ok;
    });
  }

  kept.sort((a, b) => a.top - b.top);
  const lines = kept.map((c) => c.text);
  const joined = lines.join(" ").trim();

  return {
    lines,
    endsComplete: joined === "" ? true : SENTENCE_END_RE.test(joined),
    hasChrome,
  };
}

function collectLines(blocks) {
  const out = [];
  for (const block of blocks || []) {
    for (const para of block.paragraphs || []) {
      for (const line of para.lines || []) {
        const text = typeof line.text === "string"
          ? line.text
          : (line.words || []).map((w) => w.text).join(" ");
        out.push({ text, confidence: line.confidence ?? 0, bbox: line.bbox });
      }
    }
  }
  return out;
}

function cleanLineText(text) {
  return String(text || "")
    .replace(/[‘’]/g, "'")
    .replace(/[“”]/g, '"')
    .replace(/\s+/g, " ")
    // 単独トークンの縦棒は大文字 I の定番誤認識（例: "| can't tell her"）
    .replace(/(^|\s)\|(?=\s|$)/g, "$1I")
    .replace(/^[|/\\_~`^¬•·=<>«»\s]+/, "")
    .replace(/[|/\\_~`^¬•·=<>«»\s]+$/, "")
    .trim();
}
