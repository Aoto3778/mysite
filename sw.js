// 英字幕リーダー — Service Worker
// 全アセット（OCRコア・言語データ含む）をプリキャッシュし、
// 初回読み込み後は完全オフラインで動作させる。
// アプリを更新したら CACHE_VERSION を上げること。

const CACHE_VERSION = "v1";
const CACHE_NAME = "engsub-reader-" + CACHE_VERSION;

const PRECACHE_ASSETS = [
  "./",
  "./index.html",
  "./style.css",
  "./manifest.webmanifest",
  "./js/app.js",
  "./js/components.js",
  "./js/ocr.js",
  "./js/ocr-core.js",
  "./js/settings.js",
  "./vendor/preact/standalone.module.js",
  "./vendor/tesseract/tesseract.esm.min.js",
  "./vendor/tesseract/worker.min.js",
  "./vendor/core/tesseract-core-lstm.wasm.js",
  "./vendor/core/tesseract-core-simd-lstm.wasm.js",
  "./vendor/lang/eng.traineddata.gz",
  "./assets/demo-1.png",
  "./assets/demo-2.png",
  "./assets/demo-3.png",
  "./assets/demo-4.png",
  "./assets/demo-5.png",
  "./icons/icon-192.png",
  "./icons/icon-512.png",
  "./icons/apple-touch-icon.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => cache.addAll(PRECACHE_ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(
        keys.filter((k) => k.startsWith("engsub-reader-") && k !== CACHE_NAME)
          .map((k) => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;

  event.respondWith((async () => {
    const cache = await caches.open(CACHE_NAME);
    const cached = await cache.match(req, { ignoreSearch: true });
    if (cached) return cached;
    try {
      const res = await fetch(req);
      if (res && res.ok) cache.put(req, res.clone());
      return res;
    } catch (err) {
      // オフラインで未キャッシュのナビゲーション → アプリシェルへ
      if (req.mode === "navigate") {
        const shell = await cache.match("./index.html");
        if (shell) return shell;
      }
      throw err;
    }
  })());
});
