const express = require("express");
const fs = require("fs");
const path = require("path");

const router = express.Router();

function getSession(req) {
  try {
    return JSON.parse(Buffer.from(req.cookies.session || "", "base64").toString("utf8"));
  } catch {
    return null;
  }
}

function requireLogin(req, res, next) {
  const session = getSession(req);

  if (!session || !session.username) {
    return res.status(401).type("text/plain").send("Authentication required");
  }

  req.user = session;
  next();
}

router.get("/log", requireLogin, (req, res) => {
  const file = String(req.query.file || "app.log");
  // Intentional lab vulnerability: traversal is possible because the final path is not validated.
  const requestedPath = path.join("/app/data", file);

  try {
    const content = fs.readFileSync(requestedPath, "utf8");
    return res
      .set("X-NetAudit-Viewer", "support-diagnostics")
      .set("X-NetAudit-Incident", "NT-2026-041")
      .type("text/plain")
      .send(content);
  } catch {
    return res.status(404).type("text/plain").send("File not found");
  }
});

module.exports = router;
