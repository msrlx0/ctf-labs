const express = require("express");

const router = express.Router();

router.get("/robots.txt", (req, res) => {
  res.type("text/plain");
  return res.send([
    "User-agent: *",
    "Disallow: /admin",
    "Disallow: /debug",
    "Disallow: /legacy",
    "Disallow: /api/internal",
    "Disallow: /backups"
  ].join("\n"));
});

router.get("/.well-known/security.txt", (req, res) => {
  res.type("text/plain");
  return res.send([
    "Contact: security@blackgate.local",
    "Preferred-Languages: en, pt-BR",
    "Policy: http://localhost:8096/security-policy",
    "Hiring: http://localhost:8096/careers",
    "Expires: 2027-12-31T23:59:59.000Z"
  ].join("\n"));
});

router.get("/security-policy", (req, res) => {
  return res.renderPage("security-policy", {
    title: "Security Policy"
  });
});

module.exports = router;
