# Frida Scripts — Lab 08: ObsidianPay Mobile

Scripts Frida didáticos específicos para o laboratório local **ObsidianPay**.

> **Autorização:** use estes scripts somente no laboratório local autorizado
> (`authorized local lab`). Não os use contra apps reais, dispositivos de
> terceiros ou sistemas externos.
>
> Todos os scripts têm como alvo exclusivo o pacote `com.obsidianpay.mobile`
> e dependem do ambiente de lab local (backend `127.0.0.1:8102`).

---

## Pré-requisitos

1. **frida-tools** instalado no PC: `pip install frida-tools`
2. **frida-server** rodando no dispositivo/emulador (mesma versão do frida-tools):
   ```bash
   adb push frida-server /data/local/tmp/
   adb shell chmod 755 /data/local/tmp/frida-server
   adb shell /data/local/tmp/frida-server &
   ```
3. App instalado: `adb install -r app-debug.apk`

Verifique a conexão: `frida-ps -U`

---

## Como executar os scripts

### spawn mode (recomendado)

**spawn mode** inicia o app e injeta o script antes que o código do app execute.
Útil para hookar inicialização, `Application.onCreate`, ou classes carregadas cedo.

```bash
frida -U -f com.obsidianpay.mobile -l tools/frida/01-environment-bypass.js
frida -U -f com.obsidianpay.mobile -l tools/frida/02-biometric-vault-bypass.js
frida -U -f com.obsidianpay.mobile -l tools/frida/03-network-pinning-observer.js
frida -U -f com.obsidianpay.mobile -l tools/frida/04-integrity-native-bypass.js
frida -U -f com.obsidianpay.mobile -l tools/frida/05-webview-bridge-observer.js
```

### attach mode

**attach mode** injeta em um app já rodando. Útil quando você quer observar
interações em uma tela específica após a inicialização.

```bash
frida -U com.obsidianpay.mobile -l tools/frida/01-environment-bypass.js
frida -U com.obsidianpay.mobile -l tools/frida/02-biometric-vault-bypass.js
```

> Em **attach mode**, hooks instalados após a execução de um método não afetam
> chamadas que já ocorreram. Para hooks de inicialização, use **spawn mode**.

---

## Scripts disponíveis

| Script | Domínio | Hint IDs |
|---|---|---|
| `01-environment-bypass.js` | Root/emulator detection | `hooks-change-return-values`, `patch-risk-engine-result` |
| `02-biometric-vault-bypass.js` | Biometric/local auth | `biometric-result-hook`, `force-auth-decision-true`, `patch-local-auth-state` |
| `03-network-pinning-observer.js` | Network/pinning | `okhttp-certificate-pinner-hook`, `trust-user-ca`, `network-config-cleartext-override` |
| `04-integrity-native-bypass.js` | App integrity | `jni-return-value-hook`, `patch-native-gate-result`, `hook-package-manager`, `patch-debuggable-check` |
| `05-webview-bridge-observer.js` | WebView bridge | observação de `addJavascriptInterface`, `ObsidianSupportBridge` |

---

## Estrutura dos scripts

Cada script segue o padrão:

```javascript
Java.perform(function () {
  // hooks por domínio, cada um com try/catch independente
  // logs prefixados com [ObsidianPay Lab]
});
```

Os scripts usam `try/catch` por classe/método para não quebrar se uma classe
não estiver carregada no momento da injeção. Se um hook falhar silenciosamente,
tente **spawn mode** em vez de **attach mode**.

---

## Diagnóstico rápido

```bash
# verificar se frida-server está rodando
adb shell ps | grep frida

# listar apps rodando
frida-ps -U

# listar classes do app carregadas
frida -U -f com.obsidianpay.mobile --eval "Java.perform(function(){ Java.enumerateLoadedClasses({onMatch: function(c){ send(c); }, onComplete: function(){} }); })"
```

---

## Aviso legal

Estes scripts são material didático do laboratório **ObsidianPay** (Lab 08).
São exclusivos para o ambiente do `authorized local lab` e **não** devem ser
usados para atacar, instrumentar ou monitorar apps, sistemas ou dispositivos que
não sejam o ambiente de laboratório controlado e autorizado.
