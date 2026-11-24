// Login against the server's account system.
//
// If config.apiBaseUrl is empty, the launcher runs in OFFLINE mode:
// any valid username is accepted and no password is checked.
// That is only for development; see README "Backend API" for the endpoints
// the main dev needs to provide.

const USERNAME_RE = /^[A-Za-z0-9_]{3,16}$/;

async function post(url, body) {
  let res;
  try {
    res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch {
    throw new Error('Could not reach the login server');
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Login server error (${res.status})`);
  return data;
}

// Returns { username, sessionToken }.
async function login(config, username, password) {
  username = (username || '').trim();
  if (!USERNAME_RE.test(username)) {
    throw new Error('Username must be 3-16 characters: letters, numbers or _');
  }
  if (!config.apiBaseUrl) {
    return { username, sessionToken: null }; // offline dev mode
  }
  if (!password) throw new Error('Enter your password');

  const data = await post(`${config.apiBaseUrl}/launcher/login`, { username, password });
  return { username: data.username || username, sessionToken: data.sessionToken };
}

// Asks the API for a fresh one-time join token right before each launch.
// Returns null in offline dev mode.
async function getJoinToken(config, session) {
  if (!config.apiBaseUrl) return null;
  const data = await post(`${config.apiBaseUrl}/launcher/join-token`, {
    sessionToken: session.sessionToken,
  });
  return data.joinToken;
}

module.exports = { login, getJoinToken };
