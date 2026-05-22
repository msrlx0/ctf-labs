<?php
require_once __DIR__ . '/includes/layout.php';
$id = (int)($_GET['id'] ?? 0);
$stmt = db()->prepare('SELECT * FROM cars WHERE id = ?');
$stmt->execute([$id]);
$car = $stmt->fetch();
if (!$car) {
    http_response_code(404);
    page_header('Not found');
    echo '<section class="panel"><h1>Vehicle not found</h1><p class="muted">The requested car is not in the current catalog.</p></section>';
    page_footer();
    exit;
}
header('X-Violet-Car-Inspection: ' . $car['inspection_status']);
page_header($car['name']);
?>
<section class="hero">
  <div class="hero-copy">
    <p class="eyebrow">Vehicle detail</p>
    <h1><?= e($car['name']) ?></h1>
    <p><?= e($car['description']) ?></p>
    <strong class="price-line"><?= money((int)$car['price_cents']) ?></strong>
    <div class="pill-row">
      <span class="pill"><?= e((string)$car['year']) ?></span>
      <span class="pill"><?= number_format((int)$car['mileage']) ?> mi</span>
      <span class="pill"><?= e($car['color']) ?></span>
      <span class="pill">VIN <?= e($car['vin']) ?></span>
      <span class="pill"><?= e($car['import_status']) ?></span>
    </div>
    <div class="hero-actions">
      <a class="button" href="/finance.php?car_id=<?= (int)$car['id'] ?>">Create quote</a>
      <a class="button secondary" href="/documents.php?doc=VC-2026-0007">Financing memo</a>
    </div>
  </div>
  <div class="hero-showcase">
    <div class="vehicle-visual large" aria-hidden="true"></div>
    <div class="panel">
      <h2>Inspection summary</h2>
      <p class="muted">Inspection jobs are tracked by vehicle alias during import review. Approval for purchase can still depend on reservation and seller context.</p>
      <div class="code">vin=<?= e($car['vin']) . "\n" ?>seller_ref=<?= e($car['seller_ref']) . "\n" ?>status=<?= e($car['inspection_status']) ?></div>
    </div>
  </div>
</section>
<section class="grid">
  <div class="metric"><span>Inspection</span><strong><?= e($car['inspection_status']) ?></strong><small class="muted">Vehicle readiness</small></div>
  <div class="metric"><span>Import status</span><strong><?= e($car['import_status']) ?></strong><small class="muted">Operations queue</small></div>
  <div class="metric"><span>Seller desk</span><strong><?= (int)$car['partner_only'] === 1 ? 'Required' : 'Optional' ?></strong><small class="muted">Per listing policy</small></div>
</section>
<section class="panel">
  <h2>Reservation notes</h2>
  <p class="muted">Public checkout can create buyer-visible reservations. Seller-assisted review uses a separate operations reference when a listing is moved into partner settlement.</p>
</section>
<?php page_footer(); ?>
