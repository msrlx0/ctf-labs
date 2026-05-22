<?php
require_once __DIR__ . '/includes/layout.php';
$user = require_login();
$stmt = db()->prepare('SELECT * FROM quotes WHERE user_id = ? ORDER BY id DESC LIMIT 5');
$stmt->execute([(int)$user['id']]);
$quotes = $stmt->fetchAll();
page_header('Account');
?>
<section class="panel">
  <p class="eyebrow">Account</p>
  <h1><?= e($user['display_name']) ?></h1>
  <p class="muted">Role: <?= e($user['role']) ?>. Public quote history is shown here; seller review state may lag or live in a separate operations queue.</p>
</section>
<section class="panel">
  <h2>Recent quotes</h2>
  <div class="table-wrap">
    <table>
      <thead><tr><th>ID</th><th>Token</th><th>Status</th><th>Channel</th></tr></thead>
      <tbody>
      <?php foreach ($quotes as $quote): ?>
        <tr><td><?= (int)$quote['id'] ?></td><td><?= e($quote['public_token']) ?></td><td><?= e($quote['status']) ?></td><td><?= e($quote['channel']) ?></td></tr>
      <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</section>
<?php page_footer(); ?>
