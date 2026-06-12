// DOMスモークテスト: jsdom上でアプリ全体を描画し、
// 主要なUI要素がエラーなくマウントされることを確認する。
// 実行: cd dev && node smoke-dom.mjs

import { JSDOM } from "jsdom";

const dom = new JSDOM('<!DOCTYPE html><html><body><div id="root"></div></body></html>', {
  url: "https://example.com/mysite/",
  pretendToBeVisual: true,
});

for (const key of ["window", "document", "navigator", "localStorage", "HTMLElement", "SVGElement", "Node", "CustomEvent", "requestAnimationFrame", "cancelAnimationFrame"]) {
  Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true, writable: true });
}

await import("../js/app.js");
// preactの描画はマイクロタスク/rAF後に完了する
await new Promise((r) => setTimeout(r, 50));

const root = dom.window.document.getElementById("root");
const htmlOut = root.innerHTML;

const checks = [
  ["ワードマーク", htmlOut.includes("英字幕リーダー") && htmlOut.includes("SUBTITLE READER")],
  ["オフラインバッジ", htmlOut.includes("オフライン処理")],
  ["設定ボタン", htmlOut.includes("設定")],
  ["履歴ボタン", htmlOut.includes("履歴")],
  ["ドロップゾーン", htmlOut.includes("字幕のスクリーンショットをドロップ")],
  ["サンプルリンク", htmlOut.includes("サンプル画像5枚で試す")],
  ["結果の空状態", htmlOut.includes("画像を追加すると、ここに読み取った字幕が表示されます。")],
  ["フッター注記", htmlOut.includes("端末の中")],
];

let failed = 0;
for (const [name, ok] of checks) {
  console.log((ok ? "✓" : "✗") + " " + name);
  if (!ok) failed++;
}

// 設定パネルを開いてみる
const buttons = [...root.querySelectorAll("button")];
const settingsBtn = buttons.find((b) => b.textContent.includes("設定"));
settingsBtn.dispatchEvent(new dom.window.MouseEvent("click", { bubbles: true }));
await new Promise((r) => setTimeout(r, 50));
const settingsOk = root.innerHTML.includes("表示設定") && root.innerHTML.includes("中央揃え");
console.log((settingsOk ? "✓" : "✗") + " 設定パネル表示");
if (!settingsOk) failed++;

if (failed) {
  console.error(`\n${failed} 件失敗`);
  process.exit(1);
}
console.log("\nDOMスモークテスト PASS");
