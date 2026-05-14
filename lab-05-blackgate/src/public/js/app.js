window.BlackGateClient = {
  version: "1.2.0-phase3",
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
  phaseThree: {
    status: "weak-context-token",
    message: "BlackGate Phase 3 exposes legacy context metadata and operator compatibility checks, not the final exploitation chain."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase3-weak-token",
  routes: {
    status: "/api/status",
    version: "/api/version",
    routes: "/api/routes",
    clientConfig: "/api/client-config",
    contextMe: "/api/context/me"
  },
  hints: {
    debugPrefix: "/debug",
    legacyPanel: "/legacy",
    inventoryLookup: "/api/assets/{hostname}"
  },
  context: {
    header: "X-BG-Context",
    verifyEndpoint: "/api/context/verify"
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
