const express = require("express");
const { requirePageRole } = require("../utils/session");

const router = express.Router();

router.get("/legacy", requirePageRole(["operator", "admin"]), (req, res) => {
  return res.renderPage("legacy", {
    title: "Legacy"
  });
});

module.exports = router;
