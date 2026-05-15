# Public Release Checklist - VioletCart

Use this checklist before merging `lab-06-violetcart` into a public main branch.

## File Classification

| Classification | Files | Public release action |
|---|---|---|
| A. Public-safe | `README.md`, `STUDENT-GUIDE.md`, `WALKTHROUGH.public.md`, `Dockerfile`, `docker-compose.yml`, `app/`, `database/` | Keep after leak checks pass. |
| B. Maintainer-only | `WALKTHROUGH.md`, `VALIDATION.md`, `scripts/validate-main-chain.sh`, `MAINTAINER-NOTES.md` | Keep in private/development branches only. |
| C. Public-safe with caution | `scripts/validate-secondary-vulns.sh` | Keep only if you are comfortable publishing methodology hints; otherwise remove from public release. |
| D. Must be removed or replaced before public main | Real `WALKTHROUGH.md`, `scripts/validate-main-chain.sh` | Replace walkthrough with public placeholder and remove the full-chain script. |

## Required Runtime Checks

```bash
cd ~/ctf-labs/lab-06-violetcart
docker compose down -v
docker compose up -d --build
sleep 7
curl -I http://localhost:8098
```

Expected:

- HTTP 200 response from `http://localhost:8098`
- VioletCart headers are present
- Docker Compose exposes `8098:80`

## Public Flag Leak Checks

Do not place full challenge flag literals in public files. Run these checks before release:

```bash
public_files="README.md STUDENT-GUIDE.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md app database"
for flag_id in \
  violet_recon_requires_patience \
  legacy_context_changes_everything \
  seller_flow_was_never_public \
  impossible_is_just_context_reused_correctly
do
  grep -Rni "FLAG{${flag_id}}" $public_files || true
done
```

Optional broad scan:

```bash
grep -Rni "FLAG{" README.md STUDENT-GUIDE.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md app database || true
```

The fake QA placeholder may appear only when clearly labeled as fake or not a challenge flag. Confirm every hit is labeled:

```bash
grep -Rni "qa_placeholder\|placeholder" app database README.md STUDENT-GUIDE.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md || true
```

## Walkthrough Handling

`WALKTHROUGH.md` is maintainer-only in the development branch. Before public merge, replace it with the public placeholder:

```bash
cp WALKTHROUGH.public.md WALKTHROUGH.md
git add WALKTHROUGH.md WALKTHROUGH.public.md
git diff -- WALKTHROUGH.md
```

The public replacement must not include flags, endpoint sequence, payloads, or the exploit chain.

## Script Handling

`scripts/validate-main-chain.sh` verifies the full chain and should not be included in a student-facing release:

```bash
git rm scripts/validate-main-chain.sh
```

`scripts/validate-secondary-vulns.sh` does not validate the final chain, but it still reveals methodology and payload style. Choose one release policy:

```bash
# Conservative public release:
git rm scripts/validate-secondary-vulns.sh

# Or keep it only if public methodology hints are acceptable:
git add scripts/validate-secondary-vulns.sh
```

If removing all maintainer scripts, remove the directory when empty:

```bash
rmdir scripts 2>/dev/null || true
```

## Maintainer Material Preservation

Before replacing/removing maintainer material, preserve the development state privately:

```bash
git branch private/lab-06-violetcart-maintainer
git tag private/lab-06-violetcart-maintainer-pre-public
```

Push those private refs only to a private remote:

```bash
git push <private-remote> private/lab-06-violetcart-maintainer
git push <private-remote> private/lab-06-violetcart-maintainer-pre-public
```

## Final Public Commit Shape

Keep:

- `README.md`
- `STUDENT-GUIDE.md`
- public placeholder walkthrough as `WALKTHROUGH.md`
- `WALKTHROUGH.public.md` if you want to keep the source placeholder
- `Dockerfile`
- `docker-compose.yml`
- `app/`
- `database/`
- optionally `scripts/validate-secondary-vulns.sh`

Remove or replace:

- real `WALKTHROUGH.md`
- `scripts/validate-main-chain.sh`
- `VALIDATION.md`, unless your public release intentionally includes maintainer validation notes
- `MAINTAINER-NOTES.md`, unless your public release intentionally includes release-process notes

## Final Verification

```bash
git status
git diff --stat
grep -Rni "TODO\|FIXME\|var_dump\|print_r\|die(" app database README.md STUDENT-GUIDE.md WALKTHROUGH.md WALKTHROUGH.public.md PUBLIC-RELEASE-CHECKLIST.md || true
```

The final public diff should contain no solution walkthrough, no full-chain automation, no real flags, and no accidental debug output.
