const express = require('express');
const cookieParser = require('cookie-parser');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

const { flags, users, logs, tickets } = require('./data');

const app = express();
const internalApp = express();

const port = Number(process.env.PORT || 8092);
const internalPort = Number(process.env.INTERNAL_PORT || 5000);
const jwtSecret = process.env.JWT_SECRET || 'neon';
const labRoot = path.join(__dirname, '..');
const publicDir = path.join(labRoot, 'public');
const downloadRoot = path.join(publicDir, 'downloads');
const uploadRoot = path.join(labRoot, 'uploads');

fs.mkdirSync(uploadRoot, { recursive: true });

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(express.urlencoded({ extended: false }));
app.use(express.json());
app.use(cookieParser());
app.use(express.static(publicDir));

const upload = multer({
  storage: multer.diskStorage({
    destination: uploadRoot,
    filename: (req, file, callback) => {
      const safeName = file.originalname.replace(/[^a-zA-Z0-9._-]/g, '_');
      callback(null, `${Date.now()}-${safeName}`);
    }
  }),
  limits: {
    fileSize: 1024 * 128
  }
});

function getCurrentUser(req) {
  const bearer = req.headers.authorization || '';
  const token = req.cookies.neon_token || bearer.replace(/^Bearer\s+/i, '');

  if (!token) {
    return null;
  }

  try {
    return jwt.verify(token, jwtSecret);
  } catch (error) {
    return null;
  }
}

app.use((req, res, next) => {
  res.locals.currentUser = getCurrentUser(req);
  res.locals.currentPath = req.path;
  res.locals.appName = 'NeonVault';
  next();
});

function requireLogin(req, res, next) {
  if (!res.locals.currentUser) {
    return res.redirect('/login');
  }

  return next();
}

function requireAdmin(req, res, next) {
  if (!res.locals.currentUser) {
    return res.redirect('/login');
  }

  if (res.locals.currentUser.role !== 'admin') {
    return res.status(403).render('error', {
      title: 'Core bloqueado',
      message: 'Token valido, mas a role nao autoriza acesso ao nucleo administrativo.'
    });
  }

  return next();
}

function issueToken(user) {
  return jwt.sign(
    {
      sub: user.id,
      username: user.username,
      displayName: user.displayName,
      role: 'user'
    },
    jwtSecret,
    { expiresIn: '4h' }
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function evaluateBlindCondition(input) {
  const admin = users.find((user) => user.username === 'admin');
  const code = admin.recoveryCode;
  const normalized = input.replace(/\s+/g, ' ');

  let match = normalized.match(/length\s*\(\s*recovery_code\s*\)\s*=\s*(\d+)/i);
  if (match) {
    return code.length === Number(match[1]);
  }

  match = normalized.match(/substr(?:ing)?\s*\(\s*recovery_code\s*,\s*(\d+)\s*,\s*1\s*\)\s*=\s*'?([a-z0-9])'?/i);
  if (match) {
    const position = Number(match[1]) - 1;
    return code[position] && code[position].toLowerCase() === match[2].toLowerCase();
  }

  match = normalized.match(/ascii\s*\(\s*substr(?:ing)?\s*\(\s*recovery_code\s*,\s*(\d+)\s*,\s*1\s*\)\s*\)\s*=\s*(\d+)/i);
  if (match) {
    const position = Number(match[1]) - 1;
    return code.charCodeAt(position) === Number(match[2]);
  }

  return false;
}

async function applyBlindDelay(input) {
  const sleepMatch = input.match(/sleep\s*\(\s*(\d+(?:\.\d+)?)\s*\)/i);
  if (!sleepMatch) {
    return false;
  }

  const seconds = Math.min(Number(sleepMatch[1]), 5);
  const shouldDelay = evaluateBlindCondition(input);

  if (shouldDelay) {
    await sleep(seconds * 1000);
  }

  return shouldDelay;
}

function renderUnsafeTemplate(source, currentUser) {
  return source.replace(/{{\s*([\s\S]*?)\s*}}/g, (fullMatch, expression) => {
    try {
      // Intentionally vulnerable for the lab: arbitrary expression evaluation.
      const fn = new Function('vault', 'user', 'Math', `return (${expression});`);
      return String(fn({ sstiSecret: flags.ssti, city: 'Neo-Sampa' }, currentUser || {}, Math));
    } catch (error) {
      return `[template-error: ${error.message}]`;
    }
  });
}

function weakUploadAllowed(originalName) {
  const lower = originalName.toLowerCase();

  return (
    lower.includes('.png') ||
    lower.includes('.jpg') ||
    lower.includes('.jpeg') ||
    lower.includes('.gif') ||
    lower.endsWith('.html') ||
    lower.endsWith('.phtml')
  );
}

function isBypassName(originalName) {
  const lower = originalName.toLowerCase();
  return lower.includes('.php.') || lower.endsWith('.html') || lower.endsWith('.phtml');
}

app.get('/', (req, res) => {
  res.render('index');
});

app.get('/login', (req, res) => {
  res.render('login', { error: null });
});

app.post('/login', (req, res) => {
  const username = String(req.body.username || '');
  const password = String(req.body.password || '');

  if (/('|--|union|sleep\s*\(|or\s+1\s*=\s*1)/i.test(username + password)) {
    return res.status(401).render('login', {
      error: 'Gateway de login rejeitou payload basico. Tente enumerar recuperacao de acesso.'
    });
  }

  const user = users.find((item) => item.username === username && item.password === password && item.username !== 'admin');

  if (!user) {
    return res.status(401).render('login', {
      error: 'Identidade ou senha invalida.'
    });
  }

  res.cookie('neon_token', issueToken(user), {
    httpOnly: true,
    sameSite: 'lax'
  });

  return res.redirect('/dashboard');
});

app.post('/logout', (req, res) => {
  res.clearCookie('neon_token');
  res.redirect('/');
});

app.get('/recover', (req, res) => {
  res.render('recover', {
    error: null,
    result: null
  });
});

app.post('/recover', (req, res) => {
  const username = String(req.body.username || '');
  const recoveryCode = String(req.body.recovery_code || '');
  const user = users.find((item) => item.username === username);

  if (user && user.username === 'admin' && user.recoveryCode === recoveryCode) {
    return res.render('recover', {
      error: null,
      result: `Codigo aceito para admin. ${flags.blindSqli}`
    });
  }

  return res.status(401).render('recover', {
    error: 'Codigo de recuperacao invalido.',
    result: null
  });
});

app.get('/dashboard', requireLogin, (req, res) => {
  res.render('dashboard', {
    stats: [
      ['Identidades ativas', '3'],
      ['Alertas neon', '7'],
      ['Webhooks pendentes', '2'],
      ['Integridade do core', 'degradada']
    ]
  });
});

app.get('/profile', requireLogin, (req, res) => {
  const user = users.find((item) => item.id === Number(res.locals.currentUser.sub));
  res.render('profile', { user });
});

app.get('/logs', requireLogin, (req, res) => {
  const level = String(req.query.level || 'error');
  const sql = `SELECT id, level, source, message FROM access_logs WHERE level = '${level}' AND hidden = 0`;
  const injected = /('|--|\/\*|\bor\b|\bunion\b|1\s*=\s*1)/i.test(level);
  const visibleLogs = injected
    ? logs
    : logs.filter((entry) => entry.level === level && entry.level !== 'hidden');

  res.render('logs', {
    level,
    sql,
    injected,
    logs: visibleLogs
  });
});

app.get('/files', requireLogin, (req, res) => {
  res.render('files', {
    files: ['report.pdf', 'identity-map.txt', 'ops-readme.txt']
  });
});

app.get('/download', (req, res) => {
  const requestedFile = req.query.file;

  if (!requestedFile) {
    return res.render('download', {
      error: null,
      files: ['report.pdf', 'identity-map.txt', 'ops-readme.txt']
    });
  }

  // Intentionally vulnerable: no canonical path boundary check.
  const targetPath = path.join(downloadRoot, requestedFile);

  fs.readFile(targetPath, 'utf8', (error, content) => {
    if (error) {
      return res.status(404).render('download', {
        error: 'Arquivo nao encontrado no downloader legado.',
        files: ['report.pdf', 'identity-map.txt', 'ops-readme.txt']
      });
    }

    res.type('text/plain');
    return res.send(content);
  });
});

app.get('/admin/core', requireAdmin, (req, res) => {
  res.render('admin-core', {
    flag: flags.jwt
  });
});

app.get('/messages/preview', requireLogin, (req, res) => {
  res.render('messages-preview', {
    template: 'Operador {{user.username}} sincronizado em {{vault.city}}.',
    preview: null
  });
});

app.post('/messages/preview', requireLogin, (req, res) => {
  const template = String(req.body.template || '');
  const preview = renderUnsafeTemplate(template, res.locals.currentUser);

  res.render('messages-preview', {
    template,
    preview
  });
});

app.get('/avatar', requireLogin, (req, res) => {
  res.render('avatar', {
    error: null,
    upload: null,
    flag: null
  });
});

app.post('/avatar', requireLogin, upload.single('avatar'), (req, res) => {
  if (!req.file) {
    return res.status(400).render('avatar', {
      error: 'Nenhum arquivo enviado.',
      upload: null,
      flag: null
    });
  }

  if (!weakUploadAllowed(req.file.originalname)) {
    fs.unlink(req.file.path, () => {});
    return res.status(400).render('avatar', {
      error: 'Extensao recusada pelo filtro legado.',
      upload: null,
      flag: null
    });
  }

  const body = fs.readFileSync(req.file.path, 'utf8');
  const bypassed = isBypassName(req.file.originalname) || body.includes('NEON_UPLOAD_PROBE');

  return res.render('avatar', {
    error: null,
    upload: {
      originalName: req.file.originalname,
      storedName: req.file.filename,
      url: `/uploads/${encodeURIComponent(req.file.filename)}`
    },
    flag: bypassed ? flags.upload : null
  });
});

app.get('/uploads/:name', requireLogin, (req, res) => {
  const safeName = path.basename(req.params.name);
  const targetPath = path.join(uploadRoot, safeName);

  fs.readFile(targetPath, 'utf8', (error, content) => {
    if (error) {
      return res.status(404).render('error', {
        title: 'Upload nao encontrado',
        message: 'O asset solicitado nao existe mais no cache neon.'
      });
    }

    if (safeName.toLowerCase().endsWith('.html')) {
      res.type('html');
      return res.send(content);
    }

    res.type('text/plain');
    return res.send(content);
  });
});

app.get('/tools/webhook', requireLogin, (req, res) => {
  res.render('webhook', {
    targetUrl: 'https://example.com',
    response: null,
    error: null
  });
});

app.post('/tools/webhook', requireLogin, async (req, res) => {
  const targetUrl = String(req.body.url || '');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 4000);

  try {
    const response = await fetch(targetUrl, { signal: controller.signal });
    const body = await response.text();

    return res.render('webhook', {
      targetUrl,
      response: `HTTP ${response.status}\n${body}`,
      error: null
    });
  } catch (error) {
    return res.status(502).render('webhook', {
      targetUrl,
      response: null,
      error: error.message
    });
  } finally {
    clearTimeout(timeout);
  }
});

app.get('/api/check-user', async (req, res) => {
  const startedAt = Date.now();
  const username = String(req.query.username || '');
  const unsafeSql = `SELECT id FROM identities WHERE username = '${username}' LIMIT 1`;

  await applyBlindDelay(username);

  const exists = users.some((user) => user.username === username);

  res.json({
    ok: true,
    exists,
    elapsed_ms: Date.now() - startedAt,
    debug_query: unsafeSql
  });
});

app.get('/api/tickets/:id', requireLogin, (req, res) => {
  const ticket = tickets.find((item) => item.id === Number(req.params.id));

  if (!ticket) {
    return res.status(404).json({ error: 'ticket_not_found' });
  }

  // Intentionally vulnerable: authenticated users can read any ticket ID.
  return res.json(ticket);
});

internalApp.get('/internal/status', (req, res) => {
  res.json({
    service: 'neonvault-internal-metadata',
    host: '127.0.0.1',
    status: 'online',
    hint: '/internal/flag'
  });
});

internalApp.get('/internal/flag', (req, res) => {
  res.type('text/plain');
  res.send(flags.ssrf);
});

app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Rota perdida na chuva neon',
    message: 'O recurso solicitado nao existe neste setor do vault.'
  });
});

app.use((error, req, res, next) => {
  console.error(error);
  res.status(500).render('error', {
    title: 'Falha no core',
    message: 'O NeonVault encontrou uma excecao inesperada.'
  });
});

internalApp.listen(internalPort, '127.0.0.1', () => {
  console.log(`Internal NeonVault service listening on 127.0.0.1:${internalPort}`);
});

app.listen(port, () => {
  console.log(`NeonVault listening on http://127.0.0.1:${port}`);
});
