/**
 * 05-webview-bridge-observer.js
 * [ObsidianPay Lab] — Fase 13: scaffold de instrumentação dinâmica
 *
 * Alvo: com.obsidianpay.mobile
 * target packages: com.obsidianpay.mobile.webview, android.webkit
 *
 * Objetivo didático:
 *   Observar a WebView addJavascriptInterface e os métodos da ObsidianSupportBridge.
 *   Registra chamadas sem modificar o comportamento por padrão.
 *
 * Classes alvo:
 *   - ObsidianSupportBridge  (getSessionSummary, getCachedConfig, logBridgeEvent, etc.)
 *   - WebView                (addJavascriptInterface — ponto de observação)
 *   - ObsidianBridge         (nome exposto ao JavaScript: window.ObsidianBridge)
 *
 * Uso:
 *   frida -U -f com.obsidianpay.mobile -l 05-webview-bridge-observer.js   (spawn mode)
 *   frida -U com.obsidianpay.mobile    -l 05-webview-bridge-observer.js   (attach mode)
 *
 * AVISO: Use somente no laboratório local autorizado. Não use contra apps reais.
 */

Java.perform(function () {
  var TAG = "[ObsidianPay Lab][webview-observer]";

  // -------------------------------------------------------------------------
  // Observar WebView.addJavascriptInterface
  // Registra quando e com que nome a bridge é anexada
  // -------------------------------------------------------------------------
  try {
    var WebView = Java.use("android.webkit.WebView");

    WebView.addJavascriptInterface.overload("java.lang.Object", "java.lang.String").implementation = function (obj, name) {
      console.log(TAG + " WebView.addJavascriptInterface() chamado");
      console.log(TAG + "   interfaceName: '" + name + "'");
      console.log(TAG + "   objectClass:   " + obj.getClass().getName());
      if (name === "ObsidianBridge") {
        console.log(TAG + "   -> ObsidianBridge detectado (window.ObsidianBridge disponivel no JS)");
      }
      return this.addJavascriptInterface(obj, name);
    };
    console.log(TAG + " hook WebView.addJavascriptInterface instalado");
  } catch (e) {
    console.log(TAG + " [WARN] WebView.addJavascriptInterface nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar ObsidianSupportBridge.getSessionSummary
  // -------------------------------------------------------------------------
  try {
    var ObsidianSupportBridge = Java.use("com.obsidianpay.mobile.webview.ObsidianSupportBridge");

    ObsidianSupportBridge.getSessionSummary.implementation = function () {
      var result = this.getSessionSummary();
      console.log(TAG + " ObsidianSupportBridge.getSessionSummary() chamado");
      console.log(TAG + "   resultado (preview): " + (result ? result.substring(0, 200) + "..." : "null"));
      return result;
    };
    console.log(TAG + " hook ObsidianSupportBridge.getSessionSummary instalado");
  } catch (e) {
    console.log(TAG + " [WARN] ObsidianSupportBridge.getSessionSummary nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar ObsidianSupportBridge.getCachedConfig
  // -------------------------------------------------------------------------
  try {
    var ObsidianSupportBridge2 = Java.use("com.obsidianpay.mobile.webview.ObsidianSupportBridge");

    ObsidianSupportBridge2.getCachedConfig.implementation = function () {
      var result = this.getCachedConfig();
      console.log(TAG + " ObsidianSupportBridge.getCachedConfig() chamado");
      console.log(TAG + "   resultado (preview): " + (result ? result.substring(0, 200) + "..." : "null"));
      return result;
    };
    console.log(TAG + " hook ObsidianSupportBridge.getCachedConfig instalado");
  } catch (e) {
    console.log(TAG + " [WARN] ObsidianSupportBridge.getCachedConfig nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar ObsidianSupportBridge.logBridgeEvent
  // -------------------------------------------------------------------------
  try {
    var ObsidianSupportBridge3 = Java.use("com.obsidianpay.mobile.webview.ObsidianSupportBridge");

    ObsidianSupportBridge3.logBridgeEvent.overload("java.lang.String", "java.lang.String").implementation = function (eventType, details) {
      console.log(TAG + " ObsidianSupportBridge.logBridgeEvent() chamado");
      console.log(TAG + "   eventType: '" + eventType + "'");
      console.log(TAG + "   details:   '" + details + "'");
      return this.logBridgeEvent(eventType, details);
    };
    console.log(TAG + " hook ObsidianSupportBridge.logBridgeEvent instalado");
  } catch (e) {
    console.log(TAG + " [WARN] ObsidianSupportBridge.logBridgeEvent nao disponivel: " + e.message);
  }

  // -------------------------------------------------------------------------
  // Observar demais métodos da bridge (getBridgeInfo, getCachedProfile, getLocalArtifacts)
  // -------------------------------------------------------------------------
  var bridgeMethods = ["getBridgeInfo", "getCachedProfile", "getLastSupportSync", "getLastTransferPreview", "getLocalArtifacts"];
  bridgeMethods.forEach(function (methodName) {
    try {
      var Bridge = Java.use("com.obsidianpay.mobile.webview.ObsidianSupportBridge");
      if (Bridge[methodName]) {
        Bridge[methodName].implementation = function () {
          console.log(TAG + " ObsidianSupportBridge." + methodName + "() chamado");
          var result = this[methodName]();
          console.log(TAG + "   resultado (preview): " + (result ? result.substring(0, 150) + "..." : "null"));
          return result;
        };
        console.log(TAG + " hook ObsidianSupportBridge." + methodName + " instalado");
      }
    } catch (e) {
      console.log(TAG + " [WARN] ObsidianSupportBridge." + methodName + " nao disponivel: " + e.message);
    }
  });

  console.log(TAG + " Observando interface: ObsidianBridge (window.ObsidianBridge no JS)");
  console.log(TAG + " Observando classe:    ObsidianSupportBridge");
  console.log(TAG + " Ponto de hook chave:  addJavascriptInterface");
  console.log(TAG + " --- Fase 13 scaffold carregado ---");
});
