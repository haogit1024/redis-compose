/* ==========================================================================
   Insights preview — SVG charts
   ========================================================================== */

let ACCENT = '#918dff';

function accentColor() {
  const value = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim();
  return value || '#918dff';
}
const MINT = '#45e3be';
const SKY = '#6fb7ff';
const AMBER = '#f7c566';

const THROUGHPUT = {
  commands: [
    3100, 3340, 3060, 2780, 2520, 2860, 3540, 4180, 4620, 4410, 4280, 4690, 5120, 5380, 5210, 4920, 5280, 5610,
    5880, 5520, 5210, 4790, 4320, 3980, 4150,
  ],
  reads: [
    2280, 2480, 2300, 2080, 1870, 2150, 2680, 3180, 3520, 3360, 3240, 3560, 3910, 4100, 3980, 3760, 4040, 4310,
    4520, 4240, 4010, 3690, 3310, 3040, 3180,
  ],
};

const SPARKS = {
  memory: [44, 42, 40, 41, 38, 36, 34, 35, 32, 30, 31, 28, 26, 27, 24, 22, 21, 20, 18, 16, 15, 14, 13, 12],
  ops: [8, 12, 9, 14, 11, 16, 18, 15, 20, 17, 22, 25, 21, 27, 24, 29, 26, 31, 28, 33, 30, 35, 32, 36],
  hit: [60, 61, 60, 62, 63, 62, 64, 63, 65, 64, 66, 65, 66, 67, 66, 68, 67, 69, 68, 69, 70, 69, 71, 72],
  clients: [30, 28, 29, 27, 26, 27, 25, 26, 24, 25, 23, 24, 25, 24, 22, 23, 24, 23, 22, 24, 23, 22, 21, 22],
};

function smoothPath(points) {
  if (!points.length) return '';
  let d = `M${points[0][0].toFixed(2)},${points[0][1].toFixed(2)}`;
  for (let i = 0; i < points.length - 1; i++) {
    const [x0, y0] = points[i];
    const [x1, y1] = points[i + 1];
    const mx = (x0 + x1) / 2;
    d += ` C${mx.toFixed(2)},${y0.toFixed(2)} ${mx.toFixed(2)},${y1.toFixed(2)} ${x1.toFixed(2)},${y1.toFixed(2)}`;
  }
  return d;
}

function renderThroughput() {
  const mount = document.getElementById('throughputChart');
  if (!mount) return;
  const W = 760;
  const H = 212;
  const padL = 36;
  const padR = 10;
  const padT = 12;
  const padB = 24;
  const max = 6400;
  const n = THROUGHPUT.commands.length;
  const plotW = W - padL - padR;
  const plotH = H - padT - padB;
  const xAt = (i) => padL + (i * plotW) / (n - 1);
  const yAt = (v) => padT + (1 - v / max) * plotH;

  const cmdPoints = THROUGHPUT.commands.map((v, i) => [xAt(i), yAt(v)]);
  const readPoints = THROUGHPUT.reads.map((v, i) => [xAt(i), yAt(v)]);
  const cmdPath = smoothPath(cmdPoints);
  const readPath = smoothPath(readPoints);
  const area = `${cmdPath} L${xAt(n - 1)},${padT + plotH} L${padL},${padT + plotH} Z`;

  let grid = '';
  [0, 0.5, 1].forEach((p, i) => {
    const y = padT + (1 - p) * plotH;
    const value = Math.round((max * p) / 1000);
    grid += `<line x1="${padL}" y1="${y}" x2="${W - padR}" y2="${y}" stroke="var(--border)" stroke-dasharray="2 4" />`;
    grid += `<text x="${padL - 8}" y="${y + 3}" text-anchor="end" fill="var(--text-4)" font-size="7.5" font-family="var(--mono)">${value}k</text>`;
  });

  let labels = '';
  for (let i = 0; i < n; i += 4) {
    labels += `<text x="${xAt(i)}" y="${H - 6}" text-anchor="middle" fill="var(--text-4)" font-size="7.5" font-family="var(--mono)">${String(i).padStart(2, '0')}:00</text>`;
  }

  mount.innerHTML = `<svg viewBox="0 0 ${W} ${H}" role="img" aria-label="24 小时吞吐量">
    <defs>
      <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="${ACCENT}" stop-opacity=".34" />
        <stop offset="65%" stop-color="${ACCENT}" stop-opacity=".08" />
        <stop offset="100%" stop-color="${ACCENT}" stop-opacity="0" />
      </linearGradient>
      <linearGradient id="lineStroke" x1="0" y1="0" x2="1" y2="0">
        <stop offset="0%" stop-color="#8b7dff" />
        <stop offset="100%" stop-color="#a79bff" />
      </linearGradient>
    </defs>
    ${grid}
    <path d="${area}" fill="url(#areaFill)" class="chart-area-fade" />
    <path d="${cmdPath}" fill="none" stroke="url(#lineStroke)" stroke-width="2" stroke-linecap="round" pathLength="1" class="chart-line" />
    <path d="${readPath}" fill="none" stroke="${MINT}" stroke-width="1.6" stroke-linecap="round" opacity=".85" pathLength="1" class="chart-line" style="animation-delay: .18s" />
    <circle cx="${xAt(n - 1)}" cy="${yAt(THROUGHPUT.commands[n - 1])}" r="3" fill="${ACCENT}" stroke="var(--surface)" stroke-width="2" />
    <line x1="${padL}" y1="${H - padB}" x2="${W - padR}" y2="${H - padB}" stroke="var(--border-2)" />
    ${labels}
  </svg>`;
}

function renderSpark(name, values, color) {
  const mount = document.querySelector(`[data-spark="${name}"]`);
  if (!mount) return;
  const W = 120;
  const H = 48;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const pts = values.map((v, i) => [(i * W) / (values.length - 1), H - 6 - ((v - min) / span) * (H - 14)]);
  const line = smoothPath(pts);
  const area = `${line} L${W},${H} L0,${H} Z`;
  mount.innerHTML = `<defs><linearGradient id="spark-${name}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0%" stop-color="${color}" stop-opacity=".26"/>
      <stop offset="100%" stop-color="${color}" stop-opacity="0"/>
    </linearGradient></defs>
    <path d="${area}" fill="url(#spark-${name})"/>
    <path d="${line}" fill="none" stroke="${color}" stroke-width="1.4" stroke-linecap="round" opacity=".8" pathLength="1" class="chart-line"/>`;
}

function renderCharts() {
  ACCENT = accentColor();
  renderThroughput();
  renderSpark('memory', SPARKS.memory, ACCENT);
  renderSpark('ops', SPARKS.ops, MINT);
  renderSpark('hit', SPARKS.hit, SKY);
  renderSpark('clients', SPARKS.clients, AMBER);
}

document.getElementById('refreshBtn')?.addEventListener('click', () => {
  window.addEventListener('rc:theme', () => renderCharts());
window.addEventListener('rc:accent', () => renderCharts());

renderCharts();
  rc.toast('指标已刷新 · INFO 同步完成');
});

document.getElementById('refreshInsights')?.addEventListener('click', () => {
  window.addEventListener('rc:theme', () => renderCharts());
window.addEventListener('rc:accent', () => renderCharts());

renderCharts();
  rc.toast('指标已刷新');
});

document.querySelectorAll('[data-href]').forEach((el) => {
  el.addEventListener('click', () => {
    window.location.href = el.dataset.href;
  });
});

window.addEventListener('rc:theme', () => renderCharts());
window.addEventListener('rc:accent', () => renderCharts());

renderCharts();
