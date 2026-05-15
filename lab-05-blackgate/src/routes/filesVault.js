const express = require("express");
const { requirePageRole } = require("../utils/session");

const router = express.Router();

router.get("/files-vault", requirePageRole(["operator", "admin"]), (req, res) => {
  return res.renderPage("files-vault", {
    title: "Files Vault"
  });
});

module.exports = router;
