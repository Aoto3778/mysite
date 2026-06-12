# 英字幕リーダー (English Subtitle Reader)

アニメなどのスクリーンショットから、**英語字幕のセリフ部分だけ**を読み取るiPad向けPWA（ホーム画面に追加して使うWebアプリ）。

- 1枚で完結する文はそのまま、**複数枚にまたがる文は自動で1文に連結**
- 動画プレイヤーのUI（タイトル・シークバー・時刻表示など）は**自動で除外**
- 読み取りは**すべて端末内で完結（完全オフライン）**。画像が外部に送信されることはなく、API課金など追加費用は一切かからない

OCRエンジンは [Tesseract.js](https://github.com/naptha/tesseract.js)（WASM）で、英語の学習データ含め**全アセットをこのリポジトリに同梱**している。CDNにもネットワークにも依存しない。

---

## 公開手順（最初に1回だけ・すべて無料）

GitHub Pages で公開する。リポジトリ管理者（自分）の操作が3ステップ必要:

1. **このブランチを main にマージ**
   （Pull Request を作るか、`git merge` で main へ取り込む）
2. **リポジトリを Public に変更**
   GitHub → リポジトリの **Settings → General** → 一番下 **Danger Zone** → *Change repository visibility* → **Public**
   ※ Private のまま Pages を使う場合は GitHub Pro が必要（学生は [GitHub Education](https://education.github.com/) で無料化できる）
3. **GitHub Pages を有効化**
   **Settings → Pages** → *Build and deployment* → Source: **Deploy from a branch** → Branch: **main** / **(root)** → Save

数分後に次のURLで公開される:

```
https://aoto3778.github.io/mysite/
```

## iPadへのインストール

1. iPadの **Safari** で上記URLを開く（**初回はオンライン必須**。約12MBをキャッシュする）
2. 共有ボタン（□↑）→ **「ホーム画面に追加」**
3. ホーム画面のアイコンから起動し、一度表示されればOK

以後は**機内モードでも全機能が動作**する。ネットワークを使うのは初回読み込みとアプリ更新時のみ。

> Safariの仕様上、ホーム画面に追加せずブラウザで放置すると約7日でキャッシュが消えることがある。ホーム画面への追加を推奨。

## 使い方

1. 字幕が写ったスクリーンショットを追加（タップして写真から選択 / ドラッグ&ドロップ / ⌘V ペースト）
2. 自動でOCRが走り、左カラムに読み取り結果・「文末 / 次へ続く」バッジが付く
3. 文が複数枚にまたがる場合は自動で連結される。判定が違うときはフレーム間の「区切り / 連結」チップをタップして手動調整
4. 右カラムで文ごとにコピー、または「全文コピー」。「履歴に保存」で端末内に最大30件保存（`localStorage`）

「サンプル画像5枚で試す」リンクで動作確認できる（demo-3/4が連結ケース、demo-5がUI除外ケース）。

---

## 開発

ビルド工程はない。静的ファイルをそのまま配信する構成。

```bash
# ローカル起動（Service Workerはlocalhostで動作する）
python3 -m http.server 8000
# → http://localhost:8000

# テスト（要 Node.js）
cd dev
npm install
npm test               # OCR受け入れテスト: demo 5枚を実OCRに通して期待値と比較
node smoke-dom.mjs     # DOMスモークテスト: jsdom上でUI全体を描画
node debug-raw.mjs demo-5.png   # Tesseractの生出力を確認（調整用）

# アイコン再生成 / 同梱ライブラリ更新
node make-icons.mjs
bash vendor.sh
```

**アプリのファイルを変更したら `sw.js` の `CACHE_VERSION` を上げること。** これを忘れるとインストール済み端末に更新が届かない。

### ファイル構成

```
index.html            アプリシェル（PWAメタ・SW登録）
style.css             デザイントークン + 全スタイル（ハンドオフ仕様を移植）
manifest.webmanifest  PWAマニフェスト
sw.js                 Service Worker（全アセットをプリキャッシュ → オフライン動作）
js/app.js             状態管理・連結ロジック・履歴・クリップボード
js/components.js      UIコンポーネント（Preact + HTM）
js/ocr.js             Tesseractワーカー管理・直列キュー（ブラウザ側）
js/ocr-core.js        前処理・字幕フィルタ・文末判定（純粋ロジック、テストと共用）
js/settings.js        表示設定の保存と適用
vendor/               同梱ライブラリ（dev/vendor.sh で再生成）
assets/demo-*.png     動作確認用サンプル
dev/                  テスト・生成スクリプト（配信物ではない）
```

### OCRパイプライン（js/ocr-core.js）

1. 画像の**下部42%をクロップ**（字幕は画面下部に出る前提知識）し、幅1300pxへ正規化
2. **Otsu法による適応二値化** + **アウトラインゲート**（明るい画素のうち、近傍に暗い縁取りを持つものだけを文字とみなす）→ プレイヤーUIで減光された灰色字幕にも、明るい背景にも対応
3. Tesseract（PSM 3・LSTM）で行ごとの テキスト / bbox / 信頼度 を取得
4. フィルタリング: シーク時刻（`11:45` 等）・シークバー状の線・画面最下端・中央から外れた行・極端に小さい文字を除外。除外が発生したフレームに「UI要素を除外」バッジ
5. 末尾の文字（`. ! ? … " 」` など）で**文末 / 次へ続く**を判定 → 「次へ続く」は次のフレームと自動連結

しきい値などの調整定数はすべて `js/ocr-core.js` 冒頭にまとまっており、`dev/` で `npm test` を回しながら調整できる。

## 既知の制限

- OCR精度はTesseract（オープンソース）の範囲。かすれ・極端な装飾フォントでは誤読があり得る。その場合は撮り直すか、結果をコピー後に手で修正する
- 字幕が画面下部にある前提（上部字幕・縦書きは対象外）
- 認識対象は英語字幕のみ

## ライセンス / クレジット

同梱ライブラリ: [tesseract.js](https://github.com/naptha/tesseract.js) (Apache-2.0)・tesseract.js-core (Apache-2.0)・@tesseract.js-data/eng (Apache-2.0)・[Preact](https://preactjs.com/) (MIT)・[HTM](https://github.com/developit/htm) (Apache-2.0)。各ライセンス文は `vendor/` 内に同梱。
