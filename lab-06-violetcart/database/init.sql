CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(128) NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'buyer',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cars (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  slug VARCHAR(128) NOT NULL UNIQUE,
  price_cents INT NOT NULL,
  year INT NOT NULL,
  mileage INT NOT NULL,
  color VARCHAR(64) NOT NULL,
  vin VARCHAR(64) NOT NULL,
  seller_ref VARCHAR(64) NOT NULL,
  inspection_status VARCHAR(64) NOT NULL,
  import_status VARCHAR(64) NOT NULL,
  partner_only TINYINT(1) NOT NULL DEFAULT 0,
  seller_priority INT NOT NULL DEFAULT 0,
  description TEXT NOT NULL
);

CREATE TABLE quotes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  car_id INT NOT NULL,
  public_token VARCHAR(64) NOT NULL,
  term_months INT NOT NULL,
  down_payment_cents INT NOT NULL,
  monthly_cents INT NOT NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'draft',
  channel VARCHAR(64) NOT NULL DEFAULT 'public_checkout',
  cache_key VARCHAR(128) NOT NULL,
  partner_hint VARCHAR(128) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  quote_id INT NOT NULL,
  car_id INT NOT NULL,
  public_token VARCHAR(64) NOT NULL,
  reservation_ref VARCHAR(64) NOT NULL,
  internal_reservation VARCHAR(64) NULL,
  seller_ref VARCHAR(64) NULL,
  status VARCHAR(64) NOT NULL DEFAULT 'reserved',
  channel VARCHAR(64) NOT NULL DEFAULT 'public_checkout',
  seller_status VARCHAR(64) NOT NULL DEFAULT 'not_started',
  coupon_code VARCHAR(64) NULL,
  checkout_state_key VARCHAR(128) NOT NULL,
  partner_hint VARCHAR(128) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coupons (
  id INT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(64) NOT NULL UNIQUE,
  discount_percent INT NOT NULL,
  channel_required VARCHAR(64) NOT NULL,
  requires_seller_approval TINYINT(1) NOT NULL DEFAULT 0,
  active TINYINT(1) NOT NULL DEFAULT 1,
  note VARCHAR(255) NOT NULL
);

CREATE TABLE orders (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NULL,
  quote_id INT NOT NULL,
  reservation_id INT NOT NULL,
  car_id INT NOT NULL,
  payment_method VARCHAR(64) NOT NULL,
  status VARCHAR(64) NOT NULL,
  total_cents INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE support_tickets (
  id INT AUTO_INCREMENT PRIMARY KEY,
  subject VARCHAR(180) NOT NULL,
  body TEXT NOT NULL,
  visibility VARCHAR(64) NOT NULL DEFAULT 'public',
  status VARCHAR(64) NOT NULL DEFAULT 'open',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seller_notes (
  id INT AUTO_INCREMENT PRIMARY KEY,
  seller_ref VARCHAR(64) NOT NULL,
  internal_reservation VARCHAR(64) NOT NULL,
  note TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  event VARCHAR(128) NOT NULL,
  detail TEXT NOT NULL,
  ip_address VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public_documents (
  id INT AUTO_INCREMENT PRIMARY KEY,
  doc_code VARCHAR(64) NOT NULL UNIQUE,
  title VARCHAR(180) NOT NULL,
  body TEXT NOT NULL,
  file_name VARCHAR(180) NULL
);

CREATE TABLE reviews (
  id INT AUTO_INCREMENT PRIMARY KEY,
  car_id INT NOT NULL,
  display_name VARCHAR(128) NOT NULL,
  title VARCHAR(180) NOT NULL,
  body TEXT NOT NULL,
  rating INT NOT NULL DEFAULT 5,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inspection_jobs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  car_id INT NOT NULL,
  vin VARCHAR(64) NOT NULL,
  inspection_url VARCHAR(255) NOT NULL,
  status VARCHAR(64) NOT NULL,
  result TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flags (
  name VARCHAR(64) PRIMARY KEY,
  value VARCHAR(255) NOT NULL
);

INSERT INTO users (username, password, display_name, role) VALUES
('guest', 'guest123', 'Guest Buyer', 'buyer'),
('mara', 'mara-violet', 'Mara Ellison', 'buyer'),
('seller', 'seller-review-only', 'Seller Review Desk', 'seller'),
('admin', 'not-the-path', 'VioletCart Admin', 'admin');

INSERT INTO cars (name, slug, price_cents, year, mileage, color, vin, seller_ref, inspection_status, import_status, partner_only, seller_priority, description) VALUES
('Violet GT-R', 'violet-gt-r', 18850000, 2025, 2800, 'Violet Pearl', 'VCGTR2025P7', 'SELL-GTR-91', 'passed', 'domestic clear', 0, 70, 'A limited-run grand tourer with carbon ceramic brakes and violet stitched interior.'),
('Nebula S Coupe', 'nebula-s-coupe', 14200000, 2024, 7400, 'Midnight Silver', 'VCNEB2024S9', 'SELL-NEB-44', 'passed', 'import review complete', 0, 55, 'A silent coupe with adaptive air ride and extended battery warranty.'),
('Astra V12', 'astra-v12', 31500000, 2023, 5100, 'Obsidian Purple', 'VCAST2023V12', 'SELL-AST-12', 'manual review', 'import bond pending', 1, 95, 'A rare twelve-cylinder collector car currently queued for seller-assisted delivery.'),
('Phantom LX', 'phantom-lx', 22100000, 2025, 900, 'Graphite Violet', 'VCPHX2025LX', 'SELL-PHX-77', 'passed', 'domestic clear', 0, 80, 'Executive luxury with rear lounge package and extended inspection coverage.'),
('Orion Black Edition', 'orion-black-edition', 26400000, 2024, 3200, 'Black Amethyst', 'VCORI2024BE', 'SELL-ORI-32', 'partner hold', 'seller review required', 1, 99, 'A high-demand import with partner-only settlement during the migration window.');

INSERT INTO coupons (code, discount_percent, channel_required, requires_seller_approval, active, note) VALUES
('WELCOME10', 10, 'public_checkout', 0, 1, 'Public welcome offer. Valid but not enough for seller settlement.'),
('PURPLE-STAFF', 40, 'partner_checkout', 1, 1, 'Staff-assisted settlement coupon. Requires seller channel approval.'),
('IMPORT-HOLD', 15, 'partner_checkout', 1, 0, 'Disabled import review coupon.');

INSERT INTO support_tickets (subject, body, visibility, status) VALUES
('Checkout migration behavior', 'Public checkout and partner_checkout share visual language but not the same state cache. Support should compare response headers before closing buyer reports.', 'public', 'open'),
('Staff coupon confusion', 'PURPLE-STAFF is still rejected in the public flow. Seller channel sync must finish first or the coupon service returns a public-vs-partner mismatch.', 'public', 'triage'),
('Reservation reference formats', 'Buyers see short reservation ids. Seller review desks use R-####-V internal reservations after quote sync. Do not paste those into public emails.', 'public', 'open'),
('Legacy quote sync complaint', 'quote-sync reports missing channel, reservation context missing, or unsupported public flow depending on which part of the migration state exists.', 'public', 'open'),
('Admin portal QA', 'Admin panel is disabled during the migration. A QA placeholder flag in old admin text is not a challenge flag.', 'public', 'closed');

INSERT INTO seller_notes (seller_ref, internal_reservation, note) VALUES
('SEL-VIOLET-104', 'R-1042-V', 'Old sample. Not connected to a live quote.'),
('SEL-VIOLET-109', 'R-1047-V', 'Partner review requires X-Violet-Channel partner_checkout. Public channel should remain forbidden.');

INSERT INTO public_documents (doc_code, title, body, file_name) VALUES
('VC-2026-0007', 'Financing memo: public quote token behavior', 'Public quote tokens start with vc_quote_. Seller assisted reservations use R-####-V references after sync. Old logs mention why public_docs and logs were mirrored during import.', 'VC-2026-0007.txt'),
('VC-2026-0011', 'Import status memo: seller review queues', 'Seller review state may lag behind public reservation state. QA placeholders are not challenge flags.', 'VC-2026-0011.txt'),
('VC-2026-0020', 'Coupon policy memo', 'WELCOME10 remains public. Staff settlement coupons belong to partner review and should not be accepted before seller approval.', NULL);

INSERT INTO reviews (car_id, display_name, title, body, rating) VALUES
(1, 'Aria', 'Quietly brutal', 'The Violet GT-R feels like a private jet with launch control.', 5),
(2, 'Noah', 'Effortless daily coupe', 'Nebula S is understated, fast, and incredibly calm.', 5),
(4, 'Priya', 'Best rear cabin', 'The Phantom LX makes traffic feel optional.', 4);

INSERT INTO inspection_jobs (car_id, vin, inspection_url, status, result) VALUES
(3, 'VCAST2023V12', 'violet://inspection/VCAST2023V12', 'manual-review', 'Astra import requires seller review before settlement.'),
(5, 'VCORI2024BE', 'violet://inspection/VCORI2024BE', 'partner-hold', 'Partner-only hold active. Channel policy must match quote state.');

INSERT INTO audit_logs (event, detail, ip_address) VALUES
('boot', 'VioletCart seed loaded for local CTF lab.', '127.0.0.1'),
('qa-placeholder', 'FLAG{qa_placeholder_ignore_me} is a fake QA marker and not a challenge flag.', '127.0.0.1'),
('migration-note', 'Legacy quote sync expects channel, quote, reservation, and public token context.', '127.0.0.1');

INSERT INTO flags (name, value) VALUES
('legacy_sync', 'runtime-generated'),
('seller_flow', 'runtime-generated'),
('final_order', 'runtime-generated');
