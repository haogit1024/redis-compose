/* ==========================================================================
   Workspace preview — key data, filtering and value rendering
   ========================================================================== */

const KEY_DATA = {
  'app:config:feature_flags': {
    type: 'string',
    ttl: -1,
    mem: '2.4 KB',
    enc: 'embstr',
    updated: '2 分钟前',
    value: {
      checkout_v2: true,
      one_click_pay: true,
      smart_cart: false,
      recommendations: {
        enabled: true,
        model: 'ranker-v3',
        fallback: 'popularity',
      },
      rate_limit: {
        window_seconds: 60,
        burst: 120,
      },
      updated_by: 'ops@acme.io',
      updated_at: '2026-11-18T07:42:11Z',
    },
  },
  'app:config:rate_limits': {
    type: 'string',
    ttl: -1,
    mem: '1.1 KB',
    enc: 'raw',
    updated: '1 小时前',
    value: {
      default: { window_seconds: 60, burst: 120 },
      auth: { window_seconds: 300, burst: 20 },
      search: { window_seconds: 10, burst: 40 },
      webhook: { window_seconds: 1, burst: 8 },
    },
  },
  'app:session:8f2a9c': {
    type: 'hash',
    ttl: 1680,
    mem: '1.2 KB',
    enc: 'listpack',
    updated: '4 分钟前',
    rows: [
      ['user_id', 'usr_8f2a9c4d'],
      ['email', 'ada@acme.io'],
      ['ip', '203.0.113.42'],
      ['device', 'MacBook Pro · Safari 19'],
      ['region', 'ap-east-1'],
      ['created_at', '2026-11-18T07:14:08Z'],
      ['last_seen', '2026-11-18T07:41:52Z'],
      ['mfa', 'true'],
    ],
  },
  'app:session:1c9de4': {
    type: 'hash',
    ttl: 7200,
    mem: '980 B',
    enc: 'listpack',
    updated: '18 分钟前',
    rows: [
      ['user_id', 'usr_1c9de4aa'],
      ['email', 'lin@acme.io'],
      ['ip', '198.51.100.24'],
      ['device', 'iPhone 17 · Safari'],
      ['region', 'ap-east-1'],
      ['created_at', '2026-11-18T06:02:31Z'],
      ['last_seen', '2026-11-18T07:28:09Z'],
      ['mfa', 'true'],
    ],
  },
  'cache:user:1001:profile': {
    type: 'hash',
    ttl: 3600,
    mem: '1.4 KB',
    enc: 'listpack',
    updated: '6 分钟前',
    rows: [
      ['name', 'Ada Lovelace'],
      ['email', 'ada@acme.io'],
      ['plan', 'scale'],
      ['company', 'Analytical Engines Ltd.'],
      ['seats', '24'],
      ['region', 'ap-east-1'],
      ['mfa_enabled', 'true'],
      ['last_login', '2026-11-18T07:37:19Z'],
    ],
  },
  'cache:user:1001:settings': {
    type: 'string',
    ttl: 3600,
    mem: '1.1 KB',
    enc: 'embstr',
    updated: '6 分钟前',
    value: {
      theme: 'dark',
      locale: 'zh-CN',
      timezone: 'Asia/Shanghai',
      notifications: {
        email: true,
        push: false,
        weekly_digest: true,
      },
      sidebar: {
        collapsed: false,
        width: 258,
      },
      shortcuts: {
        command_palette: 'meta+k',
        search: 'meta+f',
      },
    },
  },
  'cache:user:2048:profile': {
    type: 'hash',
    ttl: 3600,
    mem: '1.2 KB',
    enc: 'listpack',
    updated: '12 分钟前',
    rows: [
      ['name', 'Lin Chen'],
      ['email', 'lin@acme.io'],
      ['plan', 'starter'],
      ['company', 'Northwind Studio'],
      ['seats', '4'],
      ['region', 'ap-east-1'],
      ['mfa_enabled', 'false'],
      ['last_login', '2026-11-18T07:16:44Z'],
    ],
  },
  'cache:product:8842': {
    type: 'string',
    ttl: 600,
    mem: '1.8 KB',
    enc: 'embstr',
    updated: '刚刚',
    value: {
      id: 8842,
      sku: 'KB-K2-PRO-BLK',
      title: 'Mechanical Keyboard K2 Pro',
      price: 899,
      currency: 'CNY',
      stock: 42,
      status: 'active',
      tags: ['keyboard', 'wireless', 'hot-swap'],
      variants: [
        { id: 1, name: 'Carbon Black', stock: 24 },
        { id: 2, name: 'Ivory', stock: 18 },
      ],
    },
  },
  'cache:product:9103': {
    type: 'string',
    ttl: 600,
    mem: '1.6 KB',
    enc: 'embstr',
    updated: '3 分钟前',
    value: {
      id: 9103,
      sku: 'HUB-7IN1-SLV',
      title: 'USB-C Hub 7-in-1',
      price: 329,
      currency: 'CNY',
      stock: 0,
      status: 'out_of_stock',
      tags: ['usb-c', 'hub', 'accessory'],
      restock_eta: '2026-11-26',
    },
  },
  'cache:search:popular': {
    type: 'zset',
    ttl: 1800,
    mem: '3.2 KB',
    enc: 'skiplist',
    updated: '2 分钟前',
    rows: [
      ['mechanical keyboard', 982],
      ['usb-c hub', 871],
      ['desk mat', 644],
      ['webcam 4k', 528],
      ['monitor arm', 411],
      ['usb-c cable', 388],
      ['laptop stand', 321],
      ['noise canceling headphones', 287],
    ],
  },
  'queue:emails': {
    type: 'list',
    ttl: -1,
    mem: '84.6 KB',
    enc: 'quicklist',
    updated: '刚刚',
    rows: [
      '{"to":"ada@acme.io","template":"welcome","locale":"zh-CN"}',
      '{"to":"lin@acme.io","template":"invoice_ready","invoice":"INV-2026-1187"}',
      '{"to":"ops@acme.io","template":"daily_digest","date":"2026-11-18"}',
      '{"to":"ada@acme.io","template":"password_changed","ip":"203.0.113.42"}',
      '{"to":"team@acme.io","template":"weekly_report","week":46}',
      '{"to":"lin@acme.io","template":"usage_warning","percent":82}',
    ],
  },
  'queue:jobs:priority': {
    type: 'zset',
    ttl: -1,
    mem: '12.4 KB',
    enc: 'skiplist',
    updated: '1 分钟前',
    rows: [
      ['job:reindex:products', 98.5],
      ['job:sync:stripe', 94.2],
      ['job:export:invoices', 87.0],
      ['job:thumbnail:8842', 76.5],
      ['job:digest:emails', 61.3],
      ['job:cleanup:sessions', 40.0],
      ['job:warm:cache', 18.8],
    ],
  },
  'queue:dead_letter': {
    type: 'list',
    ttl: -1,
    mem: '6.2 KB',
    enc: 'quicklist',
    updated: '22 分钟前',
    rows: [
      '{"job":"job:sync:stripe","error":"timeout after 5000ms","attempts":5}',
      '{"job":"job:export:invoices","error":"S3 access denied","attempts":3}',
      '{"job":"job:thumbnail:9103","error":"source not found","attempts":1}',
    ],
  },
  'billing:invoice:INV-2026-1187': {
    type: 'hash',
    ttl: -1,
    mem: '1.9 KB',
    enc: 'hashtable',
    updated: '32 分钟前',
    rows: [
      ['customer', 'cus_ada_1001'],
      ['amount', '12800.00'],
      ['currency', 'CNY'],
      ['status', 'paid'],
      ['issued_at', '2026-11-18T06:55:02Z'],
      ['paid_at', '2026-11-18T07:04:31Z'],
      ['method', 'alipay'],
      ['items', '[{"sku":"KB-K2-PRO-BLK","qty":4,"price":899}]'],
    ],
  },
  'billing:ledger:november': {
    type: 'string',
    ttl: -1,
    mem: '8.4 KB',
    enc: 'raw',
    updated: '1 小时前',
    value: {
      month: '2026-11',
      revenue: 428900.0,
      refunds: 12480.5,
      net: 416419.5,
      currency: 'CNY',
      entries: 1842,
      reconciled: true,
    },
  },
  'metrics:daily:2026-11-17': {
    type: 'hash',
    ttl: 86400,
    mem: '640 B',
    enc: 'listpack',
    updated: '6 小时前',
    rows: [
      ['requests', '1842904'],
      ['errors', '318'],
      ['avg_latency_ms', '11.84'],
      ['p95_latency_ms', '42.6'],
      ['cache_hit_rate', '0.987'],
      ['peak_qps', '4820'],
      ['unique_users', '32714'],
    ],
  },
  'metrics:histogram:latency': {
    type: 'zset',
    ttl: 300,
    mem: '2.8 KB',
    enc: 'skiplist',
    updated: '刚刚',
    rows: [
      ['1ms', 18420],
      ['5ms', 12388],
      ['10ms', 8214],
      ['25ms', 4102],
      ['50ms', 1260],
      ['100ms', 328],
      ['250ms', 64],
    ],
  },
  'stream:events:audit': {
    type: 'stream',
    ttl: -1,
    mem: '42.8 KB',
    enc: 'stream',
    updated: '刚刚',
    entries: [
      { id: '1731906012345-0', event: 'auth.login', actor: 'usr_8f2a9c4d', ip: '203.0.113.42', time: '07:41:52' },
      { id: '1731906009121-0', event: 'key.updated', actor: 'ops@acme.io', ip: '10.0.4.18', time: '07:41:49' },
      { id: '1731905984410-0', event: 'invoice.paid', actor: 'system', ip: '—', time: '07:39:44' },
      { id: '1731905902278-0', event: 'auth.logout', actor: 'usr_1c9de4aa', ip: '198.51.100.24', time: '07:38:22' },
      { id: '1731905841003-0', event: 'cache.evicted', actor: 'redis', ip: '—', time: '07:37:21' },
      { id: '1731905770994-0', event: 'feature.flag.updated', actor: 'czh@acme.io', ip: '10.0.4.6', time: '07:36:10' },
    ],
  },
  'tags:article:204': {
    type: 'set',
    ttl: -1,
    mem: '184 B',
    enc: 'listpack',
    updated: '3 天前',
    rows: ['redis', 'kotlin', 'compose-multiplatform', 'desktop', 'tutorial'],
  },
  'locks:checkout:order-9182': {
    type: 'string',
    ttl: 24,
    mem: '88 B',
    enc: 'embstr',
    updated: '刚刚',
    value: 'owner:worker-7|token:6f1c2a|acquired_at:2026-11-18T07:42:03Z',
  },
  'temp:otp:13800001111': {
    type: 'string',
    ttl: 42,
    mem: '72 B',
    enc: 'embstr',
    updated: '18 秒前',
    value: '482913',
  },
  'feature:rollout:checkout_v2': {
    type: 'string',
    ttl: -1,
    mem: '36 B',
    enc: 'embstr',
    updated: '5 小时前',
    value: '10',
  },
};

const DEFAULT_KEY = 'app:config:feature_flags';
const pinnedKeys = new Set(['app:config:feature_flags', 'queue:jobs:priority', 'stream:events:audit']);

let currentKey = DEFAULT_KEY;
let currentFormat = 'auto';
let activeType = 'all';
let searchQuery = '';

const valuePane = document.getElementById('valuePane');
const keyTree = document.getElementById('keyTree');
const treeStatic = document.getElementById('treeStatic');
const searchResults = document.getElementById('searchResults');
const keySearch = document.getElementById('keySearch');

/* ------------------------------------------------------------------ helpers */

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function escapeAttr(str) {
  return escapeHtml(str).replace(/'/g, '&#39;');
}

function fmtBytes(n) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

function byteLength(str) {
  return new TextEncoder().encode(str).length;
}

function fmtTtl(sec) {
  if (sec < 0) return '永久';
  if (sec < 60) return `${sec}s`;
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  if (m < 60) return s ? `${m}m ${s}s` : `${m}m`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ${m % 60}m`;
  const d = Math.floor(h / 24);
  return `${d}d ${h % 24}h`;
}

function valueTypeLabel(type) {
  return (window.rc && rc.typeLabel[type]) || type.toUpperCase();
}

function highlightJson(src) {
  const escaped = String(src)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  return escaped.replace(
    /("(?:\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*")(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,
    (match, str, colon, bool) => {
      if (str) {
        const cls = colon ? 'tok-key' : 'tok-str';
        return `<span class="${cls}">${str}</span>${colon ? `<span class="tok-punct">${colon}</span>` : ''}`;
      }
      if (bool) return `<span class="tok-bool">${match}</span>`;
      return `<span class="tok-num">${match}</span>`;
    },
  );
}

function codeView(text, highlight = false) {
  const lines = text.split('\n');
  const gutter = lines.map((_, i) => `<span>${i + 1}</span>`).join('');
  return `<div class="code-wrap"><div class="code-gutter">${gutter}</div><div class="code-body">${
    highlight ? highlightJson(text) : escapeHtml(text)
  }</div></div>`;
}

function hexView(text) {
  const bytes = new TextEncoder().encode(text);
  const lines = [];
  const limit = Math.min(bytes.length, 384);
  for (let i = 0; i < limit; i += 16) {
    const slice = Array.from(bytes.slice(i, i + 16));
    const hex = slice
      .map((b) => b.toString(16).padStart(2, '0'))
      .join(' ')
      .padEnd(47, ' ');
    const ascii = slice.map((b) => (b >= 32 && b < 127 ? String.fromCharCode(b) : '.')).join('');
    lines.push(`${i.toString(16).padStart(4, '0')}   ${hex}   ${ascii}`);
  }
  if (bytes.length > limit) lines.push('…');
  return lines.join('\n');
}

function keyValueRows(data) {
  if (data.type === 'hash') return data.rows.length;
  if (data.type === 'list' || data.type === 'set' || data.type === 'zset') return data.rows.length;
  if (data.type === 'stream') return data.entries.length;
  return 0;
}

function jsonText(data) {
  return typeof data.value === 'object' && data.value !== null ? JSON.stringify(data.value, null, 2) : String(data.value);
}

function isJsonKey(data) {
  return data.type === 'string' && typeof data.value === 'object' && data.value !== null;
}

/* --------------------------------------------------------------- value view */

function metaBox(label, icon, value, sub, extra = '') {
  return `<div class="meta-box">
    <div class="meta-label"><span class="ic">${rc.svgFor(icon)}</span>${label}</div>
    <div class="meta-value">${value}</div>
    <div class="meta-sub">${sub}</div>
    ${extra}
  </div>`;
}

function headerActions(key, pinned) {
  return `<div class="key-actions" style="position: relative">
    <button class="btn-icon ${pinned ? 'starred' : ''}" data-key-action="pin" data-tip="${pinned ? '取消置顶' : '置顶 Key'}" aria-label="置顶">
      <span class="ic">${rc.svgFor('star')}</span>
    </button>
    <button class="btn-icon" data-key-action="copy" data-tip="复制 Key" aria-label="复制 Key">
      <span class="ic">${rc.svgFor('copy')}</span>
    </button>
    <a class="btn-icon" href="console.html" data-tip="在控制台打开" aria-label="在控制台打开">
      <span class="ic">${rc.svgFor('terminal')}</span>
    </a>
    <button class="btn-icon" data-key-action="more" data-tip="更多" aria-label="更多操作">
      <span class="ic">${rc.svgFor('more')}</span>
    </button>
  </div>`;
}

function stringEditor(key, data) {
  const json = isJsonKey(data);
  const raw = jsonText(data);
  let content = raw;
  let highlighted = false;
  if (currentFormat === 'auto' || currentFormat === 'json') {
    content = json ? JSON.stringify(data.value, null, 2) : raw;
    highlighted = json;
  } else if (currentFormat === 'hex') {
    content = hexView(raw);
    highlighted = false;
  } else {
    content = json ? JSON.stringify(data.value) : raw;
    highlighted = false;
  }
  const bytes = byteLength(content);

  return `<div class="editor-top">
      <span class="editor-title"><span class="ic">${rc.svgFor('code')}</span>Value</span>
      ${json ? '<span class="editor-detected">JSON detected</span>' : ''}
      <div class="editor-actions">
        <div class="segmented" data-seg="format">
          ${['auto', 'json', 'text', 'hex']
            .map(
              (f) =>
                `<button data-format="${f}" class="${currentFormat === f ? 'active' : ''}">${
                  f === 'auto' ? 'Auto' : f.toUpperCase()
                }</button>`,
            )
            .join('')}
        </div>
        ${json ? `<button class="btn ghost sm" data-act="pretty"><span class="ic">${rc.svgFor('sparkles')}</span>格式化</button>` : ''}
        <button class="btn primary sm" data-act="save"><span class="ic">${rc.svgFor('save')}</span>保存</button>
      </div>
    </div>
    ${codeView(content, highlighted)}
    <div class="code-footer">
      <span>UTF-8</span><span>${fmtBytes(bytes)}</span><span>LF</span>
      <span class="saved"><span class="ic">${rc.svgFor('check')}</span>已同步 · ${data.updated}</span>
    </div>`;
}

function rowActions(copyValue, label) {
  return `<div class="row-actions">
    <button class="btn-icon" data-copy="${escapeAttr(copyValue)}" data-tip="复制" aria-label="复制${label}">
      <span class="ic">${rc.svgFor('copy')}</span>
    </button>
    <button class="btn-icon" data-row-action="edit" data-tip="编辑" aria-label="编辑${label}">
      <span class="ic">${rc.svgFor('pencil')}</span>
    </button>
  </div>`;
}

function hashEditor(data) {
  const rows = data.rows
    .map(
      ([field, value]) => `<tr>
        <td class="field">${escapeHtml(field)}</td>
        <td>${escapeHtml(value)}</td>
        <td class="actions-col">${rowActions(value, '字段值')}</td>
      </tr>`,
    )
    .join('');
  return `<div class="collection-toolbar">
      <span class="editor-title"><span class="ic">${rc.svgFor('hash')}</span>Hash fields</span>
      <span class="badge plain">${data.rows.length} fields</span>
      <div class="search"><span class="ic">${rc.svgFor('search')}</span><input class="input" data-collection-search placeholder="筛选字段…" /></div>
      <div class="right">
        <button class="btn subtle sm" data-act="add"><span class="ic">${rc.svgFor('plus')}</span>新增字段</button>
        <button class="btn primary sm" data-act="save"><span class="ic">${rc.svgFor('save')}</span>保存</button>
      </div>
    </div>
    <table class="collection-table">
      <thead><tr><th style="width: 32%">Field</th><th>Value</th><th class="actions-col"></th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="code-footer"><span>HSET · HDEL</span><span>${data.rows.length} fields</span><span class="saved"><span class="ic">${rc.svgFor('check')}</span>已同步 · ${data.updated}</span></div>`;
}

function listEditor(data) {
  const rows = data.rows
    .map(
      (value, i) => `<tr>
        <td class="index">${i}</td>
        <td>${escapeHtml(value)}</td>
        <td class="actions-col">${rowActions(value, '成员')}</td>
      </tr>`,
    )
    .join('');
  return `<div class="collection-toolbar">
      <span class="editor-title"><span class="ic">${rc.svgFor('list')}</span>List members</span>
      <span class="badge plain">${data.rows.length} items</span>
      <div class="search"><span class="ic">${rc.svgFor('search')}</span><input class="input" data-collection-search placeholder="筛选成员…" /></div>
      <div class="right">
        <button class="btn subtle sm" data-act="lpush"><span class="ic">${rc.svgFor('arrow-left')}</span>LPUSH</button>
        <button class="btn subtle sm" data-act="rpush"><span class="ic">${rc.svgFor('arrow-up-right')}</span>RPUSH</button>
        <button class="btn primary sm" data-act="save"><span class="ic">${rc.svgFor('save')}</span>保存</button>
      </div>
    </div>
    <table class="collection-table">
      <thead><tr><th class="index">Index</th><th>Value</th><th class="actions-col"></th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="code-footer"><span>LPUSH · RPUSH · LSET</span><span>${data.rows.length} items</span><span class="saved"><span class="ic">${rc.svgFor('check')}</span>已同步 · ${data.updated}</span></div>`;
}

function setEditor(data) {
  const rows = data.rows
    .map(
      (value) => `<tr>
        <td class="member">${escapeHtml(value)}</td>
        <td class="actions-col">${rowActions(value, '成员')}</td>
      </tr>`,
    )
    .join('');
  return `<div class="collection-toolbar">
      <span class="editor-title"><span class="ic">${rc.svgFor('layers')}</span>Set members</span>
      <span class="badge plain">${data.rows.length} unique</span>
      <div class="search"><span class="ic">${rc.svgFor('search')}</span><input class="input" data-collection-search placeholder="筛选成员…" /></div>
      <div class="right">
        <button class="btn subtle sm" data-act="add"><span class="ic">${rc.svgFor('plus')}</span>添加成员</button>
        <button class="btn primary sm" data-act="save"><span class="ic">${rc.svgFor('save')}</span>保存</button>
      </div>
    </div>
    <table class="collection-table">
      <thead><tr><th>Member</th><th class="actions-col"></th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="code-footer"><span>SADD · SREM</span><span>${data.rows.length} members</span><span class="saved"><span class="ic">${rc.svgFor('check')}</span>已同步 · ${data.updated}</span></div>`;
}

function zsetEditor(data) {
  const max = Math.max(...data.rows.map((r) => Number(r[1])));
  const rows = data.rows
    .map(
      ([member, score], i) => `<tr>
        <td class="rank">${i + 1}</td>
        <td class="member">${escapeHtml(member)}</td>
        <td class="score"><span class="score-bar" style="width: ${Math.max(8, (score / max) * 44)}px"></span>${Number(
          score,
        ).toFixed(score % 1 ? 1 : 0)}</td>
        <td class="actions-col">${rowActions(String(score), 'score')}</td>
      </tr>`,
    )
    .join('');
  return `<div class="collection-toolbar">
      <span class="editor-title"><span class="ic">${rc.svgFor('sort')}</span>Sorted set</span>
      <span class="badge plain">${data.rows.length} members</span>
      <div class="search"><span class="ic">${rc.svgFor('search')}</span><input class="input" data-collection-search placeholder="筛选成员…" /></div>
      <div class="right">
        <button class="btn subtle sm" data-act="add"><span class="ic">${rc.svgFor('plus')}</span>添加成员</button>
        <button class="btn primary sm" data-act="save"><span class="ic">${rc.svgFor('save')}</span>保存</button>
      </div>
    </div>
    <table class="collection-table">
      <thead><tr><th class="rank">#</th><th>Member</th><th class="score">Score</th><th class="actions-col"></th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="code-footer"><span>ZADD · ZREM</span><span>从高到低排序</span><span class="saved"><span class="ic">${rc.svgFor('check')}</span>已同步 · ${data.updated}</span></div>`;
}

function streamEditor(data) {
  const rows = data.entries
    .map(
      (entry) => `<tr>
        <td class="field">${escapeHtml(entry.id)}</td>
        <td><span class="badge accent" style="font-family: var(--mono)">${escapeHtml(entry.event)}</span></td>
        <td>${escapeHtml(entry.actor)}</td>
        <td>${escapeHtml(entry.ip)}</td>
        <td class="score">${entry.time}</td>
        <td class="actions-col">${rowActions(entry.id, '事件 ID')}</td>
      </tr>`,
    )
    .join('');
  return `<div class="collection-toolbar">
      <span class="editor-title"><span class="ic">${rc.svgFor('activity')}</span>Stream entries</span>
      <span class="badge plain">${data.entries.length} shown · 2 groups</span>
      <div class="search"><span class="ic">${rc.svgFor('search')}</span><input class="input" data-collection-search placeholder="筛选事件…" /></div>
      <div class="right">
        <button class="btn subtle sm" data-act="add"><span class="ic">${rc.svgFor('plus')}</span>新增条目</button>
        <button class="btn subtle sm"><span class="ic">${rc.svgFor('users')}</span>消费者组</button>
      </div>
    </div>
    <table class="collection-table">
      <thead><tr><th style="width: 170px">Entry ID</th><th>Event</th><th>Actor</th><th>IP</th><th class="score">Time</th><th class="actions-col"></th></tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="code-footer"><span>XADD · XREAD</span><span>仅展示最近 6 条</span><span class="saved"><span class="ic">${rc.svgFor('check')}</span>Live · ${data.updated}</span></div>`;
}

function renderValue(key) {
  const data = KEY_DATA[key];
  if (!data) return;
  const pinned = pinnedKeys.has(key);
  const type = data.type;
  const isJson = isJsonKey(data);

  const ttlText = fmtTtl(data.ttl);
  const ttlTrack =
    data.ttl > 0
      ? `<div class="ttl-track"><i style="width: ${Math.min(100, Math.max(6, (data.ttl / 3600) * 100))}%"></i></div>`
      : '';

  let extraMeta;
  if (type === 'string') {
    const txt = jsonText(data);
    extraMeta = ['VALUE SIZE', fmtBytes(byteLength(txt)), `${txt.length} characters`];
  } else if (type === 'stream') {
    extraMeta = ['ENTRIES', `${data.entries.length}`, 'XLEN'];
  } else if (type === 'hash') {
    extraMeta = ['FIELDS', `${data.rows.length}`, 'HLEN'];
  } else if (type === 'list') {
    extraMeta = ['LENGTH', `${data.rows.length}`, 'LLEN'];
  } else if (type === 'zset') {
    extraMeta = ['MEMBERS', `${data.rows.length}`, 'ZCARD'];
  } else {
    extraMeta = ['MEMBERS', `${data.rows.length}`, 'SCARD'];
  }

  const editor =
    type === 'string'
      ? stringEditor(key, data)
      : type === 'hash'
        ? hashEditor(data)
        : type === 'list'
          ? listEditor(data)
          : type === 'set'
            ? setEditor(data)
            : type === 'zset'
              ? zsetEditor(data)
              : type === 'stream'
                ? streamEditor(data)
                : '<div class="collection-empty">暂不支持预览该类型</div>';

  const tagBadges = [isJson ? '<span class="badge accent">JSON</span>' : ''];
  if (pinned) tagBadges.push(`<span class="badge amber"><span class="ic">${rc.svgFor('star')}</span>已置顶</span>`);
  if (data.ttl > 0 && data.ttl <= 60)
    tagBadges.push(`<span class="badge danger"><span class="ic">${rc.svgFor('clock')}</span>即将过期</span>`);
  if (key.startsWith('locks:'))
    tagBadges.push(`<span class="badge plain"><span class="ic">${rc.svgFor('lock')}</span>Lock</span>`);

  valuePane.innerHTML = `<div class="value-scroll">
    <div class="value-inner">
      <div class="key-header">
        <div class="key-identity">
          <div class="key-tagline">
            <span class="type-pill" data-type="${type}">${valueTypeLabel(type)}</span>
            ${tagBadges.join('')}
          </div>
          <h1 class="key-title">${escapeHtml(key)}</h1>
          <div class="key-sub">
            <span>db0</span><span class="sep"></span>
            <span>redis-01.acme.io:6379</span><span class="sep"></span>
            <span>OBJECT ENCODING ${escapeHtml(data.enc)}</span><span class="sep"></span>
            <span>更新于 ${data.updated}</span>
          </div>
        </div>
        ${headerActions(key, pinned)}
      </div>

      <div class="meta-grid">
        ${metaBox('TTL', 'clock', ttlText, data.ttl > 0 ? '剩余生存时间' : '无过期时间', ttlTrack)}
        ${metaBox('MEMORY', 'cpu', data.mem, 'MEMORY USAGE')}
        ${metaBox('ENCODING', 'code', data.enc, 'OBJECT ENCODING')}
        ${metaBox(extraMeta[0], 'layers', extraMeta[1], extraMeta[2])}
      </div>

      <div class="editor-card ${type === 'string' ? 'fill' : ''}">${editor}</div>

      <div class="editor-footnote">
        <span class="ic">${rc.svgFor('info')}</span>
        <span><kbd>⌘S</kbd> 保存修改</span>
        <span>·</span>
        <span><kbd>⌘F</kbd> 搜索 Key</span>
        <span>·</span>
        <span>所有写入操作都会记录在当前会话中</span>
      </div>
    </div>
  </div>`;
  rc.mountIcons(valuePane);
}

/* ------------------------------------------------------------------- selection */

function syncSelected() {
  document.querySelectorAll('.key-row').forEach((row) => {
    row.classList.toggle('selected', row.dataset.key === currentKey);
  });
}

function renderPinned() {
  const mount = document.getElementById('pinnedList');
  if (!mount) return;
  mount.innerHTML = [...pinnedKeys]
    .map((key) => {
      const data = KEY_DATA[key];
      if (!data) return '';
      return `<div class="key-row pinned" data-key="${escapeAttr(key)}" data-type="${data.type}">
        <i class="type-dot"></i><span class="key-name">${escapeHtml(key)}</span>
        <span class="ic pin">${rc.svgFor('star')}</span>
      </div>`;
    })
    .join('');
  syncSelected();
}

function selectKey(key) {
  if (!KEY_DATA[key]) return;
  currentKey = key;
  currentFormat = 'auto';
  syncSelected();
  renderValue(key);
  const scroller = valuePane.querySelector('.value-scroll');
  if (scroller) scroller.scrollTop = 0;
}

/* -------------------------------------------------------------------- filtering */

function patternMatches(key, pattern) {
  if (!pattern) return true;
  const q = pattern.trim();
  if (!q) return true;
  if (q.includes('*') || q.includes('?')) {
    const re = new RegExp(
      q
        .split('')
        .map((c) => (c === '*' ? '.*' : c === '?' ? '.' : c.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
        .join(''),
      'i',
    );
    return re.test(key);
  }
  return key.toLowerCase().includes(q.toLowerCase());
}

function renderFiltered() {
  const q = searchQuery.trim();
  if (!q && activeType === 'all') {
    treeStatic.hidden = false;
    searchResults.hidden = true;
    return;
  }
  treeStatic.hidden = true;
  searchResults.hidden = false;

  const matches = Object.entries(KEY_DATA).filter(
    ([key, data]) => (activeType === 'all' || data.type === activeType) && patternMatches(key, q),
  );

  let html = `<div class="tree-section">结果<span class="line"></span><span>${matches.length} keys</span></div>`;
  if (matches.length) {
    html += matches
      .map(
        ([key, data]) => `<div class="key-row" data-key="${escapeAttr(key)}" data-type="${data.type}">
          <i class="type-dot"></i>
          <span class="key-name">${escapeHtml(key)}</span>
          ${
            data.ttl > 0
              ? `<span class="key-ttl">${fmtTtl(data.ttl)}</span>`
              : pinnedKeys.has(key)
                ? `<span class="ic pin" data-ic="star" style="opacity:1"></span>`
                : ''
          }
        </div>`,
      )
      .join('');
    html += `<div class="tree-more"><span class="ic" data-ic="filter"></span>已筛选 ${matches.length} 条 · 清除筛选</div>`;
  } else {
    html += `<div class="tree-empty">没有匹配的 Key<br /><span class="mono" style="color: var(--text-4)">${escapeHtml(
      q || activeType.toUpperCase(),
    )}</span></div>`;
  }
  searchResults.innerHTML = html;
  rc.mountIcons(searchResults);
  syncSelected();
  searchResults.querySelectorAll('.tree-more').forEach((el) => {
    el.addEventListener('click', () => {
      keySearch.value = '';
      searchQuery = '';
      activeType = 'all';
      document.querySelectorAll('#typeFilter .chip').forEach((c) => c.classList.toggle('active', c.dataset.typeFilter === 'all'));
      renderFiltered();
    });
  });
}

/* ------------------------------------------------------------------- listeners */

document.querySelector('#typeFilter').addEventListener('click', (e) => {
  const chip = e.target.closest('.chip');
  if (!chip) return;
  activeType = chip.dataset.typeFilter;
  document.querySelectorAll('#typeFilter .chip').forEach((c) => c.classList.toggle('active', c === chip));
  renderFiltered();
});

keySearch.addEventListener('input', () => {
  searchQuery = keySearch.value;
  renderFiltered();
});

keyTree.addEventListener('click', (e) => {
  const folderLabel = e.target.closest('.folder-label');
  if (folderLabel) {
    const folder = folderLabel.closest('.folder');
    const open = folder.classList.toggle('open');
    const icon = folderLabel.querySelector('.ic:not(.chev)');
    if (icon) {
      icon.innerHTML = rc.svgFor(open ? 'folder-open' : 'folder');
    }
    return;
  }
  const row = e.target.closest('.key-row');
  if (row) selectKey(row.dataset.key);
});

document.querySelectorAll('[data-action="refresh"]').forEach((btn) => {
  btn.addEventListener('click', () => {
    renderValue(currentKey);
    rc.toast('Keyspace 已刷新 · 12,480 keys');
  });
});

document.querySelectorAll('[data-action="new-key"]').forEach((btn) => {
  btn.addEventListener('click', () => {
    rc.toast('新建 Key · 快捷键 N');
  });
});

document.addEventListener('keydown', (e) => {
  const meta = e.metaKey || e.ctrlKey;
  if (meta && e.key.toLowerCase() === 'f') {
    e.preventDefault();
    keySearch.focus();
    keySearch.select();
  }
  if (meta && e.key.toLowerCase() === 's') {
    e.preventDefault();
    rc.toast(`已保存 ${currentKey}`);
  }
  if (e.key === 'n' && !meta && document.activeElement === document.body) {
    document.querySelector('[data-action="new-key"]')?.click();
  }
});

/* value pane delegation */
valuePane.addEventListener('click', (e) => {
  const copyEl = e.target.closest('[data-copy]');
  if (copyEl) {
    e.preventDefault();
    rc.copyText(copyEl.dataset.copy);
    return;
  }

  const formatBtn = e.target.closest('[data-format]');
  if (formatBtn) {
    currentFormat = formatBtn.dataset.format;
    renderValue(currentKey);
    return;
  }

  const keyAction = e.target.closest('[data-key-action]');
  if (keyAction) {
    const action = keyAction.dataset.keyAction;
    if (action === 'pin') {
      if (pinnedKeys.has(currentKey)) {
        pinnedKeys.delete(currentKey);
        rc.toast('已取消置顶');
      } else {
        pinnedKeys.add(currentKey);
        rc.toast('已置顶到 Keys 面板');
      }
      document.querySelectorAll(`.key-row[data-key="${CSS.escape(currentKey)}"]`).forEach((row) => {
        row.classList.toggle('pinned', pinnedKeys.has(currentKey));
      });
      renderPinned();
      renderValue(currentKey);
    } else if (action === 'copy') {
      rc.copyText(currentKey);
    } else if (action === 'more') {
      toggleKeyMenu(keyAction);
    }
    return;
  }

  const menuItem = e.target.closest('[data-menu-action]');
  if (menuItem) {
    const action = menuItem.dataset.menuAction;
    document.querySelectorAll('.key-actions .menu').forEach((m) => m.remove());
    if (action === 'copy-command') {
      const cmd =
        KEY_DATA[currentKey].type === 'string'
          ? `GET ${currentKey}`
          : KEY_DATA[currentKey].type === 'hash'
            ? `HGETALL ${currentKey}`
            : KEY_DATA[currentKey].type === 'list'
              ? `LRANGE ${currentKey} 0 -1`
              : KEY_DATA[currentKey].type === 'set'
                ? `SMEMBERS ${currentKey}`
                : KEY_DATA[currentKey].type === 'zset'
                  ? `ZRANGE ${currentKey} 0 -1 WITHSCORES`
                  : `XRANGE ${currentKey} - +`;
      rc.copyText(cmd);
    } else if (action === 'ttl') {
      rc.toast(`TTL · ${fmtTtl(KEY_DATA[currentKey].ttl)}`, 'warn');
    } else if (action === 'delete') {
      rc.toast(`已删除 ${currentKey}（示例）`, 'error');
    } else if (action === 'console') {
      window.location.href = 'console.html';
    }
    return;
  }

  const act = e.target.closest('[data-act]')?.dataset.act;
  if (act) {
    const type = KEY_DATA[currentKey].type;
    if (act === 'save') {
      const btn = e.target.closest('[data-act="save"]');
      if (btn) {
        const original = btn.innerHTML;
        btn.innerHTML = `<span class="ic">${rc.svgFor('check')}</span>已保存`;
        setTimeout(() => {
          btn.innerHTML = original;
        }, 1400);
      }
      rc.toast(`已保存 ${currentKey}`);
    } else if (act === 'pretty') {
      currentFormat = 'json';
      renderValue(currentKey);
      rc.toast('已格式化 JSON');
    } else if (act === 'add') {
      const labels = { hash: '新字段', set: '新成员', zset: '新成员', stream: '新条目' };
      rc.toast(`新增${labels[type] || '条目'}（示例）`);
    } else if (act === 'lpush') {
      rc.toast('已从左侧插入空成员');
    } else if (act === 'rpush') {
      rc.toast('已从右侧插入空成员');
    }
  }

  if (e.target.closest('[data-row-action]')) {
    rc.toast('编辑模式（示例）');
  }

  if (!e.target.closest('.key-actions') && !e.target.closest('.menu')) {
    document.querySelectorAll('.key-actions .menu').forEach((m) => m.remove());
  }
});

valuePane.addEventListener('input', (e) => {
  if (e.target.matches('[data-collection-search]')) {
    const q = e.target.value.toLowerCase();
    const table = e.target.closest('.editor-card').querySelector('.collection-table');
    if (!table) return;
    table.querySelectorAll('tbody tr').forEach((tr) => {
      tr.style.display = tr.textContent.toLowerCase().includes(q) ? '' : 'none';
    });
  }
});

function toggleKeyMenu(anchor) {
  const existing = anchor.parentElement.querySelector('.menu');
  if (existing) {
    existing.remove();
    return;
  }
  const menu = document.createElement('div');
  menu.className = 'menu';
  menu.style.top = '34px';
  menu.style.right = '0';
  menu.innerHTML = `
    <div class="menu-item" data-menu-action="copy-command"><span class="ic">${rc.svgFor('terminal')}</span>复制为命令</div>
    <div class="menu-item" data-menu-action="ttl"><span class="ic">${rc.svgFor('clock')}</span>查看 / 修改 TTL</div>
    <div class="menu-item" data-menu-action="console"><span class="ic">${rc.svgFor('arrow-up-right')}</span>在控制台中打开</div>
    <div class="menu-sep"></div>
    <div class="menu-item danger" data-menu-action="delete"><span class="ic">${rc.svgFor('trash')}</span>删除 Key</div>`;
  anchor.parentElement.appendChild(menu);
  setTimeout(() => {
    const close = (ev) => {
      if (!ev.target.closest('.key-actions')) {
        menu.remove();
        document.removeEventListener('click', close);
      }
    };
    document.addEventListener('click', close);
  }, 0);
}

/* sidebar connection filter */
const connFilter = document.getElementById('connFilter');
connFilter?.addEventListener('input', () => {
  const q = connFilter.value.toLowerCase();
  document.querySelectorAll('.sidebar .conn').forEach((conn) => {
    conn.style.display = conn.textContent.toLowerCase().includes(q) ? '' : 'none';
  });
});

/* ------------------------------------------------------------------- boot */

rc.mountIcons();
renderPinned();
const bootKey = new URLSearchParams(location.search).get('key');
selectKey(bootKey && KEY_DATA[bootKey] ? bootKey : DEFAULT_KEY);
