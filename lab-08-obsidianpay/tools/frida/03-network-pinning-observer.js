/**
 * 03-network-pinning-observer.js
 * [ObsidianPay Lab] — Fase 13: scaffold de instrumentação dinâmica
 *
 * Alvo: com.obsidianpay.mobile
 * target packages: com.obsidianpay.mobile.network, okhttp3
 *
 * Objetivo didático:
 *   Observar o scaffold de pinning e os pontos de hook do OkHttp/TrustManager.
 *   NÃO implementa bypass universal de pinning — aponta os checkpoints corretos.
 *   Demonstra os hint IDs:
 *     - okhttp-certificate-pinner-hook
 *     - trust-user-ca
 *     - network-config-cleartext-override
 *
 * Classes alvo:
 *   - NetworkSecurityProfile  (perfis de rede do lab)
 *   - PinningPolicy           (shouldAttachCertificatePinner, currentMode)
 *   - CertificatePinner       (ponto de hook OkHttp — checkpoint para bypass futuro)
 *
 * Uso:
 *   frida -U -f com.obsidianpay.mobile -l 03-network-pinning-observer.js
 *
 * AVISO: Use somente no laboratório local autorizado. Não use contra apps reais.
 */

Java.perform(function () {
  var TAG = "[ObsidianPay Lab][pinning-observer]";

  // -------------------------------------------------------------------------
  // Observar PinningPolicy.shouldAttachCertificatePinner
  // hint: okhttp-certificate-pinner-hook
  // -------------------------------------------------------------------------
  try {
    var PinningPolicy = Java.use("com.obsidianpay.mobile.network.PinningPolicy");

    if (PinningPolicy.shouldAttachCertificatePinner) {
      PinningPolicy.shouldAttachCertificatePinner.implementation = function () {
        var result = this.shouldAttachCertificatePinner();
        console.log(TAG + " PinningPolicy.shouldAttachCertificatePinner() => " + result);
        console.log(TAG + "   hint: okhttp-certificate-pinner-hook");
        console.log(TAG + "   Se true, CertificatePinner seria attachado ao OkHttpClient");
        return result;
      };
      console.log(TAG + " hook PinningPolicy.shouldAttachCertificatePinner instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] PinningPolicy.shouldAttachCertificatePinner nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar PinningPolicy.currentMode / getPinningMode
  // -------------------------------------------------------------------------
  try {
    var PinningPolicy2 = Java.use("com.obsidianpay.mobile.network.PinningPolicy");

    // Tenta hookar getter do modo de pinning
    var methodNames = ["getCurrentMode", "getPinningMode", "currentMode"];
    methodNames.forEach(function (name) {
      try {
        if (PinningPolicy2[name]) {
          PinningPolicy2[name].implementation = function () {
            var mode = this[name]();
            console.log(TAG + " PinningPolicy." + name + "() => " + mode);
            return mode;
          };
          console.log(TAG + " hook PinningPolicy." + name + " instalado");
        }
      } catch (inner) { /* método inexistente */ }
    });
  } catch (e) {
    console.log(TAG + " [WARN] PinningPolicy getter nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar NetworkSecurityProfile
  // hint: network-config-cleartext-override / trust-user-ca
  // -------------------------------------------------------------------------
  try {
    var NetworkSecurityProfile = Java.use("com.obsidianpay.mobile.network.NetworkSecurityProfile");

    if (NetworkSecurityProfile.buildProfile) {
      NetworkSecurityProfile.buildProfile.implementation = function () {
        var profile = this.buildProfile();
        console.log(TAG + " NetworkSecurityProfile.buildProfile() => " + profile);
        console.log(TAG + "   hint: network-config-cleartext-override");
        console.log(TAG + "   hint: trust-user-ca");
        return profile;
      };
      console.log(TAG + " hook NetworkSecurityProfile.buildProfile instalado");
    }
  } catch (e) {
    console.log(TAG + " [WARN] NetworkSecurityProfile.buildProfile nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Checkpoint: CertificatePinner.check (OkHttp)
  // hint: okhttp-certificate-pinner-hook
  //
  // NOTA: Este é o ponto de bypass de pinning real no OkHttp.
  // Quando o pinning estiver ativo, hookar este método para não lançar exceção
  // efetivamente bypassa o pinning.
  //
  // Neste lab, o pinning está em modo 'report-only' (HTTP local), então
  // CertificatePinner.check não é chamado normalmente.
  // Este bloco serve como checkpoint didático para quando o pinning for ativado.
  // -------------------------------------------------------------------------
  try {
    var CertificatePinner = Java.use("okhttp3.CertificatePinner");

    CertificatePinner.check.overload("java.lang.String", "java.util.List").implementation = function (hostname, peerCertificates) {
      console.log(TAG + " CertificatePinner.check() chamado para hostname: " + hostname);
      console.log(TAG + "   hint: okhttp-certificate-pinner-hook");
      console.log(TAG + "   Observando sem modificar (pinning em report-only no lab)");

      // Para bypass real (quando pinning estiver ativo), descomente:
      // console.log(TAG + "   [BYPASS] pulando verificacao de pin para " + hostname);
      // return;  // não lança SSLPeerUnverifiedException

      return this.check(hostname, peerCertificates);
    };
    console.log(TAG + " hook CertificatePinner.check instalado (checkpoint)");
  } catch (e) {
    console.log(TAG + " [WARN] CertificatePinner.check nao disponivel: " + e.message);
    console.log(TAG + "   (OkHttp pode nao estar no classpath ou CertificatePinner inativo)");
  }

  console.log(TAG + " hint: okhttp-certificate-pinner-hook  -> CertificatePinner.check (OkHttp)");
  console.log(TAG + " hint: trust-user-ca                   -> user CA + network_security_config");
  console.log(TAG + " hint: network-config-cleartext-override -> cleartext liberado para hosts locais");
  console.log(TAG + " --- Fase 13 scaffold carregado ---");
});
