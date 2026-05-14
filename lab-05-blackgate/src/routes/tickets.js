const express = require("express");
const { tickets } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/tickets", requireAuth, (req, res) => {
  return res.renderPage("tickets", {
    title: "Tickets",
    tickets
  });
});

module.exports = router;
