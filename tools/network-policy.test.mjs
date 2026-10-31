import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

test('data stays fresh while assets can use HTTP caching', () => {
  const calls = [];
  class XHR {
    open() {}
    send() {}
    setRequestHeader(...args) { calls.push(args); }
  }
  const context = vm.createContext({
    URL, XMLHttpRequest: XHR,
    location: { href: 'https://app.dreaming.com/spanish/browse', origin: 'https://app.dreaming.com' },
    window: { fetch: (input, options) => { calls.push([input, options]); } },
  });
  const script = readFileSync(new URL('../app/src/main/assets/js/ds-tv-network.js', import.meta.url), 'utf8');
  vm.runInContext(script, context);
  vm.runInContext(script, context);
  const fetch = context.window.fetch;
  fetch('/videos/production/es.json', { headers: { Authorization: 'token' } });
  assert.equal(calls.at(-1)[1].cache, 'no-store');
  assert.equal(calls.at(-1)[1].headers.Authorization, 'token');
  fetch({ url: 'https://app.dreaming.com/.netlify/functions/user' });
  assert.equal(calls.at(-1)[1].cache, 'no-store');
  for (const url of ['/assets/app-hash.js', '/assets/style.css', '/favicon/icon.png', 'https://www.youtube.com/embed/id']) {
    fetch(url);
    assert.equal(calls.at(-1)[1], undefined);
  }
  const xhr = new XHR();
  xhr.open('GET', '/.netlify/functions/videos');
  xhr.send();
  assert.deepEqual(calls.at(-1), ['Cache-Control', 'no-cache']);
  const count = calls.length;
  xhr.open('GET', '/assets/image.png');
  xhr.send();
  assert.equal(calls.length, count);
});
