# Lab 03 - NetAudit

NetAudit e um painel interno ficticio usado por uma equipe de TI para monitorar disponibilidade de ativos corporativos. O ambiente simula uma aplicacao simples de operacoes: login de analista, lista de ativos, checagem de host e alguns recursos legados deixados para suporte.

O laboratorio foi desenhado para investigacao manual. A superficie vulneravel principal nao fica toda em botoes visiveis; o aluno precisa observar requisicoes, inspecionar HTML/JavaScript, testar hipoteses e correlacionar pistas em logs.

## Objetivo

Praticar exploracao controlada de vulnerabilidades web/infra em um alvo local, com foco em Command Injection e descoberta progressiva de endpoints internos.

Temas praticados:

- command injection;
- weak filtering;
- information disclosure;
- path traversal;
- broken access control;
- hidden internal endpoint abuse.

## Como executar

```bash
cd ~/ctf-labs/lab-03-netaudit
docker compose up --build
```

Abra:

```text
http://127.0.0.1:8090
```

Dentro do container, a aplicacao escuta na porta `3000`.

## Credencial

```text
analyst:analyst123
```

## Escopo autorizado

Use somente o alvo local:

```text
http://127.0.0.1:8090
```

Nao use os testes ou tecnicas deste lab contra sistemas de terceiros. Todas as vulnerabilidades sao intencionais e restritas ao container Docker.

## Dica de abordagem

Comece pelo navegador. Use DevTools Network para entender as chamadas da interface, Sources/View Source para procurar referencias discretas e Burp Suite para repetir/modificar requisicoes. O lab foi feito para recompensar reconhecimento, nao clique em menu.
