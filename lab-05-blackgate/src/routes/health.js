const express = require("express");

const router = express.Router();

router.get("/health", (req, res) => {
  return res.json({
    service: "blackgate",
    status: "ok",
    version: "1.8.0-phase9"
  });
});

module.exports = router;
