// 英字幕リーダー — アプリロジック
// ハンドオフ sr-app.jsx を移植し、モックOCRを実OCR（端末内処理）に差し替え

import { html, render, useState, useRef, useEffect, useMemo } from "../vendor/preact/standalone.module.js";
import {
  Fragment, SrIcon, Dropzone, FrameCard, JoinConnector,
  SentenceCard, HistoryPanel, ResultsEmpty, SettingsPanel,
} from "./components.js";
import { recognizeQueued, preloadOcr } from "./ocr.js";
import { loadSettings, saveSettings, applySettings } from "./settings.js";

const DEMO_SOURCES = [1, 2, 3, 4, 5].map((n) => `./assets/demo-${n}.png`);

let _uid = 0;
const uid = () => "f" + (++_uid) + "_" + Date.now().toString(36);

const HISTORY_KEY = "engsub-reader-history-v1";
function loadHistory() {
  try { return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []; } catch (e) { return []; }
}
function saveHistory(items) {
  try { localStorage.setItem(HISTORY_KEY, JSON.stringify(items.slice(0, 30))); } catch (e) {}
}

function App() {
  const [settings, setSettings] = useState(loadSettings);
  const [frames, setFrames] = useState([]);
  const [joinOverrides, setJoinOverrides] = useState({});
  const [history, setHistory] = useState(loadHistory);
  const [showHistory, setShowHistory] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [toast, setToast] = useState(null);
  const startedRef = useRef(new Set());
  const dragIdRef = useRef(null);
  const [dragId, setDragId] = useState(null);
  const toastTimerRef = useRef(null);

  /* settings → CSSカスタムプロパティ / localStorage */
  useEffect(() => {
    applySettings(settings);
    saveSettings(settings);
  }, [settings]);

  /* OCRワーカーを温めておく（初回読み取りの待ち時間短縮） */
  useEffect(() => {
    const t = setTimeout(preloadOcr, 800);
    return () => clearTimeout(t);
  }, []);

  const showToast = (msg) => {
    setToast(msg);
    clearTimeout(toastTimerRef.current);
    toastTimerRef.current = setTimeout(() => setToast(null), 1800);
  };

  /* ---- add files ---- */
  const addFiles = (fileList) => {
    const imgs = Array.from(fileList).filter((f) => f.type.startsWith("image/"));
    if (!imgs.length) return;
    const added = imgs.map((f) => ({
      id: uid(), src: URL.createObjectURL(f), name: f.name,
      status: "pending", lines: [], endsComplete: true, hasChrome: false,
    }));
    setFrames((prev) => [...prev, ...added]);
  };

  const loadDemo = () => {
    startedRef.current = new Set();
    setJoinOverrides({});
    setFrames(DEMO_SOURCES.map((src) => ({
      id: uid(), src, name: src.split("/").pop(),
      status: "pending", lines: [], endsComplete: true, hasChrome: false,
    })));
  };

  /* ---- paste support (ハードウェアキーボードの ⌘V など) ---- */
  useEffect(() => {
    const onPaste = (e) => {
      if (e.clipboardData && e.clipboardData.files && e.clipboardData.files.length) {
        addFiles(e.clipboardData.files);
      }
    };
    window.addEventListener("paste", onPaste);
    return () => window.removeEventListener("paste", onPaste);
  }, []);

  /* ドロップゾーン外へのドロップでページ遷移しないように */
  useEffect(() => {
    const prevent = (e) => e.preventDefault();
    window.addEventListener("dragover", prevent);
    window.addEventListener("drop", prevent);
    return () => {
      window.removeEventListener("dragover", prevent);
      window.removeEventListener("drop", prevent);
    };
  }, []);

  /* ---- OCR pipeline（端末内・直列キュー） ---- */
  useEffect(() => {
    frames.forEach((f) => {
      if (f.status !== "pending" || startedRef.current.has(f.id)) return;
      startedRef.current.add(f.id);
      recognizeQueued(f.src, {
        onStart: () => setFrames((prev) => prev.map((p) => p.id === f.id ? { ...p, status: "reading" } : p)),
      }).then((res) => {
        setFrames((prev) => prev.map((p) => p.id === f.id
          ? { ...p, status: "done", lines: res.lines, endsComplete: res.endsComplete, hasChrome: res.hasChrome }
          : p));
      }).catch((err) => {
        console.error("OCR failed:", err);
        setFrames((prev) => prev.map((p) => p.id === f.id ? { ...p, status: "error" } : p));
      });
    });
  }, [frames]);

  const retryFrame = (id) => {
    startedRef.current.delete(id);
    setFrames((prev) => prev.map((p) => p.id === id ? { ...p, status: "pending" } : p));
  };

  /* ---- delete / reorder ---- */
  const revokeSrc = (src) => { if (src && src.startsWith("blob:")) URL.revokeObjectURL(src); };
  const deleteFrame = (id) => {
    setFrames((prev) => {
      const target = prev.find((f) => f.id === id);
      if (target) revokeSrc(target.src);
      return prev.filter((f) => f.id !== id);
    });
  };
  const clearAll = () => {
    setFrames((prev) => { prev.forEach((f) => revokeSrc(f.src)); return []; });
    setJoinOverrides({});
    startedRef.current = new Set();
  };

  const dragHandlersFor = (id) => ({
    start: (e) => {
      dragIdRef.current = id;
      setDragId(id);
      e.dataTransfer.effectAllowed = "move";
      try { e.dataTransfer.setData("text/plain", id); } catch (err) {}
    },
    over: (e) => {
      e.preventDefault();
      const from = dragIdRef.current;
      if (!from || from === id) return;
      setFrames((prev) => {
        const a = prev.findIndex((f) => f.id === from);
        const b = prev.findIndex((f) => f.id === id);
        if (a < 0 || b < 0) return prev;
        const next = prev.slice();
        const [moved] = next.splice(a, 1);
        next.splice(b, 0, moved);
        return next;
      });
    },
    end: () => { dragIdRef.current = null; setDragId(null); },
  });

  /* ---- join logic ---- */
  const pairKey = (a, b) => a.id + "|" + b.id;
  const isJoined = (a, b) => {
    const k = pairKey(a, b);
    if (k in joinOverrides) return joinOverrides[k];
    return a.status === "done" ? !a.endsComplete : false;
  };
  const toggleJoin = (a, b) => {
    const k = pairKey(a, b);
    setJoinOverrides((prev) => ({ ...prev, [k]: !isJoined(a, b) }));
  };

  /* ---- merged sentence groups（表示順から毎回導出） ---- */
  const groups = useMemo(() => {
    const done = frames.map((f, i) => ({ f, i }))
      .filter((x) => x.f.status === "done" && x.f.lines.length > 0);
    const out = [];
    let cur = null;
    for (let n = 0; n < done.length; n++) {
      const { f, i } = done[n];
      const text = f.lines.join(" ");
      if (cur) {
        cur.text += " " + text;
        cur.frameIndexes.push(i);
      } else {
        cur = { text, frameIndexes: [i] };
      }
      const nxt = done[n + 1];
      const joined = nxt && frames[i + 1] && frames[i + 1].id === nxt.f.id && isJoined(f, nxt.f);
      if (!joined) { out.push(cur); cur = null; }
    }
    if (cur) out.push(cur);
    return out;
  }, [frames, joinOverrides]);

  const allText = groups.map((g) => g.text).join("\n");
  const allDone = frames.length > 0 && frames.every((f) => f.status === "done");

  /* ---- clipboard ---- */
  const copyText = (text) => {
    const fallback = () => {
      const ta = document.createElement("textarea");
      ta.value = text; document.body.appendChild(ta); ta.select();
      try { document.execCommand("copy"); } catch (e) {}
      document.body.removeChild(ta);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).catch(fallback);
    } else fallback();
    showToast("コピーしました");
  };

  /* ---- history ---- */
  const saveToHistory = () => {
    if (!allText) return;
    const now = new Date();
    const pad = (n) => String(n).padStart(2, "0");
    const item = {
      id: uid(), text: allText, count: frames.length,
      time: (now.getMonth() + 1) + "/" + now.getDate() + " " + pad(now.getHours()) + ":" + pad(now.getMinutes()),
    };
    const next = [item, ...history].slice(0, 30);
    setHistory(next); saveHistory(next);
    showToast("履歴に保存しました");
  };
  const deleteHistoryItem = (id) => {
    const next = history.filter((h) => h.id !== id);
    setHistory(next); saveHistory(next);
  };

  const openHistory = () => { setShowSettings(false); setShowHistory(!showHistory); };
  const openSettings = () => { setShowHistory(false); setShowSettings(!showSettings); };

  /* ====== render ====== */
  return html`<div class="sr-app">
    <header class="sr-header">
      <div class="sr-wordmark">
        <span class="sr-wordmark-jp">英字幕リーダー</span>
        <span class="sr-wordmark-en">SUBTITLE READER</span>
      </div>
      <div class="sr-header-right">
        <span class="sr-offline" title="読み取りはすべて端末内で完結します（オフライン対応）">
          <${SrIcon} name="shield" size=${13} /> オフライン処理
        </span>
        <button class=${"sr-btn sr-btn-ghost" + (showSettings ? " is-active" : "")} onClick=${openSettings}>
          <${SrIcon} name="gear" size=${14} /> 設定
        </button>
        <button class=${"sr-btn sr-btn-ghost" + (showHistory ? " is-active" : "")} onClick=${openHistory}>
          <${SrIcon} name="clock" size=${14} /> 履歴
        </button>
      </div>
    </header>

    <main class="sr-main">
      <!-- ---- left: input ---- -->
      <section class="sr-col sr-col-input">
        <div class="sr-col-head">
          <h2>画像</h2>
          <div class="sr-col-head-actions">
            ${frames.length > 0 && html`<button class="sr-btn sr-btn-ghost sr-btn-sm" onClick=${clearAll}>
              <${SrIcon} name="trash" size=${13} /> すべて削除
            </button>`}
          </div>
        </div>

        ${frames.length === 0
          ? html`<${Fragment}>
              <${Dropzone} onFiles=${addFiles} />
              <button class="sr-demo-link" onClick=${loadDemo}>
                <${SrIcon} name="sparkle" size=${14} /> サンプル画像5枚で試す
              </button>
            <//>`
          : html`<${Fragment}>
              <div class="sr-frames">
                ${frames.map((f, i) => html`<${Fragment} key=${f.id}>
                  ${i > 0 && html`<${JoinConnector}
                    joined=${isJoined(frames[i - 1], f)}
                    auto=${!(pairKey(frames[i - 1], f) in joinOverrides)}
                    disabled=${frames[i - 1].status !== "done" || f.status !== "done"}
                    onToggle=${() => toggleJoin(frames[i - 1], f)}
                  />`}
                  <${FrameCard}
                    frame=${f} index=${i}
                    onDelete=${deleteFrame}
                    onRetry=${retryFrame}
                    dragHandlers=${dragHandlersFor(f.id)}
                    dragging=${dragId === f.id}
                  />
                <//>`)}
              </div>
              <${Dropzone} onFiles=${addFiles} compact=${true} />
              <p class="sr-hint">ドラッグで並べ替え ・ 「区切り / 連結」をクリックすると文のつながりを手動で調整できます</p>
            <//>`}
      </section>

      <!-- ---- right: results ---- -->
      <section class="sr-col sr-col-output">
        <div class="sr-col-head">
          <h2>読み取り結果</h2>
          <div class="sr-col-head-actions">
            ${groups.length > 0 && html`<${Fragment}>
              <button class="sr-btn sr-btn-ghost sr-btn-sm" onClick=${saveToHistory} disabled=${!allDone}>
                <${SrIcon} name="clock" size=${13} /> 履歴に保存
              </button>
              <button class="sr-btn sr-btn-primary sr-btn-sm" onClick=${() => copyText(allText)}>
                <${SrIcon} name="copy" size=${13} /> 全文コピー
              </button>
            <//>`}
          </div>
        </div>

        ${groups.length === 0
          ? html`<${ResultsEmpty} />`
          : html`<div class=${"sr-sentences" + (settings.centerText ? " is-centered" : "")}>
              ${groups.map((g, i) => html`<${SentenceCard} key=${i} group=${g} onCopy=${copyText} fontSize=${settings.subtitleSize} />`)}
            </div>`}

        ${groups.length > 0 && html`<div class="sr-fulltext">
          <div class="sr-fulltext-label">全文（コピー用）</div>
          <pre class="sr-fulltext-body">${allText}</pre>
        </div>`}
      </section>
    </main>

    ${showHistory && html`<${HistoryPanel} items=${history} onCopy=${copyText} onDelete=${deleteHistoryItem} onClose=${() => setShowHistory(false)} />`}
    ${showSettings && html`<${SettingsPanel} settings=${settings} onChange=${setSettings} onClose=${() => setShowSettings(false)} />`}

    <footer class="sr-footnote">
      読み取りはすべてこの端末の中（オフライン）で行われ、画像や結果が外部に送信されることはありません。
      動画プレイヤーのタイトル・シークバーなどのUI要素は自動で除外され、画面下部の字幕だけを抽出します。
    </footer>

    ${toast && html`<div class="sr-toast">${toast}</div>`}
  </div>`;
}

render(html`<${App} />`, document.getElementById("root"));
