# Student Guide - Lab 04 SentinelCore

Este guia ajuda a conduzir a investigacao sem entregar a solucao completa.

## Postura Recomendada

Trate o SentinelCore como um painel interno real: observe o fluxo normal, capture requisicoes, compare respostas e anote cada pista. O lab nao foi desenhado para cair por uma unica URL escondida.

Ferramentas uteis:

- DevTools do navegador
- Burp Proxy e Repeater
- Decodificador/inspector de JWT
- `curl` para repetir requests
- Comparacao de respostas JSON

## Dicas Graduais

### 1. Primeiro Acesso

Entre com a credencial inicial e visite o dashboard. Veja quais APIs aparecem na tela e quais aparecem no JavaScript estatico.

Perguntas uteis:

- O que `/api/v2/me` revela?
- O que muda entre uma lista e um objeto individual?
- O bundle JavaScript cita endpoints que nao aparecem no menu?

### 2. Alertas

Liste os alertas visiveis e observe os IDs. Depois teste acesso direto a detalhes por ID. Compare a regra de listagem com a regra de leitura individual.

### 3. Perfil e Role

O endpoint de perfil recebe JSON. Teste campos esperados e campos extras. Observe quais campos sao bloqueados e quais sao aceitos.

Se algo alterar sua role efetiva, confira novamente `/api/v2/me`.

### 4. Rotas de Analyst

Depois de obter uma role mais forte, procure endpoints de diagnostico e artefatos antigos. Vazamentos pequenos podem ser suficientes para outra etapa.

### 5. JWT

Inspecione o cookie `token`. Ele e um JWT assinado. Se algum segredo de assinatura aparecer em artefatos ou logs, pense no impacto.

### 6. Integracoes

O endpoint de integracao aceita URLs. Teste URLs externas simples primeiro e depois pense na rede Docker interna. O filtro bloqueia alguns nomes obvios, mas nao conhece todos os servicos internos.

### 7. Proxy Interno

Um endpoint de proxy aceita headers customizados. Correlacione isso com qualquer token interno descoberto em artefatos ou contextos de template.

### 8. Templates

O renderizador de relatorios usa placeholders. Nao e RCE, mas contexto demais em template tambem pode vazar segredo.

### 9. Jobs e Worker

Jobs vao para uma fila Redis e sao processados por um worker separado. Entenda:

- nome da fila
- tipos de job suportados
- onde o worker escreve outputs
- como ler esses outputs pela API principal

### 10. Flag Final

A area admin indica que a flag final esta em disco. Procure uma primitiva de leitura no app principal. Se encontrar um filtro de path traversal, pense na ordem entre filtragem e decode.

## Regras de Segurança

Mantenha tudo dentro do ambiente local:

```text
http://127.0.0.1:8090
```

Nao use ataques destrutivos, reverse shell ou qualquer alvo fora do lab.
