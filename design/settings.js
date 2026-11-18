/* ==========================================================================
   Settings preview — accent palette and navigation
   ========================================================================== */

/* navigation ------------------------------------------------------------- */

const settingsContent = document.getElementById('settingsContent');
const settingsNav = document.getElementById('settingsNav');

settingsNav.addEventListener('click', (e) => {
  const link = e.target.closest('a[href^="#"]');
  if (!link) return;
  e.preventDefault();
  const section = document.querySelector(link.getAttribute('href'));
  if (!section) return;
  settingsNav.querySelectorAll('a').forEach((a) => a.classList.toggle('active', a === link));
  const top = section.getBoundingClientRect().top - settingsContent.getBoundingClientRect().top + settingsContent.scrollTop - 14;
  settingsContent.scrollTo({ top, behavior: 'smooth' });
});

/* font stepper ----------------------------------------------------------- */

const stepper = document.querySelector('.stepper');
if (stepper) {
  const valueEl = document.getElementById('fontSize');
  const [minus, plus] = stepper.querySelectorAll('button');
  let size = 13;
  minus.addEventListener('click', () => {
    size = Math.max(11, size - 1);
    valueEl.textContent = size;
    rc.toast(`编辑器字号 ${size}px`);
  });
  plus.addEventListener('click', () => {
    size = Math.min(18, size + 1);
    valueEl.textContent = size;
    rc.toast(`编辑器字号 ${size}px`);
  });
}

/* misc ------------------------------------------------------------------- */

document.getElementById('clearCache')?.addEventListener('click', () => {
  rc.toast('本地缓存已清除（命令历史、快照与筛选）', 'warn');
});

document.getElementById('checkUpdate')?.addEventListener('click', (e) => {
  const btn = e.currentTarget;
  const original = btn.innerHTML;
  btn.disabled = true;
  btn.innerHTML = `<span class="ic">${rc.svgFor('refresh')}</span>检查中…`;
  setTimeout(() => {
    btn.disabled = false;
    btn.innerHTML = original;
    rc.toast('已是最新版本 · 0.1.0');
  }, 900);
});

document.getElementById('copyPath')?.addEventListener('click', () => {
  rc.copyText('~/.redis-compose/settings.json');
});

/* switch feedback -------------------------------------------------------- */

document.querySelectorAll('#security .switch').forEach((sw) => {
  sw.addEventListener('rc:switch', (e) => {
    const label = sw.closest('.set-row')?.querySelector('.label')?.textContent || '设置';
    rc.toast(`${label}已${e.detail ? '开启' : '关闭'}`, e.detail ? 'success' : 'warn');
  });
});
