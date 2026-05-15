<?php
require_once __DIR__ . '/includes/layout.php';
require_once __DIR__ . '/includes/filters.php';
$message = null;
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $carId = (int)($_POST['car_id'] ?? 1);
    $display = review_attribute_filter((string)($_POST['display_name'] ?? 'Guest'));
    $title = review_attribute_filter((string)($_POST['title'] ?? 'Untitled'));
    $body = violet_strong_filter((string)($_POST['body'] ?? ''));
    $rating = max(1, min(5, (int)($_POST['rating'] ?? 5)));
    $stmt = db()->prepare('INSERT INTO reviews (car_id, display_name, title, body, rating) VALUES (?, ?, ?, ?, ?)');
    $stmt->execute([$carId, $display, $title, $body, $rating]);
    $message = 'Review submitted.';
}
$cars = db()->query('SELECT id, name FROM cars ORDER BY name')->fetchAll();
$reviews = db()->query('SELECT r.*, c.name AS car_name FROM reviews r JOIN cars c ON c.id = r.car_id ORDER BY r.id DESC LIMIT 12')->fetchAll();
page_header('Reviews');
?>
<section class="form-card">
  <p class="eyebrow">Buyer reviews</p>
  <h1>Leave a review</h1>
  <?php flash($message); ?>
  <form method="post">
    <label for="car_id">Vehicle</label>
    <select id="car_id" name="car_id">
      <?php foreach ($cars as $car): ?><option value="<?= (int)$car['id'] ?>"><?= e($car['name']) ?></option><?php endforeach; ?>
    </select>
    <label for="display_name">Display name</label>
    <input id="display_name" name="display_name" required>
    <label for="title">Title</label>
    <input id="title" name="title" required>
    <label for="body">Comment</label>
    <textarea id="body" name="body" required></textarea>
    <label for="rating">Rating</label>
    <select id="rating" name="rating"><option>5</option><option>4</option><option>3</option><option>2</option><option>1</option></select>
    <button type="submit">Submit review</button>
  </form>
  <p class="muted" data-review-proof>Review renderer idle.</p>
</section>
<section class="grid">
  <?php foreach ($reviews as $review): ?>
    <article class="panel" data-review-title="<?= $review['title'] ?>" data-reviewer="<?= $review['display_name'] ?>">
      <h2><?= e($review['title']) ?></h2>
      <p class="muted"><?= e($review['car_name']) ?> by <?= e($review['display_name']) ?> - <?= (int)$review['rating'] ?>/5</p>
      <p><?= $review['body'] ?></p>
    </article>
  <?php endforeach; ?>
</section>
<script src="/assets/js/reviews.js"></script>
<?php page_footer(); ?>
