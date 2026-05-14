const express = require("express");

const router = express.Router();

router.get("/health", (req, res) => {
  return res.json({
    service: "blackgate",
    status: "ok",
    version: "1.5.0-phase6"
  });
});

module.exports = router;
