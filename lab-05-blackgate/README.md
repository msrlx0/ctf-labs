# Lab 05 - BlackGate

**BlackGate** é um laboratório CTF local sobre uma plataforma corporativa de acesso, auditoria e operações internas. A aplicação simula uma console moderna usada para controlar tickets de segurança, ativos internos, alertas operacionais e revisões de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 3 — Weak Token / Role Escalation**.

Porta pública:

```text
http://localhost:8096
```

Nesta fase, o lab adiciona um mecanismo legado de **BG-Context Token** usado por endpoints operacionais. O objetivo é comparar sessão, role, contexto operacional e endpoints de validação, sem liberar ainda admin final, SSRF, file read ou cadeia completa.

## Aviso de uso local

Este laboratório é intencionalmente vulnerável e foi criado apenas para estudo local e educacional. Não exponha os containers na internet. Não use payloads, técnicas ou comandos deste lab contra sistemas reais, terceiros ou ambientes sem permissão explícita.

## Como subir

Dentro do repositório:

```bash
cd lab-05-blackgate
docker compose up --build
```

Depois acesse:

```text
http://localhost:8096
```

Para parar:

```bash
docker compose down
```

## Credenciais comuns

```text
operator / operator123
analyst / analyst123
guest / guest123
```

A conta administrativa existe no cenário, mas não é documentada nem liberada nesta fase.

## Objetivo educacional

- Reconhecer uma aplicação corporativa interna.
- Enumerar rotas públicas e metadados expostos.
- Comparar sessão web, role e contexto operacional.
- Entender riscos de tokens legados fracos.
- Observar endpoints operator-only sem implementar admin final.
- Preparar hipóteses para fases futuras de gateway trust e SSRF controlado.

## Funcionalidades atuais

- Login com sessão.
- Logout.
- Dashboard autenticado.
- Página `/context` sobre BG-Context Token.
- Lista de tickets de segurança.
- Inventário de ativos internos fictícios.
- Endpoint `/health` com status JSON.
- Endpoints públicos de recon e metadados.
- Endpoint debug limitado.
- APIs autenticadas para tickets e assets.
- Endpoints de contexto e operator metadata.
- Página pública de política de segurança fictícia.
- CSS próprio com identidade visual amarela e tema BlackGate.

## Rotas principais

```text
/login
/dashboard
/context
/tickets
/assets
/health
/robots.txt
/.well-known/security.txt
/security-policy
/api/status
/api/version
/api/routes
/api/client-config
/api/context/me
/api/context/verify
/api/operator/briefing
/api/operator/gateway-metadata
/api/tickets/:id
/api/assets/:hostname
/debug/ping
/logout
```

## Observações

A Fase 3 introduz uma falha educacional de contexto legado fraco. Ela não implementa senha admin, endpoint admin final, SSRF real, path traversal, command injection, upload, Redis, worker ou pivot real.
