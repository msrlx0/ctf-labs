# Validation - Lab 6 VioletCart

This file is for maintainers. Keep public README and Student Guide spoiler-safe.

Before public merge, remove or replace `WALKTHROUGH.md` with a spoiler warning stub.

## Setup validation

```bash
cd lab-06-violetcart
docker compose up --build
```

In another terminal:

```bash
docker compose ps
curl -I http://localhost:8098
curl -s http://localhost:8098 | head
```

Expected:

- web container healthy enough to serve Apache/PHP;
- MySQL initialized from `database/init.sql`;
- port mapping includes `8098:80`.

## Public leak checks

```bash
grep -R "FLAG{" README.md STUDENT-GUIDE.md app/assets app/*.php app/api app/seller app/legacy || true
grep -R "FLAG{violet_recon_requires_patience}" README.md STUDENT-GUIDE.md app/ database/ || true
grep -R "FLAG{legacy_context_changes_everything}" README.md STUDENT-GUIDE.md app/ database/ || true
grep -R "FLAG{seller_flow_was_never_public}" README.md STUDENT-GUIDE.md app/ database/ || true
grep -R "FLAG{impossible_is_just_context_reused_correctly}" README.md STUDENT-GUIDE.md app/ database/ || true
grep -R "8098" .
grep -R "impossible_is_just_context_reused_correctly" README.md STUDENT-GUIDE.md app/assets || true
grep -R "legacy_context_changes_everything" README.md STUDENT-GUIDE.md app/assets || true
```

Expected:

- README and Student Guide have no real flags.
- Public assets should not include real flags.
- App source may contain runtime flag construction, but exact real flag strings must not appear in README, Student Guide, app files, static files, logs, or database seed.
- Fake QA placeholder flags may appear only when clearly labeled as fake/QA placeholders.
- Port `8098` appears in Docker/docs.

## Manual public UX checklist

1. Visit `http://localhost:8098`.
2. Login as `guest / guest123`.
3. Browse home, catalog, detail, finance, checkout, support, reviews, documents.
4. Confirm no real flag appears in normal UI.
5. Confirm admin page is a rabbit hole and labels the QA placeholder as fake.
6. Confirm support tickets give hints but not exact solution steps.
7. Confirm public docs hint at formats and checkpoint/trace values but do not reveal final flags or final chain.
8. Confirm `VC-2026-0017.txt` is reachable through normal document browsing/download.

## Maintainer endpoint checklist

Validate each behavior with Burp/Repetitor or curl equivalents.

### Main chain

1. Create quote with `/api/create_quote.php`.
2. Create reservation with `/api/create_reservation.php`.
3. Read the tracked public recon memo `VC-2026-0017.txt` through `documents.php` or `download.php` and confirm it only contains checkpoint/trace clues.
4. Submit the checkpoint and trace to `/api/query.php` with `query=reconCheckpoint` and confirm Flag 1 is returned dynamically.
5. Call `/legacy/quote-sync.php` with no channel and confirm `missing_channel`.
6. Call it with public channel and confirm `unsupported_public_flow`.
7. Call it with `X-Violet-Channel: partner_checkout`, quote, reservation, and public token; confirm seller ref, internal reservation, and Flag 2.
8. Open `/seller/reservation.php?ref=<internal>` without header and confirm 403.
9. Repeat with `X-Violet-Channel: partner_checkout`; confirm seller context and Flag 3.
10. Approve through `/seller/review.php`.
11. Confirm `PURPLE-STAFF` fails without `X-Violet-Channel: partner_checkout`, even after seller approval.
12. Confirm `PURPLE-STAFF` fails with `X-Violet-Channel: public_checkout`.
13. Apply `PURPLE-STAFF` in active partner context, including duplicate coupon variant.
14. Confirm order with `payment_method=partner_settlement`; confirm final flag and non-zero `order_id`/realistic `order_ref`.

### Vulnerability checklist

- Contextual IDOR requires valid quote/reservation/token context.
- Cross-object IDOR cannot be solved by a blind `id=1` request.
- Seller access changes only with valid internal reservation and partner header.
- Public checkout order remains safe in UI.
- Staff coupon fails before seller approval and without the active partner checkout header.
- Legacy sync returns realistic staged errors.
- Header context changes only selected endpoints.
- Quote/reservation state changes after legacy sync.
- Public token is over-trusted by reservation status.
- Headers, JS, support tickets, docs, and response differences expose subtle clues.
- Search reflected XSS blocks common payloads but JS-string proof works.
- Review stored XSS blocks common payloads but attribute proof works.
- Duplicate coupon parameters show parser inconsistency.
- Reservation mass assignment only partially influences state.
- Predictable documents expose hints, not final flag.
- Download behavior reads safe public docs; ignored runtime logs are not required for solving.
- Sort bug leaks behavior but not database contents.
- Inspection endpoint never reaches external network.
- Redirect stays non-destructive.
- Query endpoint reveals policy hints but blocks obvious sensitive names.

## Phase 2 deep audit matrix

Use this checklist to confirm every intended issue is testable, clue-backed, and unable to skip the final chain.

| # | Type | Endpoint | Blocked/simple behavior | Successful/contextual behavior | Impact | Final-chain safety |
|---|---|---|---|---|---|---|
| 1 | Main | `/api/reservation_status.php` | Random `id=1` or bad token returns 403/404. | Valid quote/reservation/token returns state; after sync it includes internal hints. | Contextual IDOR and state disclosure. | No flag returned. |
| 2 | Main | reservation/legacy/seller APIs | Mismatched quote, reservation, token, or car fails. | Correlated `quote_id`, `reservation_id`, `car_id`, `seller_ref`, and `internal_reservation` move the flow. | Cross-object authorization weakness. | Requires normal staged state. |
| 3 | Main | `/seller/reservation.php` | Missing or public channel header returns 403. | Valid internal ref plus `X-Violet-Channel: partner_checkout` renders seller view. | Broken access control by spoofed context. | Only Flag 3, no final order. |
| 4 | Main | `/api/confirm_order.php` | Public flow, missing approval, wrong coupon, or wrong payment fails. | Partner state plus seller approval plus staff coupon confirms order. | Business logic flaw. | Still requires all prior gates. |
| 5 | Main | `/api/apply_coupon.php` | `PURPLE-STAFF` fails without partner header, with `public_checkout`, or before approval. | Partner header, partner state, approval, and duplicate coupon can apply it. | Coupon logic/parser bypass. | Does not confirm order alone. |
| 6 | Main | `/legacy/quote-sync.php` | GET, missing channel, public channel, or bad context fails. | Partner header plus quote/reservation/token creates seller state and Flag 2. | Legacy endpoint abuse. | Needs prior quote/reservation. |
| 7 | Main | multiple | Header alone fails where state is missing. | Header changes behavior on legacy, seller, coupon, hold, and order endpoints. | Header/context confusion. | Header never bypasses all gates alone. |
| 8 | Main | quote/reservation/coupon/order | Public cache/state cannot settle. | Sync/hold rewrites cache keys to partner state. | Cache/key/state confusion. | Requires seller approval and coupon. |
| 9 | Main | reservation/order APIs | Public token alone cannot approve seller or order. | Public token authorizes too much reservation state when paired with IDs. | Token confusion. | No direct final flag. |
| 10 | Main | headers, JS, support, docs, errors | Direct source-visible real flag grep must fail. | Correlating hints reveals checkpoint, channel, sync path, and state vocabulary. | Information disclosure. | Clues only, except intended dynamic flags. |
| 11 | Secondary | `/search.php?q=` | `<script>`, `<img onerror>`, `<svg onload>`, `javascript:`, `alert`, `prompt`, `confirm`, `document.cookie` are filtered. | JS-string payload such as `';window.violetProof=1;violetSearchNotice('proof');//` changes local UI state. | Reflected XSS in JS string context. | No flags or cookies needed. |
| 12 | Secondary | `/reviews.php` | Common script/image/svg/event-handler payloads in body/title/display fields fail. | Attribute-breaking proof such as `" autofocus onfocus="violetReviewProof('stored')` works in title/display context. | Stored XSS attribute context. | Harmless local proof only. |
| 13 | Secondary | `/api/apply_coupon.php` | Single public `PURPLE-STAFF` request fails. | `coupon=WELCOME10&coupon=PURPLE-STAFF` shows `frontend_seen` vs `backend_applied` after partner approval. | HTTP parameter pollution. | Coupon gate still enforced. |
| 14 | Secondary | `/api/create_reservation.php` | Extra fields do not approve seller or return flags. | `channel`, `requested_status`, and `partner_hint` alter partial state/header clues. | Mass assignment. | Misleading/intermediate state only. |
| 15 | Secondary | `/documents.php` | Predicting docs reveals no real flags. | `VC-2026-0017` and neighbors expose checkpoint/trace and workflow vocabulary. | Predictable document IDs. | Flag 1 still requires query confirmation. |
| 16 | Secondary | `/download.php` | `../`, `..\\`, `%2e%2e`, `/etc/passwd`, `php://`, `data://`, `file://`, `expect://` fail. | Safe public paths, including normalized public-doc paths, download approved docs. | Limited path normalization/download mirror. | Cannot read source, creds, system files, or flags. |
| 17 | Secondary | `/cars.php?sort=` | `union`, `select`, `sleep`, `benchmark`, comments, quotes, and `or 1=1` are blocked. | Hidden names like `partner_only` or invalid safe-looking sort keys expose warnings/errors/order changes. | Filtered order-by bug. | No DB dump or flags. |
| 18 | Secondary | `/api/vehicle_inspection.php` | `localhost`, loopback, metadata, `file://`, `gopher://`, and `dict://` fail. | `violet://inspection/<VIN>` and `https://inspection.violet.local/status?vin=<VIN>` return seeded inspection hints. | Controlled SSRF-like alias resolver. | No network requests or flags. |
| 19 | Secondary | `/redirect.php?next=` | `http://`, `https://`, `//`, backslash tricks, and `evil.com` fail. | Relative paths redirect and reveal internal routing assumptions. | Weak relative redirect/allowlist behavior. | Not useful for phishing or flags. |
| 20 | Secondary | `/api/query.php` | `debugFlags`, `sellerNotes`, `internalReservation`, `flags`, `admin`, `password` fail. | `quoteMeta`, `channelPolicy`, `reconCheckpoint`, and `inspectionProfile` return safe clues. | Query enumeration. | Only exact recon checkpoint returns Flag 1. |

For all secondary issues, confirm the expected impact is a clue, local proof, or state observation only. None should create seller approval, apply the staff coupon without all gates, or return the final flag.

## Final safety checks

```bash
grep -R "FLAG{" README.md STUDENT-GUIDE.md || true
grep -R "WALKTHROUGH.md" README.md
grep -R "8098:80" docker-compose.yml
git ls-files app/storage/public_docs/VC-2026-0017.txt
git status --ignored -s app/storage
find app -name "*.php" -print0 | xargs -0 -n1 php -l
```

Expected:

- no flags in public docs;
- README warns that real walkthrough is not for public release;
- compose exposes `8098:80`;
- required public recon clues are in tracked `app/storage/public_docs/VC-2026-0017.txt`;
- ignored `app/storage/logs/` files are not required for the intended path;
- PHP syntax passes.
