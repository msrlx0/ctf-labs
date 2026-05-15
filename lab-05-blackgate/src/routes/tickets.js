const express = require("express");
const { canViewTicket, tickets } = require("../data/seed");
const { requireAuth } = require("../utils/session");

const router = express.Router();

router.get("/tickets", requireAuth, (req, res) => {
  const visibleTickets = tickets.filter((ticket) => canViewTicket(req.session.user, ticket));
  const showApiLinks = ["operator", "admin"].includes(req.session.user.role);

  return res.renderPage("tickets", {
    title: "Tickets",
    tickets: visibleTickets,
    showApiLinks
  });
});

module.exports = router;
