const allowedHosts = [
  "api-core.internal",
  "files-vault.internal",
  "legacy-panel.internal",
  "audit-db.internal"
];

const internalRoutes = new Map([
  ["api-core.internal/health", {
    status: 200,
    body: {
      service: "api-core",
      status: "ok",
      network: "internal"
    }
  }],
  ["api-core.internal/metadata", {
    status: 200,
    body: {
      service: "api-core",
      trust_boundary: "gateway-origin",
      accepted_headers: [
        "X-BG-Context",
        "X-Gateway-Origin",
        "X-Internal-Trace"
      ],
      upstreams: [
        "files-vault.internal",
        "legacy-panel.internal"
      ],
      finding: "Gateway-originated requests expose internal metadata.",
      flag: "FLAG{blackgate_gateway_trust_ssrf_phase4}",
      next_hint: "files-vault metadata references controlled document paths for phase 5."
    }
  }],
  ["api-core.internal/routes", {
    status: 200,
    body: {
      service: "api-core",
      routes: [
        "/health",
        "/metadata",
        "/internal/files/health",
        "/internal/files/catalog"
      ],
      note: "File catalog is proxied through files-vault.internal."
    }
  }],
  ["files-vault.internal/health", {
    status: 200,
    body: {
      service: "files-vault",
      status: "ok",
      network: "internal"
    }
  }],
  ["files-vault.internal/metadata", {
    status: 200,
    body: {
      service: "files-vault",
      status: "restricted",
      storage_mode: "document-catalog",
      safe_paths: [
        "/public/policies/access-policy.txt",
        "/public/notices/migration-note.txt"
      ],
      restricted_paths: [
        "/restricted/operator-notes.txt",
        "/restricted/phase5-seed.txt"
      ],
      next_hint: "File read controls are planned for phase 5."
    }
  }],
  ["legacy-panel.internal/health", {
    status: 200,
    body: {
      service: "legacy-panel",
      status: "maintenance",
      network: "internal"
    }
  }],
  ["legacy-panel.internal/metadata", {
    status: 200,
    body: {
      service: "legacy-panel",
      status: "maintenance",
      note: "Legacy rendering has been disabled until migration review.",
      planned: [
        "/legacy/render",
        "/legacy/report"
      ]
    }
  }],
  ["audit-db.internal/health", {
    status: 200,
    body: {
      service: "audit-db",
      status: "filtered",
      network: "internal",
      note: "Direct metadata access blocked by gateway policy."
    }
  }],
  ["audit-db.internal/metadata", {
    status: 403,
    body: {
      error: "upstream_forbidden",
      message: "Direct metadata access blocked by gateway policy."
    }
  }]
]);

function normalizeInternalUrl(input) {
  let parsed;

  try {
    parsed = new URL(String(input || ""));
  } catch (error) {
    return {
      ok: false,
      status: 400,
      error: "bad_request",
      message: "Valid url parameter is required."
    };
  }

  if (parsed.protocol !== "http:") {
    return {
      ok: false,
      status: 400,
      error: "bad_request",
      message: "Valid url parameter is required."
    };
  }

  const host = parsed.hostname.toLowerCase();

  if (!allowedHosts.includes(host)) {
    return {
      ok: false,
      status: 403,
      error: "blocked_upstream",
      message: "Only internal BlackGate upstreams are reachable from this training gateway."
    };
  }

  const pathname = parsed.pathname.replace(/\/{2,}/g, "/").replace(/\/$/, "") || "/health";

  return {
    ok: true,
    url: `http://${host}${pathname}`,
    host,
    path: pathname
  };
}

function resolveInternalService(inputUrl) {
  const normalized = normalizeInternalUrl(inputUrl);

  if (!normalized.ok) {
    return normalized;
  }

  const key = `${normalized.host}${normalized.path}`;
  const upstream = internalRoutes.get(key);

  if (!upstream) {
    return {
      ok: false,
      status: 404,
      error: "upstream_not_found",
      message: "Internal upstream route not found.",
      requested_url: normalized.url
    };
  }

  return {
    ok: true,
    requested_url: normalized.url,
    status: upstream.status,
    body: upstream.body
  };
}

function listInternalServices() {
  return [...allowedHosts];
}

module.exports = {
  normalizeInternalUrl,
  resolveInternalService,
  listInternalServices
};
