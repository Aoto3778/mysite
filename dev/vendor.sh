#!/usr/bin/env bash
# vendor/ ディレクトリの再生成スクリプト
# アプリはCDNに依存せず、以下のライブラリをリポジトリに同梱して
# 完全オフラインで動作する。バージョンを上げる時はここを書き換えて実行する。
#
#   bash dev/vendor.sh
#
# 同梱物とライセンス:
#   - htm 3.1.1 (preact/standalone: Preact 10 + HTM バンドル) … Apache-2.0 / Preact: MIT
#   - tesseract.js 6.0.1 (本体 + Web Worker)                  … Apache-2.0
#   - tesseract.js-core 6.1.2 (WASMコア, LSTM / SIMD両対応)    … Apache-2.0
#   - @tesseract.js-data/eng 1.0.0 (4.0.0_best_int)           … Apache-2.0

set -euo pipefail
cd "$(dirname "$0")/.."

HTM_VERSION=3.1.1
TESSERACT_VERSION=6.0.1
CORE_VERSION=6.1.2
ENG_VERSION=1.0.0

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

(cd "$TMP" && npm pack --silent \
  "htm@${HTM_VERSION}" \
  "tesseract.js@${TESSERACT_VERSION}" \
  "tesseract.js-core@${CORE_VERSION}" \
  "@tesseract.js-data/eng@${ENG_VERSION}")

mkdir -p vendor/preact vendor/tesseract vendor/core vendor/lang

tar -xzf "$TMP/htm-${HTM_VERSION}.tgz" -C "$TMP" package/preact/standalone.module.js package/LICENSE
cp "$TMP/package/preact/standalone.module.js" vendor/preact/standalone.module.js
cp "$TMP/package/LICENSE" vendor/preact/LICENSE-htm.txt
rm -rf "$TMP/package"

tar -xzf "$TMP/tesseract.js-${TESSERACT_VERSION}.tgz" -C "$TMP" \
  package/dist/tesseract.esm.min.js package/dist/worker.min.js \
  package/dist/tesseract.min.js.LICENSE.txt package/dist/worker.min.js.LICENSE.txt
cp "$TMP/package/dist/tesseract.esm.min.js" vendor/tesseract/
cp "$TMP/package/dist/worker.min.js" vendor/tesseract/
cp "$TMP/package/dist/tesseract.min.js.LICENSE.txt" vendor/tesseract/
cp "$TMP/package/dist/worker.min.js.LICENSE.txt" vendor/tesseract/
rm -rf "$TMP/package"

tar -xzf "$TMP/tesseract.js-core-${CORE_VERSION}.tgz" -C "$TMP" \
  package/tesseract-core-lstm.wasm.js package/tesseract-core-simd-lstm.wasm.js package/LICENSE
cp "$TMP/package/tesseract-core-lstm.wasm.js" vendor/core/
cp "$TMP/package/tesseract-core-simd-lstm.wasm.js" vendor/core/
cp "$TMP/package/LICENSE" vendor/core/LICENSE.txt
rm -rf "$TMP/package"

tar -xzf "$TMP/tesseract.js-data-eng-${ENG_VERSION}.tgz" -C "$TMP" package/4.0.0_best_int/eng.traineddata.gz
cp "$TMP/package/4.0.0_best_int/eng.traineddata.gz" vendor/lang/

echo "vendor/ を更新しました。アプリを更新したら sw.js の CACHE_VERSION も上げてください。"
