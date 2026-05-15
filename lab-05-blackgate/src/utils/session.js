function requireAuth(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.redirect("/login");
  }

  return next();
}

function userHasRole(user, allowedRoles) {
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];

  return Boolean(user && roles.includes(user.role));
}

function requirePageRole(allowedRoles) {
  return (req, res, next) => {
    if (!req.session || !req.session.user) {
      return res.redirect("/login");
    }

    if (!userHasRole(req.session.user, allowedRoles)) {
      return res.status(403).renderPage("error", {
        title: "Acesso restrito",
        statusCode: 403,
        message: "Esta area nao esta disponivel para sua funcao atual."
      });
    }

    return next();
  };
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
  requirePageRole,
  redirectIfAuthenticated,
  createSessionUser,
  userHasRole
};
