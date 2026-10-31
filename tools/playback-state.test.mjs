import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const script = readFileSync(new URL('../app/src/main/assets/js/ds-tv-playback-state.js', import.meta.url), 'utf8');

function fixture() {
  let video = null;
  let iframe = { src: 'https://www.youtube.com/embed/first', contentWindow: {} };
  const handlers = {};
  const location = { pathname: '/spanish/watch', search: '?id=first' };
  const context = vm.createContext({
    location,
    window: { addEventListener: (type, callback) => { handlers[type] = callback; } },
    document: {
      addEventListener: (type, callback) => { handlers[type] = callback; },
      querySelector: (selector) => selector === '.ds-watch-page video' ? video : iframe,
    },
  });
  vm.runInContext(script, context);
  return {
    location,
    get iframe() { return iframe; },
    set iframe(value) { iframe = value; },
    set video(value) { video = value; },
    ended: () => context.window.__dsTvHasVideoEnded(),
    playing: (target) => handlers.playing({ target }),
    reinject: () => vm.runInContext(script, context),
    message(info, { source = iframe?.contentWindow, event = 'onStateChange', origin = 'https://www.youtube.com' } = {}) {
      handlers.message({ source, origin, data: JSON.stringify({ event, info }) });
    },
  };
}

test('YouTube must play before an ended message can close fullscreen', () => {
  const f = fixture();
  f.message(0);
  assert.equal(f.ended(), false);
  f.message({ playerState: 1 }, { event: 'infoDelivery' });
  assert.equal(f.ended(), false);
  f.message(0);
  assert.equal(f.ended(), true);
  f.message(1);
  assert.equal(f.ended(), false);
});

test('a new page discards the previous video end state', () => {
  const f = fixture();
  f.message(1);
  f.message(0);
  assert.equal(f.ended(), true);
  f.location.search = '?id=second';
  assert.equal(f.ended(), false);
  f.message(0);
  assert.equal(f.ended(), false);
});

test('replaced players and changed iframe sources reset playback', () => {
  const f = fixture();
  f.message(1);
  f.message(0);
  const previous = f.iframe;
  f.iframe = { src: 'https://www.youtube.com/embed/second', contentWindow: {} };
  assert.equal(f.ended(), false);
  f.message(1);
  f.message(0, { source: previous.contentWindow });
  assert.equal(f.ended(), false);
  f.message(0);
  assert.equal(f.ended(), true);
  f.iframe.src = 'https://www.youtube.com/embed/third';
  assert.equal(f.ended(), false);
});

test('only playback-state messages from the current YouTube player count', () => {
  const f = fixture();
  f.message(1);
  f.message(0, { event: 'onPlaybackRateChange' });
  f.message(0, { origin: 'https://example.com' });
  f.message(0, { source: {} });
  assert.equal(f.ended(), false);
  f.message({ playerState: 0 }, { event: 'infoDelivery' });
  assert.equal(f.ended(), true);
});

test('Shaka must play and its state resets for each media source', () => {
  const f = fixture();
  const video = { currentSrc: 'first.m3u8', paused: true, ended: true, readyState: 4 };
  f.video = video;
  assert.equal(f.ended(), false);
  video.ended = false;
  video.paused = false;
  assert.equal(f.ended(), false);
  video.ended = true;
  assert.equal(f.ended(), true);
  video.currentSrc = 'second.m3u8';
  assert.equal(f.ended(), false);
  video.ended = false;
  f.playing(video);
  video.ended = true;
  assert.equal(f.ended(), true);
  f.video = { ...video };
  assert.equal(f.ended(), false);
});

test('leaving watch clears playback and reinjection preserves the active tracker', () => {
  const f = fixture();
  f.message(1);
  f.reinject();
  f.message(0);
  assert.equal(f.ended(), true);
  f.location.pathname = '/spanish/browse';
  assert.equal(f.ended(), false);
  f.location.pathname = '/spanish/watch';
  assert.equal(f.ended(), false);
  f.iframe = null;
  assert.equal(f.ended(), false);
});
