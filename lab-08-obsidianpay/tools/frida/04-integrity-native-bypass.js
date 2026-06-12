/**
 * 04-integrity-native-bypass.js
 * [ObsidianPay Lab] — Fase 13: scaffold de instrumentação dinâmica
 *
 * Alvo: com.obsidianpay.mobile
 * target packages: com.obsidianpay.mobile.integrity
 *
 * Objetivo didático:
 *   Observar e hookar os checks de integridade do app (NativeGate e TamperCheck).
 *   Demonstra os hint IDs:
 *     - jni-return-value-hook
 *     - patch-native-gate-result
 *     - hook-package-manager
 *     - patch-debuggable-check
 *     - repackage-signature-mismatch
 *
 * Classes alvo:
 *   - NativeGate    (isNativeLibraryLoaded, getNativeGateStatus)
 *   - TamperCheck   (isDebuggable, getInstallerPackage, getPackageNameStatus, run)
 *
 * Uso:
 *   frida -U -f com.obsidianpay.mobile -l 04-integrity-native-bypass.js   (spawn mode)
 *   frida -U com.obsidianpay.mobile    -l 04-integrity-native-bypass.js   (attach mode)
 *
 * AVISO: Use somente no laboratório local autorizado. Não use contra apps reais.
 */

Java.perform(function () {
  var TAG = "[ObsidianPay Lab][integrity-bypass]";

  // -------------------------------------------------------------------------
  // hook NativeGate — isNativeLibraryLoaded
  // hint: jni-return-value-hook
  // -------------------------------------------------------------------------
  try {
    var NativeGate = Java.use("com.obsidianpay.mobile.integrity.NativeGate");

    if (NativeGate.isNativeLibraryLoaded) {
      NativeGate.isNativeLibraryLoaded.implementation = function () {
        var result = this.isNativeLibraryLoaded();
        console.log(TAG + " NativeGate.isNativeLibraryLoaded() => " + result);
        console.log(TAG + "   hint: jni-return-value-hook");
        return result;

        // Para bypass (descomente):
        // console.log(TAG + "   [BYPASS] forcando nativeLibraryLoaded=true");
        // return true;
      };
      console.log(TAG + " hook NativeGate.isNativeLibraryLoaded instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] NativeGate.isNativeLibraryLoaded nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook NativeGate — getNativeGateStatus
  // hint: jni-return-value-hook / patch-native-gate-result
  // -------------------------------------------------------------------------
  try {
    var NativeGate2 = Java.use("com.obsidianpay.mobile.integrity.NativeGate");

    if (NativeGate2.getNativeGateStatus) {
      NativeGate2.getNativeGateStatus.implementation = function () {
        var result = this.getNativeGateStatus();
        console.log(TAG + " NativeGate.getNativeGateStatus() chamado");
        try {
          console.log(TAG + "   statusLabel = " + result.getStatusLabel());
          console.log(TAG + "   bypassHintId = " + result.getBypassHintId());
        } catch (fieldErr) {
          console.log(TAG + "   (campos: " + result + ")");
        }
        console.log(TAG + "   hint: patch-native-gate-result");
        return result;

        // Para bypass (descomente e ajuste):
        // Construir NativeGateResult com status de "gate aberto" exige conhecer
        // o construtor da classe — use JADX para inspecionar os parâmetros.
      };
      console.log(TAG + " hook NativeGate.getNativeGateStatus instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] NativeGate.getNativeGateStatus nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook TamperCheck — isDebuggable
  // hint: patch-debuggable-check
  // -------------------------------------------------------------------------
  try {
    var TamperCheck = Java.use("com.obsidianpay.mobile.integrity.TamperCheck");

    if (TamperCheck.isDebuggable) {
      TamperCheck.isDebuggable.overload("android.content.Context").implementation = function (ctx) {
        var result = this.isDebuggable(ctx);
        console.log(TAG + " TamperCheck.isDebuggable() => " + result);
        console.log(TAG + "   hint: patch-debuggable-check");
        return result;

        // Para bypass (descomente):
        // console.log(TAG + "   [BYPASS] forcando isDebuggable=false");
        // return false;
      };
      console.log(TAG + " hook TamperCheck.isDebuggable instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] TamperCheck.isDebuggable nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook TamperCheck — getInstallerPackage
  // hint: hook-package-manager
  // -------------------------------------------------------------------------
  try {
    var TamperCheck2 = Java.use("com.obsidianpay.mobile.integrity.TamperCheck");

    if (TamperCheck2.getInstallerPackage) {
      TamperCheck2.getInstallerPackage.overload("android.content.Context").implementation = function (ctx) {
        var result = this.getInstallerPackage(ctx);
        console.log(TAG + " TamperCheck.getInstallerPackage() => '" + result + "'");
        console.log(TAG + "   hint: hook-package-manager");
        return result;

        // Para bypass (descomente):
        // console.log(TAG + "   [BYPASS] retornando instalador esperado");
        // return "com.android.vending";
      };
      console.log(TAG + " hook TamperCheck.getInstallerPackage instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] TamperCheck.getInstallerPackage nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // hook TamperCheck — getPackageNameStatus
  // hint: hook-package-manager / repackage-signature-mismatch
  // -------------------------------------------------------------------------
  try {
    var TamperCheck3 = Java.use("com.obsidianpay.mobile.integrity.TamperCheck");

    if (TamperCheck3.getPackageNameStatus) {
      TamperCheck3.getPackageNameStatus.overload("android.content.Context").implementation = function (ctx) {
        var result = this.getPackageNameStatus(ctx);
        console.log(TAG + " TamperCheck.getPackageNameStatus() => '" + result + "'");
        console.log(TAG + "   hint: repackage-signature-mismatch");
        return result;
      };
      console.log(TAG + " hook TamperCheck.getPackageNameStatus instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] TamperCheck.getPackageNameStatus nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar TamperCheck.run() — calcula tamperScore total
  // hint: patch-debuggable-check / hook-package-manager
  // -------------------------------------------------------------------------
  try {
    var TamperCheck4 = Java.use("com.obsidianpay.mobile.integrity.TamperCheck");

    if (TamperCheck4.run) {
      TamperCheck4.run.implementation = function () {
        var result = this.run();
        try {
          console.log(TAG + " TamperCheck.run() => tamperScore=" + result.getTamperScore());
        } catch (fieldErr) {
          console.log(TAG + " TamperCheck.run() => " + result);
        }
        return result;
      };
      console.log(TAG + " hook TamperCheck.run instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] TamperCheck.run nao disponivel: " + e.message);
  }

  console.log(TAG + " hint: jni-return-value-hook        -> NativeGate.isNativeLibraryLoaded / getNativeGateStatus");
  console.log(TAG + " hint: patch-native-gate-result     -> alternativa: apktool+smali em NativeGate");
  console.log(TAG + " hint: hook-package-manager         -> TamperCheck.getInstallerPackage");
  console.log(TAG + " hint: patch-debuggable-check       -> TamperCheck.isDebuggable");
  console.log(TAG + " hint: repackage-signature-mismatch -> TamperCheck.getPackageNameStatus / signatureHash");
  console.log(TAG + " --- Fase 13 scaffold carregado ---");
});
