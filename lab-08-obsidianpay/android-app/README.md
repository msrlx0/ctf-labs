# ObsidianPay — App Android

App Android nativo (Kotlin + Jetpack Compose) que consome a API mobile do
**Lab 08 — ObsidianPay Mobile**. O app está **implementado** (não é um scaffold
inicial) e inclui: cache local/offline, **deep links**, tela **QR Payment** e
**Web Support** em WebView com **support bridge** JavaScript (`ObsidianBridge`),
**componentes Android exportados** (pacote `platform/`), fluxo **Device Trust**
com trilha de reverse engineering (pacote `security/`), tela **Security Check**
(root/emulador, pacote `environment/`), **Secure Vault** com autenticação local
(biometria scaffold + fallback PIN, pacote `auth/`), scaffold de **Network
Security / Certificate Pinning** (pacote `network/`) com tela **API Host** para
override de base URL (emulador ↔ celular físico), scaffold de **App Integrity**
(`NativeGate`/`TamperCheck`, pacote `integrity/`) e a integração com a **Final
Challenge Chain** do backend. JNI/NDK é **opcional**: o app compila sem NDK e usa
fallback Kotlin quando a biblioteca nativa está ausente.

> **Ambiente somente local.** O app fala com o backend do lab em
> `http://10.0.2.2:8102` (emulador) ou em um IP de LAN configurável via tela
> "API Host" (celular físico). Veja a nota de dev abaixo.
>
> **APK estável: ainda NÃO publicado.** O pipeline de build (Fase 22A) gera um
> **APK candidato a QA** (`ObsidianPay-Lab08-v1.0.0-rc2.apk`), pendente de
> validação em celular físico. Download e instalação: **[../DOWNLOAD.md](../DOWNLOAD.md)**.

---

## Pré-requisitos e configuração do build

O projeto é a **fonte da verdade** dos requisitos abaixo (ver
[build.gradle](./build.gradle) e [app/build.gradle](./app/build.gradle)):

| Item | Valor |
|---|---|
| JDK | **17** (Temurin) — `sourceCompatibility`/`targetCompatibility = 17`, `jvmTarget = 17` |
| Gradle | **8.7** (via wrapper, `gradle/wrapper/gradle-wrapper.properties`) |
| Android Gradle Plugin | **8.5.2** |
| Kotlin | **1.9.24** (Compose compiler `1.5.14`) |
| `compileSdk` / `targetSdk` | **34** |
| `minSdk` | **24** (Android 7.0) |
| `applicationId` (release) | `com.obsidianpay.mobile` |
| `applicationId` (debug) | `com.obsidianpay.mobile.debug` (sufixo `.debug`) |
| `versionCode` / `versionName` | `2` / `1.0.0-rc2` |
| UI | Jetpack Compose (BOM `2024.06.00`), Material 3 (tema escuro "obsidian", navegação por abas) |
| HTTP | OkHttp `4.12.0` |

Ferramentas: **Android Studio** (recente) **ou** Gradle + Android SDK instalados;
backend do lab rodando (ver abaixo).

## 1. Suba o backend

Na raiz do lab:

```bash
cd lab-08-obsidianpay
docker compose up --build -d
curl -s http://127.0.0.1:8102/health
```

## 2. Abra o app no Android Studio

1. `File > Open` e selecione a pasta `lab-08-obsidianpay/android-app`.
2. Aguarde o **Gradle sync** (baixa AGP 8.5.2, Kotlin 1.9.24 e dependências).
3. Rode em um **emulador** (Pixel/API 24+). No emulador, `10.0.2.2` aponta para
   o `127.0.0.1` da sua máquina — por isso a base URL padrão é
   `http://10.0.2.2:8102`.

## 3. Use o app

- Login já vem preenchido com `guest` / `guest123` para teste rápido.
- A navegação principal usa **abas inferiores**: **Início** (dashboard com saldo,
  ações rápidas e atividade), **Transferir**, **Cartões**, **Segurança** (central
  de segurança) e **Conta** (perfil, ajuda e **Configurações**).
- A configuração de conexão (emulador ↔ dispositivo físico) fica em
  **Conta → Configurações → Conexão avançada**, com presets persistentes.

---

## Build via linha de comando

O projeto inclui o **Gradle wrapper** (`gradlew` / `gradlew.bat`, Gradle 8.7).

```bash
cd lab-08-obsidianpay/android-app

# listar tarefas (requer JDK 17; baixa o Gradle na 1ª vez)
./gradlew tasks

# build limpo do APK debug (requer Android SDK configurado)
./gradlew --no-daemon clean :app:assembleDebug
```

O `assembleDebug` gera o APK em:

```
app/build/outputs/apk/debug/app-debug.apk
```

O `assembleDebug` precisa do **Android SDK** (Platform `android-34` e
`build-tools;34.0.0`). Defina o SDK via Android Studio ou crie um
`local.properties` com `sdk.dir=/caminho/para/Android/Sdk`. Sem SDK, o build do
APK não roda — use o Android Studio, que provisiona o SDK automaticamente.

### Artefato candidato a QA (Fase 22A)

O workflow **Lab 08 Android APK**
([`.github/workflows/lab08-android-apk.yml`](../../.github/workflows/lab08-android-apk.yml))
roda exatamente esse build, copia/renomeia o `app-debug.apk` para
**`ObsidianPay-Lab08-v1.0.0-rc2.apk`**, gera o `.sha256` e publica os dois como
artefato do GitHub Actions. O helper local
[`../scripts/package-android-apk.sh`](../scripts/package-android-apk.sh) faz o
mesmo empacotamento a partir de um build local. Download e verificação em
**[../DOWNLOAD.md](../DOWNLOAD.md)**.

> Se o `gradle-wrapper.jar` não estiver presente no seu clone, o Android Studio
> o regenera no primeiro sync, ou rode `gradle wrapper --gradle-version 8.7`.

---

## Estrutura

```
android-app/
├── settings.gradle / build.gradle / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
└── app/
    ├── build.gradle / proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/obsidianpay/mobile/
        │   ├── MainActivity.kt          # shell Compose + navegação simples
        │   ├── api/                      # ApiClient (OkHttp), modelos, ApiResult
        │   ├── auth/                     # LocalAuthState, BiometricGate (Fase 10)
        │   ├── deeplink/                 # DeepLinkRouter + modelos (Fase 5)
        │   ├── environment/              # RootDetector, EmulatorDetector, EnvironmentRiskEngine (Fase 9)
        │   ├── platform/                 # componentes Android exportados (Fase 7)
        │   ├── security/                 # HardcodedSecrets, WeakCrypto, LegacyRequestSigner (Fase 8)
        │   ├── storage/                  # InsecureSessionStore, ObsidianLocalDb (SQLite), LocalCacheManager
        │   ├── ui/                       # telas Login/Home/Receipts/Cards/Support/Transfer/WebSupport/VaultScreen
        │   ├── webview/                  # ObsidianSupportBridge (Fase 6)
        │   └── util/Constants.kt         # base URL, chaves de storage, header de debug
        └── res/ (values/, xml/network_security_config.xml)
```

## Armazenamento local (Fase 4)

O app mantém estado local/offline, em texto puro **de propósito** (semente para
estudo de armazenamento inseguro):

- **SharedPreferences** (`InsecureSessionStore`): sessão e cache de perfil/config,
  além de últimos resultados de suporte/diagnostics/preview e IDs abertos.
- **SQLite** (`ObsidianLocalDb` → `obsidianpay_local.db`): tabelas
  `cached_receipts`, `cached_cards` (com `rawJson`) e `debug_events`.
- **Arquivos internos**: `cacheDir/obsidian-support-last-sync.json`,
  `filesDir/receipts/receipt-<id>.json`, `filesDir/debug/obsidian-debug-export.json`.
- **External app-specific**: `getExternalFilesDir(null)/obsidian-export.txt`
  (storage específico do app, não público global).
- **`LocalCacheManager`** orquestra tudo; a tela **Local State** (na Início)
  mostra o estado local como ferramenta de suporte/dev.

Educacionalmente: vale observar **o que** o app persiste e **onde**. Este README
não descreve extração nem exploração — isso faz parte do exercício.

## Deep links, QR e Web Support (Fase 5)

Deep links suportados (`MainActivity` os recebe):

```
obsidianpay://transfer?toUserId=2001&amount=10&memo=test
obsidianpay://support?topic=mobile&message=hello
obsidianpay://receipt?id=1002
```

Disparar por `adb` (app instalado no emulador):

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "obsidianpay://transfer?toUserId=2001&amount=10&memo=test" \
  com.obsidianpay.mobile.debug
```

> O `applicationId` debug é `com.obsidianpay.mobile.debug` (sufixo `.debug`).
> Para o build release, use `com.obsidianpay.mobile`.

- **QR Payment** (tela): cole/digite um dos payloads acima; o app os interpreta
  como uma leitura de QR faria.
- **Web Support** (WebView): carrega
  `http://10.0.2.2:8102/api/mobile/webview/support?topic=mobile`. JavaScript e
  DOM storage ficam habilitados.

## Support bridge (Fase 6)

A WebView de **Web Support** expõe uma **support bridge** JavaScript
(`webview/ObsidianSupportBridge.kt`, anexada como `ObsidianBridge` via
`addJavascriptInterface`). Ela existe para que o portal de suporte mostre, dentro
do app, contexto local (resumo de sessão, status, diagnóstico) sem uma chamada
extra ao backend. O portal detecta `window.ObsidianBridge` e habilita botões de
diagnóstico assistido ("Show bridge info", "Show session summary").

Educacionalmente: vale observar **o que** uma ponte de suporte como essa
disponibiliza para a página da WebView e **quem** pode acioná-la. Este README não
descreve abuso nem entrega solução — isso faz parte do exercício. (Sem flags.)

## Componentes internos (Fase 7)

O app inclui um pacote `platform/` com componentes Android usados para
integrações internas de operações/diagnóstico — declarados no `AndroidManifest`:

- `platform/InternalOpsActivity.kt` — tela interna "Internal Operations"
  (suporte/diagnóstico) acionável por intent.
- `platform/DebugCommandReceiver.kt` — `BroadcastReceiver` que recebe comandos de
  debug/automação e atualiza apenas o estado local do app.
- `platform/ObsidianNotesProvider.kt` — `ContentProvider` de notas/estado de
  suporte (authority `com.obsidianpay.mobile.provider.notes`).

Para desenvolvimento/teste, esses componentes podem ser acionados via `adb`
(`am start`, `am broadcast`, `content query`). Educacionalmente: ao analisar um
app Android, vale revisar o `AndroidManifest` e perguntar **quais** componentes
ficam acessíveis a outros apps e **o que** cada um faz com o estado local. Este
README não trata isso como solução final nem entrega exploração — a investigação
faz parte do exercício. (Sem flags; o provedor nunca devolve o token inteiro.)

## Device Trust e trilha de reverse engineering (Fase 8)

O app inclui um pacote `security/` e uma tela **Device Trust** (na Início) que
simulam um fluxo de atestação/segurança "legado":

- `security/HardcodedSecrets.kt` — config/segredos hardcoded **fragmentados**
  (client id, salt de assinatura, hint em Base64, rotas internas), reassemblados
  em runtime. São didáticos, **não** são segredos reais.
- `security/WeakCrypto.kt` — Base64, XOR de chave repetida e SHA-1/MD5
  (intencionalmente fracos; Base64 não é criptografia).
- `security/LegacyRequestSigner.kt` — monta os headers
  `X-Obsidian-Client/Device/Timestamp/Signature` com uma assinatura SHA-1 local.
- `ui/DeviceTrustScreen.kt` — chama `POST /api/mobile/internal/device-trust`.

Para desenvolvimento/teste, este material é exatamente o tipo de coisa que se
analisa com **JADX/apktool/`strings`**. Este README descreve a existência do
fluxo, **não** entrega a solução completa (assinatura/segredo/rota interna) — a
investigação faz parte do exercício. (Sem flags; o app nunca embute segredos
reais.)

## Instrumentação dinâmica — pontos didáticos (Fase 13)

O app contém pontos didáticos para instrumentação dinâmica no pacote
`com.obsidianpay.mobile`. Cada domínio tem um script Frida scaffold em
`tools/frida/`:

| Pacote | Classes | Script Frida |
|---|---|---|
| `environment/` | `RootDetector`, `EmulatorDetector`, `EnvironmentRiskEngine` | `tools/frida/01-environment-bypass.js` |
| `auth/` | `LocalAuthState`, `BiometricGate` | `tools/frida/02-biometric-vault-bypass.js` |
| `network/` | `NetworkSecurityProfile`, `PinningPolicy` | `tools/frida/03-network-pinning-observer.js` |
| `integrity/` | `NativeGate`, `TamperCheck` | `tools/frida/04-integrity-native-bypass.js` |
| `webview/` | `ObsidianSupportBridge` | `tools/frida/05-webview-bridge-observer.js` |

Os scripts são exclusivos do laboratório local autorizado e específicos para o
pacote `com.obsidianpay.mobile`. Consulte `tools/frida/README.md` para instruções
de uso e `tools/adb/lab08-adb-playbook.sh` para o playbook de comandos ADB.

## Notas técnicas (Fase 3)

- **HTTP local:** `usesCleartextTraffic` + `network_security_config.xml`
  permitem cleartext **apenas** para `10.0.2.2`/`127.0.0.1`/`localhost`. Para
  celular físico, o override de base URL usa o flag `usesCleartextTraffic` do
  manifest (válido para qualquer host, intencional para o lab). Não use em produção.
- **Nota de dev — emulador vs. celular físico (Fase 11):**
  - Emulador: `http://10.0.2.2:8102` (padrão, sem configuração extra).
  - Celular real: use a tela **API Host** para definir `http://IP_DO_PC:8102`
    (onde `IP_DO_PC` é o IP do seu computador na rede local, ex. `192.168.0.50`).
  - O override é salvo em SharedPreferences e restaurado na próxima inicialização.
- **Armazenamento local:** `InsecureSessionStore` grava token/perfil em
  `SharedPreferences` em texto puro, **de propósito** (ver seção acima).
- **Componentes exportados:** além de `MainActivity` (launcher + deep links), a
  Fase 7 adiciona componentes do pacote `platform/` exportados de propósito
  (Activity/Receiver/Provider) — ver "Componentes internos (Fase 7)" acima.
- **Fora de escopo nesta fase:** Frida, pinning real, lib nativa, root/biometria
  bypass, binary patching e scanner de QR por câmera real.

> Sem flags e sem soluções aqui. A investigação faz parte do desafio.
