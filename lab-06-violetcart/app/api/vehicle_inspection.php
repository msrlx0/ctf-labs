<?php
require_once __DIR__ . '/../includes/helpers.php';

$inspectionUrl = (string)input_value('inspection_url', '');
if ($inspectionUrl === '') {
    json_response(['error' => 'inspection_url_required', 'message' => 'Inspection URL is required.'], 400);
}

if (preg_match('/(localhost|127\.0\.0\.1|0\.0\.0\.0|::1|file:|gopher:|dict:|169\.254\.169\.254|169\.254|metadata)/i', $inspectionUrl)) {
    json_response(['error' => 'inspection_target_blocked', 'message' => 'Inspection target is not allowed.'], 403, ['X-Violet-Trace' => 'inspection-blocked-target']);
}

$vin = null;
if (preg_match('/^violet:\/\/inspection\/([A-Z0-9-]+)$/', $inspectionUrl, $m)) {
    $vin = $m[1];
} elseif (preg_match('/^https:\/\/inspection\.violet\.local\/status\?vin=([A-Z0-9-]+)$/', $inspectionUrl, $m)) {
    $vin = $m[1];
}

if (!$vin) {
    json_response(['error' => 'inspection_alias_only', 'message' => 'Only Violet inspection aliases are supported in the lab.'], 400, ['X-Violet-Trace' => 'inspection-alias-required']);
}

$stmt = db()->prepare('SELECT * FROM inspection_jobs WHERE vin = ? ORDER BY id DESC LIMIT 1');
$stmt->execute([$vin]);
$job = $stmt->fetch();
if (!$job) {
    json_response(['error' => 'inspection_not_found', 'message' => 'Inspection job not found for alias.'], 404);
}

json_response([
    'vin' => $vin,
    'status' => $job['status'],
    'result' => $job['result'],
    'note' => 'Inspection status is advisory; checkout still depends on reservation and seller context.'
], 200, ['X-Violet-Trace' => 'inspection-alias-resolved']);
