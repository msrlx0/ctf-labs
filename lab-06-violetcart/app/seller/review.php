<?php
require_once __DIR__ . '/../includes/auth.php';

$channel = $_SERVER['HTTP_X_VIOLET_CHANNEL'] ?? (string)input_value('channel', 'public_checkout');
$internal = (string)input_value('internal_reservation', '');
$decision = (string)input_value('decision', '');

if ($channel !== 'partner_checkout') {
    json_response(['error' => 'seller_channel_required', 'message' => 'Seller review updates require partner checkout channel.'], 403, ['X-Violet-Trace' => 'seller-review-public-denied']);
}

$stmt = db()->prepare('SELECT * FROM reservations WHERE internal_reservation = ?');
$stmt->execute([$internal]);
$reservation = $stmt->fetch();
if (!$reservation) {
    json_response(['error' => 'seller_reservation_not_found', 'message' => 'Internal reservation was not found.'], 404);
}

if ($decision !== 'approve') {
    json_response(['reviewed' => false, 'message' => 'Only approve is enabled in the local seller review simulation.'], 409);
}

$stmt = db()->prepare('UPDATE reservations SET seller_status = "approved", status = "seller_approved", channel = "partner_checkout" WHERE id = ?');
$stmt->execute([(int)$reservation['id']]);
$stmt = db()->prepare('UPDATE quotes SET channel = "partner_checkout", status = "seller_approved" WHERE id = ?');
$stmt->execute([(int)$reservation['quote_id']]);
audit_log('seller_review_approved', 'internal=' . $internal);

json_response([
    'reviewed' => true,
    'internal_reservation' => $internal,
    'seller_status' => 'approved',
    'next' => 'Return to checkout and apply the partner settlement coupon.'
], 200, ['X-Violet-Trace' => 'seller-review-approved']);
