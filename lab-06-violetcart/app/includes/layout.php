<?php
declare(strict_types=1);

require_once __DIR__ . '/auth.php';

function page_header(string $title): void
{
    $user = current_user();
    header('X-Violet-App: VioletCart');
    header('X-Violet-Flow: public_checkout');
    header('X-Violet-Legacy: disabled');
    ?>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><?= e($title) ?> - VioletCart</title>
  <link rel="stylesheet" href="/assets/css/style.css">
</head>
<body>
  <header class="topbar">
    <a class="brand" href="/">
      <span class="brand-mark">VC</span>
      <span><strong>VioletCart</strong><small>Premium Vehicle Marketplace</small></span>
    </a>
    <nav>
      <a href="/cars.php">Catalog</a>
      <a href="/finance.php">Finance</a>
      <a href="/support.php">Support</a>
      <a href="/reviews.php">Reviews</a>
      <a href="/documents.php">Documents</a>
      <a href="/search.php">Search</a>
      <?php if ($user): ?>
        <a href="/account.php">Account</a>
        <a href="/orders.php">Orders</a>
      <?php endif; ?>
    </nav>
    <div class="account-pill">
      <?php if ($user): ?>
        <span><?= e($user['username']) ?></span>
        <a href="/login.php?logout=1">Logout</a>
      <?php else: ?>
        <a href="/login.php">Login</a>
      <?php endif; ?>
    </div>
  </header>
  <main class="content">
    <?php
}

function page_footer(): void
{
    ?>
  </main>
  <footer class="footer">
    <strong>VioletCart</strong> keeps public reservations and seller-assisted reviews on separate migration tracks.
    Public support notes may describe operational names without granting review state.
  </footer>
  <script src="/assets/js/search.js"></script>
</body>
</html>
    <?php
}

function flash(?string $message, string $type = 'info'): void
{
    if (!$message) {
        return;
    }
    echo '<div class="flash ' . e($type) . '">' . e($message) . '</div>';
}
