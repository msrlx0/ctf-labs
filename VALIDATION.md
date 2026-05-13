# Validação geral do repositório ctf-labs

Este documento reúne checks gerais para validar se o repositório está pronto para uso público/local. Validações específicas ficam dentro de cada lab, em seu próprio `VALIDATION.md`.

## Escopo

Este arquivo valida o repositório como coleção.

Cada lab possui sua própria validação técnica, documentação e gabarito. Para validar um lab específico, entre na pasta do lab e siga o `VALIDATION.md` local quando existir. Quando não houver `VALIDATION.md` local, consulte o `README.md`, `WALKTHROUGH.md`, `SOLUTION.md` ou `SOLUTIONS.md` do lab.

## Checks gerais

Verificar estado do repositório:

```bash
git status --short
```

Procurar arquivos que normalmente não devem ser publicados por acidente:

```bash
find . -name ".env" -o -name "cookies.txt" -o -name "node_modules" -o -name "*.pem" -o -name "*.key"
```

Listar arquivos versionados que parecem conter material de solução, flags ou credenciais:

```bash
git ls-files | grep -Ei "solution|walkthrough|validation|flag|secret|token|password|senha|\\.env|key|pem"
```

Como o repositório é público completo, arquivos de solução, walkthrough, flags e segredos fictícios de CTF podem aparecer. O objetivo do check é identificar acidentalmente arquivos reais, como `.env`, chaves privadas, cookies ou credenciais pessoais.

## Validação por lab

### Lab 1 — MiniBank

```bash
cd lab-01-minibank
docker compose up --build
```

Se existir validação interna, consulte também `README.md`, `WALKTHROUGH.md`, `SOLUTION.md` e validações locais.

### Lab 2 — NeonVault

```bash
cd lab-02-neonvault
docker compose up --build
```

Em outro terminal:

```bash
bash scripts/validate.sh
```

Resultado esperado:

```text
34 OK, 0 FAIL
```

### Lab 3 — NetAudit

```bash
cd lab-03-netaudit
docker compose up --build
```

Consultar:

```text
README.md
SOLUTION.md
WALKTHROUGH.md
```

### Lab 4 — SentinelCore

```bash
cd lab-04-sentinelcore
docker compose up --build
```

Consultar:

```text
README.md
STUDENT-GUIDE.md
WALKTHROUGH.md
```

## Checklist antes de publicar

- [ ] `README.md` raiz atualizado.
- [ ] Todos os labs listados no `README.md` raiz.
- [ ] Aviso de uso autorizado presente.
- [ ] Aviso de spoilers presente.
- [ ] Nenhum arquivo `.env` real versionado.
- [ ] Nenhuma chave privada real versionada.
- [ ] Nenhum cookie local versionado.
- [ ] Containers sobem localmente.
- [ ] Labs não devem ser expostos na internet.
- [ ] Arquivos de solução/walkthrough são intencionais.

## Observação sobre spoilers

Este repositório pode conter spoilers completos por decisão do autor. Quem quiser praticar como desafio deve evitar `WALKTHROUGH.md`, `SOLUTION.md` e `SOLUTIONS.md` até tentar resolver.
