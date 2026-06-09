# WALKTHROUGH (Instrutor) — Lab 08: ObsidianPay Mobile

> **Documento interno do instrutor.** Não é material do aluno.
>
> **Estado: Fase 1.** Este walkthrough descreve a **arquitetura planejada** e as
> **cadeias futuras em alto nível**. Ele **não** contém, nesta fase:
> - flags finais,
> - payloads avançados,
> - passo a passo de exploração.
>
> Esses detalhes serão adicionados nas fases em que cada vulnerabilidade for de
> fato implementada.

---

## 1. Visão geral da arquitetura planejada

```
┌──────────────────────────┐        HTTP (local)        ┌───────────────────────────┐
│  App Android (futuro)    │ ─────────────────────────▶ │  ObsidianPay Mobile API     │
│  - UI carteira/pagamentos│                            │  Node.js + Express          │
│  - storage local         │ ◀───────────────────────── │  127.0.0.1:8102             │
│  - WebView support center│        JSON/token          │  estado em memória (fase 1) │
│  - deep links / QR        │                            └───────────────────────────┘
└──────────────────────────┘
```

- **Fase 1 (atual):** apenas a API. Define contratos: login, profile, config,
  receipt, support/sync. Token didático previsível (costura para vuln futura).
- **Fases seguintes:** o APK Android consome esses contratos e introduz as
  cadeias de vulnerabilidade do lado cliente, conectadas à API.

O ObsidianPay deve sempre **parecer um produto financeiro real**. Vulnerabilidades
ficam embutidas no fluxo de produto, nunca como um "menu de bugs".

---

## 2. Cadeias futuras planejadas (alto nível)

Sequência didática pretendida — sem soluções nesta fase:

1. **Recon do APK** — inventário de telas, permissões, componentes, endpoints,
   strings e configs. Base para todo o resto.
2. **Interceptação de tráfego** — observar o diálogo app↔API; identificar o que
   trafega, como e onde a confiança de transporte pode ser quebrada (local).
3. **Armazenamento local** — onde o app guarda token/dados sensíveis e o que
   vaza em SharedPreferences, SQLite, cache e arquivos temporários.
4. **Componentes exportados** — Activities/Services/Receivers/Providers
   exportados e o que pode ser acionado por outro app.
5. **Deep link + WebView** — roteamento por deep link levando a uma WebView com
   configurações inseguras e/ou bridge JS exposta.
6. **Frida / pinning / root / biometria** — instrumentação dinâmica para estudar
   pinning, detecção de root/emulador e callbacks biométricos.
7. **API authorization** — fechar a cadeia explorando autorização da API
   (acesso a objetos, mass assignment, etc.), ligando cliente e servidor.

---

## 3. Costuras já plantadas na Fase 1

Itens já presentes no backend que sustentam vulnerabilidades futuras
(intencionalmente **inofensivos** agora):

- **Token previsível/decodificável** em `/api/mobile/login` — base para forja de
  token / sessão fraca no futuro.
- **Múltiplos recibos** no modelo (`data.js`), incluindo de outro `ownerUserId`
  — preparação para estudo de autorização a nível de objeto. A Fase 1 ainda
  **valida ownership** (comportamento correto).
- **`legacySupportEndpoint` em cleartext** e **`/api/mobile/support/sync`** como
  stub legado — gancho para tráfego HTTP/legacy.
- **`mobileFeatureFlags`** (biometric, qr, webview, deep link) — sinalizam as
  superfícies que o app vai expor.

---

## 4. Matriz de vulnerabilidades planejadas

Detalhe completo por trilha em [docs/VULNERABILITY-ROADMAP.md](./docs/VULNERABILITY-ROADMAP.md).
Resumo de status (Fase 1 = tudo `planned`):

| # | Trilha | Vulnerabilidade planejada | Status |
|---|---|---|---|
| 1 | Network/API | HTTP legacy sync | planned |
| 2 | Network/API | HTTPS interception | planned |
| 3 | Network/API | Certificate pinning bypass | planned |
| 4 | Network/API | Native pinning | planned |
| 5 | Storage/RE | SharedPreferences token leak | planned |
| 6 | Storage/RE | SQLite sensitive data | planned |
| 7 | Storage/RE | Temp/cache file leak | planned |
| 8 | Storage/RE | Hardcoded/config secrets | planned |
| 9 | Storage/RE | Weak crypto | planned |
| 10 | Platform | Exported Activity | planned |
| 11 | Platform | Exported Service | planned |
| 12 | Platform | BroadcastReceiver debug trigger | planned |
| 13 | Platform | ContentProvider exposure | planned |
| 14 | Platform | Deep link abuse | planned |
| 15 | Platform | QR Code untrusted input | planned |
| 16 | WebView | Unsafe WebView settings | planned |
| 17 | WebView | JavaScript bridge exposure | planned |
| 18 | WebView | Deep link → WebView chain | planned |
| 19 | WebView | Local file / token disclosure | planned |
| 20 | Anti-analysis/Auth | Root detection bypass | planned |
| 21 | Anti-analysis/Auth | Emulator detection bypass | planned |
| 22 | Anti-analysis/Auth | Biometric callback bypass | planned |
| 23 | Anti-analysis/Auth | Binary patching | planned |
| 24 | Anti-analysis/Auth | API broken access control | planned |
| 25 | Anti-analysis/Auth | Mass assignment | planned |

---

## 5. Notas de manutenção

- Flags reais **não** entram em `README.md` nem `STUDENT-GUIDE.md`.
- Soluções e payloads só serão adicionados aqui (ou em SOLUTION.md) quando a
  vulnerabilidade correspondente for implementada.
- Manter tudo **local**: porta `127.0.0.1:8102`, sem dependências externas.
