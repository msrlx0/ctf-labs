window.BlackGateClient = {
  version: "1.1.0-phase2",
  routes: {
    dashboard: "/dashboard",
    tickets: "/tickets",
    assets: "/assets",
    health: "/health"
  },
  operations: {
    gateway: "gw-blackgate.local",
    inventory: ["api-core.internal", "files-vault.internal", "queue-worker.internal"],
    notes: [
      "legacy routes will be migrated soon",
      "token audit pending",
      "internal trust boundary review required"
    ]
  },
  phaseTwo: {
    status: "recon-only",
    message: "BlackGate Phase 2 exposes recon metadata and controlled authorization inconsistencies, not the final exploitation chain."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase2-recon",
  routes: {
    status: "/api/status",
    version: "/api/version",
    routes: "/api/routes",
    clientConfig: "/api/client-config"
  },
  hints: {
    debugPrefix: "/debug",
    legacyPanel: "/legacy",
    inventoryLookup: "/api/assets/{hostname}"
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
