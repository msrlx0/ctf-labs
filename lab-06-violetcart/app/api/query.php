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
            'note' => 'Quote metadata alone does not prove seller review state.'
        ], 200, ['X-Violet-Trace' => 'query-quote-meta']);
        break;
    case 'channelPolicy':
        json_response([
            'query' => $query,
            'public_checkout' => ['quote simulation', 'buyer reservation', 'public promotion'],
            'partner_checkout' => ['reservation context', 'seller review state', 'settlement policy'],
            'note' => 'Policy names describe lanes; they do not initialize state.'
        ], 200, ['X-Violet-Trace' => 'query-channel-policy']);
        break;
    case 'documentIndex':
        json_response([
            'query' => $query,
            'documents' => ['VC-2026-0007', 'VC-2026-0011', 'VC-2026-0017', 'VC-2026-0020'],
            'note' => 'Tracked recon memos are public documents; checkpoint confirmation is stateful.'
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
            'note' => 'Seller review is mirrored from migration state, not from the parked admin console.'
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
            'operations_note' => 'Migration checkpoints become useful only after buyer-visible reservation context exists.'
        ], 200, ['X-Violet-Trace' => 'query-recon-checkpoint-confirmed']);
        break;
    default:
        json_response(['error' => 'query_not_found', 'message' => 'Query name is not in the public query registry.'], 404, ['X-Violet-Trace' => 'query-not-found']);
}
