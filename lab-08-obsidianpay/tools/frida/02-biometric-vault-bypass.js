/**
 * 02-biometric-vault-bypass.js
 * [ObsidianPay Lab] — Fase 13: scaffold de instrumentação dinâmica
 *
 * Alvo: com.obsidianpay.mobile
 * target packages: com.obsidianpay.mobile.auth, com.obsidianpay.mobile.ui
 *
 * Objetivo didático:
 *   Observar e hookar o fluxo de autenticação local do Secure Vault.
 *   Demonstra os hint IDs:
 *     - biometric-result-hook
 *     - force-auth-decision-true
 *     - patch-local-auth-state
 *
 * Classes alvo:
 *   - LocalAuthState  (validateFallbackPin, isVaultUnlocked, markVaultUnlocked)
 *   - BiometricGate   (canUseBiometric, buildBypassHintId)
 *   - VaultScreen     (tela de UI — Compose, difícil de hookar diretamente)
 *
 * Uso:
 *   frida -U -f com.obsidianpay.mobile -l 02-biometric-vault-bypass.js   (spawn mode)
 *   frida -U com.obsidianpay.mobile    -l 02-biometric-vault-bypass.js   (attach mode)
 *
 * AVISO: Use somente no laboratório local autorizado. Não use contra apps reais.
 */

Java.perform(function () {
  var TAG = "[ObsidianPay Lab][biometric-bypass]";

  // -------------------------------------------------------------------------
  // hook LocalAuthState.validateFallbackPin
  // hint: force-auth-decision-true
  // -------------------------------------------------------------------------
  try {
    var LocalAuthState = Java.use("com.obsidianpay.mobile.auth.LocalAuthState");

    LocalAuthState.validateFallbackPin.overload("java.lang.String").implementation = function (pin) {
      var result = this.validateFallbackPin(pin);
      console.log(TAG + " LocalAuthState.validateFallbackPin() chamado");
      console.log(TAG + "   pin fornecido: '" + pin + "'");
      console.log(TAG + "   resultado original: " + result);
      console.log(TAG + "   hint: force-auth-decision-true");

      // Para observar sem modificar: retorne o valor original
      return result;

      // Para bypass (descomente):
      // console.log(TAG + "   [BYPASS] forcando validacao true independente do PIN");
      // return true;
    };
    console.log(TAG + " hook LocalAuthState.validateFallbackPin instalado");
  } catch (e) {
    console.log(TAG + " [WARN] LocalAuthState.validateFallbackPin nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook LocalAuthState.isVaultUnlocked
  // hint: patch-local-auth-state
  // -------------------------------------------------------------------------
  try {
    var LocalAuthState2 = Java.use("com.obsidianpay.mobile.auth.LocalAuthState");

    LocalAuthState2.isVaultUnlocked.overload("android.content.SharedPreferences").implementation = function (prefs) {
      var result = this.isVaultUnlocked(prefs);
      console.log(TAG + " LocalAuthState.isVaultUnlocked() => " + result);
      console.log(TAG + "   hint: patch-local-auth-state - estado em SharedPreferences em claro");

      // Para observar sem modificar: retorne o valor original
      return result;

      // Para bypass (descomente):
      // console.log(TAG + "   [BYPASS] vault sempre desbloqueado");
      // return true;
    };
    console.log(TAG + " hook LocalAuthState.isVaultUnlocked instalado");
  } catch (e) {
    console.log(TAG + " [WARN] LocalAuthState.isVaultUnlocked nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook LocalAuthState.markVaultUnlocked
  // hint: patch-local-auth-state
  // -------------------------------------------------------------------------
  try {
    var LocalAuthState3 = Java.use("com.obsidianpay.mobile.auth.LocalAuthState");

    if (LocalAuthState3.markVaultUnlocked) {
      LocalAuthState3.markVaultUnlocked.implementation = function () {
        var args = Array.prototype.slice.call(arguments);
        console.log(TAG + " LocalAuthState.markVaultUnlocked() chamado com args: " + JSON.stringify(args.map(String)));
        return this.markVaultUnlocked.apply(this, args);
      };
      console.log(TAG + " hook LocalAuthState.markVaultUnlocked instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] LocalAuthState.markVaultUnlocked nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook BiometricGate.canUseBiometric
  // hint: biometric-result-hook
  // -------------------------------------------------------------------------
  try {
    var BiometricGate = Java.use("com.obsidianpay.mobile.auth.BiometricGate");

    BiometricGate.canUseBiometric.overload("android.content.Context").implementation = function (ctx) {
      var result = this.canUseBiometric(ctx);
      console.log(TAG + " BiometricGate.canUseBiometric() => " + result);
      console.log(TAG + "   hint: biometric-result-hook");

      // Para observar sem modificar: retorne o valor original
      return result;

      // Para bypass (descomente):
      // console.log(TAG + "   [BYPASS] biometric sempre disponivel");
      // return true;
    };
    console.log(TAG + " hook BiometricGate.canUseBiometric instalado");
  } catch (e) {
    console.log(TAG + " [WARN] BiometricGate.canUseBiometric nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar buildBypassHintId (BiometricGate)
  // -------------------------------------------------------------------------
  try {
    var BiometricGate2 = Java.use("com.obsidianpay.mobile.auth.BiometricGate");

    if (BiometricGate2.buildBypassHintId) {
      BiometricGate2.buildBypassHintId.implementation = function () {
        var hint = this.buildBypassHintId();
        console.log(TAG + " BiometricGate.buildBypassHintId() => " + hint);
        return hint;
      };
      console.log(TAG + " hook BiometricGate.buildBypassHintId instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] BiometricGate.buildBypassHintId nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Nota sobre VaultScreen
  // VaultScreen é uma tela Compose — hookar diretamente é complexo.
  // O ponto mais efetivo é LocalAuthState.validateFallbackPin (acima) ou
  // o estado em SharedPreferences (patch-local-auth-state).
  // -------------------------------------------------------------------------
  console.log(TAG + " Nota: VaultScreen (Compose) — hook via LocalAuthState ou SharedPreferences");
  console.log(TAG + " hint: biometric-result-hook     -> BiometricGate.canUseBiometric");
  console.log(TAG + " hint: force-auth-decision-true  -> LocalAuthState.validateFallbackPin");
  console.log(TAG + " hint: patch-local-auth-state    -> LocalAuthState.isVaultUnlocked / SharedPreferences");
  console.log(TAG + " --- Fase 13 scaffold carregado ---");
});
