<?php
require_once __DIR__ . '/../includes/auth.php';

$quoteId = (int)input_value('quote_id', 0);
$reservationId = (int)input_value('reservation_id', 0);
$dupes = raw_duplicate_values('coupon');
$frontendCoupon = $dupes[0] ?? (string)input_value('coupon', '');
$backendCoupon = $dupes ? end($dupes) : (string)input_value('coupon', '');
$channel = channel();

$quote = find_quote($quoteId);
$reservation = find_reservation($reservationId);
if (!$quote || !$reservation || (int)$reservation['quote_id'] !== $quoteId) {
    json_response(['error' => 'checkout_context_missing', 'message' => 'Quote and reservation must be paired before coupon application.'], 400, ['X-Violet-Trace' => 'coupon-context-missing']);
}

$stmt = db()->prepare('SELECT * FROM coupons WHERE code = ? AND active = 1');
$stmt->execute([$backendCoupon]);
$coupon = $stmt->fetch();
if (!$coupon) {
    json_response(['error' => 'coupon_invalid', 'message' => 'Coupon is not valid for this checkout.'], 404, ['X-Violet-Trace' => 'coupon-not-found']);
}

if ($frontendCoupon !== $backendCoupon) {
    header('X-Violet-Coupon-Parser: frontend-first/backend-last');
}

if ($backendCoupon === 'PURPLE-STAFF') {
    $partnerState = $channel === 'partner_checkout' || $reservation['channel'] === 'partner_checkout' || $quote['channel'] === 'partner_checkout';
    if (!$partnerState) {
        json_response(['error' => 'coupon_channel_mismatch', 'message' => 'Staff coupons are not accepted in the public checkout context.'], 409, ['X-Violet-Trace' => 'staff-coupon-public-flow']);
    }
    if ($reservation['seller_status'] !== 'approved') {
        json_response(['error' => 'seller_approval_required', 'message' => 'Partner coupon requires seller review state before settlement.'], 409, ['X-Violet-Trace' => 'staff-coupon-review-missing']);
    }
}

if ($coupon['channel_required'] !== 'public_checkout' && $channel !== $coupon['channel_required'] && $reservation['channel'] !== $coupon['channel_required']) {
    json_response(['error' => 'coupon_channel_mismatch', 'message' => 'Coupon channel does not match checkout context.'], 409, ['X-Violet-Trace' => 'coupon-channel-mismatch']);
}

$stmt = db()->prepare('UPDATE reservations SET coupon_code = ? WHERE id = ?');
$stmt->execute([$backendCoupon, $reservationId]);

json_response([
    'applied' => true,
    'coupon' => $backendCoupon,
    'frontend_seen' => $frontendCoupon,
    'backend_applied' => $backendCoupon,
    'discount_percent' => (int)$coupon['discount_percent'],
    'channel' => $channel
], 200, ['X-Violet-Trace' => $backendCoupon === 'PURPLE-STAFF' ? 'staff-coupon-partner-applied' : 'coupon-public-applied']);
