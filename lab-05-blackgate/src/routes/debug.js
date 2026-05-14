const express = require("express");

const router = express.Router();

router.get("/debug/ping", (req, res) => {
  if (req.get("X-Debug-Token") === "guest-debug") {
    return res.json({
      pong: true,
      debug: true,
      message: "Limited diagnostics enabled.",
      diagnostics: {
        context: {
          issuer: "legacy-context-service",
          mode: "compatibility"
        },
        gateway: {
          mode: "operator-mediated",
          note: "Selected upstream checks are available only through approved operational context."
        },
        filesVault: {
          note: "Document diagnostics remain restricted during migration."
        },
        legacy: {
          note: "Legacy panel diagnostics are intentionally sparse during migration."
        },
        reports: {
          note: "Report diagnostics are sparse during migration."
        },
        worker: {
          note: "Worker diagnostics remain restricted during migration."
        }
      }
    });
  }

  return res.json({
    pong: true,
    debug: false,
    message: "Debug interface is disabled in production profile."
  });
});

router.get("/debug/trace", (req, res) => {
  return res.status(501).json({
    error: "not_implemented",
    message: "Debug trace is not available in Phase 8."
  });
});

module.exports = router;
