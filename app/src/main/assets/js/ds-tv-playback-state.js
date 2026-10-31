(() => {
  if (window.__dsTvPlaybackStateInstalled) return;
  window.__dsTvPlaybackStateInstalled = true;

  let playback;
  const currentPlayback = () => {
    const page = location.pathname + location.search;
    const video = location.pathname.includes('/watch') ? document.querySelector('.ds-watch-page video') : null;
    const iframe = location.pathname.includes('/watch') ? document.querySelector('.ds-youtube-player iframe') : null;
    const player = video || iframe;
    const source = video ? (video.currentSrc || video.src) : iframe?.src;
    if (!playback || playback.page !== page || playback.player !== player || playback.source !== source) {
      playback = { page, player, source, video, started: false, ended: false };
    }
    return playback;
  };

  // Capture playback even when it starts and ends between Android's polls.
  document.addEventListener('playing', (event) => {
    const current = currentPlayback();
    if (current.video && event.target === current.video) current.started = true;
  }, true);

  window.addEventListener('message', (event) => {
    if (event.origin !== 'https://www.youtube.com') return;
    const current = currentPlayback();
    if (!current.player || current.video || event.source !== current.player.contentWindow) return;
    let data;
    try { data = JSON.parse(event.data); } catch (e) { return; }
    if (!data) return;
    const state = data.event === 'onStateChange' ? data.info
      : data.event === 'infoDelivery' ? data.info?.playerState : undefined;
    if (typeof state !== 'number') return;
    if (state === 1) current.started = true;
    current.ended = state === 0 && current.started;
  });

  window.__dsTvIsPlayerReady = () => {
    const current = currentPlayback();
    if (!current.player) return false;
    return current.video ? current.video.readyState >= 2 && !current.video.ended : current.started && !current.ended;
  };

  window.__dsTvHasVideoEnded = () => {
    const current = currentPlayback();
    if (!current.player) return false;
    if (current.video) {
      if (!current.video.paused && !current.video.ended && current.video.readyState >= 2) current.started = true;
      return current.started && current.video.ended;
    }
    return current.ended;
  };
})();
