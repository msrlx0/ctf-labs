# Challenge Scoring — Lab 08: ObsidianPay Mobile (Fase 14)

Guia público da **cadeia final de CTF** do Lab 08. Este documento descreve a
estrutura de pontuação, como submeter progresso e como registrar evidências.

> **Sem spoilers.** Este documento NÃO revela a solução de nenhum estágio nem
> contém flags. Os valores reais das flags vivem apenas no backend
> (`api/src/flags.js`) e são descobertos resolvendo cada trilha. Exemplos de
> submissão usam o marcador `<flag_redacted>` no lugar da flag real.

---

## Chain ID

```
obsidianpay-mobile-final-chain
```

- **Total de estágios:** 9
- **Ordem oficial:** stage-01 → stage-09 (progressão didática sugerida).
- **Etapa final:** `stage-09-final-operator-chain` (destrava ao concluir as 8
  trilhas anteriores).

---

## Estágios e pontuação

| # | Stage ID | Trilha | Dificuldade | Pontos | Evidência esperada (alto nível) |
|---|---|---|---|---|---|
| 1 | `stage-01-recon` | Mobile Recon & Config Review | easy | 100 | Bloco de checkpoint na config mobile ao usar o modo de revisão. |
| 2 | `stage-02-insecure-storage` | Insecure Local Storage | easy | 150 | Checkpoint no sync de suporte + material cacheado localmente. |
| 3 | `stage-03-exported-components` | Exported Android Components | medium | 200 | Saída de `adb` demonstrando o componente Android exportado. |
| 4 | `stage-04-webview-bridge` | WebView JavaScript Bridge | medium | 200 | Checkpoint da bridge no portal de suporte em WebView. |
| 5 | `stage-05-device-trust` | Legacy Device Trust Bypass | medium | 250 | Resposta de confiança legada aceita pelo backend. |
| 6 | `stage-06-biometric-vault` | Biometric Vault Bypass | hard | 250 | Resposta de acesso ao vault concedido. |
| 7 | `stage-07-network-pinning` | Network Pinning Review | hard | 250 | Checkpoint do perfil de rede no modo de revisão. |
| 8 | `stage-08-app-integrity` | App Integrity / NativeGate Bypass | hard | 300 | Checkpoint de integridade no relatório do cliente. |
| 9 | `stage-09-final-operator-chain` | Final Operator Chain | insane | 400 | Resposta final que consolida as trilhas internas. |

**Pontuação máxima:** 2000 pontos.

> A coluna de evidência é **alto nível** e descreve **o que** comprova o estágio —
> não **como** chegar lá. Os endpoints, headers e payloads exatos fazem parte do
> desafio.

Cada estágio possui dois níveis de dica (`hintLevel1`, `hintLevel2`), um resumo
público (`publicSummary`) e a evidência esperada (`evidenceExpected`),
disponíveis via `GET /api/mobile/challenge/progress`.

---

## Como submeter progresso

Todos os endpoints da cadeia exigem um **Bearer token** válido (faça login em
`POST /api/mobile/login`, ex.: conta `guest`).

### 1. Ver progresso

```
GET /api/mobile/challenge/progress
Authorization: Bearer <token>
```

Retorna `chainId`, `totalStages`, a lista de estágios públicos e, por estágio,
`submitted` (true/false) e `pointsAwarded`. **Nunca retorna flags.**

### 2. Submeter a flag de um estágio

```
POST /api/mobile/challenge/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "stageId": "stage-01-recon",
  "flag": "<flag_redacted>",
  "evidence": "descrição curta de como você obteve a flag"
}
```

**`accepted: true` ou `accepted: false`:**

- **`accepted: true`** — a flag bate com o estágio. A resposta traz `stageId`,
  `pointsAwarded`, `totalScore` e `nextStageHint`.
- **`accepted: false`** — a flag não corresponde àquele `stageId`
  (`message: "Flag inválida para este estágio."`). Nenhum ponto é atribuído.

**Submissão idempotente:** o submit é **idempotente** por estágio — reenviar uma
flag correta já submetida **não** duplica pontos (`duplicate: true`,
`pointsAwarded: 0`). Você pode reenviar sem medo de inflar o placar.

### 3. Ver o placar (scoreboard)

```
GET /api/mobile/challenge/scoreboard
Authorization: Bearer <token>
```

Retorna o placar do usuário:

| Campo | Significado |
|---|---|
| `user` | identidade do token autenticado |
| `totalScore` | soma dos pontos dos estágios já aceitos |
| `solvedStages` | quantidade de estágios resolvidos |
| `totalStages` | total de estágios da cadeia (9) |
| `completionPercent` | percentual concluído (`solvedStages / totalStages`) |
| `finalUnlocked` | `true` quando a etapa final pode ser concluída |

`completionPercent` chega a `100` e `finalUnlocked` vira `true` ao completar a
cadeia. O placar é **em memória** e zera quando o backend reinicia.

---

## Como registrar evidência

O campo `evidence` do submit é o lugar para registrar **como** a flag foi
obtida (endpoint, header, payload, hook/patch utilizado, captura de tráfego,
etc.). Recomenda-se:

- Uma linha objetiva por estágio (ferramenta + passo decisivo).
- Referenciar artefatos salvos em `evidence/` quando aplicável (logs, capturas,
  scripts Frida usados).
- Não colar a flag dentro do texto de evidência — ela já vai no campo `flag`.

---

## Etapa final (operator chain)

A etapa `stage-09-final-operator-chain` é destravada após concluir as 8 trilhas
anteriores e consolida o domínio das cadeias internas. A submissão final segue
o mesmo fluxo de `POST /api/mobile/challenge/submit` com `stageId`
`stage-09-final-operator-chain`. Os requisitos exatos (header de device-trust e
provas de cada trilha) fazem parte do desafio e **não** são detalhados aqui.

---

## Notas

- Ambiente **local apenas** (`127.0.0.1:8102`). O placar é em memória e zera
  quando o backend é reiniciado.
- Este material é público e **não** contém flags nem credenciais internas.
- A solução completa, com flags e payloads, é material de instrutor
  (`WALKTHROUGH.md`).
