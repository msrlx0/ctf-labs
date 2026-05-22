<?php
require_once __DIR__ . '/../includes/layout.php';
page_header('Seller');
?>
<section class="panel">
  <p class="eyebrow">Seller desk</p>
  <h1>Seller workflow</h1>
  <p class="muted">Seller reservations are operations records, not buyer-facing reservation IDs. The desk opens only when review context and an internal reference line up.</p>
  <form action="/seller/reservation.php" method="get">
    <label for="ref">Internal reservation</label>
    <input id="ref" name="ref" placeholder="R-####-V">
    <button type="submit">Open reservation</button>
  </form>
</section>
<?php page_footer(); ?>
