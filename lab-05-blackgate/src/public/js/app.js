window.BlackGateClient = {
  version: "1.3.0-phase4",
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
  phaseFour: {
    status: "gateway-trust-review",
    message: "BlackGate Phase 4 exposes operator-mediated gateway metadata and simulated upstream checks, not direct internal access."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase4-gateway-trust",
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
  },
  gateway: {
    mode: "operator-mediated",
    metadataEndpoint: "/api/operator/gateway-metadata"
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
