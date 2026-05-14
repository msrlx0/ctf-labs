const express = require("express");
const {
  canViewTicket,
  findAssetByHostname,
  findTicketById
} = require("../data/seed");

const router = express.Router();

function sendError(res, status, error, message) {
  return res.status(status).json({ error, message });
}

function requireApiAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return sendError(res, 401, "authentication_required", "Login required to access this resource.");
  }

  return next();
}

function publicRoutes() {
  return [
    "/login",
    "/health",
    "/api/status",
    "/api/version"
  ];
}

router.get("/api/status", (req, res) => {
  return res.json({
    service: "blackgate-gateway",
    status: "degraded",
    environment: "training",
    public_gateway: true,
    internal_services_visible: false
  });
});

router.get("/api/version", (req, res) => {
  return res.json({
    name: "BlackGate Operations Console",
    version: "1.6.0-phase7",
    build: "bg-phase7-report-workflow",
    commit: "local-training-build",
    node_env: process.env.NODE_ENV || "development"
  });
});

router.get("/api/client-config", (req, res) => {
  return res.json({
    appName: "BlackGate",
    apiBase: "/api",
    features: {
      tickets: true,
      assets: true,
      legacyMode: false,
      debugBanner: false
    },
    internalHints: {
      legacy: "migration",
      debug: "limited",
      gateway: "operator-mediated"
    },
    context: {
      issuer: "legacy-context-service",
      mode: "compatibility"
    },
    gateway: {
      mode: "operator-mediated"
    },
    filesVault: {
      mode: "migration"
    },
    legacy: {
      mode: "migration",
      realm: "separate"
    },
    reports: {
      mode: "migration"
    }
  });
});

router.get("/api/routes", (req, res) => {
  return res.json({
    public: publicRoutes(),
    documented: [
      "/tickets",
      "/assets"
    ],
    undocumented: {
      enabled: true,
      note: "Compatibility routes are excluded from the public route map."
    },
    deprecated: [
      "/legacy",
      "/debug"
    ]
  });
});

router.get("/api/tickets/:id", requireApiAuth, (req, res) => {
  const ticket = findTicketById(req.params.id);

  if (!ticket) {
    return sendError(res, 404, "not_found", "Resource not found.");
  }

  if (canViewTicket(req.session.user, ticket)) {
    return res.json({
      id: ticket.id,
      title: ticket.title,
      severity: ticket.severity,
      status: ticket.status,
      owner: ticket.owner,
      exposure: ticket.exposure,
      description: ticket.description,
      metadata: ticket.metadata
    });
  }

  if (ticket.exposure === "metadata") {
    return res.json({
      id: ticket.id,
      title: ticket.title,
      severity: ticket.severity,
      status: ticket.status,
      exposure: ticket.exposure,
      metadata: ticket.metadata,
      authorization_note: "Limited metadata returned by legacy object lookup."
    });
  }

  return sendError(res, 403, "forbidden", "Insufficient role for this resource.");
});

router.get("/api/assets/:hostname", requireApiAuth, (req, res) => {
  const asset = findAssetByHostname(req.params.hostname);

  if (!asset) {
    return sendError(res, 404, "not_found", "Resource not found.");
  }

  return res.json({
    hostname: asset.hostname,
    type: asset.type,
    environment: asset.environment,
    status: asset.status,
    exposure: asset.exposure,
    notes: asset.note
  });
});

router.use("/api", (req, res) => {
  return sendError(res, 404, "not_found", "Resource not found.");
});

module.exports = router;
