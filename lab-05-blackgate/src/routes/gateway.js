const express = require("express");
const { listInternalServices } = require("../utils/internalServices");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/gateway", requireAuth, (req, res) => {
  return res.renderPage("gateway", {
    title: "Gateway",
    allowedHosts: listInternalServices()
  });
});

module.exports = router;
