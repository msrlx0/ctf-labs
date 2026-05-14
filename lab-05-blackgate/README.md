# Lab 05 - BlackGate

**BlackGate** é um laboratório CTF local sobre uma plataforma corporativa de acesso, auditoria e operações internas. A aplicação simula uma console moderna usada para controlar tickets de segurança, ativos internos, alertas operacionais e revisões de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 2 — Recon & Metadata Exposure**.

Porta pública:

```text
http://localhost:8096
```

Nesta fase, o objetivo é enumerar a aplicação, ler HTML/JS público, observar metadados, diferenciar endpoints públicos de rotas autenticadas e correlacionar tickets, assets e APIs. A cadeia completa de exploração ainda não está implementada.

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
- Praticar navegação manual em painel autenticado.
- Observar cookies de sessão e controle básico de acesso.
- Comparar interface, HTML, JavaScript público e APIs JSON.
- Identificar inconsistências leves sem transformar a Fase 2 em exploração final.

## Funcionalidades da Fase 2

- Login com sessão.
- Logout.
- Dashboard autenticado.
- Lista de tickets de segurança.
- Inventário de ativos internos fictícios.
- Endpoint `/health` com status JSON.
- Endpoints públicos de recon e metadados.
- Endpoint debug limitado.
- APIs autenticadas para tickets e assets.
- Página pública de política de segurança fictícia.
- CSS próprio com identidade visual amarela e tema BlackGate.

## Rotas principais

```text
/login
/dashboard
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
/api/tickets/:id
/api/assets/:hostname
/debug/ping
/logout
```

## Observações

A Fase 2 prepara a base de enumeração e exposição controlada de metadados. Algumas respostas indicam serviços internos, rotas planejadas e componentes legados, mas não há flag final, senha admin ou cadeia completa nesta etapa.
