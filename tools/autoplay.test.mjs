import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const script = readFileSync(new URL('../app/src/main/assets/js/ds-tv-autoplay.js', import.meta.url), 'utf8');

function fixture() {
  const storage = new Map();
  const intervals = new Map();
  const labels = [];
  const controls = new Set();
  let scans = 0;
  let writes = 0;
  let nextId = 0;
  const location = { pathname: '/spanish/watch' };
  const context = vm.createContext({
    window: {}, location,
    localStorage: {
      getItem: (key) => storage.get(key) ?? null,
      setItem: (key, value) => { writes++; storage.set(key, value); },
      removeItem: (key) => { writes++; storage.delete(key); },
    },
    document: {
      querySelectorAll: () => { scans++; return labels; }, querySelector: () => null,
      contains: (control) => controls.has(control),
    },
    setInterval: (callback) => { intervals.set(++nextId, callback); return nextId; },
    clearInterval: (id) => intervals.delete(id),
  });
  vm.runInContext(script, context);
  return {
    storage, intervals, location,
    get scans() { return scans; },
    get writes() { return writes; },
    unmount() { labels.length = 0; controls.clear(); },
    apply: (enabled) => context.window.__dsTvApplyAutoplay(enabled),
    tick: () => [...intervals.values()].forEach((callback) => callback()),
    mount(name, initial, checkbox = false) {
      let state = initial;
      let clicks = 0;
      const control = {
        matches: () => checkbox,
        get checked() { return state; },
        set checked(value) { throw new Error('Do not bypass website state'); },
        getAttribute: () => String(state),
        click() {
          clicks++;
          state = !state;
          if (state) storage.delete('autoplay');
          else storage.set('autoplay', 'false');
        },
      };
      labels.push({ textContent: name, querySelector: () => control });
      controls.add(control);
      return { get state() { return state; }, get clicks() { return clicks; } };
    },
  };
}

test('menu choice is saved in a clean session before a player exists', () => {
  const f = fixture();
  f.apply(false);
  assert.equal(f.storage.get('autoplay'), 'false');
  f.apply(true);
  assert.equal(f.storage.has('autoplay'), false);
  assert.equal(f.intervals.size, 1);
});

test('a watch page mounted after the old eight-second limit adopts the menu choice', () => {
  const f = fixture();
  f.apply(false);
  for (let i = 0; i < 30; i++) f.tick();
  const toggle = f.mount('Autoplay', true);
  const skip = f.mount('Skip watched', true);
  f.tick();
  assert.equal(toggle.state, false);
  assert.equal(toggle.clicks, 1);
  assert.equal(skip.clicks, 0);
  f.tick();
  assert.equal(toggle.clicks, 1);
});

test('menu changes update both the active website state and its stored setting', () => {
  const f = fixture();
  const toggle = f.mount('Autoplay', true);
  f.apply(false);
  assert.equal(toggle.state, false);
  assert.equal(f.storage.get('autoplay'), 'false');
  f.apply(true);
  assert.equal(toggle.state, true);
  assert.equal(f.storage.has('autoplay'), false);
  assert.equal(f.intervals.size, 1);
});

test('an already correct setting is preserved and checkbox changes use real clicks', () => {
  const f = fixture();
  const toggle = f.mount('Autoplay', false, true);
  f.apply(false);
  f.tick();
  assert.equal(toggle.clicks, 0);
  f.apply(true);
  assert.equal(toggle.state, true);
  assert.equal(toggle.clicks, 1);
});

test('idle autoplay checks reuse the switch without scanning labels or writing storage', () => {
  const f = fixture();
  f.mount('Autoplay', false);
  f.apply(false);
  const scans = f.scans;
  const writes = f.writes;
  for (let i = 0; i < 20; i++) f.tick();
  assert.equal(f.scans, scans);
  assert.equal(f.writes, writes);
  f.unmount();
  const replacement = f.mount('Autoplay', true);
  f.tick();
  assert.equal(replacement.state, false);
  assert.equal(f.scans, scans + 1);
});

test('browse pages do not scan for a nonexistent autoplay switch', () => {
  const f = fixture();
  f.location.pathname = '/spanish/browse';
  f.apply(false);
  for (let i = 0; i < 20; i++) f.tick();
  assert.equal(f.scans, 0);
  assert.equal(f.storage.get('autoplay'), 'false');
  f.location.pathname = '/spanish/watch';
  const toggle = f.mount('Autoplay', true);
  f.tick();
  assert.equal(toggle.state, false);
});
