<?php
require_once __DIR__ . '/includes/layout.php';
require_once __DIR__ . '/includes/filters.php';
$q = (string)($_GET['q'] ?? '');
$filtered = violet_strong_filter($q);
$stmt = db()->prepare('SELECT * FROM cars WHERE name LIKE ? OR description LIKE ? ORDER BY seller_priority DESC');
$like = '%' . $q . '%';
$stmt->execute([$like, $like]);
$results = $q === '' ? [] : $stmt->fetchAll();
page_header('Search');
?>
<section class="form-card">
  <p class="eyebrow">Search</p>
  <h1>Search vehicles</h1>
  <form method="get">
    <label for="q">Query</label>
    <input id="q" name="q" value="<?= e($q) ?>">
    <button type="submit">Search</button>
  </form>
  <p class="muted" data-search-proof>Search UI ready.</p>
</section>
<script src="/assets/js/search.js"></script>
<script>
const violetSearchTerm = '<?= $filtered ?>';
if (violetSearchTerm.length > 0) {
  violetSearchNotice("Searched for " + violetSearchTerm);
}
</script>
<section class="car-grid">
  <?php foreach ($results as $car): ?>
    <article class="car-card"><div class="car-body"><h2><?= e($car['name']) ?></h2><p class="muted"><?= e($car['description']) ?></p><a class="inline-link" href="/car.php?id=<?= (int)$car['id'] ?>">Open</a></div></article>
  <?php endforeach; ?>
</section>
<?php page_footer(); ?>
