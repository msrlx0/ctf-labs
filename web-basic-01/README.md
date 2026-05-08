# web-basic-01

## Objetivo

`web-basic-01` e um CTF web pequeno e local para treinar novos estagiarios em fundamentos de pentest web autorizado.

A aplicacao alvo se chama **MiniBank Internal Portal** e simula um portal interno antigo, com pistas de enumeracao, quatro vulnerabilidades principais com flag e uma falha didatica de enumeracao de usuarios no login.

## Publico-Alvo

- Novos estagiarios de seguranca
- Pessoas praticando enumeracao web basica
- Instrutores validando fluxo de coleta de evidencias e relatorio tecnico

## Orientação de enumeração

Antes de tentar explorar falhas, o aluno deve começar pela enumeração web básica. Observe com calma a página inicial, o código-fonte HTML, arquivos comuns de orientação para crawlers, links visíveis, formulários, parâmetros de URL, mensagens de erro e respostas HTTP.

O objetivo é seguir pistas deixadas pela própria aplicação, e não adivinhar rotas aleatórias.

## Aviso de Uso Autorizado

Este lab foi criado somente para execucao local e autorizada.

Nao use comandos, payloads ou tecnicas deste lab contra terceiros, sistemas reais ou ambientes sem permissao explicita.

## Stack

- Docker Compose
- Node.js
- Express
- EJS
- MySQL
- CSS simples

## Escopo Autorizado

Somente:

- `http://localhost:8088`
- containers criados pelo `docker compose` desta pasta

O MySQL fica acessivel apenas pela rede interna do Docker Compose. A porta `3306` nao e publicada no host.

## Vulnerabilidades Principais

Este lab possui exatamente quatro vulnerabilidades principais com flag:

| Vulnerabilidade | Local principal | Flag |
|---|---|---|
| SQL Injection no login | `POST /login` | `FLAG{sqli_capturada}` |
| IDOR em contas | `GET /account/:id` | `FLAG{idor_capturada}` |
| Credencial vazada em arquivo publico | `GET /dev.txt` | `FLAG{credencial_exposta_capturada}` |
| Path Traversal / LFI controlado | `GET /download?file=` | `FLAG{path_traversal_capturada}` |

## Falha Didatica Sem Flag

O `POST /login` tambem ensina enumeracao de usuarios por mensagens de erro diferentes:

- username inexistente: `Usuario nao encontrado.`
- username existente com senha errada: `Senha invalida.`

Essa falha e intencional e nao possui flag propria.

## Pistas de Enumeracao

Estas rotas e artefatos existem como pistas, mas nao possuem flag propria:

- `/status`
- `/robots.txt`
- comentario HTML na home
- `/dev.txt`
- `/backup`
- `/download`
- `/admin`
- mensagens de erro do `POST /login`

## Como Subir

Dentro da pasta `web-basic-01`:

```bash
sudo docker compose up --build
```

## Como Parar

```bash
sudo docker compose down
```

## Como Resetar o Banco

O reset remove o volume do MySQL e recria os dados a partir de `db/init.sql` no proximo `up`.

```bash
sudo docker compose down -v
```

## URL

```text
http://localhost:8088
```

## Credenciais Legitimas Para Fluxo Normal

- `joao` / `joao123`
- `maria` / `maria123`
- `auditor` / `audit2026`

O usuario `admin` existe no banco, mas o caminho esperado para obter sessao admin e identificar a falha no fluxo de login.

## Documentacao

- [WALKTHROUGH.md](./WALKTHROUGH.md): passo a passo didatico com raciocinio, comandos, impacto e correcoes.
- [SOLUTION.md](./SOLUTION.md): gabarito tecnico objetivo com rotas, acoes e flags.

## Validacao

Os comandos completos de validacao do release candidate estao em [../VALIDATION.md](../VALIDATION.md).

```bash
curl -i http://localhost:8088
curl -i http://localhost:8088/status
curl -i http://localhost:8088/robots.txt
curl -i http://localhost:8088/dev.txt
curl -i -X POST http://localhost:8088/login -d "username=naoexiste" -d "password=teste"
curl -i -X POST http://localhost:8088/login -d "username=joao" -d "password=errada"
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=../../../../flags/final.txt"
```

## Checklist Final do Instrutor

- [ ] Docker sobe sem erro
- [ ] Home responde
- [ ] `/status` responde
- [ ] `/robots.txt` responde
- [ ] `/dev.txt` mostra credencial e flag
- [ ] Login diferencia `Usuario nao encontrado.` de `Senha invalida.`
- [ ] SQL Injection mostra `FLAG{sqli_capturada}`
- [ ] `/account/2` mostra `FLAG{idor_capturada}`
- [ ] Path traversal mostra `FLAG{path_traversal_capturada}`
- [ ] `grep` de flags retorna somente as 4 flags finais

## Orientação de enumeração

Antes de tentar explorar qualquer falha, comece pela enumeração web básica. Observe com atenção:

- página inicial
- código-fonte HTML
- arquivos comuns de orientação para crawlers
- links visíveis
- formulários
- parâmetros de URL
- mensagens de erro
- respostas HTTP

O objetivo é seguir pistas deixadas pela própria aplicação, e não adivinhar rotas aleatórias.
