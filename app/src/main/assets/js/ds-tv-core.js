(() => {
  let viewport = document.querySelector('meta[name="viewport"]');
  if (!viewport) { viewport = document.createElement('meta'); viewport.name = 'viewport'; document.head.appendChild(viewport); }
  viewport.setAttribute('content', 'width=1280, initial-scale=1, maximum-scale=1, user-scalable=no');
  const styleId = 'ds-tv-desktop-viewport';
  let s = document.getElementById(styleId);
  if (!s) { s = document.createElement('style'); s.id = styleId; document.head.appendChild(s); }
  s.textContent = 'html,body{overflow-x:hidden!important;} body{touch-action:manipulation!important;}'
    + '.ds-watch-page .ds-watch-page__sections{display:block!important;}'
    + '.ds-watch-page .ds-video-section{width:100%!important;max-width:100%!important;}'
    + '.ds-watch-page .ds-video-section__card-video{width:100%!important;max-width:100%!important;}'
    + '.ds-watch-page .ds-video-section__embed,.ds-watch-page .embed-responsive,.ds-watch-page .ds-shaka-player,.ds-watch-page video{width:100%!important;max-width:100%!important;}'
    + '.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-container,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-container *,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-bottom-controls,.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-controls-button-panel{opacity:1!important;visibility:visible!important;}'
    + '.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .shaka-bottom-controls{background:linear-gradient(transparent,rgba(0,0,0,.82))!important;}'
    + '.ds-watch-page .ds-shaka-player.__ds-tv-reveal-player-controls .ds-video-settings-menu{opacity:1!important;visibility:visible!important;background:rgba(12,16,22,.96)!important;}'
    + '.ds-watch-page .ds-youtube-player{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;background:#000!important;}'
    + '.ds-watch-page .embed-responsive.__ds-tv-has-player .ds-video-section__default{display:none!important;}'
    + '.ds-watch-page .ds-tv-player-controls{position:absolute!important;right:16px!important;top:16px!important;z-index:2147483002!important;display:flex!important;gap:8px!important;opacity:0!important;transition:opacity .12s linear!important;}'
    + '.ds-watch-page .ds-tv-player-controls.__ds-tv-reveal,.ds-watch-page .ds-tv-player-controls.__ds-tv-selection-active{opacity:1!important;}'
    + '.ds-watch-page .ds-tv-player-controls button,.ds-watch-page .ds-tv-player-quality-menu button{appearance:none!important;border:0!important;border-radius:6px!important;background:rgba(12,16,22,.92)!important;color:white!important;font:600 14px system-ui,sans-serif!important;min-width:44px!important;height:36px!important;padding:0 12px!important;}'
    + '.ds-watch-page .ds-tv-player-quality-menu{position:absolute!important;right:16px!important;top:60px!important;z-index:2147483003!important;display:none!important;grid-template-columns:1fr!important;gap:6px!important;background:rgba(12,16,22,.96)!important;padding:8px!important;border-radius:8px!important;opacity:1!important;visibility:visible!important;pointer-events:auto!important;}'
    + '.ds-watch-page .ds-tv-player-quality-menu.show{display:grid!important;opacity:1!important;visibility:visible!important;pointer-events:auto!important;}'
    + '.ds-watch-page .ds-tv-player-quality-menu button.__ds-tv-quality-active{background:#00d084!important;color:#07110c!important;}'
    + 'html.__ds-tv-fullscreen,html.__ds-tv-fullscreen body{overflow:hidden!important;background:#000!important;}'
    + 'html.__ds-tv-fullscreen .ds-top-navbar,html.__ds-tv-fullscreen .ds-sidebar-area,html.__ds-tv-fullscreen .ds-video-section__information,html.__ds-tv-fullscreen .ds-playlist-section{display:none!important;}'
    + 'html.__ds-tv-fullscreen .ds-watch-page,html.__ds-tv-fullscreen .ds-watch-page__sections,html.__ds-tv-fullscreen .ds-video-section,html.__ds-tv-fullscreen .ds-video-section__card-video,html.__ds-tv-fullscreen .ds-video-section__embed,html.__ds-tv-fullscreen .embed-responsive,html.__ds-tv-fullscreen .ds-youtube-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;background:#000!important;z-index:2147483000!important;}'
    + 'html.__ds-tv-fullscreen .ds-video-section__default,html.__ds-tv-fullscreen .ds-video-overlay{display:none!important;}'
    + 'html.__ds-tv-fullscreen .ds-shaka-player,html.__ds-tv-fullscreen iframe,html.__ds-tv-fullscreen video{position:absolute!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;object-fit:contain!important;background:#000!important;}'
    + 'html.__ds-tv-fullscreen .shaka-controls-container{z-index:2147483001!important;}'
    + '.ds-watch-page .ds-playlist-section,.ds-watch-page .ds-video-section__card-comments,.ds-watch-page .ds-video-section__comments-mobile,.ds-watch-page .ds-video-section__actions,.ds-watch-page .ds-video-section__share-button,.ds-watch-page .ds-video-section__download,.ds-watch-page [class*=comments]{display:none!important;}'
    + '.ds-watch-page .ds-video-section__information,.ds-watch-page .ds-video-section__description{display:block!important;max-width:100%!important;width:100%!important;}';

  window.__dsTvYoutubeCommand = (func, args) => {
    const iframe = document.querySelector('.ds-youtube-player iframe');
    if (!iframe || !iframe.contentWindow) return false;
    let origin = '*'; try { origin = new URL(iframe.src).origin || '*'; } catch (e) {}
    iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: func, args: args || [] }), origin);
    return true;
  };

  window.__dsTvVisible = (el) => { if (!el) return false; const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return r.width > 20 && r.height > 20 && r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth && s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0'; };

  window.__dsTvShakaQuality = (value) => {
    const player = document.querySelector('.ds-shaka-player'); if (!player) return false;
    const label = { default: 'auto', hd1080: '1080', hd720: '720', large: '480', medium: '360', small: '240' }[value || 'default'] || 'auto';
    window.__dsTvRevealPlayerControls && window.__dsTvRevealPlayerControls(player);
    const clickMatch = () => {
      const items = [...document.querySelectorAll('.ds-video-settings-menu__option,.shaka-settings-menu button,.shaka-overflow-menu button,button')].filter(window.__dsTvVisible);
      const text = (el) => ((el.getAttribute('aria-label') || el.title || el.innerText || el.textContent || '') + '').toLowerCase();
      const direct = items.find((el) => label === 'auto' ? /^\s*auto\b/.test(text(el)) || /\bauto\b/.test(text(el)) : (text(el).includes(label) && /p?\b/.test(text(el))));
      if (direct) { direct.click(); return true; }
      const quality = items.find((el) => /quality|resolution/.test(text(el)));
      if (quality) { quality.click(); setTimeout(clickMatch, 140); return true; }
      return false;
    };
    player.querySelector('.shaka-video-settings-button')?.click(); setTimeout(clickMatch, 160); return true;
  };

  window.__dsTvPlayerAction = (action, value, source) => {
    const sourceHost = source && source.closest && source.closest('.embed-responsive');
    const sourcePlayer = (sourceHost && sourceHost.querySelector('.ds-shaka-player,.ds-youtube-player')) || (source && source.closest && source.closest('.ds-shaka-player,.ds-youtube-player'));
    const player = sourcePlayer || document.querySelector('.ds-shaka-player,.ds-youtube-player');
    const host = (player && player.closest && player.closest('.embed-responsive')) || player;
    if (action === 'fullscreen') return true;
    if (action === 'quality') { const menu = host?.querySelector(':scope > .ds-tv-player-quality-menu'); if (menu) { const open = menu.classList.toggle('show'); const controls = host.querySelector(':scope > .ds-tv-player-controls'); if (open && window.__dsTvRevealPlayerControls) { window.__dsTvRevealPlayerControls(menu); } else if (!open) { controls?.classList.remove('__ds-tv-reveal'); } return true; } return false; }
    if (action === 'set-quality') { document.querySelectorAll('.ds-tv-player-quality-menu button').forEach((b) => b.classList.toggle('__ds-tv-quality-active', b.dataset.dsTvPlayerValue === (value || 'default'))); const menu = source?.closest?.('.ds-tv-player-quality-menu') || host?.querySelector(':scope > .ds-tv-player-quality-menu'); const targetHost = menu?.closest('.embed-responsive') || host; const targetPlayer = targetHost?.querySelector('.ds-shaka-player,.ds-youtube-player') || player; const controls = targetHost?.querySelector(':scope > .ds-tv-player-controls'); menu?.classList.remove('show'); setTimeout(() => controls?.classList.remove('__ds-tv-reveal'), 450); if (targetPlayer?.classList.contains('ds-youtube-player')) return !!((window.__dsTvYoutubeCommand && window.__dsTvYoutubeCommand('setPlaybackQuality', [value || 'default'])) | (window.__dsTvYoutubeCommand && window.__dsTvYoutubeCommand('setPlaybackQualityRange', [value || 'default']))); return window.__dsTvShakaQuality && window.__dsTvShakaQuality(value || 'default'); }
    if (action === 'pause') { const v = player?.querySelector('video') || document.querySelector('video'); if (v) { v.pause && v.pause(); return true; } window.__dsTvYoutubePaused = true; return window.__dsTvYoutubeCommand && window.__dsTvYoutubeCommand('pauseVideo', []); }
    if (action === 'play') { const v = player?.querySelector('video') || document.querySelector('video'); if (v) { v.play && v.play(); return true; } window.__dsTvYoutubePaused = false; return window.__dsTvYoutubeCommand && window.__dsTvYoutubeCommand('playVideo', []); }
    if (action === 'play-pause') { const v = player?.querySelector('video') || document.querySelector('video'); if (v) { if (v.paused) { v.play && v.play(); } else { v.pause && v.pause(); } return true; } window.__dsTvYoutubePaused = !window.__dsTvYoutubePaused; return window.__dsTvYoutubeCommand && window.__dsTvYoutubeCommand(window.__dsTvYoutubePaused ? 'pauseVideo' : 'playVideo', []); }
    return false;
  };

  const ensurePlayerControls = () => {
    document.querySelectorAll('.ds-shaka-player,.ds-youtube-player').forEach((player) => {
      const host = player.closest('.embed-responsive') || player;
      host.classList.add('__ds-tv-has-player');
      if (host.querySelector(':scope > .ds-tv-player-controls')) return;
      const controls = document.createElement('div'); controls.className = 'ds-tv-player-controls';
      const makeButton = (text, action, value) => { const b = document.createElement('button'); b.type = 'button'; b.textContent = text; b.setAttribute('aria-label', text); b.dataset.dsTvPlayerAction = action; if (value) b.dataset.dsTvPlayerValue = value; return b; };
      controls.appendChild(makeButton('Play/Pause', 'play-pause'));
      controls.appendChild(makeButton('Quality', 'quality'));
      controls.appendChild(makeButton('Fullscreen', 'fullscreen'));
      const menu = document.createElement('div'); menu.className = 'ds-tv-player-quality-menu';
      [['Auto', 'default'], ['1080p', 'hd1080'], ['720p', 'hd720'], ['480p', 'large'], ['360p', 'medium'], ['240p', 'small']].forEach(([text, value]) => menu.appendChild(makeButton(text, 'set-quality', value)));
      host.appendChild(controls); host.appendChild(menu);
    });
  };
  ensurePlayerControls();
  clearInterval(window.__dsTvEnsurePlayerControlsInterval);
  window.__dsTvEnsurePlayerControlsInterval = setInterval(ensurePlayerControls, 1000);
  window.__dsTvPlayerControlsObserver && window.__dsTvPlayerControlsObserver.disconnect && window.__dsTvPlayerControlsObserver.disconnect();
  window.__dsTvPlayerControlsObserver = new MutationObserver(() => ensurePlayerControls());
  window.__dsTvPlayerControlsObserver.observe(document.body, { childList: true, subtree: true });

  window.__dsTvRevealPlayerControls = (el) => {
    const host = el && el.closest && el.closest('.embed-responsive');
    const player = (el && el.closest && el.closest('.ds-shaka-player,.ds-youtube-player')) || (host && host.querySelector('.ds-shaka-player,.ds-youtube-player')) || document.querySelector('.ds-shaka-player,.ds-youtube-player');
    if (!player) return;
    player.classList.add('__ds-tv-reveal-player-controls');
    (player.closest('.embed-responsive') || player).querySelector(':scope > .ds-tv-player-controls')?.classList.add('__ds-tv-reveal');
    clearTimeout(window.__dsTvRevealPlayerTimer);
    window.__dsTvRevealPlayerTimer = setTimeout(() => { player.classList.remove('__ds-tv-reveal-player-controls'); (player.closest('.embed-responsive') || player).querySelector(':scope > .ds-tv-player-controls')?.classList.remove('__ds-tv-reveal'); }, 2200);
  };

  window.__dsTvEnterFullscreen = () => {
    if (!location.href.includes('/watch')) return false;
    const player = (document.querySelector('.ds-shaka-player,.ds-youtube-player,video')?.closest('.embed-responsive')) || document.querySelector('.embed-responsive') || document.querySelector('.ds-video-section__embed');
    if (!player) return false;
    document.documentElement.classList.add('__ds-tv-fullscreen');
    window.scrollTo(0, 0);
    window.__dsTvRevealPlayerControls && window.__dsTvRevealPlayerControls(player);
    return true;
  };

  window.__dsTvEnterTvFullscreen = function () {
    if (!location.href.includes('/watch')) return false;
    const player = (document.querySelector('.ds-shaka-player,.ds-youtube-player,video')?.closest('.embed-responsive')) || document.querySelector('.embed-responsive') || document.querySelector('.ds-video-section__embed');
    if (!player) return false;
    let fs = document.getElementById('ds-tv-fullscreen-fallback-style');
    if (!fs) { fs = document.createElement('style'); fs.id = 'ds-tv-fullscreen-fallback-style'; document.head.appendChild(fs); }
    fs.textContent = 'html.__ds-tv-fullscreen,html.__ds-tv-fullscreen body{overflow:hidden!important;background:#000!important;}'
      + 'html.__ds-tv-fullscreen .ds-top-navbar,html.__ds-tv-fullscreen .ds-sidebar-area,html.__ds-tv-fullscreen .ds-video-section__information,html.__ds-tv-fullscreen .ds-playlist-section{display:none!important;}'
      + '.ds-watch-page .ds-youtube-player{position:absolute!important;inset:0!important;width:100%!important;height:100%!important;background:#000!important;}'
      + '.ds-watch-page .embed-responsive.__ds-tv-has-player .ds-video-section__default{display:none!important;}'
      + 'html.__ds-tv-fullscreen .ds-watch-page,html.__ds-tv-fullscreen .ds-watch-page__sections,html.__ds-tv-fullscreen .ds-video-section,html.__ds-tv-fullscreen .ds-video-section__card-video,html.__ds-tv-fullscreen .ds-video-section__embed,html.__ds-tv-fullscreen .embed-responsive,html.__ds-tv-fullscreen .ds-youtube-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;margin:0!important;padding:0!important;background:#000!important;z-index:2147483000!important;}'
      + 'html.__ds-tv-fullscreen .ds-video-section__default,html.__ds-tv-fullscreen .ds-video-overlay{display:none!important;}'
      + 'html.__ds-tv-fullscreen .ds-shaka-player,html.__ds-tv-fullscreen iframe,html.__ds-tv-fullscreen video{position:absolute!important;inset:0!important;width:100vw!important;height:100vh!important;max-width:none!important;object-fit:contain!important;background:#000!important;}'
      + 'html.__ds-tv-fullscreen .shaka-controls-container{z-index:2147483001!important;}';
    document.documentElement.classList.add('__ds-tv-fullscreen');
    window.scrollTo(0, 0);
    if (window.__dsTvRevealPlayerControls) { window.__dsTvRevealPlayerControls(player); }
    else { player.classList.add('__ds-tv-reveal-player-controls'); }
    return true;
  };

  window.__dsTvExitTvFullscreen = function () {
    document.documentElement.classList.remove('__ds-tv-fullscreen');
    if (document.fullscreenElement && document.exitFullscreen) { document.exitFullscreen().catch(() => {}); }
    document.querySelector('.ds-tv-player-quality-menu')?.classList.remove('show');
    document.querySelector('.ds-tv-player-controls')?.classList.remove('__ds-tv-reveal');
    const player = document.querySelector('.ds-shaka-player,.ds-youtube-player');
    if (player) { player.classList.remove('__ds-tv-reveal-player-controls'); }
    const button = document.querySelector('.shaka-video-settings-button,.shaka-fullscreen-button');
    if (button) { button.focus && button.focus({ preventScroll: true }); }
    return true;
  };

  window.__dsTvExitFullScreenVideoCleanup = function () {
    document.documentElement.classList.remove('__ds-tv-fullscreen');
    if (document.fullscreenElement && document.exitFullscreen) { document.exitFullscreen().catch(() => {}); }
    document.querySelector('.ds-tv-player-quality-menu')?.classList.remove('show');
    document.querySelector('.ds-tv-player-controls')?.classList.remove('__ds-tv-reveal');
    const iframe = document.querySelector('.ds-youtube-player iframe');
    if (iframe && iframe.contentWindow) { window.__dsTvYoutubePaused = true; iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: 'pauseVideo', args: [] }), 'https://www.youtube.com'); }
    return true;
  };

  window.__dsTvSetPlayback = function (action) {
    const v = document.querySelector('video');
    if (v) {
      if (action === 'toggle') {
        if (v.paused) { v.play && v.play(); } else { v.pause && v.pause(); }
      } else if (action === 'play') {
        v.play && v.play();
      } else {
        v.pause && v.pause();
      }
      return true;
    }
    if (window.__dsTvPlayerAction) return window.__dsTvPlayerAction(action === 'toggle' ? 'play-pause' : action);
    const iframe = document.querySelector('.ds-youtube-player iframe');
    if (!iframe || !iframe.contentWindow) return false;
    const paused = action === 'toggle' ? !window.__dsTvYoutubePaused : action === 'pause';
    window.__dsTvYoutubePaused = paused;
    iframe.contentWindow.postMessage(JSON.stringify({ event: 'command', func: paused ? 'pauseVideo' : 'playVideo', args: [] }), 'https://www.youtube.com');
    return true;
  };

  window.__dsTvClearStorage = function () {
    try { localStorage.clear(); sessionStorage.clear(); } catch (e) {}
  };

  window.__dsTvPointerAction = function (nativeX, nativeY, viewWidth, viewHeight, action) {
    const x = nativeX * window.innerWidth / viewWidth;
    const y = nativeY * window.innerHeight / viewHeight;
    const el = document.elementFromPoint(x, y);
    if (!el) return;
    const target = el.closest('a,button,[role="button"],input,textarea,select,video,[tabindex]') || el;
    target.focus && target.focus({ preventScroll: true });
    if (action === 'click') {
      ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach((type) => target.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, clientX: x, clientY: y, view: window })));
    }
  };

  window.__dsTvSelectedElementAction = function (action) {
    const selected = window.__dsTvSelected;
    const pointerEl = document.elementFromPoint(window.__dsTvPointerX || window.innerWidth / 2, window.__dsTvPointerY || window.innerHeight / 2);
    const base = selected || pointerEl;
    if (!base) return '';
    const isPopupItem = base.closest && base.closest('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role="listbox"],[role="menu"],[role="dialog"],.ds-select-menu,.ds-popover,[class*="popover"],.ds-tv-player-quality-menu.show');
    const commentsCard = base.closest && base.closest('.ds-video-section__card-comments');
    const target = (commentsCard || isPopupItem) ? base : (window.__dsTvClickTarget ? window.__dsTvClickTarget(base) : base);
    const videoCard = !!(target.closest && target.closest('.ds-browse-videos__video > a[href],.ds-browse-videos__video'));
    const playerAction = target.dataset && target.dataset.dsTvPlayerAction;
    const playerValue = target.dataset && target.dataset.dsTvPlayerValue;
    const label = ((target.getAttribute && target.getAttribute('aria-label')) || target.title || target.innerText || target.textContent || '').trim();
    const fullscreenButton = playerAction === 'fullscreen' || !!(target.closest && target.closest('.shaka-fullscreen-button')) || /\b(full screen|fullscreen)\b/i.test(label);
    const playPauseButton = playerAction === 'play-pause' || playerAction === 'play' || playerAction === 'pause' || !!(target.closest && target.closest('.shaka-play-button')) || /\b(play|pause)\b/i.test(label);
    target.focus && target.focus({ preventScroll: true });
    if (action === 'click') {
      if (fullscreenButton) {
        // handled natively by MainActivity via the FULLSCREEN| marker in the return value
      } else if (playerAction) {
        window.__dsTvPlayerAction && window.__dsTvPlayerAction(playerAction, playerValue, target);
      } else if (playPauseButton) {
        const v = document.querySelector('video');
        if (v) {
          if (v.paused) { v.play && v.play(); } else { v.pause && v.pause(); }
        } else {
          ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach((type) => target.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, clientX: window.__dsTvPointerX || 0, clientY: window.__dsTvPointerY || 0, view: window })));
        }
      } else {
        ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach((type) => target.dispatchEvent(new MouseEvent(type, { bubbles: true, cancelable: true, clientX: window.__dsTvPointerX || 0, clientY: window.__dsTvPointerY || 0, view: window })));
      }
    }
    const r = target.getBoundingClientRect();
    return (fullscreenButton && action === 'click' ? 'FULLSCREEN|' : '') + (videoCard && action === 'click' ? 'VIDEO_CARD|' : '') + Math.round(r.left + r.width / 2) + ',' + Math.round(r.top + r.height / 2) + ',' + innerWidth + ',' + innerHeight;
  };

  window.__dsTvHasOpenPopup = function () {
    const visible = (el) => { const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return r.width > 30 && r.height > 30 && r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth && s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0'; };
    return [...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role="listbox"],[role="menu"],[role="dialog"],.ds-select-menu,.ds-popover,[class*="popover"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].some((el) => visible(el) && !el.closest('.ds-sidebar-area') && !el.classList.contains('ds-browse-menu'));
  };

  window.__dsTvCloseOpenPopup = function () {
    const visible = (el) => { const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return r.width > 30 && r.height > 30 && r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth && s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0'; };
    const popup = [...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role="listbox"],[role="menu"],[role="dialog"],.ds-select-menu,.ds-popover,[class*="popover"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].find((el) => visible(el) && !el.closest('.ds-sidebar-area') && !el.classList.contains('ds-browse-menu'));
    if (!popup) return false;
    if (popup.classList.contains('ds-tv-player-quality-menu')) {
      popup.classList.remove('show');
      const host = popup.closest('.embed-responsive');
      const toggle = host?.querySelector('[data-ds-tv-player-action="quality"]');
      document.querySelectorAll('.ds-tv-player-controls.__ds-tv-selection-active').forEach((el) => el.classList.remove('__ds-tv-selection-active'));
      document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected'));
      window.__dsTvSelected = toggle || null;
      if (toggle) {
        toggle.classList.add('__ds-tv-selected');
        toggle.closest('.ds-tv-player-controls')?.classList.add('__ds-tv-selection-active');
        const r = toggle.getBoundingClientRect();
        window.__dsTvPointerX = r.left + r.width / 2;
        window.__dsTvPointerY = r.top + r.height / 2;
      }
      return true;
    }
    const toggle = popup.closest('.dropdown,.ds-form-dropdown')?.querySelector('button,.dropdown-toggle,[role="button"]') || popup.closest('.ds-shaka-player,.ds-youtube-player')?.querySelector('[data-ds-tv-player-action="quality"]') || popup.closest('.ds-shaka-player')?.querySelector('.shaka-video-settings-button');
    document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected'));
    window.__dsTvSelected = toggle || null;
    if (toggle) {
      toggle.click();
      const r = toggle.getBoundingClientRect();
      window.__dsTvPointerX = r.left + r.width / 2;
      window.__dsTvPointerY = r.top + r.height / 2;
    } else {
      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', code: 'Escape', bubbles: true }));
    }
    return true;
  };

  window.__dsTvHideVisiblePlayerControls = function () {
    const visible = (el) => { if (!el) return false; const r = el.getBoundingClientRect(); const s = getComputedStyle(el); return r.width > 20 && r.height > 20 && r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth && s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0'; };
    const controls = [...document.querySelectorAll('.ds-tv-player-controls.__ds-tv-reveal,.ds-tv-player-controls.__ds-tv-selection-active,.ds-shaka-player.__ds-tv-reveal-player-controls')];
    if (!controls.some(visible)) return false;
    document.querySelectorAll('.ds-tv-player-controls').forEach((el) => el.classList.remove('__ds-tv-reveal', '__ds-tv-selection-active'));
    document.querySelectorAll('.ds-shaka-player').forEach((el) => el.classList.remove('__ds-tv-reveal-player-controls'));
    return true;
  };

  window.__dsTvSpatialNavigate = function (direction) {
    const interactive = 'a[href],button,[role="button"],[onclick],input:not([type="hidden"]),textarea,select,video,[tabindex]:not([tabindex="-1"])';
    const selStyleId = 'ds-tv-selection-style';
    if (!document.getElementById(selStyleId)) { const ss = document.createElement('style'); ss.id = selStyleId; ss.textContent = '.__ds-tv-selected{outline:5px solid #00d084!important;outline-offset:5px!important;box-shadow:0 0 0 9px rgba(0,208,132,.28)!important;border-radius:10px!important;}'; document.head.appendChild(ss); }
    const isVisible = (el) => { const r = el.getBoundingClientRect(); const cs = getComputedStyle(el); return r.width > 24 && r.height > 24 && r.bottom > 8 && r.right > 8 && r.top < innerHeight - 8 && r.left < innerWidth - 8 && cs.visibility !== 'hidden' && cs.display !== 'none' && cs.opacity !== '0'; };
    const fullyVisible = (el) => { const r = el.getBoundingClientRect(); return r.top >= 155 && r.bottom <= innerHeight - 12 && r.left >= 0 && r.right <= innerWidth; };
    const center = (el) => { const r = el.getBoundingClientRect(); return { x: r.left + r.width / 2, y: r.top + r.height / 2, w: r.width, h: r.height }; };
    const clickable = (el) => { if (el.matches && el.matches(interactive)) return el; const inner = el.querySelector && el.querySelector(interactive); if (inner) return inner; return el.closest && el.closest(interactive) || el; };
    window.__dsTvClickTarget = clickable;
    const menuRoot = document.querySelector('.ds-sidebar-area') || document.querySelector('nav,aside,[role="navigation"]') || document.body;
    const blockedByMenu = (el) => { const p = center(el); const top = document.elementFromPoint(p.x, p.y); return top && top.closest && top.closest('nav,aside,[role="navigation"],header') && !el.contains(top); };
    const allVideos = [...document.querySelectorAll('.ds-browse-videos__video > a[href]')];
    const videoItems = allVideos.filter((el) => isVisible(el) && !blockedByMenu(el)).sort((a, b) => { const ar = a.getBoundingClientRect(); const br = b.getBoundingClientRect(); return ar.top - br.top || ar.left - br.left; });
    const watchRoot = document.querySelector('.ds-watch-page');
    const watchSelector = '[data-ds-tv-player-action],.ds-video-settings-menu__option';
    const labelOf = (el) => (((el && el.getAttribute && el.getAttribute('aria-label')) || (el && el.title) || (el && el.innerText) || (el && el.textContent) || '') + '').trim();
    const playerControlVisible = (el) => { const r = el.getBoundingClientRect(); const cs = getComputedStyle(el); return r.width > 20 && r.height > 20 && r.bottom > 8 && r.right > 8 && r.top < innerHeight - 8 && r.left < innerWidth - 8 && cs.visibility !== 'hidden' && cs.display !== 'none'; };
    const isWantedPlayerControl = (el) => { const label = labelOf(el); return !!(el.matches && el.matches('[data-ds-tv-player-action],.shaka-play-button,.shaka-video-settings-button,.shaka-fullscreen-button,.ds-video-settings-menu__option')) || /^(play\/pause|play|pause|video settings|settings|quality|full screen|fullscreen)$/i.test(label) || /\b(quality|resolution)\b/i.test(label) || /^(auto|1080p?|720p?|480p?|360p?|240p?)$/i.test(label); };
    const isBlockedPlayerControl = (el) => { const label = labelOf(el); return !!(el.matches && el.matches('.shaka-skip-button,.shaka-mute-button,.shaka-current-time,.shaka-volume-bar,.shaka-seek-bar')) || /\b(next video|skip|mute|volume|share|download|comments?)\b/i.test(label); };
    const watchItems = watchRoot ? [...new Set([...watchRoot.querySelectorAll(watchSelector)].map(clickable))].filter((el) => playerControlVisible(el) && isWantedPlayerControl(el) && !isBlockedPlayerControl(el) && !el.closest('.ds-sidebar-area')).sort((a, b) => { const ar = a.getBoundingClientRect(); const br = b.getBoundingClientRect(); return ar.top - br.top || ar.left - br.left; }) : [];
    const toolbarRoot = document.querySelector('.ds-browse-toolbar');
    const toolbarItems = toolbarRoot ? [...new Set([...toolbarRoot.querySelectorAll('button,[role="button"],a[href],input:not([type="hidden"]),[tabindex]:not([tabindex="-1"])')].map(clickable))].filter(isVisible).sort((a, b) => a.getBoundingClientRect().left - b.getBoundingClientRect().left) : [];
    const menuItems = [...new Set([...menuRoot.querySelectorAll('a[href],button,[role="button"],[tabindex]:not([tabindex="-1"])')].map(clickable))].filter(isVisible).sort((a, b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top);
    const popupRoots = [...document.querySelectorAll('.dropdown-menu.show,.ds-form-dropdown__menu.show,[role="listbox"],[role="menu"],[role="dialog"],.ds-select-menu,.ds-popover,[class*="popover"],.ds-video-settings-menu,.ds-tv-player-quality-menu.show')].filter((el) => isVisible(el) && !el.closest('.ds-sidebar-area') && !el.classList.contains('ds-browse-menu'));
    const popupRoot = popupRoots.sort((a, b) => b.getBoundingClientRect().width * b.getBoundingClientRect().height - a.getBoundingClientRect().width * a.getBoundingClientRect().height)[0];
    const popupItems = popupRoot ? [...new Set([...popupRoot.querySelectorAll('[data-ds-tv-player-action],.dropdown-item,.ds-form-dropdown__item,.ds-video-settings-menu__option,button,a[role="button"],[role="button"],[role="option"],[role="menuitem"],a[href],label,[tabindex]:not([tabindex="-1"])')].map((el) => el.classList && el.classList.contains('dropdown-item') ? el : clickable(el)))].filter(isVisible).sort((a, b) => { const ar = a.getBoundingClientRect(); const br = b.getBoundingClientRect(); return ar.top - br.top || ar.left - br.left; }) : [];
    const watchMenuItem = menuItems.find((el) => el.matches && el.matches('.ds-sidebar__links-link--watch')) || menuItems.find((el) => /\bwatch\b/i.test((el.textContent || '').trim())) || menuItems[0];
    const menuStartItem = watchMenuItem || menuItems[0];
    let current = window.__dsTvSelected;
    let currentIsPopup = !!(current && popupItems.includes(current));
    let currentIsMenu = !!(current && menuItems.includes(current));
    let currentIsToolbar = !!(current && toolbarItems.includes(current));
    let currentIsVideo = !!(current && videoItems.includes(current));
    let currentIsWatch = !!(current && watchItems.includes(current));
    let items = popupItems.length ? popupItems : (currentIsMenu && direction !== 'right' ? menuItems : (watchItems.length && !videoItems.length ? watchItems : (currentIsToolbar ? toolbarItems : videoItems)));
    if (direction === 'left' && currentIsVideo && !popupItems.length) {
      const c = center(current); const leftVideos = videoItems.filter((el) => center(el).x < c.x - 10);
      items = leftVideos.length ? videoItems : (menuStartItem ? [menuStartItem] : menuItems);
    }
    if (direction === 'left' && currentIsWatch && menuStartItem && !popupItems.length) { items = [menuStartItem]; }
    if (direction === 'right' && currentIsMenu && !popupItems.length) { items = watchItems.length && !videoItems.length ? watchItems : videoItems; }
    if (!items.length) { items = [...videoItems, ...toolbarItems, ...menuItems]; }
    if (!items.length && watchItems.length) { items = watchItems; }
    if (!items.length) return '';
    if (!current || !document.contains(current) || !isVisible(current)) { current = document.activeElement && isVisible(document.activeElement) ? document.activeElement : null; }
    let origin = current ? center(current) : { x: window.__dsTvPreferredX || innerWidth / 2, y: window.__dsTvPreferredY || innerHeight / 2 };
    let next = null;
    const videoCenters = videoItems.map((el) => ({ el, c: center(el), r: el.getBoundingClientRect() }));
    const watchCenters = watchItems.map((el) => ({ el, c: center(el), r: el.getBoundingClientRect() }));
    const toolbarCenters = toolbarItems.map((el) => ({ el, c: center(el), r: el.getBoundingClientRect() }));
    const popupCenters = popupItems.map((el) => ({ el, c: center(el), r: el.getBoundingClientRect() }));
    const selectNearest = (pool, x, y) => pool.map((item) => ({ item, score: Math.abs(item.c.x - x) * 2 + Math.abs(item.c.y - y) })).sort((a, b) => a.score - b.score)[0]?.item?.el;
    if (direction === 'first') { next = videoItems[0] || watchItems[0] || items[0]; }
    else if (direction === 'nearest' && window.__dsTvPendingElement && document.contains(window.__dsTvPendingElement) && isVisible(window.__dsTvPendingElement)) { next = window.__dsTvPendingElement; window.__dsTvPendingElement = null; }
    else if (popupItems.length) {
      const popupToggle = popupRoot.closest('.embed-responsive')?.querySelector('[data-ds-tv-player-action="quality"]') || popupRoot.closest('.dropdown,.ds-form-dropdown')?.querySelector('button,.dropdown-toggle,[role="button"]');
      if (direction === 'left' && popupRoot.classList.contains('ds-tv-player-quality-menu') && currentIsPopup) { next = popupToggle || current; }
      else if (direction === 'nearest' || !currentIsPopup) { next = selectNearest(popupCenters, window.__dsTvPointerX || origin.x, window.__dsTvPointerY || origin.y) || popupItems[0]; }
      else if (direction === 'up' || direction === 'down') { const index = Math.max(0, popupItems.indexOf(current)); const nextIndex = Math.max(0, Math.min(popupItems.length - 1, index + (direction === 'down' ? 1 : -1))); next = popupItems[nextIndex]; }
      else { const candidates = popupCenters.filter((item) => item.el !== current).filter((item) => direction === 'left' ? item.c.x < origin.x - 10 : item.c.x > origin.x + 10); next = selectNearest(candidates, origin.x, origin.y) || current; }
    }
    else if (currentIsMenu) {
      if (direction === 'right') { next = watchItems.length && !videoItems.length ? watchItems[0] : videoItems[0]; }
      else if (direction === 'up' || direction === 'down') { const index = Math.max(0, menuItems.indexOf(current)); const nextIndex = Math.max(0, Math.min(menuItems.length - 1, index + (direction === 'down' ? 1 : -1))); next = menuItems[nextIndex]; }
      else { next = current; }
    }
    else if (watchItems.length && !videoItems.length) {
      const index = Math.max(0, watchItems.indexOf(current));
      if (direction === 'left' && menuStartItem) { next = menuStartItem; }
      else if (direction === 'up' || direction === 'down') { const nextIndex = Math.max(0, Math.min(watchItems.length - 1, index + (direction === 'down' ? 1 : -1))); next = watchItems[nextIndex]; }
      else { const candidates = watchCenters.filter((item) => item.el !== current).filter((item) => { const dx = item.c.x - origin.x; return direction === 'left' ? dx < -10 : dx > 10; }); next = selectNearest(candidates, origin.x, origin.y) || current; }
      if (next && !fullyVisible(next) && !watchItems.includes(next)) { window.__dsTvPendingElement = next; const p = center(next); window.__dsTvPreferredX = p.x; window.__dsTvPreferredY = p.y; window.__dsTvSelected = null; document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected')); next.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'auto' }); return 'SCROLLED'; }
    }
    else if (direction === 'nearest') { next = selectNearest(videoCenters.length ? videoCenters : items.map((el) => ({ el, c: center(el) })), window.__dsTvPreferredX || origin.x, window.__dsTvPreferredY || origin.y); }
    else if (direction === 'down' && currentIsToolbar) { next = selectNearest(videoCenters, window.__dsTvPreferredX || origin.x, innerHeight / 2); }
    else if ((direction === 'up' || direction === 'down') && currentIsVideo) {
      const sign = direction === 'down' ? 1 : -1;
      const rows = [...new Set(videoCenters.map((item) => Math.round(item.r.top)))].sort((a, b) => a - b);
      const currentTop = current.getBoundingClientRect().top;
      const currentRow = rows.reduce((best, row) => Math.abs(row - currentTop) < Math.abs(best - currentTop) ? row : best, rows[0]);
      const targetRow = direction === 'down' ? rows.find((row) => row > currentRow + 30) : [...rows].reverse().find((row) => row < currentRow - 30);
      if (targetRow !== undefined) {
        const rowItems = videoCenters.filter((item) => Math.abs(item.r.top - targetRow) < 35);
        next = selectNearest(rowItems, window.__dsTvPreferredX || origin.x, targetRow);
        if (next && !fullyVisible(next)) { const nr = next.getBoundingClientRect(); const cr = current.getBoundingClientRect(); window.__dsTvPendingElement = next; window.__dsTvPreferredX = center(next).x; window.__dsTvPreferredY = origin.y; window.__dsTvSelected = null; document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected')); window.scrollBy({ top: nr.top - cr.top, left: 0, behavior: 'auto' }); return 'SCROLLED'; }
      }
      if (!next && direction === 'up' && toolbarCenters.length) { next = selectNearest(toolbarCenters, window.__dsTvPreferredX || origin.x, 0); }
      if (!next) { const rowStep = Math.max(240, Math.min(330, Math.abs((rows[1] || origin.y + 272) - rows[0]) || 272)); window.__dsTvPreferredX = origin.x; window.__dsTvPreferredY = origin.y; window.__dsTvSelected = null; document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected')); window.scrollBy({ top: sign * rowStep, left: 0, behavior: 'auto' }); return 'SCROLLED'; }
    } else {
      const candidates = items.filter((el) => el !== current).map((el) => ({ el, c: center(el) })).filter((item) => { const dx = item.c.x - origin.x; const dy = item.c.y - origin.y; return direction === 'left' ? dx < -10 : direction === 'right' ? dx > 10 : direction === 'up' ? dy < -10 : dy > 10; });
      const scored = candidates.map((item) => { const dx = item.c.x - origin.x; const dy = item.c.y - origin.y; const horizontal = direction === 'left' || direction === 'right'; const primary = horizontal ? Math.abs(dx) : Math.abs(dy); const secondary = horizontal ? Math.abs(dy) : Math.abs(dx); const rowPenalty = horizontal && secondary > 80 ? 2000 : 0; const colPenalty = !horizontal && secondary > 150 ? 700 : 0; return { el: item.el, c: item.c, score: primary + secondary * 4 + rowPenalty + colPenalty }; }).sort((a, b) => a.score - b.score);
      next = scored[0] && scored[0].el;
    }
    if (!next) { return current ? (Math.round(origin.x) + ',' + Math.round(origin.y) + ',' + innerWidth + ',' + innerHeight) : ''; }
    document.querySelectorAll('.ds-tv-player-controls.__ds-tv-selection-active').forEach((el) => el.classList.remove('__ds-tv-selection-active'));
    document.querySelectorAll('.__ds-tv-selected').forEach((el) => el.classList.remove('__ds-tv-selected'));
    window.__dsTvSelected = next; next.classList.add('__ds-tv-selected');
    next.closest && next.closest('.ds-tv-player-controls')?.classList.add('__ds-tv-selection-active');
    const p = center(next); window.__dsTvPointerX = p.x; window.__dsTvPointerY = p.y;
    window.__dsTvPreferredX = p.x; window.__dsTvPreferredY = p.y;
    next.focus && next.focus({ preventScroll: true });
    if (window.__dsTvRevealPlayerControls && (next.closest && next.closest('.ds-shaka-player,.ds-video-section__embed,.embed-responsive') || watchItems.includes(next))) { window.__dsTvRevealPlayerControls(next); }
    return Math.round(p.x) + ',' + Math.round(p.y) + ',' + innerWidth + ',' + innerHeight;
  };
})();
