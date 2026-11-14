import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const core = readFileSync(new URL('../app/src/main/assets/js/ds-tv-core.js', import.meta.url), 'utf8');

// November 2026: the browse wrapper contains a data-video-id card whose
// title link is nested under content/header, alongside a separate options button.
function fixture(legacy = false) {
  const clicks = [];
  const menuItem = {
    matches: (selector) => !selector.includes('.ds-browse-videos__video'), closest: () => null, textContent: 'Watch',
    getBoundingClientRect: () => ({ left: 10, top: 200, width: 140, height: 50, right: 150, bottom: 250 }),
    classList: { add() {}, remove() {} }, focus() {},
  };
  const menu = { querySelectorAll: () => [menuItem] };
  const cards = Array.from({ length: 8 }, (_, index) => {
    const rect = () => {
      const left = 250 + (index % 4) * 250;
      const top = 200 + Math.floor(index / 4) * 250;
      return { left, top, width: 220, height: 210, right: left + 220, bottom: top + 210 };
    };
    const card = {
      matches: (selector) => selector.includes('.ds-browse-videos__video'),
      closest: (selector) => selector.includes('.ds-browse-videos__video') ? card : null,
      contains: () => false, getBoundingClientRect: rect,
      classList: { add() {}, remove() {} }, focus() {},
    };
    const link = {
      href: `https://app.dreaming.com/spanish/watch?id=video-${index}`,
      dataset: {}, getAttribute: () => null, textContent: `Title ${index}`,
      closest: (selector) => selector.includes('.ds-browse-videos__video') ? card : null,
      getBoundingClientRect: rect, focus() {},
      dispatchEvent: (event) => { if (event.type === 'click') clicks.push(index); },
    };
    const options = { dataset: {}, textContent: 'Options', dispatchEvent: () => clicks.push('options') };
    card.querySelector = (selector) => {
      if (selector.includes('video-card-title')) return legacy ? null : link;
      if (selector === 'a[href]') return link;
      return options;
    };
    return card;
  });
  const context = vm.createContext({
    window: {}, innerWidth: 1600, innerHeight: 900,
    getComputedStyle: () => ({ visibility: 'visible', display: 'block', opacity: '1' }),
    MouseEvent: class { constructor(type) { this.type = type; } },
    document: {
      getElementById: () => ({}), activeElement: null,
      querySelector: (selector) => selector === '.ds-sidebar-area' ? menu : null,
      querySelectorAll: (selector) => selector.includes('[data-testid="watch-video-card"]') ? cards : [],
      contains: () => true, elementFromPoint: () => null,
    },
  });
  vm.runInContext(core.slice(core.indexOf('  window.__dsTvPointerAction ='), core.indexOf('  window.__dsTvHasOpenPopup =')), context);
  vm.runInContext(core.slice(core.indexOf('  window.__dsTvSpatialNavigate ='), core.lastIndexOf('\n})();')), context);
  return { window: context.window, menuItem, cards, clicks };
}

for (const legacy of [false, true]) {
  test(`${legacy ? 'legacy' : 'nested-title'} cards support menu entry, grid movement and playback`, () => {
    const f = fixture(legacy);
    f.window.__dsTvSpatialNavigate('first');
    assert.equal(f.window.__dsTvSelected, f.cards[0]);
    f.window.__dsTvSpatialNavigate('left');
    assert.equal(f.window.__dsTvSelected, f.menuItem);
    f.window.__dsTvSpatialNavigate('right');
    assert.equal(f.window.__dsTvSelected, f.cards[0]);
    f.window.__dsTvSpatialNavigate('right');
    assert.equal(f.window.__dsTvSelected, f.cards[1]);
    f.window.__dsTvSpatialNavigate('down');
    assert.equal(f.window.__dsTvSelected, f.cards[5]);
    const result = f.window.__dsTvSelectedElementAction('click');
    assert.match(result, /^VIDEO_CARD\|/);
    assert.equal(decodeURIComponent(result.split('|')[1]), 'https://app.dreaming.com/spanish/watch?id=video-5');
    assert.deepEqual(f.clicks, [5]);
  });
}
