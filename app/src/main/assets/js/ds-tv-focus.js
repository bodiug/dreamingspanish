(() => {
  const styleId = 'ds-tv-focus-style';
  if (!document.getElementById(styleId)) {
    const s = document.createElement('style');
    s.id = styleId;
    s.textContent = 'a:focus,button:focus,[role="button"]:focus,[tabindex]:focus,video:focus{outline:4px solid #00d084!important;outline-offset:4px!important;border-radius:8px!important;}';
    document.head.appendChild(s);
  }
  const selector = 'a,button,[role="button"],video';
  document.querySelectorAll(selector).forEach((el) => { if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '0'); });
  if (!document.activeElement || document.activeElement === document.body) {
    const first = document.querySelector(selector);
    if (first) first.focus();
  }
})();
