<?php
require_once __DIR__ . '/includes/layout.php';
$carId = (int)($_GET['car_id'] ?? 1);
$cars = db()->query('SELECT id, name, price_cents FROM cars ORDER BY seller_priority DESC')->fetchAll();
page_header('Finance');
?>
<section class="form-card">
  <p class="eyebrow">Finance simulation</p>
  <h1>Create a public quote</h1>
  <p class="muted">The browser creates a public quote first. Reservation and seller review state are separate migration steps.</p>
  <form id="quote-form">
    <label for="car_id">Vehicle</label>
    <select id="car_id" name="car_id">
      <?php foreach ($cars as $car): ?>
        <option value="<?= (int)$car['id'] ?>" <?= (int)$car['id'] === $carId ? 'selected' : '' ?>>
          <?= e($car['name']) ?> - <?= money((int)$car['price_cents']) ?>
        </option>
      <?php endforeach; ?>
    </select>
    <label for="term_months">Term</label>
    <select id="term_months" name="term_months">
      <option value="36">36 months</option>
      <option value="48">48 months</option>
      <option value="60" selected>60 months</option>
    </select>
    <label for="down_payment">Down payment</label>
    <input id="down_payment" name="down_payment" type="number" value="25000" min="0">
    <button type="submit">Create quote</button>
  </form>
  <pre class="code" data-violet-status>Quote output will appear here.</pre>
</section>
<script src="/assets/js/checkout-violet.js"></script>
<script>
document.getElementById("quote-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const carId = Number(document.getElementById("car_id").value);
  const term = Number(document.getElementById("term_months").value);
  const down = Number(document.getElementById("down_payment").value);
  const quote = await violetCreateQuote(carId, term, down);
  violetCheckoutNotice(JSON.stringify(quote, null, 2));
});
</script>
<?php page_footer(); ?>
