/* ==========================================================================
   Connections preview — filtering, view mode and new connection sheet
   ========================================================================== */

const sheet = document.getElementById('connSheet');
const grid = document.getElementById('connGrid');
const search = document.getElementById('connSearch');
const count = document.getElementById('connCount');
const testResult = document.getElementById('testResult');
let activeEnv = 'all';
let lastFocused = null;

function openSheet() {
  lastFocused = document.activeElement;
  sheet.hidden = false;
  requestAnimationFrame(() => sheet.classList.add('show'));
  document.body.style.overflow = 'hidden';
  setTimeout(() => document.getElementById('newConnName')?.focus(), 180);
}

function closeSheet() {
  sheet.hidden = true;
  sheet.classList.remove('show');
  document.body.style.overflow = '';
  lastFocused?.focus?.();
}

document.querySelectorAll('[data-open-sheet]').forEach((el) => {
  el.addEventListener('click', (e) => {
    e.preventDefault();
    openSheet();
  });
});

document.querySelectorAll('[data-close-sheet]').forEach((el) => {
  el.addEventListener('click', closeSheet);
});

sheet.addEventListener('mousedown', (e) => {
  if (e.target === sheet) closeSheet();
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape' && !sheet.hidden) closeSheet();
});

if (new URLSearchParams(location.search).get('new') === '1') {
  openSheet();
}

/* tabs ------------------------------------------------------------------- */

const sheetTabs = document.getElementById('sheetTabs');
sheetTabs.addEventListener('click', (e) => {
  const btn = e.target.closest('[data-sheet-tab]');
  if (!btn) return;
  sheetTabs.querySelectorAll('button').forEach((b) => b.classList.toggle('active', b === btn));
  document.querySelectorAll('.sheet-panel').forEach((panel) => {
    panel.hidden = panel.dataset.panel !== btn.dataset.sheetTab;
  });
});

/* filters ---------------------------------------------------------------- */

function applyFilters() {
  const q = search.value.trim().toLowerCase();
  let visible = 0;
  grid.querySelectorAll('.conn-card:not(.new)').forEach((card) => {
    const envMatch = activeEnv === 'all' || card.dataset.env === activeEnv;
    const textMatch = !q || card.textContent.toLowerCase().includes(q) || (card.dataset.name || '').toLowerCase().includes(q);
    const show = envMatch && textMatch;
    card.hidden = !show;
    if (show) visible += 1;
  });
  count.textContent = `${visible} 个连接`;
}

document.getElementById('envTabs').addEventListener('click', (e) => {
  const btn = e.target.closest('[data-env]');
  if (!btn) return;
  activeEnv = btn.dataset.env;
  document.querySelectorAll('#envTabs button').forEach((b) => b.classList.toggle('active', b === btn));
  applyFilters();
});

search.addEventListener('input', applyFilters);

/* view mode -------------------------------------------------------------- */

document.getElementById('viewToggle').addEventListener('click', (e) => {
  const btn = e.target.closest('[data-view]');
  if (!btn) return;
  document.querySelectorAll('#viewToggle button').forEach((b) => b.classList.toggle('active', b === btn));
  grid.classList.toggle('list', btn.dataset.view === 'list');
});

/* card actions ----------------------------------------------------------- */

grid.addEventListener('click', (e) => {
  if (e.target.closest('[data-card-menu]')) {
    e.stopPropagation();
    const card = e.target.closest('.conn-card');
    showCardMenu(e.target.closest('[data-card-menu]'), card);
    return;
  }
  const card = e.target.closest('.conn-card');
  if (!card) return;
  if (card.classList.contains('new')) {
    openSheet();
    return;
  }
  if (card.dataset.href) window.location.href = card.dataset.href;
  else rc.toast('连接失败 · 请检查 Host 与网络', 'error');
});

function showCardMenu(anchor, card) {
  document.querySelectorAll('.conn-card .menu').forEach((m) => m.remove());
  const menu = document.createElement('div');
  menu.className = 'menu';
  menu.style.top = '38px';
  menu.style.right = '10px';
  menu.innerHTML = `
    <div class="menu-item" data-menu-act="open"><span class="ic">${rc.svgFor('key')}</span>打开工作区</div>
    <div class="menu-item" data-menu-act="edit"><span class="ic">${rc.svgFor('pencil')}</span>编辑连接</div>
    <div class="menu-item" data-menu-act="copy"><span class="ic">${rc.svgFor('copy')}</span>复制连接</div>
    <div class="menu-item" data-menu-act="test"><span class="ic">${rc.svgFor('wifi')}</span>测试连接</div>
    <div class="menu-sep"></div>
    <div class="menu-item danger" data-menu-act="delete"><span class="ic">${rc.svgFor('trash')}</span>移除连接</div>`;
  card.appendChild(menu);
  menu.addEventListener('click', (e) => {
    e.stopPropagation();
    const act = e.target.closest('[data-menu-act]')?.dataset.menuAct;
    menu.remove();
    if (act === 'open' && card.dataset.href) window.location.href = card.dataset.href;
    if (act === 'edit') openSheet();
    if (act === 'copy') rc.toast('已复制连接配置');
    if (act === 'test') rc.toast('连接可用 · 12 ms');
    if (act === 'delete') rc.toast('已移除连接（示例）', 'error');
  });
  setTimeout(() => {
    document.addEventListener(
      'click',
      (ev) => {
        if (!ev.target.closest('.conn-card .menu')) menu.remove();
      },
      { once: true },
    );
  }, 0);
}

/* colors ----------------------------------------------------------------- */

document.querySelectorAll('#connSheet .conn-color-row .swatch').forEach((swatch) => {
  swatch.addEventListener('click', () => {
    document.querySelectorAll('#connSheet .conn-color-row .swatch').forEach((s) => s.classList.remove('active'));
    swatch.classList.add('active');
  });
});

/* test & save ------------------------------------------------------------ */

document.getElementById('testConn').addEventListener('click', (e) => {
  const btn = e.currentTarget;
  const original = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = `<span class="ic">${rc.svgFor('refresh')}</span>测试中…`;
  setTimeout(() => {
    btn.disabled = false;
    btn.innerHTML = original;
    testResult.hidden = false;
    testResult.classList.remove('fail');
    testResult.textContent = '✓ 连接成功 · 12 ms · Redis 7.4.2';
    rc.toast('连接成功 · 12 ms');
  }, 900);
});

document.getElementById('saveConn').addEventListener('click', () => {
  const name = document.getElementById('newConnName');
  const host = document.getElementById('newConnHost');
  if (!name.value.trim()) {
    name.style.borderColor = 'var(--danger)';
    name.focus();
    rc.toast('请填写连接名称', 'error');
    return;
  }
  if (!host.value.trim()) {
    host.focus();
    rc.toast('请填写 Host', 'error');
    return;
  }
  closeSheet();
  rc.toast(`已保存连接 ${name.value.trim()}`);
});

document.getElementById('newConnName')?.addEventListener('input', (e) => {
  e.target.style.borderColor = '';
});

document.getElementById('importBtn')?.addEventListener('click', () => {
  rc.toast('支持导入 JSON / .env 连接配置', 'success');
});

applyFilters();
