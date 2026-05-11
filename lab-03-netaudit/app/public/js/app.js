// legacy resolver kept for support troubleshooting
const legacyResolverEndpoint = "/api/tools/resolve";
const supportDiagnostics = "/support.html";

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

async function parseResponse(response) {
  const contentType = response.headers.get("content-type") || "";

  if (contentType.includes("application/json")) {
    return response.json();
  }

  return response.text();
}

async function postJson(url, body, headers = {}) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers
    },
    body: JSON.stringify(body)
  });

  return parseResponse(response);
}

async function login() {
  const username = $("username").value;
  const password = $("password").value;
  const data = await postJson("/api/auth/login", { username, password });

  writeJson("result", data);

  if (data.success) {
    window.location.href = "/dashboard.html";
  }
}

async function logout() {
  await postJson("/api/auth/logout", {});
  window.location.href = "/login.html";
}

async function checkAsset(event) {
  event.preventDefault();
  const host = $("assetHost").value;
  const data = await postJson("/api/tools/check", { host });
  writeJson("checkResult", data);
}

async function loadSupportLog(event) {
  event.preventDefault();
  const file = $("logFile").value;
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

    const handlers = {
      login,
      logout
    };

    if (handlers[action]) {
      handlers[action]();
    }
  });

  const assetCheckForm = $("assetCheckForm");
  if (assetCheckForm) {
    assetCheckForm.addEventListener("submit", checkAsset);
  }

  const supportLogForm = $("supportLogForm");
  if (supportLogForm) {
    supportLogForm.addEventListener("submit", loadSupportLog);
  }
}

bindActions();
