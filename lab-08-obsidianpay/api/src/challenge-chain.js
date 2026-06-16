'use strict';

/**
 * challenge-chain.js — Fase 14 (Final Challenge Chain)
 *
 * Define a cadeia oficial de CTF do Lab 08 (ObsidianPay Mobile): metadados de
 * cada estágio, pontuação, dificuldade, dicas em dois níveis, evidência
 * esperada e notas de instrutor.
 *
 * IMPORTANTE (nota de instrutor):
 * - Este arquivo NÃO contém o valor das flags. Cada estágio referencia apenas
 *   `flagKey` (= stageId), e o valor real vive em `flags.js`.
 * - `publicSummary` é texto seguro para o aluno (sem solução completa).
 * - `instructorNote` é orientação curta de instrutor (sem flag).
 */

const chainId = 'obsidianpay-mobile-final-chain';
const totalFlags = 9;

const stages = Object.freeze([
  {
    id: 'stage-01-recon',
    title: 'Mobile Recon & Config Review',
    category: 'recon',
    points: 100,
    difficulty: 'easy',
    flagKey: 'stage-01-recon',
    hintLevel1:
      'Comece pelo contrato mobile público. Nem todo endpoint de configuração trata um header de revisão da mesma forma.',
    hintLevel2:
      'Repita GET /api/mobile/config enviando o header X-Obsidian-Recon com o valor de revisão de configuração mobile.',
    evidenceExpected:
      'Resposta de /api/mobile/config contendo o bloco reconCheckpoint quando o header de recon é enviado.',
    publicSummary:
      'Reconhecimento do app: mapear o contrato mobile, chaves de armazenamento e rotas internas divulgadas.',
    instructorNote:
      'Checkpoint disparado por GET /api/mobile/config + header X-Obsidian-Recon: mobile-config-review.',
  },
  {
    id: 'stage-02-insecure-storage',
    title: 'Insecure Local Storage',
    category: 'storage',
    points: 150,
    difficulty: 'easy',
    flagKey: 'stage-02-insecure-storage',
    hintLevel1:
      'O app sincroniza um cache local legado. O backend ecoa o que o cliente afirma sobre esse cache.',
    hintLevel2:
      'POST /api/mobile/support/sync com um campo cacheCheckpoint indicando revisão de armazenamento local.',
    evidenceExpected:
      'Resposta de /api/mobile/support/sync contendo localStorageCheckpoint.',
    publicSummary:
      'Armazenamento local inseguro: SharedPreferences/SQLite/cache offline expõem material de sessão.',
    instructorNote:
      'Checkpoint disparado por POST /api/mobile/support/sync com cacheCheckpoint: local-storage-review.',
  },
  {
    id: 'stage-03-exported-components',
    title: 'Exported Android Components',
    category: 'android-components',
    points: 200,
    difficulty: 'medium',
    flagKey: 'stage-03-exported-components',
    hintLevel1:
      'Componentes Android exportados (Activity/Receiver/ContentProvider) são acessíveis por outros apps e por adb.',
    hintLevel2:
      'Consulte o ContentProvider exportado (authority com.obsidianpay.mobile.provider.notes) via adb shell content query e correlacione com a cadeia documentada.',
    evidenceExpected:
      'Saída de adb (content query / am broadcast / am start) demonstrando o componente exportado e o marcador associado.',
    publicSummary:
      'Componentes exportados: enumerar Activity/Receiver/ContentProvider e abusar de actions/authority/extras previsíveis.',
    instructorNote:
      'Estágio orientado ao Android (Fase 7). A flag é validada no submit; a evidência vem do device/adb. Sem alteração de backend obrigatória.',
  },
  {
    id: 'stage-04-webview-bridge',
    title: 'WebView JavaScript Bridge',
    category: 'webview',
    points: 200,
    difficulty: 'medium',
    flagKey: 'stage-04-webview-bridge',
    hintLevel1:
      'O portal de suporte em WebView fala com uma bridge @JavascriptInterface. O backend tem um modo de auditoria da bridge.',
    hintLevel2:
      'GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review retorna o bloco bridgeCheckpoint.',
    evidenceExpected:
      'HTML/JSON de /api/mobile/webview/support contendo bridgeCheckpoint para o tópico de auditoria da bridge.',
    publicSummary:
      'Bridge WebView: abuso de @JavascriptInterface para ler sessão/caches locais a partir do portal de suporte.',
    instructorNote:
      'Checkpoint disparado por GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review.',
  },
  {
    id: 'stage-05-device-trust',
    title: 'Legacy Device Trust Bypass',
    category: 'reverse-engineering',
    points: 250,
    difficulty: 'medium',
    flagKey: 'stage-05-device-trust',
    hintLevel1:
      'O fluxo legado de device-trust aceita uma assinatura SHA-1 fraca calculada com um salt embutido no app.',
    hintLevel2:
      'Recupere o salt fragmentado, forje a assinatura sha1(username:deviceId:timestamp:salt) e chame POST /api/mobile/internal/device-trust.',
    evidenceExpected:
      'Resposta trusted-legacy de /api/mobile/internal/device-trust contendo deviceTrustCheckpoint.',
    publicSummary:
      'Device trust legado: recuperar segredos hardcoded e forjar a assinatura local fraca aceita pelo backend.',
    instructorNote:
      'Checkpoint incluído quando a assinatura legacy é aceita em POST /api/mobile/internal/device-trust.',
  },
  {
    id: 'stage-06-biometric-vault',
    title: 'Biometric Vault Bypass',
    category: 'local-auth',
    points: 250,
    difficulty: 'hard',
    flagKey: 'stage-06-biometric-vault',
    hintLevel1:
      'O vault confia na asserção de autenticação local do cliente. O servidor não verifica biometria de forma independente.',
    hintLevel2:
      'POST /api/mobile/internal/vault-mobile/unlock afirmando localAuth=true com decisão coerente (vaultUnlocked/authDecision).',
    evidenceExpected:
      'Resposta vault-access-granted de /api/mobile/internal/vault-mobile/unlock contendo vaultCheckpoint.',
    publicSummary:
      'Vault biométrico: bypass da autenticação local (hook/patch) que o servidor aceita como verdade.',
    instructorNote:
      'Checkpoint incluído quando localAuth=true e a decisão de auth do cliente é coerente.',
  },
  {
    id: 'stage-07-network-pinning',
    title: 'Network Pinning Review',
    category: 'network',
    points: 250,
    difficulty: 'hard',
    flagKey: 'stage-07-network-pinning',
    hintLevel1:
      'O perfil de rede descreve a postura de pinning (report-only) e os hint IDs de bypass. Há um modo de revisão Burp/pinning.',
    hintLevel2:
      'GET /api/mobile/internal/network-profile com header X-Obsidian-Network-Review de checagem de pinning via Burp.',
    evidenceExpected:
      'Resposta de /api/mobile/internal/network-profile contendo networkCheckpoint.',
    publicSummary:
      'Pinning de rede: configurar proxy/CA, observar pinning e usar hints de bypass (Frida/network config).',
    instructorNote:
      'Checkpoint disparado por GET /api/mobile/internal/network-profile + header X-Obsidian-Network-Review: burp-pinning-check.',
  },
  {
    id: 'stage-08-app-integrity',
    title: 'App Integrity / NativeGate Bypass',
    category: 'integrity',
    points: 300,
    difficulty: 'hard',
    flagKey: 'stage-08-app-integrity',
    hintLevel1:
      'A integridade é report-only: o servidor confia no relatório do cliente, que é totalmente patchável.',
    hintLevel2:
      'POST /api/mobile/internal/app-integrity reportando um bypassHintId de NativeGate (jni-return-value-hook ou patch-native-gate-result).',
    evidenceExpected:
      'Resposta de /api/mobile/internal/app-integrity contendo integrityCheckpoint.',
    publicSummary:
      'Integridade do app: bypass de NativeGate/TamperCheck via hook/patch, demonstrando confiança indevida do servidor.',
    instructorNote:
      'Checkpoint incluído quando o relatório indica jni-return-value-hook ou patch-native-gate-result.',
  },
  {
    id: 'stage-09-final-operator-chain',
    title: 'Final Operator Chain',
    category: 'chain',
    points: 400,
    difficulty: 'insane',
    flagKey: 'stage-09-final-operator-chain',
    hintLevel1:
      'A etapa final exige provar que todas as trilhas internas foram dominadas, com um device marcado como confiável.',
    hintLevel2:
      'POST /api/mobile/internal/finalize-operator com header X-Obsidian-Device-Trust: trusted-legacy e as quatro provas (deviceTrustProof, vaultProof, integrityProof, networkProof).',
    evidenceExpected:
      'Resposta de /api/mobile/internal/finalize-operator entregando a flag final da cadeia.',
    publicSummary:
      'Cadeia final do operador: combinar device-trust, vault, integridade e rede para destravar a flag final.',
    instructorNote:
      'Endpoint POST /api/mobile/internal/finalize-operator. Exige header de device-trust + 4 provas no body. Não vaza flag se faltar prova/header.',
  },
]);

/**
 * Versão pública (sem flagKey/instructorNote) de um estágio, segura para o aluno.
 * @param {object} stage
 */
function toPublicStage(stage) {
  return {
    id: stage.id,
    title: stage.title,
    category: stage.category,
    points: stage.points,
    difficulty: stage.difficulty,
    publicSummary: stage.publicSummary,
    evidenceExpected: stage.evidenceExpected,
  };
}

/**
 * Retorna um estágio pelo id, ou undefined.
 * @param {string} stageId
 */
function getStage(stageId) {
  return stages.find((s) => s.id === stageId);
}

const totalPoints = stages.reduce((sum, s) => sum + s.points, 0);

module.exports = {
  chainId,
  totalFlags,
  totalPoints,
  stages,
  getStage,
  toPublicStage,
};
