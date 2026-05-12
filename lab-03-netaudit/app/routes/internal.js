const express = require("express");
const fs = require("fs");
const path = require("path");

const router = express.Router();
const SUPPORT_TOKEN = "netaudit-debug-2026";

function requireSupportToken(req, res, next) {
  const token = req.headers["x-support-token"];

  if (token !== SUPPORT_TOKEN) {
    return res.status(403).json({
      ok: false,
      error: "Invalid support token"
    });
  }

  next();
}

router.get("/health", requireSupportToken, (req, res) => {
  return res.json({
    status: "healthy",
    service: "NetAudit",
    environment: "production",
    diagnosticEndpoint: "/api/internal/diagnostics",
    backupEndpoint: "/api/internal/backup",
    backupMethod: "POST",
    requiredParameter: "includeFile"
  });
});

router.get("/diagnostics", requireSupportToken, (req, res) => {
  return res.json({
    status: "diagnostics_ready",
    flag: "FLAG{internal_diagnostics_token_abuse_lab3}",
    backupManifest: "/app/data/backup-manifest.log"
  });
});

router.post("/backup", requireSupportToken, (req, res) => {
  const includeFile = String(req.body.includeFile || "");

  if (!includeFile) {
    return res.status(400).json({
      ok: false,
      error: "includeFile is required"
    });
  }

  // Intentional lab vulnerability: includeFile is joined under /app without an allowlist.
  const requestedPath = path.join("/app", includeFile);

  try {
    const content = fs.readFileSync(requestedPath, "utf8");
    return res.json({
      ok: true,
      status: "export_ready",
      includeFile,
      content
    });
  } catch {
    return res.status(404).json({
      ok: false,
      error: "Requested export file not found"
    });
  }
});

module.exports = router;
