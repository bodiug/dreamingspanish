import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const core = readFileSync(new URL('../app/src/main/assets/js/ds-tv-core.js', import.meta.url), 'utf8');

function section(start, end) {
  assert(core.includes(start) && core.includes(end));
  return core.slice(core.indexOf(start), core.indexOf(end));
}

function actionFixture(action) {
  const calls = [];
  const target = {
    dataset: { dsTvPlayerAction: action },
    closest: () => null,
    getAttribute: () => '',
    focus() {},
    getBoundingClientRect: () => ({ left: 10, top: 20, width: 40, height: 30 }),
    dispatchEvent: (event) => calls.push(event.type),
  };
  const context = vm.createContext({
    innerWidth: 1280, innerHeight: 720,
    window: {
      innerWidth: 1280, innerHeight: 720,
      __dsTvSelected: target,
      __dsTvPlayerAction: (value) => calls.push(value),
    },
    document: { elementFromPoint: () => target },
    MouseEvent: class { constructor(type) { this.type = type; } },
  });
  vm.runInContext(section('  window.__dsTvPointerAction =', '  window.__dsTvHasOpenPopup ='), context);
  return { window: context.window, calls };
}

for (const action of ['play-pause', 'quality', 'set-quality']) {
  test(`mouse and selection activate ${action} exactly once`, () => {
    const f = actionFixture(action);
    f.window.__dsTvPointerAction(50, 50, 1280, 720, 'click');
    assert.deepEqual(f.calls, [action]);
    f.window.__dsTvSelectedElementAction('click');
    assert.deepEqual(f.calls, [action, action]);
  });
}

test('both navigation modes return the native fullscreen marker', () => {
  const f = actionFixture('fullscreen');
  assert.match(f.window.__dsTvPointerAction(50, 50, 1280, 720, 'click'), /^FULLSCREEN\|/);
  assert.match(f.window.__dsTvSelectedElementAction('click'), /^FULLSCREEN\|/);
  assert.deepEqual(f.calls, []);
});

test('idle timeout hides controls even while a player button remains selected', () => {
  const classes = new Set(['__ds-tv-selection-active']);
  const controls = {
    classList: {
      add: (name) => classes.add(name),
      remove: (...names) => names.forEach((name) => classes.delete(name)),
    },
  };
  const player = { classList: { add() {}, remove() {} }, dispatchEvent() {} };
  const host = { querySelector: (selector) => selector.includes('ds-tv-player-controls') ? controls : player };
  let timeout;
  const context = vm.createContext({
    window: {},
    document: { querySelector: () => host },
    MouseEvent: class {},
    setTimeout: (callback) => { timeout = callback; }, clearTimeout() {},
    setInterval() {}, clearInterval() {},
  });
  vm.runInContext(section('  window.__dsTvRevealPlayerControls =', '  window.__dsTvEnterFullscreen ='), context);
  context.window.__dsTvRevealPlayerControls(null);
  assert(classes.has('__ds-tv-reveal'));
  timeout();
  assert.equal(classes.has('__ds-tv-reveal'), false);
  assert.equal(classes.has('__ds-tv-selection-active'), false);
});
