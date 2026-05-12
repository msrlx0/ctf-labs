# Walkthrough - Lab 03 NetAudit

Este guia acompanha uma resolucao manual pelo navegador, DevTools e Burp Suite. Ele segue o raciocinio de um analista: observar a aplicacao, capturar requisicoes, alterar poucos campos por vez, correlacionar pistas e confirmar impacto.

## 1. Reconhecimento pre-auth

Abra o navegador em:

```text
http://127.0.0.1:8090
```

A tela de login nao fornece credenciais. Antes de testar login aleatoriamente, faca reconhecimento das paginas e arquivos publicos.

Verifique arquivos publicos comuns, como `robots.txt`. Ele lista diretorios que nao deveriam ter sido publicados como pista operacional, nao como controle de acesso.

Acesse:

```text
/old/deployment-notes.txt
```

Identifique o usuario:

```text
username: analyst
```

A nota tambem mostra uma senha temporaria antiga:

```text
analyst2022
```

Teste `analyst2022` no login e confirme que falha. O aviso da propria nota diz que ela esta desatualizada e aponta para uma lista candidata arquivada.

Acesse:

```text
/backup/readme.txt
```

Esse arquivo indica a lista pequena de candidatos:

```text
password-candidates.txt
```

Abra ou baixe a lista. Ela foi criada para validacao controlada dentro do lab local, com poucas senhas candidatas. Nao use wordlists grandes, ataques agressivos ou alvos fora do escopo.

No Burp Suite:

1. Use o Proxy para capturar uma tentativa de login com `username` fixo como `analyst`.
2. Envie o `POST /api/auth/login` para o Intruder.
3. Marque somente o valor do campo `password` como posicao.
4. Carregue as senhas candidatas da lista pequena.
5. Inicie o ataque apenas contra o ambiente local.
6. Compare status code, tamanho da resposta e mensagem.

A tentativa correta retorna `200` e a mensagem:

```text
Login successful
```

Use a credencial encontrada para entrar:

```text
analyst:analyst123
```

O login nao e SQL Injection e nao depende de brute force pesado. Esta etapa e uma fase inicial de information disclosure com validacao manual controlada.

## 2. Dashboard e reconhecimento da aplicacao

Observe os cards de ativos, os hostnames, os status e o historico recente. Clique em `Verificar` em um ativo e veja o resultado tecnico mostrado na tela.

Abra DevTools Network ou configure o Burp Proxy e repita a verificacao. A chamada importante e um `POST /api/assets/check`. No corpo JSON, procure:

```json
{
  "assetId": "gw-01",
  "checkType": "icmp",
  "target": "gateway.local"
}
```

O detalhe central e que `target` nao e editavel na UI, mas e enviado pelo cliente.

## 3. Request tampering

Envie a requisicao para o Burp Repeater. Antes de tentar qualquer exploracao, altere campos com cuidado e mantenha a estrutura JSON valida.

O risco aqui e o backend confiar em um alvo tecnico vindo do navegador. Se o servidor usa `target` diretamente, o usuario pode mudar a operacao executada sem que a tela ofereca um campo para isso.

## 4. Command Injection com output

No Repeater, altere `target` para um teste inofensivo:

```text
127.0.0.1; whoami
```

Se a resposta incluir a saida desse comando, voce confirmou execucao de comando com output visivel. Essa etapa nao deve retornar flag automaticamente; ela e apenas uma prova de impacto.

Depois, use a propria falha para ler a primeira evidencia:

```text
127.0.0.1; cat /app/flags/flag1.txt
```

## 5. Blind/Time-Based Command Injection

Nem toda command injection retorna stdout ou stderr. Use a mesma requisicao `POST /api/assets/check` para testar um modo sem output visivel.

No Burp Repeater, troque o corpo para um check TCP normal:

```json
{
  "assetId": "gw-01",
  "checkType": "tcp",
  "target": "127.0.0.1",
  "port": "80"
}
```

Observe `durationMs`. Depois altere apenas o alvo:

```text
127.0.0.1; sleep 5; #
```

Compare o tempo de resposta e o `durationMs`. Um atraso de aproximadamente 5 segundos confirma execucao por canal de tempo, mesmo sem stdout/stderr. Essa etapa confirma blind command injection; ela nao captura flag automaticamente.

## 6. Inspecao do JavaScript

Abra DevTools Sources ou acesse `/js/app.js` no navegador. Procure referencias discretas, nao links de menu.

Voce deve notar pistas para um resolver legado e para `support.html`. Trate isso como descoberta: esses fluxos existem no codigo, mas nao aparecem como botoes principais da aplicacao.

## 7. Resolver legado e filtro fraco

Monte manualmente no Burp Repeater uma requisicao para o resolver legado descoberto no JavaScript. Ele nao esta na UI.

Comece com entrada normal, depois teste se `;` e bloqueado. Em seguida, compare com um operador permitido, como `&&`, usando um comando inofensivo.

Para capturar a segunda flag, use a falha com:

```text
localhost && cat /app/flags/flag2.txt
```

A licao aqui e simples: bloquear apenas um caractere nao corrige command injection.

## 8. Support diagnostics escondido

A partir das pistas no HTML/JS, acesse `support.html`. Ela nao estava no menu principal.

Use o campo `Log file` para abrir `app.log`. Leia o arquivo como log operacional e procure:

- endpoint interno;
- header esperado;
- token dividido em partes;
- rota de backup;
- evidencia fora do diretorio de dados.

## 9. Path Traversal

O visualizador aceita um nome de arquivo. Teste variacoes controladas e pense no caminho final que o backend pode estar montando.

Use traversal para ler:

```text
../flags/flag3.txt
../flags/flag4.txt
```

A causa raiz e aceitar entrada do usuario em `path.join` sem garantir que o caminho final permaneceu dentro do diretorio permitido.

## 10. Token interno e Broken Access Control

Monte o token a partir das partes vistas nos logs:

```text
netaudit-debug-2026
```

No Burp Repeater, chame `/api/internal/health` com:

```text
X-Support-Token: netaudit-debug-2026
```

Sem token, a resposta deve ser `403`. Com token, o healthcheck retorna metadados. Ele nao retorna flag diretamente, mas revela o endpoint de backup e o arquivo diagnostico.

## 11. Backup command injection

Monte manualmente um `POST` para `/api/internal/backup`, usando o header `X-Support-Token`.

Teste primeiro um body normal com `archiveName`. Depois explore o parametro para ler:

```text
/app/flags/flag5.txt
/app/flags/root.txt
```

Essa e uma command injection em uma funcao administrativa exposta por metadados internos.

## 12. Mitigacoes

Mitigacoes principais:

- nao usar `exec` com concatenacao;
- usar `execFile` ou `spawn` com argumentos separados;
- validar `checkType` por allowlist;
- validar `target` como IP ou hostname esperado;
- validar `port` como numero dentro do intervalo permitido;
- nao confiar em campos enviados pelo cliente;
- nao expor endpoints internos;
- nao usar token estatico;
- nao vazar token em logs;
- validar acesso a arquivos com allowlist e checagem de base directory.

## Dicas progressivas

1. Se a UI nao deixa editar um valor, olhe o JSON da requisicao.
2. Se uma resposta nao mostra output, compare tempos de resposta.
3. Comentarios HTML e constantes JS podem revelar fluxos sem link visivel.
4. Logs operacionais raramente deveriam conter segredos ou rotas internas.
5. Blacklist de caractere unico costuma deixar bypasses simples.
