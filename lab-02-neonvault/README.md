# Lab 2 — NeonVault: Cyber Identity Breach

## Historia

NeonVault e um painel futurista de identidade digital usado pela corporacao NeonCore em Neo-Sampa 2099. O sistema mistura componentes modernos com adaptadores legados: recuperacao de acesso, webhooks, templates, uploads, APIs internas e arquivos esquecidos em backup legado.

O jogador assume o papel de um estagiario de seguranca autorizado a testar o ambiente local e precisa comprometer o nucleo administrativo do vault, coletando evidencias de impacto ao longo do caminho.

## Aviso de uso autorizado

Este laboratorio foi criado apenas para execucao local e educacional.

Nao exponha este projeto na internet. Nao use payloads, tecnicas ou comandos deste lab contra sistemas reais, terceiros ou ambientes sem permissao explicita.

## Stack

- Node.js
- Express
- EJS
- JWT
- Multer
- Armazenamento local simples em memoria e arquivos
- Docker Compose

## Como rodar localmente

O metodo recomendado e Docker. Nao e necessario ter `npm` instalado no host: as dependencias sao instaladas dentro do container durante o build.

```bash
cd lab-02-neonvault
docker compose up --build
```

Para parar:

```bash
docker compose down
```

URL principal:

```text
http://127.0.0.1:8092
```

O app tambem inicia um servico interno em `127.0.0.1:5000`, usado somente para o exercicio de SSRF. A porta interna nao e publicada pelo Docker Compose.

Opcionalmente, com Node.js e npm instalados no host:

```bash
cd lab-02-neonvault
npm install
npm start
```

## Credencial inicial

```text
nova / nova2099
```

## Rotas principais

- `/`
- `/login`
- `/recover`
- `/dashboard`
- `/profile`
- `/logs`
- `/files`
- `/download?file=`
- `/admin/core`
- `/messages/preview`
- `/avatar`
- `/tools/webhook`
- `/api/check-user`
- `/api/tickets/:id`

## Objetivo do jogador

Explorar oito vulnerabilidades diferentes, sem repetir o fluxo simples do Lab 1:

- Blind SQL Injection em recuperacao/verificacao de usuario
- JWT com segredo fraco
- SSRF em testador de webhook
- SSTI em preview de mensagens
- Bypass de upload de avatar/assets
- SQL Injection em filtro de logs
- IDOR em API de tickets
- Path Traversal guiado por logs e backup legado

As solucoes completas e flags ficam em `SOLUTIONS.md`.
