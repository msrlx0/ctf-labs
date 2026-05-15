<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Home');
$cars = db()->query('SELECT * FROM cars WHERE partner_only = 0 ORDER BY seller_priority DESC LIMIT 3')->fetchAll();
?>
<section class="hero">
  <div class="hero-copy">
    <p class="eyebrow">Premium marketplace</p>
    <h1>Curated luxury vehicles with VioletCart concierge checkout.</h1>
    <p>VioletCart is migrating reservation and settlement workflows. Public checkout remains available while seller-assisted delivery is reviewed for select vehicles.</p>
    <div class="hero-actions">
      <a class="button" href="/cars.php">Browse catalog</a>
      <a class="button secondary" href="/finance.php">Simulate financing</a>
    </div>
  </div>
  <div class="panel">
    <h2>Migration status</h2>
    <p class="muted">Public checkout is stable. Partner checkout is enabled only after seller review context is available.</p>
    <div class="grid">
      <div class="metric"><span>Public flow</span><strong>Active</strong><small class="muted">Quote and reservation</small></div>
      <div class="metric"><span>Partner flow</span><strong>Review</strong><small class="muted">Seller-assisted</small></div>
      <div class="metric"><span>Imports</span><strong>Mixed</strong><small class="muted">Per vehicle</small></div>
    </div>
  </div>
</section>

<section class="car-grid">
  <?php foreach ($cars as $car): ?>
    <article class="car-card">
      <div class="car-art"></div>
      <div class="car-body">
        <h2><?= e($car['name']) ?></h2>
        <p class="muted"><?= e($car['description']) ?></p>
        <div class="car-meta">
          <span class="pill"><?= e((string)$car['year']) ?></span>
          <span class="pill"><?= e($car['color']) ?></span>
          <span class="pill"><?= money((int)$car['price_cents']) ?></span>
        </div>
        <div class="button-row">
          <a class="button secondary" href="/car.php?id=<?= (int)$car['id'] ?>">View details</a>
        </div>
      </div>
    </article>
  <?php endforeach; ?>
</section>
<?php page_footer(); ?>
