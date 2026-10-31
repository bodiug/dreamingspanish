import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

test('player changes are batched without polling or rescanning the document', () => {
  const core = readFileSync(new URL('../app/src/main/assets/js/ds-tv-core.js', import.meta.url), 'utf8');
  let initialScans = 0;
  let subtreeScans = 0;
  let observe;
  const frames = [];
  const root = {
    nodeType: 1, matches: () => false, querySelector: () => ({}),
    querySelectorAll: () => { subtreeScans++; return []; },
  };
  const context = vm.createContext({
    window: {}, clearInterval() {}, cancelAnimationFrame() {},
    requestAnimationFrame: (callback) => { frames.push(callback); return frames.length; },
    document: {
      body: {}, contains: () => true,
      querySelectorAll: () => { initialScans++; return []; },
    },
    MutationObserver: class {
      constructor(callback) { observe = callback; }
      observe() {}
    },
  });
  vm.runInContext(core.slice(core.indexOf('  const playerSelector ='), core.indexOf('  window.__dsTvRevealPlayerControls =')), context);
  assert.equal(initialScans, 1);
  for (let i = 0; i < 10; i++) observe([{ addedNodes: [root], removedNodes: [], target: {} }]);
  assert.equal(frames.length, 1);
  frames.shift()();
  assert.equal(subtreeScans, 1);
  assert.equal(initialScans, 1);
  observe([{ addedNodes: [{ nodeType: 3 }], removedNodes: [], target: {} }]);
  assert.equal(frames.length, 0);
});
