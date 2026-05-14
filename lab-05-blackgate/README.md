# Lab 05 - BlackGate

**BlackGate** e um laboratorio CTF local sobre uma plataforma corporativa de acesso, auditoria e operacoes internas. A aplicacao simula uma console moderna usada para controlar tickets de seguranca, ativos internos, alertas operacionais e revisoes de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 8 - Maintenance Worker Processing Abuse**.

Porta publica:

```text
http://localhost:8096
```

Nesta fase, o lab expande a cadeia da fila de reports com processamento inseguro simulado no maintenance worker. O objetivo e correlacionar o job aceito na fase anterior, metadados de revisao, notas internas e comportamento de diagnosticos sem receber um mapa completo da aplicacao.

Tudo continua local e controlado em memoria.

## Aviso de uso local

Este laboratorio e intencionalmente vulneravel e foi criado apenas para estudo local e educacional. Nao exponha os containers na internet. Nao use payloads, tecnicas ou comandos deste lab contra sistemas reais, terceiros ou ambientes sem permissao explicita.

## Como subir

Dentro do repositorio:

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

A conta administrativa existe no cenario, mas nao e documentada nem liberada nesta fase.

## Objetivo educacional

- Reconhecer uma aplicacao corporativa interna.
- Enumerar rotas publicas e metadados expostos.
- Entender diferencas entre sessao web, role e contexto operacional.
- Entender riscos de tokens legados fracos.
- Explorar confianca de gateway de forma simulada e segura.
- Inferir servicos internos permitidos sem fazer request real para a internet.
- Investigar diferencas entre catalogo, registros nomeados e referencias legadas.
- Entender riscos de canonicalizacao fraca em fluxos de compatibilidade.
- Diferenciar identidade publica, contexto de gateway e realm legado de manutencao.
- Reconhecer decoys e credenciais obsoletas em arquivos de migracao.
- Identificar abuso de workflow legado de reports sem execucao real de worker.
- Diferenciar preview, criacao de job, fila e processamento assincrono.
- Avaliar validacao fraca de acoes de diagnostico em um processador simulado.

## Funcionalidades atuais

- Login com sessao.
- Logout.
- Dashboard autenticado.
- Pagina `/context` sobre contexto operacional.
- Pagina `/gateway` sobre gateway trust.
- Pagina `/legacy` sobre lockdown do painel legado.
- Pagina `/files-vault` sobre migracao de documentos.
- Lista de tickets de seguranca.
- Inventario de ativos internos ficticios.
- Endpoint `/health` com status JSON.
- Endpoints publicos de recon e metadados limitados.
- Diagnostico limitado.
- APIs autenticadas para tickets e assets.
- Controles operacionais simulados.
- Gateway simulado para upstreams internos em allowlist.
- Files Vault simulado com metadata, catalogo e leitura controlada por path.
- Legacy Panel simulado com autenticacao de manutencao separada e decoys.
- Workflow legado de reports com templates, previews e fila simulada em memoria.
- Maintenance worker simulado em modo review, sem processo externo ou shell.
- Pagina publica de politica de seguranca ficticia.
- CSS proprio com identidade visual amarela e tema BlackGate.

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

## Observacoes

A Fase 8 introduz worker processing abuse simulado no painel legado. Ela nao implementa command injection real, upload, Redis, worker separado, shell, endpoint admin final, pivot real, banco externo, leitura de filesystem real ou chamadas externas. O processamento de diagnosticos e apenas uma simulacao controlada em memoria.
