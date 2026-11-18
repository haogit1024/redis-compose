/* ==========================================================================
   Console preview — small interactive shell
   ========================================================================== */

const termBody = document.getElementById('termBody');
const input = document.getElementById('consoleInput');
const suggestions = document.getElementById('suggestions');
const runBtn = document.getElementById('runCmd');

const COMMANDS = [
  { cmd: 'HGETALL', syntax: 'key' },
  { cmd: 'HSCAN', syntax: 'key cursor [MATCH pattern]' },
  { cmd: 'INFO', syntax: '[section]' },
  { cmd: 'TTL', syntax: 'key' },
  { cmd: 'SCAN', syntax: 'cursor [MATCH pattern] [COUNT n]' },
  { cmd: 'MEMORY USAGE', syntax: 'key [SAMPLES n]' },
  { cmd: 'SLOWLOG GET', syntax: '[count]' },
  { cmd: 'DBSIZE', syntax: '' },
  { cmd: 'XINFO GROUPS', syntax: 'key' },
  { cmd: 'CLIENT LIST', syntax: '[TYPE normal]' },
  { cmd: 'GET', syntax: 'key' },
  { cmd: 'SET', syntax: 'key value [EX seconds]' },
];

let historyIndex = -1;
const history = [
  'HGETALL cache:user:1001:profile',
  'TTL cache:user:1001:profile',
  'SCAN 0 MATCH queue:* COUNT 100',
  'MEMORY USAGE cache:user:1001:profile SAMPLES 5',
  'OBJECT ENCODING cache:user:1001:profile',
  'INFO keyspace',
  'SLOWLOG GET 10',
];

function now() {
  return new Date().toLocaleTimeString('en-GB', { hour12: false });
}

function esc(value) {
  return String(value).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function caretBlock() {
  return `<div class="t-cmd"><span class="t-time">${now()}</span><span class="t-prompt">redis@prod-cache<span class="host">:6379[0]&gt;</span></span><span class="t-input"><span class="cursor-blink"> </span></span></div>`;
}

function scrollBottom() {
  termBody.scrollTop = termBody.scrollHeight;
}

function removeCaret() {
  termBody.querySelectorAll('.cursor-blink').forEach((el) => el.closest('.t-cmd').remove());
}

function appendRaw(html) {
  termBody.insertAdjacentHTML('beforeend', html);
}

function resultFor(command) {
  const upper = command.trim().toUpperCase();
  if (!command.trim()) return '';
  if (upper === 'PING') return '<div class="t-out t-ok">PONG</div>';
  if (upper === 'DBSIZE') return '<div class="t-out t-ok">(integer) 12480</div>';
  if (upper.startsWith('INFO MEMORY')) {
    return `<div class="t-out"># Memory</div>
      <div class="t-out">used_memory:1334234112</div>
      <div class="t-out">used_memory_human:1.24G</div>
      <div class="t-out">used_memory_peak:1588510720</div>
      <div class="t-out">used_memory_rss:1476395008</div>
      <div class="t-out">mem_fragmentation_ratio:1.11</div>
      <div class="t-out t-ok">maxmemory_policy:allkeys-lru</div>`;
  }
  if (upper.startsWith('INFO KEYSPACE')) {
    return `<div class="t-out"># Keyspace</div>
      <div class="t-out">db0:keys=12480,expires=3218,avg_ttl=284311</div>
      <div class="t-out">db1:keys=8201,expires=1840,avg_ttl=190224</div>
      <div class="t-out">db2:keys=3904,expires=602,avg_ttl=88014</div>`;
  }
  if (upper.startsWith('TTL ')) return '<div class="t-out t-ok">(integer) 3271</div>';
  if (upper.startsWith('SCAN ')) {
    return `<div class="t-out">1) <span class="t-dim">"0"</span></div>
      <div class="t-out">2) 1) <span class="t-ok">"queue:emails"</span></div>
      <div class="t-out" style="padding-left:4ch">2) <span class="t-ok">"queue:jobs:priority"</span></div>
      <div class="t-out" style="padding-left:4ch">3) <span class="t-ok">"queue:dead_letter"</span></div>`;
  }
  if (upper.startsWith('SLOWLOG')) {
    return `<div class="t-out">1) 1) (integer) 4218  2) (integer) 1731905984  3) (integer) 18420  4) 1) "GET" 2) "cache:product:8842"</div>
      <div class="t-out">2) 1) (integer) 4217  2) (integer) 1731905902  3) (integer) 12044  4) 1) "HGETALL" 2) "cache:user:1001:profile"</div>
      <div class="t-out t-warn">3) 慢查询共 4 条 · 最长 18.4 ms</div>`;
  }
  if (upper.startsWith('MEMORY DOCTOR')) {
    return `<div class="t-out">Sam, I detected a few issues in this Redis instance memory implants:</div>
      <div class="t-out t-warn"> * Peak memory: 1.48 GB, fragmentation ratio 1.11</div>
      <div class="t-out t-ok"> * I'm here just for mental support buddy, you are the one with the keys.</div>`;
  }
  if (upper.startsWith('MEMORY USAGE')) return '<div class="t-out t-ok">(integer) 1432</div>';
  if (upper.startsWith('OBJECT ENCODING')) return '<div class="t-out t-ok">"listpack"</div>';
  if (upper === 'CLIENT LIST') {
    return `<div class="t-out">id=18 addr=10.0.4.6:52412 fd=9 name=redis-compose age=1840 idle=0 db=0</div>
      <div class="t-out">id=22 addr=10.0.4.18:41222 fd=11 name=worker-7 age=203 idle=12 db=0</div>
      <div class="t-out t-dim">… 24 clients connected</div>`;
  }
  if (upper.startsWith('GET ')) return '<div class="t-out t-ok">"482913"</div>';
  if (upper.startsWith('SET ')) return '<div class="t-out t-ok">OK</div>';
  if (upper.startsWith('HGETALL')) {
    return `<div class="t-result"><div class="r-head"><strong>HGETALL</strong><span>8 fields</span><span>·</span><span>0.42 ms</span></div>
      <div class="r-body"><span class="r-key">name</span><span class="r-val">"Ada Lovelace"</span><span class="r-key">email</span><span class="r-val">"ada@acme.io"</span><span class="r-key">plan</span><span class="r-val">"scale"</span><span class="r-key">region</span><span class="r-val">"ap-east-1"</span></div></div>`;
  }
  return `<div class="t-out t-warn">(error) 预览模式仅演示部分命令 · 试试 PING / DBSIZE / INFO memory</div>`;
}

function runCommand(raw) {
  const command = (raw ?? input.value).trim();
  if (!command) return;
  removeCaret();
  appendRaw(`<div class="t-cmd"><span class="t-time">${now()}</span><span class="t-prompt">redis@prod-cache<span class="host">:6379[0]&gt;</span></span><span class="t-input">${esc(
    command,
  )}</span></div>`);
  appendRaw(resultFor(command));
  appendRaw(caretBlock());
  history.push(command);
  historyIndex = -1;
  input.value = '';
  hideSuggestions();
  scrollBottom();
}

function showSuggestions() {
  const q = input.value.trim().toUpperCase();
  if (!q) {
    hideSuggestions();
    return;
  }
  const matches = COMMANDS.filter((c) => c.cmd.startsWith(q) || q.startsWith(c.cmd.split(' ')[0]));
  if (!matches.length) {
    hideSuggestions();
    return;
  }
  suggestions.innerHTML =
    `<div class="s-head">命令建议</div>` +
    matches
      .slice(0, 6)
      .map(
        (c) =>
          `<button type="button" data-fill="${c.cmd} "><span>${c.cmd}</span><span class="syntax">${c.syntax || ''}</span></button>`,
      )
      .join('');
  suggestions.hidden = false;
  suggestions.querySelectorAll('[data-fill]').forEach((btn) => {
    btn.addEventListener('mousedown', (e) => {
      e.preventDefault();
      input.value = btn.dataset.fill;
      input.focus();
      hideSuggestions();
    });
  });
}

function hideSuggestions() {
  suggestions.hidden = true;
}

input.addEventListener('input', showSuggestions);
input.addEventListener('focus', showSuggestions);
input.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    runCommand();
  } else if (e.key === 'ArrowUp') {
    e.preventDefault();
    if (!history.length) return;
    historyIndex = historyIndex < 0 ? history.length - 1 : Math.max(0, historyIndex - 1);
    input.value = history[historyIndex];
    setTimeout(() => input.setSelectionRange(input.value.length, input.value.length), 0);
  } else if (e.key === 'ArrowDown') {
    e.preventDefault();
    if (historyIndex < 0) return;
    historyIndex += 1;
    if (historyIndex >= history.length) {
      historyIndex = -1;
      input.value = '';
    } else {
      input.value = history[historyIndex];
    }
  } else if (e.key === 'Escape') {
    hideSuggestions();
  }
});

runBtn.addEventListener('click', () => runCommand());

document.querySelectorAll('.cmd-chip, [data-cmd]').forEach((el) => {
  el.addEventListener('click', () => {
    input.value = el.dataset.cmd || el.dataset.cmd;
    input.focus();
    showSuggestions();
  });
});

document.querySelectorAll('.quick-cmds .cmd-chip').forEach((el) => {
  el.addEventListener('click', () => runCommand(el.dataset.cmd));
});

document.querySelectorAll('.hist-item, .saved-cmd').forEach((el) => {
  el.addEventListener('click', () => {
    input.value = el.dataset.cmd || '';
    input.focus();
    input.setSelectionRange(input.value.length, input.value.length);
  });
});

function clearTerminal() {
  termBody.innerHTML = `<div class="t-dim">Redis Compose shell · connected to redis-01.acme.io:6379 (7.4.2) · type HELP for commands</div>${caretBlock()}`;
}

['clearLog', 'clearTop'].forEach((id) => {
  document.getElementById(id)?.addEventListener('click', () => {
    clearTerminal();
    rc.toast('会话已清空');
  });
});

document.getElementById('copyLog')?.addEventListener('click', () => {
  rc.copyText(termBody.innerText);
});

document.getElementById('exportLog')?.addEventListener('click', () => {
  rc.toast('会话日志已导出为 console-2026-11-18.log');
});

scrollBottom();
