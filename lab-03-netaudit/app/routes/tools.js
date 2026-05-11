const express = require("express");
const { exec } = require("child_process");

const router = express.Router();

function getSession(req) {
  try {
    return JSON.parse(Buffer.from(req.cookies.session || "", "base64").toString("utf8"));
  } catch {
    return null;
  }
}

function requireLogin(req, res, next) {
  const session = getSession(req);

  if (!session || !session.username) {
    return res.status(401).json({
      ok: false,
      error: "Authentication required"
    });
  }

  req.user = session;
  next();
}

function runCommand(command, res, metadata) {
  const startedAt = Date.now();

  exec(command, { timeout: 5000 }, (error, stdout, stderr) => {
    let output = `${stdout || ""}${stderr || ""}`;

    if (!output && error) {
      output = error.message;
    }

    return res.json({
      ok: !error,
      status: error ? "completed_with_errors" : "completed",
      target: metadata.target,
      diagnostic: metadata.diagnostic,
      durationMs: Date.now() - startedAt,
      output
    });
  });
}

function checkHost(req, res) {
  const { host } = req.body;

  if (!host) {
    return res.status(400).json({
      ok: false,
      error: "host is required"
    });
  }

  // Intentional lab vulnerability: input is concatenated into a shell command.
  const command = `ping -c 2 ${host}`;
  return runCommand(command, res, {
    target: host,
    diagnostic: "availability"
  });
}

function resolveHost(req, res) {
  const host = req.body.host || req.body.target;

  if (!host) {
    return res.status(400).json({
      ok: false,
      error: "host is required"
    });
  }

  if (host.includes(";")) {
    return res.status(400).json({
      ok: false,
      error: "Invalid character detected"
    });
  }

  // Intentional weak filter: only ";" is blocked.
  const command = `nslookup ${host}`;
  return runCommand(command, res, {
    target: host,
    diagnostic: "legacy-resolver"
  });
}

router.post("/check", requireLogin, checkHost);
router.post("/ping", requireLogin, checkHost);
router.post("/resolve", requireLogin, resolveHost);
router.post("/dns", requireLogin, resolveHost);

module.exports = router;
