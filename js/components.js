// 英字幕リーダー — プレゼンテーション層
// ハンドオフ sr-components.jsx を Preact + HTM へ移植

import { html, useState } from "../vendor/preact/standalone.module.js";
import { ACCENT_OPTIONS } from "./settings.js";

export const Fragment = (props) => props.children;

/* ---------- icons (simple stroke paths) ---------- */
const ICON_PATHS = {
  upload: () => html`<path d="M12 16V4" /><path d="M6 9l6-6 6 6" /><path d="M4 20h16" />`,
  copy: () => html`<rect x="9" y="9" width="11" height="11" rx="2" /><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />`,
  trash: () => html`<path d="M3 6h18" /><path d="M8 6V4h8v2" /><path d="M6 6l1 14h10l1-14" />`,
  check: () => html`<path d="M5 12l5 5L20 7" />`,
  clock: () => html`<circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" />`,
  image: () => html`<rect x="3" y="3" width="18" height="18" rx="2" /><circle cx="9" cy="9" r="2" /><path d="M21 15l-5-5L5 21" />`,
  x: () => html`<path d="M6 6l12 12" /><path d="M18 6L6 18" />`,
  shield: () => html`<path d="M12 3l7 3v5c0 5-3.5 8-7 9-3.5-1-7-4-7-9V6z" />`,
  link: () => html`<path d="M9 17H7A5 5 0 0 1 7 7h2" /><path d="M15 7h2a5 5 0 1 1 0 10h-2" /><path d="M8 12h8" />`,
  grip: () => html`<circle cx="9" cy="6" r="1" /><circle cx="9" cy="12" r="1" /><circle cx="9" cy="18" r="1" /><circle cx="15" cy="6" r="1" /><circle cx="15" cy="12" r="1" /><circle cx="15" cy="18" r="1" />`,
  download: () => html`<path d="M12 4v12" /><path d="M6 11l6 6 6-6" /><path d="M4 20h16" />`,
  sparkle: () => html`<path d="M12 3l2.2 6.8L21 12l-6.8 2.2L12 21l-2.2-6.8L3 12l6.8-2.2z" />`,
  gear: () => html`<circle cx="12" cy="12" r="3" /><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.38a2 2 0 0 0-.73-2.73l-.15-.09a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />`,
  retry: () => html`<path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8" /><path d="M21 3v5h-5" />`,
};

export function SrIcon({ name, size = 16, strokeWidth = 2, style }) {
  const path = ICON_PATHS[name];
  return html`<svg
    width=${size} height=${size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
    stroke-width=${strokeWidth} stroke-linecap="round" stroke-linejoin="round"
    style=${style} aria-hidden="true">${path ? path() : null}</svg>`;
}

/* ---------- drop zone ---------- */
export function Dropzone({ onFiles, compact }) {
  const [over, setOver] = useState(false);
  let inputEl = null;

  const handle = (e) => {
    e.preventDefault();
    setOver(false);
    const files = e.dataTransfer ? e.dataTransfer.files : e.target.files;
    if (files && files.length) onFiles(files);
    if (inputEl) inputEl.value = "";
  };
  const open = () => inputEl && inputEl.click();

  return html`<div
    class=${"sr-dropzone" + (over ? " is-over" : "") + (compact ? " is-compact" : "")}
    onClick=${open}
    onDragOver=${(e) => { e.preventDefault(); setOver(true); }}
    onDragLeave=${() => setOver(false)}
    onDrop=${handle}
    role="button"
    tabIndex="0"
    onKeyDown=${(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); open(); } }}
  >
    <input ref=${(el) => { inputEl = el; }} type="file" accept="image/*" multiple
      style=${{ display: "none" }} onChange=${handle} />
    <div class="sr-dropzone-inner">
      <div class="sr-dropzone-icon"><${SrIcon} name="upload" size=${compact ? 18 : 26} strokeWidth=${1.8} /></div>
      <div class="sr-dropzone-label">
        ${compact ? "画像を追加" : "字幕のスクリーンショットをドロップ"}
      </div>
      ${!compact && html`<div class="sr-dropzone-sub">クリックで選択 ・ ⌘V でペーストも可 ・ 複数枚OK</div>`}
    </div>
  </div>`;
}

/* ---------- frame card (uploaded image) ---------- */
export function FrameCard({ frame, index, onDelete, onRetry, dragHandlers, dragging }) {
  const done = frame.status === "done";
  const hasText = done && frame.lines.length > 0;
  return html`<div
    class=${"sr-frame" + (dragging ? " is-dragging" : "")}
    draggable=${true}
    onDragStart=${dragHandlers.start}
    onDragOver=${dragHandlers.over}
    onDragEnd=${dragHandlers.end}
  >
    <div class="sr-frame-grip"><${SrIcon} name="grip" size=${14} /></div>
    <div class="sr-frame-num">${index + 1}</div>
    <div class="sr-frame-thumb">
      <img src=${frame.src} alt=${"フレーム " + (index + 1)} draggable=${false} />
      ${frame.status === "reading" && html`<div class="sr-frame-scan"></div>`}
    </div>
    <div class="sr-frame-body">
      ${hasText
        ? html`<div class="sr-frame-text">${frame.lines.join(" ")}</div>`
        : done
          ? html`<div class="sr-frame-text is-empty">字幕を検出できませんでした</div>`
          : html`<div class="sr-frame-status">
              ${frame.status === "reading" ? "読み取り中…" :
                frame.status === "error" ? "読み取りに失敗しました" : "待機中"}
            </div>`}
      <div class="sr-frame-meta">
        ${done && frame.hasChrome && html`<span class="sr-badge sr-badge-chrome"
          title="タイトルやシークバーなどのUI要素は読み取り対象から自動で除外されます">UI要素を除外</span>`}
        ${hasText && html`<span class=${"sr-badge " + (frame.endsComplete ? "sr-badge-done" : "sr-badge-cont")}>
          ${frame.endsComplete ? "文末" : "次へ続く"}
        </span>`}
        ${frame.status === "error" && html`<button class="sr-btn sr-btn-ghost sr-btn-xs" onClick=${() => onRetry(frame.id)}>
          <${SrIcon} name="retry" size=${11} /> 再試行
        </button>`}
      </div>
    </div>
    <button class="sr-iconbtn sr-frame-del" title="削除" aria-label="削除" onClick=${() => onDelete(frame.id)}>
      <${SrIcon} name="x" size=${14} />
    </button>
  </div>`;
}

/* ---------- connector between frames ---------- */
export function JoinConnector({ joined, auto, onToggle, disabled }) {
  return html`<div class="sr-connector">
    <div class=${"sr-connector-line" + (joined ? " is-joined" : "")}></div>
    <button
      class=${"sr-connector-chip" + (joined ? " is-joined" : "")}
      onClick=${onToggle}
      disabled=${disabled}
      title=${joined ? "クリックで区切る" : "クリックで連結する"}
    >
      <${SrIcon} name="link" size=${12} />
      ${joined ? "連結" : "区切り"}
      ${auto && html`<span class="sr-connector-auto">自動</span>`}
    </button>
    <div class=${"sr-connector-line" + (joined ? " is-joined" : "")}></div>
  </div>`;
}

/* ---------- sentence (merged result) card ---------- */
export function SentenceCard({ group, onCopy, fontSize }) {
  const [copied, setCopied] = useState(false);
  const doCopy = () => {
    onCopy(group.text);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };
  return html`<div class="sr-sentence">
    <div class="sr-sentence-top">
      <div class="sr-sentence-frames">
        ${group.frameIndexes.map((n) => html`<span key=${n} class="sr-framechip">${n + 1}</span>`)}
        ${group.frameIndexes.length > 1 && html`<span class="sr-sentence-merged">連結済み</span>`}
      </div>
      <button class=${"sr-iconbtn" + (copied ? " is-ok" : "")} onClick=${doCopy} title="この文をコピー" aria-label="この文をコピー">
        <${SrIcon} name=${copied ? "check" : "copy"} size=${14} />
      </button>
    </div>
    <p class="sr-sentence-text" style=${{ fontSize: fontSize + "px" }}>${group.text}</p>
  </div>`;
}

/* ---------- history ---------- */
export function HistoryPanel({ items, onCopy, onDelete, onClose }) {
  return html`<div class="sr-history">
    <div class="sr-history-head">
      <span class="sr-history-title"><${SrIcon} name="clock" size=${14} style=${{ marginRight: 6 }} />読み取り履歴</span>
      <button class="sr-iconbtn" onClick=${onClose} title="閉じる" aria-label="閉じる"><${SrIcon} name="x" size=${14} /></button>
    </div>
    ${items.length === 0
      ? html`<div class="sr-history-empty">まだ履歴がありません。読み取り結果を「履歴に保存」すると、ここに残ります。</div>`
      : html`<div class="sr-history-list">
          ${items.map((it) => html`<div key=${it.id} class="sr-history-item">
            <div class="sr-history-item-meta">
              <span>${it.time}</span>
              <span>${it.count}枚</span>
            </div>
            <div class="sr-history-item-text">${it.text}</div>
            <div class="sr-history-item-actions">
              <button class="sr-iconbtn" onClick=${() => onCopy(it.text)} title="コピー" aria-label="コピー"><${SrIcon} name="copy" size=${13} /></button>
              <button class="sr-iconbtn" onClick=${() => onDelete(it.id)} title="削除" aria-label="削除"><${SrIcon} name="trash" size=${13} /></button>
            </div>
          </div>`)}
        </div>`}
  </div>`;
}

/* ---------- empty state for results ---------- */
export function ResultsEmpty() {
  return html`<div class="sr-results-empty">
    <div class="sr-results-empty-lines">
      <div class="sr-ghostline" style=${{ width: "55%" }}></div>
      <div class="sr-ghostline" style=${{ width: "80%" }}></div>
    </div>
    <p>画像を追加すると、ここに読み取った字幕が表示されます。</p>
  </div>`;
}

/* ---------- settings popover (表示設定) ---------- */
export function SettingsPanel({ settings, onChange, onClose }) {
  const set = (patch) => onChange({ ...settings, ...patch });
  return html`<div class="sr-settings">
    <div class="sr-settings-head">
      <span class="sr-settings-title"><${SrIcon} name="gear" size=${14} style=${{ marginRight: 6 }} />表示設定</span>
      <button class="sr-iconbtn" onClick=${onClose} title="閉じる" aria-label="閉じる"><${SrIcon} name="x" size=${14} /></button>
    </div>
    <div class="sr-settings-body">
      <div class="sr-set-row">
        <span class="sr-set-label">テーマ</span>
        <div class="sr-seg">
          <button class=${settings.theme === "dark" ? "is-active" : ""} onClick=${() => set({ theme: "dark" })}>ダーク</button>
          <button class=${settings.theme === "light" ? "is-active" : ""} onClick=${() => set({ theme: "light" })}>ライト</button>
        </div>
      </div>
      <div class="sr-set-row">
        <span class="sr-set-label">アクセント</span>
        <div class="sr-swatches">
          ${ACCENT_OPTIONS.map((c) => html`<button key=${c}
            class=${"sr-swatch" + (settings.accent === c ? " is-active" : "")}
            style=${{ background: c }}
            title=${c} aria-label=${"アクセント " + c}
            onClick=${() => set({ accent: c })}></button>`)}
        </div>
      </div>
      <div class="sr-set-row">
        <span class="sr-set-label">字幕の文字サイズ</span>
        <div class="sr-set-size">
          <input type="range" min="16" max="34" step="1" value=${settings.subtitleSize}
            onInput=${(e) => set({ subtitleSize: Number(e.target.value) })} />
          <span class="sr-set-size-val">${settings.subtitleSize}px</span>
        </div>
      </div>
      <div class="sr-set-row">
        <span class="sr-set-label">中央揃え</span>
        <button class=${"sr-toggle" + (settings.centerText ? " is-on" : "")}
          role="switch" aria-checked=${settings.centerText} aria-label="中央揃え"
          onClick=${() => set({ centerText: !settings.centerText })}>
          <span class="sr-toggle-knob"></span>
        </button>
      </div>
    </div>
  </div>`;
}
