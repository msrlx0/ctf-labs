# lab-01-minibank

## Objetivo

`lab-01-minibank` e um CTF web pequeno e local para treinar novos estagiarios em fundamentos de pentest web autorizado.

A aplicacao alvo se chama **MiniBank Internal Portal** e simula um portal interno antigo, com pistas de enumeracao, sete vulnerabilidades principais com flag e uma falha didatica de enumeracao de usuarios no login.

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

Este lab possui sete vulnerabilidades principais com flag:

| Vulnerabilidade | Local principal | Flag |
|---|---|---|
| SQL Injection no login | `POST /login` | `FLAG{sqli_capturada}` |
| IDOR em contas | `GET /account/:id` | `FLAG{idor_capturada}` |
| Credencial vazada em arquivo publico | `GET /dev.txt` | `FLAG{credencial_exposta_capturada}` |
| Path Traversal / LFI controlado | `GET /download?file=` | `FLAG{path_traversal_capturada}` |
| Bypass de 403 por compatibilidade legada | Relatorios administrativos | `FLAG{403_bypass_capturado}` |
| XSS basico refletido | Busca de clientes | `FLAG{xss_basico_capturado}` |
| DOM XSS intermediario | Ferramentas do cliente | `FLAG{dom_xss_capturado}` |

As novas etapas adicionam falhas de controle de acesso, XSS refletido e DOM XSS. O objetivo e comparar vulnerabilidades que aparecem na resposta HTML do servidor com vulnerabilidades que acontecem apenas no navegador por JavaScript client-side.

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
- busca de clientes no dashboard
- ferramentas do cliente no dashboard
- mensagens de erro do `POST /login`

## Como Subir

Dentro da pasta `lab-01-minibank`:

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
curl -i "http://localhost:8088/download?file=report-q2.txt"
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Fluxo esperado do Path Traversal:

1. Descobrir `/download` por enumeracao.
2. Ler `/download?file=report-q2.txt`.
3. Identificar a pista `config` e `legacy.conf`.
4. Usar `/download?file=../../../../etc/passwd` para provar leitura fora do diretorio permitido.
5. Usar o vazamento de `/status` para identificar `internal_path=/usr/src/app`.
6. Ler `/download?file=../config/legacy.conf` para obter `FLAG{path_traversal_capturada}`.

## Checklist Final do Instrutor

- [ ] Docker sobe sem erro
- [ ] Home responde
- [ ] `/status` responde
- [ ] `/robots.txt` responde
- [ ] `/dev.txt` mostra credencial e flag
- [ ] Login diferencia `Usuario nao encontrado.` de `Senha invalida.`
- [ ] SQL Injection mostra `FLAG{sqli_capturada}`
- [ ] `/account/2` mostra `FLAG{idor_capturada}`
- [ ] Path traversal le `/etc/passwd` e `../config/legacy.conf`
- [ ] Bypass de 403 mostra `FLAG{403_bypass_capturado}` apenas com header legado
- [ ] XSS refletido executa payload simples e mostra `FLAG{xss_basico_capturado}`
- [ ] DOM XSS executa a partir do hash e mostra `FLAG{dom_xss_capturado}`
- [ ] `grep` de flags retorna as 7 flags finais

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
