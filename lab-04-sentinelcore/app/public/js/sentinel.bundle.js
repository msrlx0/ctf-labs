/*
  SentinelCore frontend bundle
  Build: 2026.05.lab04

  Support metadata left in the client bundle during an incident-response
  sprint. Analysts use these notes to replay requests from DevTools.
*/

const SentinelCore = {
  apiBase: "/api/v2",

  routes: {
    me: {
      method: "GET",
      path: "/api/v2/me"
    },
    alerts: {
      method: "GET",
      path: "/api/v2/alerts",
      detailShape: "/api/v2/alerts/{incidentId}",
      incidentIdPattern: "7xxx",
      detailHeaders: {
        "X-Sentinel-Client": "web-console",
        "X-Tenant-Scope": "ACME-SOC"
      }
    },
    profile: {
      method: "PATCH",
      path: "/api/v2/me/profile",
      expectedBody: {
        displayName: "Analyst Name",
        preferences: {
          density: "compact"
        },
        access: {
          requestedRole: "viewer"
        }
      }
    },
    debugHealth: {
      method: "GET",
      path: "/api/v2/debug/health",
      minimumRole: "analyst"
    },
    buildManifest: {
      method: "GET",
      path: "/api/v2/artifacts/build-manifest",
      minimumRole: "analyst"
    },
    integrationCheck: {
      method: "POST",
      path: "/api/v2/integrations/check",
      expectedBody: {
        url: "https://example.internal/status"
      }
    },
    integrationProxy: {
      method: "POST",
      path: "/api/v2/integrations/proxy",
      expectedBody: {
        url: "https://example.internal/config",
        headers: {
          "x-internal-token": "provided-by-ops"
        }
      }
    },
    reportRender: {
      method: "POST",
      path: "/api/v2/reports/render",
      placeholders: [
        "{{user.username}}",
        "{{user.role}}",
        "{{internal.url}}",
        "{{config.jwt_hint}}",
        "{{config.internal_token_hint}}"
      ]
    },
    jobs: {
      method: "POST",
      path: "/api/v2/jobs",
      queue: "sentinel:jobs",
      knownTypes: ["report.export", "token.debug"],
      legacyHandlers: "see internal config",
      expectedBody: {
        type: "report.export",
        source: "daily-summary",
        output: "daily-summary.txt"
      }
    },
    jobOutput: {
      method: "GET",
      path: "/api/v2/jobs/output?file={name}"
    },
    diagnosticsRead: {
      method: "GET",
      path: "/api/v2/admin/diagnostics/read?file=app.log",
      note: "Reader starts from the API log directory."
    }
  },

  internalServices: {
    adminBaseUrl: "http://internal-admin:8081",
    health: "http://internal-admin:8081/status",
    protectedPrefix: "/internal/",
    protectedResources: ["/internal/users", "/internal/config"]
  }
};

window.SentinelCore = SentinelCore;
