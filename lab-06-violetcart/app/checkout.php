<?php
require_once __DIR__ . '/includes/layout.php';
page_header('Checkout');
?>
<section class="two-col">
<div class="form-card">
  <p class="eyebrow">Checkout</p>
  <h1>Public checkout</h1>
  <p class="muted">Use this workspace for buyer-visible reservation steps and public promotions. Partner settlement state is evaluated outside the public checkout lane.</p>
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
</div>
<aside class="panel">
  <p class="eyebrow">Reservation desk</p>
  <h2>Public lane checkpoints</h2>
  <ul class="step-list">
    <li><b>Q</b><span>Quote context must match the public token issued by finance.</span></li>
    <li><b>R</b><span>Reservation context is created before coupon services can compare checkout state.</span></li>
    <li><b>C</b><span>WELCOME10 is a public offer. Staff settlement promotions require a different review state.</span></li>
  </ul>
</aside>
</section>
<script src="/assets/js/checkout-violet.js"></script>
<script>
violetHydrateCheckoutForm();
document.getElementById("reserve-button").addEventListener("click", async () => {
  const quoteId = Number(document.getElementById("quote_id").value);
  const token = document.getElementById("public_token").value;
  const reservation = await violetCreateReservation(quoteId, token);
  if (reservation.reservation_id) {
    document.getElementById("reservation_id").value = reservation.reservation_id;
  }
  violetHydrateCheckoutForm();
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
