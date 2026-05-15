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
    <div class="pill-row">
      <span class="pill"><?= money((int)$car['price_cents']) ?></span>
      <span class="pill">VIN <?= e($car['vin']) ?></span>
      <span class="pill"><?= e($car['import_status']) ?></span>
    </div>
    <div class="hero-actions">
      <a class="button" href="/finance.php?car_id=<?= (int)$car['id'] ?>">Create quote</a>
      <a class="button secondary" href="/documents.php?doc=VC-2026-0007">Financing memo</a>
    </div>
  </div>
  <div class="panel">
    <h2>Inspection</h2>
    <p class="muted">Inspection jobs are reviewed by internal aliases during the import workflow.</p>
    <div class="code">vin=<?= e($car['vin']) . "\n" ?>seller_ref=<?= e($car['seller_ref']) . "\n" ?>status=<?= e($car['inspection_status']) ?></div>
  </div>
</section>
<?php page_footer(); ?>
