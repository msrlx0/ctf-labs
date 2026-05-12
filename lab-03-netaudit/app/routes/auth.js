const express = require("express");

const router = express.Router();

const users = [
  {
    id: 1,
    username: "analyst",
    password: "analyst123",
    role: "analyst"
  }
];

function isMalformedReturnUrl(returnUrl) {
  if (returnUrl === undefined || returnUrl === "/dashboard.html") {
    return false;
  }

  return typeof returnUrl !== "string" || returnUrl.includes("'") || !returnUrl.startsWith("/");
}

function buildRedirectDebugError(returnUrl) {
  const token = JSON.stringify(returnUrl);

  return [
    "NetAudit Login Redirect Error",
    "",
    `Error: malformed returnUrl token ${token}`,
    "",
    "    at RedirectParser.parse (/app/server/redirect-parser.js:27:11)",
    "    at AuthController.login (/app/routes/auth.js:42:9)",
    "    at Layer.handle [as handle_request] (/app/node_modules/express/lib/router/layer.js:95:5)",
    "",
    "Debug context:",
    "legacy notes root: /app/public/old/",
    "backup export root: /app/public/backup/",
    "last migration note: /old/deployment-notes.txt",
    "candidate export note: /backup/readme.txt",
    "",
    "Environment:",
    "NODE_ENV=production",
    "debug_login_redirects=true"
  ].join("\n");
}

router.post("/login", (req, res) => {
  const { username, password, returnUrl } = req.body;

  if (isMalformedReturnUrl(returnUrl)) {
    return res.status(500).type("text/plain").send(buildRedirectDebugError(returnUrl));
  }

  const user = users.find(item => item.username === username && item.password === password);

  if (!user) {
    return res.status(401).json({
      ok: false,
      success: false,
      message: "Invalid credentials"
    });
  }

  const session = Buffer.from(JSON.stringify({
    id: user.id,
    username: user.username,
    role: user.role
  })).toString("base64");

  res.cookie("session", session, {
    httpOnly: false,
    sameSite: "lax"
  });

  return res.status(200).json({
    ok: true,
    success: true,
    message: "Login successful",
    user: {
      id: user.id,
      username: user.username,
      role: user.role
    }
  });
});

router.get("/me", (req, res) => {
  const session = req.cookies.session;

  if (!session) {
    return res.status(401).json({
      ok: false,
      authenticated: false
    });
  }

  try {
    const user = JSON.parse(Buffer.from(session, "base64").toString());
    return res.json({
      ok: true,
      authenticated: true,
      user
    });
  } catch {
    return res.status(401).json({
      ok: false,
      authenticated: false
    });
  }
});

router.post("/logout", (req, res) => {
  res.clearCookie("session");
  return res.json({
    ok: true,
    success: true
  });
});

module.exports = router;
