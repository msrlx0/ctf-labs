<?php
require_once __DIR__ . '/includes/layout.php';
$user = require_login();
$stmt = db()->prepare('SELECT o.*, c.name AS car_name FROM orders o JOIN cars c ON c.id = o.car_id WHERE o.user_id = ? OR o.user_id IS NULL ORDER BY o.id DESC');
$stmt->execute([(int)$user['id']]);
$orders = $stmt->fetchAll();
page_header('Orders');
?>
<section class="panel">
  <p class="eyebrow">Orders</p>
  <h1>Order history</h1>
  <div class="table-wrap">
    <table>
      <thead><tr><th>ID</th><th>Vehicle</th><th>Status</th><th>Payment</th><th>Total</th></tr></thead>
      <tbody>
      <?php foreach ($orders as $order): ?>
        <tr><td><?= (int)$order['id'] ?></td><td><?= e($order['car_name']) ?></td><td><?= e($order['status']) ?></td><td><?= e($order['payment_method']) ?></td><td><?= money((int)$order['total_cents']) ?></td></tr>
      <?php endforeach; ?>
      </tbody>
    </table>
  </div>
</section>
<?php page_footer(); ?>
