# Walkthrough - Lab 03 NetAudit

Este guia foi escrito para uma pessoa resolvendo o lab pelo navegador, DevTools e Burp Suite. Ele evita virar uma lista de comandos prontos: a ideia e observar o sistema, montar hipoteses, repetir requisicoes manualmente e confirmar impacto com cuidado.

## 1. Login

Abra o lab no navegador:

```text
http://127.0.0.1:8090
```

Entre com a credencial de laboratorio indicada na tela. Depois do login, confirme que voce chegou ao dashboard de ativos.

## 2. Observar dashboard de ativos

Leia a tela como uma ferramenta interna de monitoramento. O fluxo principal gira em torno de ativos com status, hostname e um botao de verificacao.

Repare que nao existe um campo livre para digitar host ou IP. Isso e importante: a interface parece controlar os alvos, mas o navegador ainda precisa enviar dados para o backend.

## 3. Clicar em Verificar

Escolha um ativo, por exemplo o gateway, e clique em "Verificar". Observe o resultado tecnico renderizado na tela.

Nesta etapa, nao tente explorar nada. Entenda primeiro qual e o comportamento normal e que tipo de retorno a aplicacao mostra.

## 4. Interceptar a requisicao

Abra DevTools Network ou configure o Burp Proxy e repita a verificacao.

No DevTools, filtre por chamadas `fetch` ou `XHR`. No Burp, capture a chamada e envie para o Repeater para conseguir modificar e reenviar a mesma requisicao.

## 5. Identificar assetId, checkType e target

Na requisicao de verificacao, observe o corpo JSON. Ele deve conter tres valores relevantes:

- `assetId`: o ativo selecionado;
- `checkType`: o tipo de verificacao;
- `target`: o hostname enviado pelo frontend.

O detalhe mais interessante e o `target`: ele existe na requisicao, embora nao seja editavel diretamente pela tela.

## 6. Entender que target nao e editavel na tela, mas e confiado pelo backend

A pergunta de investigacao e simples: o servidor recalcula o alvo a partir do `assetId`, ou confia no `target` recebido do cliente?

Se o backend confiar no campo enviado pelo navegador, alterar esse valor no Repeater pode mudar o comando executado no container.

## 7. Alterar target no Repeater com comando inofensivo

No Burp Repeater, mantenha `assetId` e `checkType` como estavam e altere apenas `target`.

Use primeiro uma prova inofensiva, como anexar um comando que apenas identifique o usuario do processo. Esse tipo de teste confirma execucao sem modificar o sistema e sem buscar flag automaticamente.

## 8. Confirmar command injection

Compare a resposta normal com a resposta modificada. Se a saida do comando adicional aparecer no campo de resultado, voce confirmou que o backend concatena o valor de `target` em um comando de sistema.

Anote o ponto de entrada, o parametro afetado e o operador que funcionou. Essa confirmacao deve vir antes de qualquer leitura de arquivo.

## 9. Usar a falha para ler a primeira flag

Com o impacto confirmado, use o mesmo ponto para ler o arquivo de flag correspondente a primeira falha.

A aplicacao nao entrega a flag quando voce usa `whoami`, `id` ou qualquer outra prova de execucao. Ela so aparece se a requisicao modificada ler explicitamente o arquivo correto no container.

## 10. Inspecionar JS e comentarios HTML

Volte ao navegador e use View Source ou DevTools Sources. Leia o HTML do dashboard e o JavaScript carregado pela pagina.

Procure pistas discretas: comentarios de manutencao, constantes nao usadas pela interface e referencias a fluxos legados.

## 11. Descobrir resolver legado

O JavaScript deixa uma referencia discreta a um resolver legado de ativos. Ele nao aparece como botao na UI, entao a requisicao precisa ser montada manualmente no Burp Repeater.

Comece enviando uma entrada normal, como um hostname local, para entender o formato da resposta.

## 12. Descobrir support.html

Continue a leitura do HTML e do JavaScript. Ha uma pista de que diagnosticos de suporte foram movidos apos um incidente.

Acesse a pagina indicada manualmente pelo navegador. Ela deve parecer uma ferramenta interna simples, nao uma pagina de CTF.

## 13. Ler app.log

Na pagina de suporte, carregue `app.log` e trate o conteudo como log operacional.

Procure referencias a health interno, nome de header esperado, partes de token, backup exposto apenas por healthcheck e evidencia arquivada fora do diretorio de dados.

## 14. Montar token por partes

O log nao deve entregar uma chave pronta em uma unica linha. Monte o token juntando prefixo, meio e sufixo na ordem indicada.

Guarde tambem o nome exato do header esperado; ele sera necessario para consultar o health interno.

## 15. Explorar path traversal

O viewer de logs aceita um nome de arquivo. Teste primeiro arquivos esperados, como logs existentes, e observe que o retorno e texto puro.

Depois avalie se o parametro permite sair do diretorio de dados com `../`. Essa falha permite acessar evidencias arquivadas fora da pasta de logs.

## 16. Chamar health interno com header

No Burp Repeater, monte uma requisicao para o health interno indicado nos logs. Primeiro confirme que, sem token, o endpoint responde `403`.

Em seguida, adicione o header de suporte com o token montado na etapa anterior. A resposta deve trazer metadados operacionais, mas nao deve retornar flag diretamente.

## 17. Descobrir backup

Leia a resposta do health como uma pista de operacao interna. Ela informa o endpoint de backup, o metodo HTTP e o parametro esperado.

Esse fluxo nao existe na interface. A proxima requisicao precisa ser criada manualmente no Burp Repeater.

## 18. Explorar archiveName no backup

Monte a requisicao de backup com um valor normal primeiro e veja como o backend responde.

Depois altere `archiveName` para provar command injection de forma controlada. Quando o impacto estiver claro, use a mesma falha para ler o arquivo diagnostico indicado pelo health e, por fim, a flag final.

## 19. Conclusao e mitigacao

O lab encadeia confianca indevida em parametros do cliente, uso de shell com concatenacao, filtro fraco, vazamento de informacoes em logs, path traversal e endpoints internos protegidos por token estatico.

Mitigacoes principais:

- recalcular alvos sensiveis no servidor a partir de IDs confiaveis;
- substituir `exec` com strings por `execFile` ou `spawn` com argumentos separados;
- validar hostnames e nomes de arquivo por allowlist;
- evitar tokens estaticos e segredos em logs;
- separar endpoints internos de verdade, com autenticacao e autorizacao adequadas;
- retornar o minimo necessario em diagnosticos operacionais.

## Dicas progressivas

1. Se a tela nao deixa editar um campo, olhe a requisicao que ela envia.
2. Se um parametro parece tecnico demais para vir do cliente, teste se o backend confia nele.
3. Prove execucao com comandos inofensivos antes de ler arquivos.
4. Comentarios HTML e constantes JS podem revelar fluxos que nao aparecem no menu.
5. Logs podem vazar contexto suficiente para montar a proxima requisicao.
6. Um filtro que bloqueia um unico caractere raramente bloqueia a classe inteira de ataque.
