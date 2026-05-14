window.BlackGateClient = {
  version: "1.4.0-phase5",
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
  phaseFive: {
    status: "files-vault-migration",
    message: "BlackGate Phase 5 keeps Files Vault access gateway-only while document migration checks are reviewed."
  }
};

window.BLACKGATE_CONFIG = {
  apiBase: "/api",
  build: "phase5-files-vault",
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
  },
  filesVault: {
    mode: "gateway-only",
    catalogHint: "metadata-first"
  }
};

document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".metric-card, .panel").forEach((card) => {
    card.addEventListener("mouseenter", () => card.classList.add("is-hovered"));
    card.addEventListener("mouseleave", () => card.classList.remove("is-hovered"));
  });
});
