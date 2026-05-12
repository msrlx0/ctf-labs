const express = require("express");
const { exec } = require("child_process");

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
    return res.status(401).json({
      ok: false,
      error: "Authentication required"
    });
  }

  req.user = session;
  next();
}

router.post("/tcp-probe", requireLogin, (req, res) => {
  const { target, port } = req.body;

  if (!target || !port) {
    return res.status(400).json({
      ok: false,
      error: "target and port are required"
    });
  }

  const startedAt = Date.now();
  // Intentional lab vulnerability: target and port are concatenated into a shell command.
  const command = `nc -zvw2 ${target} ${port}`;

  exec(command, { timeout: 8000 }, error => {
    const timedOut = Boolean(error && (error.killed || error.signal === "SIGTERM"));

    return res.json({
      ok: !error,
      status: timedOut ? "completed_with_timeout" : "completed",
      target,
      port,
      durationMs: Date.now() - startedAt
    });
  });
});

module.exports = router;
