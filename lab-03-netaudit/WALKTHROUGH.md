# Walkthrough - Lab 03 NetAudit

Este guia e investigativo. Ele mostra a ordem de raciocinio e oferece dicas progressivas, sem transformar o lab em uma lista de flags. Priorize navegador, DevTools e Burp Suite; deixe `curl` para validacao ou para o gabarito.

## 1. Acesso e contexto

Abra a aplicacao local em:

```text
http://127.0.0.1:8090
```

Entre com a credencial de analista:

```text
analyst:analyst123
```

Depois do login, observe que a aplicacao parece apenas um painel de monitoramento de ativos. Nao ha menu de Admin, Logs ou Flags.

## 2. Entender a funcionalidade visivel

No dashboard, leia os ativos listados:

- gateway.local;
- dns01.local;
- intranet.local;
- backup01.local.

Use o formulario "Verificar disponibilidade de ativo" com uma entrada normal, como `127.0.0.1`. Abra DevTools Network antes de clicar em Verificar.

Voce deve observar uma chamada para:

```text
POST /api/tools/check
```

Analise o corpo JSON, o metodo, os headers e a resposta.

## 3. Levantar hipotese sobre o backend

Uma checagem de disponibilidade normalmente chama algo como ping no backend. A pergunta e: a aplicacao passa o host como argumento seguro ou monta uma linha de comando com string?

Dicas progressivas:

- Teste primeiro entradas normais.
- Compare respostas de IP valido e nome que nao resolve.
- Teste caracteres de shell de forma inofensiva.
- Comandos como `whoami` e `id` sao bons para prova de execucao, porque nao alteram o sistema.

Se o output da checagem incluir resultado de outro comando, voce confirmou Command Injection. A partir dai, pense em como ler arquivos locais dentro do container.

## 4. Procurar pistas no frontend

Agora va alem da interface visivel:

- DevTools Sources;
- View Page Source;
- arquivos JavaScript carregados;
- comentarios HTML;
- historico de requisicoes no Burp.

Procure referencias que parecam sobras de suporte, diagnostico ou recurso legado. O dashboard tem uma pista discreta no HTML e o JavaScript tem referencias que nao aparecem como botoes.

Perguntas uteis:

- Existe alguma pagina de suporte nao linkada?
- Existe endpoint legado que a UI nao chama?
- Que nomes de endpoint combinam com diagnostico/resolver?

## 5. Testar o resolver legado

Ao descobrir a referencia ao resolver legado, monte a requisicao manualmente no Burp Repeater. Use o mesmo cookie de sessao do navegador.

Primeiro envie uma entrada normal, como `localhost`. Depois teste o comportamento do filtro com operadores de shell.

Dicas:

- Um caractere pode ser bloqueado e outros ainda funcionarem.
- `;`, `&&` e `|` nao sao equivalentes, mas todos sao relevantes em shell.
- Se um filtro bloqueia apenas um deles, isso e uma pista de blacklist fraca.

Quando confirmar execucao de comando, use a mesma linha de raciocinio do primeiro Command Injection para procurar a proxima flag.

## 6. Acessar diagnostics de suporte

Pelas pistas do HTML/JS, acesse a pagina de suporte escondida. Ela nao aparece no menu principal.

Nessa pagina, o viewer de logs deve carregar `app.log` por padrao. Intercepte a chamada no Burp e observe que o nome do arquivo vai como parametro de query.

Leia os logs como um analista:

- endpoints movidos;
- nome de header esperado;
- partes de token;
- referencia a backup;
- indicios de arquivo fora do diretorio de logs;
- mencao ao resolver legado.

## 7. Path Traversal no log viewer

O viewer aceita nomes de arquivo. Teste primeiro logs esperados, como `system.log` e `audit.log`.

Depois pense no que acontece quando a aplicacao junta um diretorio base com uma entrada controlada pelo usuario. Se nao houver validacao do caminho final, `../` pode sair do diretorio previsto.

Dicas progressivas:

- Troque apenas o nome do arquivo e observe diferencas.
- Teste subir um diretorio.
- Procure arquivos de flags que os logs indicam estar fora de `/app/data`.

Esse mesmo bug permite demonstrar Information Disclosure e Path Traversal.

## 8. Montar o token de suporte

Os logs nao entregam o token em uma linha unica. Eles separam prefixo, meio e sufixo.

Monte o valor final como um token composto por essas partes e use o header indicado nos logs. Esse e um ponto importante: o controle de acesso depende de um segredo estatico vazado por informacao operacional.

## 9. Chamar o health interno

Com o header correto, chame o endpoint interno indicado nos logs pelo Burp Repeater. A resposta nao deve retornar flag diretamente, mas deve revelar:

- status do servico;
- ambiente;
- endpoint de backup;
- metodo esperado;
- parametro necessario;
- arquivo diagnostico relacionado a flag.

Anote esses detalhes para montar a proxima requisicao.

## 10. Explorar o backup interno

O endpoint de backup recebe um nome de arquivo. A hipotese agora deve soar familiar: se o nome do arquivo for concatenado em um comando `tar`, metacaracteres podem encadear outro comando.

Comece com o valor normal para entender a resposta. Depois teste um comando inofensivo. Por fim, use as pistas do healthcheck para ler o arquivo diagnostico e avance para a flag final.

## 11. Conclusao

O lab encadeia vulnerabilidades comuns:

- input de usuario concatenado em shell;
- filtro fraco por blacklist;
- comentario/JS entregando superficie escondida;
- logs vazando segredos e rotas;
- path traversal por validacao incompleta de caminho;
- rota interna confiando em token estatico;
- backup administrativo com command injection.

Mitigacoes esperadas:

- evitar shell e usar `execFile`/`spawn` com argumentos separados;
- validar host/IP por allowlist;
- nao confiar em blacklist de metacaracteres;
- nao registrar tokens ou rotas sensiveis em logs;
- validar caminho final dentro do diretorio base;
- exigir autenticacao e autorizacao reais para endpoints internos;
- remover recursos debug/suporte de producao.
