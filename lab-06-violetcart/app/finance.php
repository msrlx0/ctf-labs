<?php
require_once __DIR__ . '/includes/layout.php';
$carId = (int)($_GET['car_id'] ?? 1);
$cars = db()->query('SELECT id, name, price_cents FROM cars ORDER BY seller_priority DESC')->fetchAll();
page_header('Finance');
?>
<section class="two-col">
<div class="form-card">
  <p class="eyebrow">Finance simulation</p>
  <h1>Create a public quote</h1>
  <p class="muted">Build a buyer-facing payment estimate before reserving inventory. Seller-assisted review is tracked separately from this public quote.</p>
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
</div>
<aside class="panel">
  <p class="eyebrow">Underwriting notes</p>
  <h2>Reservation context matters</h2>
  <ul class="step-list">
    <li><b>1</b><span>Public quotes capture vehicle, term, down payment, and a buyer-visible token.</span></li>
    <li><b>2</b><span>Reservations pair a quote with a held vehicle before checkout services evaluate promotions.</span></li>
    <li><b>3</b><span>Imported or partner-held listings may need seller review state before settlement policy changes.</span></li>
  </ul>
</aside>
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
