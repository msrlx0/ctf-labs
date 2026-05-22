<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Home');
$cars = db()->query('SELECT * FROM cars WHERE partner_only = 0 ORDER BY seller_priority DESC LIMIT 3')->fetchAll();
?>
<section class="hero">
  <div class="hero-copy">
    <p class="eyebrow">Private-market vehicle reservations</p>
    <h1>VioletCart</h1>
    <p>Curated performance, luxury, and collector vehicles with public reservations, finance simulation, and seller-assisted review for select imports.</p>
    <div class="hero-actions">
      <a class="button" href="/cars.php">Browse catalog</a>
      <a class="button secondary" href="/finance.php">Simulate financing</a>
    </div>
  </div>
  <div class="hero-showcase">
    <div class="vehicle-visual large" aria-hidden="true"></div>
    <div class="panel">
      <h2>Migration status</h2>
      <p class="muted">Public checkout is stable. Partner-assisted checkout requires reservation context before seller sync can be evaluated.</p>
      <div class="grid">
        <div class="metric"><span>Public flow</span><strong>Active</strong><small class="muted">Quote and reservation</small></div>
        <div class="metric"><span>Seller review</span><strong>Gated</strong><small class="muted">Context-dependent</small></div>
        <div class="metric"><span>Imports</span><strong>Mixed</strong><small class="muted">Per vehicle</small></div>
      </div>
    </div>
  </div>
</section>

<div class="section-head">
  <div>
    <p class="eyebrow">Featured inventory</p>
    <h2>Concierge-ready listings</h2>
  </div>
  <a class="inline-link" href="/cars.php">View full catalog</a>
</div>

<section class="car-grid">
  <?php foreach ($cars as $car): ?>
    <article class="car-card">
      <div class="car-art"></div>
      <div class="car-body">
        <h2><?= e($car['name']) ?></h2>
        <p class="muted"><?= e($car['description']) ?></p>
        <strong class="price-line"><?= money((int)$car['price_cents']) ?></strong>
        <div class="car-meta">
          <span class="pill"><?= e((string)$car['year']) ?></span>
          <span class="pill"><?= e($car['color']) ?></span>
          <span class="pill"><?= e($car['inspection_status']) ?></span>
        </div>
        <div class="button-row">
          <a class="button secondary" href="/car.php?id=<?= (int)$car['id'] ?>">View details</a>
        </div>
      </div>
    </article>
  <?php endforeach; ?>
</section>
<?php page_footer(); ?>
