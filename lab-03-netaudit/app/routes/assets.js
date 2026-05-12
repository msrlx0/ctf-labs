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
    hostname: "backup01.local",
    status: "maintenance"
  }
];

const resolverMetadata = {
  "gateway.local": {
    resolved: "10.10.0.1",
    owner: "netops"
  },
  "dns01.local": {
    resolved: "10.10.0.53",
    owner: "netops"
  },
  "intranet.local": {
    resolved: "10.10.0.20",
    owner: "webops"
  },
  "backup01.local": {
    resolved: "10.10.0.41",
    owner: "backup"
  }
};

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

router.get("/", requireLogin, (req, res) => {
  return res.json({
    ok: true,
    assets
  });
});

router.post("/check", requireLogin, (req, res) => {
  const { assetId, checkType, target } = req.body;
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

  if (checkType !== "icmp") {
    return res.status(400).json({
      ok: false,
      error: "unsupported check type"
    });
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
  const target = String(req.body.target || "");

  if (!target) {
    return res.status(400).json({
      ok: false,
      error: "target is required"
    });
  }

  if (target === "metadata" || target === "internal-metadata") {
    return res.json({
      ok: true,
      target,
      metadata: {
        note: "legacy resolver metadata should not be exposed",
        flag: "FLAG{hidden_resolver_metadata_disclosure_lab3}"
      }
    });
  }

  const entry = resolverMetadata[target];

  if (!entry) {
    return res.status(404).json({
      ok: false,
      error: "target metadata not found"
    });
  }

  return res.json({
    ok: true,
    target,
    resolved: entry.resolved,
    metadata: {
      owner: entry.owner,
      environment: "internal",
      resolver: "legacy"
    }
  });
});

module.exports = router;
