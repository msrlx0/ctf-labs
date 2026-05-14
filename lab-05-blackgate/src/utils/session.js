function requireAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.redirect("/login");
  }

  return next();
}

function redirectIfAuthenticated(req, res, next) {
  if (req.session && req.session.user) {
    return res.redirect("/dashboard");
  }

  return next();
}

function createSessionUser(user) {
  return {
    id: user.id,
    username: user.username,
    displayName: user.displayName,
    role: user.role,
    team: user.team
  };
}

module.exports = {
  requireAuth,
  redirectIfAuthenticated,
  createSessionUser
};
