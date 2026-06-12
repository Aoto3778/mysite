// PWAアイコン生成（依存パッケージなし・node:zlib のみ）
// モチーフ: ダーク地に字幕バー2本（結果カラムの空状態と同じゴーストライン）
// 実行: node dev/make-icons.mjs

import { deflateSync } from "node:zlib";
import { writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");

const BG = [0x0d, 0x0d, 0x10];
const ACCENT = [0xff, 0x8a, 0x3d];
const WHITE = [0xf2, 0xf2, 0xef];

// 角丸横バーの被覆率 (0..1)。中心(cx,cy)・全幅w・全高h、端は半円
function barCoverage(px, py, cx, cy, w, h) {
  const r = h / 2;
  const halfInner = w / 2 - r;
  const dx = Math.max(0, Math.abs(px - cx) - halfInner);
  const dy = py - cy;
  const d = Math.hypot(dx, dy);
  return Math.max(0, Math.min(1, r - d + 0.5));
}

function drawIcon(size) {
  const bars = [
    { color: ACCENT, cx: 0.5, cy: 0.595, w: 0.52, h: 0.085 },
    { color: WHITE, cx: 0.5, cy: 0.745, w: 0.78, h: 0.085 },
  ];
  const px = new Uint8Array(size * size * 3);
  const SS = 3; // supersampling
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      let r = BG[0], g = BG[1], b = BG[2];
      for (const bar of bars) {
        let cov = 0;
        for (let sy = 0; sy < SS; sy++) {
          for (let sx = 0; sx < SS; sx++) {
            cov += barCoverage(
              x + (sx + 0.5) / SS, y + (sy + 0.5) / SS,
              bar.cx * size, bar.cy * size, bar.w * size, bar.h * size
            );
          }
        }
        cov /= SS * SS;
        if (cov > 0) {
          r = r + (bar.color[0] - r) * cov;
          g = g + (bar.color[1] - g) * cov;
          b = b + (bar.color[2] - b) * cov;
        }
      }
      const i = (y * size + x) * 3;
      px[i] = r; px[i + 1] = g; px[i + 2] = b;
    }
  }
  return encodePng(px, size, size);
}

// ---- 最小限のPNGエンコーダ (truecolor, filter 0) ----
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c >>> 0;
  }
  return t;
})();
function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const out = Buffer.alloc(8 + data.length + 4);
  out.writeUInt32BE(data.length, 0);
  out.write(type, 4, "ascii");
  data.copy(out, 8);
  out.writeUInt32BE(crc32(out.subarray(4, 8 + data.length)), 8 + data.length);
  return out;
}
function encodePng(rgb, w, h) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0);
  ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8;  // bit depth
  ihdr[9] = 2;  // color type: truecolor
  const raw = Buffer.alloc(h * (1 + w * 3));
  for (let y = 0; y < h; y++) {
    raw[y * (1 + w * 3)] = 0; // filter: none
    Buffer.from(rgb.buffer, y * w * 3, w * 3).copy(raw, y * (1 + w * 3) + 1);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", ihdr),
    chunk("IDAT", deflateSync(raw, { level: 9 })),
    chunk("IEND", Buffer.alloc(0)),
  ]);
}

mkdirSync(join(ROOT, "icons"), { recursive: true });
for (const [file, size] of [["icon-512.png", 512], ["icon-192.png", 192], ["apple-touch-icon.png", 180]]) {
  writeFileSync(join(ROOT, "icons", file), drawIcon(size));
  console.log("wrote icons/" + file);
}
