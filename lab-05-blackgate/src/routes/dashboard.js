const express = require("express");
const { assets, events, getMetrics, tickets } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/dashboard", requireAuth, (req, res) => {
  return res.renderPage("dashboard", {
    title: "Dashboard",
    metrics: getMetrics(),
    phaseStatus: {
      gateway: "degraded",
      metadataSync: "pending",
      legacyMigration: "scheduled",
      contextService: "compatibility mode",
      gatewayTrust: "review required",
      filesVault: "migration mode"
    },
    recentTickets: tickets.slice(0, 3),
    criticalAssets: assets.filter((asset) => ["degraded", "locked", "watch"].includes(asset.status)),
    events
  });
});

module.exports = router;
