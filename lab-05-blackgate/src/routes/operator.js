const express = require("express");
const { requireOperatorContext } = require("../utils/contextToken");
const { listInternalServices, resolveInternalService } = require("../utils/internalServices");

const router = express.Router();

router.get("/api/operator/briefing", requireOperatorContext, (req, res) => {
  return res.json({
    classification: "operator-only",
    briefing: "Legacy context validation accepted elevated operator scope.",
    phase: "3",
    finding: "weak unsigned context token",
    flag: "FLAG{blackgate_weak_token_role_escalation_phase3}",
    next_hint: "Operator context can see internal gateway metadata, but direct internal access is still blocked."
  });
});

router.get("/api/operator/gateway-metadata", requireOperatorContext, (req, res) => {
  return res.json({
    gateway: "gw-blackgate.local",
    trusted_upstream: "api-core.internal",
    gateway_fetch: "/api/operator/gateway-fetch?url=http://api-core.internal/health",
    allowed_internal_hosts: listInternalServices(),
    internal_candidates: [
      "api-core.internal",
      "files-vault.internal",
      "legacy-panel.internal"
    ],
    blocked_paths: [
      "/api/internal/files",
      "/legacy",
      "/debug/trace"
    ],
    phase4_hint: "Gateway-originated requests can resolve selected internal upstreams.",
    phase5_hint: "files-vault exposes a catalog and legacy read compatibility through gateway-fetch.",
    sample_files_vault_url: "http://files-vault.internal/metadata"
  });
});

router.get("/api/operator/gateway-fetch", requireOperatorContext, (req, res) => {
  if (!req.query.url) {
    return res.status(400).json({
      error: "bad_request",
      message: "Valid url parameter is required."
    });
  }

  const upstream = resolveInternalService(req.query.url);

  if (!upstream.ok) {
    return res.status(upstream.status).json({
      error: upstream.error,
      message: upstream.message
    });
  }

  return res.status(upstream.status).json({
    gateway: "gw-blackgate.local",
    requested_url: upstream.requested_url,
    allowed: true,
    upstream_status: upstream.status,
    response: upstream.body
  });
});

module.exports = router;
