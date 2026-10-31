import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

test('grid navigation measures each element once per key and refreshes between keys', () => {
  const core = readFileSync(new URL('../app/src/main/assets/js/ds-tv-core.js', import.meta.url), 'utf8');
  let shift = 0;
  const elements = Array.from({ length: 8 }, (_, index) => {
    const classes = new Set();
    return {
      reads: 0, styles: 0,
      getBoundingClientRect() {
        this.reads++;
        const left = 250 + (index % 4) * 240 + shift;
        const top = 200 + Math.floor(index / 4) * 250;
        return { left, top, width: 200, height: 200, right: left + 200, bottom: top + 200 };
      },
      matches: () => true, closest: () => null, contains: () => false,
      classList: { add: (value) => classes.add(value), remove: (value) => classes.delete(value) },
      focus() {},
    };
  });
  const context = vm.createContext({
    window: {}, innerWidth: 1600, innerHeight: 900,
    getComputedStyle: (el) => { el.styles++; return { visibility: 'visible', display: 'block', opacity: '1' }; },
    document: {
      getElementById: () => ({}), querySelector: () => null,
      body: { querySelectorAll: () => [] }, activeElement: null,
      querySelectorAll: (selector) => selector === '.ds-browse-videos__video > a[href]' ? elements : [],
      contains: (el) => elements.includes(el), elementFromPoint: () => null,
    },
  });
  vm.runInContext(core.slice(core.indexOf('  window.__dsTvSpatialNavigate ='), core.lastIndexOf('\n})();')), context);
  for (const [direction, index] of [['first', 0], ['right', 1], ['down', 5], ['up', 1]]) {
    for (const el of elements) { el.reads = 0; el.styles = 0; }
    context.window.__dsTvSpatialNavigate(direction);
    assert.equal(context.window.__dsTvSelected, elements[index]);
    for (const el of elements) {
      assert.equal(el.reads, 1);
      assert.equal(el.styles, 1);
    }
  }
  const before = context.window.__dsTvPointerX;
  shift = 15;
  context.window.__dsTvSpatialNavigate('nearest');
  assert.equal(context.window.__dsTvPointerX, before + 15);
});
