function requireLogin(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.redirect('/login');
  }

  return next();
}

function requireAdmin(req, res, next) {
  if (!req.session || !req.session.user) {
    return res.redirect('/login');
  }

  if (req.session.user.role !== 'admin') {
    return res.status(403).render('error', {
      title: 'Acesso negado',
      message: 'Este painel e restrito a administradores do MiniBank.'
    });
  }

  return next();
}

function requireBackupAuth(req, res, next) {
  const header = req.headers.authorization || '';

  if (!header.startsWith('Basic ')) {
    res.set('WWW-Authenticate', 'Basic realm="MiniBank Backup"');
    return res.status(401).render('error', {
      title: 'Backup protegido',
      message: 'Credenciais de backup sao necessarias para acessar este recurso legado.'
    });
  }

  const encoded = header.slice('Basic '.length);
  const decoded = Buffer.from(encoded, 'base64').toString('utf8');
  const separatorIndex = decoded.indexOf(':');
  const username = separatorIndex >= 0 ? decoded.slice(0, separatorIndex) : decoded;
  const password = separatorIndex >= 0 ? decoded.slice(separatorIndex + 1) : '';

  if (username === 'backup_user' && password === 'backup123') {
    return next();
  }

  res.set('WWW-Authenticate', 'Basic realm="MiniBank Backup"');
  return res.status(401).render('error', {
    title: 'Credencial invalida',
    message: 'O usuario ou senha de backup informado nao foi aceito.'
  });
}

module.exports = {
  requireLogin,
  requireAdmin,
  requireBackupAuth
};

