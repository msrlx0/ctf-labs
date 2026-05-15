<?php
require_once __DIR__ . '/../includes/helpers.php';

$data = request_json();
$query = (string)($data['query'] ?? $_GET['query'] ?? '');
$checkpoint = (string)($data['checkpoint'] ?? $_GET['checkpoint'] ?? '');
$trace = (string)($data['trace'] ?? $_GET['trace'] ?? '');

if ($query === '') {
    json_response(['error' => 'query_required', 'message' => 'A query name is required.'], 400);
}

if (preg_match('/(debugFlags|sellerNotes|internalReservation|flags|admin|password)/i', $query)) {
    json_response(['error' => 'query_blocked', 'message' => 'Query is not available from the public console.'], 403, ['X-Violet-Trace' => 'query-blocked-name']);
}

switch ($query) {
    case 'quoteMeta':
        json_response([
            'query' => $query,
            'fields' => ['quote_id', 'public_token', 'channel', 'cache_key'],
            'note' => 'Quote metadata alone is not seller approval.'
        ], 200, ['X-Violet-Trace' => 'query-quote-meta']);
        break;
    case 'channelPolicy':
        json_response([
            'query' => $query,
            'public_checkout' => ['quote', 'reservation', 'WELCOME10'],
            'partner_checkout' => ['quote-sync', 'seller-review', 'staff-coupon', 'partner-settlement'],
            'note' => 'Policy names are not proof of state.'
        ], 200, ['X-Violet-Trace' => 'query-channel-policy']);
        break;
    case 'documentIndex':
        json_response([
            'query' => $query,
            'documents' => ['VC-2026-0007', 'VC-2026-0011', 'VC-2026-0017', 'VC-2026-0020'],
            'note' => 'Tracked recon memos are public documents; flags are confirmed through stateful queries.'
        ], 200);
        break;
    case 'inspectionProfile':
        json_response([
            'query' => $query,
            'aliases' => ['violet://inspection/<VIN>', 'https://inspection.violet.local/status?vin=<VIN>'],
            'note' => 'Inspection aliases can explain seller review state, but they do not approve checkout.'
        ], 200, ['X-Violet-Trace' => 'query-inspection-profile']);
        break;
    case 'reviewQueues':
        json_response([
            'query' => $query,
            'queues' => ['public_checkout', 'seller_review', 'partner_checkout'],
            'note' => 'Seller review is created by sync, not by the admin page.'
        ], 200, ['X-Violet-Trace' => 'query-review-queues']);
        break;
    case 'reconCheckpoint':
        if ($checkpoint !== 'violet-recon-patience' || $trace !== 'quote-migration-2026-02') {
            json_response([
                'query' => $query,
                'accepted' => false,
                'message' => 'Recon checkpoint confirmation requires the migration checkpoint and trace.'
            ], 403, ['X-Violet-Trace' => 'query-recon-checkpoint-mismatch']);
        }

        json_response([
            'query' => $query,
            'accepted' => true,
            'checkpoint' => $checkpoint,
            'trace' => $trace,
            'flag' => challenge_flag('recon'),
            'next_hint' => 'The public flow still needs quote and reservation context before legacy sync is useful.'
        ], 200, ['X-Violet-Trace' => 'query-recon-checkpoint-confirmed']);
        break;
    default:
        json_response(['error' => 'query_not_found', 'message' => 'Query name is not in the public query registry.'], 404, ['X-Violet-Trace' => 'query-not-found']);
}
