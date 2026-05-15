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
11. Apply `PURPLE-STAFF` in partner context, including duplicate coupon variant.
12. Confirm order with `payment_method=partner_settlement`; confirm final flag.

### Vulnerability checklist

- Contextual IDOR requires valid quote/reservation/token context.
- Cross-object IDOR cannot be solved by a blind `id=1` request.
- Seller access changes only with valid internal reservation and partner header.
- Public checkout order remains safe in UI.
- Staff coupon fails before seller approval.
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
