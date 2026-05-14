const express = require("express");
const { requireOperatorContext } = require("../utils/contextToken");

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
    phase4_hint: "Some internal checks trust gateway-originated requests."
  });
});

module.exports = router;
