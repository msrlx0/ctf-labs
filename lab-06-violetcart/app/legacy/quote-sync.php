<?php
require_once __DIR__ . '/../includes/auth.php';

header('X-Violet-Legacy: quote-sync');
$channel = $_SERVER['HTTP_X_VIOLET_CHANNEL'] ?? (string)input_value('channel', '');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(['error' => 'method_not_allowed', 'message' => 'Quote sync accepts migration POST requests only.'], 405, ['Allow' => 'POST']);
}

if ($channel === '') {
    json_response(['error' => 'missing_channel', 'message' => 'Legacy sync requires a checkout channel.'], 400, ['X-Violet-Trace' => 'quote-sync-missing-channel']);
}

if ($channel !== 'partner_checkout') {
    json_response(['error' => 'unsupported_public_flow', 'message' => 'Public checkout cannot perform seller channel sync.'], 409, ['X-Violet-Trace' => 'quote-sync-public-flow']);
}

$quoteId = (int)input_value('quote_id', 0);
$reservationId = (int)input_value('reservation_id', 0);
$publicToken = (string)input_value('public_token', '');

$quote = find_quote($quoteId);
if (!$quote) {
    json_response(['error' => 'quote_not_ready', 'message' => 'Quote is not ready for legacy sync.'], 409, ['X-Violet-Trace' => 'quote-sync-quote-missing']);
}

$reservation = find_reservation($reservationId);
if (!$reservation) {
    json_response(['error' => 'reservation_context_missing', 'message' => 'Reservation context is required before seller sync.'], 409, ['X-Violet-Trace' => 'quote-sync-reservation-missing']);
}

if (!hash_equals((string)$quote['public_token'], $publicToken) || (int)$reservation['quote_id'] !== $quoteId) {
    json_response(['error' => 'context_mismatch', 'message' => 'Quote token and reservation context do not match.'], 403, ['X-Violet-Trace' => 'quote-sync-context-mismatch']);
}

$internal = $reservation['internal_reservation'] ?: internal_reservation_for($reservationId);
$sellerRef = $reservation['seller_ref'] ?: seller_ref_for($reservationId);
$stateKey = $reservationId . ':partner_checkout';

$stmt = db()->prepare('UPDATE reservations SET channel = "partner_checkout", status = "seller_review", seller_status = "pending", internal_reservation = ?, seller_ref = ?, checkout_state_key = ? WHERE id = ?');
$stmt->execute([$internal, $sellerRef, $stateKey, $reservationId]);
$stmt = db()->prepare('UPDATE quotes SET channel = "partner_checkout", status = "synced", cache_key = ? WHERE id = ?');
$stmt->execute([$stateKey, $quoteId]);
$stmt = db()->prepare('INSERT INTO seller_notes (seller_ref, internal_reservation, note) VALUES (?, ?, ?)');
$stmt->execute([$sellerRef, $internal, 'Legacy sync created seller review state from partner checkout context.']);
audit_log('legacy_quote_sync', 'quote=' . $quoteId . ' reservation=' . $reservationId . ' internal=' . $internal);

json_response([
    'synced' => true,
    'quote_id' => $quoteId,
    'reservation_id' => $reservationId,
    'seller_ref' => $sellerRef,
    'internal_reservation' => $internal,
    'seller_status' => 'pending',
    'flag' => flag_value('legacy_sync'),
    'next' => '/seller/reservation.php?ref=' . $internal
], 200, [
    'X-Violet-Trace' => 'quote-sync-partner-context',
    'X-Violet-Channel' => 'partner_checkout'
]);
