# 家計簿（iD / Vpass 自動記録）

Android 個人用の家計簿アプリ。**Vpass（三井住友カード）のご利用通知を読み取り、自動で家計簿に記録する**。
iD（おサイフケータイ）の支払いも、後から Vpass のご利用通知として届くため記録される。
データは端末内のみ・通信なし・無料。

> このブランチ `kakeibo` は、字幕アプリ（`main` ブランチ／GitHub Pages）とは別物の Android アプリです。`main` には一切影響しません。

## これは何
- **通知連動**: **Vpass のご利用通知**を自動で記帳（支出）。iD の支払いも後から Vpass 通知として届くので、これ1本でまとめて記録する（iD 通知側は記録せず**二重計上を防ぐ**）
- iD 決済（`iD/利用先/iD` 形式）は利用先名を取り出して通常の支出として分類（例: 業務スーパー→食費）
- **分類ルール**: 利用先に含まれるワードを好きなカテゴリに割り当て可能（例: 「Salon」→ 衣服・美容）。以後その語を含む決済は自動分類され、未分類の既存データにも反映される
- **収入は手動入力**。支出と収入から **収支・貯蓄率・カテゴリ別内訳（ドーナツ）・月次推移グラフ** を表示
- 手動入力・カテゴリ分け・月ごとの集計・CSV 書き出し
- **完全オフライン**（INTERNET 権限なし）。マネーフォワードのような銀行 API 連携（有料）は使わず、端末に届く通知から記録する無料方式

## インストール（スマホだけで完結・PC不要）
ビルドは GitHub Actions で自動実行され、最新 APK は常に Releases に公開される。

1. スマホのブラウザで次を開く（公開リポジトリなのでログイン不要）:
   **https://github.com/Aoto3778/mysite/releases/download/latest/kakeibo.apk**
   （または Releases ページ → tag `latest` → `kakeibo.apk`）
2. ダウンロードした `kakeibo.apk` をインストール（初回は「提供元不明のアプリ」を許可）
3. アプリを開き、案内に従って **「通知へのアクセス」** を許可

> APK を作り直したいときは Actions タブ → Build APK → Run workflow（または `kakeibo` ブランチへ push）。
> 万一 Release が更新されない場合のみ、Settings → Actions → General → Workflow permissions を「Read and write」にする。

## 取りこぼさないための設定
- **Vpass**: アプリで **「ご利用通知サービス」を ON**（決済ごとに通知が届くようになる）。**iD の支払いもこの通知で記録される**ので必ず ON にする
- **iD（おサイフケータイ）アプリ自体の通知**は、ON でも OFF でもよい（二重計上を避けるため**記録には使わない**）
- **バッテリー最適化**から **本アプリ・Vpass** を除外（設定画面にボタンあり）

## 使い方
- **一覧**: 月ごとの支出。自動記録された決済が並ぶ。行をタップで編集、右下の ＋ で手動追加
- **集計**: 当月のカテゴリ別合計
- **設定**: 通知アクセス、バッテリー最適化、CSV 書き出し、**分類ルール（ワード→カテゴリ）**、診断（未対応通知の文面確認）

## Vpass の文面調整について
Vpass の通知文面は環境により異なることがある。うまく記録されないときは、
**設定 →「すべての通知をキャプチャ」** を一時的に ON にして決済 → 届いた文面を確認し、
`app/src/main/java/com/aoto/kakeibo/parse/Rules.kt` の判定・正規表現を調整する（ユニットテストを回しながら）。

## 開発
- ビルドは GitHub Actions（`.github/workflows/build.yml`）。ローカルでやる場合は Android Studio で開いて Run。
- ユニットテスト: `gradle test`（金額パース・解析ルール・カテゴリ推定。Android 非依存）。
- 技術: Kotlin / Jetpack Compose / Room。minSdk 26 / targetSdk 35。署名は同梱の固定鍵 `signing.jks`。

## 既知の制限
- **インストール後の新規決済のみ**記録（過去分は入らない）
- 通知 ON・バッテリー最適化除外が前提（OS がサービスを落とすと取りこぼす）
- 利用先が「Visa 加盟店利用」等の固定名称になる場合がある
- 個人 sideload（Google Play 非経由・無保証）。署名鍵は使い捨ての公開鍵で、個人利用では実害なし

## ファイル構成
```
app/src/main/java/com/aoto/kakeibo/
  MainActivity.kt / MainViewModel.kt / KakeiboApp.kt
  data/        Room（TxnEntity・RawCapture・RuleEntity・Dao・AppDatabase・Repository）
  parse/       AmountParser（金額抽出）・Rules（Vpass ご利用通知の判定・利用先抽出）
  category/    AutoCategory（利用先→カテゴリ推定。ユーザー定義ルール優先）
  noti/        NotiListenerService（通知の読み取り本体）
  ui/          Compose 画面（一覧・集計・設定・編集・分類ルール）
  util/        Prefs（設定保存）・NotiAccess（権限・設定画面）
app/src/test/  ユニットテスト
.github/workflows/build.yml   CI（テスト→APK ビルド→Release 公開）
signing.jks                    固定署名鍵（個人 sideload 用）
```
