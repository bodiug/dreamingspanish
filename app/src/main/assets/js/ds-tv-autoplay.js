window.__dsTvApplyAutoplay = function (enabled) {
  const eventOptions = { bubbles: true, cancelable: true };
  const text = (el) => ((el?.innerText || el?.textContent || '') + '').replace(/\s+/g, ' ').trim();
  const checked = (el) => {
    if (!el) return null;
    if (el.matches && el.matches('input[type="checkbox"],input[type="radio"]')) return !!el.checked;
    const aria = el.getAttribute && el.getAttribute('aria-checked');
    if (aria === 'true' || aria === 'false') return aria === 'true';
    return null;
  };
  const setChecked = (el, value) => {
    if (!el) return false;
    const current = checked(el);
    if (current === value) return true;
    if (el.matches && el.matches('input[type="checkbox"],input[type="radio"]')) {
      const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'checked')?.set;
      if (setter) setter.call(el, value); else el.checked = value;
      el.dispatchEvent(new Event('input', eventOptions));
      el.dispatchEvent(new Event('change', eventOptions));
    }
    if (checked(el) !== value) { (el.closest && el.closest('label'))?.click?.(); }
    if (checked(el) !== value) { el.click && el.click(); }
    return checked(el) === value;
  };
  const findAutoplayControl = () => {
    const controls = [...document.querySelectorAll('input[type="checkbox"],input[type="radio"],button,[role="switch"],[role="checkbox"],label')];
    for (const el of controls) {
      const area = el.closest('label,.form-check,.custom-control,.ds-toggle,.ds-playlist-section,.ds-video-section') || el.parentElement || el;
      const nearby = text(area);
      if (/\bAutoplay\b/i.test(nearby) && !/Skip watched/i.test(nearby)) {
        return el.matches && el.matches('label') ? (el.querySelector('input,[role="switch"],[role="checkbox"],button') || el) : el;
      }
    }
    const labels = [...document.querySelectorAll('*')].filter((el) => text(el) === 'Autoplay');
    for (const label of labels) {
      let node = label;
      for (let i = 0; i < 4 && node; i++, node = node.parentElement) {
        const input = node.querySelector && node.querySelector('input[type="checkbox"],input[type="radio"],[role="switch"],[role="checkbox"],button');
        if (input) return input;
      }
    }
    return null;
  };
  const apply = () => setChecked(findAutoplayControl(), enabled);
  apply();
  clearInterval(window.__dsTvWebsiteAutoplayInterval);
  window.__dsTvWebsiteAutoplayInterval = setInterval(() => { if (apply()) clearInterval(window.__dsTvWebsiteAutoplayInterval); }, 500);
  setTimeout(() => clearInterval(window.__dsTvWebsiteAutoplayInterval), 8000);
  if (enabled) {
    window.__dsTvNextAutoplayEnabled = true;
    return;
  }
  window.__dsTvNextAutoplayEnabled = false;
  if (!window.__dsTvEndedBlockerInstalled) {
    window.__dsTvEndedBlockerInstalled = true;
    window.addEventListener('ended', (event) => {
      if (window.__dsTvNextAutoplayEnabled) return;
      const target = event.target;
      if (target && target.matches && target.matches('video,audio')) {
        event.preventDefault();
        event.stopImmediatePropagation();
        setTimeout(() => { try { target.pause(); } catch (e) {} }, 0);
      }
    }, true);
  }
};
