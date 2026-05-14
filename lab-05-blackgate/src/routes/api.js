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
    "/robots.txt",
    "/.well-known/security.txt",
    "/security-policy",
    "/api/status",
    "/api/version",
    "/api/client-config"
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
    version: "1.3.0-phase4",
    build: "bg-phase4-gateway-trust",
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
      legacyPanel: "/legacy",
      debugPrefix: "/debug",
      internalApi: "/api/internal"
    },
    context: {
      header: "X-BG-Context",
      issuer: "legacy-context-service",
      mode: "compatibility"
    },
    gateway: {
      mode: "operator-mediated",
      metadata: "/api/operator/gateway-metadata"
    }
  });
});

router.get("/api/routes", (req, res) => {
  return res.json({
    public: publicRoutes(),
    authenticated: [
      "/dashboard",
      "/context",
      "/tickets",
      "/assets",
      "/api/context/me",
      "/api/context/verify",
      "/api/tickets/:id",
      "/api/assets/:hostname"
    ],
    operator_context: [
      "/api/operator/briefing",
      "/api/operator/gateway-metadata",
      "/api/operator/gateway-fetch?url="
    ],
    planned: [
      "/legacy",
      "/api/internal/health",
      "/api/internal/files",
      "/api/internal/fetch",
      "/api/internal/files/catalog",
      "/api/internal/files/read",
      "/legacy/render",
      "/debug/trace"
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
