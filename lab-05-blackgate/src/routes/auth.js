const express = require("express");
const { findUser } = require("../data/seed");
const { createSessionUser, redirectIfAuthenticated } = require("../utils/session");

const router = express.Router();

router.get("/login", redirectIfAuthenticated, (req, res) => {
  return res.renderPage("login", {
    title: "Login",
    error: null
  });
});

router.post("/login", redirectIfAuthenticated, (req, res) => {
  const username = String(req.body.username || "").trim();
  const password = String(req.body.password || "");
  const user = findUser(username);

  if (!user || user.locked || user.password !== password) {
    return res.status(401).renderPage("login", {
      title: "Login",
      error: "Credenciais invalidas ou conta indisponivel nesta fase."
    });
  }

  req.session.user = createSessionUser(user);
  return res.redirect("/dashboard");
});

router.get("/logout", (req, res) => {
  req.session.destroy(() => {
    res.clearCookie("blackgate.sid");
    return res.redirect("/login");
  });
});

module.exports = router;
