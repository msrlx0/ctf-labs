#!/usr/bin/env bash
set -u

BASE_URL="${BASE_URL:-http://localhost:8098}"
COOKIE_JAR="$(mktemp)"
FAILURES=0

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

json_value() {
  local json="$1"
  local key="$2"
  printf '%s' "$json" | sed -n 's/.*"'"$key"'":[[:space:]]*\("[^"]*"\|[0-9][0-9]*\|true\|false\).*/\1/p' | head -n1 | tr -d '"'
}

urlencode() {
  local value="$1"
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$value"
}

require_lab() {
  banner "VioletCart secondary vulnerability validation"
  printf 'Target: %s\n' "$BASE_URL"
  local head
  head="$(curl -sS -I "$BASE_URL/" || true)"
  contains "$head" "200 OK" "lab responds on $BASE_URL"
  contains "$head" "X-Violet-App: VioletCart" "VioletCart headers are present"
}

create_quote_and_reservation() {
  local quote reservation quote_id token reservation_id
  quote="$(http_post "/api/create_quote.php" "car_id=5&term_months=60&down_payment=25000")"
  quote_id="$(json_value "$quote" "quote_id")"
  token="$(json_value "$quote" "public_token")"
  if [ -z "$quote_id" ] || [ -z "$token" ]; then
    fail "created quote for stateful tests"
    return 1
  fi
  reservation="$(http_post "/api/create_reservation.php" "quote_id=$quote_id&public_token=$token")"
  reservation_id="$(json_value "$reservation" "reservation_id")"
  if [ -z "$reservation_id" ]; then
    fail "created reservation for stateful tests"
    return 1
  fi
  printf '%s|%s|%s\n' "$quote_id" "$token" "$reservation_id"
}

test_reflected_xss() {
  banner "A. Reflected XSS filter behavior"
  local payloads payload encoded body proof
  payloads=(
    '<script>alert(1)</script>'
    '<img src=x onerror=alert(1)>'
    '<svg onload=alert(1)>'
    'javascript:alert(1)'
    'document.cookie'
  )
  for payload in "${payloads[@]}"; do
    encoded="$(urlencode "$payload")"
    body="$(http_get "/search.php?q=$encoded")"
    contains "$body" "[filtered]" "search marks filtered content for common payload: $payload"
    not_contains "$body" "alert(1)" "search removes alert primitive for: $payload"
    not_contains "$body" "document.cookie" "search does not reflect document.cookie primitive for: $payload"
  done

  proof="';window.violetProof=1;violetSearchNotice('proof');//"
  body="$(http_get "/search.php?q=$(urlencode "$proof")")"
  contains "$body" "window.violetProof=1" "search reflects JS-string context proof marker"
  not_contains "$body" "<script>alert(1)</script>" "search proof does not rely on script tags"
  pass "curl confirms reflection and filtering; browser/Burp is required to prove JavaScript execution"
}

test_stored_xss() {
  banner "B. Stored XSS filter behavior"
  local bad_title bad_body response page proof
  bad_title='<img src=x onerror=alert(1)>'
  bad_body='<script>alert(1)</script>'
  response="$(http_post "/reviews.php" "car_id=1&display_name=$(urlencode "$bad_title")&title=$(urlencode "$bad_title")&body=$(urlencode "$bad_body")&rating=5")"
  contains "$response" "Review submitted" "review submission accepts test review"
  page="$(http_get "/reviews.php")"
  not_contains "$page" "<script>alert(1)</script>" "stored review body filters script tag"
  not_contains "$page" "onerror=alert(1)" "stored review fields filter common onerror payload"

  proof='" autofocus onfocus="violetReviewProof('\''stored'\'')'
  response="$(http_post "/reviews.php" "car_id=1&display_name=$(urlencode "$proof")&title=AttributeProof&body=Harmless%20local%20proof&rating=5")"
  page="$(http_get "/reviews.php")"
  contains "$page" "violetReviewProof" "review attribute context stores safe proof marker"
  not_contains "$page" "FLAG{" "stored XSS page does not expose flags"
  pass "curl confirms stored marker and filters; browser/Burp is required to prove focus-trigger execution"
}

test_hpp() {
  banner "C. HTTP Parameter Pollution"
  local state quote_id token reservation_id legacy internal review public_try public_header_try dup
  state="$(create_quote_and_reservation)" || return
  IFS='|' read -r quote_id token reservation_id <<< "$state"

  legacy="$(http_post "/legacy/quote-sync.php" "quote_id=$quote_id&reservation_id=$reservation_id&public_token=$token" -H 'X-Violet-Channel: partner_checkout')"
  internal="$(json_value "$legacy" "internal_reservation")"
  if [ -z "$internal" ]; then
    fail "legacy sync created internal reservation for HPP test"
    return
  fi
  review="$(http_post "/seller/review.php" "internal_reservation=$internal&decision=approve" -H 'X-Violet-Channel: partner_checkout')"
  contains "$review" '"reviewed": true' "seller review approved for HPP setup"

  public_try="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=PURPLE-STAFF")"
  contains "$public_try" "coupon_not_valid_for_public_checkout" "PURPLE-STAFF fails without partner header"
  not_contains "$public_try" '"applied": true' "public staff coupon request is not applied"

  public_header_try="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=PURPLE-STAFF" -H 'X-Violet-Channel: public_checkout')"
  contains "$public_header_try" "coupon_not_valid_for_public_checkout" "PURPLE-STAFF fails with explicit public header"

  dup="$(http_post "/api/apply_coupon.php" "quote_id=$quote_id&reservation_id=$reservation_id&coupon=WELCOME10&coupon=PURPLE-STAFF" -H 'X-Violet-Channel: partner_checkout')"
  contains "$dup" '"applied": true' "duplicate coupon request applies in partner context"
  contains "$dup" '"frontend_seen": "WELCOME10"' "frontend parser sees first coupon"
  contains "$dup" '"backend_applied": "PURPLE-STAFF"' "backend parser applies last coupon"
  contains "$dup" '"channel": "partner_checkout"' "coupon response remains partner scoped"
}

test_mass_assignment() {
  banner "D. Mass assignment"
  local quote quote_id token reservation
  quote="$(http_post "/api/create_quote.php" "car_id=5&term_months=60&down_payment=25000")"
  quote_id="$(json_value "$quote" "quote_id")"
  token="$(json_value "$quote" "public_token")"
  reservation="$(http_post "/api/create_reservation.php" "quote_id=$quote_id&public_token=$token&channel=partner_checkout&requested_status=seller_review&partner_hint=seller-assisted")"
  contains "$reservation" '"channel": "partner_checkout"' "extra channel field influences reservation response"
  contains "$reservation" "seller_review_requested" "requested_status/partner_hint influence intermediate state"
  not_contains "$reservation" '"seller_status": "approved"' "mass assignment does not grant seller approval"
  not_contains "$reservation" "internal_reservation" "mass assignment does not create internal reservation"
  not_contains "$reservation" "final_order" "mass assignment does not expose final flag material"
  not_contains "$reservation" "FLAG{" "mass assignment does not return any flag"
}

test_documents() {
  banner "E. Predictable documents"
  local index memo qa
  index="$(http_get "/documents.php")"
  contains "$index" "VC-2026-0017" "document index exposes tracked recon memo ID"
  memo="$(http_get "/download.php?file=public_docs/VC-2026-0017.txt")"
  contains "$memo" "checkpoint: violet-recon-patience" "recon memo contains checkpoint clue"
  contains "$memo" "trace: quote-migration-2026-02" "recon memo contains trace clue"
  not_contains "$memo" "FLAG{" "recon memo does not contain flags"
  qa="$(http_get "/download.php?file=public_docs/VC-2026-0011.txt")"
  if printf '%s' "$qa" | grep -Fq 'FLAG{qa_placeholder_ignore_me}'; then
    contains "$qa" "This placeholder is not a challenge flag" "QA placeholder is clearly marked fake"
  else
    pass "QA placeholder not present in fetched document"
  fi
}

test_lfi() {
  banner "F. Limited LFI / path normalization"
  local payload body safe
  local payloads=(
    '../includes/db.php'
    '../../../../etc/passwd'
    '%2e%2e%2fincludes%2fdb.php'
    'php://filter/resource=index.php'
    'file:///etc/passwd'
    'data://text/plain,test'
  )
  for payload in "${payloads[@]}"; do
    body="$(http_get_i "/download.php?file=$payload")"
    contains "$body" "Invalid public document path" "download blocks unsafe value: $payload"
    not_contains "$body" "DB_PASS" "blocked download does not reveal DB credentials"
    not_contains "$body" "<?php" "blocked download does not reveal PHP source"
    not_contains "$body" "root:" "blocked download does not reveal passwd content"
    not_contains "$body" "FLAG{" "blocked download does not reveal flags"
  done
  safe="$(http_get "/download.php?file=public_docs/VC-2026-0007.txt")"
  contains "$safe" "Public checkout quotes use a public token" "download allows safe public_docs file"
}

test_sort() {
  banner "G. Filtered SQL / ORDER BY issue"
  local payload body sort hidden
  local payloads=(
    'union select'
    'sleep(1)'
    'price desc--'
    "' or 1=1"
    'benchmark(1,md5(1))'
  )
  for payload in "${payloads[@]}"; do
    body="$(http_get_i "/cars.php?sort=$(urlencode "$payload")")"
    not_contains "$body" "SQLSTATE" "sort probe avoids raw SQL error: $payload"
    not_contains "$body" "information_schema" "sort probe does not dump database metadata: $payload"
    not_contains "$body" "FLAG{" "sort probe does not expose flags: $payload"
    contains "$body" "VioletCart vehicles" "sort probe still renders safe catalog page: $payload"
  done
  for sort in price year mileage; do
    body="$(http_get "/cars.php?sort=$sort")"
    contains "$body" "VioletCart vehicles" "normal sort works: $sort"
  done
  hidden="$(http_get_i "/cars.php?sort=partner_only")"
  contains "$hidden" "X-Violet-Sort-Warning: non-public-sort-key" "hidden sort key reveals only subtle warning"
  not_contains "$hidden" "FLAG{" "hidden sort key does not expose flags"
}

test_ssrf() {
  banner "H. Controlled SSRF-like behavior"
  local payload body alias
  local payloads=(
    'http://localhost/'
    'http://127.0.0.1/'
    'http://0.0.0.0/'
    'file:///etc/passwd'
    'gopher://127.0.0.1'
    'http://169.254.169.254/latest/meta-data/'
  )
  for payload in "${payloads[@]}"; do
    body="$(http_get_i "/api/vehicle_inspection.php?inspection_url=$(urlencode "$payload")")"
    contains "$body" "inspection_target_blocked" "inspection blocks unsafe target: $payload"
    not_contains "$body" "root:" "inspection does not read files for: $payload"
    not_contains "$body" "meta-data" "inspection does not return metadata for: $payload"
  done
  alias="$(http_get "/api/vehicle_inspection.php?inspection_url=$(urlencode "violet://inspection/VCORI2024BE")")"
  contains "$alias" '"status": "partner-hold"' "safe inspection alias returns seeded status clue"
  not_contains "$alias" "FLAG{" "inspection alias does not return flags"
}

test_redirect() {
  banner "I. Weak open redirect"
  local payload body
  local payloads=(
    'http://evil.com'
    'https://evil.com'
    '//evil.com'
    '/\evil.com'
  )
  for payload in "${payloads[@]}"; do
    body="$(http_get_i "/redirect.php?next=$(urlencode "$payload")")"
    not_contains "$body" "Location: http://evil.com" "redirect blocks external target: $payload"
    not_contains "$body" "Location: https://evil.com" "redirect blocks external HTTPS target: $payload"
    not_contains "$body" "Location: //evil.com" "redirect blocks scheme-relative target: $payload"
    contains "$body" "Location: /" "redirect falls back inside app: $payload"
  done
  body="$(http_get_i "/redirect.php?next=/checkout.php")"
  contains "$body" "Location: /checkout.php" "relative redirect demonstrates internal routing behavior"
  not_contains "$body" "FLAG{" "redirect does not expose flags"
}

test_query() {
  banner "J. Query endpoint enumeration"
  local query body
  for query in debugFlags sellerNotes internalReservation flags admin password; do
    body="$(http_get "/api/query.php?query=$query")"
    contains "$body" "query_blocked" "sensitive query is blocked: $query"
  done
  for query in quoteMeta channelPolicy inspectionProfile; do
    body="$(http_get "/api/query.php?query=$query")"
    contains "$body" "\"query\": \"$query\"" "safe query returns clue: $query"
    not_contains "$body" "final_order" "safe query does not expose final flag material: $query"
    not_contains "$body" "FLAG{" "safe query does not return flags: $query"
  done
  body="$(http_get "/api/query.php?query=reconCheckpoint&checkpoint=wrong&trace=wrong")"
  contains "$body" '"accepted": false' "wrong recon checkpoint is rejected"
  body="$(http_get "/api/query.php?query=reconCheckpoint&checkpoint=violet-recon-patience&trace=quote-migration-2026-02")"
  contains "$body" '"accepted": true' "correct recon checkpoint is accepted"
  contains "$body" '"flag":' "correct recon checkpoint returns only the first-stage flag key"
  not_contains "$body" "final_order" "recon checkpoint does not expose final flag material"
}

require_lab
test_reflected_xss
test_stored_xss
test_hpp
test_mass_assignment
test_documents
test_lfi
test_sort
test_ssrf
test_redirect
test_query

banner "Result"
if [ "$FAILURES" -eq 0 ]; then
  pass "all secondary vulnerability validations passed"
  exit 0
fi

fail "$FAILURES validation check(s) failed"
exit 1
