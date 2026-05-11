# Walkthrough - Lab 03 NetAudit

Este guia acompanha uma resolucao manual pelo navegador, DevTools e Burp Suite. O objetivo e treinar investigacao: observar o comportamento normal, modificar requisicoes com cuidado, correlacionar pistas e so entao confirmar impacto.

## 1. Acessar o lab

Abra o alvo local no navegador:

```text
http://127.0.0.1:8090
```

Observe a pagina inicial como se fosse um sistema interno comum.

## 2. Fazer login

Use a credencial de laboratorio indicada na tela. Depois do login, confirme que voce chegou ao dashboard.

## 3. Observar dashboard de ativos

O dashboard mostra ativos, status e historico recente. Nao ha campo livre de host ou IP, nem atalhos visiveis para areas internas.

## 4. Clicar em Verificar

Clique em "Verificar" em um ativo. A tela deve mostrar uma saida tecnica em formato de diagnostico.

## 5. Abrir DevTools Network ou Burp Proxy

Repita a verificacao com DevTools Network aberto ou com o Burp Proxy interceptando o navegador.

## 6. Observar a requisicao POST enviada

Procure a chamada enviada pela acao do botao. Anote metodo, caminho, headers e corpo JSON.

## 7. Identificar assetId, checkType e target

No corpo da requisicao, identifique `assetId`, `checkType` e `target`. Esses campos descrevem o ativo, o tipo de rotina e o alvo enviado pelo frontend.

## 8. Perceber que target nao e editavel na UI, mas esta no JSON

A tela nao oferece um input para o alvo. Mesmo assim, o alvo viaja no JSON, o que permite testar se o backend confia no cliente.

## 9. Enviar requisicao para Burp Repeater

Envie a requisicao para o Repeater. Mantenha a versao original por perto para comparar comportamento normal e comportamento alterado.

## 10. Alterar target para um teste inofensivo

Modifique somente `target` e use um comando de prova que nao altera o sistema. A ideia e verificar execucao, nao buscar dados sensiveis de primeira.

## 11. Confirmar execucao de comando

Compare a resposta. Se a saida do comando de prova aparecer no retorno tecnico, voce confirmou que o servidor executou algo alem da rotina esperada.

## 12. Usar a falha para ler a primeira flag

Depois da prova inofensiva, use o mesmo ponto de entrada para ler o arquivo da primeira etapa. A flag nao aparece automaticamente durante a prova; ela depende de leitura explicita.

## 13. Inspecionar JS

Abra DevTools Sources e leia o JavaScript carregado pelo dashboard. Procure constantes, comentarios e referencias que nao estejam expostas como botoes.

## 14. Descobrir resolver legado

Uma pista discreta aponta para um resolver legado. Como ele nao aparece na interface, a proxima requisicao precisa ser montada manualmente.

## 15. Montar requisicao manual para resolver no Burp Repeater

No Repeater, crie uma requisicao com JSON simples e mantenha o cookie de sessao do login. Comece com uma entrada normal para entender a resposta.

## 16. Testar filtro fraco

Teste um operador que deve ser bloqueado e compare com outros operadores de shell. O objetivo e perceber que bloquear um caractere nao elimina a classe de problema.

## 17. Usar bypass para capturar flag2

Com o bypass confirmado por um comando inofensivo, use a mesma falha para capturar a segunda flag.

## 18. Inspecionar HTML/JS para descobrir support.html

Volte ao HTML do dashboard e ao JavaScript. Comentarios de manutencao podem revelar uma pagina movida apos incidente.

## 19. Acessar support.html

Acesse manualmente a pagina descoberta no navegador. Ela deve parecer uma ferramenta interna simples de diagnostico.

## 20. Ler app.log

Use o campo `Log file` com o valor padrao e clique em `View`. Leia o retorno como log operacional, nao como texto de tutorial.

## 21. Correlacionar pistas

Procure uma rota interna de health, o nome do header esperado, partes de token, mencao a healthcheck interno e evidencia arquivada fora do diretorio de dados.

## 22. Montar token por partes

Junte prefixo, meio e sufixo na ordem indicada pelos logs. Guarde tambem o nome exato do header.

## 23. Explorar traversal no campo de log file

Teste se o campo aceita sair do diretorio padrao usando caminhos relativos. O retorno deve continuar como texto puro.

## 24. Obter flag3 e flag4

Use a falha de traversal para acessar os dois arquivos de evidencia indicados pelo lab.

## 25. Chamar endpoint interno com header

No Burp Repeater, chame o health interno. Primeiro confirme o `403` sem header; depois repita com o header e token montados.

## 26. Descobrir backup interno

Leia os metadados retornados pelo health. Eles indicam o endpoint, metodo e parametro esperado para uma rotina interna.

## 27. Montar requisicao POST manual para backup

Crie manualmente a requisicao no Repeater com JSON e o header de suporte correto. Teste primeiro um valor normal.

## 28. Explorar archiveName

Altere `archiveName` para confirmar se o parametro e concatenado em um comando. Prove impacto antes de buscar as flags finais.

## 29. Capturar flag5 e root

Use a falha no parametro de archive para ler o arquivo diagnostico informado pelo health e, depois, o arquivo final.

## 30. Fechar com mitigacao

As correcoes passam por nao confiar em parametros tecnicos vindos do cliente, evitar shell com concatenacao, validar entradas por allowlist, nao vazar segredos em logs, restringir endpoints internos e retornar somente metadados necessarios.

## Dicas progressivas

1. Se a tela nao deixa editar um valor, olhe a requisicao que ela envia.
2. Se o backend recebe um alvo tecnico do cliente, teste se ele recalcula ou confia.
3. Prove execucao com comandos inofensivos antes de ler arquivos.
4. Comentarios HTML e constantes JS podem ser pistas, mesmo sem link visivel.
5. Logs operacionais podem revelar contexto suficiente para montar a proxima requisicao.
6. Filtros baseados em bloquear um unico caractere costumam deixar outros operadores disponiveis.
