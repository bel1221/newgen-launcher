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

// Background: random clips back to back, forever. Each clip plays from start to end at normal
// speed; the only thing that starts another clip is the clip on screen reaching its end.
// Two stacked <video> elements: the next clip loads in the covered one and comes to the front
// only once it is playing, so the old clip's last frame stays up meanwhile (no black flash).
// A clip that cannot be played is dropped from the list and another is tried. Nothing is
// timed: a clip that is slow to load is simply waited for.
function startBackground(files) {
  const videos = [0, 1].map(() => {
    const v = document.createElement('video');
    v.muted = true;
    v.autoplay = true;
    $('bg').append(v);
    return v;
  });
  let shown = null; // the <video> on screen
  let loading = null; // the <video> loading the next clip underneath; null while a clip just plays
  const next = () => {
    if (!files.length) return; // nothing playable: plain dark background
    const current = shown && shown.getAttribute('src');
    const others = files.filter((f) => f !== current); // never the same clip twice in a row
    const choices = others.length ? others : files;
    loading = videos.find((v) => v !== shown);
    loading.src = choices[Math.floor(Math.random() * choices.length)];
  };
  for (const v of videos) {
    v.addEventListener('playing', () => {
      if (v !== loading) return;
      shown = v;
      loading = null;
      videos.forEach((other) => other.classList.toggle('front', other === v));
    });
    v.addEventListener('ended', () => {
      if (v === shown && !loading) next();
    });
    v.addEventListener('error', () => {
      if (v !== loading && (v !== shown || loading)) return; // not the clip being loaded or shown
      files = files.filter((f) => f !== v.getAttribute('src'));
      next();
    });
  }
  next();
}

async function init() {
  const info = await window.api.getInfo();
  startBackground(info.videos);
  document.title = info.serverName;
  $('username').value = info.savedUsername;
  $('footer').textContent = `${info.serverName} · v${info.version}`;
  if (!info.authEnabled) {
    $('password-row').classList.add('hidden');
    $('offline-note').classList.remove('hidden');
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
  }
});

window.api.on('status', (text) => ($('status').textContent = text));
window.api.on('progress', ({ task, current, total }) => {
  $('status').textContent = task;
  $('progress').max = total || 1;
  $('progress').value = current || 0;
});
window.api.on('log', (line) => {
  const log = $('log');
  log.textContent += line.endsWith('\n') ? line : line + '\n';
  if (log.textContent.length > 200000) log.textContent = log.textContent.slice(-100000);
  log.scrollTop = log.scrollHeight;
});
window.api.on('game-closed', () => ($('play-btn').disabled = false));

init();
