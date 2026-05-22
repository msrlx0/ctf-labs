<?php
require_once __DIR__ . '/../includes/auth.php';

$quoteId = (int)input_value('quote_id', 0);
$reservationId = (int)input_value('reservation_id', 0);
$publicToken = (string)input_value('public_token', '');
$channel = channel();

$quote = find_quote($quoteId);
$reservation = find_reservation($reservationId);
if (!$quote || !$reservation || !hash_equals((string)$quote['public_token'], $publicToken)) {
    json_response(['error' => 'hold_context_invalid', 'message' => 'Quote token and reservation context are required for hold review.'], 403, ['X-Violet-Trace' => 'hold-context-invalid']);
}

if ($channel !== 'partner_checkout') {
    json_response([
        'held' => false,
        'message' => 'Public checkout can reserve a car, but partner hold review needs seller-assisted context.'
    ], 409, ['X-Violet-Trace' => 'hold-public-channel']);
}

$stateKey = $reservationId . ':partner_checkout';
$stmt = db()->prepare('UPDATE quotes SET channel = "partner_checkout", cache_key = ? WHERE id = ?');
$stmt->execute([$stateKey, $quoteId]);
$stmt = db()->prepare('UPDATE reservations SET channel = "partner_checkout", checkout_state_key = ? WHERE id = ?');
$stmt->execute([$stateKey, $reservationId]);

json_response([
    'held' => true,
    'quote_id' => $quoteId,
    'reservation_id' => $reservationId,
    'state_key' => $stateKey,
    'message' => 'Partner hold context warmed for seller-assisted checkout.'
], 200, ['X-Violet-Trace' => 'partner-hold-warmed']);
