'use strict';

/**
 * ObsidianPay Mobile API - Lab 08, Phase 2
 *
 * Express backend that defines the mobile contract AND introduces the first
 * controlled, backend-side vulnerabilities a mobile pentest would explore:
 *   - IDOR / broken object-level authorization (receipts, cards)
 *   - Mass assignment (PATCH profile)
 *   - Weak debug gate (support/diagnostics)
 *   - Legacy route disclosure (legacy/routes)
 *   - Role gate for an internal vault status endpoint
 *   - QR/deep-link transfer preview scaffold + WebView support scaffold
 *
 * Local use only. Progress markers (FLAG{...}) live in data.js and are only
 * reachable through these flaws — never in public docs.
 */

const express = require('express');
const crypto = require('crypto');
const {
  LAB,
  users,
  receipts,
  cards,
  featureFlags,
  vaultStatusByRole,
  legacyMobileTrust,
  environmentConfig,
  mobileVaultConfig,
  networkProfileConfig,
  appIntegrityConfig,
  buildMobileConfig,
} = require('./data');

const app = express();
const HOST = '0.0.0.0';
const PORT = Number(process.env.PORT) || LAB.port;

const DEBUG_HEADER = 'x-obsidian-debug';
const DEBUG_TOKEN = 'mobile-diagnostics';

app.disable('x-powered-by');
app.use(express.json({ limit: '64kb' }));

// --- Simple request logging --------------------------------------------------
app.use((req, _res, next) => {
  const ts = new Date().toISOString();
  console.log(`[${ts}] ${req.method} ${req.originalUrl}`);
  next();
});

// --- Standard responses ------------------------------------------------------
function sendError(res, status, error, message) {
  return res.status(status).json({ error, message });
}

// --- Didactic token helpers --------------------------------------------------
// Predictable-but-consistent token: obsidian-mobile-token-<username>-<userId>.
// Intentionally forgeable: this is a teaching seam, not real security.
function issueToken(user) {
  return `obsidian-mobile-token-${user.username}-${user.id}`;
}

function parseToken(token) {
  if (typeof token !== 'string') return null;
  const m = /^obsidian-mobile-token-([a-z0-9_]+)-(\d+)$/i.exec(token);
  if (!m) return null;
  return { username: m[1], userId: Number(m[2]) };
}

function getBearer(req) {
  const header = req.headers['authorization'] || '';
  const match = /^Bearer\s+(.+)$/i.exec(header);
  return match ? match[1].trim() : null;
}

function requireAuth(req, res, next) {
  const claims = parseToken(getBearer(req));
  if (!claims) {
    return sendError(res, 401, 'unauthorized', 'Missing or invalid Bearer token.');
  }
  const user = users.find((u) => u.id === claims.userId && u.username === claims.username);
  if (!user) {
    return sendError(res, 401, 'unauthorized', 'Token does not map to a known user.');
  }
  req.authUser = user;
  next();
}

// --- Serializers -------------------------------------------------------------
function publicProfile(u) {
  return {
    id: u.id,
    username: u.username,
    displayName: u.displayName,
    phone: u.phone,
    role: u.role,
    plan: u.plan,
    walletId: u.walletId,
    dailyLimit: u.dailyLimit,
    kycApproved: u.kycApproved,
    supportTier: u.supportTier,
    balanceBRL: u.balanceBRL,
  };
}

function maskCardNumber(number) {
  const last4 = String(number).slice(-4);
  return `**** **** **** ${last4}`;
}

// Card view returned to clients. Number is masked, but ownerRole and the
// internalReference are intentionally exposed (controlled leak via IDOR).
function cardView(card) {
  return {
    cardId: card.cardId,
    ownerUserId: card.ownerUserId,
    ownerRole: card.ownerRole,
    brand: card.brand,
    maskedNumber: maskCardNumber(card.number),
    expiry: card.expiry,
    holder: card.holder,
    internalReference: card.internalReference,
  };
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

// --- Auth --------------------------------------------------------------------
app.post('/api/mobile/login', (req, res) => {
  const { username, password } = req.body || {};
  if (typeof username !== 'string' || typeof password !== 'string') {
    return sendError(res, 400, 'bad_request', 'username and password are required.');
  }

  const user = users.find((u) => u.username === username && u.password === password);
  if (!user) {
    return sendError(res, 401, 'invalid_credentials', 'Invalid username or password.');
  }

  res.json({
    token: issueToken(user),
    tokenType: 'Bearer',
    profile: {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      role: user.role,
      plan: user.plan,
    },
    featureFlags: { ...featureFlags },
  });
});

// --- Profile -----------------------------------------------------------------
app.get('/api/mobile/profile', requireAuth, (req, res) => {
  res.json(publicProfile(req.authUser));
});

// PATCH profile — controlled MASS ASSIGNMENT.
// The app would only send displayName/phone, but the backend also accepts
// privileged fields (role, plan, dailyLimit, kycApproved, supportTier) and
// mutates the in-memory user. Only a known set of keys is applied so the app
// cannot be broken by arbitrary input.
const PATCHABLE_FIELDS = [
  'displayName',
  'phone',
  'role',
  'plan',
  'dailyLimit',
  'kycApproved',
  'supportTier',
];

app.patch('/api/mobile/profile', requireAuth, (req, res) => {
  const body = req.body || {};
  if (typeof body !== 'object' || Array.isArray(body)) {
    return sendError(res, 400, 'bad_request', 'JSON object body required.');
  }

  const applied = [];
  for (const key of PATCHABLE_FIELDS) {
    if (Object.prototype.hasOwnProperty.call(body, key)) {
      let value = body[key];
      if (key === 'dailyLimit') value = Number(value);
      if (key === 'kycApproved') value = Boolean(value);
      if (key === 'dailyLimit' && Number.isNaN(value)) continue; // keep app stable
      req.authUser[key] = value;
      applied.push(key);
    }
  }

  res.json({ updated: true, appliedFields: applied, profile: publicProfile(req.authUser) });
});

// --- Config ------------------------------------------------------------------
app.get('/api/mobile/config', (_req, res) => {
  res.json(buildMobileConfig());
});

// --- Receipts ----------------------------------------------------------------
// List: correctly scoped to the authenticated user.
app.get('/api/mobile/receipts', requireAuth, (req, res) => {
  const own = receipts
    .filter((r) => r.ownerUserId === req.authUser.id)
    .map((r) => ({
      receiptId: r.receiptId,
      type: r.type,
      status: r.status,
      amountBRL: r.amountBRL,
      currency: r.currency,
      counterparty: r.counterparty,
      createdAt: r.createdAt,
      reference: r.reference,
    }));
  res.json({ count: own.length, receipts: own });
});

// IDOR (broken object-level authorization): any valid token can fetch ANY
// existing receipt by id, regardless of ownership. The full object (including
// metadata.internalNote) is returned.
app.get('/api/mobile/receipts/:receiptId', requireAuth, (req, res) => {
  const id = Number(req.params.receiptId);
  if (!Number.isInteger(id)) {
    return sendError(res, 400, 'bad_request', 'Invalid receipt id.');
  }
  const receipt = receipts.find((r) => r.receiptId === id);
  if (!receipt) {
    return sendError(res, 404, 'not_found', 'Receipt not found.');
  }
  res.json(receipt);
});

// Phase 1 compatibility endpoint (singular). Ownership IS enforced here so the
// Phase 1 validation continues to pass unchanged.
app.get('/api/mobile/receipt/:id', requireAuth, (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) {
    return sendError(res, 400, 'bad_request', 'Invalid receipt id.');
  }
  const receipt = receipts.find((r) => r.receiptId === id);
  if (!receipt) {
    return sendError(res, 404, 'not_found', 'Receipt not found.');
  }
  if (receipt.ownerUserId !== req.authUser.id) {
    return sendError(res, 403, 'forbidden', 'This receipt does not belong to you.');
  }
  res.json(receipt);
});

// --- Cards -------------------------------------------------------------------
// List: correctly scoped to the authenticated user.
app.get('/api/mobile/cards', requireAuth, (req, res) => {
  const own = cards.filter((c) => c.ownerUserId === req.authUser.id).map(cardView);
  res.json({ count: own.length, cards: own });
});

// IDOR: any valid token can fetch ANY card by id. Number is masked, but
// ownerRole and internalReference leak.
app.get('/api/mobile/cards/:cardId', requireAuth, (req, res) => {
  const card = cards.find((c) => c.cardId === req.params.cardId);
  if (!card) {
    return sendError(res, 404, 'not_found', 'Card not found.');
  }
  res.json(cardView(card));
});

// --- Support: legacy sync (Phase 1 stub, kept) ------------------------------
app.post('/api/mobile/support/sync', (req, res) => {
  const { message, ticketRef } = req.body || {};
  if (typeof message !== 'string' || message.length === 0) {
    return sendError(res, 400, 'bad_request', 'A non-empty message is required.');
  }
  res.json({
    accepted: true,
    mode: 'legacy-http',
    ticketRef: typeof ticketRef === 'string' ? ticketRef : 'OP-SUP-AUTO',
    echo: message,
    syncedAt: new Date().toISOString(),
    note: 'Legacy support sync stub.',
  });
});

// --- Support: diagnostics (weak debug gate) ----------------------------------
// Requires a valid token AND a static debug header. The header is the only
// gate — a weak control on purpose.
app.get('/api/mobile/support/diagnostics', requireAuth, (req, res) => {
  if (req.headers[DEBUG_HEADER] !== DEBUG_TOKEN) {
    return sendError(res, 403, 'forbidden', 'Diagnostics require the mobile debug header.');
  }
  const enabledModules = Object.keys(featureFlags).filter((k) => featureFlags[k]);
  res.json({
    apiVersion: 'v1',
    buildChannel: 'internal-dev',
    enabledModules,
    legacyRoutes: [
      '/api/mobile/support/sync',
      '/api/mobile/legacy/routes',
      '/api/mobile/internal/vault-status',
    ],
    mobileDeepLinks: ['obsidianpay://transfer', 'obsidianpay://support'],
    storageHints: [
      'obsidian.session.token',
      'obsidian.profile.cache',
      'obsidian.receipts.offline',
      'obsidian.debug.last_sync',
    ],
  });
});

// --- Transfer preview (QR / deep link scaffold) ------------------------------
// Weak validation on purpose: accepts amount as string or number, memo is not
// strongly sanitized, and no transfer is executed.
app.post('/api/mobile/transfer/preview', requireAuth, (req, res) => {
  const { toUserId, amount, memo } = req.body || {};
  const normalizedAmount = Number(amount);
  if (Number.isNaN(normalizedAmount)) {
    return sendError(res, 400, 'bad_request', 'amount must be a number or numeric string.');
  }

  const recipient = users.find((u) => u.id === Number(toUserId));
  res.json({
    willExecute: false,
    normalizedPreview: {
      fromUserId: req.authUser.id,
      toUserId: Number(toUserId),
      recipientKnown: Boolean(recipient),
      recipientDisplayName: recipient ? recipient.displayName : null,
      amount: normalizedAmount,
      currency: 'BRL',
      feeBRL: 0,
      memo: typeof memo === 'string' ? memo : '',
      deepLinkScheme: 'obsidianpay://transfer',
    },
    note: 'Preview only. No funds moved (Phase 2 scaffold).',
  });
});

// --- WebView support portal --------------------------------------------------
// Returns an HTML "Mobile Support Portal" that reflects the `topic`/`message`
// query parameters and exposes small support/diagnostics actions. When loaded
// inside the app's WebView it can talk to the in-app `ObsidianBridge`
// (@JavascriptInterface). The page never exfiltrates anything on its own — it
// only renders output into a div when the user taps a button. Local only.
app.get('/api/mobile/webview/support', (req, res) => {
  const topic = typeof req.query.topic === 'string' ? req.query.topic : 'home';
  const message = typeof req.query.message === 'string' ? req.query.message : '';
  res
    .type('html')
    .send(
      `<!doctype html>
<html lang="pt-br">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>ObsidianPay — Mobile Support Portal</title>
    <style>
      body { font-family: -apple-system, Roboto, Arial, sans-serif; margin: 0; background: #0f1115; color: #e9edf2; }
      header { background: #14181f; padding: 16px; border-bottom: 1px solid #232a35; }
      header h1 { font-size: 18px; margin: 0; }
      header small { color: #8aa0b6; }
      main { padding: 16px; }
      .card { background: #161b22; border: 1px solid #232a35; border-radius: 10px; padding: 14px; margin-bottom: 14px; }
      .muted { color: #8aa0b6; font-size: 13px; }
      .pill { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 12px; background: #1f2733; color: #9fb4c9; }
      .ok { background: #143226; color: #6fe2a8; }
      .off { background: #321a1a; color: #e29a9a; }
      button { background: #2563eb; color: #fff; border: 0; border-radius: 8px; padding: 10px 12px; margin: 4px 6px 4px 0; font-size: 14px; }
      button.secondary { background: #2b3340; }
      a.support-link { color: #6aa9ff; display: block; margin: 6px 0; text-decoration: none; }
      pre { background: #0b0e12; border: 1px solid #232a35; border-radius: 8px; padding: 12px; overflow: auto; white-space: pre-wrap; word-break: break-word; font-size: 12px; }
    </style>
  </head>
  <body>
    <header>
      <h1>ObsidianPay · Mobile Support Portal</h1>
      <small>Central de Suporte — atendimento e diagnóstico do app</small>
    </header>
    <main>
      <div class="card">
        <div class="muted">Tópico atual</div>
        <div id="topic">${topic}</div>
        <div class="muted" style="margin-top:8px">Mensagem</div>
        <div id="message">${message}</div>
      </div>

      <div class="card">
        <div>Status do app: <span id="bridgeStatus" class="pill off">verificando…</span></div>
        <div class="muted" id="bridgeHint">Abra esta página dentro do app ObsidianPay para recursos de suporte assistido.</div>
      </div>

      <div class="card">
        <div class="muted" style="margin-bottom:6px">Diagnóstico assistido</div>
        <button onclick="showBridgeInfo()">Show bridge info</button>
        <button class="secondary" onclick="showSessionSummary()">Show session summary</button>
        <button class="secondary" onclick="showCachedConfig()">Show cached config</button>
        <pre id="out">—</pre>
      </div>

      <div class="card">
        <div class="muted" style="margin-bottom:6px">Links de suporte</div>
        <a class="support-link" href="?topic=billing">Cobrança e faturas</a>
        <a class="support-link" href="?topic=transfers">Transferências e Pix</a>
        <a class="support-link" href="?topic=security">Segurança da conta</a>
      </div>

      <p class="muted">Ambiente local apenas (lab). Nenhum dado é enviado automaticamente.</p>
    </main>

    <script>
      function hasBridge() { return typeof window.ObsidianBridge !== 'undefined'; }
      function render(label, value) {
        var out = document.getElementById('out');
        out.textContent = label + '\\n' + value;
      }
      function safeCall(label, fn) {
        if (!hasBridge()) { render(label, 'Mobile bridge unavailable (abra no app).'); return; }
        try { render(label, fn()); } catch (e) { render(label, 'erro: ' + e); }
      }
      function showBridgeInfo() { safeCall('bridgeInfo', function () { return window.ObsidianBridge.getBridgeInfo(); }); }
      function showSessionSummary() { safeCall('sessionSummary', function () { return window.ObsidianBridge.getSessionSummary(); }); }
      function showCachedConfig() { safeCall('cachedConfig', function () { return window.ObsidianBridge.getCachedConfig(); }); }

      (function initBridgeStatus() {
        var el = document.getElementById('bridgeStatus');
        var hint = document.getElementById('bridgeHint');
        if (hasBridge()) {
          el.textContent = 'Mobile bridge available';
          el.className = 'pill ok';
          hint.textContent = 'Recursos de suporte assistido estão disponíveis neste dispositivo.';
        } else {
          el.textContent = 'Standalone (sem app)';
          el.className = 'pill off';
        }
      })();
    </script>
  </body>
</html>`
    );
});

// --- Legacy route disclosure -------------------------------------------------
app.get('/api/mobile/legacy/routes', requireAuth, (_req, res) => {
  res.json({
    note: 'Internal/legacy and planned mobile routes.',
    routes: [
      '/api/mobile/support/sync',
      '/api/mobile/support/diagnostics',
      '/api/mobile/webview/support',
      '/api/mobile/transfer/preview',
      '/api/mobile/internal/vault-status',
      '/api/mobile/internal/device-trust',
      '/api/mobile/internal/reverse-hint',
      '/api/mobile/internal/environment-report',
      '/api/mobile/internal/vault-mobile/status',
      '/api/mobile/internal/vault-mobile/unlock',
      '/api/mobile/internal/network-profile',
      '/api/mobile/internal/app-integrity',
    ],
  });
});

// --- Internal vault status (role gate) ---------------------------------------
// customer -> 403. analyst/operator -> different statuses. Future use:
// biometric / root / binary-patching chains.
app.get('/api/mobile/internal/vault-status', requireAuth, (req, res) => {
  const role = req.authUser.role;
  if (role === 'customer') {
    return sendError(res, 403, 'forbidden', 'Vault status is restricted to internal roles.');
  }
  const status = vaultStatusByRole[role];
  if (!status) {
    return sendError(res, 403, 'forbidden', 'Role not permitted for vault status.');
  }
  res.json(status);
});

// --- Internal legacy device trust (Phase 8) ----------------------------------
// Weak, forgeable local request signing. The signature is a plain SHA-1 over
// predictable fields joined with a HARDCODED salt that the mobile client also
// embeds (fragmented) in security/HardcodedSecrets.kt. No HMAC, no nonce. This is
// intentionally weak for the reverse-engineering trail. No flags are returned.
function expectedLegacySignature(username, deviceId, timestamp) {
  const base = `${username}:${deviceId}:${timestamp}:${legacyMobileTrust.signingSalt}`;
  return crypto.createHash('sha1').update(base, 'utf8').digest('hex');
}

app.post('/api/mobile/internal/device-trust', requireAuth, (req, res) => {
  const username = req.authUser.username;
  const clientId = req.headers['x-obsidian-client'];
  const deviceId = req.headers['x-obsidian-device'];
  const timestamp = req.headers['x-obsidian-timestamp'];
  const signature = req.headers['x-obsidian-signature'];

  if (clientId !== legacyMobileTrust.internalClientId) {
    return sendError(res, 403, 'forbidden', 'Unknown or missing legacy mobile client.');
  }
  if (!deviceId || !timestamp || !signature) {
    return sendError(res, 400, 'bad_request', 'Missing legacy trust headers.');
  }

  const expected = expectedLegacySignature(username, deviceId, timestamp);
  if (String(signature).toLowerCase() !== expected) {
    return sendError(res, 403, 'forbidden', 'Invalid legacy device-trust signature.');
  }

  res.json({
    status: 'trusted-legacy',
    mode: 'legacy-attestation',
    user: username,
    deviceId,
    trustLevel: 'support-diagnostics',
    nextStepHint: 'review local operator hint and mobile config',
  });
});

// --- Internal environment report (Phase 9) -----------------------------------
// Receives the on-device root/emulator risk report. Policy is monitor-only:
// the app is never blocked even if root/emulator signals are present.
// The check is advisory and entirely client-side — a didactic teaching seam.
app.post('/api/mobile/internal/environment-report', requireAuth, (req, res) => {
  if (!environmentConfig.enableEnvironmentChecks) {
    return sendError(res, 503, 'disabled', 'Environment checks are not enabled.');
  }

  const {
    root,
    emulator,
    rootScore,
    emulatorScore,
    riskLevel,
    signals,
    bypassHintId,
  } = req.body || {};

  const effectiveRisk = typeof riskLevel === 'string' ? riskLevel : 'unknown';
  const environmentStatus = effectiveRisk === 'high' ? 'review-required' : 'accepted';

  res.json({
    status: 'received',
    environmentStatus,
    riskLevel: effectiveRisk,
    serverPolicy: 'monitor-only',
    nextStepHint: 'client-side checks are advisory in this lab',
  });
});

// Reverse-hint: gated only by the correct legacy client id header. Returns a
// short didactic hint (no flag).
app.get('/api/mobile/internal/reverse-hint', requireAuth, (req, res) => {
  if (req.headers['x-obsidian-client'] !== legacyMobileTrust.internalClientId) {
    return sendError(res, 403, 'forbidden', 'Reverse hint requires the legacy mobile client id.');
  }
  res.json({
    hint: 'Legacy mobile clients assemble trust headers locally.',
    mode: 'legacy-attestation',
  });
});

// --- Mobile vault status (Phase 10) ------------------------------------------
// Returns the current vault policy to the mobile client. The server never
// independently verifies biometric state — it only describes what auth methods
// are allowed. Actual lock/unlock decisions are fully client-side (teaching seam).
app.get('/api/mobile/internal/vault-mobile/status', requireAuth, (_req, res) => {
  if (!mobileVaultConfig.enableMobileVault) {
    return sendError(res, 503, 'disabled', 'Mobile vault is not enabled.');
  }
  res.json({
    status: 'locked',
    policy: 'local-auth-required',
    allowedMethods: ['biometric', 'fallback-pin'],
    serverTrust: 'client-asserted',
  });
});

// --- Mobile vault unlock (Phase 10) ------------------------------------------
// Intentionally weak gate: if the client asserts localAuth===true the server
// grants vault access without any independent check. The teaching point is that
// server trust must not be delegated to a client-side boolean. Bypassing this
// endpoint requires only setting localAuth=true in the request body — trivially
// achievable by hooking the app or crafting a raw request.
app.post('/api/mobile/internal/vault-mobile/unlock', requireAuth, (req, res) => {
  if (!mobileVaultConfig.enableMobileVault) {
    return sendError(res, 503, 'disabled', 'Mobile vault is not enabled.');
  }

  const { localAuth, method, bypassHintId } = req.body || {};

  if (localAuth !== true) {
    return sendError(
      res,
      403,
      'forbidden',
      'Vault unlock requires a successful local authentication assertion.',
    );
  }

  res.json({
    status: 'vault-access-granted',
    method: typeof method === 'string' ? method : 'unknown',
    serverTrust: 'client-asserted',
    nextStepHint: 'server trusts local auth assertion in this lab',
  });
});

// --- Network security profile (Phase 11) -------------------------------------
// Returns the active network-security posture for the mobile client: pinning
// mode ("report-only" in this lab), cleartext policy and bypass hint IDs.
// Auth required — students must obtain a valid token first.
// No flags, no credentials in the response.
// nextStepHint: "configure the app base URL to reach the lab API from emulator or phone"
app.get('/api/mobile/internal/network-profile', requireAuth, (_req, res) => {
  if (!networkProfileConfig.enableNetworkProfile) {
    return sendError(res, 503, 'disabled', 'Network profile endpoint is not enabled.');
  }
  // pinningMode is "report-only" for the local lab (cleartext HTTP only).
  res.json({
    status: 'ok',
    profile: 'burp-proxy-ready',
    pinningMode: networkProfileConfig.pinningMode,   // "report-only"
    cleartextAllowed: networkProfileConfig.cleartextAllowed,
    defaultEmulatorBaseUrl: networkProfileConfig.defaultEmulatorBaseUrl,
    phoneLanExample: networkProfileConfig.phoneLanExample,
    bypassHintIds: [
      'trust-user-ca',
      'okhttp-certificate-pinner-hook',
      'network-config-cleartext-override',
    ],
    nextStepHint: networkProfileConfig.note,
    // configure the app base URL to reach the lab API from emulator or phone
  });
});

// --- App integrity attestation (Phase 12) ------------------------------------
// Receives the client's NativeGate + TamperCheck integrity report.
// Policy is "report-only": the server never blocks based on client-reported
// values. The teaching point is that client-asserted integrity is always
// patchable — hooking or patching the app can produce any desired report.
// No flags, no credentials returned.
app.post('/api/mobile/internal/app-integrity', requireAuth, (req, res) => {
  if (!appIntegrityConfig.enableAppIntegrity) {
    return sendError(res, 503, 'disabled', 'App integrity endpoint is not enabled.');
  }

  const {
    tamperScore,
    debuggable,
    installerPackage,
    packageNameStatus,
    signatureHashPreview,
    nativeLibraryLoaded,
    nativeGateStatus,
    bypassHintIds,
  } = req.body || {};

  const score = typeof tamperScore === 'number' ? tamperScore : 0;
  // High tamper score triggers review-required; low score is accepted.
  // Either way the server does not block — report-only policy.
  const integrityDecision = score >= 50 ? 'review-required' : 'accepted';

  res.json({
    status: 'received',
    integrityDecision,
    integrityPolicy: appIntegrityConfig.integrityPolicy,       // "report-only"
    nativeGatePolicy: appIntegrityConfig.nativeGatePolicy,     // "fallback-allowed"
    serverTrust: 'client-asserted-integrity',
    nextStepHint: 'client-side integrity checks are patchable in this lab',
  });
});

// --- Error handling ----------------------------------------------------------
app.use((_req, res) => {
  sendError(res, 404, 'not_found', 'Unknown endpoint.');
});

// eslint-disable-next-line no-unused-vars
app.use((err, _req, res, _next) => {
  if (err && err.type === 'entity.parse.failed') {
    return sendError(res, 400, 'bad_request', 'Malformed JSON body.');
  }
  console.error('[error]', err && err.message ? err.message : err);
  sendError(res, 500, 'internal_error', 'Something went wrong.');
});

const server = app.listen(PORT, HOST, () => {
  console.log(`ObsidianPay Mobile API (${LAB.version}) listening on http://${HOST}:${PORT}`);
  console.log('Local lab environment only. Do not expose outside 127.0.0.1.');
});

for (const sig of ['SIGINT', 'SIGTERM']) {
  process.on(sig, () => {
    console.log(`Received ${sig}, shutting down.`);
    server.close(() => process.exit(0));
  });
}

module.exports = app;
