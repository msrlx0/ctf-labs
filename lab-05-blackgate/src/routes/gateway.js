const express = require("express");
const { listInternalServices } = require("../utils/internalServices");
const { requirePageRole } = require("../utils/session");

const router = express.Router();

router.get("/gateway", requirePageRole(["operator", "admin"]), (req, res) => {
  return res.renderPage("gateway", {
    title: "Gateway",
    allowedHosts: listInternalServices()
  });
});

module.exports = router;
