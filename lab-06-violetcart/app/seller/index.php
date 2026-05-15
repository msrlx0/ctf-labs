<?php
require_once __DIR__ . '/../includes/layout.php';
page_header('Seller');
?>
<section class="panel">
  <p class="eyebrow">Seller desk</p>
  <h1>Seller workflow</h1>
  <p class="muted">Seller reservations are not public reservations. The desk expects partner checkout context and an internal reservation reference.</p>
  <form action="/seller/reservation.php" method="get">
    <label for="ref">Internal reservation</label>
    <input id="ref" name="ref" placeholder="R-####-V">
    <button type="submit">Open reservation</button>
  </form>
</section>
<?php page_footer(); ?>
