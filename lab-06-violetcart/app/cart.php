<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Cart');
?>
<section class="panel">
  <p class="eyebrow">Cart</p>
  <h1>Reservation cart</h1>
  <p class="muted">The cart shows buyer-facing reservation context. It does not initialize seller settlement or import review on its own.</p>
  <a class="button" href="/checkout.php">Continue to checkout</a>
</section>
<?php page_footer(); ?>
