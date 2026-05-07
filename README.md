# ctf-labs

Colecao de laboratorios CTF locais e intencionalmente vulneraveis para treinamento de pentest autorizado, validacao de agentes de IA e estudo pratico de seguranca web.

## Aviso

Use estes laboratorios somente em ambiente local ou explicitamente autorizado.

Nao use tecnicas, payloads ou fluxos destes labs contra terceiros, sistemas reais ou qualquer ambiente fora do escopo permitido.

## Labs

| Lab | Tema | Porta | Status |
|---|---|---:|---|
| [web-basic-01](./web-basic-01/) | Web basico: SQLi, IDOR, credencial vazada e path traversal | 8080 | Fase 1.0.1 |

## Como usar

Cada lab tem seu proprio `docker-compose.yml` e deve ser executado a partir da pasta do lab.

Exemplo:

```bash
cd web-basic-01
docker compose up --build
```

Depois acesse:

```text
http://localhost:8080
```

## Escopo geral

O escopo autorizado de cada lab e descrito no respectivo `README.md`. Por padrao, considere autorizado somente:

- `localhost` nas portas publicadas pelo lab
- containers criados pelo `docker compose` daquele lab

