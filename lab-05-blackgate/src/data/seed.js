const users = [
  {
    id: 1,
    username: "admin",
    password: "not-available-in-phase2",
    displayName: "BlackGate Administrator",
    role: "admin",
    team: "Core Access",
    locked: true
  },
  {
    id: 2,
    username: "operator",
    password: "operator123",
    displayName: "Operations Operator",
    role: "operator",
    team: "Gate Operations",
    locked: false
  },
  {
    id: 3,
    username: "analyst",
    password: "analyst123",
    displayName: "Security Analyst",
    role: "analyst",
    team: "Access Review",
    locked: false
  },
  {
    id: 4,
    username: "guest",
    password: "guest123",
    displayName: "Guest Reviewer",
    role: "guest",
    team: "Read Only",
    locked: false
  }
];

const tickets = [
  {
    id: "BG-1001",
    title: "Revisar acesso VPN de fornecedor",
    severity: "medium",
    status: "open",
    owner: "operator",
    exposure: "public-summary",
    visibility: ["operator", "guest"],
    description: "Fornecedor externo solicitou janela de acesso para manutencao controlada.",
    metadata: "External gateway active; request is safe for limited review."
  },
  {
    id: "BG-1002",
    title: "Validar alerta em servidor financeiro",
    severity: "high",
    status: "triage",
    owner: "analyst",
    exposure: "restricted",
    visibility: ["analyst"],
    description: "Evento de autenticacao fora do padrao em sistema financeiro interno.",
    metadata: "Financial server event requires analyst context."
  },
  {
    id: "BG-1003",
    title: "Investigar falha de autenticacao no gateway",
    severity: "high",
    status: "open",
    owner: "operator",
    exposure: "restricted",
    visibility: ["operator"],
    description: "External gateway active; verificar excesso de tentativas negadas.",
    metadata: "Gateway failures correlate with access-control telemetry."
  },
  {
    id: "BG-1004",
    title: "Revisar logs do servico legacy-files",
    severity: "medium",
    status: "pending",
    owner: "analyst",
    exposure: "metadata",
    visibility: ["analyst"],
    description: "Legacy components are scheduled for migration.",
    metadata: "legacy-files was moved behind api-core.internal. Direct access should be blocked by gateway rules."
  },
  {
    id: "BG-1005",
    title: "Auditoria de tokens internos",
    severity: "critical",
    status: "blocked",
    owner: "admin",
    exposure: "metadata",
    visibility: ["admin"],
    description: "Token audit pending. Internal trust boundary review required.",
    metadata: "Token audit pending. Legacy debug token should be rotated."
  }
];

const assets = [
  {
    hostname: "gw-blackgate.local",
    type: "Gateway",
    environment: "edge",
    status: "active",
    exposure: "public entrypoint",
    note: "BlackGate controls access to restricted operational systems.",
    visibility: ["operator", "analyst", "guest"]
  },
  {
    hostname: "api-core.internal",
    type: "API",
    environment: "internal",
    status: "nominal",
    exposure: "trusted paths only",
    note: "Some internal services are visible only through trusted paths.",
    visibility: ["operator", "analyst"]
  },
  {
    hostname: "files-vault.internal",
    type: "File service",
    environment: "internal",
    status: "migration",
    exposure: "internal",
    note: "Legacy components are scheduled for migration.",
    visibility: ["analyst"]
  },
  {
    hostname: "queue-worker.internal",
    type: "Worker",
    environment: "operations",
    status: "watch",
    exposure: "internal",
    note: "Processes asynchronous access review tasks.",
    visibility: ["operator"]
  },
  {
    hostname: "audit-db.internal",
    type: "Database",
    environment: "restricted",
    status: "locked",
    exposure: "internal",
    note: "Stores audit snapshots and approval history.",
    visibility: ["admin"]
  },
  {
    hostname: "legacy-panel.internal",
    type: "Admin panel",
    environment: "legacy",
    status: "degraded",
    exposure: "internal",
    note: "Scheduled for route migration.",
    visibility: ["admin"]
  }
];

const events = [
  {
    time: "08:10",
    label: "External gateway active",
    detail: "Access monitor heartbeat received."
  },
  {
    time: "08:24",
    label: "Token audit pending",
    detail: "Review task waiting for privileged approval."
  },
  {
    time: "08:41",
    label: "Internal trust boundary review required",
    detail: "Operations note attached to BG-1005."
  },
  {
    time: "09:03",
    label: "Legacy route migration queued",
    detail: "No migration window assigned in Phase 3."
  }
];

function findUser(username) {
  return users.find((user) => user.username === username);
}

function findTicketById(id) {
  return tickets.find((ticket) => ticket.id.toLowerCase() === String(id || "").toLowerCase());
}

function findAssetByHostname(hostname) {
  return assets.find((asset) => asset.hostname.toLowerCase() === String(hostname || "").toLowerCase());
}

function canViewTicket(user, ticket) {
  if (!user || !ticket) {
    return false;
  }

  return user.role === "admin"
    || ticket.owner === user.username
    || (ticket.visibility || []).includes(user.role);
}

function canViewAssetInInterface(user, asset) {
  if (!user || !asset) {
    return false;
  }

  return user.role === "admin" || (asset.visibility || []).includes(user.role);
}

function getMetrics() {
  return {
    totalTickets: tickets.length,
    monitoredAssets: assets.length,
    pendingAlerts: tickets.filter((ticket) => ["open", "triage", "pending"].includes(ticket.status)).length,
    blockedOperations: tickets.filter((ticket) => ticket.status === "blocked").length
  };
}

module.exports = {
  users,
  tickets,
  assets,
  events,
  findUser,
  findTicketById,
  findAssetByHostname,
  canViewTicket,
  canViewAssetInInterface,
  getMetrics
};
