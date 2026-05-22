<?php
require_once __DIR__ . '/includes/layout.php';
require_once __DIR__ . '/includes/filters.php';

$sort = (string)($_GET['sort'] ?? 'seller_priority');
$direction = 'DESC';
$publicSorts = ['price' => 'price_cents', 'year' => 'year', 'mileage' => 'mileage', 'name' => 'name'];

header('X-Violet-Catalog: public');

if (looks_like_sql_probe($sort)) {
    header('X-Violet-Trace: sort-filter-hit');
    $sort = 'seller_priority';
}

if (isset($publicSorts[$sort])) {
    $orderBy = $publicSorts[$sort];
} elseif (preg_match('/^[a-z_]+$/', $sort)) {
    $orderBy = $sort;
    header('X-Violet-Sort-Warning: non-public-sort-key');
} else {
    $orderBy = 'seller_priority';
}

try {
    $cars = db()->query("SELECT * FROM cars ORDER BY {$orderBy} {$direction}, id ASC")->fetchAll();
    $sortError = null;
} catch (Throwable $error) {
    header('X-Violet-Trace: catalog-sort-error');
    $cars = db()->query('SELECT * FROM cars ORDER BY seller_priority DESC, id ASC')->fetchAll();
    $sortError = 'Sort key is not available in the public catalog.';
}

page_header('Catalog');
?>
<section class="panel">
  <p class="eyebrow">Catalog</p>
  <h1>VioletCart vehicles</h1>
  <p class="muted">Inventory blends public checkout listings with imports that may need seller-assisted review. Public sort keys are stable; operational keys can behave differently during migration.</p>
  <?php flash($sortError, 'error'); ?>
  <div class="pill-row">
    <a class="pill" href="/cars.php?sort=price">Price</a>
    <a class="pill" href="/cars.php?sort=year">Year</a>
    <a class="pill" href="/cars.php?sort=mileage">Mileage</a>
    <a class="pill" href="/cars.php?sort=name">Name</a>
  </div>
</section>

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
          <span class="pill"><?= number_format((int)$car['mileage']) ?> mi</span>
          <span class="pill"><?= e($car['color']) ?></span>
          <span class="pill"><?= e($car['inspection_status']) ?></span>
          <span class="pill"><?= e($car['import_status']) ?></span>
        </div>
        <div class="button-row">
          <a class="button" href="/car.php?id=<?= (int)$car['id'] ?>">Details</a>
          <a class="button secondary" href="/finance.php?car_id=<?= (int)$car['id'] ?>">Finance</a>
        </div>
      </div>
    </article>
  <?php endforeach; ?>
</section>
<?php page_footer(); ?>
