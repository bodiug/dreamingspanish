import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const script = readFileSync(new URL('../app/src/main/assets/js/ds-tv-autoplay.js', import.meta.url), 'utf8');

function fixture() {
  const storage = new Map();
  const intervals = new Map();
  const labels = [];
  let nextId = 0;
  const context = vm.createContext({
    window: {},
    localStorage: {
      getItem: (key) => storage.get(key) ?? null,
      setItem: (key, value) => storage.set(key, value),
      removeItem: (key) => storage.delete(key),
    },
    document: { querySelectorAll: () => labels, querySelector: () => null },
    setInterval: (callback) => { intervals.set(++nextId, callback); return nextId; },
    clearInterval: (id) => intervals.delete(id),
  });
  vm.runInContext(script, context);
  return {
    storage, intervals,
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
