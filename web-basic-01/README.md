# web-basic-01

## Descricao

`web-basic-01` e o primeiro CTF web basico do repositorio `ctf-labs`.

A aplicacao se chama **MiniBank Internal Portal** e simula um portal bancario interno antigo, usado por funcionarios para consultar contas, relatorios e areas legadas.

O objetivo e treinar enumeracao web e exploracao controlada de quatro vulnerabilidades principais.

## Aviso de uso autorizado/local

Este lab foi criado somente para execucao local e autorizada.

Nao use os comandos, payloads ou tecnicas deste lab contra terceiros, sistemas reais ou ambientes sem permissao explicita.

Todas as flags sao ficticias.

## Stack

- Docker Compose
- Node.js
- Express
- EJS
- MySQL
- CSS simples

## Escopo autorizado

Somente:

- `http://localhost:8080`
- containers criados pelo `docker compose` desta pasta

O MySQL fica acessivel apenas pela rede interna do Docker Compose. A porta `3306` nao e publicada no host.

## Vulnerabilidades principais

Este lab possui exatamente quatro vulnerabilidades principais com flag:

| Vulnerabilidade | Local principal | Flag |
|---|---|---|
| SQL Injection no login | `POST /login` | `FLAG{sqli_capturada}` |
| IDOR em contas | `GET /account/:id` | `FLAG{idor_capturada}` |
| Credencial vazada em arquivo publico | `GET /dev-notes.txt` | `FLAG{credencial_exposta_capturada}` |
| Path Traversal / LFI controlado | `GET /download?file=` | `FLAG{path_traversal_capturada}` |

## Pistas de enumeracao

Estas rotas e artefatos continuam existindo como pistas, mas nao possuem flag propria:

- `/health`
- `/robots.txt`
- comentario HTML na pagina inicial
- `/admin`
- `/backup`

## Como subir

Dentro da pasta `web-basic-01`:

```bash
docker compose up --build
```

Para rodar em segundo plano:

```bash
docker compose up -d --build
```

Para parar:

```bash
docker compose down
```

## Como acessar

```text
http://localhost:8080
```

## Credenciais legitimas para fluxo normal

- `joao` / `joao123`
- `maria` / `maria123`
- `auditor` / `audit2026`

O usuario `admin` existe no banco, mas o caminho esperado para obter sessao admin e explorar a falha de SQL Injection no login.

## Objetivos do aluno/agente

- Enumerar rotas visiveis e pistas publicas
- Identificar tecnologia e comportamento da aplicacao
- Encontrar `robots.txt`, comentario HTML e `dev-notes.txt`
- Explorar SQL Injection no login
- Explorar IDOR em contas
- Reutilizar credencial vazada para validar impacto
- Explorar path traversal controlado
- Coletar as quatro flags
- Produzir relatorio tecnico com evidencias e recomendacoes

## Documentacao

- [WALKTHROUGH.md](./WALKTHROUGH.md): passo a passo didatico com raciocinio, validacao, impacto e correcoes.
- [SOLUTION.md](./SOLUTION.md): gabarito tecnico objetivo com comandos e flags.
