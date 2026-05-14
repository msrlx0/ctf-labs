# Lab 05 - BlackGate - Walkthrough da Fase 1

Este walkthrough cobre apenas a **Fase 1** do Lab 05. A aplicação ainda não implementa a cadeia completa de exploração; esta fase existe para validar base, identidade visual, sessão, navegação e pistas iniciais.

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
  "version": "1.0.0-phase1"
}
```

## Login

Abra `http://localhost:8096`. A aplicação deve redirecionar para `/login`.

Use uma conta comum:

```text
operator / operator123
```

Também existem contas comuns para `analyst` e `guest`. A conta administrativa está presente no cenário, mas não é liberada na Fase 1.

Resultado esperado:

- login bem-sucedido;
- redirecionamento para `/dashboard`;
- topo com usuário, role e link de logout.

## Dashboard

Em `/dashboard`, valide:

- usuário logado;
- role atual;
- cards de métricas;
- total de tickets;
- ativos monitorados;
- alertas pendentes;
- operações bloqueadas;
- eventos recentes.

Também existe um comentário HTML discreto indicando migração futura de rotas legadas. Ele é apenas uma pista leve nesta fase.

## Tickets

Abra:

```text
http://localhost:8096/tickets
```

Valide que aparecem tickets fictícios como:

- `BG-1001` — Revisar acesso VPN de fornecedor;
- `BG-1002` — Validar alerta em servidor financeiro;
- `BG-1003` — Investigar falha de autenticação no gateway;
- `BG-1004` — Revisar logs do serviço legacy-files;
- `BG-1005` — Auditoria de tokens internos.

Nesta fase, a tabela é apenas superfície de reconhecimento.

## Assets

Abra:

```text
http://localhost:8096/assets
```

Valide ativos como:

- `gw-blackgate.local`;
- `api-core.internal`;
- `files-vault.internal`;
- `queue-worker.internal`;
- `audit-db.internal`;
- `legacy-panel.internal`.

Esses nomes ajudam a construir o tema do lab, mas ainda não representam serviços exploráveis nesta fase.

## JavaScript público

Abra DevTools > Sources ou acesse:

```text
http://localhost:8096/static/js/app.js
```

Observe o objeto `BlackGateClient`. Ele contém rotas, nomes operacionais e mensagens de Fase 1. Não há segredo real nem cadeia completa implementada nesse arquivo.

## Logout

Abra:

```text
http://localhost:8096/logout
```

Resultado esperado:

- sessão encerrada;
- redirecionamento para `/login`;
- `/dashboard` volta a exigir autenticação.

## O que deve ficar para fases futuras

A Fase 1 não implementa SSRF funcional, JWT explorável, command injection, upload, path traversal, fila real, banco externo, flag final ou exploração de admin.

Ela prepara o terreno narrativo e técnico para próximas fases, mantendo o lab seguro para execução local.
