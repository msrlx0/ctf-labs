#!/usr/bin/env bash
set -u

BASE_URL="${BASE_URL:-http://localhost:8098}"
COOKIE_JAR="$(mktemp)"
FAILURES=0
DEBUG="${DEBUG:-0}"

cleanup() {
  rm -f "$COOKIE_JAR"
}
trap cleanup EXIT

banner() {
  printf '\n== %s ==\n' "$1"
}

pass() {
  printf 'PASS: %s\n' "$1"
}

fail() {
  printf 'FAIL: %s\n' "$1"
  FAILURES=$((FAILURES + 1))
}

contains() {
  local haystack="$1"
  local needle="$2"
  local message="$3"
  if printf '%s' "$haystack" | grep -Fqi -- "$needle"; then
    pass "$message"
  else
    fail "$message"
  fi
}

not_contains() {
  local haystack="$1"
  local needle="$2"
  local message="$3"
  if printf '%s' "$haystack" | grep -Fqi -- "$needle"; then
    fail "$message"
  else
    pass "$message"
  fi
}

debug_dump() {
  local label="$1"
  local value="$2"
  if [ "$DEBUG" = "1" ]; then
    printf '\n-- DEBUG %s --\n%s\n' "$label" "$value"
  fi
}

http_get() {
  curl -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$BASE_URL$1"
}

http_get_i() {
  curl -sS -i -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$BASE_URL$1"
}

http_post() {
  local path="$1"
  local body="$2"
  shift 2
  curl -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" "$@" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data "$body" "$BASE_URL$path"
}

http_post_json() {
  local path="$1"
  local body="$2"
  curl -sS -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    -H 'Content-Type: application/json' \
    --data "$body" "$BASE_URL$path"
}

json_value() {
  local json="$1"
  local key="$2"
  printf '%s' "$json" | sed -n 's/.*"'"$key"'":[[:space:]]*\("[^"]*"\|[0-9][0-9]*\|true\|false\).*/\1/p' | head -n1 | tr -d '"'
}

urlencode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}

require_value() {
  local value="$1"
  local message="$2"
  if [ -n "$value" ]; then
    pass "$message"
  else
    fail "$message"
    return 1
  fi
}

banner "Main chain regression validation"
printf 'Target: %s\n' "$BASE_URL"

banner "1. Home and headers"
home_headers="$(curl -sS -I "$BASE_URL/" || true)"
debug_dump "home headers" "$home_headers"
contains "$home_headers" "200 OK" "home page responds with HTTP 200"
contains "$home_headers" "X-Violet-App:" "X-Violet-App header is present"
contains "$home_headers" "X-Violet-Flow:" "X-Violet-Flow header is present"
contains "$home_headers" "X-Violet-Legacy:" "X-Violet-Legacy header is present"

banner "2. Recon memo discovery"
docs="$(http_get "/documents.php")"
contains "$docs" "VC-2026-0017" "documents page lists VC-2026-0017"
memo="$(http_get "/download.php?file=VC-2026-0017.txt")"
contains "$memo" "checkpoint: violet-recon-patience" "recon memo contains checkpoint"
contains "$memo" "trace: quote-migration-2026-02" "recon memo contains trace"
not_contains "$memo" "legacy_context_changes_everything" "recon memo does not contain legacy flag material"
not_contains "$memo" "seller_flow_was_never_public" "recon memo does not contain seller flag material"
not_contains "$memo" "impossible_is_just_context_reused_correctly" "recon memo does not contain final flag material"

banner "3. Recon checkpoint"
wrong_recon="$(http_post_json "/api/query.php" '{"query":"reconCheckpoint","checkpoint":"wrong","trace":"wrong"}')"
debug_dump "wrong recon response" "$wrong_recon"
contains "$wrong_recon" '"accepted": false' "wrong checkpoint/trace fails"
right_recon="$(http_post_json "/api/query.php" '{"query":"reconCheckpoint","checkpoint":"violet-recon-patience","trace":"quote-migration-2026-02"}')"
debug_dump "right recon response" "$right_recon"
contains "$right_recon" '"flag":' "correct checkpoint returns a flag key"
contains "$right_recon" "violet_recon_requires_patience" "correct checkpoint returns first-stage flag identifier"
not_contains "$right_recon" "legacy_context_changes_everything" "recon response does not expose legacy stage"
not_contains "$right_recon" "seller_flow_was_never_public" "recon response does not expose seller stage"
not_contains "$right_recon" "impossible_is_just_context_reused_correctly" "recon response does not expose final stage"

banner "4. Quote and reservation"
quote_response="$(http_post "/api/create_quote.php" "car_id=1&down_payment=15000&term_months=48&months=48")"
debug_dump "quote response" "$quote_response"
quote_id="$(json_value "$quote_response" "quote_id")"
public_token="$(json_value "$quote_response" "public_token")"
require_value "$quote_id" "quote_id extracted"
require_value "$public_token" "public_token extracted"

reservation_response="$(http_post "/api/create_reservation.php" "quote_id=$quote_id&public_token=$(urlencode "$public_token")")"
debug_dump "reservation response" "$reservation_response"
reservation_id="$(json_value "$reservation_response" "reservation_id")"
require_value "$reservation_id" "reservation_id extracted"

banner "5. Legacy sync negative tests"
legacy_no_channel="$(http_post "/legacy/quote-sync.php" "quote_id=$quote_id&reservation_id=$reservation_id&public_token=$(urlencode "$public_token")")"
debug_dump "legacy no channel" "$legacy_no_channel"
contains "$legacy_no_channel" "missing_channel" "legacy sync without channel returns missing_channel"
legacy_public="$(http_post "/legacy/quote-sync.php" "channel=public_checkout&quote_id=$quote_id&reservation_id=$reservation_id&public_token=$(urlencode "$public_token")" -H 'X-Violet-Channel: public_checkout')"
debug_dump "legacy public channel" "$legacy_public"
contains "$legacy_public" "unsupported_public_flow" "legacy sync with public channel is rejected"

banner "6. Legacy sync positive test"
legacy_partner="$(http_post "/legacy/quote-sync.php" "channel=partner_checkout&quote_id=$quote_id&reservation_id=$reservation_id&public_token=$(urlencode "$public_token")" -H 'X-Violet-Channel: partner_checkout')"
debug_dump "legacy partner response" "$legacy_partner"
contains "$legacy_partner" '"synced": true' "legacy sync succeeds with partner context"
contains "$legacy_partner" '"seller_ref":' "legacy sync returns seller_ref"
contains "$legacy_partner" '"internal_reservation":' "legacy sync returns internal_reservation"
contains "$legacy_partner" '"flag":' "legacy sync returns a stage flag key"
contains "$legacy_partner" "legacy_context_changes_everything" "legacy sync returns legacy stage identifier"
contains "$legacy_partner" "/seller/reservation.php?ref=" "legacy sync returns next seller URL"
not_contains "$legacy_partner" "impossible_is_just_context_reused_correctly" "legacy sync does not expose final stage"
internal_reservation="$(json_value "$legacy_partner" "internal_reservation")"
require_value "$internal_reservation" "internal_reservation extracted"

banner "7. Seller reservation access control"
seller_public="$(http_get_i "/seller/reservation.php?ref=$(urlencode "$internal_reservation")")"
debug_dump "seller reservation public" "$seller_public"
contains "$seller_public" "403 Forbidden" "seller reservation without partner header is forbidden"
not_contains "$seller_public" "seller_flow_was_never_public" "forbidden seller page does not show seller flag identifier"

seller_partner="$(curl -sS -i -b "$COOKIE_JAR" -c "$COOKIE_JAR" -H 'X-Violet-Channel: partner_checkout' "$BASE_URL/seller/reservation.php?ref=$(urlencode "$internal_reservation")")"
debug_dump "seller reservation partner" "$seller_partner"
contains "$seller_partner" "Seller review" "seller reservation shows seller review context"
contains "$seller_partner" "seller_flow_was_never_public" "seller reservation shows seller stage identifier"
not_contains "$seller_partner" "impossible_is_just_context_reused_correctly" "seller reservation does not expose final stage"

banner "8. Seller review approval"
review_response="$(http_post "/seller/review.php" "internal_reservation=$(urlencode "$internal_reservation")&decision=approve" -H 'X-Violet-Channel: partner_checkout')"
debug_dump "seller review approval" "$review_response"
contains "$review_response" '"reviewed": true' "seller review returns reviewed=true"
contains "$review_response" '"seller_status": "approved"' "seller review returns approved status"

banner "9. Coupon gates"
coupon_no_header="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=PURPLE-STAFF")"
debug_dump "coupon no header" "$coupon_no_header"
not_contains "$coupon_no_header" '"applied": true' "PURPLE-STAFF without partner header is not applied"
contains "$coupon_no_header" "coupon_not_valid_for_public_checkout" "PURPLE-STAFF without partner header fails as public checkout"

coupon_public="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=PURPLE-STAFF" -H 'X-Violet-Channel: public_checkout')"
debug_dump "coupon public header" "$coupon_public"
not_contains "$coupon_public" '"applied": true' "PURPLE-STAFF with public header is not applied"
contains "$coupon_public" "coupon_not_valid_for_public_checkout" "PURPLE-STAFF with public header fails"

coupon_partner="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=WELCOME10&coupon=PURPLE-STAFF" -H 'X-Violet-Channel: partner_checkout')"
debug_dump "coupon partner duplicate" "$coupon_partner"
contains "$coupon_partner" '"applied": true' "duplicate coupon applies in partner context"
contains "$coupon_partner" '"frontend_seen": "WELCOME10"' "frontend sees WELCOME10"
contains "$coupon_partner" '"backend_applied": "PURPLE-STAFF"' "backend applies PURPLE-STAFF"
contains "$coupon_partner" '"channel": "partner_checkout"' "coupon response is partner scoped"

banner "10. Final confirmation"
confirm_response="$(http_post "/api/confirm_order.php" "quote_id=$quote_id&reservation_id=$reservation_id&payment_method=partner_settlement" -H 'X-Violet-Channel: partner_checkout')"
debug_dump "confirm response" "$confirm_response"
contains "$confirm_response" '"confirmed": true' "final confirm returns confirmed=true"
contains "$confirm_response" '"status": "confirmed"' "final confirm returns confirmed status"
contains "$confirm_response" '"flag":' "final confirm returns a flag key"
contains "$confirm_response" "impossible_is_just_context_reused_correctly" "final confirm returns final stage identifier"
order_id="$(json_value "$confirm_response" "order_id")"
if [ -n "$order_id" ] && [ "$order_id" != "0" ]; then
  pass "final confirm returns non-zero order_id"
else
  fail "final confirm returns non-zero order_id"
fi

banner "11. Duplicate final confirmation"
confirm_again="$(http_post "/api/confirm_order.php" "quote_id=$quote_id&reservation_id=$reservation_id&payment_method=partner_settlement" -H 'X-Violet-Channel: partner_checkout')"
debug_dump "duplicate confirm response" "$confirm_again"
contains "$confirm_again" '"confirmed": true' "duplicate final confirm remains stable"
contains "$confirm_again" '"order_id":' "duplicate final confirm returns a valid order id"
not_contains "$confirm_again" "seller_ref" "duplicate final confirm does not expose seller internals"

banner "Result"
if [ "$FAILURES" -eq 0 ]; then
  pass "main chain regression validation passed"
  exit 0
fi

fail "$FAILURES validation check(s) failed"
exit 1
