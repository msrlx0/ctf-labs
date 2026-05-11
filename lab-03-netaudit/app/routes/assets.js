const express = require("express");
const { exec } = require("child_process");

const router = express.Router();

const assets = [
  {
    id: "gw-01",
    name: "Gateway Edge",
    hostname: "gateway.local",
    status: "online"
  },
  {
    id: "dns-01",
    name: "DNS Resolver",
    hostname: "dns01.local",
    status: "online"
  },
  {
    id: "web-01",
    name: "Intranet Portal",
    hostname: "intranet.local",
    status: "degraded"
  },
  {
    id: "bkp-01",
    name: "Storage Node",
    hostname: "storage01.local",
    status: "maintenance"
  }
];

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

function runCommand(command, metadata, res) {
  const startedAt = Date.now();

  exec(command, { timeout: 5000 }, (error, stdout, stderr) => {
    let output = `${stdout || ""}${stderr || ""}`;

    if (!output && error) {
      output = error.message;
    }

    return res.json({
      ok: !error,
      status: error ? "completed_with_errors" : "completed",
      ...metadata,
      durationMs: Date.now() - startedAt,
      output
    });
  });
}

function runBlindCommand(command, metadata, res) {
  const startedAt = Date.now();

  exec(command, { timeout: 8000 }, error => {
    const timedOut = Boolean(error && (error.killed || error.signal === "SIGTERM"));

    return res.json({
      ok: !error,
      status: timedOut ? "completed_with_timeout" : "completed",
      ...metadata,
      durationMs: Date.now() - startedAt
    });
  });
}

router.get("/", requireLogin, (req, res) => {
  return res.json({
    ok: true,
    assets
  });
});

router.post("/check", requireLogin, (req, res) => {
  const { assetId, checkType, target, port } = req.body;
  const asset = assets.find(item => item.id === assetId);

  if (!assetId || !checkType || !target) {
    return res.status(400).json({
      ok: false,
      error: "assetId, checkType and target are required"
    });
  }

  if (!asset) {
    return res.status(404).json({
      ok: false,
      error: "asset not found"
    });
  }

  if (checkType !== "icmp" && checkType !== "tcp") {
    return res.status(400).json({
      ok: false,
      error: "unsupported check type"
    });
  }

  if (checkType === "tcp") {
    if (!port) {
      return res.status(400).json({
        ok: false,
        error: "port is required for tcp checks"
      });
    }

    // Intentional lab vulnerability: target and port are trusted from the request body.
    const command = `nc -zvw2 ${target} ${port}`;
    return runBlindCommand(command, {
      assetId,
      assetName: asset.name,
      checkType,
      target,
      port
    }, res);
  }

  // Intentional lab vulnerability: target is trusted from the request body.
  const command = `ping -c 2 ${target}`;
  return runCommand(command, {
    assetId,
    assetName: asset.name,
    checkType,
    target
  }, res);
});

router.post("/resolve", requireLogin, (req, res) => {
  const { target } = req.body;

  if (!target) {
    return res.status(400).json({
      ok: false,
      error: "target is required"
    });
  }

  if (target.includes(";")) {
    return res.status(400).json({
      ok: false,
      error: "Invalid character detected"
    });
  }

  // Intentional weak filter: only ";" is blocked.
  const command = `nslookup ${target}`;
  return runCommand(command, {
    assetId: "legacy-resolver",
    assetName: "Legacy Resolver",
    checkType: "resolve",
    target
  }, res);
});

module.exports = router;
