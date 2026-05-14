# Lab 05 - BlackGate

**BlackGate** é um laboratório CTF local sobre uma plataforma corporativa de acesso, auditoria e operações internas. A aplicação simula uma console moderna usada para controlar tickets de segurança, ativos internos, alertas operacionais e revisões de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 6 — Credential Reuse / Legacy Panel Labyrinth**.

Porta pública:

```text
http://localhost:8096
```

Nesta fase, o lab adiciona uma cadeia mais difícil de credential reuse em um painel legado simulado. O objetivo é correlacionar sinais de migração, controles operacionais, artefatos internos e realm separado sem receber um mapa completo da aplicação. Tudo continua local e controlado em memória.

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
- Entender diferenças entre sessão web, role e contexto operacional.
- Entender riscos de tokens legados fracos.
- Explorar confiança de gateway de forma simulada e segura.
- Inferir serviços internos permitidos sem fazer request real para a internet.
- Investigar diferenças entre catálogo, registros nomeados e referências legadas.
- Entender riscos de canonicalização fraca em fluxos de compatibilidade.
- Diferenciar identidade pública, contexto de gateway e realm legado de manutenção.
- Reconhecer decoys e credenciais obsoletas em arquivos de migração.

## Funcionalidades atuais

- Login com sessão.
- Logout.
- Dashboard autenticado.
- Página `/context` sobre contexto operacional.
- Página `/gateway` sobre gateway trust.
- Página `/legacy` sobre lockdown do painel legado.
- Página `/files-vault` sobre migração de documentos.
- Lista de tickets de segurança.
- Inventário de ativos internos fictícios.
- Endpoint `/health` com status JSON.
- Endpoints públicos de recon e metadados limitados.
- Diagnóstico limitado.
- APIs autenticadas para tickets e assets.
- Controles operacionais simulados.
- Gateway simulado para upstreams internos em allowlist.
- Files Vault simulado com metadata, catálogo e leitura controlada por path.
- Legacy Panel simulado com autenticação de manutenção separada e decoys.
- Página pública de política de segurança fictícia.
- CSS próprio com identidade visual amarela e tema BlackGate.

## Rotas principais

```text
/login
/dashboard
/context
/gateway
/legacy
/files-vault
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

A Fase 6 introduz credential reuse em um painel legado simulado. Ela não implementa command injection, upload, Redis, worker, shell, endpoint admin final, pivot real, banco externo ou chamadas externas. A etapa de credencial é limitada a artefatos fictícios do lab e não deve ser usada contra sistemas reais.
