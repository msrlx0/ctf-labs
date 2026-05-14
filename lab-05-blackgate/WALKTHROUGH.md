# Lab 05 - BlackGate - Walkthrough da Fase 2

Este walkthrough cobre a **Fase 2 — Recon & Metadata Exposure** do Lab 05. A aplicação ainda não implementa a cadeia completa de exploração; esta fase valida base visual, sessão, navegação, enumeração, metadados e uma inconsistência leve de autorização.

## Como subir

```bash
cd lab-05-blackgate
docker compose up --build
```

Acesse:

```text
http://localhost:8096
```

## Validação de saúde

Abra:

```text
http://localhost:8096/health
```

Resultado esperado:

```json
{
  "service": "blackgate",
  "status": "ok",
  "version": "1.1.0-phase2"
}
```

## Login

Abra `http://localhost:8096`. A aplicação deve redirecionar para `/login`.

Use uma conta comum:

```text
operator / operator123
```

Também existem contas comuns para `analyst` e `guest`. A conta administrativa está presente no cenário, mas não é liberada na Fase 2.

Resultado esperado:

- login bem-sucedido;
- redirecionamento para `/dashboard`;
- topo com usuário, role e link de logout.

## Enumeração pública

Antes ou depois do login, abra:

```text
http://localhost:8096/robots.txt
http://localhost:8096/.well-known/security.txt
http://localhost:8096/api/status
http://localhost:8096/api/version
http://localhost:8096/api/client-config
http://localhost:8096/api/routes
```

O que observar:

- `robots.txt` cita caminhos planejados como `/debug`, `/legacy`, `/api/internal` e `/backups`.
- `security.txt` aponta para `/security-policy`.
- `/api/status` mostra gateway degradado em ambiente de treinamento.
- `/api/version` informa `1.1.0-phase2` e build `bg-phase2-recon`.
- `/api/client-config` expõe configuração pública sem segredo real.
- `/api/routes` mistura rotas públicas, autenticadas e planejadas.

## Página security-policy

Abra:

```text
http://localhost:8096/security-policy
```

Resultado esperado:

- página visual pública usando o tema BlackGate;
- política fictícia de reporte;
- reforço de que o escopo é local.

## Debug ping limitado

Abra:

```text
http://localhost:8096/debug/ping
```

Resultado esperado:

```json
{
  "pong": true,
  "debug": false,
  "message": "Debug interface is disabled in production profile."
}
```

Depois envie a mesma rota com o header:

```text
X-Debug-Token: guest-debug
```

Resultado esperado:

```json
{
  "pong": true,
  "debug": true,
  "message": "Debug handshake accepted for limited diagnostics.",
  "diagnostics": {
    "routes": ["/debug/ping", "/debug/trace"],
    "note": "Full trace requires elevated operator context."
  }
}
```

`/debug/trace` não é funcional nesta fase e retorna que ainda não está implementado.

## Dashboard

Em `/dashboard`, valide:

- usuário logado;
- role atual;
- cards de métricas;
- `Gateway Status: degraded`;
- `Metadata Sync: pending`;
- `Legacy Migration: scheduled`;
- eventos recentes.

Também existem comentários HTML discretos sobre migração legada e uso de hostnames como identificadores de inventário.

## Tickets e API de tickets

Abra:

```text
http://localhost:8096/tickets
```

Observe os links discretos `API view`.

Com uma sessão ativa, acesse exemplos como:

```text
http://localhost:8096/api/tickets/BG-1001
http://localhost:8096/api/tickets/BG-1004
http://localhost:8096/api/tickets/BG-1005
```

O comportamento esperado:

- sem login, a API retorna `authentication_required`;
- tickets permitidos para a role retornam dados completos;
- tickets com `exposure: metadata` podem retornar metadados limitados mesmo quando a role não deveria ver o objeto completo;
- tickets restritos sem exposição de metadata retornam `forbidden`.

Exemplo didático com `guest`:

- `BG-1001` retorna informação permitida;
- `BG-1004` ou `BG-1005` retornam metadados limitados;
- `BG-1002` tende a retornar `forbidden`.

Isso cria uma falha leve no estilo IDOR/BOLA, mas sem flag, senha ou exploração final.

## Assets e API de assets

Abra:

```text
http://localhost:8096/assets
```

A interface mostra assets conforme o contexto da role. Em seguida, teste hostnames diretamente pela API:

```text
http://localhost:8096/api/assets/api-core.internal
http://localhost:8096/api/assets/files-vault.internal
http://localhost:8096/api/assets/legacy-panel.internal
```

O comportamento esperado:

- sem login, a API retorna `authentication_required`;
- com login, qualquer usuário autenticado consegue consultar metadados por hostname;
- a resposta contém `hostname`, `type`, `environment`, `status`, `exposure` e `notes`;
- nenhum serviço interno real é acessado.

## JavaScript público

Abra DevTools > Sources ou acesse:

```text
http://localhost:8096/static/js/app.js
```

Observe:

- `BlackGateClient`;
- `BLACKGATE_CONFIG`;
- `apiBase`;
- rotas de status, version, client config e routes;
- hints como `/debug`, `/legacy` e `/api/assets/{hostname}`.

Não há segredo real, senha admin ou flag final nesse arquivo.

## Logout

Abra:

```text
http://localhost:8096/logout
```

Resultado esperado:

- sessão encerrada;
- redirecionamento para `/login`;
- `/dashboard`, `/api/tickets/:id` e `/api/assets/:hostname` voltam a exigir autenticação.

## O que deve ficar para fases futuras

A Fase 2 não implementa SSRF funcional, JWT explorável, command injection, upload, path traversal, fila real, banco externo, flag final ou exploração de admin.

Ela prepara o terreno para a próxima etapa: token fraco ou role escalation controlada.
