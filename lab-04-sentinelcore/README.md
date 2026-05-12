# Lab 04 - SentinelCore

SentinelCore e um laboratorio CTF web local que simula um painel interno de SOC usado para acompanhar alertas, evidencias, integracoes, relatorios e jobs assincronos.

O lab foi criado para treinar correlacao de pistas entre frontend, APIs, tokens, servicos internos e worker assincromo. Ele e **Hard**, mas didatico e resolvivel com exploracao manual cuidadosa.

## Escopo

```text
http://127.0.0.1:8090
```

Somente a aplicacao principal fica publicada no host. Redis, internal-admin e worker ficam acessiveis apenas na rede Docker interna.

## Credencial Inicial

```text
intern / intern2026
```

## Como Executar

Dentro desta pasta:

```bash
docker compose up --build
```

Para parar:

```bash
docker compose down
```

Para resetar containers e volume compartilhado:

```bash
docker compose down -v
```

## Servicos

| Servico | Funcao | Exposicao |
|---|---|---|
| sentinel-app | Aplicacao web e API principal | `127.0.0.1:8090` |
| internal-admin | Servico administrativo interno | Somente rede Docker |
| worker | Consumidor de jobs assincronos | Sem porta |
| redis | Fila `sentinel:jobs` | Somente rede Docker |

## Temas Abordados

- BOLA / IDOR em APIs
- Mass Assignment
- JWT forgery com segredo vazado
- Debug disclosure
- Build artifact leak
- SSRF para servico interno
- Proxy interno com headers controlados
- Template context disclosure
- Queue poisoning
- Leitura de output de worker
- Arbitrary File Read com bypass de filtro por encoding

## Materiais

- [STUDENT-GUIDE.md](./STUDENT-GUIDE.md): dicas graduais sem spoiler direto.
- [WALKTHROUGH.md](./WALKTHROUGH.md): solucao completa para instrutores.

## Aviso de Uso Local

Este laboratorio contem vulnerabilidades intencionais e deve ser executado apenas em ambiente local ou explicitamente autorizado.

Nao reutilize payloads, tecnicas ou fluxos contra sistemas reais, terceiros ou ambientes fora do escopo permitido.
