const express = require("express");

const router = express.Router();

router.get("/debug/ping", (req, res) => {
  if (req.get("X-Debug-Token") === "guest-debug") {
    return res.json({
      pong: true,
      debug: true,
      message: "Debug handshake accepted for limited diagnostics.",
      diagnostics: {
        routes: ["/debug/ping", "/debug/trace"],
        note: "Full trace requires elevated operator context.",
        context: {
          header: "X-BG-Context",
          verify: "/api/context/verify",
          note: "Legacy context validation is enabled for compatibility checks."
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
    message: "Debug trace is not available in Phase 3."
  });
});

module.exports = router;
