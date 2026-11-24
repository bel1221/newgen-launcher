// UI logic only. All real work happens in the main process via window.api.
const $ = (id) => document.getElementById(id);

// Electron wraps errors as "Error invoking remote method 'x': Error: msg" - keep just msg.
function cleanError(err) {
  return String(err && err.message ? err.message : err).replace(/^.*Error: /, '');
}

function show(view) {
  $('login-view').classList.toggle('hidden', view !== 'login');
  $('play-view').classList.toggle('hidden', view !== 'play');
}

// Background: the clips in the order main.js sends them (file-name order), back to back, then
// round again from the first, forever. Each clip plays from its start to its real end at normal
// speed. FADE seconds before that end the next clip starts playing behind it and the ending
// clip fades out over what it has left, so one clip dissolves into the next and none is cut short.
// Two stacked <video> elements take turns: the next clip waits ready (paused) in the covered one.
// A clip that cannot be loaded or played is skipped until the next start; the order goes on.
// The dissolve is deliberately not switched off by "prefers-reduced-motion" (Windows: "Show animations"
// off): a dissolve is the gentler change, and a cut would take the transition away on such a PC.
const FADE = 1.5; // seconds one clip takes to dissolve into the next; 0 = plain cut

function startBackground(files) {
  const videos = [0, 1].map(() => {
    const v = document.createElement('video');
    v.muted = true;
    v.preload = 'auto';
    $('bg').append(v);
    return v;
  });
  let at = -1; // place in `files` of the newest clip
  let shown = null; // the <video> whose clip is running (the newer one while two overlap)
  let next = null; // the <video> with the clip after that; null while that <video> still plays out the ending clip
  let due = true; // the next clip should start as soon as it can (true now: nothing is on screen)
  let frame = 0; // the pending requestAnimationFrame of watch()

  const play = () => next.play().catch(() => {}); // a clip that cannot play reports through 'error' below
  // Puts the following playable clip into the <video> that is free.
  const load = () => {
    if (!files.some(Boolean)) return; // nothing playable: the plain dark background stays
    do at = (at + 1) % files.length; while (!files[at]);
    next = videos.find((v) => v !== shown);
    next.src = files[at];
    if (due) play();
  };
  const begin = () => {
    due = true;
    if (next) play();
  };
  // The ending clip is over (or there was none): the running clip comes to the front and the
  // other <video> takes the following clip behind it.
  const release = () => {
    for (const v of videos) v.className = v === shown ? 'front' : '';
    load();
  };
  // The running clip's own clock decides when the next one starts, so a stalled or paused clip
  // delays it. 'timeupdate' comes about four times a second: for the last half second before
  // the fade the clock is read on every frame instead.
  const watch = () => {
    cancelAnimationFrame(frame);
    if (!shown || due) return;
    const left = shown.duration - shown.currentTime;
    if (left <= FADE) begin();
    else if (left < FADE + 0.5) frame = requestAnimationFrame(watch);
  };
  for (const v of videos) {
    v.addEventListener('timeupdate', watch);
    v.addEventListener('playing', () => {
      if (v !== next) return; // the next clip is really playing: it takes over
      const old = shown;
      shown = v;
      next = null;
      due = false;
      if (old && !old.ended && !old.error) {
        // The dissolve: the ending clip, in front, fades out over exactly what it has left.
        old.style.setProperty('--fade', `${old.duration - old.currentTime}s`);
        old.classList.add('out');
      } else release(); // the first clip, or the old one is already over: plain cut
    });
    // The end of the running clip is the fallback when the timed start was missed;
    // the end of the ending clip frees its <video>.
    const over = () => (v === shown ? begin() : release());
    v.addEventListener('ended', over);
    v.addEventListener('error', () => {
      const i = files.indexOf(v.getAttribute('src'));
      if (i >= 0) files[i] = null; // skipped from now on
      over();
    });
  }
  load();
}

async function init() {
  const info = await window.api.getInfo();
  startBackground(info.videos);
  document.title = info.serverName;
  $('username').value = info.savedUsername;
  $('footer').textContent = `v${info.version}`;
  if (!info.authEnabled) {
    // Offline mode: a password does nothing, so its box is switched off and says so.
    $('password').disabled = true;
    $('password').placeholder = 'Not needed';
  }
  // Link buttons: one shows only if config.json has its link, with the address as its tooltip.
  // A click sends just the link's name; main.js finds the address, checks it and opens the browser.
  for (const btn of document.querySelectorAll('[data-link]')) {
    const url = info.links[btn.dataset.link];
    btn.title = url;
    btn.classList.toggle('hidden', !url);
    btn.addEventListener('click', () => window.api.openLink(btn.dataset.link));
  }
}

$('login-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('login-error').textContent = '';
  $('login-btn').disabled = true;
  try {
    const { username } = await window.api.login($('username').value, $('password').value);
    $('password').value = '';
    $('player-name').textContent = username;
    show('play');
  } catch (err) {
    $('login-error').textContent = cleanError(err);
  } finally {
    $('login-btn').disabled = false;
  }
});

$('logout-btn').addEventListener('click', async () => {
  await window.api.logout();
  show('login');
});

$('win-min').addEventListener('click', () => window.api.minimize());
$('win-close').addEventListener('click', () => window.api.close());

$('play-btn').addEventListener('click', async () => {
  $('play-error').textContent = '';
  $('play-btn').disabled = true; // also blocks a second click while the animation plays
  try {
    // The animation is decoration only: if it fails, launch anyway.
    await window.weaponBurst?.($('play-btn')).catch((err) => console.error(err));
    await window.api.play();
  } catch (err) {
    $('play-error').textContent = cleanError(err);
    $('play-btn').disabled = false;
    $('status').textContent = ''; // the task that failed is over: only the error stays on screen
  }
});

// The progress bar is on screen only while a task reports progress: a plain status line hides it again.
window.api.on('status', (text) => {
  $('status').textContent = text;
  $('progress').classList.add('hidden');
});
window.api.on('progress', ({ task, current, total }) => {
  $('status').textContent = task;
  $('progress').max = total || 1;
  $('progress').value = current || 0;
  $('progress').classList.remove('hidden');
});
window.api.on('log', (line) => {
  const log = $('log');
  log.textContent += line.endsWith('\n') ? line : line + '\n';
  if (log.textContent.length > 200000) log.textContent = log.textContent.slice(-100000);
  log.scrollTop = log.scrollHeight;
});
// The game log starts closed (and cannot scroll while closed): its sidebar button opens and closes it,
// and its newest lines are shown when it opens.
$('log-btn').addEventListener('click', () => {
  const closed = $('log-panel').classList.toggle('hidden');
  $('log-btn').setAttribute('aria-expanded', !closed);
  $('log').scrollTop = $('log').scrollHeight;
});
window.api.on('game-closed', () => ($('play-btn').disabled = false));

init();
