window.BlackGateClient = {
  version: "1.7.0-phase8",
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
  phaseEight: {
    status: "worker-processing-review",
    message: "BlackGate Phase 8 keeps maintenance processing behind separated operational controls."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase8-worker-processing",
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
  },
  worker: {
    mode: "review"
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
