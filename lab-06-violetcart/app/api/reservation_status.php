<?php
require_once __DIR__ . '/../includes/auth.php';

$reservationId = (int)($_GET['reservation_id'] ?? $_GET['id'] ?? 0);
$quoteId = (int)($_GET['quote_id'] ?? 0);
$publicToken = (string)($_GET['public_token'] ?? '');

$reservation = find_reservation($reservationId);
$quote = find_quote($quoteId);

if (!$reservation || !$quote) {
    current_trace('reservation-status-not-found');
    json_response(['error' => 'not_found', 'message' => 'Reservation context was not found.'], 404);
}

$sameQuote = (int)$reservation['quote_id'] === (int)$quote['id'];
$sameCar = (int)$reservation['car_id'] === (int)$quote['car_id'];
$tokenMatches = hash_equals((string)$quote['public_token'], $publicToken);

if (!$tokenMatches || (!$sameQuote && !$sameCar)) {
    current_trace('reservation-status-context-denied');
    json_response(['error' => 'context_denied', 'message' => 'Quote context does not authorize this reservation view.'], 403);
}

$payload = [
    'reservation_id' => (int)$reservation['id'],
    'quote_id' => (int)$quote['id'],
    'car_id' => (int)$reservation['car_id'],
    'status' => $reservation['status'],
    'seller_status' => $reservation['seller_status'],
    'channel' => $reservation['channel'],
    'checkout_state_key' => $reservation['checkout_state_key'],
];

if ($reservation['internal_reservation']) {
    $payload['internal_reservation_hint'] = $reservation['internal_reservation'];
    $payload['seller_ref_hint'] = $reservation['seller_ref'];
}

json_response($payload, 200, [
    'X-Violet-Trace' => $sameQuote ? 'reservation-status-direct' : 'reservation-status-cross-object',
    'X-Violet-Context' => 'quote-id-trusted'
]);
