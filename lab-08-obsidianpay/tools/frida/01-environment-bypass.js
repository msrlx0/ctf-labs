/**
 * 01-environment-bypass.js
 * [ObsidianPay Lab] — Fase 13: scaffold de instrumentação dinâmica
 *
 * Alvo: com.obsidianpay.mobile
 * target package: com.obsidianpay.mobile.environment
 *
 * Objetivo didático:
 *   Observar e hookar os detectores de root/emulador do ObsidianPay.
 *   Demonstra os hint IDs:
 *     - hooks-change-return-values
 *     - patch-risk-engine-result
 *     - env-check-local-only
 *
 * Uso:
 *   frida -U -f com.obsidianpay.mobile -l 01-environment-bypass.js   (spawn mode)
 *   frida -U com.obsidianpay.mobile    -l 01-environment-bypass.js   (attach mode)
 *
 * AVISO: Use somente no laboratório local autorizado. Não use contra apps reais.
 */

Java.perform(function () {
  var TAG = "[ObsidianPay Lab][env-bypass]";

  // -------------------------------------------------------------------------
  // hook RootDetector.check
  // hint: hooks-change-return-values / env-check-local-only
  // -------------------------------------------------------------------------
  try {
    var RootDetector = Java.use("com.obsidianpay.mobile.environment.RootDetector");

    RootDetector.check.overload("android.content.Context").implementation = function (ctx) {
      var original = this.check(ctx);
      console.log(TAG + " RootDetector.check() chamado");
      console.log(TAG + "   isRooted (original) = " + original.getIsRooted());
      console.log(TAG + "   rootScore (original) = " + original.getRootScore());
      console.log(TAG + "   bypassHintId = hooks-change-return-values");

      // Para observar sem modificar: retorne o valor original
      return original;

      // Para bypass (descomente e comente o return acima):
      // var RootResult = Java.use("com.obsidianpay.mobile.environment.RootDetectionResult");
      // var bypassed = RootResult.$new(false, 0, Java.use("java.util.ArrayList").$new(), "env-check-local-only");
      // console.log(TAG + "   [BYPASS] retornando isRooted=false");
      // return bypassed;
    };
    console.log(TAG + " hook RootDetector.check instalado");
  } catch (e) {
    console.log(TAG + " [WARN] RootDetector.check nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook EmulatorDetector.check
  // hint: hooks-change-return-values / env-check-local-only
  // -------------------------------------------------------------------------
  try {
    var EmulatorDetector = Java.use("com.obsidianpay.mobile.environment.EmulatorDetector");

    EmulatorDetector.check.implementation = function () {
      var original = this.check();
      console.log(TAG + " EmulatorDetector.check() chamado");
      console.log(TAG + "   isEmulator (original) = " + original.getIsEmulator());
      console.log(TAG + "   emulatorScore (original) = " + original.getEmulatorScore());
      console.log(TAG + "   bypassHintId = hooks-change-return-values");

      // Para observar sem modificar: retorne o valor original
      return original;

      // Para bypass (descomente):
      // var EmulatorResult = Java.use("com.obsidianpay.mobile.environment.EmulatorDetectionResult");
      // var bypassed = EmulatorResult.$new(false, 0, Java.use("java.util.ArrayList").$new());
      // console.log(TAG + "   [BYPASS] retornando isEmulator=false");
      // return bypassed;
    };
    console.log(TAG + " hook EmulatorDetector.check instalado");
  } catch (e) {
    console.log(TAG + " [WARN] EmulatorDetector.check nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook EnvironmentRiskEngine.run
  // hint: patch-risk-engine-result
  // -------------------------------------------------------------------------
  try {
    var EnvironmentRiskEngine = Java.use("com.obsidianpay.mobile.environment.EnvironmentRiskEngine");

    // Tenta hookar método de instância 'run' ou 'evaluate' conforme disponível
    if (EnvironmentRiskEngine.run) {
      EnvironmentRiskEngine.run.overload("android.content.Context").implementation = function (ctx) {
        var original = this.run(ctx);
        console.log(TAG + " EnvironmentRiskEngine.run() chamado");
        console.log(TAG + "   riskLevel (original) = " + original.getRiskLevel());
        console.log(TAG + "   bypassHintId = patch-risk-engine-result");

        // Para observar sem modificar: retorne o valor original
        return original;

        // Para bypass do risk engine (descomente):
        // Retornar um EnvironmentRiskResult com riskLevel="low"
        // console.log(TAG + "   [BYPASS] forcando riskLevel=low");
      };
      console.log(TAG + " hook EnvironmentRiskEngine.run instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] EnvironmentRiskEngine.run nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar serialização JSON (toJson / toResultJson)
  // hint: patch-risk-engine-result — alternativa a binary patching
  // -------------------------------------------------------------------------
  try {
    var EnvironmentRiskEngine2 = Java.use("com.obsidianpay.mobile.environment.EnvironmentRiskEngine");
    if (EnvironmentRiskEngine2.toJson) {
      EnvironmentRiskEngine2.toJson.implementation = function () {
        var result = this.toJson();
        console.log(TAG + " EnvironmentRiskEngine.toJson() => " + result);
        return result;
      };
      console.log(TAG + " hook EnvironmentRiskEngine.toJson instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] EnvironmentRiskEngine.toJson nao disponivel: " + e.message);
  }

  console.log(TAG + " --- Fase 13 scaffold carregado ---");
  console.log(TAG + " hint: env-check-local-only - todos os checks correm no processo do app");
  console.log(TAG + " hint: hooks-change-return-values - modifique os returns acima para bypass");
  console.log(TAG + " hint: patch-risk-engine-result - alternativa: apktool+smali no EnvironmentRiskEngine");
});
