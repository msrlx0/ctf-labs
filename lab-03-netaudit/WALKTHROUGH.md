# Walkthrough - Lab 03 NetAudit

Este guia e investigativo. Ele mostra a ordem de raciocinio e oferece dicas progressivas, sem transformar o lab em uma lista de flags. Priorize navegador, DevTools e Burp Suite; deixe `curl` para o gabarito.

## 1. Acessar aplicacao e fazer login

Abra:

```text
http://127.0.0.1:8090
```

Entre com:

```text
analyst:analyst123
```

Depois do login, note o contexto: a aplicacao parece um painel normal de monitoramento de ativos, sem menu de Admin, Logs ou Flags.

## 2. Entender funcionalidades visiveis

No dashboard, observe a lista de ativos:

- gateway.local;
- dns01.local;
- intranet.local;
- backup01.local.

Tambem ha um formulario chamado "Verificar disponibilidade de ativo", um campo "Host ou IP", um botao "Verificar" e um historico recente. Trate isso como uma ferramenta real de TI: primeiro entenda o comportamento normal.

## 3. Observar a requisicao no DevTools Network

Abra DevTools Network ou Burp Proxy. Envie uma verificacao para `127.0.0.1`.

Observe:

- metodo HTTP;
- URL chamada;
- corpo JSON;
- cookie de sessao;
- formato da resposta.

A interface usa a rota de checagem de host para alimentar o `<pre>` tecnico no dashboard.

## 4. Testar comportamento normal

Teste entradas esperadas:

- um IP local;
- um ativo listado no painel;
- um nome que talvez nao resolva.

O objetivo e construir uma linha de base. Se uma entrada normal falha por DNS, isso nao e necessariamente vulnerabilidade; pode ser apenas comportamento esperado do lab.

## 5. Levantar hipotese de ping no backend

Uma checagem de disponibilidade costuma chamar `ping` no servidor. A pergunta de seguranca e: o backend passa o host como argumento isolado ou concatena a entrada em uma linha de shell?

Dicas:

- respostas com formato de ping sao pistas;
- mensagens de erro de resolucao tambem ajudam;
- se a entrada for concatenada, operadores de shell podem alterar a execucao.

## 6. Provar command injection com comando inofensivo

Teste com cuidado. Use comandos que apenas identificam o processo, como `whoami` ou `id`.

Se o output da verificacao incluir resultado de um comando que voce adicionou, a vulnerabilidade principal foi confirmada. A partir dai, o raciocinio natural e descobrir que arquivos locais o processo consegue ler.

## 7. Procurar arquivos e pistas sem brute force agressivo

Evite varredura pesada. Trabalhe com evidencias:

- nomes de diretorios comuns em apps Docker;
- mensagens retornadas pelo backend;
- arquivos referenciados pela propria aplicacao;
- comportamento de comandos inofensivos.

O objetivo e chegar as flags por investigacao, nao por tentativa cega.

## 8. Inspecionar JS e comentarios HTML

Abra DevTools Sources e tambem View Page Source do dashboard.

Procure por:

- comentarios HTML esquecidos;
- constantes JavaScript nao usadas pela interface;
- referencias a suporte, diagnostico ou recurso legado.

O lab deixa pistas discretas para dois pontos: um resolver legado e uma pagina de diagnostico de suporte.

## 9. Testar endpoint resolver manualmente no Burp

Use Burp Repeater para montar a chamada ao resolver legado descoberto no JavaScript. Reaproveite o cookie de sessao do navegador e mantenha o corpo em JSON, como na requisicao observada no dashboard.

Primeiro teste uma entrada normal, como `localhost`. Depois avalie o filtro:

- um operador pode ser bloqueado;
- outros operadores de shell podem continuar aceitos;
- `&&` e `|` sao boas hipoteses de bypass.

Quando provar execucao com `id`, aplique a mesma ideia usada no check de host para ler a segunda flag.

## 10. Acessar support.html

Com a pista do HTML/JS, acesse a pagina escondida de suporte. Ela nao esta no menu principal e nao tem atalhos para Admin ou Backup.

Visualmente, ela parece uma ferramenta interna simples de diagnostico. O campo padrao e `app.log`, e o botao chama um endpoint de leitura de logs.

## 11. Ler logs

Carregue `app.log` e leia como um analista de incidentes.

Procure:

- endpoint interno de health;
- nome do header esperado;
- partes de token;
- mencao a backup;
- indicio de evidencia fora do diretorio de dados;
- mencao ao resolver legado.

Os logs entregam pistas, nao flags diretamente.

## 12. Juntar token por partes

O token nao aparece inteiro em uma linha. Ele e dividido em prefixo, meio e sufixo.

Junte as partes na ordem indicada e use o nome do header encontrado no log. Essa e a falha de controle de acesso: uma rota interna depende de um segredo estatico que vazou por logs.

## 13. Usar path traversal fora de /app/data

O log viewer recebe um nome de arquivo. Teste primeiro arquivos esperados, como `system.log` e `audit.log`.

Depois teste a hipotese de traversal com `../`, tentando sair de `/app/data`. O retorno deve ser somente o conteudo do arquivo, sem mensagem extra.

Esse mesmo ponto cobre Information Disclosure e Path Traversal.

## 14. Chamar /api/internal/health com header correto

Com o token reconstruido, chame:

```text
GET /api/internal/health
```

Inclua o header indicado nos logs. Sem o token correto, a resposta deve ser `403`.

Com o token certo, a resposta revela metadados internos, mas nao retorna flag diretamente.

## 15. Descobrir /api/internal/backup

Na resposta do health, observe:

- endpoint de backup;
- metodo HTTP;
- parametro exigido;
- arquivo diagnostico relacionado.

Essas informacoes indicam como montar a proxima requisicao manualmente.

## 16. Explorar backup command injection

O endpoint de backup recebe um nome de arquivo. A hipotese agora deve soar familiar: se esse nome for concatenado em um comando `tar`, metacaracteres podem encadear outro comando.

Comece com o valor normal para entender a resposta. Depois prove execucao com comando inofensivo e use as pistas do health para ler o arquivo diagnostico. Em seguida, procure a flag final.

## 17. Conclusao e mitigacao

O lab encadeia vulnerabilidades comuns em um fluxo realista:

- input concatenado em shell;
- filtro fraco por blacklist;
- comentario/JS expondo superficie escondida;
- logs vazando rotas e partes de token;
- path traversal por validacao incompleta;
- rota interna protegida por token estatico;
- backup administrativo vulneravel a command injection.

Mitigacoes:

- usar `execFile` ou `spawn` com argumentos separados;
- validar host/IP por allowlist;
- nao tentar corrigir command injection apenas bloqueando caracteres;
- nao vazar tokens em logs;
- validar caminho final dentro do diretorio permitido;
- exigir autenticacao e autorizacao reais em rotas internas;
- remover endpoints debug/suporte de producao.
