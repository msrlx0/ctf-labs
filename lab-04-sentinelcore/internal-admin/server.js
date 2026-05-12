const express = require("express");

const app = express();

const PORT = Number(process.env.PORT || 8081);
const INTERNAL_ADMIN_TOKEN = process.env.INTERNAL_ADMIN_TOKEN || "internal-admin-token-7f3a9c21";

app.use(express.json());

function requireInternalToken(req, res, next) {
  const token = req.headers["x-internal-token"];

  if (token !== INTERNAL_ADMIN_TOKEN) {
    return res.status(403).json({
      ok: false,
      error: "missing or invalid internal token"
    });
  }

  return next();
}

app.get("/status", (req, res) => {
  return res.json({
    service: "internal-admin",
    status: "ok",
    flag: "flag{ssrf_reached_internal_admin}"
  });
});

app.get("/internal/users", requireInternalToken, (req, res) => {
  return res.json({
    service: "internal-admin",
    users: [
      { id: 1, username: "intern", role: "intern", tenant: "training" },
      { id: 2, username: "analyst", role: "analyst", tenant: "soc" },
      { id: 3, username: "admin", role: "admin", tenant: "platform" }
    ],
    flag: "flag{internal_admin_token_accepted}"
  });
});

app.get("/internal/config", requireInternalToken, (req, res) => {
  return res.json({
    service: "internal-admin",
    trustedNetwork: "sentinel-net",
    queue: "sentinel:jobs",
    worker: {
      service: "worker",
      outputDirectory: "/shared",
      supportedJobTypes: ["report.export", "token.debug", "file.read"]
    },
    note: "Worker writes exported artifacts into the shared output volume.",
    flag: "flag{internal_config_disclosed}"
  });
});

app.use((req, res) => {
  return res.status(404).json({ ok: false, error: "internal route not found" });
});

app.listen(PORT, () => {
  console.log(`Internal admin listening on ${PORT}`);
});
