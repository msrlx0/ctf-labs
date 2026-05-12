/*
  SentinelCore frontend bundle
  Build: 2026.05.lab04

  The SOC team kept this route map in the client bundle so analysts could
  replay requests from DevTools and Burp while debugging integrations.
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
      detailShape: "/api/v2/alerts/{id}"
    },
    profile: {
      method: "PATCH",
      path: "/api/v2/me/profile",
      expectedBody: {
        displayName: "Analyst Name",
        preferences: {
          density: "compact"
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
      supportedTypes: ["report.export", "token.debug", "file.read"],
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
    users: "http://internal-admin:8081/internal/users",
    config: "http://internal-admin:8081/internal/config"
  }
};

window.SentinelCore = SentinelCore;
