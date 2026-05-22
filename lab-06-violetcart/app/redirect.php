<?php
$next = (string)($_GET['next'] ?? '/');
header('X-Violet-Redirect-Policy: relative-or-violetcart');

$decoded = rawurldecode($next);
$blocked = preg_match('/^(https?:)?\/\//i', $decoded) || preg_match('/(evil\.com|\\\|%5c)/i', $next . ' ' . $decoded);

if (!$blocked && str_starts_with($decoded, '/')) {
    header('Location: ' . $next, true, 302);
    exit;
}

header('Location: /', true, 302);
