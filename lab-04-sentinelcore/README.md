# Lab 04 - SentinelCore

**SentinelCore** e um laboratorio CTF web local sobre um painel interno de SOC. A historia simula uma equipe investigando alertas, evidencias, integracoes administrativas, relatorios e jobs assincronos de uma plataforma corporativa.

Nivel: **Hard**.

O objetivo didatico e correlacionar pistas pequenas entre frontend, APIs, tokens, servicos internos e worker. Este README apresenta o ambiente e o escopo; ele nao entrega a solucao completa.

## Escopo Autorizado

```text
http://127.0.0.1:8094
```

Somente esse alvo faz parte do escopo autorizado no host local.

## Como Executar

Dentro de `lab-04-sentinelcore`:

```bash
docker compose up --build
```

Depois acesse:

```text
http://127.0.0.1:8094
```

Para parar o ambiente:

```bash
docker compose down
```

Para resetar containers e o volume compartilhado:

```bash
docker compose down -v
```

## Credenciais Iniciais

```text
intern / intern2026
```

## Servicos Internos

| Servico | Funcao | Exposicao |
|---|---|---|
| sentinel-app | Aplicacao web e API principal | `127.0.0.1:8094` |
| internal-admin | Servico administrativo usado pela aplicacao | Somente rede Docker |
| worker | Processador de jobs assincronos | Sem porta publicada |
| redis | Fila usada pelos jobs | Somente rede Docker |

## Temas Abordados

- Reconhecimento com DevTools e JavaScript publico
- Descoberta de endpoints internos da aplicacao
- BOLA / IDOR em APIs
- Mass Assignment
- Debug disclosure
- Build artifact leak
- JWT forgery a partir de segredo vazado
- SSRF para servico interno
- Abuso de proxy com headers controlados
- Template context disclosure
- Queue poisoning e worker abuse
- Leitura de output de worker
- Arbitrary file read com bypass por encoding

## Materiais

- [STUDENT-GUIDE.md](./STUDENT-GUIDE.md): dicas graduais para estudantes, sem flags e sem payloads finais.
- [WALKTHROUGH.md](./WALKTHROUGH.md): gabarito completo para instrutores.

## Aviso de Uso Local

Este laboratorio contem vulnerabilidades intencionais e deve ser executado apenas localmente ou em ambiente explicitamente autorizado.

Nao reutilize payloads, tecnicas ou fluxos contra sistemas reais, terceiros ou qualquer alvo fora de `http://127.0.0.1:8094`.
