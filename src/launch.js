// Installs Fabric (if needed) and starts Minecraft in offline mode,
// auto-joining the server and handing the join token to the client mod.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Client } = require('minecraft-launcher-core');

// Downloads Fabric's version JSON so the launcher library can launch it.
async function installFabric(config, root) {
  const id = `fabric-loader-${config.fabricLoaderVersion}-${config.minecraftVersion}`;
  const dir = path.join(root, 'versions', id);
  const file = path.join(dir, `${id}.json`);
  if (!fs.existsSync(file)) {
    const url = `https://meta.fabricmc.net/v2/versions/loader/${config.minecraftVersion}/${config.fabricLoaderVersion}/profile/json`;
    const res = await fetch(url).catch(() => null);
    if (!res || !res.ok) throw new Error('Could not download Fabric (check versions in config.json)');
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(file, await res.text());
  }
  return id;
}

// Same UUID an offline-mode server generates: UUID v3 of "OfflinePlayer:<name>".
function offlineUuid(username) {
  const hash = crypto.createHash('md5').update(`OfflinePlayer:${username}`).digest();
  hash[6] = (hash[6] & 0x0f) | 0x30;
  hash[8] = (hash[8] & 0x3f) | 0x80;
  const h = hash.toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

function offlineAuth(username) {
  return {
    access_token: '0',
    client_token: '0',
    uuid: offlineUuid(username),
    name: username,
    user_properties: '{}',
    meta: { type: 'mojang', demo: false },
  };
}

function versionAtLeast(version, target) {
  const a = version.split('.').map(Number);
  const b = target.split('.').map(Number);
  for (let i = 0; i < Math.max(a.length, b.length); i++) {
    const x = a[i] || 0;
    const y = b[i] || 0;
    if (x !== y) return x > y;
  }
  return true;
}

// Writes the token where the client mod can read it (the mod must delete it after reading).
function writeTokenFile(config, root, username, joinToken) {
  const file = path.join(root, 'config', config.tokenFileName);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  if (joinToken) {
    fs.writeFileSync(file, JSON.stringify({ username, token: joinToken, createdAt: Date.now() }));
  } else if (fs.existsSync(file)) {
    fs.unlinkSync(file);
  }
}

async function launchGame(config, root, session, joinToken, { onLog, onProgress, onClose }) {
  const fabricId = await installFabric(config, root);
  writeTokenFile(config, root, session.username, joinToken);

  const host = config.serverHost;
  const port = String(config.serverPort);
  const joinArgs = versionAtLeast(config.minecraftVersion, '1.20')
    ? ['--quickPlayMultiplayer', `${host}:${port}`]
    : ['--server', host, '--port', port];

  const launcher = new Client();
  launcher.on('data', onLog);
  launcher.on('debug', (line) => onLog(`[launcher] ${line}`));
  launcher.on('progress', (e) =>
    onProgress({ task: `Downloading ${e.type}`, current: e.task, total: e.total })
  );
  launcher.on('close', onClose);

  const proc = await launcher.launch({
    root,
    version: { number: config.minecraftVersion, type: 'release', custom: fabricId },
    authorization: offlineAuth(session.username),
    memory: config.memory,
    javaPath: config.javaPath || undefined, // TODO: bundle/download Java so players don't need it installed
    customLaunchArgs: joinArgs,
  });
  if (!proc) throw new Error('Minecraft failed to start. Check the log, and that Java is installed.');
}

module.exports = { launchGame };
