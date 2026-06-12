// ブラウザ側OCR: Tesseract.js ワーカー管理と直列キュー
//
// すべて同梱アセット（vendor/）から読み込むため、初回キャッシュ後は
// 完全オフラインで動作する。ワーカーは Blob URL から起動されるため、
// corePath / langPath / workerPath は絶対URLで渡す必要がある。

import { preprocessForOcr, analyzeOcrResult, OCR_PSM } from "./ocr-core.js";

const BASE = new URL(".", document.baseURI);
// 読み込み時の安全上限（これ以上大きい画像は縮小してから処理）
const MAX_DECODE_WIDTH = 4000;

let workerPromise = null;
let queue = Promise.resolve();

function getWorker() {
  if (!workerPromise) {
    workerPromise = createTessWorker().catch((err) => {
      workerPromise = null;
      throw err;
    });
  }
  return workerPromise;
}

async function createTessWorker() {
  // 必要になるまで本体(~64KB)を読み込まない（初回描画を速くする）
  const { default: Tesseract } = await import("../vendor/tesseract/tesseract.esm.min.js");
  const worker = await Tesseract.createWorker("eng", 1 /* LSTM_ONLY */, {
    workerPath: new URL("vendor/tesseract/worker.min.js", BASE).href,
    corePath: new URL("vendor/core", BASE).href,
    langPath: new URL("vendor/lang", BASE).href,
    gzip: true,
    logger: () => {},
    errorHandler: (err) => console.warn("[ocr worker]", err),
  });
  await worker.setParameters({ tessedit_pageseg_mode: OCR_PSM });
  return worker;
}

/** アプリ起動後にワーカーを温めておく（初回読み取りを速くする） */
export function preloadOcr() {
  getWorker().catch(() => {});
}

/**
 * 画像URL（blob: / 相対パス）をOCRし、字幕抽出結果を返す。
 * 呼び出しは内部キューで直列化される。
 * @returns {Promise<{lines: string[], endsComplete: boolean, hasChrome: boolean}>}
 */
export function recognizeQueued(src, { onStart } = {}) {
  const task = async () => {
    if (onStart) onStart();
    const raw = await loadImagePixels(src);
    const pre = preprocessForOcr(raw);
    const canvas = imageToCanvas(pre);
    const worker = await getWorker();
    const { data } = await worker.recognize(canvas, {}, { blocks: true, text: true });
    return analyzeOcrResult(data.blocks || [], pre.geometry);
  };
  const result = queue.then(task, task);
  queue = result.then(() => {}, () => {});
  return result;
}

async function loadImagePixels(src) {
  const img = new Image();
  img.decoding = "async";
  img.src = src;
  await img.decode();
  let w = img.naturalWidth;
  let h = img.naturalHeight;
  if (!w || !h) throw new Error("画像を読み込めませんでした");
  if (w > MAX_DECODE_WIDTH) {
    h = Math.round(h * (MAX_DECODE_WIDTH / w));
    w = MAX_DECODE_WIDTH;
  }
  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  ctx.drawImage(img, 0, 0, w, h);
  return ctx.getImageData(0, 0, w, h);
}

function imageToCanvas(pre) {
  const canvas = document.createElement("canvas");
  canvas.width = pre.width;
  canvas.height = pre.height;
  const ctx = canvas.getContext("2d");
  ctx.putImageData(new ImageData(pre.data, pre.width, pre.height), 0, 0);
  return canvas;
}
