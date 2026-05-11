# Walkthrough - Lab 03 NetAudit

Este guia e pensado para uma pessoa usando navegador, DevTools e Burp Suite. A ideia nao e copiar comandos prontos, e sim treinar investigacao: observar comportamento normal, levantar hipoteses, modificar requisicoes e confirmar impacto.

## 1. Acessar o lab no navegador

Abra:

```text
http://127.0.0.1:8090
```

Leia a pagina inicial como se fosse um sistema interno real de monitoramento.

## 2. Fazer login

Entre com:

```text
analyst:analyst123
```

Depois do login, confirme que voce esta no dashboard de ativos.

## 3. Observar o dashboard de ativos

O painel mostra ativos como Gateway Edge, DNS Resolver, Intranet Portal e Backup Node. Cada ativo tem hostname visivel, status e botao "Verificar".

Nao ha campo livre de host, nem menu de Logs/Admin.

## 4. Clicar em "Verificar" em um ativo normal

Escolha um ativo e clique em "Verificar". Observe a saida tecnica no `<pre>`.

Nesta etapa, apenas entenda o fluxo normal.

## 5. Abrir DevTools Network ou Burp Proxy

Com DevTools Network aberto, repita a verificacao. Com Burp, deixe o proxy ligado e capture a chamada.

Procure o metodo, a rota, os headers e o corpo JSON.

## 6. Observar a requisicao de verificacao

A verificacao envia uma requisicao `POST` para a API de ativos. O corpo inclui campos como:

- `assetId`;
- `checkType`;
- `target`.

O ponto importante e que o `target` esta na requisicao mesmo nao sendo editavel pela UI.

## 7. Entender o risco do target controlado pelo cliente

O dashboard mostra hostnames fixos, mas o backend recebe o `target` enviado pelo navegador. Isso cria uma pergunta: o servidor confia nesse valor ou recalcula o alvo a partir do `assetId`?

Se o servidor confiar no `target`, alterar a requisicao pode mudar o comando executado no backend.

## 8. Enviar para Repeater

No Burp, envie a requisicao de verificacao para o Repeater. Mantenha `assetId` e `checkType` como estavam e altere apenas `target`.

Comece com um alvo controlado e inofensivo para entender a resposta.

## 9. Confirmar execucao com comando inofensivo

Teste uma alteracao pequena no `target` para confirmar se operadores de shell sao interpretados. Comandos como `whoami` ou `id` sao bons para prova, porque nao alteram o sistema.

Se o resultado da resposta incluir a saida desse comando, voce confirmou Command Injection.

## 10. Ler a primeira flag pelo impacto confirmado

Depois de provar a falha, use o mesmo ponto vulneravel para ler arquivos locais dentro do container. A flag nao aparece automaticamente quando voce prova execucao; ela so aparece se a exploracao ler o arquivo correto.

## 11. Inspecionar JavaScript

Abra DevTools Sources e leia o JavaScript carregado pelo dashboard. Procure constantes ou comentarios que indiquem funcionalidade legada.

Ha uma pista discreta sobre um resolver de ativos que nao aparece como botao na interface.

## 12. Montar a requisicao do resolver no Burp

No Burp Repeater, monte uma requisicao manual para o resolver legado descoberto no JS. Use JSON e mantenha o cookie de sessao.

Comece com entrada normal, como `localhost`, para entender a resposta.

## 13. Testar filtro fraco

Teste operadores de shell de forma controlada. Um caractere pode ser bloqueado, mas outros podem continuar aceitos.

Dicas:

- observe se `;` e rejeitado;
- compare com operadores como `&&` e `|`;
- use `id` para provar execucao sem buscar flag ainda.

Depois de confirmar o bypass, use a falha para capturar a segunda flag.

## 14. Descobrir support.html

Inspecione o HTML do dashboard e o JavaScript. Ha uma pista discreta sobre uma pagina de diagnostico de suporte movida apos um incidente.

Acesse essa pagina manualmente pelo navegador.

## 15. Ler app.log

Na pagina de suporte, carregue `app.log`. Leia como se fosse log operacional real.

Procure:

- rota de health interno;
- nome do header esperado;
- partes de um token;
- indicio de backup exposto apenas via health;
- evidencia arquivada fora do diretorio de dados.

Os logs nao devem entregar flags diretamente.

## 16. Usar traversal no campo Log file

O viewer recebe um nome de arquivo. Teste primeiro logs esperados, como `system.log` e `audit.log`.

Depois teste a hipotese de sair do diretorio de logs com `../`. Esse bug permite obter as flags de disclosure/traversal.

## 17. Montar o token de suporte

O token aparece dividido em partes nos logs. Junte prefixo, meio e sufixo na ordem indicada.

Use esse valor no header indicado pelo proprio log.

## 18. Chamar o health interno pelo Burp

No Burp Repeater, monte uma requisicao `GET` para o health interno e adicione o header correto.

Sem o token, a rota deve responder `403`. Com o token certo, ela retorna metadados internos, mas nao retorna flag.

## 19. Descobrir o backup interno

Na resposta do health, observe o endpoint de backup, metodo e parametro esperado.

Esse endpoint nao existe na UI. A proxima etapa precisa ser montada manualmente.

## 20. Explorar o backup

Crie uma requisicao `POST` no Burp para o endpoint de backup, com JSON contendo o nome do arquivo de archive.

Teste primeiro um valor normal. Depois altere o parametro para provar Command Injection e ler o arquivo diagnostico apontado pelo health. Em seguida, busque a flag final.

## 21. Mitigacao

Pontos de correcao:

- nao confiar em `target` vindo do cliente;
- recalcular dados sensiveis no servidor a partir do `assetId`;
- usar `execFile` ou `spawn` com argumentos separados;
- validar allowlist de host/IP;
- nao tentar corrigir Command Injection apenas bloqueando caracteres;
- nao vazar tokens ou rotas sensiveis em logs;
- usar allowlist e validacao de path para log viewer;
- exigir autenticacao e autorizacao reais em endpoints internos.
