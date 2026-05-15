<?php
require_once __DIR__ . '/../includes/layout.php';

$ref = (string)($_GET['ref'] ?? '');
$channel = $_SERVER['HTTP_X_VIOLET_CHANNEL'] ?? 'public_checkout';
header('X-Violet-Seller-Desk: reservation');

$stmt = db()->prepare('SELECT r.*, q.public_token, c.name AS car_name FROM reservations r JOIN quotes q ON q.id = r.quote_id JOIN cars c ON c.id = r.car_id WHERE r.internal_reservation = ?');
$stmt->execute([$ref]);
$reservation = $stmt->fetch();

if (!$reservation) {
    http_response_code(404);
    page_header('Seller reservation');
    echo '<section class="panel"><h1>Reservation not found</h1><p class="muted">Seller desk references are created by legacy quote sync.</p></section>';
    page_footer();
    exit;
}

if ($channel !== 'partner_checkout') {
    http_response_code(403);
    header('X-Violet-Trace: seller-public-channel-denied');
    page_header('Seller reservation');
    echo '<section class="panel"><h1>Forbidden</h1><p class="muted">Seller reservation exists, but this request is not in the partner checkout channel.</p></section>';
    page_footer();
    exit;
}

header('X-Violet-Trace: seller-reservation-partner');
page_header('Seller reservation');
?>
<section class="panel">
  <p class="eyebrow">Seller review</p>
  <h1><?= e($reservation['internal_reservation']) ?></h1>
  <p class="muted">Partner checkout context accepted. This view is not reachable from public checkout alone.</p>
  <div class="grid">
    <div class="metric"><span>Vehicle</span><strong><?= e($reservation['car_name']) ?></strong><small><?= e($reservation['seller_ref']) ?></small></div>
    <div class="metric"><span>Status</span><strong><?= e($reservation['status']) ?></strong><small><?= e($reservation['seller_status']) ?></small></div>
    <div class="metric"><span>Channel</span><strong><?= e($reservation['channel']) ?></strong><small><?= e($reservation['checkout_state_key']) ?></small></div>
  </div>
  <pre class="code"><?= e(flag_value('seller_flow')) ?></pre>
  <form method="post" action="/seller/review.php">
    <input type="hidden" name="internal_reservation" value="<?= e($reservation['internal_reservation']) ?>">
    <input type="hidden" name="decision" value="approve">
    <button type="submit">Approve seller review</button>
  </form>
</section>
<?php page_footer(); ?>
