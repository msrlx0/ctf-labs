# Validation - Lab 6 VioletCart

This public-safe validation guide verifies that the lab starts cleanly and does not leak solution material in release-facing files. It intentionally does not include the exploit chain, challenge flags, or commands that solve the lab.

Maintainer-only regression scripts and full solution notes are intentionally omitted from the public release. Keep those materials in private branch history or a private copy.

## Runtime Check

```bash
cd ~/ctf-labs/lab-06-violetcart
docker compose down -v
docker compose up -d --build
sleep 7
curl -I http://localhost:8098
```

Expected:

- the application responds with HTTP 200;
- VioletCart headers are present;
- Docker Compose exposes port `8098`;
- no external internet access is required.

## Public File Checks

Confirm the expected public files exist:

```bash
test -f README.md
test -f STUDENT-GUIDE.md
test -f WALKTHROUGH.md
test -f WALKTHROUGH.public.md
test -f PUBLIC-RELEASE-CHECKLIST.md
test -f MAINTAINER-NOTES.md
test -f scripts/README.md
```

Confirm maintainer automation is not present in the public release:

```bash
find scripts -maxdepth 1 -type f ! -name README.md -print
```

Expected: only `scripts/README.md` is present.

## Public Leak Checks

Run broad public scans:

```bash
grep -Rni 'FLAG[{]' README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md MAINTAINER-NOTES.md VALIDATION.md app database scripts || true
grep -Rni "TODO\|FIXME\|var_dump\|print_r\|die(" app database README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md MAINTAINER-NOTES.md VALIDATION.md scripts || true
```

The only acceptable challenge-flag-pattern hit in public-facing files is the clearly labeled fake QA placeholder. It must be marked as fake, placeholder, or not a challenge flag.

## Basic Page Smoke Test

These checks should return successful HTML responses without revealing flags:

```bash
curl -s http://localhost:8098/ | head
curl -s http://localhost:8098/cars.php | head
curl -s http://localhost:8098/support.php | head
curl -s http://localhost:8098/documents.php | head
curl -s http://localhost:8098/reviews.php | head
```

## Public Release Reminder

The public release should preserve challenge difficulty. Do not add walkthroughs, full-chain validation scripts, exact exploit payloads, or final solution sequencing to public files.
