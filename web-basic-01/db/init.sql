CREATE DATABASE IF NOT EXISTS minibank;
USE minibank;

DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(120) NOT NULL,
  role VARCHAR(30) NOT NULL
);

CREATE TABLE accounts (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  owner_name VARCHAR(120) NOT NULL,
  balance DECIMAL(12, 2) NOT NULL,
  account_number VARCHAR(30) NOT NULL,
  secret_note VARCHAR(255) NOT NULL,
  CONSTRAINT fk_accounts_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
);

CREATE TABLE transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  account_id INT NOT NULL,
  description VARCHAR(160) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  CONSTRAINT fk_transactions_account
    FOREIGN KEY (account_id) REFERENCES accounts(id)
    ON DELETE CASCADE
);

INSERT INTO users (id, username, password, role) VALUES
  (1, 'admin', 'S9x!vK2#internal_only_2026', 'admin'),
  (2, 'joao', 'joao123', 'employee'),
  (3, 'maria', 'maria123', 'employee'),
  (4, 'auditor', 'audit2026', 'auditor');

INSERT INTO accounts (id, user_id, owner_name, balance, account_number, secret_note) VALUES
  (1, 2, 'Joao Silva', 12840.55, 'MB-1001-2026', 'Conta operacional sem restricoes especiais.'),
  (2, 3, 'Maria Oliveira', 98220.10, 'MB-1002-2026', 'FLAG{idor_capturada}'),
  (3, 4, 'Carlos Backup', 4430.00, 'MB-1003-2026', 'Conta usada para testes do modulo de backup legado.'),
  (4, 1, 'Admin User', 250000.00, 'MB-0001-ROOT', 'Conta administrativa de reconciliacao interna.'),
  (5, 2, 'Joao Silva - Reserva', 3210.75, 'MB-1001-RES', 'Conta reserva vinculada ao atendimento interno.');

INSERT INTO transactions (account_id, description, amount) VALUES
  (1, 'Deposito folha interna', 5800.00),
  (1, 'Pagamento fornecedor local', -1220.45),
  (1, 'Ajuste manual de saldo', 140.00),
  (2, 'Recebimento contrato corporate', 42000.00),
  (2, 'Transferencia agencia central', -3100.25),
  (2, 'Credito conciliacao Q2', 850.35),
  (3, 'Teste de restauracao de backup', 100.00),
  (3, 'Taxa de manutencao legado', -25.00),
  (4, 'Reserva administrativa', 100000.00),
  (4, 'Reconciliacao manual', -4500.00),
  (5, 'Transferencia entre contas', 750.00),
  (5, 'Pagamento de servico', -90.15);
