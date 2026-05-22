<?php
require_once __DIR__ . '/includes/layout.php';
$message = null;
$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim((string)($_POST['username'] ?? ''));
    $password = (string)($_POST['password'] ?? '');
    $display = trim((string)($_POST['display_name'] ?? $username));
    if ($username === '' || $password === '') {
        $error = 'Username and password are required.';
    } else {
        try {
            $stmt = db()->prepare('INSERT INTO users (username, password, display_name, role) VALUES (?, ?, ?, "buyer")');
            $stmt->execute([$username, $password, $display]);
            $message = 'Account created. You can now sign in.';
        } catch (Throwable $e) {
            $error = 'Account could not be created.';
        }
    }
}

page_header('Register');
?>
<section class="form-card">
  <p class="eyebrow">Account</p>
  <h1>Create account</h1>
  <?php flash($message); flash($error, 'error'); ?>
  <form method="post">
    <label for="display_name">Display name</label>
    <input id="display_name" name="display_name">
    <label for="username">Username</label>
    <input id="username" name="username" required>
    <label for="password">Password</label>
    <input id="password" name="password" type="password" required>
    <button type="submit">Register</button>
  </form>
</section>
<?php page_footer(); ?>
