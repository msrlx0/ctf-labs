window.BlackGateClient = {
  version: "1.6.0-phase7",
  routes: {
    dashboard: "/dashboard",
    tickets: "/tickets",
    assets: "/assets",
    health: "/health"
  },
  operations: {
    gateway: "gw-blackgate.local",
    inventory: ["core-services", "document-services", "legacy-services"],
    notes: [
      "legacy routes will be migrated soon",
      "token audit pending",
      "internal trust boundary review required"
    ]
  },
  phaseSeven: {
    status: "report-migration-review",
    message: "BlackGate Phase 7 keeps queued migration workflows behind separated operational controls."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase7-report-workflow",
  surface: "partial",
  hints: {
    debug: "limited",
    legacy: "migration",
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
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
