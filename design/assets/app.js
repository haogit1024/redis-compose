/* ==========================================================================
   Redis Compose — shared preview behaviours
   ========================================================================== */

const ICONS = {
  logo: '<path d="M12 3.2 3.6 7.4 12 11.6l8.4-4.2L12 3.2Z" fill="currentColor" opacity=".95"/><path d="M4.6 11.3 12 15l7.4-3.7" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/><path d="M4.6 15.2 12 18.9l7.4-3.7" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round" opacity=".55"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="m16.6 16.6 4.4 4.4"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  close: '<path d="m6 6 12 12M18 6 6 18"/>',
  'chevron-down': '<path d="m6 9 6 6 6-6"/>',
  'chevron-right': '<path d="m9 6 6 6-6 6"/>',
  'chevron-up': '<path d="m18 15-6-6-6 6"/>',
  'chevron-left': '<path d="m15 18-6-6 6-6"/>',
  'arrow-left': '<path d="M19 12H5m7-7-7 7 7 7"/>',
  'arrow-up-right': '<path d="M7 17 17 7M8 7h9v9"/>',
  refresh: '<path d="M20.5 12a8.5 8.5 0 1 1-2.6-6.1"/><path d="M21 3v5.5h-5.5"/>',
  sliders: '<path d="M4 7h9M17 7h3M4 17h3M11 17h9"/><circle cx="15" cy="7" r="2"/><circle cx="9" cy="17" r="2"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z"/>',
  key: '<circle cx="7.5" cy="15.5" r="3.5"/><path d="m10.5 12.5 8.5-8.5M15 8l3 3M18 5l2 2"/>',
  terminal: '<path d="m5 7 5 5-5 5M13 17h6"/>',
  activity: '<path d="M3 12h4l2.4-6.5 5.2 13L17 12h4"/>',
  database: '<ellipse cx="12" cy="5.5" rx="8" ry="3"/><path d="M4 5.5v13c0 1.66 3.58 3 8 3s8-1.34 8-3v-13"/><path d="M4 12c0 1.66 3.58 3 8 3s8-1.34 8-3"/>',
  server: '<rect x="3" y="4" width="18" height="7" rx="2"/><rect x="3" y="13" width="18" height="7" rx="2"/><path d="M7 7.5h.01M7 16.5h.01M11 7.5h4M11 16.5h4"/>',
  star: '<path d="m12 3.4 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.9-5.4 2.9 1-6.1L3.2 9.8l6.1-.9L12 3.4Z"/>',
  copy: '<rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v1"/>',
  trash: '<path d="M3.5 6h17M9 6V3.8h6V6M18.5 6l-1 14.2h-11L5.5 6M10 10.5v5.5M14 10.5v5.5"/>',
  pencil: '<path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/>',
  more: '<circle cx="5" cy="12" r="1"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5.2l3.2 2"/>',
  cpu: '<rect x="6" y="6" width="12" height="12" rx="2"/><rect x="9.5" y="9.5" width="5" height="5" rx="1"/><path d="M9 2v2M15 2v2M9 20v2M15 20v2M2 9h2M2 15h2M20 9h2M20 15h2"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 7M18.5 20a6 6 0 0 0-3-5.2"/>',
  shield: '<path d="m12 3 7 3v6c0 4.5-3 7.6-7 9-4-1.4-7-4.5-7-9V6Z"/><path d="m9 12 2 2 4-4"/>',
  lock: '<rect x="4.5" y="10.5" width="15" height="10" rx="2"/><path d="M8 10.5V7a4 4 0 0 1 8 0v3.5"/>',
  folder: '<path d="M3 6.5A2 2 0 0 1 5 4.5h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z"/>',
  'folder-open': '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v1H7l-4 9V7Z"/><path d="M3 19 7 9h15l-4 10H3Z"/>',
  play: '<path d="M7 4.5 19 12 7 19.5Z" fill="currentColor" stroke="none"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  'check-circle': '<circle cx="12" cy="12" r="9"/><path d="m8 12 2.7 2.7L16 9"/>',
  alert: '<path d="M10.3 4.2 2.7 17.5A2 2 0 0 0 4.4 20.5h15.2a2 2 0 0 0 1.7-3L13.7 4.2a2 2 0 0 0-3.4 0Z"/><path d="M12 9v4M12 16.8h.01"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.8h.01"/>',
  zap: '<path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z"/>',
  wifi: '<path d="M2.5 8.5a14 14 0 0 1 19 0M5.5 12a9.5 9.5 0 0 1 13 0M8.5 15.5a5 5 0 0 1 7 0M12 19h.01"/>',
  external: '<path d="M15 3h6v6M10 14 21 3M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"/>',
  command: '<path d="M15 6v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3V6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3"/>',
  hash: '<path d="M4 9h16M4 15h16M10 3 8 21M16 3l-2 18"/>',
  list: '<path d="M9 6h12M9 12h12M9 18h12"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
  layers: '<path d="m12 2.5 9.5 4.7L12 12 2.5 7.2 12 2.5Z"/><path d="m2.5 12 9.5 4.8 9.5-4.8M2.5 16.8l9.5 4.7 9.5-4.7"/>',
  grid: '<rect x="3.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.5"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.5"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.5"/>',
  filter: '<path d="M3 5h18l-7 8v6l-4 2v-8L3 5Z"/>',
  sort: '<path d="M8 4v16M4.5 16.5 8 20l3.5-3.5M16 20V4M12.5 7.5 16 4l3.5 3.5"/>',
  eye: '<path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z"/><circle cx="12" cy="12" r="3"/>',
  'eye-off': '<path d="m3 3 18 18M10.6 6.1A9.9 9.9 0 0 1 12 6c6 0 9.5 6 9.5 6a17 17 0 0 1-3.2 3.9M6.6 6.7C4 8.5 2.5 12 2.5 12S6 18 12 18c1.6 0 3-.4 4.2-1"/><path d="M9.9 9.9a3 3 0 0 0 4.2 4.2"/>',
  link: '<path d="M10 13a5 5 0 0 0 7.5.5l2-2a5 5 0 0 0-7-7l-1.1 1.1"/><path d="M14 11a5 5 0 0 0-7.5-.5l-2 2a5 5 0 0 0 7 7l1.1-1.1"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>',
  keyboard: '<rect x="2.5" y="6" width="19" height="12" rx="2"/><path d="M6 10h.01M9.5 10h.01M13 10h.01M16.5 10h.01M6 13.5h.01M9.5 13.5h.01M13 13.5h.01M16.5 13.5h.01M8 16.5h8"/>',
  bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8M10 20a2.2 2.2 0 0 0 4 0"/>',
  send: '<path d="m3.5 3.5 18 8.5-18 8.5 3-8.5-3-8.5Z"/><path d="M6.5 12h15"/>',
  save: '<path d="M4 4h12l4 4v12H4Z"/><path d="M8 4v6h8V4M8 20v-6h8v6"/>',
  book: '<path d="M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17H6.5A2.5 2.5 0 0 0 4 21.5Z"/><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>',
  gauge: '<path d="M4.5 19a9 9 0 1 1 15 0"/><path d="m12 12 4-3"/><circle cx="12" cy="12" r="1.5"/>',
  percent: '<path d="M19 5 5 19"/><circle cx="6.5" cy="6.5" r="2.5"/><circle cx="17.5" cy="17.5" r="2.5"/>',
  download: '<path d="M12 3v12M7.5 10.5 12 15l4.5-4.5M4 20h16"/>',
  upload: '<path d="M12 16V4M7.5 8.5 12 4l4.5 4.5M4 20h16"/>',
  code: '<path d="m8.5 7-5 5 5 5M15.5 7l5 5-5 5"/>',
  split: '<rect x="3" y="4" width="18" height="16" rx="2"/><path d="M12 4v16"/>',
  sparkles: '<path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="m19 16 .8 2.2L22 19l-2.2.8L19 22l-.8-2.2L16 19l2.2-.8L19 16Z"/>',
  history: '<path d="M3.5 12a8.5 8.5 0 1 0 2.5-6M3.5 3.5V9H9"/><path d="M12 7.5V12l3 1.8"/>',
  'plus-circle': '<circle cx="12" cy="12" r="9"/><path d="M12 8v8M8 12h8"/>',
  'x-circle': '<circle cx="12" cy="12" r="9"/><path d="m9 9 6 6M15 9l-6 6"/>',
  'corner-down-left': '<path d="M20 4v7a4 4 0 0 1-4 4H5M9 11l-4 4 4 4"/>',
  drag: '<circle cx="9" cy="6" r="1"/><circle cx="15" cy="6" r="1"/><circle cx="9" cy="12" r="1"/><circle cx="15" cy="12" r="1"/><circle cx="9" cy="18" r="1"/><circle cx="15" cy="18" r="1"/>',
  'git-branch': '<path d="M6 3v12M18 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM18 9a9 9 0 0 1-9 9"/>',
  'maximize': '<path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5"/>',
  'command-key': '<path d="M9 6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3V6Z"/>',
};

const TYPE_LABEL = {
  string: 'STRING',
  hash: 'HASH',
  list: 'LIST',
  set: 'SET',
  zset: 'ZSET',
  stream: 'STREAM',
  none: 'NONE',
};

function svgFor(name, cls = '') {
  if (name === 'logo') {
    return `<svg viewBox="0 0 24 24" class="${cls}" aria-hidden="true">${ICONS.logo}</svg>`;
  }
  return `<svg viewBox="0 0 24 24" class="${cls}" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name] || ''}</svg>`;
}

function mountIcons(root = document) {
  root.querySelectorAll('[data-ic]').forEach((el) => {
    if (el.dataset.icDone === '1') return;
    el.innerHTML = svgFor(el.dataset.ic);
    el.dataset.icDone = '1';
  });
}

/* ------------------------------------------------------------------ theme */

const THEME_KEY = 'rc-theme';

function applyTheme(theme) {
  const safe = theme === 'light' ? 'light' : 'dark';
  document.documentElement.dataset.theme = safe;
  try {
    localStorage.setItem(THEME_KEY, safe);
  } catch (e) {}
  document.querySelectorAll('[data-theme-toggle]').forEach((btn) => {
    btn.innerHTML = svgFor(safe === 'dark' ? 'sun' : 'moon');
    btn.dataset.tip = safe === 'dark' ? '切换到浅色' : '切换到深色';
  });
  window.dispatchEvent(new CustomEvent('rc:theme', { detail: safe }));
}

function syncThemeCards(choice) {
  document.querySelectorAll('[data-theme-choice]').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.themeChoice === choice);
  });
}

function setThemeChoice(choice) {
  try {
    localStorage.setItem('rc-theme-choice', choice);
  } catch (e) {}
  syncThemeCards(choice);
}

function currentTheme() {
  return document.documentElement.dataset.theme === 'light' ? 'light' : 'dark';
}

function toggleTheme() {
  const next = currentTheme() === 'dark' ? 'light' : 'dark';
  applyTheme(next);
  setThemeChoice(next);
}

/* ----------------------------------------------------------------- accent */

const ACCENTS = {
  iris: {
    dark: { a: '#918dff', b: '#6b67f4' },
    light: { a: '#5b58e8', b: '#4a47d4' },
  },
  mint: {
    dark: { a: '#45e3be', b: '#19b99a' },
    light: { a: '#0c9f7f', b: '#0a8a6e' },
  },
  sky: {
    dark: { a: '#6fb7ff', b: '#3d8fe0' },
    light: { a: '#2f7dd1', b: '#2668b3' },
  },
  amber: {
    dark: { a: '#f7c566', b: '#d9a33f' },
    light: { a: '#a86a12', b: '#8e590e' },
  },
  rose: {
    dark: { a: '#ff7ba9', b: '#e05287' },
    light: { a: '#d63f78', b: '#bb2f66' },
  },
};

function hexToRgba(hex, alpha) {
  const value = hex.replace('#', '');
  const r = parseInt(value.slice(0, 2), 16);
  const g = parseInt(value.slice(2, 4), 16);
  const b = parseInt(value.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function applyAccent(name) {
  const accent = ACCENTS[name] || ACCENTS.iris;
  const theme = currentTheme();
  const palette = accent[theme];
  const root = document.documentElement.style;
  root.setProperty('--accent', palette.a);
  root.setProperty('--accent-2', palette.b);
  const value = palette.a.replace('#', '');
  const r = parseInt(value.slice(0, 2), 16) / 255;
  const g = parseInt(value.slice(2, 4), 16) / 255;
  const b = parseInt(value.slice(4, 6), 16) / 255;
  const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
  root.setProperty('--accent-ink', theme === 'light' || luminance < 0.6 ? '#ffffff' : '#090b10');
  root.setProperty('--accent-soft', hexToRgba(palette.a, theme === 'light' ? 0.1 : 0.13));
  root.setProperty('--accent-softer', hexToRgba(palette.a, theme === 'light' ? 0.05 : 0.06));
  root.setProperty('--accent-line', hexToRgba(palette.a, theme === 'light' ? 0.35 : 0.4));
  document.querySelectorAll('.accent-swatch').forEach((swatch) => {
    swatch.classList.toggle('active', swatch.dataset.accent === name);
  });
  try {
    localStorage.setItem('rc-accent', name);
  } catch (e) {}
  window.dispatchEvent(new CustomEvent('rc:accent', { detail: name }));
}

/* ------------------------------------------------------------------ toast */

function toast(message, kind = 'success') {
  let stack = document.querySelector('.toast-stack');
  if (!stack) {
    stack = document.createElement('div');
    stack.className = 'toast-stack';
    document.body.appendChild(stack);
  }
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  const icon = kind === 'error' ? 'x-circle' : kind === 'warn' ? 'alert' : 'check-circle';
  el.innerHTML = `<span class="ic">${svgFor(icon)}</span><span>${message}</span>`;
  stack.appendChild(el);
  setTimeout(() => {
    el.style.transition = 'opacity .2s ease, transform .2s ease';
    el.style.opacity = '0';
    el.style.transform = 'translateY(4px)';
    setTimeout(() => el.remove(), 220);
  }, 2600);
}

/* ---------------------------------------------------------------- clipboard */

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
  } catch (e) {
    const ta = document.createElement('textarea');
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    try {
      document.execCommand('copy');
    } catch (err) {}
    ta.remove();
  }
  toast('已复制到剪贴板');
}

/* ----------------------------------------------------------- command palette */

const PALETTE_ITEMS = [
  { group: '页面', icon: 'key', label: '键浏览器', hint: 'Workspace', href: 'workspace.html' },
  { group: '页面', icon: 'terminal', label: '命令控制台', hint: 'Console', href: 'console.html' },
  { group: '页面', icon: 'activity', label: '服务器洞察', hint: 'Insights', href: 'insights.html' },
  { group: '页面', icon: 'database', label: '连接管理', hint: 'Connections', href: 'connections.html' },
  { group: '页面', icon: 'sliders', label: '偏好设置', hint: 'Settings', href: 'settings.html' },
  { group: '页面', icon: 'sparkles', label: '设计总览', hint: 'Overview', href: 'index.html' },
  { group: '操作', icon: 'plus-circle', label: '新建 Key', hint: 'N', action: 'new-key' },
  { group: '操作', icon: 'server', label: '新建连接', hint: '⇧N', href: 'connections.html?new=1' },
  { group: '操作', icon: 'refresh', label: '刷新当前 Keyspace', hint: 'R', action: 'refresh' },
  { group: '操作', icon: 'moon', label: '切换深色 / 浅色主题', hint: 'T', action: 'theme' },
  { group: '连接', icon: 'database', label: 'Prod Cache · db0', hint: '12 ms', href: 'workspace.html' },
  { group: '连接', icon: 'database', label: 'Session Store · db2', hint: '8 ms', href: 'workspace.html' },
  { group: '连接', icon: 'database', label: 'Billing Staging · db0', hint: '41 ms', href: 'workspace.html' },
  { group: '连接', icon: 'database', label: 'Local Sandbox · db0', hint: '1 ms', href: 'workspace.html' },
];

let paletteState = { open: false, index: 0, filtered: [] };

function ensurePalette() {
  if (document.getElementById('rc-palette')) return;
  const wrap = document.createElement('div');
  wrap.className = 'overlay';
  wrap.id = 'rc-palette';
  wrap.hidden = true;
  wrap.innerHTML = `
    <div class="palette" role="dialog" aria-modal="true" aria-label="命令面板">
      <div class="palette-input">
        <span class="ic">${svgFor('search')}</span>
        <input id="rc-palette-input" placeholder="搜索页面、命令或连接…" autocomplete="off" spellcheck="false" />
        <kbd>ESC</kbd>
      </div>
      <div class="palette-list" id="rc-palette-list"></div>
      <div class="palette-foot">
        <span><kbd>↑</kbd><kbd>↓</kbd> 导航</span>
        <span><kbd>↵</kbd> 打开</span>
        <span><kbd>⌘</kbd><kbd>K</kbd> 关闭</span>
        <span style="margin-left:auto">Redis Compose</span>
      </div>
    </div>`;
  document.body.appendChild(wrap);
  wrap.addEventListener('mousedown', (e) => {
    if (e.target === wrap) closePalette();
  });
  wrap.querySelector('#rc-palette-input').addEventListener('input', (e) => {
    paletteState.index = 0;
    renderPalette(e.target.value);
  });
  wrap.querySelector('#rc-palette-input').addEventListener('keydown', (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      movePalette(1);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      movePalette(-1);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      runPaletteItem(paletteState.filtered[paletteState.index]);
    }
  });
}

function renderPalette(query = '') {
  const list = document.getElementById('rc-palette-list');
  if (!list) return;
  const q = query.trim().toLowerCase();
  const items = PALETTE_ITEMS.filter(
    (it) => !q || it.label.toLowerCase().includes(q) || it.hint.toLowerCase().includes(q) || it.group.toLowerCase().includes(q),
  );
  paletteState.filtered = items;
  if (paletteState.index >= items.length) paletteState.index = Math.max(0, items.length - 1);
  let html = '';
  let group = '';
  items.forEach((it, i) => {
    if (it.group !== group) {
      group = it.group;
      html += `<div class="palette-group-title">${group}</div>`;
    }
    html += `<div class="palette-item ${i === paletteState.index ? 'active' : ''}" data-index="${i}">
      <span class="ic">${svgFor(it.icon)}</span>
      <span>${it.label}</span>
      <span class="meta">${it.hint}</span>
    </div>`;
  });
  if (!items.length) html = `<div class="empty" style="padding:32px"><div class="empty-desc">没有匹配的命令</div></div>`;
  list.innerHTML = html;
  list.querySelectorAll('.palette-item').forEach((el) => {
    el.addEventListener('mouseenter', () => {
      paletteState.index = Number(el.dataset.index);
      list.querySelectorAll('.palette-item').forEach((n) => n.classList.remove('active'));
      el.classList.add('active');
    });
    el.addEventListener('click', () => runPaletteItem(items[Number(el.dataset.index)]));
  });
}

function movePalette(dir) {
  const n = paletteState.filtered.length;
  if (!n) return;
  paletteState.index = (paletteState.index + dir + n) % n;
  const list = document.getElementById('rc-palette-list');
  list.querySelectorAll('.palette-item').forEach((el) => {
    el.classList.toggle('active', Number(el.dataset.index) === paletteState.index);
  });
  const active = list.querySelector('.palette-item.active');
  if (active) active.scrollIntoView({ block: 'nearest' });
}

function openPalette() {
  ensurePalette();
  const overlay = document.getElementById('rc-palette');
  overlay.hidden = false;
  paletteState.index = 0;
  renderPalette('');
  const input = document.getElementById('rc-palette-input');
  input.value = '';
  setTimeout(() => input.focus(), 30);
}

function closePalette() {
  const overlay = document.getElementById('rc-palette');
  if (overlay) overlay.hidden = true;
}

function runPaletteItem(item) {
  if (!item) return;
  closePalette();
  if (item.href) {
    window.location.href = item.href;
    return;
  }
  if (item.action === 'theme') toggleTheme();
  if (item.action === 'refresh') toast('Keyspace 已刷新', 'success');
  if (item.action === 'new-key') {
    const button = document.querySelector('[data-action="new-key"]');
    if (button) button.click();
    else toast('新建 Key · 快捷键 N');
  }
}

/* --------------------------------------------------------------- bootstrap */

document.addEventListener('DOMContentLoaded', () => {
  mountIcons();
  ensurePalette();
  applyTheme(currentTheme());
  try {
    syncThemeCards(localStorage.getItem('rc-theme-choice') || currentTheme());
  } catch (e) {
    syncThemeCards(currentTheme());
  }

  document.querySelectorAll('[data-theme-toggle]').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      toggleTheme();
    });
  });

  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      const overlay = document.getElementById('rc-palette');
      if (overlay && !overlay.hidden) closePalette();
      else openPalette();
    }
    if (e.key === 'Escape') {
      closePalette();
      document.querySelectorAll('.overlay:not([hidden])').forEach((el) => {
        if (el.id !== 'rc-palette' && el.dataset.static !== '1') el.hidden = true;
      });
    }
  });

  document.querySelectorAll('[data-open-palette]').forEach((el) => {
    el.addEventListener('focus', openPalette);
    el.addEventListener('click', openPalette);
  });

  document.querySelectorAll('[data-copy]').forEach((el) => {
    el.addEventListener('click', () => copyText(el.dataset.copy));
  });

  document.querySelectorAll('.switch').forEach((el) => {
    el.setAttribute('role', 'switch');
    el.setAttribute('tabindex', '0');
    const toggle = () => {
      el.classList.toggle('on');
      el.setAttribute('aria-checked', el.classList.contains('on') ? 'true' : 'false');
      el.dispatchEvent(new CustomEvent('rc:switch', { bubbles: true, detail: el.classList.contains('on') }));
    };
    el.addEventListener('click', toggle);
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        toggle();
      }
    });
  });

  document.querySelectorAll('[data-theme-choice]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const choice = btn.dataset.themeChoice;
      if (choice === 'system') {
        const sys = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
        applyTheme(sys);
        setThemeChoice('system');
        toast(`已跟随系统 · ${sys === 'light' ? '浅色' : '深色'}`);
      } else {
        applyTheme(choice);
        setThemeChoice(choice);
      }
    });
  });

  try {
    applyAccent(localStorage.getItem('rc-accent') || 'iris');
  } catch (e) {}
  window.addEventListener('rc:theme', () => {
    try {
      applyAccent(localStorage.getItem('rc-accent') || 'iris');
    } catch (e) {}
  });
  document.querySelectorAll('.accent-swatch').forEach((swatch) => {
    swatch.addEventListener('click', () => {
      applyAccent(swatch.dataset.accent);
      if (typeof toast === 'function') toast(`强调色已切换为 ${swatch.title || swatch.dataset.accent}`);
    });
  });

  document.querySelectorAll('[data-seg]').forEach((group) => {
    group.querySelectorAll('button').forEach((btn) => {
      btn.addEventListener('click', () => {
        group.querySelectorAll('button').forEach((b) => b.classList.remove('active'));
        btn.classList.add('active');
      });
    });
  });
});

window.rc = {
  toast,
  copyText,
  toggleTheme,
  applyTheme,
  applyAccent,
  openPalette,
  mountIcons,
  svgFor,
  icon: svgFor,
  typeLabel: TYPE_LABEL,
};
