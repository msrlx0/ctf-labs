// legacy resolver kept for support troubleshooting
const legacyAssetResolver = "/api/assets/resolve";
const supportDiagnostics = "/support.html";
const supportedCheckTypes = ["icmp", "tcp"];

let monitoredAssets = [];

function $(id) {
  return document.getElementById(id);
}

function writeJson(targetId, value) {
  const target = $(targetId);

  if (target) {
    target.textContent = JSON.stringify(value, null, 2);
  }
}

function writeText(targetId, value) {
  const target = $(targetId);

  if (target) {
    target.textContent = value;
  }
}

function statusClass(status) {
  const allowed = ["online", "degraded", "maintenance", "offline", "warning"];
  return allowed.includes(status) ? status : "warning";
}

function renderCheckResult(data) {
  if (typeof data === "string") {
    return data;
  }

  return [
    `status: ${data.status || "unknown"}`,
    `assetId: ${data.assetId || "n/a"}`,
    `asset: ${data.assetName || "n/a"}`,
    `type: ${data.checkType || "n/a"}`,
    `target: ${data.target || "n/a"}`,
    `durationMs: ${data.durationMs || 0}`,
    "",
    data.output || ""
  ].join("\n");
}

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

async function postJsonWithResponse(url, body, headers = {}) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers
    },
    body: JSON.stringify(body)
  });

  const data = await parseResponse(response);
  return { response, data };
}

async function postJson(url, body, headers = {}) {
  const { data } = await postJsonWithResponse(url, body, headers);
  return data;
}

async function login() {
  const username = $("username").value;
  const password = $("password").value;
  let result;

  try {
    result = await postJsonWithResponse("/api/auth/login", {
      username,
      password,
      client: "web",
      returnUrl: "/dashboard.html"
    });
  } catch {
    writeText("result", "Erro interno ao processar login.");
    return;
  }

  const { response, data } = result;

  if (response.ok && data.success) {
    window.location.href = "/dashboard.html";
    return;
  }

  if (response.status === 401) {
    writeText("result", "Usuário ou senha inválidos.");
    return;
  }

  writeText("result", "Erro interno ao processar login.");
}

async function logout() {
  await postJson("/api/auth/logout", {});
  window.location.href = "/login.html";
}

function renderAssets() {
  const grid = $("assetGrid");

  if (!grid) {
    return;
  }

  grid.innerHTML = monitoredAssets.map(asset => {
    return `
      <article class="asset-card">
        <span class="asset-status ${statusClass(asset.status)}"></span>
        <div class="asset-copy">
          <h3>${asset.name}</h3>
          <p>${asset.hostname}</p>
          <em>${asset.status}</em>
        </div>
        <button class="asset-action" data-action="check-asset" data-asset-id="${asset.id}">Verificar</button>
      </article>
    `;
  }).join("");
}

async function loadAssets() {
  const grid = $("assetGrid");

  if (!grid) {
    return;
  }

  const response = await fetch("/api/assets");
  const data = await parseResponse(response);

  monitoredAssets = data.assets || [];
  renderAssets();
}

async function checkAsset(assetId) {
  const asset = monitoredAssets.find(item => item.id === assetId);

  if (!asset) {
    writeText("checkResult", "Asset not found.");
    return;
  }

  writeText("checkResult", "status: running\n");

  const data = await postJson("/api/assets/check", {
    assetId: asset.id,
    checkType: "icmp",
    target: asset.hostname
  });

  writeText("checkResult", renderCheckResult(data));
}

async function loadSupportLog(event) {
  event.preventDefault();
  const file = $("logFile").value;
  writeText("supportLogResult", "Loading log stream...");
  const response = await fetch(`/api/support/log?file=${encodeURIComponent(file)}`);
  const data = await response.text();
  writeText("supportLogResult", data);
}

function bindActions() {
  document.addEventListener("click", event => {
    const action = event.target.dataset.action;

    if (!action) {
      return;
    }

    if (action === "login") {
      login();
    }

    if (action === "logout") {
      logout();
    }

    if (action === "check-asset") {
      checkAsset(event.target.dataset.assetId);
    }
  });

  const supportLogForm = $("supportLogForm");
  if (supportLogForm) {
    supportLogForm.addEventListener("submit", loadSupportLog);
  }

  const loginForm = $("loginForm");
  if (loginForm) {
    loginForm.addEventListener("submit", event => {
      event.preventDefault();
      login();
    });
  }
}

bindActions();
loadAssets();
