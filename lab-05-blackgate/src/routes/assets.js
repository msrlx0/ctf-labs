const express = require("express");
const { assets } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/assets", requireAuth, (req, res) => {
  return res.renderPage("assets", {
    title: "Assets",
    assets
  });
});

module.exports = router;
