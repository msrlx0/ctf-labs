<?php
declare(strict_types=1);

require_once __DIR__ . '/db.php';

function e(?string $value): string
{
    return htmlspecialchars((string)$value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function money(int $cents): string
{
    return '$' . number_format($cents / 100, 2);
}

function json_response(array $payload, int $status = 200, array $headers = []): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    foreach ($headers as $name => $value) {
        header($name . ': ' . $value);
    }
    echo json_encode($payload, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
    exit;
}

function request_json(): array
{
    $raw = file_get_contents('php://input') ?: '';
    $json = json_decode($raw, true);
    return is_array($json) ? $json : [];
}

function input_value(string $key, mixed $default = null): mixed
{
    $json = request_json();
    if (array_key_exists($key, $json)) {
        return $json[$key];
    }
    if (array_key_exists($key, $_POST)) {
        return $_POST[$key];
    }
    if (array_key_exists($key, $_GET)) {
        return $_GET[$key];
    }
    return $default;
}

function channel(): string
{
    $header = $_SERVER['HTTP_X_VIOLET_CHANNEL'] ?? '';
    $body = (string)input_value('channel', '');
    return $header !== '' ? $header : ($body !== '' ? $body : 'public_checkout');
}

function current_trace(string $value): void
{
    header('X-Violet-Trace: ' . $value);
}

function challenge_flag(string $name): string
{
    $parts = [
        'recon' => ['violet', 'recon', 'requires', 'patience'],
        'legacy_sync' => ['legacy', 'context', 'changes', 'everything'],
        'seller_flow' => ['seller', 'flow', 'was', 'never', 'public'],
        'final_order' => ['impossible', 'is', 'just', 'context', 'reused', 'correctly'],
    ];

    if (!isset($parts[$name])) {
        return '';
    }

    return 'FL' . 'AG{' . implode('_', $parts[$name]) . '}';
}

function flag_value(string $name): string
{
    return challenge_flag($name);
}

function public_token_for(int $quoteId): string
{
    return 'vc_quote_' . (700 + $quoteId);
}

function internal_reservation_for(int $reservationId): string
{
    return 'R-' . (1041 + $reservationId) . '-V';
}

function seller_ref_for(int $reservationId): string
{
    return 'SEL-VIOLET-' . (420 + $reservationId);
}

function raw_duplicate_values(string $name): array
{
    $raw = file_get_contents('php://input') ?: '';
    if ($raw === '' && isset($_SERVER['QUERY_STRING'])) {
        $raw = (string)$_SERVER['QUERY_STRING'];
    }

    $values = [];
    foreach (explode('&', $raw) as $part) {
        if ($part === '') {
            continue;
        }
        [$key, $value] = array_pad(explode('=', $part, 2), 2, '');
        if (urldecode($key) === $name) {
            $values[] = urldecode($value);
        }
    }
    return $values;
}

function find_quote(int $quoteId): ?array
{
    $stmt = db()->prepare('SELECT * FROM quotes WHERE id = ?');
    $stmt->execute([$quoteId]);
    $row = $stmt->fetch();
    return $row ?: null;
}

function find_reservation(int $reservationId): ?array
{
    $stmt = db()->prepare('SELECT * FROM reservations WHERE id = ?');
    $stmt->execute([$reservationId]);
    $row = $stmt->fetch();
    return $row ?: null;
}

function audit_log(string $event, string $detail): void
{
    $stmt = db()->prepare('INSERT INTO audit_logs (event, detail, ip_address) VALUES (?, ?, ?)');
    $stmt->execute([$event, $detail, $_SERVER['REMOTE_ADDR'] ?? 'local']);
}

function app_url(string $path): string
{
    return $path;
}
