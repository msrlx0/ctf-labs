'use strict';

/**
 * ObsidianPay Mobile API - Lab 08, Phase 1
 *
 * Minimal Express backend that establishes the technical contract for the
 * mobile lab. It is intentionally simple and intentionally NOT yet vulnerable
 * in the ways future phases will explore. Local use only.
 */

const express = require('express');
const { LAB, users, receipts, mobileConfig } = require('./data');

const app = express();
const HOST = '0.0.0.0';
const PORT = Number(process.env.PORT) || LAB.port;

app.disable('x-powered-by');
app.use(express.json({ limit: '64kb' }));

// --- Simple request logging --------------------------------------------------
app.use((req, _res, next) => {
  const ts = new Date().toISOString();
  console.log(`[${ts}] ${req.method} ${req.originalUrl}`);
  next();
});

// --- Didactic token helpers --------------------------------------------------
// Phase 1 token is intentionally predictable/decodable. This is a deliberate
// teaching seam: a future phase will turn this into a controlled vulnerability
// (token forging / weak session material). Do NOT treat this as security.
function issueToken(user) {
  const payload = `${user.username}:${user.role}:${user.id}`;
  return 'op_' + Buffer.from(payload, 'utf8').toString('base64');
}

function decodeToken(token) {
  if (typeof token !== 'string' || !token.startsWith('op_')) return null;
  try {
    const raw = Buffer.from(token.slice(3), 'base64').toString('utf8');
    const [username, role, id] = raw.split(':');
    if (!username || !role || !id) return null;
    return { username, role, userId: Number(id) };
  } catch (_err) {
    return null;
  }
}

function getBearer(req) {
  const header = req.headers['authorization'] || '';
  const match = /^Bearer\s+(.+)$/i.exec(header);
  return match ? match[1].trim() : null;
}

function requireAuth(req, res, next) {
  const token = getBearer(req);
  const claims = decodeToken(token);
  if (!claims) {
    return res
      .status(401)
      .json({ error: 'unauthorized', message: 'Missing or invalid Bearer token.' });
  }
  const user = users.find((u) => u.id === claims.userId && u.username === claims.username);
  if (!user) {
    return res
      .status(401)
      .json({ error: 'unauthorized', message: 'Token does not map to a known user.' });
  }
  req.authUser = user;
  next();
}

// --- Public / meta endpoints -------------------------------------------------
app.get('/', (_req, res) => {
  res.json({
    service: 'ObsidianPay Mobile API',
    tagline: 'Sua carteira digital, no seu bolso.',
    lab: LAB.lab,
    version: LAB.version,
    docs: 'See README.md and STUDENT-GUIDE.md in the lab folder.',
    environment: 'local-only',
  });
});

app.get('/health', (_req, res) => {
  res.json({
    status: 'ok',
    name: LAB.name,
    lab: LAB.lab,
    version: LAB.version,
    phase: LAB.phase,
    expectedPort: LAB.port,
    uptimeSeconds: Math.round(process.uptime()),
  });
});

// --- Mobile API --------------------------------------------------------------
app.post('/api/mobile/login', (req, res) => {
  const { username, password } = req.body || {};
  if (typeof username !== 'string' || typeof password !== 'string') {
    return res
      .status(400)
      .json({ error: 'bad_request', message: 'username and password are required.' });
  }

  const user = users.find((u) => u.username === username && u.password === password);
  if (!user) {
    return res
      .status(401)
      .json({ error: 'invalid_credentials', message: 'Invalid username or password.' });
  }

  const token = issueToken(user);
  res.json({
    token,
    tokenType: 'Bearer',
    profile: {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      role: user.role,
      tier: user.tier,
    },
    featureFlags: mobileConfig.mobileFeatureFlags,
  });
});

app.get('/api/mobile/profile', requireAuth, (req, res) => {
  const u = req.authUser;
  res.json({
    id: u.id,
    username: u.username,
    displayName: u.displayName,
    role: u.role,
    tier: u.tier,
    walletId: u.walletId,
    balanceBRL: u.balanceBRL,
    kycLevel: u.kycLevel,
  });
});

app.get('/api/mobile/config', (_req, res) => {
  res.json(mobileConfig);
});

app.get('/api/mobile/receipt/:id', requireAuth, (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) {
    return res.status(400).json({ error: 'bad_request', message: 'Invalid receipt id.' });
  }

  const receipt = receipts.find((r) => r.receiptId === id);
  if (!receipt) {
    return res.status(404).json({ error: 'not_found', message: 'Receipt not found.' });
  }

  // Phase 1: enforce ownership so the endpoint is well-behaved for now.
  // (Object-level authorization is a planned topic for a later phase.)
  if (receipt.ownerUserId !== req.authUser.id) {
    return res
      .status(403)
      .json({ error: 'forbidden', message: 'This receipt does not belong to you.' });
  }

  res.json(receipt);
});

app.post('/api/mobile/support/sync', (req, res) => {
  const { message, ticketRef } = req.body || {};
  if (typeof message !== 'string' || message.length === 0) {
    return res
      .status(400)
      .json({ error: 'bad_request', message: 'A non-empty message is required.' });
  }

  // Legacy support sync stub. Controlled echo for now; future phases use this
  // path to study legacy/HTTP/mobile sync behavior. No external calls.
  res.json({
    accepted: true,
    mode: mobileConfig.supportSyncMode,
    ticketRef: typeof ticketRef === 'string' ? ticketRef : 'OP-SUP-AUTO',
    echo: message,
    syncedAt: new Date().toISOString(),
    note: 'Legacy support sync stub (Phase 1).',
  });
});

// --- Error handling ----------------------------------------------------------
app.use((_req, res) => {
  res.status(404).json({ error: 'not_found', message: 'Unknown endpoint.' });
});

// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  if (err && err.type === 'entity.parse.failed') {
    return res.status(400).json({ error: 'bad_request', message: 'Malformed JSON body.' });
  }
  console.error('[error]', err && err.message ? err.message : err);
  res.status(500).json({ error: 'internal_error', message: 'Something went wrong.' });
});

const server = app.listen(PORT, HOST, () => {
  console.log(`ObsidianPay Mobile API (${LAB.version}) listening on http://${HOST}:${PORT}`);
  console.log('Local lab environment only. Do not expose outside 127.0.0.1.');
});

// Graceful shutdown for container stop signals.
for (const sig of ['SIGINT', 'SIGTERM']) {
  process.on(sig, () => {
    console.log(`Received ${sig}, shutting down.`);
    server.close(() => process.exit(0));
  });
}

module.exports = app;
