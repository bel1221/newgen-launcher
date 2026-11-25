// Electron main process: owns the window and does all the real work
// (login, file sync, launching). The UI talks to it through preload.js.
const { app, BrowserWindow, ipcMain, shell, screen, dialog } = require('electron');
const path = require('path');
const fs = require('fs');
const config = require('../config.json');
const { login, getJoinToken } = require('./auth');
const { syncFiles } = require('./sync');
const { launchGame } = require('./launch');
const { installMods } = require('./mods');
const { WIDTH, HEIGHT, fitScale } = require('./fit');

// Never hand the background videos to a Windows overlay plane: Chromium then always draws them into
// the page itself, where the rounded window clip and the dim apply (README "Background video").
// This is a Chromium GPU workaround name (underscores), checked on Electron 33; it must be set before 'ready'.
app.commandLine.appendSwitch('disable_direct_composition_video_overlays');

const gameRoot = path.join(app.getPath('appData'), '.' + config.folderName);
const settingsFile = path.join(app.getPath('userData'), 'settings.json');
// Bundled mods (README "Mods"): the project's mods folder, shipped next to the app (package.json "extraResources").
const bundledModsDir = app.isPackaged ? path.join(process.resourcesPath, 'mods') : path.join(__dirname, '..', 'mods');

let win = null;
let session = null; // { username, sessionToken }
let gameRunning = false;

function loadSettings() {
  try {
    return JSON.parse(fs.readFileSync(settingsFile, 'utf8'));
  } catch {
    return {};
  }
}

function saveSettings(settings) {
  fs.mkdirSync(path.dirname(settingsFile), { recursive: true });
  fs.writeFileSync(settingsFile, JSON.stringify(settings, null, 2));
}

function send(channel, data) {
  if (win && !win.isDestroyed()) win.webContents.send(channel, data);
}

function createWindow() {
  // 1280x720, or smaller in proportion on a screen it would not fit: the page is zoomed by the same
  // factor, so it always lays out as 1280x720 (README "Window").
  const scale = fitScale(screen.getPrimaryDisplay().workAreaSize);
  win = new BrowserWindow({
    width: Math.floor(WIDTH * scale),
    height: Math.floor(HEIGHT * scale),
    center: true,
    resizable: false,
    maximizable: false,
    // No OS frame. The window is see-through so the page can draw its own rounded
    // corners (style.css, "Window shell"); the page also draws the minimize/close buttons.
    frame: false,
    transparent: true,
    title: config.serverName,
    icon: path.join(__dirname, 'renderer', 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      zoomFactor: scale,
    },
  });
  win.setMenu(null);
  // The launcher is one local page: nothing in it may load another page or open another window.
  // Web links leave only through 'open-link' below, into the player's own browser.
  win.webContents.on('will-frame-navigate', (event) => event.preventDefault());
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
}

// Link buttons (README "Links"). The page only ever names a link; the address comes from
// config.json and must be a full https:// address, otherwise the link does not exist.
function linkUrl(name) {
  try {
    // hasOwn: only a name written in config.json counts, never a built-in such as "constructor"
    const url = new URL(Object.hasOwn(config.links || {}, name) ? config.links[name] : '');
    return url.protocol === 'https:' ? url.href : '';
  } catch {
    return ''; // missing, empty or not an address
  }
}

// Background videos: every .mp4/.webm in this folder, as paths the page can load, in play order.
// The play order is the file-name order, with numbers compared as numbers (bg-2 before bg-10).
// Add, remove or rename a file there and it is picked up on the next start (README "Background video").
const videosDir = path.join(__dirname, 'renderer', 'assets', 'videos');

function listVideos() {
  try {
    return fs.readdirSync(videosDir)
      .filter((name) => /\.(mp4|webm)$/i.test(name))
      .sort((a, b) => a.localeCompare(b, 'en', { numeric: true }))
      .map((name) => 'assets/videos/' + encodeURIComponent(name));
  } catch {
    return []; // no folder: plain dark background
  }
}

ipcMain.handle('get-info', () => ({
  videos: listVideos(),
  serverName: config.serverName,
  version: require('../package.json').version,
  authEnabled: Boolean(config.apiBaseUrl),
  savedUsername: loadSettings().username || '',
  links: { discord: linkUrl('discord'), website: linkUrl('website') }, // '' = no button
}));

ipcMain.handle('open-link', (_event, name) => {
  const url = linkUrl(name);
  if (!url) throw new Error('No such link');
  return shell.openExternal(url);
});

ipcMain.handle('login', async (_event, { username, password }) => {
  session = await login(config, username, password);
  saveSettings({ ...loadSettings(), username: session.username });
  return { username: session.username };
});

ipcMain.handle('logout', () => {
  session = null;
});

ipcMain.handle('minimize', () => win.minimize());
ipcMain.handle('close', () => win.close());

ipcMain.handle('play', async () => {
  if (!session) throw new Error('Not logged in');
  if (gameRunning) throw new Error('The game is already running');
  gameRunning = true;
  try {
    send('status', 'Checking server files...');
    await syncFiles(config, gameRoot, (p) => send('progress', p));

    // After the sync, so nothing gets past it: install the bundled mods, refuse anything else.
    try {
      installMods(bundledModsDir, path.join(gameRoot, 'mods'), path.join(app.getPath('userData'), 'installed-mods.json'));
    } catch (err) {
      if (!err.addedMods) throw err;
      const message = `Adding Mods is not Allowed, please remove "${err.addedMods.join(', ')}" to continue using Newgen Launcher`;
      await dialog.showMessageBox(win, { type: 'warning', title: config.serverName, message });
      throw new Error(message);
    }

    send('status', 'Getting login ticket...');
    const joinToken = await getJoinToken(config, session);

    send('status', 'Starting Minecraft...');
    await launchGame(config, gameRoot, session, joinToken, {
      onLog: (line) => send('log', String(line)),
      onProgress: (p) => send('progress', p),
      onClose: (code) => {
        gameRunning = false;
        send('status', `Game closed (code ${code})`);
        send('game-closed');
      },
    });
    send('status', 'Game running');
  } catch (err) {
    gameRunning = false;
    throw err;
  }
});

app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
