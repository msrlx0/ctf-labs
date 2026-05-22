const VioletCheckout = {
  flow: "public_checkout",
  migrationMirror: "reservation-context-required",
  migrationSyncPath: "/legacy/quote-sync.php",
  staffCouponMode: "seller-review-state-required",
  quoteStorageKey: "violet:lastQuote",
  reservationStorageKey: "violet:lastReservation",
};

async function violetCreateQuote(carId, termMonths, downPayment) {
  const response = await fetch("/api/create_quote.php", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ car_id: carId, term_months: termMonths, down_payment: downPayment })
  });
  const json = await response.json();
  if (json.quote_id) {
    localStorage.setItem(VioletCheckout.quoteStorageKey, JSON.stringify(json));
  }
  return json;
}

async function violetCreateReservation(quoteId, publicToken) {
  const response = await fetch("/api/create_reservation.php", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ quote_id: quoteId, public_token: publicToken })
  });
  const json = await response.json();
  if (json.reservation_id) {
    localStorage.setItem(VioletCheckout.reservationStorageKey, JSON.stringify(json));
  }
  return json;
}

async function violetApplyCoupon(quoteId, reservationId, coupon) {
  const body = new URLSearchParams({ quote_id: quoteId, reservation_id: reservationId, coupon });
  const response = await fetch("/api/apply_coupon.php", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body
  });
  return response.json();
}

function violetSavedJson(key) {
  try {
    return JSON.parse(localStorage.getItem(key) || "null");
  } catch (error) {
    return null;
  }
}

function violetHydrateCheckoutForm() {
  const quote = violetSavedJson(VioletCheckout.quoteStorageKey);
  const reservation = violetSavedJson(VioletCheckout.reservationStorageKey);
  const quoteId = document.getElementById("quote_id");
  const publicToken = document.getElementById("public_token");
  const reservationId = document.getElementById("reservation_id");

  if (quote && quoteId && !quoteId.value) {
    quoteId.value = quote.quote_id || "";
  }
  if (quote && publicToken && !publicToken.value) {
    publicToken.value = quote.public_token || "";
  }
  if (reservation && reservationId && !reservationId.value) {
    reservationId.value = reservation.reservation_id || "";
  }
}

function violetCheckoutNotice(message) {
  const el = document.querySelector("[data-violet-status]");
  if (el) {
    el.textContent = message;
  }
}

window.VioletCheckout = VioletCheckout;
window.violetHydrateCheckoutForm = violetHydrateCheckoutForm;
