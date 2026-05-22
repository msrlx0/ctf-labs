<?php
require_once __DIR__ . '/../includes/auth.php';

$data = request_json();
$quoteId = (int)($data['quote_id'] ?? $_POST['quote_id'] ?? 0);
$publicToken = (string)($data['public_token'] ?? $_POST['public_token'] ?? '');
$requestedChannel = (string)($data['channel'] ?? $_POST['channel'] ?? 'public_checkout');
$requestedStatus = (string)($data['requested_status'] ?? $_POST['requested_status'] ?? 'reserved');
$partnerHint = (string)($data['partner_hint'] ?? $_POST['partner_hint'] ?? '');
$user = current_user();

$quote = find_quote($quoteId);
if (!$quote || !hash_equals((string)$quote['public_token'], $publicToken)) {
    current_trace('reservation-quote-mismatch');
    json_response(['error' => 'quote_context_invalid', 'message' => 'A valid quote and public token are required.'], 403);
}

$channel = in_array($requestedChannel, ['public_checkout', 'partner_checkout'], true) ? $requestedChannel : 'public_checkout';
$status = 'reserved';
if ($requestedStatus === 'seller_review' || $partnerHint !== '') {
    $status = 'seller_review_requested';
}

$stateKey = $quoteId . ':' . $channel;
$stmt = db()->prepare('INSERT INTO reservations (user_id, quote_id, car_id, public_token, reservation_ref, status, channel, seller_status, checkout_state_key, partner_hint) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
$stmt->execute([
    $user['id'] ?? null,
    $quoteId,
    (int)$quote['car_id'],
    $publicToken,
    'VC-RES-' . (9000 + $quoteId),
    $status,
    $channel,
    $status === 'seller_review_requested' ? 'requested' : 'not_started',
    $stateKey,
    $partnerHint ?: null,
]);

$reservationId = (int)db()->lastInsertId();
audit_log('reservation_created', 'reservation=' . $reservationId . ' quote=' . $quoteId . ' channel=' . $channel);

json_response([
    'reservation_id' => $reservationId,
    'reservation_ref' => 'VC-RES-' . (9000 + $quoteId),
    'quote_id' => $quoteId,
    'car_id' => (int)$quote['car_id'],
    'status' => $status,
    'channel' => $channel,
    'note' => 'Public reservation state is recorded separately from seller review state.'
], 200, [
    'X-Violet-Flow' => $channel,
    'X-Violet-Trace' => $status === 'seller_review_requested' ? 'reservation-massaged-review' : 'reservation-created'
]);
