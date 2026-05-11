const express = require("express");
const { exec } = require("child_process");

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
    ok: true,
    status: "healthy",
    service: "NetAudit",
    environment: "production",
    backupEndpoint: "/api/internal/backup",
    method: "POST",
    requiredParameter: "archiveName",
    diagnosticFile: "/app/flags/flag5.txt"
  });
});

router.post("/backup", requireSupportToken, (req, res) => {
  const { archiveName } = req.body;

  if (!archiveName) {
    return res.status(400).json({
      ok: false,
      error: "archiveName is required"
    });
  }

  // Intentional lab vulnerability: archiveName is concatenated into a shell command.
  const command = `tar -czf /tmp/${archiveName} /app/data`;

  exec(command, { timeout: 5000 }, (error, stdout, stderr) => {
    let output = `${stdout || ""}${stderr || ""}`;

    if (!output && error) {
      output = error.message;
    }

    if (!output) {
      output = "Backup queued successfully";
    }

    return res.json({
      ok: !error,
      output
    });
  });
});

module.exports = router;
