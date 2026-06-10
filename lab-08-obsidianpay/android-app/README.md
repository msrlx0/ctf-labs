# ObsidianPay — App Android (Fase 8)

App Android nativo (Kotlin + Jetpack Compose) que consome a API mobile do
**Lab 08 — ObsidianPay Mobile**. Mantém cache local/offline (Fase 4), suporta
**deep links**, uma tela **QR Payment** e um **Web Support** em WebView (Fase 5),
uma **support bridge** JavaScript na WebView (Fase 6), **componentes Android
internos** (pacote `platform/`, Fase 7) e, a partir da Fase 8, um fluxo
**Device Trust** com trilha de reverse engineering (pacote `security/`).

> **Ambiente somente local.** O app só fala com o backend do lab em
> `http://10.0.2.2:8102` (alias do emulador para o `127.0.0.1` do host).
>
> **Ainda não há APK final publicado.** Esta fase entrega o código-fonte do app.

---

## Pré-requisitos

- Android Studio (recente) **ou** Gradle + Android SDK instalados.
- JDK 17+ (o projeto usa `sourceCompatibility = 17`).
- Backend do lab rodando (ver abaixo).

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
- Navegue por: Início, Recibos, Cartões, Suporte, Prévia de transferência e
  Configuração.

---

## Build via linha de comando

O projeto inclui o **Gradle wrapper** (`gradlew` / `gradlew.bat`, Gradle 8.7).

```bash
cd lab-08-obsidianpay/android-app

# listar tarefas (requer JDK; baixa o Gradle na 1ª vez)
./gradlew tasks

# gerar APK debug (requer Android SDK configurado)
./gradlew assembleDebug
```

O `assembleDebug` precisa do **Android SDK**. Defina o SDK via Android Studio ou
crie um `local.properties` com `sdk.dir=/caminho/para/Android/Sdk`. Sem SDK, o
build do APK não roda — use o Android Studio, que provisiona o SDK
automaticamente.

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
        │   ├── deeplink/                 # DeepLinkRouter + modelos (Fase 5)
        │   ├── platform/                 # componentes Android exportados (Fase 7)
        │   ├── security/                 # HardcodedSecrets, WeakCrypto, LegacyRequestSigner (Fase 8)
        │   ├── storage/                  # InsecureSessionStore, ObsidianLocalDb (SQLite), LocalCacheManager
        │   ├── ui/                       # telas Login/Home/Receipts/Cards/Support/Transfer/WebSupport
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

## Notas técnicas (Fase 3)

- **HTTP local:** `usesCleartextTraffic` + `network_security_config.xml`
  permitem cleartext **apenas** para `10.0.2.2`/`127.0.0.1`/`localhost`. Não use
  em produção.
- **Armazenamento local:** `InsecureSessionStore` grava token/perfil em
  `SharedPreferences` em texto puro, **de propósito** (ver seção acima).
- **Componentes exportados:** além de `MainActivity` (launcher + deep links), a
  Fase 7 adiciona componentes do pacote `platform/` exportados de propósito
  (Activity/Receiver/Provider) — ver "Componentes internos (Fase 7)" acima.
- **Fora de escopo nesta fase:** Frida, pinning real, lib nativa, root/biometria
  bypass, binary patching e scanner de QR por câmera real.

> Sem flags e sem soluções aqui. A investigação faz parte do desafio.
