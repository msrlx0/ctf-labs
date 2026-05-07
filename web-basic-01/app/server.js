const express = require('express');
const session = require('express-session');
const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const { requireLogin, requireAdmin, requireBackupAuth } = require('./middleware/auth');

const app = express();
const port = Number(process.env.PORT || 8080);
const appVersion = '1.4.2-dev';
const appName = 'MiniBank Internal Portal';
const filesDir = path.join(__dirname, 'files');

let pool;

async function waitForDatabase(maxAttempts = 30) {
  const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: Number(process.env.DB_PORT || 3306),
    user: process.env.DB_USER || 'minibank',
    password: process.env.DB_PASSWORD || 'minibankpass',
    database: process.env.DB_NAME || 'minibank',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
  };

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    try {
      pool = mysql.createPool(dbConfig);
      await pool.query('SELECT 1');
      console.log(`Connected to MySQL at ${dbConfig.host}:${dbConfig.port}`);
      return;
    } catch (error) {
      console.log(`Waiting for MySQL (${attempt}/${maxAttempts}): ${error.message}`);
      if (pool) {
        await pool.end().catch(() => {});
      }
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }

  throw new Error('MySQL did not become available in time.');
}

function asyncRoute(handler) {
  return (req, res, next) => {
    Promise.resolve(handler(req, res, next)).catch(next);
  };
}

app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

app.use(express.urlencoded({ extended: false }));
app.use(express.static(path.join(__dirname, 'public')));
app.use(session({
  secret: process.env.SESSION_SECRET || 'local-dev-secret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    sameSite: 'lax'
  }
}));

app.use((req, res, next) => {
  res.locals.currentUser = req.session.user || null;
  res.locals.appName = appName;
  res.locals.appVersion = appVersion;
  next();
});

app.get('/', (req, res) => {
  res.render('index');
});

app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    app: appName,
    version: appVersion,
    env: 'development',
    internal_path: '/usr/src/app',
    database: 'mysql://minibank-db:3306/minibank'
  });
});

app.get('/login', (req, res) => {
  res.render('login', {
    error: null,
    username: ''
  });
});

app.post('/login', asyncRoute(async (req, res) => {
  const username = req.body.username || '';
  const password = req.body.password || '';

  // Intentionally vulnerable for the local CTF: direct string concatenation enables SQL Injection.
  const sql = "SELECT id, username, role FROM users WHERE username = '" + username + "' AND password = '" + password + "' LIMIT 1";
  const [rows] = await pool.query(sql);

  if (rows.length === 0) {
    return res.status(401).render('login', {
      error: 'Usuario ou senha invalidos.',
      username
    });
  }

  req.session.user = rows[0];
  return res.redirect('/dashboard');
}));

app.post('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/');
  });
});

app.get('/dashboard', requireLogin, asyncRoute(async (req, res) => {
  let accountQuery = 'SELECT id, owner_name, balance, account_number FROM accounts WHERE user_id = ? ORDER BY id';
  let params = [req.session.user.id];

  if (req.session.user.role === 'admin') {
    accountQuery = 'SELECT id, owner_name, balance, account_number FROM accounts ORDER BY id';
    params = [];
  }

  const [accounts] = await pool.query(accountQuery, params);
  res.render('dashboard', { accounts });
}));

app.get('/account/:id', requireLogin, asyncRoute(async (req, res) => {
  const accountId = Number(req.params.id);

  if (!Number.isInteger(accountId) || accountId < 1) {
    return res.status(400).render('error', {
      title: 'Conta invalida',
      message: 'O identificador de conta informado nao e valido.'
    });
  }

  // Intentionally vulnerable IDOR: verifies authentication, but not account ownership.
  const [accounts] = await pool.query(
    'SELECT id, user_id, owner_name, balance, account_number, secret_note FROM accounts WHERE id = ?',
    [accountId]
  );

  if (accounts.length === 0) {
    return res.status(404).render('error', {
      title: 'Conta nao encontrada',
      message: 'Nenhuma conta foi encontrada para o identificador solicitado.'
    });
  }

  const [transactions] = await pool.query(
    'SELECT id, description, amount FROM transactions WHERE account_id = ? ORDER BY id DESC',
    [accountId]
  );

  const account = accounts[0];
  const isForeignAccount = account.user_id !== req.session.user.id;

  res.render('account', {
    account,
    transactions,
    isForeignAccount
  });
}));

app.get('/backup', requireBackupAuth, (req, res) => {
  res.render('backup', {
    files: [
      'report-q1.txt',
      'report-q2.txt',
      'public-info.txt'
    ]
  });
});

app.get('/download', asyncRoute(async (req, res) => {
  const requestedFile = req.query.file;

  if (!requestedFile) {
    return res.render('download', {
      error: null,
      files: [
        'report-q1.txt',
        'report-q2.txt',
        'public-info.txt'
      ]
    });
  }

  // Intentionally vulnerable for the local CTF: no canonical path boundary check.
  const targetPath = path.join(filesDir, requestedFile);

  fs.readFile(targetPath, 'utf8', (error, content) => {
    if (error) {
      return res.status(404).render('download', {
        error: 'Arquivo nao encontrado ou indisponivel no downloader legado.',
        files: [
          'report-q1.txt',
          'report-q2.txt',
          'public-info.txt'
        ]
      });
    }

    res.type('text/plain');
    return res.send(content);
  });
}));

app.get('/admin', requireAdmin, asyncRoute(async (req, res) => {
  const [userCountRows] = await pool.query('SELECT COUNT(*) AS total FROM users');
  const [accountCountRows] = await pool.query('SELECT COUNT(*) AS total FROM accounts');

  res.render('admin', {
    userCount: userCountRows[0].total,
    accountCount: accountCountRows[0].total
  });
}));

app.use((req, res) => {
  res.status(404).render('error', {
    title: 'Pagina nao encontrada',
    message: 'O recurso solicitado nao existe neste portal interno.'
  });
});

app.use((error, req, res, next) => {
  console.error(error);
  res.status(500).render('error', {
    title: 'Erro interno',
    message: 'O portal encontrou um erro ao processar a requisicao.'
  });
});

waitForDatabase()
  .then(() => {
    app.listen(port, () => {
      console.log(`${appName} listening on port ${port}`);
    });
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
