<?php
require_once __DIR__ . '/../includes/auth.php';

$data = request_json();
$carId = (int)($data['car_id'] ?? $_POST['car_id'] ?? 0);
$term = max(12, min(84, (int)($data['term_months'] ?? $_POST['term_months'] ?? 60)));
$down = max(0, (int)($data['down_payment'] ?? $_POST['down_payment'] ?? 0)) * 100;
$user = current_user();

$stmt = db()->prepare('SELECT * FROM cars WHERE id = ?');
$stmt->execute([$carId]);
$car = $stmt->fetch();
if (!$car) {
    current_trace('quote-car-missing');
    json_response(['error' => 'car_not_found', 'message' => 'Vehicle is not available for quote.'], 404);
}

$principal = max(0, (int)$car['price_cents'] - $down);
$monthly = (int)ceil(($principal * 1.08) / $term);
$cacheKey = 'public_checkout:' . $carId . ':' . substr(hash('sha256', $car['vin'] . microtime(true)), 0, 10);

$stmt = db()->prepare('INSERT INTO quotes (user_id, car_id, public_token, term_months, down_payment_cents, monthly_cents, status, channel, cache_key, partner_hint) VALUES (?, ?, ?, ?, ?, ?, "quoted", "public_checkout", ?, ?)');
$stmt->execute([
    $user['id'] ?? null,
    $carId,
    'pending',
    $term,
    $down,
    $monthly,
    $cacheKey,
    $car['partner_only'] ? 'seller review likely required' : null,
]);

$quoteId = (int)db()->lastInsertId();
$token = public_token_for($quoteId);
$stmt = db()->prepare('UPDATE quotes SET public_token = ? WHERE id = ?');
$stmt->execute([$token, $quoteId]);

audit_log('quote_created', 'quote=' . $quoteId . ' channel=public_checkout car=' . $carId);

json_response([
    'quote_id' => $quoteId,
    'public_token' => $token,
    'car_id' => $carId,
    'vehicle' => $car['name'],
    'monthly_cents' => $monthly,
    'status' => 'quoted',
    'flow' => 'public_checkout',
    'next' => '/api/create_reservation.php'
], 200, [
    'X-Violet-Flow' => 'public_checkout',
    'X-Violet-Legacy' => 'disabled',
    'X-Violet-Trace' => 'quote-created-public'
]);
