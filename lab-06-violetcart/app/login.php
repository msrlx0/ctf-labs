<?php
require_once __DIR__ . '/includes/layout.php';

if (isset($_GET['logout'])) {
    logout_user();
    header('Location: /login.php');
    exit;
}

$error = null;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim((string)($_POST['username'] ?? ''));
    $password = (string)($_POST['password'] ?? '');
    $stmt = db()->prepare('SELECT * FROM users WHERE username = ?');
    $stmt->execute([$username]);
    $user = $stmt->fetch();
    if ($user && hash_equals((string)$user['password'], $password)) {
        login_user($user);
        header('Location: /account.php');
        exit;
    }
    $error = 'Invalid VioletCart credentials.';
}

page_header('Login');
?>
<section class="form-card">
  <p class="eyebrow">Account</p>
  <h1>Sign in</h1>
  <?php flash($error, 'error'); ?>
  <form method="post">
    <label for="username">Username</label>
    <input id="username" name="username" autocomplete="username" required>
    <label for="password">Password</label>
    <input id="password" name="password" type="password" autocomplete="current-password" required>
    <button type="submit">Login</button>
  </form>
  <p class="muted">Public training account: guest / guest123</p>
</section>
<?php page_footer(); ?>
