# Download do APK — Lab 08: ObsidianPay Mobile

Esta página é a referência oficial de **download e instalação** do APK Android do
Lab 08 (ObsidianPay Mobile). O laboratório é **local e intencionalmente
vulnerável**, criado apenas para estudo e pentest mobile **autorizado**.

---

## Status atual

> **APK candidato a QA (release candidate) — pendente de aprovação em celular
> físico.** Ainda **não** há release estável publicado.

A Fase 22A entrega o **pipeline automatizado de build** do APK via GitHub Actions.
O workflow produz um **artefato candidato (`rc1`)** para o smoke test em
dispositivo físico. A **publicação estável** acontece somente **depois** que o
smoke test em celular físico passar.

| Item | Valor |
|---|---|
| Arquivo do APK | `ObsidianPay-Lab08-v1.0.0-rc1.apk` |
| Arquivo do checksum | `ObsidianPay-Lab08-v1.0.0-rc1.apk.sha256` |
| Tipo de build | Debug (`com.obsidianpay.mobile.debug`) |
| Android mínimo | **Android 7.0 (API 24)** — derivado de `minSdk 24` |
| Arquitetura | **Universal** — o app é Kotlin/Compose **sem biblioteca nativa obrigatória** (sem `abiFilters`/sem splits por ABI), então o mesmo APK roda em `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`. |

---

## Como baixar o artefato (GitHub Actions)

Enquanto não há release estável, o APK candidato é obtido como **artefato do
GitHub Actions**:

1. Abra a aba **Actions** do repositório no GitHub.
2. Selecione o workflow **Lab 08 Android APK**
   ([`.github/workflows/lab08-android-apk.yml`](../.github/workflows/lab08-android-apk.yml)).
3. Abra a execução mais recente concluída com sucesso (✅).
4. Na seção **Artifacts** da execução, baixe o artefato
   **`obsidianpay-lab08-v1.0.0-rc1`**.
5. Descompacte o `.zip` baixado. Dentro dele estão:
   - `ObsidianPay-Lab08-v1.0.0-rc1.apk`
   - `ObsidianPay-Lab08-v1.0.0-rc1.apk.sha256`

> **Login no GitHub pode ser necessário.** O download de artefatos do GitHub
> Actions normalmente exige que você esteja **autenticado** no GitHub (artefatos
> não têm link público anônimo).
>
> Os logs do workflow também imprimem o **tamanho** e o **SHA256** do APK, úteis
> para conferência rápida antes mesmo de descompactar.

### Release estável (no futuro)

Quando o smoke test em celular físico passar, o APK estável será publicado em
**GitHub Releases** do repositório (aba **Releases**), com o mesmo padrão de
nome e o arquivo `.sha256` correspondente. Até lá, **use o artefato de QA acima**.

---

## Verificação do SHA256

Sempre confira o hash antes de instalar. Compare o valor calculado com o conteúdo
do arquivo `ObsidianPay-Lab08-v1.0.0-rc1.apk.sha256`.

**Linux / macOS:**

```bash
sha256sum ObsidianPay-Lab08-v1.0.0-rc1.apk
# compare com:
cat ObsidianPay-Lab08-v1.0.0-rc1.apk.sha256
```

**Windows (PowerShell):**

```powershell
Get-FileHash .\ObsidianPay-Lab08-v1.0.0-rc1.apk -Algorithm SHA256
# compare com o conteúdo de:
Get-Content .\ObsidianPay-Lab08-v1.0.0-rc1.apk.sha256
```

Os hashes devem ser **idênticos** (ignore maiúsculas/minúsculas). Se divergirem,
**não instale** — baixe novamente.

---

## Instalação com adb

Com o **Android SDK Platform Tools** (`adb`) no PATH e o dispositivo/emulador
conectado:

```bash
adb install -r ObsidianPay-Lab08-v1.0.0-rc1.apk
```

A flag `-r` reinstala mantendo os dados, útil ao atualizar uma versão anterior.

### Conferir o pacote instalado

O **build debug** usa o `applicationId` **`com.obsidianpay.mobile.debug`**:

```bash
adb shell pm list packages | grep com.obsidianpay.mobile.debug
adb shell dumpsys package com.obsidianpay.mobile.debug | grep versionName
```

O **build estável (release)**, quando publicado, usa o `applicationId`
**`com.obsidianpay.mobile`** (sem o sufixo `.debug`):

```bash
adb shell pm list packages | grep com.obsidianpay.mobile
```

### Desinstalar

```bash
# build debug (este APK de QA)
adb uninstall com.obsidianpay.mobile.debug

# build estável (quando aplicável)
adb uninstall com.obsidianpay.mobile
```

---

## Subir o backend

O app precisa do backend do lab no ar (porta **8102**):

```bash
cd lab-08-obsidianpay
docker compose up --build -d
curl -s http://127.0.0.1:8102/health
```

Para derrubar: `docker compose down`.

---

## API Host: emulador vs. celular físico

O destino de rede muda conforme o ambiente:

| Ambiente | API Host | Como configurar |
|---|---|---|
| **Emulador Android** | `http://10.0.2.2:8102` | Padrão do app — `10.0.2.2` é o alias do emulador para o `127.0.0.1` do host. Sem configuração extra. |
| **Celular físico (LAN)** | `http://<IP_DO_PC>:8102` | Abra a tela **API Host** no app e salve a URL com o IP de LAN do PC (ex.: `http://192.168.0.50:8102`). O backend precisa estar acessível na rede e a porta `8102` liberada no firewall. |

> No celular físico, `127.0.0.1`/`10.0.2.2` apontam para o **próprio aparelho**,
> não para o PC — por isso use o IP de LAN via tela **API Host**.

### Alternativa de QA com `adb reverse` (celular físico)

Para um smoke test rápido em celular físico **sem** expor o backend na LAN, use
`adb reverse` para tunelar a porta `8102` do aparelho para o `127.0.0.1` do PC:

```bash
adb reverse tcp:8102 tcp:8102
```

Com o reverse ativo, configure a tela **API Host** para `http://127.0.0.1:8102`
(o tráfego do aparelho é encaminhado para o backend local do PC pelo cabo USB).

---

## Credenciais públicas

A única conta documentada para o aluno é:

| Usuário | Senha |
|---|---|
| `guest` | `guest123` |

> São credenciais didáticas controladas, **não** segredos. Não há flags reais
> nesta página nem em qualquer documento público do lab.

---

## Aviso de uso autorizado

O ObsidianPay é um laboratório **somente local** e **intencionalmente
vulnerável**. Todo o ambiente roda em `127.0.0.1:8102` (host) /
`10.0.2.2:8102` (emulador). **Não** exponha os containers na internet e **não**
use técnicas ou payloads deste lab contra apps ou sistemas de terceiros.

---

## Veja também

- **README do Lab 08:** [README.md](./README.md)
- **Guia do aluno (sem spoilers):** [STUDENT-GUIDE.md](./STUDENT-GUIDE.md)
- **Setup de pentest mobile:** [docs/mobile-pentest/SETUP.md](./docs/mobile-pentest/SETUP.md)
- **Checklist de build Android:** [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md)
