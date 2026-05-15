<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Checkout');
?>
<section class="form-card">
  <p class="eyebrow">Checkout</p>
  <h1>Public checkout</h1>
  <p class="muted">Normal checkout expects quote, reservation, coupon, then payment. Seller-assisted settlement is handled by a different context.</p>
  <div class="grid">
    <div>
      <label for="quote_id">Quote ID</label>
      <input id="quote_id" name="quote_id" placeholder="from finance response">
    </div>
    <div>
      <label for="public_token">Public token</label>
      <input id="public_token" name="public_token" placeholder="vc_quote_...">
    </div>
    <div>
      <label for="reservation_id">Reservation ID</label>
      <input id="reservation_id" name="reservation_id" placeholder="create reservation first">
    </div>
  </div>
  <div class="button-row">
    <button id="reserve-button">Create reservation</button>
    <button id="coupon-button" type="button">Apply WELCOME10</button>
  </div>
  <pre class="code" data-violet-status>Checkout response output.</pre>
</section>
<script src="/assets/js/checkout-violet.js"></script>
<script>
document.getElementById("reserve-button").addEventListener("click", async () => {
  const quoteId = Number(document.getElementById("quote_id").value);
  const token = document.getElementById("public_token").value;
  const reservation = await violetCreateReservation(quoteId, token);
  if (reservation.reservation_id) {
    document.getElementById("reservation_id").value = reservation.reservation_id;
  }
  violetCheckoutNotice(JSON.stringify(reservation, null, 2));
});
document.getElementById("coupon-button").addEventListener("click", async () => {
  const quoteId = document.getElementById("quote_id").value;
  const reservationId = document.getElementById("reservation_id").value;
  const response = await violetApplyCoupon(quoteId, reservationId, "WELCOME10");
  violetCheckoutNotice(JSON.stringify(response, null, 2));
});
</script>
<?php page_footer(); ?>
