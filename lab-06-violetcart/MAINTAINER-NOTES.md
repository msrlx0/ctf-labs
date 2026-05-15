# Maintainer Notes - VioletCart

These notes are for development and release management. They are not a student walkthrough.

## Private Materials

`WALKTHROUGH.md` is private/development-only. It contains solution detail and should not be published as-is to the public main branch.

The private full-path regression script is maintainer-only because it verifies the complete intended path and reveals the shape of the solution. Keep it in a private branch or remove it before public release.

The private secondary-vulnerability regression script is mostly safe from a final-flag perspective, but it still reveals methodology, payload style, and intended bug classes. Treat it as optional public material only if that matches the release policy.

`VALIDATION.md` is useful for maintainers, but it describes intended behavior in enough detail to reduce challenge difficulty. Review it before publishing.

## Public Release Preparation

Before merging to public main:

- preserve this development state in a private branch or tag;
- replace the private walkthrough with `WALKTHROUGH.public.md`;
- remove the full-chain validation script from the public release;
- decide whether secondary validation methodology should be public;
- run the public leak checks in `PUBLIC-RELEASE-CHECKLIST.md`;
- rebuild Docker and smoke test port `8098`.

Suggested private preservation:

```bash
git branch private/lab-06-violetcart-maintainer
git tag private/lab-06-violetcart-maintainer-pre-public
```

Push private refs only to a private remote.

## Public Snapshot Note

The public release intentionally omits full solution automation. Maintainer-only walkthrough content and validation scripts must be kept in the development branch history or a private copy.

To continue private development with the complete maintainer materials, use the `lab-06-violetcart` branch history before the public-sanitization commit or restore them from a private maintainer branch/tag.
