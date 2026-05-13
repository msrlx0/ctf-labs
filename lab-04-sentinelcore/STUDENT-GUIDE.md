# Student Guide - Lab 04 SentinelCore

Este guia orienta a investigacao sem listar flags, sem expor segredos e sem entregar o payload final. Use-o como uma trilha de dicas graduais: avance uma secao por vez e tente validar cada hipotese antes de abrir a proxima.

## Objetivo

Entender como uma cadeia web hard nasce da correlacao entre frontend, APIs, roles, cookies, JWT, servicos internos e processamento assincrono. O lab nao foi feito para cair por uma unica URL escondida.

## Escopo

O unico alvo autorizado no host local e:

```text
http://127.0.0.1:8094
```

Servicos internos fazem parte do cenario, mas nao devem ser acessados diretamente pelo host.

## Ferramentas Recomendadas

- Navegador com DevTools
- Burp Proxy e Repeater
- Inspector de JWT
- `curl`
- Comparador de respostas JSON

## Metodologia Sugerida

Trate o SentinelCore como um painel interno real. Faca login, navegue pelo fluxo normal, capture requisicoes, compare respostas e anote cada pista. Quando uma etapa mudar sua permissao, valide a identidade atual antes de seguir.

Boas perguntas durante o lab:

- O que a interface mostra e o que a API realmente retorna?
- O que muda entre lista e detalhe?
- Qual role parece necessaria para cada acao?
- O cookie mudou depois de uma operacao sensivel?
- Uma resposta de debug ou artefato antigo explica outra etapa?

## Dicas Graduais por Fase

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

Rotas de diagnostico e artefatos antigos costumam ser feitas para suporte interno, mas podem expor configuracoes sensiveis. Compare o que muda antes e depois de obter uma role mais forte, e pense em como uma pista parcial pode completar outra.

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

## Quando Pedir Ajuda

Peca uma dica se voce ja comparou as respostas principais, inspecionou o JavaScript publico, testou variacoes de ID, revisou o cookie atual e ainda nao sabe qual permissao falta. Uma boa pergunta e "qual artefato eu deveria correlacionar agora?", sem pedir o payload pronto.

## O Que Evitar

- Nao pule direto para brute force ou scanners barulhentos.
- Nao use ataques destrutivos, reverse shell ou qualquer alvo fora do lab.
- Nao trate cada etapa como isolada; quase todas dependem de uma pista anterior.
- Nao assuma que uma resposta 403 significa a mesma coisa que uma resposta de autenticacao ausente.

## Regras de Seguranca

Mantenha tudo dentro do ambiente local autorizado:

```text
http://127.0.0.1:8094
```
