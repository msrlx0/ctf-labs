const express = require("express");
const { exec } = require("child_process");

const router = express.Router();

function requireLogin(req, res, next) {
  if (!req.cookies.session) {
    return res.status(401).json({
      ok: false,
      error: "Authentication required"
    });
  }

  next();
}

function runCommand(command, res) {
  exec(command, { timeout: 5000 }, (error, stdout, stderr) => {
    let output = `${stdout || ""}${stderr || ""}`;

    if (!output && error) {
      output = error.message;
    }

    return res.json({
      ok: !error,
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
  return runCommand(command, res);
}

function resolveHost(req, res) {
  const { host } = req.body;

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
  return runCommand(command, res);
}

router.post("/check", requireLogin, checkHost);
router.post("/ping", requireLogin, checkHost);
router.post("/resolve", requireLogin, resolveHost);
router.post("/dns", requireLogin, resolveHost);

module.exports = router;
