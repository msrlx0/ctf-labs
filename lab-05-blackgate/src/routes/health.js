const express = require("express");

const router = express.Router();

router.get("/health", (req, res) => {
  return res.json({
    service: "blackgate",
    status: "ok",
    version: "1.7.0-phase8"
  });
});

module.exports = router;
