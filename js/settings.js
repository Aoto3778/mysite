// 表示設定 (テーマ / アクセント / 字幕サイズ / 中央揃え) の永続化と適用

const SETTINGS_KEY = "engsub-reader-settings-v1";

export const ACCENT_OPTIONS = ["#FF8A3D", "#5EC9F8", "#7BE0A2", "#E76A9B"];

export const DEFAULT_SETTINGS = {
  accent: "#FF8A3D",
  theme: "dark", // "dark" | "light"
  subtitleSize: 22, // 16〜34px
  centerText: true,
};

export function loadSettings() {
  try {
    const raw = JSON.parse(localStorage.getItem(SETTINGS_KEY));
    if (!raw || typeof raw !== "object") return { ...DEFAULT_SETTINGS };
    return {
      accent: ACCENT_OPTIONS.includes(raw.accent) ? raw.accent : DEFAULT_SETTINGS.accent,
      theme: raw.theme === "light" ? "light" : "dark",
      subtitleSize: Math.min(34, Math.max(16, Number(raw.subtitleSize) || DEFAULT_SETTINGS.subtitleSize)),
      centerText: raw.centerText !== false,
    };
  } catch (e) {
    return { ...DEFAULT_SETTINGS };
  }
}

export function saveSettings(settings) {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch (e) {}
}

export function applySettings(settings) {
  const root = document.documentElement;
  root.setAttribute("data-theme", settings.theme === "light" ? "light" : "dark");
  root.style.setProperty("--accent", settings.accent);
}
