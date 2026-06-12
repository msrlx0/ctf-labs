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

| # | Stage ID | Trilha | Dificuldade | Pontos |
|---|---|---|---|---|
| 1 | `stage-01-recon` | Mobile Recon & Config Review | easy | 100 |
| 2 | `stage-02-insecure-storage` | Insecure Local Storage | easy | 150 |
| 3 | `stage-03-exported-components` | Exported Android Components | medium | 200 |
| 4 | `stage-04-webview-bridge` | WebView JavaScript Bridge | medium | 200 |
| 5 | `stage-05-device-trust` | Legacy Device Trust Bypass | medium | 250 |
| 6 | `stage-06-biometric-vault` | Biometric Vault Bypass | hard | 250 |
| 7 | `stage-07-network-pinning` | Network Pinning Review | hard | 250 |
| 8 | `stage-08-app-integrity` | App Integrity / NativeGate Bypass | hard | 300 |
| 9 | `stage-09-final-operator-chain` | Final Operator Chain | insane | 400 |

**Pontuação máxima:** 2000 pontos.

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

- **Correto:** `{ "accepted": true, "stageId": ..., "pointsAwarded": ..., "totalScore": ..., "nextStageHint": ... }`
- **Incorreto:** `{ "accepted": false, "message": "Flag inválida para este estágio." }`
- **Idempotente:** reenviar uma flag correta já submetida **não** duplica pontos
  (`duplicate: true`, `pointsAwarded: 0`).

### 3. Ver o placar

```
GET /api/mobile/challenge/scoreboard
Authorization: Bearer <token>
```

Retorna `user`, `totalScore`, `solvedStages`, `totalStages`,
`completionPercent` e `finalUnlocked`.

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
