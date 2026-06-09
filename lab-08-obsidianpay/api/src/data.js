'use strict';

/**
 * In-memory seed data for the ObsidianPay Mobile API (Phase 2).
 *
 * IMPORTANT (instructor note):
 * - Nothing here is a production secret. These are didactic, controlled values.
 * - Internal progress markers (FLAG{...}) live ONLY in this backend data and are
 *   reachable only through the intentionally-vulnerable endpoints (IDOR, mass
 *   assignment, weak debug gate, role gate). They are NOT present in any public
 *   document (README / STUDENT-GUIDE / docs).
 * - Credentials for `analyst` and `operator` are intentionally NOT documented
 *   publicly; they exist here to be discovered through future mobile/RE flows.
 * - `users` is intentionally MUTABLE (let on fields) so the mass-assignment
 *   endpoint can mutate it in memory.
 */

const LAB = Object.freeze({
  name: 'ObsidianPay Mobile',
  lab: 'lab-08-obsidianpay',
  version: '0.2.0-phase2',
  port: 8102,
  phase: 2,
});

// --- Accounts ----------------------------------------------------------------
// Only `guest` / `guest123` is documented for students. analyst/operator are
// seeded here to be discovered later through the lab's flows.
const users = [
  {
    id: 1001,
    username: 'guest',
    password: 'guest123',
    role: 'customer',
    plan: 'basic',
    displayName: 'Guest Wallet',
    phone: '+55 11 90000-0001',
    walletId: 'OP-WALLET-1001',
    dailyLimit: 500,
    kycApproved: false,
    supportTier: 'standard',
    balanceBRL: 1240.55,
  },
  {
    id: 2001,
    username: 'analyst',
    password: 'analyst123',
    role: 'analyst',
    plan: 'internal',
    displayName: 'Internal Analyst',
    phone: '+55 11 90000-2001',
    walletId: 'OP-WALLET-2001',
    dailyLimit: 5000,
    kycApproved: true,
    supportTier: 'internal',
    balanceBRL: 18400.0,
  },
  {
    id: 3001,
    username: 'operator',
    password: 'operator123',
    role: 'operator',
    plan: 'privileged',
    displayName: 'Operations',
    phone: '+55 11 90000-3001',
    walletId: 'OP-WALLET-3001',
    dailyLimit: 25000,
    kycApproved: true,
    supportTier: 'priority',
    balanceBRL: 402000.0,
  },
];

// --- Receipts ----------------------------------------------------------------
// receiptId 9001 is an internal/legacy "support export" with sensitive metadata.
// metadata.internalNote carries progress markers reachable only via the IDOR
// endpoint GET /api/mobile/receipts/:receiptId.
const receipts = [
  {
    receiptId: 1001,
    ownerUserId: 1001,
    ownerRole: 'customer',
    type: 'transfer',
    status: 'settled',
    amountBRL: 89.9,
    currency: 'BRL',
    counterparty: 'Cafe Obsidian Ltda',
    createdAt: '2026-05-30T14:21:00Z',
    reference: 'OP-RCPT-1001',
    metadata: {
      channel: 'mobile',
      internalNote: 'Standard customer receipt. Nothing sensitive here.',
    },
  },
  {
    receiptId: 1002,
    ownerUserId: 2001,
    ownerRole: 'analyst',
    type: 'transfer',
    status: 'settled',
    amountBRL: 4200.0,
    currency: 'BRL',
    counterparty: 'Internal Settlement',
    createdAt: '2026-05-31T09:05:00Z',
    reference: 'OP-RCPT-1002',
    metadata: {
      channel: 'internal',
      internalNote:
        'Analyst settlement export. Cross-account visibility should be restricted.',
    },
  },
  {
    receiptId: 1003,
    ownerUserId: 3001,
    ownerRole: 'operator',
    type: 'topup',
    status: 'pending',
    amountBRL: 15000.0,
    currency: 'BRL',
    counterparty: 'External Bank',
    createdAt: '2026-06-01T18:42:00Z',
    reference: 'OP-RCPT-1003',
    metadata: {
      channel: 'internal',
      internalNote: 'Operator top-up. FLAG{obsidian_idor_operator_receipt}',
    },
  },
  {
    receiptId: 9001,
    ownerUserId: 3001,
    ownerRole: 'operator',
    type: 'legacy-support-export',
    status: 'archived',
    amountBRL: 0.0,
    currency: 'BRL',
    counterparty: 'Legacy Support Pipeline',
    createdAt: '2026-02-11T03:14:00Z',
    reference: 'OP-RCPT-9001-LEGACY',
    metadata: {
      channel: 'legacy',
      migratedFrom: 'support-sync-v0',
      internalNote:
        'Legacy support export. Migrated session material. FLAG{obsidian_legacy_receipt_9001}',
    },
  },
];

// --- Cards -------------------------------------------------------------------
// `number` is the full PAN held server-side; it must be masked on output.
// `internalReference` is intentionally leaked through the IDOR card endpoint.
const cards = [
  {
    cardId: 'card-guest-01',
    ownerUserId: 1001,
    ownerRole: 'customer',
    brand: 'ObsidianCard',
    number: '4111111111110001',
    expiry: '12/29',
    holder: 'GUEST WALLET',
    internalReference: 'OP-CARD-REF-1001',
  },
  {
    cardId: 'card-analyst-01',
    ownerUserId: 2001,
    ownerRole: 'analyst',
    brand: 'ObsidianCard',
    number: '4111111111112001',
    expiry: '08/28',
    holder: 'INTERNAL ANALYST',
    internalReference: 'OP-CARD-REF-2001',
  },
  {
    cardId: 'card-operator-01',
    ownerUserId: 3001,
    ownerRole: 'operator',
    brand: 'ObsidianCard',
    number: '4111111111113001',
    expiry: '03/30',
    holder: 'OPERATIONS',
    internalReference: 'OP-CARD-REF-3001',
  },
];

// --- Feature flags -----------------------------------------------------------
const featureFlags = {
  enableLegacySupportSync: true,
  enableReceiptOfflineCache: true,
  enableQrTransferPreview: true,
  enableWebViewSupportPortal: true,
  enableBiometricVault: false,
  enableNativePinningExperiment: false,
};

// --- Vault status by role ----------------------------------------------------
// Used by GET /api/mobile/internal/vault-status. customer is denied (403) at the
// route; analyst/operator receive different statuses. The operator status holds
// a progress marker for future biometric/root/binary-patching chains.
const vaultStatusByRole = {
  analyst: {
    role: 'analyst',
    vault: 'restricted',
    biometricVault: false,
    nativePinning: false,
    note: 'Analyst access: read-only vault telemetry.',
  },
  operator: {
    role: 'operator',
    vault: 'unlocked',
    biometricVault: true,
    nativePinning: true,
    note: 'Operator vault unlocked. internalRef: FLAG{obsidian_vault_operator_status}',
  },
};

// --- Mobile config -----------------------------------------------------------
// Leaks internal resource NAMES (storage keys, deep link schemes, routes) that
// help map the future APK, but never returns a flag directly.
function buildMobileConfig() {
  return {
    apiVersion: 'v1',
    minSupportedAppVersion: '1.0.0',
    baseUrlHint: 'http://127.0.0.1:8102',
    supportSyncMode: 'legacy-http',
    legacySupportEndpoint: 'http://127.0.0.1:8102/api/mobile/support/sync',
    receiptCacheStrategy: 'offline-first',
    qrTransferScheme: 'obsidianpay://transfer',
    supportDeepLinkScheme: 'obsidianpay://support',
    webViewSupportPath: '/api/mobile/webview/support',
    mobileFeatureFlags: { ...featureFlags },
    clientStorageKeys: {
      sessionToken: 'obsidian.session.token',
      profileCache: 'obsidian.profile.cache',
      receiptsOffline: 'obsidian.receipts.offline',
      debugLastSync: 'obsidian.debug.last_sync',
    },
    warning:
      'Local lab environment only. Do not deploy or expose this configuration outside 127.0.0.1.',
  };
}

module.exports = {
  LAB,
  users,
  receipts,
  cards,
  featureFlags,
  vaultStatusByRole,
  buildMobileConfig,
};
