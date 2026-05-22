# Lab 6 - VioletCart

VioletCart is a premium online car marketplace CTF lab focused on realistic web application behavior, subtle context reuse, business logic flaws, and manual HTTP analysis.

Difficulty: **Impossible, but fair**.

Port: **8098**.

Status: **Public-release snapshot**.

## Story

VioletCart is migrating from a public vehicle checkout flow to a seller-assisted partner checkout flow. The migration left behind inconsistent validation, legacy behavior, confusing state transitions, old support notes, and context-dependent responses.

The public UI is styled as a dark violet premium vehicle marketplace with financing, reservation, inspection, support, document, review, and seller-desk surfaces.

The lab is intentionally hard because the correct path is spread across pages, JavaScript, response headers, support tickets, public documents, seller workflow behavior, and repeated request comparison. It should not require brute force, internet access, external callbacks, malware, or destructive behavior.

## Run

```bash
cd lab-06-violetcart
docker compose up --build
```

Open:

```text
http://localhost:8098
```

Docker Compose exposes:

```text
8098:80
```

## Recommended tools

- Browser
- Burp Suite, Caido, OWASP ZAP, or another HTTP proxy
- Repeater
- Local decoders/encoders
- Notes

This lab is not designed as a copy/paste terminal challenge. The intended experience is to intercept, compare, modify, retry, and correlate.

No brute force is needed. Most progress comes from manual request analysis: comparing headers, duplicate parameters, JSON fields, redirects, document IDs, and how older migration endpoints react after normal application state has been created.

## Public starting point

Use the public training account shown by the application:

```text
guest / guest123
```

## Rules

- Use only the local Docker environment.
- Do not attack real systems.
- Do not use brute force.
- Do not use external callbacks.
- Do not use scanners that create noisy traffic.
- Treat all hostnames and business data as fictional lab content.

## What to expect

You will need to inspect:

- catalog and car details;
- financing simulation;
- reservation and checkout behavior;
- coupon responses;
- account/order state;
- support tickets;
- public financing documents;
- frontend JavaScript;
- response headers;
- seller/partner workflow behavior;
- legacy migration responses.

Some paths are honest rabbit holes. They are useful for learning what is not the answer.

Advanced secondary vulnerabilities are present for manual testing practice. They are designed to be safe, local, and context-dependent; common payloads may fail, and careful request comparison is expected.

## Public release note

The official solution walkthrough is not included in the public release. `WALKTHROUGH.md` is a public-safe placeholder.

`README.md` and `STUDENT-GUIDE.md` are intended to remain public-safe and must not contain flags or exact solution steps.

## Public Release Notes

The official walkthrough is not included in the public release. Maintainers should follow `PUBLIC-RELEASE-CHECKLIST.md` before publishing.

No brute force is required. Burp Suite or similar manual HTTP analysis is recommended.
