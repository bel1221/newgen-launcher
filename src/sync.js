// Keeps the player's mods / resource packs / configs identical to the server's list.
// The list (manifest) is a JSON file hosted somewhere; format in README and
// manifest.example.json. If config.manifestUrl is empty, syncing is skipped.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function sha1(data) {
  return crypto.createHash('sha1').update(data).digest('hex');
}

// Resolve a manifest path and make sure it can't escape the game folder.
function safeResolve(root, relPath) {
  const base = path.resolve(root);
  const full = path.resolve(base, relPath);
  if (!full.startsWith(base + path.sep)) throw new Error(`Unsafe path in manifest: ${relPath}`);
  return full;
}

async function syncFiles(config, root, onProgress) {
  if (!config.manifestUrl) return;

  const res = await fetch(config.manifestUrl).catch(() => null);
  if (!res || !res.ok) throw new Error('Could not download the server file list');
  const manifest = await res.json();
  const files = manifest.files || [];
  const wanted = new Set();

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    const dest = safeResolve(root, file.path);
    const expected = String(file.sha1).toLowerCase();
    wanted.add(dest);
    onProgress({ task: `Checking ${file.path}`, current: i, total: files.length });

    if (fs.existsSync(dest) && sha1(fs.readFileSync(dest)) === expected) continue;

    const dl = await fetch(file.url).catch(() => null);
    if (!dl || !dl.ok) throw new Error(`Could not download ${file.path}`);
    const data = Buffer.from(await dl.arrayBuffer());
    if (sha1(data) !== expected) throw new Error(`Download was corrupted: ${file.path}`);

    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.writeFileSync(dest, data);
  }

  // Delete anything in managed folders that isn't on the list
  // (removes old versions and mods players added themselves).
  for (const folder of manifest.managedFolders || ['mods']) {
    const dir = safeResolve(root, folder);
    if (!fs.existsSync(dir)) continue;
    for (const name of fs.readdirSync(dir)) {
      const full = path.join(dir, name);
      if (fs.statSync(full).isFile() && !wanted.has(full)) fs.unlinkSync(full);
    }
  }

  onProgress({ task: 'Files up to date', current: files.length, total: files.length });
}

module.exports = { syncFiles };
