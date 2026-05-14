# Lab 05 - BlackGate

**BlackGate** é um laboratório CTF local sobre uma plataforma corporativa de acesso, auditoria e operações internas. A aplicação simula uma console moderna usada para controlar tickets de segurança, ativos internos, alertas operacionais e revisões de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 4 — Gateway Trust / SSRF Setup**.

Porta pública:

```text
http://localhost:8096
```

Nesta fase, o lab adiciona um gateway interno simulado, acessível somente com contexto operacional. O objetivo é reaproveitar o BG-Context Token da Fase 3 para estudar gateway trust, descoberta de serviços internos e SSRF controlado sem fazer chamadas reais para rede externa.

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
- Explorar gateway trust de forma simulada e segura.
- Descobrir serviços internos permitidos sem fazer request real para a internet.
- Preparar hipóteses para uma futura fase de files-vault e leitura controlada.

## Funcionalidades atuais

- Login com sessão.
- Logout.
- Dashboard autenticado.
- Página `/context` sobre BG-Context Token.
- Página `/gateway` sobre gateway trust.
- Lista de tickets de segurança.
- Inventário de ativos internos fictícios.
- Endpoint `/health` com status JSON.
- Endpoints públicos de recon e metadados.
- Endpoint debug limitado.
- APIs autenticadas para tickets e assets.
- Endpoints de contexto, operator briefing e gateway metadata.
- Gateway fetch simulado para hosts internos em allowlist.
- Página pública de política de segurança fictícia.
- CSS próprio com identidade visual amarela e tema BlackGate.

## Rotas principais

```text
/login
/dashboard
/context
/gateway
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
/api/operator/gateway-fetch
/api/tickets/:id
/api/assets/:hostname
/debug/ping
/logout
```

## Observações

A Fase 4 introduz uma simulação segura de SSRF/gateway trust. Ela não implementa leitura real de arquivos, path traversal, command injection, upload, Redis, worker, senha admin, endpoint admin final, pivot real ou chamadas externas.
