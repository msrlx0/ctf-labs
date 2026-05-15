# Student Guide - Lab 6 VioletCart

## Mindset

VioletCart is meant to feel unfair at first, but it is fair. Every intended step has at least one clue. The challenge is that clues are distributed across normal application features and only become meaningful after comparison.

Start from the browser. Use a proxy and repeater when behavior changes slightly between requests.

## Scope

Target only:

```text
http://localhost:8098
```

Use the provided Docker lab locally. No internet access or external callback is required.

## Starting point

Use the public training account:

```text
guest / guest123
```

## How to work

1. Browse the home page, catalog, car details, financing, checkout, support, reviews, and documents.
2. Read response headers. Some are more useful than they look.
3. Inspect frontend JavaScript and compare it with what the UI actually sends.
4. Create normal quotes and reservations before testing odd behavior.
5. Repeat requests in a proxy and change one variable at a time.
6. Compare public checkout behavior with partner/seller wording.
7. Track IDs, tokens, references, state keys, and status messages.
8. Return to earlier endpoints after state changes.
9. Treat errors as clues, especially when two similar requests fail differently.

## Things that matter

- Context matters more than a single parameter.
- Public checkout and partner checkout are not the same thing.
- A valid quote does not automatically mean a valid reservation.
- A visible reference may not be the reference required by another workflow.
- A coupon can be valid but still not useful in the current context.
- Seller review state and checkout state can disagree.
- Legacy endpoints may look useless if called too early.
- Rabbit holes are usually labeled by behavior, not by comments.

## Useful areas to inspect

- Catalog sorting and hidden metadata hints.
- Financing quote responses.
- Reservation creation responses.
- Coupon errors.
- Support ticket language.
- Public document IDs.
- Download behavior for approved documents.
- Search and review rendering differences.
- Inspection/import status behavior.
- Query-like JSON endpoints.
- Seller pages and their forbidden responses.

## Avoid

- Brute force.
- Guessing random IDs without context.
- External SSRF or callbacks.
- Trying to steal cookies.
- Treating the admin page as the main path.
- Assuming a common XSS payload will work.
- Assuming the first credential-like string is useful.

## Testing style

Burp or a similar proxy is strongly recommended. Compare near-identical requests, including duplicate parameters, custom headers, JSON bodies with extra fields, and requests repeated after state changes. Some filters are intentionally strong, so common payloads may fail even when a context-specific issue still exists.

## If you are stuck

Ask what changed:

- Did a header change?
- Did the status code change?
- Did the error become more specific?
- Did the response mention public vs partner context?
- Did an earlier request create state that a later request now trusts?
- Did a support ticket or document describe the same concept with different words?

The solution is a chain of context reuse, not a single bug.
