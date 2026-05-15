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

if ($file === '' || preg_match('/(\.\.\/|\.\.\\\\|^[a-z]+:\/\/|^\/|%00)/i', $file)) {
    http_response_code(400);
    echo 'Invalid public document path.';
    exit;
}

if (!str_starts_with($file, 'public_docs/')) {
    http_response_code(403);
    echo 'Only public document mirrors are downloadable.';
    exit;
}

$decoded = rawurldecode($file);
$storageRoot = realpath(__DIR__ . '/storage');
$publicRoot = realpath(__DIR__ . '/storage/public_docs');
$logsRoot = realpath(__DIR__ . '/storage/logs');
$target = realpath(__DIR__ . '/storage/' . $decoded);

if (!$target || !$storageRoot || !$publicRoot || !$logsRoot) {
    http_response_code(404);
    echo 'Document not found.';
    exit;
}

$insidePublic = str_starts_with($target, $publicRoot);
$insideLogsViaMirror = str_starts_with($target, $logsRoot) && str_starts_with($file, 'public_docs/');

if (!$insidePublic && !$insideLogsViaMirror) {
    http_response_code(403);
    echo 'Document mirror denied.';
    exit;
}

header('Content-Type: text/plain; charset=utf-8');
readfile($target);
