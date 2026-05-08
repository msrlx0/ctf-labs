# Lab 2 — NeonVault: Cyber Identity Breach

## Descrição

NeonVault é um laboratório web local sobre comprometimento de identidade digital em um painel cyberpunk da corporação NeonCore.

O jogador entra no **NeonVault Identity Grid**, em Neo-Sampa 2099, e investiga recuperação de acesso, claims de sessão, integrações internas, templates, uploads, APIs, logs e arquivos legados.

Este lab foi desenhado como uma evolução natural da trilha: os conceitos continuam didáticos, mas os fluxos são mais variados e exigem correlação entre módulos.

## História

A NeonCore opera identidades digitais de funcionários, operadores e sistemas automatizados em Neo-Sampa 2099. O NeonVault nasceu moderno, mas ainda carrega adaptadores antigos de migração, ferramentas internas de diagnóstico e componentes legados que sobreviveram ao tempo.

Você recebeu uma credencial operacional básica para auditar o ambiente. O objetivo é entender como o Identity Grid se comporta, seguir pistas internas e provar impacto sobre o núcleo administrativo.

## Aviso de uso autorizado

Este laboratório foi criado apenas para execução local e educacional.

Não exponha este projeto na internet. Não use comandos, payloads ou técnicas deste lab contra sistemas reais, terceiros ou ambientes sem permissão explícita.

## Stack

- Node.js
- Express
- EJS
- JWT
- Multer
- Armazenamento local simples em memória e arquivos
- Docker Compose

## Como rodar com Docker

Docker é o método recomendado. Não é necessário ter `npm` instalado no host: as dependências são instaladas dentro do container durante o build.

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

O app também inicia um serviço interno na porta `5000`, usado apenas pelo próprio ambiente para o exercício de SSRF. Essa porta não deve ser publicada no Docker Compose.

## Execução opcional sem Docker

Use apenas se você já tiver Node.js e npm instalados no host:

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

Investigar o NeonVault como um operador autenticado, correlacionar pistas dos módulos e capturar oito evidências de impacto.

Tipos de falha presentes no lab:

- Blind SQL Injection
- JWT Forgery
- SSRF
- SSTI
- Upload Bypass
- SQL Injection em filtro de logs
- IDOR em API
- Path Traversal

As soluções completas ficam em `SOLUTIONS.md`. Esse arquivo contém spoilers e deve ser usado apenas por instrutores, validadores ou depois da tentativa do jogador.

## Mapa de habilidades

| Área | Vulnerabilidade | Dificuldade | Foco |
|---|---|---:|---|
| Recuperação | Blind SQLi | Médio | Inferência por tempo em fluxo auxiliar |
| Sessão | JWT fraco | Médio | Claims e assinatura |
| Integrações | SSRF | Médio | Acesso a recurso interno |
| Templates | SSTI | Médio | Renderização insegura |
| Upload | Bypass de validação | Fácil/Médio | Validação por nome/extensão |
| Logs | SQLi em filtro | Fácil/Médio | Filtro legado |
| API | IDOR | Fácil | Controle de acesso por objeto |
| Arquivos | Path Traversal | Médio | Leitura fora do diretório |

## Regras do lab

- Execute apenas localmente.
- Comece pela credencial inicial.
- Use a interface para entender os módulos antes de automatizar testes.
- Não altere o código durante a resolução.
- Não exponha o container em rede pública.
- Registre evidências de impacto de forma organizada.

## Estrutura resumida

```text
lab-02-neonvault/
├── src/                 # servidor Express, dados e views EJS
├── public/              # CSS, imagem e arquivos publicados
├── backup/              # artefatos legados do cenário
├── var/log/neonvault/   # logs usados no fluxo de investigação
├── uploads/             # cache local de uploads do lab
├── scripts/             # validação automatizada
├── Dockerfile
├── docker-compose.yml
├── README.md
├── SOLUTIONS.md
└── VALIDATION.md
```

## Próximos passos para o jogador

1. Suba o lab com Docker.
2. Acesse `http://127.0.0.1:8092`.
3. Entre com a credencial inicial.
4. Use o dashboard como briefing.
5. Explore os módulos sem abrir `SOLUTIONS.md`.
6. Capture as oito evidências.

## Troubleshooting rápido

Ver containers:

```bash
docker compose ps
```

Ver logs:

```bash
docker compose logs -f
```

Parar o lab:

```bash
docker compose down
```

Rebuildar do zero:

```bash
docker compose down
docker compose up --build
```

Se a porta `8092` estiver em uso, pare o processo antigo ou outro container que esteja ocupando a porta.

Se o Docker retornar erro de permissão, verifique se seu usuário pode acessar o daemon Docker ou execute o comando conforme o padrão do seu ambiente local.
