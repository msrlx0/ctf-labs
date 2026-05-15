<?php
$next = (string)($_GET['next'] ?? '/');
header('X-Violet-Redirect-Policy: relative-or-violetcart');

if (str_starts_with($next, '/') || str_contains($next, 'violetcart.local')) {
    header('Location: ' . $next, true, 302);
    exit;
}

header('Location: /', true, 302);
