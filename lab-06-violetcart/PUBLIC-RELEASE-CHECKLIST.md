# Public Release Checklist - VioletCart

This checklist describes the public-safe release state for `lab-06-violetcart`.

## Completed Public-Safety Actions

- `WALKTHROUGH.md` has been replaced with a public placeholder.
- The public walkthrough contains no flags, payloads, endpoint sequence, or exploit chain.
- Maintainer validation scripts have been removed from the public release.
- `scripts/README.md` explains that maintainer automation is intentionally absent.
- `VALIDATION.md` has been sanitized to public-safe smoke checks.
- `README.md` and `STUDENT-GUIDE.md` remain high-level and public-safe.
- Fake QA placeholder material remains clearly marked fake or not a challenge flag.

## Keep In Public Release

- `README.md`
- `STUDENT-GUIDE.md`
- `WALKTHROUGH.md`
- `WALKTHROUGH.public.md`
- `VALIDATION.md`
- `PUBLIC-RELEASE-CHECKLIST.md`
- `MAINTAINER-NOTES.md`
- `Dockerfile`
- `docker-compose.yml`
- `app/`
- `database/`
- `scripts/README.md`

## Keep Only Privately

- original maintainer walkthrough content;
- full-chain validation automation;
- secondary vulnerability validation automation;
- any notes containing flags, exploit payloads, or exact solution sequencing.

Those materials should remain in private branch history, a private tag, or an out-of-repository maintainer archive.

## Runtime Checks

```bash
cd ~/ctf-labs/lab-06-violetcart
docker compose down -v
docker compose up -d --build
sleep 7
curl -I http://localhost:8098
```

Expected:

- HTTP 200 from `http://localhost:8098`;
- VioletCart headers are present;
- Docker Compose still maps `8098:80`.

## Public Leak Checks

Use the private maintainer flag list outside this repository for exact literal checks. Public files should not contain real challenge flags.

Broad public scans:

```bash
grep -Rni 'FLAG[{]' README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md MAINTAINER-NOTES.md VALIDATION.md app database scripts || true
grep -Rni "placeholder\|fake QA\|not a challenge flag" README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md MAINTAINER-NOTES.md VALIDATION.md app database scripts || true
```

The fake QA placeholder may appear only if the surrounding text clearly says it is fake, a placeholder, or not a challenge flag.

## Solution-Chain Leak Checks

Public documentation and public scripts should not include final solution sequencing, exact payloads, or complete validation automation. Run:

```bash
grep -Rni "full chain\|final chain\|solution chain\|official solution\|exploit chain" README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md MAINTAINER-NOTES.md VALIDATION.md scripts || true
```

Review any hit manually. It is acceptable for release notes to say that solution material is intentionally omitted.

## Docker Build Check Before Merge

Run the runtime checks above immediately before merging to public main. Do not merge if Docker fails, port `8098` changes, or the public leak checks show real challenge flags.

## Private Development Continuation

To continue private development with maintainer materials, use branch history before this public-sanitization commit or a private copy created before public release.
