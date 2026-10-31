(() => {
  if (window.__dsTvNetworkPolicyInstalled) return;
  window.__dsTvNetworkPolicyInstalled = true;
  const freshData = (input) => {
    try {
      const url = new URL(typeof input === 'string' ? input : input.url, location.href);
      return url.origin === location.origin && !/^\/(assets|favicon)\//.test(url.pathname);
    } catch (e) { return false; }
  };
  const fetch = window.fetch;
  window.fetch = function (input, options) {
    return fetch.call(this, input, freshData(input) ? { ...options, cache: 'no-store' } : options);
  };
  const requests = new WeakMap();
  const open = XMLHttpRequest.prototype.open;
  const send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function (method, url, ...args) {
    requests.set(this, freshData(String(url)));
    return open.call(this, method, url, ...args);
  };
  XMLHttpRequest.prototype.send = function (...args) {
    if (requests.get(this)) this.setRequestHeader('Cache-Control', 'no-cache');
    return send.apply(this, args);
  };
})();
