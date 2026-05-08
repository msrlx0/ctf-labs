const flags = {
  blindSqli: 'FLAG{blind_sqli_extracted_admin}',
  jwt: 'FLAG{jwt_forged_neon_admin}',
  ssrf: 'FLAG{ssrf_internal_neon_service}',
  ssti: 'FLAG{ssti_template_breach}',
  upload: 'FLAG{upload_filter_bypass}',
  logs: 'FLAG{logs_filter_sqli}',
  idor: 'FLAG{api_idor_object_leak}',
  traversal: 'FLAG{traversal_follow_the_logs}'
};

const users = [
  {
    id: 1,
    username: 'admin',
    password: 'disabled-admin-login',
    displayName: 'NeonCore Root Operator',
    role: 'admin',
    recoveryCode: 'N3ON',
    email: 'root@neoncore.local',
    department: 'Core Identity'
  },
  {
    id: 2,
    username: 'nova',
    password: 'nova2099',
    displayName: 'Nova Tanaka',
    role: 'user',
    recoveryCode: 'P1NK',
    email: 'nova@neoncore.local',
    department: 'Threat Desk'
  },
  {
    id: 3,
    username: 'kai',
    password: 'kai-terminal',
    displayName: 'Kai Almeida',
    role: 'user',
    recoveryCode: 'BLUE',
    email: 'kai@neoncore.local',
    department: 'Identity Ops'
  }
];

const logs = [
  { id: 1, level: 'info', source: 'auth-gateway', message: 'Nova session established from neon terminal.' },
  { id: 2, level: 'error', source: 'webhook-tester', message: 'Webhook target timeout on remote integration.' },
  { id: 3, level: 'warn', source: 'avatar-service', message: 'Legacy extension validator still enabled for migration.' },
  { id: 4, level: 'error', source: 'template-preview', message: 'Preview renderer returned expression output.' },
  {
    id: 900,
    level: 'hidden',
    source: 'log-filter',
    message: 'Hidden operator log unlocked through filter injection. FLAG{logs_filter_sqli}'
  }
];

const tickets = [
  {
    id: 101,
    ownerId: 2,
    title: 'Avatar sync failed',
    status: 'open',
    body: 'Nova needs a new badge render for the night shift console.'
  },
  {
    id: 102,
    ownerId: 2,
    title: 'Webhook staging check',
    status: 'pending',
    body: 'Confirm that the webhook tester only reaches approved partner URLs.'
  },
  {
    id: 201,
    ownerId: 3,
    title: 'Identity audit mismatch',
    status: 'open',
    body: 'Kai can see stale ticket IDs in the mobile console.'
  },
  {
    id: 777,
    ownerId: 1,
    title: 'Core admin object leak',
    status: 'restricted',
    body: 'Internal ticket contains administrative evidence: FLAG{api_idor_object_leak}'
  }
];

module.exports = {
  flags,
  users,
  logs,
  tickets
};
