const express = require("express");
const { assets, canViewAssetInInterface } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/assets", requireAuth, (req, res) => {
  const visibleAssets = assets.filter((asset) => canViewAssetInInterface(req.session.user, asset));
  const showApiLinks = ["operator", "admin"].includes(req.session.user.role);
  const limitedAssetView = req.session.user.role === "guest";
  const assetsForView = limitedAssetView
    ? visibleAssets.map((asset) => ({
        ...asset,
        type: "Service",
        environment: "training",
        exposure: "scoped",
        note: "Servico disponivel para revisao inicial."
      }))
    : visibleAssets;

  return res.renderPage("assets", {
    title: "Assets",
    assets: assetsForView,
    showApiLinks,
    limitedAssetView
  });
});

module.exports = router;
