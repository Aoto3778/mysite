// OCR受け入れテスト
// assets/demo-1..5.png をブラウザ実装と同一のパイプライン
// (js/ocr-core.js: 前処理 → Tesseract → 字幕フィルタ) に通し、
// ハンドオフREADME記載の期待値と比較する。
//
// 実行: cd dev && npm install && npm test

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { PNG } from "pngjs";
import { createWorker } from "tesseract.js";
import { preprocessForOcr, analyzeOcrResult, OCR_PSM } from "../js/ocr-core.js";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const OUT_DIR = join(ROOT, "dev", "out");

const EXPECTED = [
  {
    file: "demo-1.png",
    lines: ["I can't tell her", "not to push herself too hard."],
    endsComplete: true,
    hasChrome: false,
  },
  {
    file: "demo-2.png",
    lines: ["That sounds", "like the safer bet to me."],
    endsComplete: true,
    hasChrome: false,
  },
  {
    file: "demo-3.png",
    lines: ["I'd never in my life seen one", "with so much malice exuding"],
    endsComplete: false,
    hasChrome: false,
  },
  {
    file: "demo-4.png",
    lines: ["from every page."],
    endsComplete: true,
    hasChrome: false,
  },
  {
    file: "demo-5.png",
    lines: ["there was lots", "of back and forth beforehand,"],
    endsComplete: false,
    hasChrome: true,
  },
];

const norm = (s) => s.replace(/\s+/g, " ").trim();

function encodeProcessedPng(pre) {
  const png = new PNG({ width: pre.width, height: pre.height });
  Buffer.from(pre.data.buffer, pre.data.byteOffset, pre.data.byteLength).copy(png.data);
  return PNG.sync.write(png);
}

mkdirSync(OUT_DIR, { recursive: true });

const worker = await createWorker("eng", 1, {
  langPath: join(ROOT, "vendor", "lang"),
  gzip: true,
  cacheMethod: "none",
  logger: () => {},
});
await worker.setParameters({ tessedit_pageseg_mode: OCR_PSM });

let failures = 0;

for (const exp of EXPECTED) {
  const png = PNG.sync.read(readFileSync(join(ROOT, "assets", exp.file)));
  const raw = { data: new Uint8ClampedArray(png.data.buffer, png.data.byteOffset, png.data.byteLength), width: png.width, height: png.height };

  const t0 = Date.now();
  const pre = preprocessForOcr(raw);
  const processedPng = encodeProcessedPng(pre);
  writeFileSync(join(OUT_DIR, "pre-" + exp.file), processedPng);

  const { data } = await worker.recognize(processedPng, {}, { blocks: true, text: true });
  const res = analyzeOcrResult(data.blocks || [], pre.geometry);
  const ms = Date.now() - t0;

  const problems = [];
  const gotLines = res.lines.map(norm);
  const expLines = exp.lines.map(norm);
  if (JSON.stringify(gotLines) !== JSON.stringify(expLines)) {
    problems.push(`lines:\n    expected: ${JSON.stringify(expLines)}\n    got:      ${JSON.stringify(gotLines)}`);
  }
  if (res.endsComplete !== exp.endsComplete) {
    problems.push(`endsComplete: expected ${exp.endsComplete}, got ${res.endsComplete}`);
  }
  if (res.hasChrome !== exp.hasChrome) {
    problems.push(`hasChrome: expected ${exp.hasChrome}, got ${res.hasChrome}`);
  }

  if (problems.length) {
    failures++;
    console.log(`✗ ${exp.file} (${ms}ms)`);
    for (const p of problems) console.log(`  - ${p}`);
  } else {
    console.log(`✓ ${exp.file} (${ms}ms)  lines=${JSON.stringify(res.lines)} endsComplete=${res.endsComplete} hasChrome=${res.hasChrome}`);
  }
}

await worker.terminate();

if (failures) {
  console.log(`\n${failures} 件失敗（前処理後の画像は dev/out/ に保存済み）`);
  process.exit(1);
}
console.log("\n全5件 PASS");
