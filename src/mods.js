// Bundled mods (README "Mods"): the game's mods folder must hold exactly the jars shipped with
// the launcher. Anything else is an "added mod" and blocks PLAY. Self-check: `node src/mods.js`.
// shortcut: checked once, before the game starts, on the player's own PC. It stops casual
// attempts only; upgrade to a server-side check for real enforcement.
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function sha256(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

// { "<name>.jar": "<sha256>" } for every jar directly in the bundled folder.
function bundledHashes(bundledDir) {
  const hashes = {};
  for (const name of fs.existsSync(bundledDir) ? fs.readdirSync(bundledDir) : []) {
    if (name.toLowerCase().endsWith('.jar')) hashes[name] = sha256(path.join(bundledDir, name));
  }
  return hashes;
}

// Names of everything in the game's mods folder (files and subfolders) that is not a bundled jar
// with the same content. A bundled jar swapped for another file under the same name is in the list.
function addedMods(bundledDir, modsDir) {
  if (!fs.existsSync(modsDir)) return [];
  const bundled = bundledHashes(bundledDir);
  return fs.readdirSync(modsDir, { withFileTypes: true })
    .filter((e) => !(e.isFile() && Object.hasOwn(bundled, e.name) && bundled[e.name] === sha256(path.join(modsDir, e.name))))
    .map((e) => e.name);
}

// Throws { addedMods: [names] } when the player added something; otherwise copies in the bundled
// jars that are missing. `recordFile` remembers what the launcher itself installed, so a launcher
// update that changes the bundled mods replaces its own old jars instead of blaming the player.
function installMods(bundledDir, modsDir, recordFile) {
  const bundled = bundledHashes(bundledDir);
  if (!Object.keys(bundled).length) throw new Error('The launcher mods are missing. Reinstall the launcher.');
  fs.mkdirSync(modsDir, { recursive: true });

  let record = {};
  try {
    record = JSON.parse(fs.readFileSync(recordFile, 'utf8'));
  } catch {} // first run
  for (const [name, hash] of Object.entries(record)) {
    const file = path.join(modsDir, path.basename(name)); // basename: the record can never point outside mods
    if (bundled[name] === hash || !fs.existsSync(file) || !fs.statSync(file).isFile()) continue;
    if (sha256(file) === hash) fs.unlinkSync(file); // still the launcher's own old jar, untouched
  }

  const added = addedMods(bundledDir, modsDir);
  if (added.length) throw Object.assign(new Error('Added mods: ' + added.join(', ')), { addedMods: added });

  for (const name of Object.keys(bundled)) {
    const dest = path.join(modsDir, name);
    if (!fs.existsSync(dest)) fs.copyFileSync(path.join(bundledDir, name), dest);
  }
  fs.mkdirSync(path.dirname(recordFile), { recursive: true });
  fs.writeFileSync(recordFile, JSON.stringify(bundled, null, 2));
}

module.exports = { addedMods, installMods };

if (require.main === module) {
  const assert = require('assert');
  const tmp = fs.mkdtempSync(path.join(require('os').tmpdir(), 'mods-check-'));
  const bundledDir = path.join(tmp, 'bundled');
  const modsDir = path.join(tmp, 'game', 'mods');
  const record = path.join(tmp, 'record.json');
  const blocked = () => {
    try {
      installMods(bundledDir, modsDir, record);
      return [];
    } catch (err) {
      return err.addedMods;
    }
  };
  try {
    fs.mkdirSync(bundledDir);
    fs.writeFileSync(path.join(bundledDir, 'a.jar'), 'A');
    fs.writeFileSync(path.join(bundledDir, 'b.jar'), 'B');

    assert.deepStrictEqual(addedMods(bundledDir, modsDir), []); // no mods folder yet
    assert.deepStrictEqual(blocked(), []); // clean: installs
    assert.deepStrictEqual(fs.readdirSync(modsDir).sort(), ['a.jar', 'b.jar']);
    assert.deepStrictEqual(addedMods(bundledDir, modsDir), []);

    fs.writeFileSync(path.join(modsDir, 'cheat.jar'), 'X'); // one extra jar
    assert.deepStrictEqual(addedMods(bundledDir, modsDir), ['cheat.jar']);
    assert.deepStrictEqual(blocked(), ['cheat.jar']);
    assert.ok(fs.existsSync(path.join(modsDir, 'cheat.jar'))); // never deleted
    fs.unlinkSync(path.join(modsDir, 'cheat.jar'));

    fs.writeFileSync(path.join(modsDir, 'a.jar'), 'not A'); // swapped jar
    assert.deepStrictEqual(blocked(), ['a.jar']);
    assert.strictEqual(fs.readFileSync(path.join(modsDir, 'a.jar'), 'utf8'), 'not A'); // not overwritten
    fs.unlinkSync(path.join(modsDir, 'a.jar'));

    fs.mkdirSync(path.join(modsDir, 'extra')); // extra subfolder
    assert.deepStrictEqual(blocked(), ['extra']);
    fs.rmdirSync(path.join(modsDir, 'extra'));

    assert.deepStrictEqual(blocked(), []); // removed jar is put back
    assert.strictEqual(fs.readFileSync(path.join(modsDir, 'a.jar'), 'utf8'), 'A');

    fs.renameSync(path.join(bundledDir, 'b.jar'), path.join(bundledDir, 'b-2.jar')); // launcher update
    assert.deepStrictEqual(blocked(), []);
    assert.deepStrictEqual(fs.readdirSync(modsDir).sort(), ['a.jar', 'b-2.jar']);
    console.log('mods self-check passed');
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
}
