const express = require("express");
const jwt = require("jsonwebtoken");
const cookieParser = require("cookie-parser");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const { createClient } = require("redis");

const app = express();

const PORT = Number(process.env.PORT || 3000);
const JWT_SECRET = process.env.JWT_SECRET || "sentinelcore-dev-2026";
const INTERNAL_ADMIN_URL = process.env.INTERNAL_ADMIN_URL || "http://internal-admin:8081";
const REDIS_URL = process.env.REDIS_URL || "redis://redis:6379";
const SHARED_DIR = "/shared";

const redis = createClient({ url: REDIS_URL });

const users = [
  {
    id: 1,
    username: "intern",
    password: "intern2026",
    role: "viewer",
    displayName: "SOC Intern"
  },
  {
    id: 2,
    username: "analyst",
    password: "bluewatch-disabled",
    role: "analyst",
    displayName: "Security Analyst"
  },
  {
    id: 3,
    username: "admin",
    password: "not_available_here",
    role: "admin",
    displayName: "Sentinel Admin"
  }
];

const profileExtras = new Map();

const alerts = [
  {
    id: 7412,
    owner_id: 1,
    title: "VPN login from new ASN",
    severity: "medium",
    status: "triage",
    evidence: "The event belongs to the intern training tenant.",
    analyst_note: "Compare list responses with direct object reads."
  },
  {
    id: 7468,
    owner_id: 1,
    title: "EDR quarantine summary",
    severity: "low",
    status: "closed",
    evidence: "No malicious payload recovered.",
    analyst_note: "Dashboard links intentionally show only common APIs."
  },
  {
    id: 7391,
    owner_id: 2,
    title: "Legacy diagnostic secret fragment observed",
    severity: "high",
    status: "open",
    evidence: "flag{bola_alert_cross_tenant}",
    analyst_note: "Old debug health checks referenced auth material as sentinelcore-dev-****."
  },
  {
    id: 7524,
    owner_id: 3,
    title: "Internal admin dependency reachable from API network",
    severity: "critical",
    status: "restricted",
    evidence: "Internal route metadata should never be exposed to browser-driven checks.",
    analyst_note: "Service name appears in static bundle and operational logs."
  }
];

app.use(express.urlencoded({ extended: false }));
app.use(express.json({ limit: "128kb" }));
app.use(cookieParser());
app.use("/static", express.static(path.join(__dirname, "public")));

function signToken(user) {
  return jwt.sign(
    {
      id: user.id,
      username: user.username,
      role: user.role,
      scope: [`${user.role}:read`, "alerts:read"]
    },
    JWT_SECRET,
    { expiresIn: "3h" }
  );
}

function issueToken(res, user) {
  const token = signToken(user);

  res.cookie("token", token, {
    httpOnly: true,
    sameSite: "lax"
  });

  return token;
}

function getCurrentUser(req) {
  const bearer = req.headers.authorization || "";
  const token = req.cookies.token || (bearer.startsWith("Bearer ") ? bearer.slice("Bearer ".length) : "");

  if (!token) {
    return null;
  }

  try {
    return jwt.verify(token, JWT_SECRET);
  } catch {
    return null;
  }
}

function requireAuth(req, res, next) {
  const user = getCurrentUser(req);

  if (!user) {
    return res.status(401).json({ ok: false, error: "authentication required" });
  }

  req.user = user;
  return next();
}

function requireDashboardAuth(req, res, next) {
  const user = getCurrentUser(req);

  if (!user) {
    return res.redirect("/");
  }

  req.user = user;
  return next();
}

function hasRole(user, roles) {
  return user && roles.includes(user.role);
}

function requireAnyRole(roles) {
  return (req, res, next) => {
    if (!hasRole(req.user, roles)) {
      return res.status(403).json({ ok: false, error: `${roles.join(" or ")} role required` });
    }

    return next();
  };
}

function getRawQueryValue(originalUrl, key) {
  const query = originalUrl.split("?")[1] || "";
  const parts = query.split("&");
  const prefix = `${key}=`;
  const match = parts.find((part) => part.startsWith(prefix));

  if (!match) {
    return "";
  }

  return match.slice(prefix.length);
}

function htmlPage(title, body) {
  return `<!doctype html>
<html lang="pt-BR">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title}</title>
  <link rel="stylesheet" href="/static/style.css">
</head>
<body>
${body}
<script src="/static/js/sentinel.bundle.js"></script>
</body>
</html>`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

app.get("/", (req, res) => {
  const user = getCurrentUser(req);

  if (user) {
    return res.redirect("/dashboard");
  }

  return res.send(htmlPage("SentinelCore", `
  <main class="auth-shell">
    <section class="auth-brief">
      <p class="eyebrow">Lab 04 - SentinelCore</p>
      <h1>SentinelCore</h1>
      <p class="lede">Threat Operations Console</p>
      <p class="muted">Internal SOC console for alerts, evidence, integrations and asynchronous jobs.</p>
      <div class="signal-row" aria-label="Operational status">
        <span class="chip cyan">session: gated</span>
        <span class="chip amber">tenant: ACME-SOC</span>
        <span class="chip violet">pipeline: monitored</span>
      </div>
    </section>

    <section class="login-card">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Secure access</p>
          <h2>Console login</h2>
        </div>
        <span class="status-dot" aria-hidden="true"></span>
      </div>
      <form method="post" action="/login" class="form-stack">
        <label>
          Usuario
          <input name="username" autocomplete="off" required>
        </label>
        <label>
          Senha
          <input name="password" type="password" autocomplete="off" required>
        </label>
        <button type="submit">Entrar na console</button>
      </form>
      <p class="credential-note">Credencial inicial do lab: <code>intern / intern2026</code></p>
    </section>
  </main>`));
});

app.post("/login", (req, res) => {
  const username = String(req.body.username || "");
  const password = String(req.body.password || "");
  const user = users.find((item) => item.username === username && item.password === password);

  if (!user) {
    return res.status(401).send(htmlPage("Login failed", `
  <main class="shell narrow">
    <section class="panel">
      <p class="eyebrow">Falha de autenticacao</p>
      <h1>Credenciais invalidas</h1>
      <p>Revise usuario e senha e tente novamente.</p>
      <a class="button" href="/">Voltar</a>
    </section>
  </main>`));
  }

  issueToken(res, user);
  return res.redirect("/dashboard");
});

app.get("/logout", (req, res) => {
  res.clearCookie("token");
  return res.redirect("/");
});

app.get("/dashboard", requireDashboardAuth, (req, res) => {
  const username = escapeHtml(req.user.username);
  const role = escapeHtml(req.user.role);
  const roleNote = req.user.role === "admin"
    ? "Acesso administrativo detectado. Alguns workflows internos continuam fora da navegacao."
    : "Nem todo fluxo interno aparece no menu. Analise bundle, requisicoes e respostas da API.";

  return res.send(htmlPage("SentinelCore Dashboard", `
  <main class="ops-shell">
    <header class="ops-header">
      <div class="brand-lockup">
        <p class="eyebrow">Threat Operations Console</p>
        <h1>SentinelCore</h1>
        <p class="muted">Central de triagem para alertas, evidencias e workflows assincronos.</p>
      </div>
      <div class="session-panel">
        <span class="chip cyan">session: active</span>
        <span class="chip violet">user: ${username}</span>
        <span class="chip amber">role: ${role}</span>
        <a class="button secondary" href="/logout">Sair</a>
      </div>
    </header>

    <section class="status-grid" aria-label="Operational telemetry">
      <div class="status-cell">
        <span>tenant</span>
        <strong>ACME-SOC</strong>
      </div>
      <div class="status-cell">
        <span>pipeline</span>
        <strong>nominal</strong>
      </div>
      <div class="status-cell">
        <span>queue</span>
        <strong>internal</strong>
      </div>
      <div class="status-cell">
        <span>scope</span>
        <strong>local</strong>
      </div>
    </section>

    <section class="ops-grid">
      <article class="ops-card">
        <div class="card-kicker">Detection</div>
        <h2>Fluxo de alertas</h2>
        <p>Revise o stream visivel do tenant e compare respostas em nivel de objeto.</p>
        <a class="card-link" href="/api/v2/alerts">/api/v2/alerts</a>
      </article>
      <article class="ops-card">
        <div class="card-kicker">Access</div>
        <h2>Claims de identidade</h2>
        <p>Inspecione claims, role efetiva e limites de permissao da sessao atual.</p>
        <a class="card-link" href="/api/v2/me">/api/v2/me</a>
      </article>
      <article class="ops-card">
        <div class="card-kicker">Telemetry</div>
        <h2>Telemetria do frontend</h2>
        <p>O bundle estatico guarda metadados operacionais usados em troubleshooting.</p>
        <a class="card-link" href="/static/js/sentinel.bundle.js">sentinel.bundle.js</a>
      </article>
    </section>

    <section class="ops-note">
      <div>
        <p class="eyebrow">Operational notes</p>
        <h2>Mapa de sinais</h2>
      </div>
      <p>${roleNote}</p>
      <div class="signal-row">
        <span class="chip">DevTools</span>
        <span class="chip">Burp</span>
        <span class="chip">API diff</span>
        <span class="chip">Bundle review</span>
      </div>
    </section>
  </main>`));
});

app.get("/api/v2/me", requireAuth, (req, res) => {
  const extras = profileExtras.get(req.user.id) || {};

  return res.json({
    id: req.user.id,
    username: req.user.username,
    role: req.user.role,
    scope: req.user.scope,
    profile: extras
  });
});

app.get("/api/v2/alerts", requireAuth, (req, res) => {
  const visibleAlerts = alerts
    .filter((alert) => alert.owner_id === req.user.id)
    .map((alert) => ({
      id: alert.id,
      title: alert.title,
      severity: alert.severity,
      status: alert.status
    }));

  return res.json({ ok: true, alerts: visibleAlerts });
});

app.get("/api/v2/alerts/:id", requireAuth, (req, res) => {
  const hasConsoleContext = req.headers["x-sentinel-client"] === "web-console"
    && req.headers["x-tenant-scope"] === "ACME-SOC";

  if (!hasConsoleContext) {
    return res.status(404).json({ ok: false, error: "alert unavailable" });
  }

  const alert = alerts.find((item) => item.id === Number(req.params.id));

  if (!alert) {
    return res.status(404).json({ ok: false, error: "alert unavailable" });
  }

  return res.json({ ok: true, alert });
});

app.patch("/api/v2/me/profile", requireAuth, (req, res) => {
  const blocked = new Set(["id", "username", "password"]);
  const profile = profileExtras.get(req.user.id) || {};
  const ignored = [];
  const directRole = Object.prototype.hasOwnProperty.call(req.body || {}, "role")
    ? String(req.body.role)
    : "";

  if (directRole === "admin") {
    return res.status(403).json({
      ok: false,
      error: "direct admin role assignment is blocked",
      ignored
    });
  }

  for (const [key, value] of Object.entries(req.body || {})) {
    if (blocked.has(key) || key === "role") {
      ignored.push(key);
      continue;
    }

    profile[key] = value;
  }

  profileExtras.set(req.user.id, profile);

  const requestedRole = req.body
    && req.body.access
    && typeof req.body.access === "object"
    ? String(req.body.access.requestedRole || "")
    : "";

  if (requestedRole === "admin") {
    return res.status(403).json({
      ok: false,
      error: "admin role request requires approval",
      ignored
    });
  }

  if (requestedRole === "analyst") {
    const promotedUser = {
      id: req.user.id,
      username: req.user.username,
      role: "analyst"
    };

    issueToken(res, promotedUser);

    return res.json({
      ok: true,
      message: "profile updated",
      effectiveRole: "analyst",
      ignored,
      profile,
      flag: "flag{mass_assignment_analyst_role}"
    });
  }

  return res.json({
    ok: true,
    message: "profile updated",
    effectiveRole: req.user.role,
    ignored,
    profile
  });
});

app.get("/api/v2/debug/health", requireAuth, requireAnyRole(["analyst", "admin"]), (req, res) => {
  return res.json({
    status: "ok",
    service: "sentinelcore-api",
    build: "2026.05.lab04",
    diagnostic: "Loaded auth secret: sentinelcore-dev-****",
    flag: "flag{debug_secret_fragment_disclosed}"
  });
});

app.get("/api/v2/artifacts/build-manifest", requireAuth, requireAnyRole(["analyst", "admin"]), (req, res) => {
  const manifestPath = path.join(__dirname, "data", "build-manifest.old.json");
  const manifest = fs.readFileSync(manifestPath, "utf8");

  res.type("application/json");
  return res.send(manifest);
});

app.get("/api/v2/admin/final", requireAuth, requireAnyRole(["admin"]), (req, res) => {
  return res.json({
    ok: true,
    message: "Admin area reached. The final flag is on disk and requires a read primitive.",
    flag: "flag{jwt_forged_admin_access}",
    hint: "Look for diagnostics or export features that read files."
  });
});

app.post("/api/v2/integrations/check", requireAuth, requireAnyRole(["analyst", "admin"]), async (req, res) => {
  const targetUrl = String(req.body.url || "");
  const blocked = ["localhost", "127.0.0.1", "0.0.0.0"];
  const lower = targetUrl.toLowerCase();

  if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
    return res.status(400).json({ ok: false, error: "only http and https are supported" });
  }

  if (blocked.some((value) => lower.includes(value))) {
    return res.status(403).json({ ok: false, error: "blocked host" });
  }

  try {
    const response = await fetch(targetUrl, {
      method: "GET",
      redirect: "manual",
      signal: AbortSignal.timeout(3500)
    });
    const body = await response.text();

    return res.json({
      ok: true,
      status: response.status,
      headers: Object.fromEntries(response.headers.entries()),
      body: body.slice(0, 2000)
    });
  } catch (error) {
    return res.status(502).json({
      ok: false,
      error: "integration check failed",
      detail: error.message
    });
  }
});

app.post("/api/v2/integrations/proxy", requireAuth, requireAnyRole(["admin"]), async (req, res) => {
  const targetUrl = String(req.body.url || "");
  const headers = req.body.headers && typeof req.body.headers === "object" ? req.body.headers : {};
  const lower = targetUrl.toLowerCase();

  if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
    return res.status(400).json({ ok: false, error: "only http and https are supported" });
  }

  try {
    const response = await fetch(targetUrl, {
      method: "GET",
      redirect: "manual",
      headers,
      signal: AbortSignal.timeout(3500)
    });
    const body = await response.text();

    return res.json({
      ok: true,
      status: response.status,
      headers: Object.fromEntries(response.headers.entries()),
      body: body.slice(0, 4000)
    });
  } catch (error) {
    return res.status(502).json({
      ok: false,
      error: "proxy request failed",
      detail: error.message
    });
  }
});

app.post("/api/v2/reports/render", requireAuth, requireAnyRole(["admin"]), (req, res) => {
  const title = String(req.body.title || "Untitled SentinelCore Report");
  const template = String(req.body.template || "");
  const context = {
    "{{user.username}}": req.user.username,
    "{{user.role}}": req.user.role,
    "{{internal.url}}": INTERNAL_ADMIN_URL,
    "{{config.jwt_hint}}": JWT_SECRET,
    "{{config.internal_token_hint}}": "internal-admin-token-7f3a9c21"
  };

  let rendered = template;

  for (const [placeholder, value] of Object.entries(context)) {
    rendered = rendered.split(placeholder).join(value);
  }

  return res.json({
    ok: true,
    title,
    rendered,
    flag: template.includes("{{config.jwt_hint}}") ? "flag{template_context_leaked}" : undefined
  });
});

app.post("/api/v2/jobs", requireAuth, requireAnyRole(["admin"]), async (req, res) => {
  const job = {
    id: `job-${Date.now()}-${crypto.randomBytes(3).toString("hex")}`,
    type: String(req.body.type || "report.export"),
    source: String(req.body.source || "daily-summary"),
    output: String(req.body.output || "report.txt")
  };

  await redis.lPush("sentinel:jobs", JSON.stringify(job));

  return res.json({
    ok: true,
    message: "job queued",
    queue: "sentinel:jobs",
    job
  });
});

app.get("/api/v2/jobs/output", requireAuth, requireAnyRole(["admin"]), (req, res) => {
  const requested = String(req.query.file || "");

  if (!requested) {
    return res.status(400).json({ ok: false, error: "file query parameter is required" });
  }

  const outputPath = path.join(SHARED_DIR, path.basename(requested));

  try {
    const content = fs.readFileSync(outputPath, "utf8");
    res.type("text/plain");
    return res.send(content);
  } catch {
    return res.status(404).json({ ok: false, error: "output not found" });
  }
});

app.get("/api/v2/admin/diagnostics/read", requireAuth, requireAnyRole(["admin"]), (req, res) => {
  const rawFile = getRawQueryValue(req.originalUrl, "file");

  if (!rawFile) {
    return res.status(400).json({ ok: false, error: "file query parameter is required" });
  }

  if (rawFile.includes("../")) {
    return res.status(403).json({ ok: false, error: "blocked traversal token" });
  }

  let decoded;

  try {
    decoded = decodeURIComponent(rawFile);
  } catch {
    return res.status(400).json({ ok: false, error: "invalid encoding" });
  }

  const baseDir = path.join(__dirname, "logs");
  const targetPath = path.join(baseDir, decoded);

  try {
    const content = fs.readFileSync(targetPath, "utf8");
    res.type("text/plain");
    return res.send(content);
  } catch {
    return res.status(404).json({
      ok: false,
      error: "file not found",
      attemptedBase: baseDir
    });
  }
});

app.use((req, res) => {
  return res.status(404).json({ ok: false, error: "not found" });
});

redis.connect()
  .then(() => {
    app.listen(PORT, () => {
      console.log(`SentinelCore running on port ${PORT}`);
    });
  })
  .catch((error) => {
    console.error("Redis connection failed", error);
    process.exit(1);
  });
