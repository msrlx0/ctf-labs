const express = require("express");
const {
  buildContextForUser,
  decodeContext,
  encodeContext
} = require("../utils/contextToken");
const { requireAuth } = require("../utils/session");

const router = express.Router();

function sendJsonError(res, status, error, message) {
  return res.status(status).json({ error, message });
}

function requireApiAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return sendJsonError(res, 401, "authentication_required", "Login required to access this resource.");
  }

  return next();
}

router.get("/context", requireAuth, (req, res) => {
  return res.renderPage("context", {
    title: "Context",
    contextPreview: buildContextForUser(req.session.user)
  });
});

router.get("/api/context/me", requireApiAuth, (req, res) => {
  const context = buildContextForUser(req.session.user);
  const token = encodeContext(context);

  return res.json({
    user: context.user,
    role: context.role,
    scope: context.scope,
    context_token: token,
    hint: "Context tokens are consumed by legacy operator endpoints."
  });
});

router.post("/api/context/verify", requireApiAuth, (req, res) => {
  const token = req.get("X-BG-Context") || (req.body ? req.body.token : "");

  if (!token) {
    return sendJsonError(res, 400, "bad_request", "Context token is required.");
  }

  const context = decodeContext(token);

  if (!context) {
    return res.status(400).json({
      valid: false,
      error: "invalid_context_token"
    });
  }

  return res.json({
    valid: true,
    context,
    warning: "Legacy context tokens are not intended for direct client manipulation."
  });
});

module.exports = router;
