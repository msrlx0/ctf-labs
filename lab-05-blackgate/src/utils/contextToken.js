function encodeContext(payload) {
  return Buffer.from(JSON.stringify(payload), "utf8").toString("base64url");
}

function decodeContext(token) {
  try {
    const decoded = Buffer.from(String(token || ""), "base64url").toString("utf8");
    const context = JSON.parse(decoded);

    if (!context || typeof context !== "object" || Array.isArray(context)) {
      return null;
    }

    return context;
  } catch (error) {
    return null;
  }
}

function scopeForRole(role) {
  if (role === "operator") {
    return "operations";
  }

  if (role === "analyst") {
    return "analysis";
  }

  if (role === "admin") {
    return "admin";
  }

  return "limited";
}

function buildContextForUser(user) {
  return {
    user: user.username,
    role: user.role,
    scope: scopeForRole(user.role),
    issued_by: "legacy-context-service"
  };
}

function sendJsonError(res, status, error, message) {
  return res.status(status).json({ error, message });
}

function readContextToken(req) {
  return req.get("X-BG-Context")
    || (req.cookies ? req.cookies.bg_context : "");
}

function requireOperatorContext(req, res, next) {
  if (!req.session || !req.session.user) {
    return sendJsonError(res, 401, "authentication_required", "Login required to access this resource.");
  }

  const token = readContextToken(req);

  if (!token) {
    return sendJsonError(res, 400, "bad_request", "Context token is required.");
  }

  const context = decodeContext(token);

  if (!context) {
    return sendJsonError(res, 400, "bad_request", "Context token is required.");
  }

  if (context.role !== "operator" && context.scope !== "operations") {
    return sendJsonError(res, 403, "forbidden", "Valid operator context required.");
  }

  req.bgContext = context;
  return next();
}

module.exports = {
  encodeContext,
  decodeContext,
  buildContextForUser,
  requireOperatorContext
};
