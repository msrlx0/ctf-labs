<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Cart');
?>
<section class="panel">
  <p class="eyebrow">Cart</p>
  <h1>Reservation cart</h1>
  <p class="muted">The cart UI only knows public quote state. Use checkout after a quote and reservation exist.</p>
  <a class="button" href="/checkout.php">Continue to checkout</a>
</section>
<?php page_footer(); ?>
