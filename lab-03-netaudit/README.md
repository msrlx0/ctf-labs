# Lab 03 - NetAudit

## Historia

NetAudit e um painel interno ficticio usado por uma equipe de TI para acompanhar disponibilidade de ativos corporativos. A interface comum mostra ativos, status operacional, historico recente e botoes de verificacao, como uma ferramenta interna de monitoramento.

O laboratorio foi desenhado para investigacao manual. As superficies interessantes nao aparecem em um menu de CTF; o aluno precisa observar requisicoes, inspecionar HTML/JavaScript, testar hipoteses e correlacionar pistas.

## Objetivo

Praticar exploracao controlada de vulnerabilidades web/infra em um alvo local, com foco em descoberta progressiva e alteracao manual de requisicoes.

## Temas abordados

- Pre-auth information disclosure
- Debug/stack trace disclosure
- Credential discovery com Burp Intruder
- Request tampering
- Command Injection com output
- Blind/Time-Based Command Injection
- Hidden endpoint discovery
- Metadata disclosure
- Information disclosure em logs
- Path Traversal
- Broken Access Control
- Internal endpoint abuse
- Arbitrary File Read em funcao administrativa

O lab possui 7 flags principais.

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

## Acesso inicial

A aplicacao simula um painel interno com artefatos legados expostos por erro de configuracao.
Comece pelo reconhecimento da aplicacao e pela observacao das requisicoes para descobrir como acessar o painel.

## Escopo autorizado

Use somente o alvo local:

```text
http://127.0.0.1:8090
```

Nao use os testes ou tecnicas deste lab contra sistemas de terceiros. Todas as vulnerabilidades sao intencionais e restritas ao container Docker.
Mantenha a validacao manual dentro do escopo local e evite testes agressivos ou automatizados fora do objetivo do laboratorio.

## Abordagem sugerida

Comece pelo navegador. Use DevTools Network para entender as chamadas da interface, Sources/View Source para procurar referencias discretas e Burp Suite para repetir/modificar requisicoes. A tela principal foi feita para parecer uma ferramenta normal de TI.
