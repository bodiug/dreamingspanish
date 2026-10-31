window.__dsTvApplyAutoplay = function (enabled) {
  const text = (el) => (el.textContent || '').replace(/\s+/g, ' ').trim();
  const checked = (el) => {
    if (el.matches('input[type="checkbox"]')) return el.checked;
    const value = el.getAttribute('aria-checked');
    return value === 'true' ? true : value === 'false' ? false : null;
  };
  const findControl = () => {
    // The website wraps its React-controlled autoplay switch in a label.
    for (const label of document.querySelectorAll('label')) {
      if (text(label) !== 'Autoplay') continue;
      const control = label.querySelector('input[type="checkbox"],[role="switch"],[role="checkbox"]');
      if (control) return control;
    }
    return document.querySelector('[role="switch"][aria-label="Autoplay"],input[type="checkbox"][aria-label="Autoplay"]');
  };
  const apply = () => {
    try {
      if (enabled) localStorage.removeItem('autoplay');
      else if (localStorage.getItem('autoplay') !== 'false') localStorage.setItem('autoplay', 'false');
    } catch (e) {}
    const control = findControl();
    // A real click updates both React state and the website's saved preference.
    if (control && !control.disabled && checked(control) !== null && checked(control) !== enabled) control.click();
  };
  clearInterval(window.__dsTvWebsiteAutoplayInterval);
  apply();
  // The watch page and its switch may mount much later without a full page load.
  window.__dsTvWebsiteAutoplayInterval = setInterval(apply, 500);
};
