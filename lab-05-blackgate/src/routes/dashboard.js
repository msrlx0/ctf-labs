const express = require("express");
const {
  assets,
  canViewAssetInInterface,
  canViewTicket,
  events,
  getMetrics,
  tickets
} = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/dashboard", requireAuth, (req, res) => {
  const user = req.session.user;
  const isOperationalRole = ["operator", "admin"].includes(user.role);
  const visibleTickets = tickets.filter((ticket) => canViewTicket(user, ticket));
  const visibleAssets = assets.filter((asset) => canViewAssetInInterface(user, asset));
  const visibleCriticalAssets = visibleAssets.filter((asset) => ["degraded", "locked", "watch"].includes(asset.status));
  const dashboardCards = isOperationalRole
    ? [
        { label: "Gateway Status", value: "degraded", note: "Public edge accepts limited training telemetry.", tone: "warning" },
        { label: "Gateway Trust", value: "review required", note: "Gateway-mediated operations remain under review.", tone: "warning" },
        { label: "Context Service", value: "compatibility mode", note: "Operational context is evaluated separately from the web session." },
        { label: "Files Vault", value: "migration mode", note: "Document access is moving through gateway-only compatibility checks.", tone: "warning" },
        { label: "Legacy Panel", value: "migration lockdown", note: "Interactive access is blocked while maintenance workflows are reviewed.", tone: "danger" },
        { label: "Metadata Sync", value: "pending", note: "Client configuration and route registry are under review." },
        { label: "Legacy Migration", value: "scheduled", note: "Legacy routes are listed for future migration windows." }
      ]
    : user.role === "analyst"
      ? [
          { label: "Review Queue", value: "active", note: "Assigned ticket review is available." },
          { label: "Context Review", value: "limited", note: "Context details are restricted to the analyst view." },
          { label: "Asset Inventory", value: "scoped", note: "Only assets in your review scope are listed." },
          { label: "Session", value: "standard", note: "Training access is active." }
        ]
      : [
          { label: "Training Access", value: "active", note: "Initial training session is available." },
          { label: "Tickets", value: "scoped", note: "Visible tickets are ready for review." },
          { label: "Assets", value: "scoped", note: "Visible assets are ready for review." },
          { label: "Session", value: "standard", note: "Use the available menu items to continue." }
        ];
  const visibleEvents = isOperationalRole
    ? events
    : [
        {
          time: "08:10",
          label: "Training session active",
          detail: "User access confirmed for the current review scope."
        },
        {
          time: "08:24",
          label: "Review queue updated",
          detail: "Visible tickets and assets are available."
        }
      ];
  const metrics = isOperationalRole
    ? getMetrics()
    : {
        totalTickets: visibleTickets.length,
        monitoredAssets: visibleAssets.length,
        pendingAlerts: visibleTickets.filter((ticket) => ["open", "triage", "pending"].includes(ticket.status)).length,
        blockedOperations: visibleTickets.filter((ticket) => ticket.status === "blocked").length
      };

  return res.renderPage("dashboard", {
    title: "Dashboard",
    metrics,
    dashboardCards,
    recentTickets: visibleTickets.slice(0, 3),
    criticalAssets: visibleCriticalAssets.length ? visibleCriticalAssets : visibleAssets.slice(0, 3),
    assetPanelTitle: isOperationalRole ? "Ativos criticos" : "Ativos visiveis",
    events: visibleEvents,
    isOperationalRole
  });
});

module.exports = router;
