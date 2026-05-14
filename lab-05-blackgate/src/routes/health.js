const express = require("express");

const router = express.Router();

router.get("/health", (req, res) => {
  return res.json({
    service: "blackgate",
    status: "ok",
    version: "1.3.0-phase4"
  });
});

module.exports = router;
