<?php
require_once __DIR__ . '/includes/helpers.php';
$file = '';
foreach (explode('&', $_SERVER['QUERY_STRING'] ?? '') as $part) {
    [$key, $value] = array_pad(explode('=', $part, 2), 2, '');
    if (urldecode($key) === 'file') {
        $file = $value;
        break;
    }
}
header('X-Violet-Download-Mirror: public-docs');

$decodedOnce = rawurldecode($file);
$blocked = '/(\.\.\/|\.\.\\\\|%2e%2e|\/etc\/passwd|^[a-z]+:\/\/|php:\/\/|data:\/\/|file:\/\/|expect:\/\/|^\/|%00)/i';

if ($file === '' || preg_match($blocked, $file) || preg_match($blocked, $decodedOnce)) {
    http_response_code(400);
    echo 'Invalid public document path.';
    exit;
}

$decodedOnce = str_replace('\\', '/', $decodedOnce);
if (!str_contains($decodedOnce, '/') && preg_match('/^VC-2026-[0-9]{4}\.txt$/', $decodedOnce)) {
    $decodedOnce = 'public_docs/' . $decodedOnce;
}

if (!str_starts_with($decodedOnce, 'public_docs/')) {
    http_response_code(403);
    echo 'Only public document mirrors are downloadable.';
    exit;
}

$storageRoot = realpath(__DIR__ . '/storage');
$publicRoot = realpath(__DIR__ . '/storage/public_docs');
$target = realpath(__DIR__ . '/storage/' . $decodedOnce);

if (!$target || !$storageRoot || !$publicRoot) {
    http_response_code(404);
    echo 'Document not found.';
    exit;
}

$insidePublic = str_starts_with($target, $publicRoot);

if (!$insidePublic) {
    http_response_code(403);
    echo 'Document mirror denied.';
    exit;
}

header('Content-Type: text/plain; charset=utf-8');
readfile($target);
