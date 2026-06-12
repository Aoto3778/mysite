import { createWorker } from "tesseract.js";
import { PNG } from "pngjs";
import fs from "node:fs";
import path from "node:path";
import { preprocessForOcr, OCR_PSM } from "../js/ocr-core.js";

const file = process.argv[2] || "demo-1.png";
const png = PNG.sync.read(fs.readFileSync("../assets/" + file));
const raw = { data: new Uint8ClampedArray(png.data.buffer), width: png.width, height: png.height };
const pre = preprocessForOcr(raw);
const out = new PNG({ width: pre.width, height: pre.height });
Buffer.from(pre.data.buffer).copy(out.data);
const worker = await createWorker("eng", 1, { langPath: path.resolve("../vendor/lang"), gzip: true, cacheMethod: "none", logger: () => {} });
await worker.setParameters({ tessedit_pageseg_mode: OCR_PSM });
const { data } = await worker.recognize(PNG.sync.write(out), {}, { blocks: true, text: true });
for (const b of data.blocks || [])
  for (const p of b.paragraphs || [])
    for (const l of p.lines || [])
      console.log(JSON.stringify(l.text), "conf=" + Math.round(l.confidence), JSON.stringify(l.bbox));
await worker.terminate();
