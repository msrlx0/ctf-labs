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
  "/restricted/legacy-panel-creds.txt": "Do not expose in phase 5. Reserved for phase 6."
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

  if (rawPath === "/restricted/legacy-panel-creds.txt") {
    return {
      ok: false,
      status: 403,
      error: "redacted",
      message: "Credential archive is sealed until migration approval.",
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

  if (documentPath === "/restricted/legacy-panel-creds.txt") {
    return {
      ok: false,
      status: 403,
      error: "redacted",
      message: "Credential archive is sealed until migration approval.",
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
