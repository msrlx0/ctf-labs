'use strict';

/**
 * In-memory seed data for the ObsidianPay Mobile API (Phase 1).
 *
 * IMPORTANT (instructor note):
 * - Nothing here is a real secret. These are didactic, controlled values.
 * - The data model is intentionally shaped so future phases can introduce
 *   controlled vulnerabilities (e.g. multiple receipts per/across users to
 *   enable an IDOR study later). Phase 1 does NOT implement those flaws yet.
 */

const LAB = Object.freeze({
  name: 'ObsidianPay Mobile',
  lab: 'lab-08-obsidianpay',
  version: '0.1.0-phase1',
  port: 8102,
  phase: 1,
});

// Controlled test accounts. Phase 1 ships only the guest account as documented.
const users = [
  {
    id: 1001,
    username: 'guest',
    // Plaintext on purpose for the lab. Never do this in production.
    password: 'guest123',
    role: 'customer',
    displayName: 'Guest Wallet',
    walletId: 'OP-WALLET-1001',
    tier: 'standard',
    balanceBRL: 1240.55,
    kycLevel: 'basic',
  },
];

// Multiple receipts already modeled so future phases can explore object-level
// authorization. Phase 1 only exposes receipt 1001 (the guest's own receipt).
const receipts = [
  {
    receiptId: 1001,
    ownerUserId: 1001,
    type: 'transfer',
    status: 'settled',
    amountBRL: 89.9,
    currency: 'BRL',
    counterparty: 'Cafe Obsidian Ltda',
    createdAt: '2026-05-30T14:21:00Z',
    reference: 'OP-RCPT-1001',
  },
  {
    receiptId: 1002,
    ownerUserId: 2002,
    type: 'transfer',
    status: 'settled',
    amountBRL: 4200.0,
    currency: 'BRL',
    counterparty: 'Private Account',
    createdAt: '2026-05-31T09:05:00Z',
    reference: 'OP-RCPT-1002',
  },
  {
    receiptId: 1003,
    ownerUserId: 2002,
    type: 'topup',
    status: 'pending',
    amountBRL: 15000.0,
    currency: 'BRL',
    counterparty: 'External Bank',
    createdAt: '2026-06-01T18:42:00Z',
    reference: 'OP-RCPT-1003',
  },
];

// Simulated mobile configuration delivered to the app at startup.
const mobileConfig = Object.freeze({
  apiVersion: 'v1',
  minSupportedAppVersion: '1.0.0',
  supportSyncMode: 'legacy-http',
  // Intentionally references a legacy/plaintext-friendly path. Used in a later
  // phase to study cleartext/legacy traffic. Local only.
  legacySupportEndpoint: 'http://127.0.0.1:8102/api/mobile/support/sync',
  mobileFeatureFlags: {
    biometricLogin: false,
    qrPayments: true,
    webviewSupportCenter: true,
    deepLinkRouting: true,
    rememberDeviceToken: true,
  },
  warning:
    'Local lab environment only. Do not deploy or expose this configuration outside 127.0.0.1.',
});

module.exports = { LAB, users, receipts, mobileConfig };
