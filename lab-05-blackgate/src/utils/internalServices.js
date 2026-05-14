const path = require("path");

const allowedHosts = [
  "api-core.internal",
  "files-vault.internal",
  "legacy-panel.internal",
  "audit-db.internal"
];

const filesVaultDocuments = {
  "/public/policies/access-policy.txt": "BlackGate Access Policy: operator-mediated gateway access is required for internal document retrieval.",
  "/public/notices/migration-note.txt": "Migration note: files-vault is moving from named downloads to path-based reads. Legacy path compatibility remains enabled during transition.",
  "/public/runbooks/gateway-checklist.txt": "Gateway checklist: verify /metadata, /catalog, and controlled /read paths before enabling files-vault migration.",
  "/restricted/operator-notes.txt": "Operator notes: restricted seeds are stored outside /public but legacy compatibility checks should prevent direct reads.",
  "/restricted/phase5-seed.txt": "FLAG{blackgate_files_vault_controlled_read_phase5}\nNext: legacy panel migration may reuse operational credentials in phase 6.",
  "/restricted/legacy-migration-notes.txt": "Legacy migration note:\nThe public BlackGate login and the legacy maintenance panel do not share the same identity provider.\nSome operator migration accounts were mirrored only for gateway-originated maintenance checks.\nDo not trust the first credential block in archived notes.",
  "/restricted/operator-archive-2026.txt": "Archived operator notes:\ncandidate: operator / operator123\ncandidate: admin / admin\ncandidate: bg_admin / blackgate\ncandidate: svc_audit / audit2026\nStatus: most archived candidates are identity-provider accounts, not legacy maintenance accounts.",
  "/restricted/credential-review.txt": "Credential review:\n- public operator account remains valid only on the public console.\n- svc_audit was disabled after audit-db filtering.\n- bg_admin is a placeholder in old screenshots.\n- migration service accounts use the legacy realm.\n- legacy realm accepts username format svc_migration, not email format.",
  "/restricted/legacy-panel-creds.txt": "Legacy credential archive:\n[deprecated]\noperator:operator123\nadmin:admin\nbg_admin:blackgate\n\n[disabled]\nsvc_audit:audit2026\nsvc_backup:backup2026\n\n[maintenance-realm]\nsvc_migration:migrate-yellow-gate\n\nNote:\nMaintenance realm credentials are not valid on the public login page.\nGateway-originated panel checks require the maintenance realm."
};

const downloadAliases = {
  "access-policy.txt": "/public/policies/access-policy.txt",
  "migration-note.txt": "/public/notices/migration-note.txt",
  "gateway-checklist.txt": "/public/runbooks/gateway-checklist.txt"
};

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
        "/internal/files/catalog",
        "/internal/files/read"
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
      catalog: "/catalog",
      read_endpoint: "/read?path=/public/notices/migration-note.txt",
      download_endpoint: "/download?file=migration-note.txt",
      safe_paths: [
        "/public/policies/access-policy.txt",
        "/public/notices/migration-note.txt",
        "/public/runbooks/gateway-checklist.txt"
      ],
      restricted_paths: [
        "/restricted/operator-notes.txt",
        "/restricted/phase5-seed.txt"
      ],
      path_policy: "Public documents are normalized before access checks.",
      next_hint: "Some legacy path checks compare raw input before normalization."
    }
  }],
  ["files-vault.internal/catalog", {
    status: 200,
    body: {
      service: "files-vault",
      documents: [
        {
          id: "access-policy",
          path: "/public/policies/access-policy.txt",
          classification: "public"
        },
        {
          id: "migration-note",
          path: "/public/notices/migration-note.txt",
          classification: "public"
        },
        {
          id: "gateway-checklist",
          path: "/public/runbooks/gateway-checklist.txt",
          classification: "public"
        }
      ],
      restricted_index: {
        enabled: false,
        note: "Restricted documents are hidden from the public catalog."
      }
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
      auth: "legacy-realm",
      public_idp: false,
      gateway_required: true,
      notes: [
        "Public console sessions are not accepted here.",
        "Maintenance realm checks are still enabled for migration.",
        "Archived credentials may contain stale entries."
      ]
    }
  }],
  ["legacy-panel.internal/status", {
    status: 200,
    body: {
      service: "legacy-panel",
      status: "maintenance",
      enabled_routes: [
        "/health",
        "/metadata",
        "/auth",
        "/maintenance"
      ],
      disabled_routes: [
        "/login",
        "/reports"
      ],
      note: "Interactive login disabled. Maintenance auth endpoint remains enabled for gateway-originated checks."
    }
  }],
  ["legacy-panel.internal/login", {
    status: 403,
    body: {
      error: "interactive_login_disabled",
      message: "Legacy interactive login is disabled during migration."
    }
  }],
  ["legacy-panel.internal/session", {
    status: 403,
    body: {
      error: "session_inspection_disabled",
      message: "Legacy session inspection is disabled during migration."
    }
  }],
  ["legacy-panel.internal/reports", {
    status: 403,
    body: {
      service: "legacy-panel",
      status: "queued-reporting-disabled",
      note: "Report generation is disabled until the maintenance queue is reviewed."
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
    url: `http://${host}${pathname}${parsed.search}`,
    host,
    path: pathname,
    searchParams: parsed.searchParams
  };
}

function normalizeDocumentPath(rawPath) {
  const normalized = path.posix.normalize(rawPath);
  return normalized.startsWith("/") ? normalized : `/${normalized}`;
}

function isAllowedLegacyTraversal(rawPath, documentPath) {
  if (documentPath.startsWith("/public/")) {
    return true;
  }

  return rawPath.startsWith("/public/../restricted/");
}

function resolveFilesVaultRead(normalized) {
  const rawPath = normalized.searchParams.get("path");

  if (!rawPath) {
    return {
      ok: false,
      status: 400,
      error: "bad_request",
      message: "path parameter is required.",
      requested_url: normalized.url
    };
  }

  if (!rawPath.startsWith("/public/")) {
    return {
      ok: false,
      status: 403,
      error: "forbidden_path",
      message: "Only public document paths are allowed by the legacy pre-check.",
      requested_url: normalized.url
    };
  }

  const documentPath = normalizeDocumentPath(rawPath);

  if (!isAllowedLegacyTraversal(rawPath, documentPath)) {
    return {
      ok: false,
      status: 403,
      error: "forbidden_path",
      message: "Only public document paths are allowed by the legacy pre-check.",
      requested_url: normalized.url
    };
  }

  const content = filesVaultDocuments[documentPath];

  if (!content) {
    return {
      ok: false,
      status: 404,
      error: "file_not_found",
      message: "Document not found.",
      requested_url: normalized.url
    };
  }

  return {
    ok: true,
    requested_url: normalized.url,
    status: 200,
    body: {
      service: "files-vault",
      path: documentPath,
      content
    }
  };
}

function resolveFilesVaultDownload(normalized) {
  const file = normalized.searchParams.get("file");

  if (!file) {
    return {
      ok: false,
      status: 400,
      error: "bad_request",
      message: "file parameter is required.",
      requested_url: normalized.url
    };
  }

  const documentPath = downloadAliases[file];

  if (!documentPath) {
    return {
      ok: false,
      status: 404,
      error: "file_not_found",
      message: "Document not found.",
      requested_url: normalized.url
    };
  }

  return {
    ok: true,
    requested_url: normalized.url,
    status: 200,
    body: {
      service: "files-vault",
      file,
      path: documentPath,
      content: filesVaultDocuments[documentPath]
    }
  };
}

function resolveLegacyPanelAuth(normalized) {
  const user = normalized.searchParams.get("user");
  const pass = normalized.searchParams.get("pass");

  if (!user || !pass) {
    return {
      ok: false,
      status: 400,
      error: "missing_credentials",
      message: "Maintenance realm credentials are required.",
      requested_url: normalized.url
    };
  }

  if (user === "operator" && pass === "operator123") {
    return {
      ok: false,
      status: 401,
      error: "wrong_realm",
      message: "Public console credentials are not valid in the maintenance realm.",
      requested_url: normalized.url
    };
  }

  if (user !== "svc_migration" || pass !== "migrate-yellow-gate") {
    return {
      ok: false,
      status: 401,
      error: "invalid_legacy_credentials",
      message: "Legacy realm authentication failed.",
      requested_url: normalized.url
    };
  }

  return {
    ok: true,
    requested_url: normalized.url,
    status: 200,
    body: {
      authenticated: true,
      realm: "maintenance",
      principal: "svc_migration",
      legacy_session: "bg6-legacy-session-migration",
      next: "/maintenance",
      note: "Legacy sessions are accepted only through gateway-originated requests."
    }
  };
}

function resolveLegacyPanelMaintenance(normalized) {
  const session = normalized.searchParams.get("session");

  if (!session) {
    return {
      ok: false,
      status: 401,
      error: "legacy_session_required",
      message: "Authenticated maintenance session required.",
      requested_url: normalized.url
    };
  }

  if (session !== "bg6-legacy-session-migration") {
    return {
      ok: false,
      status: 403,
      error: "invalid_legacy_session",
      message: "Legacy maintenance session was not accepted.",
      requested_url: normalized.url
    };
  }

  return {
    ok: true,
    requested_url: normalized.url,
    status: 200,
    body: {
      service: "legacy-panel",
      area: "maintenance",
      principal: "svc_migration",
      finding: "credential reuse across migration boundary",
      flag: "FLAG{blackgate_legacy_credential_reuse_phase6}",
      next_hint: "Maintenance reports queue jobs for asynchronous processing in the next phase."
    }
  };
}

function resolveInternalService(inputUrl) {
  const normalized = normalizeInternalUrl(inputUrl);

  if (!normalized.ok) {
    return normalized;
  }

  if (normalized.host === "files-vault.internal" && normalized.path === "/read") {
    return resolveFilesVaultRead(normalized);
  }

  if (normalized.host === "files-vault.internal" && normalized.path === "/download") {
    return resolveFilesVaultDownload(normalized);
  }

  if (normalized.host === "legacy-panel.internal" && normalized.path === "/auth") {
    return resolveLegacyPanelAuth(normalized);
  }

  if (normalized.host === "legacy-panel.internal" && normalized.path === "/maintenance") {
    return resolveLegacyPanelMaintenance(normalized);
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
