# Android Build Checklist — Lab 08: ObsidianPay Mobile

Guia de **preparação do build real do APK** no Android Studio. O componente
Android do Lab 08 é entregue como código-fonte (Kotlin + Jetpack Compose) em
`android-app/`; o APK é gerado localmente.

> **Documentação, não execução obrigatória.** A validação automatizada de shell
> (`scripts/validate-phase*.sh`) **não exige Android SDK** e não gera o APK. O
> build real é feito manualmente no Android Studio seguindo este checklist.
>
> **Sem flags.** Este documento é público e não contém valores de flag nem
> credenciais internas. A única conta documentada é `guest` / `guest123`.

---

## 1. Pré-requisitos

- [ ] **Android Studio** (versão recente) instalado.
- [ ] **Android SDK** configurado (Platform SDK API 24+ e build-tools).
- [ ] **emulator** ou **device** (emulador Pixel API 24+ ou aparelho físico).
- [ ] **adb** disponível no PATH (vem com o Android SDK Platform Tools).
- [ ] **backend Docker** do lab no ar:

```bash
cd lab-08-obsidianpay
docker compose up --build -d
curl -s http://127.0.0.1:8102/health
```

JDK 17+ é necessário (o projeto usa `sourceCompatibility = 17`); o Android Studio
já fornece um JDK adequado.

---

## 2. Abrir o projeto

- [ ] No Android Studio: `File > Open`.
- [ ] Selecione a pasta **`lab-08-obsidianpay/android-app`** (a pasta do módulo
      Gradle, não a raiz do lab).

---

## 3. Conferir SDK / `local.properties`

- [ ] Após abrir, confirme que o Android Studio resolveu o caminho do SDK.
- [ ] Se necessário, crie/edite `android-app/local.properties` apontando para o
      SDK:

```properties
sdk.dir=/caminho/para/Android/Sdk
```

> `local.properties` é específico da sua máquina e **não** deve ser commitado.
> O Android Studio costuma provisionar o SDK automaticamente.

---

## 4. Sincronizar Gradle

- [ ] Aguarde o **Gradle sync** concluir (baixa AGP 8.5.2, Kotlin 1.9.24 e
      dependências). O projeto inclui o **Gradle wrapper** (Gradle 8.7).
- [ ] Resolva qualquer aviso de sync antes de seguir.

Linha de comando (opcional):

```bash
cd lab-08-obsidianpay/android-app
./gradlew tasks    # baixa o Gradle na 1ª vez; requer JDK 17+
```

---

## 5. Build do APK debug

- [ ] Gere o **debug APK**:

```bash
cd lab-08-obsidianpay/android-app
./gradlew assembleDebug
```

- [ ] O artefato sai em `app/build/outputs/apk/debug/app-debug.apk`.

O `applicationId` do build debug é `com.obsidianpay.mobile.debug` (sufixo
`.debug`); o release usa `com.obsidianpay.mobile`. O `assembleDebug` requer o
**Android SDK** — sem SDK ele falha apenas em "SDK location not found", o que é
esperado no shell.

---

## 6. Instalar no emulador

- [ ] Suba um emulador (Pixel, API 24+) e instale o APK debug:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ou use **Run ▶** no Android Studio com o emulador selecionado.

---

## 7. Configurar o API Host

| Ambiente | URL a usar | Como |
|---|---|---|
| **emulador** | `http://10.0.2.2:8102` | Padrão do app — `10.0.2.2` é o alias do emulador para o `127.0.0.1` do host. Nenhuma configuração extra. |
| **celular físico** | `http://<IP_DO_PC>:8102` | Abra a tela **API Host** no app e salve a URL com o IP LAN do PC (ex.: `http://192.168.0.50:8102`), **desde que o backend esteja acessível na rede**. |

> No **celular físico**, `127.0.0.1`/`10.0.2.2` não alcançam o PC. Descubra o IP
> de LAN do computador e use a tela **API Host**. O override é salvo localmente e
> mantido entre reinicializações.

---

## 8. Teste mínimo (smoke test)

Com o app instalado e o backend no ar, confirme o caminho feliz:

- [ ] **login guest / guest123** — autenticação funciona.
- [ ] **Home carrega** — tela inicial com os dados do perfil.
- [ ] **Receipts carrega** — lista de recibos abre.
- [ ] **Support Sync funciona** — o sync de suporte responde.
- [ ] **Web Support abre** — a WebView do portal de suporte carrega.
- [ ] **API Host screen abre** — a tela de override de base URL abre.
- [ ] **Device Trust screen abre** — a tela de Device Trust abre.
- [ ] **Vault screen abre** — a tela do Secure Vault abre.
- [ ] **App Integrity screen abre** — a tela de App Integrity abre.

---

## 9. Troubleshooting

| Sintoma | Causa provável | Ação |
|---|---|---|
| **SDK ausente** | Android SDK não configurado | defina `sdk.dir` em `local.properties` ou configure o SDK pelo Android Studio (`File > Project Structure > SDK Location`). |
| **Gradle sync fail** | dependências/AGP não baixadas | confira conexão; rode `./gradlew --refresh-dependencies`; use o JDK 17+ embutido do Android Studio. |
| **porta 8102 ocupada** | outro processo usando a porta | `lsof -i :8102` (ou `ss -ltnp`), encerre o processo; ou ajuste o mapeamento no `docker-compose.yml` só localmente. |
| **emulador sem acesso ao backend** | usando `127.0.0.1` no emulador | a base URL do app no emulador deve ser `http://10.0.2.2:8102`. |
| **celular físico sem acesso (bind em 127.0.0.1)** | backend acessível só no loopback do host | exponha o backend na LAN e use o IP do PC na tela **API Host**; garanta que firewall/porta `8102` estejam liberados na rede. |
| **cleartext HTTP** | app bloqueando HTTP em host novo | o lab usa `usesCleartextTraffic`; o `network_security_config.xml` libera os hosts locais. Confirme a URL e o host configurado. |
| **Burp / proxy** | interceptação não captura tráfego | configure o proxy do emulador/dispositivo; lembre que, em HTTPS real, Android 7+ não confia em user CA por padrão (cenário futuro do lab). |

---

## 10. Resultado esperado

Ao final, você deve ter:

- APK debug instalado num emulador (ou device).
- Login `guest` / `guest123` funcionando.
- As telas principais abrindo (Home, Receipts, Support, Web Support, API Host,
  Device Trust, Vault, App Integrity).
- O app conversando com o backend em `http://10.0.2.2:8102` (emulador) ou no IP
  de LAN (celular físico).

A partir daí, a cadeia de CTF é exercitada como descrito em `STUDENT-GUIDE.md`
(aluno) e `WALKTHROUGH.md` (instrutor).
