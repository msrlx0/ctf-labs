window.BlackGateClient = {
  version: "1.5.0-phase6",
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
  phaseSix: {
    status: "legacy-migration-lockdown",
    message: "BlackGate Phase 6 keeps migration workflows behind separated operational controls."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase6-legacy-reuse",
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
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
