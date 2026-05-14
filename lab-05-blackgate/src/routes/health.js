const express = require("express");

const router = express.Router();

router.get("/health", (req, res) => {
  return res.json({
    service: "blackgate",
    status: "ok",
    version: "1.1.0-phase2"
  });
});

module.exports = router;
