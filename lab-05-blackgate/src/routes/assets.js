const express = require("express");
const { assets, canViewAssetInInterface } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/assets", requireAuth, (req, res) => {
  const visibleAssets = assets.filter((asset) => canViewAssetInInterface(req.session.user, asset));

  return res.renderPage("assets", {
    title: "Assets",
    assets: visibleAssets
  });
});

module.exports = router;
