# Lab 05 - BlackGate

**BlackGate** é um laboratório CTF local sobre uma plataforma corporativa de acesso, auditoria e operações internas. A aplicação simula uma console moderna usada para controlar tickets de segurança, ativos internos, alertas operacionais e revisões de acesso.

Dificuldade: **Boss Final**.

Status: **Fase 1**.

Porta pública:

```text
http://localhost:8096
```

Nesta fase, o objetivo é reconhecer a aplicação, entender sua navegação, validar login, observar o inventário e identificar pistas leves para expansões futuras. A cadeia completa de exploração ainda não está implementada.

## Aviso de uso local

Este laboratório é intencionalmente vulnerável e foi criado apenas para estudo local e educacional. Não exponha os containers na internet. Não use payloads, técnicas ou comandos deste lab contra sistemas reais, terceiros ou ambientes sem permissão explícita.

## Como subir

Dentro do repositório:

```bash
cd lab-05-blackgate
docker compose up --build
```

Depois acesse:

```text
http://localhost:8096
```

Para parar:

```bash
docker compose down
```

## Credenciais comuns

```text
operator / operator123
analyst / analyst123
guest / guest123
```

A conta administrativa existe no cenário, mas não é documentada nem liberada nesta fase.

## Objetivo educacional

- Reconhecer uma aplicação corporativa interna.
- Praticar navegação manual em painel autenticado.
- Observar cookies de sessão e controle básico de acesso.
- Enumerar tickets, ativos e metadados públicos.
- Identificar pistas leves sem transformar a Fase 1 em exploração completa.

## Funcionalidades da Fase 1

- Login com sessão.
- Logout.
- Dashboard autenticado.
- Lista de tickets de segurança.
- Inventário de ativos internos fictícios.
- Endpoint `/health` com status JSON.
- CSS próprio com identidade visual amarela e tema BlackGate.

## Rotas principais

```text
/login
/dashboard
/tickets
/assets
/health
/logout
```

## Observações

A Fase 1 prepara a base visual, narrativa e técnica do lab. Algumas páginas e arquivos públicos possuem pistas leves sobre componentes legados, serviços internos e revisões de confiança, mas não há flag final nem cadeia completa implementada nesta etapa.
