const wsUrl = process.argv[2];

if (!wsUrl) {
  console.error("Usage: node tools/inspect-webview.mjs ws://...");
  process.exit(1);
}

const expression = String.raw`
(() => {
  const visible = (el) => {
    const r = el.getBoundingClientRect();
    const s = getComputedStyle(el);
    return r.width > 30 && r.height > 30 &&
      r.bottom > 0 && r.top < innerHeight &&
      r.right > 0 && r.left < innerWidth &&
      s.display !== 'none' && s.visibility !== 'hidden';
  };

  const rows = [...document.querySelectorAll(
    'a[href], article, [role="article"], button, [role="button"], div'
  )].map((el, i) => {
    const r = el.getBoundingClientRect();
    const text = (el.innerText || el.ariaLabel || '').replace(/\s+/g, ' ').trim().slice(0, 100);
    return {
      i,
      tag: el.tagName,
      role: el.getAttribute('role'),
      href: el.href || el.getAttribute('href'),
      className: String(el.className || '').slice(0, 120),
      x: Math.round(r.left),
      y: Math.round(r.top),
      w: Math.round(r.width),
      h: Math.round(r.height),
      visible: visible(el),
      media: el.querySelectorAll('img,picture,video,canvas').length,
      buttons: el.querySelectorAll('button,[role="button"]').length,
      text,
    };
  }).filter((e) => e.visible && e.w > 100 && e.h > 60).slice(0, 120);

  return {
    url: location.href,
    title: document.title,
    innerWidth,
    innerHeight,
    scrollY: Math.round(scrollY),
    bodyWidth: document.body.scrollWidth,
    bodyHeight: document.body.scrollHeight,
    rows,
  };
})()
`;

const ws = new WebSocket(wsUrl);
const timeout = setTimeout(() => {
  console.error("Timed out waiting for DevTools response");
  process.exit(2);
}, 5000);

ws.addEventListener("open", () => {
  ws.send(JSON.stringify({
    id: 1,
    method: "Runtime.evaluate",
    params: {
      expression,
      returnByValue: true,
      awaitPromise: false,
    },
  }));
});

ws.addEventListener("message", (event) => {
  clearTimeout(timeout);
  const data = JSON.parse(event.data);
  console.log(JSON.stringify(data.result?.result?.value ?? data, null, 2));
  ws.close();
});
