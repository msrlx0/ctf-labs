<?php
require_once __DIR__ . '/../includes/auth.php';

$quoteId = (int)input_value('quote_id', 0);
$reservationId = (int)input_value('reservation_id', 0);
$paymentMethod = (string)input_value('payment_method', '');
$channel = channel();
$user = current_user();

$quote = find_quote($quoteId);
$reservation = find_reservation($reservationId);
if (!$quote || !$reservation || (int)$reservation['quote_id'] !== $quoteId) {
    json_response(['error' => 'checkout_context_missing', 'message' => 'Order confirmation requires paired quote and reservation.'], 400, ['X-Violet-Trace' => 'order-context-missing']);
}

$partnerState = $channel === 'partner_checkout' || $quote['channel'] === 'partner_checkout' || $reservation['channel'] === 'partner_checkout';
if (!$partnerState) {
    json_response(['error' => 'unsupported_public_flow', 'message' => 'Public checkout cannot use partner settlement.'], 409, ['X-Violet-Trace' => 'order-public-flow']);
}

if ($reservation['seller_status'] !== 'approved') {
    json_response(['error' => 'seller_review_required', 'message' => 'Seller approval is required before partner settlement.'], 409, ['X-Violet-Trace' => 'order-seller-review-missing']);
}

if ($reservation['coupon_code'] !== 'PURPLE-STAFF') {
    json_response(['error' => 'settlement_coupon_missing', 'message' => 'Partner settlement coupon has not been applied.'], 409, ['X-Violet-Trace' => 'order-coupon-missing']);
}

if ($paymentMethod !== 'partner_settlement') {
    json_response(['error' => 'payment_method_rejected', 'message' => 'Payment method does not match seller-assisted settlement.'], 409, ['X-Violet-Trace' => 'order-payment-method-rejected']);
}

$stmt = db()->prepare('SELECT price_cents FROM cars WHERE id = ?');
$stmt->execute([(int)$reservation['car_id']]);
$price = (int)$stmt->fetchColumn();
$total = (int)ceil($price * 0.60);

$stmt = db()->prepare('INSERT INTO orders (user_id, quote_id, reservation_id, car_id, payment_method, status, total_cents) VALUES (?, ?, ?, ?, ?, "confirmed", ?)');
$stmt->execute([$user['id'] ?? null, $quoteId, $reservationId, (int)$reservation['car_id'], $paymentMethod, $total]);

$stmt = db()->prepare('UPDATE reservations SET status = "ordered" WHERE id = ?');
$stmt->execute([$reservationId]);

json_response([
    'confirmed' => true,
    'order_id' => (int)db()->lastInsertId(),
    'status' => 'confirmed',
    'payment_method' => $paymentMethod,
    'total_cents' => $total,
    'flag' => flag_value('final_order')
], 200, ['X-Violet-Trace' => 'partner-settlement-confirmed']);
