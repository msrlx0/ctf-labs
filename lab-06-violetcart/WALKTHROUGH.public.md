# VioletCart Walkthrough

The official solution walkthrough is intentionally not included in the public release.

VioletCart is designed for careful manual analysis, HTTP proxy testing, and context correlation. The intended path is not a brute-force exercise and does not require external callbacks, internet access, or destructive testing.

Every meaningful step has at least one clue. Those clues may appear in normal application behavior, response differences, frontend code, public-facing notes, headers, documents, or state changes after legitimate actions.

Recommended approach:

- browse the application normally first;
- use Burp Suite, Caido, OWASP ZAP, or a similar proxy;
- compare similar requests and responses;
- pay attention to headers, IDs, tokens, wording, and error messages;
- revisit earlier observations after state changes;
- avoid brute force and noisy scanning.

The challenge is intentionally difficult, but it is fair. Progress comes from correlating context rather than guessing secrets.
