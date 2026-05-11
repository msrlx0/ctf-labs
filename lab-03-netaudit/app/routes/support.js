const express = require("express");
const fs = require("fs");
const path = require("path");

const router = express.Router();

function requireLogin(req, res, next) {
  if (!req.cookies.session) {
    return res.status(401).type("text/plain").send("Authentication required");
  }

  next();
}

router.get("/log", requireLogin, (req, res) => {
  const file = req.query.file || "app.log";
  const basePath = path.join("/app", "data");
  // Intentional lab vulnerability: traversal is possible because the final path is not validated.
  const requestedPath = path.join(basePath, file);

  try {
    const content = fs.readFileSync(requestedPath, "utf8");
    return res.type("text/plain").send(content);
  } catch {
    return res.status(404).type("text/plain").send("File not found");
  }
});

module.exports = router;
