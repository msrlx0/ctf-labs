<?php
declare(strict_types=1);

function violet_strong_filter(string $value): string
{
    $blocked = [
        '<script', '</script', 'onerror', 'onload', 'javascript:', 'alert',
        'prompt', 'confirm', 'document', 'cookie', 'svg', 'iframe', 'img', 'src'
    ];

    $filtered = $value;
    foreach ($blocked as $word) {
        $filtered = preg_replace('/' . preg_quote($word, '/') . '/i', '[filtered]', $filtered);
    }

    return str_replace(['<', '>', '`'], ['&lt;', '&gt;', ''], $filtered);
}

function review_attribute_filter(string $value): string
{
    $blocked = ['<script', '</script', 'onerror', 'onload', 'javascript:', 'alert', 'prompt', 'confirm', 'document', 'cookie', 'svg', 'iframe', 'img', 'src'];
    $filtered = strip_tags($value);
    foreach ($blocked as $word) {
        $filtered = preg_replace('/' . preg_quote($word, '/') . '/i', '[filtered]', $filtered);
    }
    return str_replace(['<', '>', '`'], '', $filtered);
}

function looks_like_sql_probe(string $value): bool
{
    return (bool)preg_match('/(union|select|sleep|benchmark|information_schema|--|#|\/\*|\*\/|;)/i', $value);
}
