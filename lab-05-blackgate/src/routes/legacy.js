const express = require("express");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/legacy", requireAuth, (req, res) => {
  return res.renderPage("legacy", {
    title: "Legacy"
  });
});

module.exports = router;
