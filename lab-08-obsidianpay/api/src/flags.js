'use strict';

/**
 * flags.js — Fase 14 (Final Challenge Chain)
 *
 * Registro central das 9 flags oficiais do Lab 08 (ObsidianPay Mobile).
 *
 * IMPORTANTE (nota de instrutor):
 * - Este arquivo é o ÚNICO lugar do backend onde os valores reais das flags
 *   ficam materializados (junto de data.js, que carrega marcadores de progresso
 *   das fases anteriores). Os valores NÃO aparecem em nenhum documento público
 *   (README / STUDENT-GUIDE / docs/* / android-app/README / tools/).
 * - O `challenge-chain.js` referencia apenas `flagKey` (id do estágio), nunca o
 *   valor da flag.
 * - A submissão de progresso (POST /api/mobile/challenge/submit) valida a flag
 *   enviada pelo aluno contra este registro via `isValidFlag(stageId, flag)`.
 * - Nada aqui é segredo de produção: são valores didáticos e controlados.
 */

// Ordem oficial da cadeia. A chave é o stageId usado pelo challenge-chain e
// pelos endpoints; o valor é a flag oficial daquele estágio.
const FLAGS = Object.freeze({
  'stage-01-recon': 'FLAG{obsidianpay_mobile_recon_01}',
  'stage-02-insecure-storage': 'FLAG{obsidianpay_insecure_storage_02}',
  'stage-03-exported-components': 'FLAG{obsidianpay_exported_components_03}',
  'stage-04-webview-bridge': 'FLAG{obsidianpay_webview_bridge_04}',
  'stage-05-device-trust': 'FLAG{obsidianpay_device_trust_05}',
  'stage-06-biometric-vault': 'FLAG{obsidianpay_biometric_vault_06}',
  'stage-07-network-pinning': 'FLAG{obsidianpay_network_pinning_07}',
  'stage-08-app-integrity': 'FLAG{obsidianpay_integrity_bypass_08}',
  'stage-09-final-operator-chain': 'FLAG{obsidianpay_final_operator_chain_09}',
});

// Flag da etapa final, também exposta nominalmente para o endpoint de finalize.
const FINAL_FLAG = FLAGS['stage-09-final-operator-chain'];

/**
 * Retorna a flag oficial de um estágio, ou null se o stageId for desconhecido.
 * @param {string} stageId
 * @returns {string|null}
 */
function getFlagByStageId(stageId) {
  if (typeof stageId !== 'string') return null;
  return Object.prototype.hasOwnProperty.call(FLAGS, stageId) ? FLAGS[stageId] : null;
}

/**
 * Retorna uma cópia do mapa completo stageId -> flag.
 * Uso interno/instrutor (ex.: scoreboard administrativo, validação). NÃO deve
 * ser serializado em respostas voltadas ao aluno.
 * @returns {Record<string,string>}
 */
function getAllFlags() {
  return { ...FLAGS };
}

/**
 * Compara, de forma robusta a espaços/caixa, a flag submetida com a oficial do
 * estágio. Retorna true apenas se o estágio existir e a flag bater.
 * @param {string} stageId
 * @param {string} submittedFlag
 * @returns {boolean}
 */
function isValidFlag(stageId, submittedFlag) {
  const expected = getFlagByStageId(stageId);
  if (!expected || typeof submittedFlag !== 'string') return false;
  return submittedFlag.trim() === expected;
}

module.exports = {
  FLAGS,
  FINAL_FLAG,
  getFlagByStageId,
  getAllFlags,
  isValidFlag,
};
