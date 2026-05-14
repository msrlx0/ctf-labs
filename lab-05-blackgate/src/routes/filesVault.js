const express = require("express");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/files-vault", requireAuth, (req, res) => {
  return res.renderPage("files-vault", {
    title: "Files Vault"
  });
});

module.exports = router;
