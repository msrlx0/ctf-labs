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

router.post("/login", (req, res) => {
  const { username, password } = req.body;
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
