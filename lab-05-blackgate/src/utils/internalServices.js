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
  "/restricted/legacy-panel-creds.txt": "Legacy credential archive:\n[deprecated]\noperator:operator123\nadmin:admin\nbg_admin:blackgate\n\n[disabled]\nsvc_audit:audit2026\nsvc_backup:backup2026\n\n[maintenance-realm]\nsvc_migration:migrate-yellow-gate\n\nNote:\nMaintenance realm credentials are not valid on the public login page.\nGateway-originated panel checks require the maintenance realm.",
  "/restricted/report-workflow-notes.txt": "Report migration notes:\nQueue-only mode was added for maintenance validation.\nWorker diagnostics templates are not available in the public selector.\nInternal scope should remain restricted until the worker review is complete.\nThe maintenance worker accepts queued diagnostics but does not render them synchronously.",
  "/restricted/queue-review.txt": "Queue review:\ndefault queue: migration-report-queue\nlegacy queue: report-render\nmaintenance queue: maintenance-worker\nOnly maintenance queue jobs are reviewed by the worker team.",
  "/restricted/template-archive.txt": "Template archive:\ndaily-summary enabled\nasset-inventory enabled\nsecurity-audit disabled\nmigration-check enabled\nworker-diagnostics archived\nDo not expose archived templates in public selectors."
};

const downloadAliases = {
  "access-policy.txt": "/public/policies/access-policy.txt",
  "migration-note.txt": "/public/notices/migration-note.txt",
  "gateway-checklist.txt": "/public/runbooks/gateway-checklist.txt"
};

const legacySessionId = "bg6-legacy-session-migration";
const phase7Flag = "FLAG{blackgate_report_workflow_abuse_phase7}";
const reportJobs = new Map();

const reportTemplates = [
  {
    id: "daily-summary",
    status: "enabled",
    safe: true
  },
  {
    id: "asset-inventory",
    status: "enabled",
    safe: true
  },
  {
    id: "security-audit",
    status: "disabled",
    safe: false
  },
  {
    id: "migration-check",
    status: "enabled",
    safe: true
  },
  {
    id: "worker-diagnostics",
    status: "hidden",
    safe: false
  }
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
        "/login"
      ],
      migration_modules: [
        "maintenance reports"
      ],
      note: "Interactive login disabled. Maintenance auth endpoint remains enabled for gateway-originated checks. Queued reporting is under migration review."
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
      legacy_session: legacySessionId,
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

  if (session !== legacySessionId) {
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
      next_hint: "Maintenance reports queue jobs for asynchronous processing."
    }
  };
}

function sendUpstream(normalized, status, body) {
  return {
    ok: true,
    requested_url: normalized.url,
    status,
    body
  };
}

function requireLegacySession(normalized) {
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

  if (session !== legacySessionId) {
    return {
      ok: false,
      status: 403,
      error: "invalid_legacy_session",
      message: "Legacy maintenance session was not accepted.",
      requested_url: normalized.url
    };
  }

  return null;
}

function findReportTemplate(templateId) {
  return reportTemplates.find((template) => template.id === templateId);
}

function listReportTemplates(includeMode, auditMode) {
  const enabledTemplates = reportTemplates.filter((template) => template.status === "enabled");

  if (includeMode === "archived") {
    return {
      templates: [
        ...enabledTemplates,
        {
          id: "security-audit",
          status: "disabled",
          safe: false
        },
        {
          id: "worker-diagnostics",
          status: "archived",
          visibility: "restricted"
        }
      ],
      note: "Archive entries are incomplete outside audit review."
    };
  }

  if (includeMode === "all" && auditMode === "1") {
    return {
      templates: reportTemplates.map((template) => {
        if (template.id !== "worker-diagnostics") {
          return template;
        }

        return {
          id: template.id,
          status: "hidden",
          safe: template.safe,
          renderer: "deferred",
          visibility: "maintenance"
        };
      }),
      note: "Audit view includes migration-only records."
    };
  }

  if (includeMode === "all") {
    return {
      templates: [
        ...enabledTemplates,
        {
          id: "security-audit",
          status: "disabled",
          safe: false
        }
      ],
      note: "Full archive review is restricted."
    };
  }

  return {
    templates: enabledTemplates,
    note: "Only enabled templates are shown in the public selector."
  };
}

function makeJobId(template, queue) {
  if (template === "worker-diagnostics" && queue === "maintenance-worker") {
    return "bg7-job-worker-diagnostics";
  }

  const safeTemplate = String(template || "unknown").replace(/[^a-z0-9-]/gi, "-").toLowerCase();
  const safeQueue = String(queue || "default").replace(/[^a-z0-9-]/gi, "-").toLowerCase();
  return `bg7-job-${safeTemplate}-${safeQueue}`;
}

function persistReportJob(job) {
  const storedJob = {
    created_at: "2026-05-14T00:00:00.000Z",
    worker: "pending",
    ...job
  };

  reportJobs.set(storedJob.job_id, storedJob);
  return storedJob;
}

function reportJobSummary(job) {
  return {
    job_id: job.job_id,
    template: job.template,
    format: job.format,
    scope: job.scope,
    queue: job.queue,
    mode: job.mode,
    status: job.status,
    worker: job.worker
  };
}

function resolveLegacyPanelReportsHome(normalized) {
  return sendUpstream(normalized, 200, {
    service: "legacy-panel",
    module: "reports",
    status: "partial",
    message: "Report generation is partially enabled for migration validation.",
    available_actions: [
      "templates",
      "preview",
      "create"
    ],
    queue: "migration-report-queue",
    note: "Only migration-safe templates are accepted."
  });
}

function resolveLegacyPanelReportTemplates(normalized) {
  const includeMode = normalized.searchParams.get("include");
  const auditMode = normalized.searchParams.get("audit");
  const templateView = listReportTemplates(includeMode, auditMode);

  return sendUpstream(normalized, 200, {
    service: "legacy-panel",
    module: "reports",
    selector: includeMode || "enabled",
    templates: templateView.templates,
    note: templateView.note
  });
}

function resolveLegacyPanelReportPreview(normalized) {
  const templateId = normalized.searchParams.get("template");
  const format = normalized.searchParams.get("format") || "json";
  const scope = normalized.searchParams.get("scope") || "summary";

  if (!templateId) {
    return sendUpstream(normalized, 400, {
      error: "missing_template",
      message: "template parameter is required."
    });
  }

  const template = findReportTemplate(templateId);

  if (!template) {
    return sendUpstream(normalized, 404, {
      error: "template_not_found",
      message: "Requested report template was not found."
    });
  }

  if (format === "pdf") {
    return sendUpstream(normalized, 415, {
      error: "unsupported_format",
      message: "PDF rendering is not enabled during migration."
    });
  }

  if (!["json", "html"].includes(format)) {
    return sendUpstream(normalized, 415, {
      error: "unsupported_format",
      message: "Requested report format is not supported."
    });
  }

  if (template.status === "disabled") {
    return sendUpstream(normalized, 403, {
      error: "template_disabled",
      template: template.id,
      message: "Template is disabled during migration."
    });
  }

  if (template.id === "worker-diagnostics") {
    return sendUpstream(normalized, 202, {
      service: "legacy-panel",
      module: "reports",
      template: template.id,
      rendered: false,
      status: "queue_validation_required",
      message: "Template requires queue validation before rendering."
    });
  }

  if (scope === "full") {
    return sendUpstream(normalized, 403, {
      error: "restricted_scope",
      message: "Full report scope is restricted during migration."
    });
  }

  if (scope !== "summary") {
    return sendUpstream(normalized, 403, {
      error: "restricted_scope",
      message: "Requested scope is not available for this template."
    });
  }

  return sendUpstream(normalized, 200, {
    service: "legacy-panel",
    module: "reports",
    template: template.id,
    format,
    scope,
    rendered: true,
    sanitized: format === "html",
    preview: {
      rows: 3,
      source: "migration-sample"
    }
  });
}

function resolveLegacyPanelReportCreate(normalized) {
  const templateId = normalized.searchParams.get("template");
  const format = normalized.searchParams.get("format");
  const scope = normalized.searchParams.get("scope");
  const queue = normalized.searchParams.get("queue");
  const mode = normalized.searchParams.get("mode");
  const missing = [
    ["template", templateId],
    ["format", format],
    ["scope", scope],
    ["queue", queue],
    ["mode", mode]
  ].filter(([, value]) => !value).map(([name]) => name);

  if (missing.length > 0) {
    return sendUpstream(normalized, 400, {
      error: "job_config_incomplete",
      missing,
      message: "Report job config is incomplete."
    });
  }

  const template = findReportTemplate(templateId);

  if (!template) {
    return sendUpstream(normalized, 404, {
      error: "template_not_found",
      message: "Requested report template was not found."
    });
  }

  if (template.status === "disabled") {
    return sendUpstream(normalized, 403, {
      error: "template_disabled",
      template: template.id,
      message: "Template is disabled during migration."
    });
  }

  if (template.id === "worker-diagnostics") {
    if (format !== "json") {
      return sendUpstream(normalized, 415, {
        error: "unsupported_format",
        template: template.id,
        message: "Worker diagnostics jobs only accept queued JSON configs."
      });
    }

    if (mode === "render") {
      return sendUpstream(normalized, 403, {
        error: "synchronous_render_disabled",
        template: template.id,
        message: "Synchronous rendering is disabled for diagnostics templates."
      });
    }

    if (scope !== "internal") {
      return sendUpstream(normalized, 202, {
        service: "legacy-panel",
        module: "reports",
        created: false,
        template: template.id,
        status: "preview-only",
        message: "Template requires queue validation before rendering."
      });
    }

    if (queue !== "maintenance-worker") {
      const wrongQueueJob = persistReportJob({
        job_id: makeJobId(template.id, queue),
        template: template.id,
        format,
        scope,
        queue,
        mode,
        status: "queued",
        risk: "wrong-queue-no-worker-review"
      });

      return sendUpstream(normalized, 202, {
        service: "legacy-panel",
        module: "reports",
        created: true,
        job_id: wrongQueueJob.job_id,
        queue: wrongQueueJob.queue,
        status: wrongQueueJob.status,
        finding: "diagnostics job accepted by a non-maintenance queue"
      });
    }

    if (mode !== "queue-only") {
      return sendUpstream(normalized, 403, {
        error: "queue_mode_required",
        template: template.id,
        message: "Diagnostics templates are accepted only for queue validation."
      });
    }

    const diagnosticsJob = persistReportJob({
      job_id: "bg7-job-worker-diagnostics",
      template: template.id,
      format,
      scope,
      queue,
      mode,
      status: "queued",
      risk: "unsafe-template-accepted",
      phase8_hint: "Worker execution is disabled until maintenance review."
    });

    return sendUpstream(normalized, 200, {
      service: "legacy-panel",
      module: "reports",
      created: true,
      job_id: diagnosticsJob.job_id,
      queue: diagnosticsJob.queue,
      status: diagnosticsJob.status,
      risk: diagnosticsJob.risk,
      finding: "report workflow accepted an internal worker diagnostics job",
      flag: phase7Flag,
      next_hint: "Queued diagnostics jobs are processed by a maintenance worker in the next phase."
    });
  }

  if (!["json", "html"].includes(format)) {
    return sendUpstream(normalized, 415, {
      error: "unsupported_format",
      message: "Requested report format is not supported."
    });
  }

  if (scope !== "summary") {
    return sendUpstream(normalized, 403, {
      error: "restricted_scope",
      message: "Only summary scope is available for migration-safe templates."
    });
  }

  if (mode === "render") {
    return sendUpstream(normalized, 403, {
      error: "synchronous_render_disabled",
      message: "Synchronous rendering is disabled during migration."
    });
  }

  const normalJob = persistReportJob({
    job_id: makeJobId(template.id, queue),
    template: template.id,
    format,
    scope,
    queue,
    mode,
    status: "queued",
    risk: "migration-safe"
  });

  return sendUpstream(normalized, 202, {
    service: "legacy-panel",
    module: "reports",
    created: true,
    job_id: normalJob.job_id,
    queue: normalJob.queue,
    status: normalJob.status,
    risk: normalJob.risk,
    message: "Report job queued for migration validation."
  });
}

function resolveLegacyPanelReportJobs(normalized) {
  const jobs = [...reportJobs.values()].map(reportJobSummary);

  return sendUpstream(normalized, 200, {
    service: "legacy-panel",
    module: "reports",
    jobs,
    count: jobs.length
  });
}

function resolveLegacyPanelReportJob(normalized, jobId) {
  const job = reportJobs.get(jobId);

  if (!job) {
    return sendUpstream(normalized, 404, {
      error: "job_not_found",
      message: "Report job was not found in the migration queue."
    });
  }

  if (job.job_id === "bg7-job-worker-diagnostics") {
    return sendUpstream(normalized, 200, {
      job_id: job.job_id,
      template: job.template,
      queue: job.queue,
      status: job.status,
      worker: job.worker,
      phase8_hint: "Worker execution is disabled until maintenance review."
    });
  }

  return sendUpstream(normalized, 200, reportJobSummary(job));
}

function resolveLegacyPanelReportQueue(normalized) {
  const queues = [...reportJobs.values()].reduce((accumulator, job) => {
    accumulator[job.queue] = accumulator[job.queue] || [];
    accumulator[job.queue].push(reportJobSummary(job));
    return accumulator;
  }, {});

  return sendUpstream(normalized, 200, {
    service: "legacy-panel",
    module: "reports",
    queue_mode: "in-memory",
    queues,
    worker: "paused"
  });
}

function resolveLegacyPanelWorkerStatus(normalized) {
  return sendUpstream(normalized, 200, {
    worker: "maintenance-worker",
    status: "paused",
    accepts: [
      "queued diagnostics jobs"
    ],
    blocked: [
      "synchronous execution",
      "external callbacks",
      "shell execution"
    ],
    next_hint: "Queued jobs are reviewed by the worker in the next phase."
  });
}

function resolveLegacyPanelReports(normalized) {
  const sessionError = requireLegacySession(normalized);

  if (sessionError) {
    return sessionError;
  }

  if (normalized.path === "/reports") {
    return resolveLegacyPanelReportsHome(normalized);
  }

  if (normalized.path === "/reports/templates") {
    return resolveLegacyPanelReportTemplates(normalized);
  }

  if (normalized.path === "/reports/preview") {
    return resolveLegacyPanelReportPreview(normalized);
  }

  if (normalized.path === "/reports/create") {
    return resolveLegacyPanelReportCreate(normalized);
  }

  if (normalized.path === "/reports/jobs") {
    return resolveLegacyPanelReportJobs(normalized);
  }

  if (normalized.path.startsWith("/reports/jobs/")) {
    const jobId = decodeURIComponent(normalized.path.slice("/reports/jobs/".length));
    return resolveLegacyPanelReportJob(normalized, jobId);
  }

  if (normalized.path === "/reports/queue") {
    return resolveLegacyPanelReportQueue(normalized);
  }

  if (normalized.path === "/reports/worker-status") {
    return resolveLegacyPanelWorkerStatus(normalized);
  }

  return {
    ok: false,
    status: 404,
    error: "upstream_not_found",
    message: "Internal upstream route not found.",
    requested_url: normalized.url
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

  if (normalized.host === "legacy-panel.internal" && normalized.path.startsWith("/reports")) {
    return resolveLegacyPanelReports(normalized);
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
