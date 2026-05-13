# Student Guide - Lab 04 SentinelCore

Este guia orienta a investigacao sem listar flags, sem expor segredos e sem entregar o payload final. Use-o como uma trilha de dicas graduais: avance uma secao por vez e tente validar cada hipotese antes de abrir a proxima.

## Postura Recomendada

Trate o SentinelCore como um painel interno real. Faca login, navegue pelo fluxo normal, capture requisicoes, compare respostas JSON e anote cada pista. A cadeia depende de correlacao entre frontend, APIs, tokens, servicos internos e worker.

Ferramentas uteis:

- DevTools do navegador
- Burp Proxy e Repeater
- Inspector de JWT
- `curl`
- Comparador de respostas JSON

## Dicas Graduais

### 1. DevTools

Comece pelo navegador. Abra Network, Application/Storage e Sources. Observe cookies, chamadas de API, codigos de status e diferencas entre o que a interface mostra e o que a API retorna.

### 2. JS publico

Procure o bundle JavaScript servido pela aplicacao. Ele pode revelar rotas, parametros esperados e nomes de funcionalidades que nao aparecem diretamente no dashboard.

Perguntas uteis:

- Quais endpoints aparecem no JavaScript?
- Algum endpoint parece exigir uma role diferente?
- Existem referencias a integracoes, jobs ou diagnosticos?

### 3. Comparacao de alertas

Liste os alertas visiveis e depois abra detalhes individuais. Compare a regra da listagem com a regra da leitura por objeto.

O que observar:

- IDs sequenciais ou previsiveis
- Campos omitidos na lista e presentes no detalhe
- Alertas que parecem pertencer a outro contexto

### 4. Alteracao de IDs

Quando uma rota usa um ID na URL, teste variacoes com cuidado. A pergunta central e: a API valida apenas se o objeto existe ou tambem valida se o usuario pode acessa-lo?

### 5. Campos extras em JSON

O endpoint de perfil recebe JSON. Envie primeiro campos esperados, depois campos extras controlados. Observe quais chaves sao ignoradas, rejeitadas ou persistidas.

### 6. Mudanca de role

Se uma resposta indicar mudanca de permissao, confira novamente sua identidade pela API. Verifique tambem se o navegador ou o `curl` guardaram o cookie novo depois da alteracao.

### 7. JWT

Inspecione o cookie de sessao. Entenda header, payload, algoritmo e claims. Se algum artefato revelar material de assinatura, pense no impacto sobre roles e escopos.

### 8. Vazamentos em debug/artefatos

Rotas de diagnostico e artefatos antigos costumam ser feitas para suporte interno, mas podem expor configuracoes sensiveis. Compare o que muda antes e depois de obter uma role mais forte.

### 9. SSRF

Se uma integracao aceita uma URL, teste primeiro um destino simples e controlado. Depois pense na diferenca entre o que seu navegador acessa e o que a aplicacao consegue acessar a partir da rede Docker.

### 10. Servico interno

Servicos sem porta publicada ainda podem ser alcancaveis por outros containers. Procure nomes, portas e caminhos sugeridos por mensagens, JavaScript publico, artefatos ou respostas de debug.

### 11. Proxy com headers

Um proxy interno com headers controlaveis muda bastante o impacto de um SSRF. Correlacione qualquer token interno descoberto com endpoints que aceitam cabecalhos enviados pelo usuario.

### 12. Jobs/worker

Mapeie a fila, os tipos de job, os campos aceitos e onde o worker escreve os resultados. A aplicacao principal pode ter um endpoint para recuperar esses outputs.

### 13. Primitiva de leitura final

A area administrativa indica onde procurar a etapa final. Se encontrar uma leitura de arquivo com filtro de traversal, raciocine sobre a ordem entre validacao, decode e resolucao do caminho.

## Regras de Seguranca

Mantenha tudo dentro do ambiente local autorizado:

```text
http://127.0.0.1:8094
```

Nao use ataques destrutivos, reverse shell ou qualquer alvo fora do lab.
