# ctf-labs

Repositório com laboratórios CTF locais e intencionalmente vulneráveis para treinamento de pentest autorizado, segurança web, APIs, autenticação, autorização, exploração manual e validação técnica.

Cada pasta representa um CTF independente, com seu próprio Docker Compose, documentação, walkthrough e solução.

## Aviso de uso autorizado

Estes laboratórios são intencionalmente vulneráveis e foram criados apenas para estudo local e educacional. Não exponha os containers na internet. Não use payloads, técnicas ou comandos deste repositório contra sistemas reais, terceiros ou ambientes sem permissão explícita.

## Aviso sobre spoilers

Este repositório é público completo e pode conter arquivos com spoilers, payloads, flags e soluções.

Arquivos comuns:

- `README.md`: descrição e instruções do lab.
- `WALKTHROUGH.md`: passo a passo didático com spoilers.
- `SOLUTION.md` ou `SOLUTIONS.md`: gabarito técnico direto.
- `VALIDATION.md`: checklist técnico de validação.

Se quiser praticar como desafio, comece apenas pelo `README.md` do lab e evite abrir `WALKTHROUGH.md` ou `SOLUTION.md`/`SOLUTIONS.md` antes de tentar.

## Pré-requisitos

- Git
- Docker
- Docker Compose
- Navegador
- Opcional: curl

## Como clonar

```bash
git clone https://github.com/msrlx0/ctf-labs.git
cd ctf-labs
```

## Labs

| Lab | Tema | Porta | Status |
|---|---|---:|---|
| [lab-01-minibank](./lab-01-minibank/) | Fundamentos web: enumeração, SQLi, IDOR, acesso indevido, XSS e path traversal | 8088 | Finalizado |
| [lab-02-neonvault](./lab-02-neonvault/) | Cyberpunk Identity Grid: Blind SQLi, JWT, SSRF, SSTI, Upload Bypass, Logs SQLi, IDOR e Path Traversal | 8092 | Finalizado |
| [lab-03-netaudit](./lab-03-netaudit/) | Investigação manual web/infra: debug, request tampering, command injection, logs, traversal e abuso de endpoint interno | ver README do lab | Finalizado |
| [lab-04-sentinelcore](./lab-04-sentinelcore/) | Cadeia hard de SOC/IR: BOLA, mass assignment, JWT, SSRF, serviços internos, worker e file read | 8094 | Finalizado |
| [Lab 5 — BlackGate](./lab-05-blackgate/) | Dificuldade: Boss Final / Hard; cadeia corporativa manual de acesso, auditoria, tickets e ativos | 8096 | Completo |

## Como rodar um lab

Exemplo:

```bash
cd lab-02-neonvault
docker compose up --build
```

Acesse:

```text
http://127.0.0.1:8092
```

Para parar:

```bash
docker compose down
```

## Como estudar

Fluxo recomendado:

1. Escolha um lab.
2. Leia apenas o `README.md` do lab.
3. Suba o container com Docker.
4. Explore a aplicação pelo navegador.
5. Observe requisições e respostas com ferramentas locais simples.
6. Anote hipóteses e evidências.
7. Se travar, consulte `WALKTHROUGH.md`.
8. Se quiser o gabarito direto, consulte `SOLUTION.md` ou `SOLUTIONS.md`.
9. Use `VALIDATION.md` para validar tecnicamente o lab.

## Estrutura comum

Exemplo:

```text
lab-XX-nome/
├── README.md
├── WALKTHROUGH.md
├── SOLUTION.md ou SOLUTIONS.md
├── VALIDATION.md
├── docker-compose.yml
├── Dockerfile
├── src/ ou app/
└── arquivos do cenário
```

Nem todos os labs seguem exatamente a mesma estrutura, mas todos devem ser executáveis localmente.

## Troubleshooting rápido

Comandos úteis:

```bash
docker compose ps
docker compose logs -f
docker compose down
docker compose down -v
docker compose up --build
```

Problemas comuns:

- porta já em uso;
- Docker sem permissão;
- container antigo;
- porta errada;
- abriu arquivo de solução antes de tentar.
