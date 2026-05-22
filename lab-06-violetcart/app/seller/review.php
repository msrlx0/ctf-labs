<?php
require_once __DIR__ . '/../includes/auth.php';

$channel = $_SERVER['HTTP_X_VIOLET_CHANNEL'] ?? (string)input_value('channel', 'public_checkout');
$internal = (string)input_value('internal_reservation', '');
$decision = (string)input_value('decision', '');

if ($channel !== 'partner_checkout') {
    json_response(['error' => 'seller_review_context_unavailable', 'message' => 'Seller review updates require a partner checkout context.'], 403, ['X-Violet-Trace' => 'seller-review-public-denied']);
}

$stmt = db()->prepare('SELECT * FROM reservations WHERE internal_reservation = ?');
$stmt->execute([$internal]);
$reservation = $stmt->fetch();
if (!$reservation) {
    json_response(['error' => 'seller_reservation_not_found', 'message' => 'Internal reservation was not found.'], 404);
}

if ($decision !== 'approve') {
    json_response(['reviewed' => false, 'message' => 'Requested seller decision is unavailable in this local review queue.'], 409);
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
    'review_note' => 'Seller approval is recorded; settlement policy is evaluated by checkout state.'
], 200, ['X-Violet-Trace' => 'seller-review-approved']);
