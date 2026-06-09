# ObsidianPay — App Android (Fase 4)

App Android nativo (Kotlin + Jetpack Compose) que consome a API mobile do
**Lab 08 — ObsidianPay Mobile**. A partir da Fase 4, o app mantém **cache
local/offline** (SharedPreferences, SQLite e arquivos internos), como um app de
carteira real faria.

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
        │   ├── storage/                  # InsecureSessionStore, ObsidianLocalDb (SQLite), LocalCacheManager
        │   ├── ui/                       # telas Login/Home/Receipts/Cards/Support/Transfer
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

## Notas técnicas (Fase 3)

- **HTTP local:** `usesCleartextTraffic` + `network_security_config.xml`
  permitem cleartext **apenas** para `10.0.2.2`/`127.0.0.1`/`localhost`. Não use
  em produção.
- **Armazenamento local:** `InsecureSessionStore` grava token/perfil em
  `SharedPreferences` em texto puro, **de propósito** (ver seção acima).
- **Componentes exportados:** apenas `MainActivity` é exportada (exigência do
  launcher). Componentes exportados vulneráveis ficam para uma fase futura.
- **Fora de escopo nesta fase:** Frida, pinning real, lib nativa, root/biometria
  bypass, binary patching, WebView bridge avançada e scanner de QR real.

> Sem flags e sem soluções aqui. A investigação faz parte do desafio.
