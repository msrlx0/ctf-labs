# Lab 5 — BlackGate

Nome: **Lab 5 — BlackGate**.

Dificuldade: **Boss Final / Hard**.

Porta: **8096**.

Status: **Completo**.

BlackGate e um laboratorio CTF local sobre uma console corporativa de acesso, auditoria, tickets, ativos e operacoes internas. O lab foi desenhado como uma cadeia manual de exploracao: o aluno comeca com acesso limitado, observa a aplicacao, compara respostas, enumera superficies, testa hipoteses e correlaciona pistas ate reconstruir o caminho completo.

Este lab nao foi pensado para ser resolvido copiando uma sequencia de comandos. A experiencia principal e no navegador, com apoio de proxy HTTP, repeater, inspecao de respostas e tentativa controlada.

## Aviso de uso local

Este laboratorio e intencionalmente vulneravel e foi criado apenas para estudo local e educacional. Nao exponha os containers na internet. Nao use payloads, tecnicas ou comandos deste lab contra sistemas reais, terceiros ou ambientes sem permissao explicita.

Tudo roda localmente e de forma simulada.

## Como subir

Dentro do repositorio:

```bash
cd lab-05-blackgate
docker compose up --build
```

Depois acesse no navegador:

```text
http://localhost:8096
```

Para parar:

```bash
docker compose down
```

## Credencial inicial

```text
guest / guest123
```

## Experiencia esperada

O aluno inicia com uma conta comum e uma visao limitada da aplicacao. Ao longo do lab, diferentes papeis e contextos revelam superficies distintas, mas nenhuma tela inicial entrega o mapa completo.

A resolucao exige:

- observar o que aparece e o que nao aparece na interface;
- comparar tickets, assets, mensagens e status codes;
- diferenciar sessao web, role visivel e contexto operacional;
- usar um proxy HTTP ou repeater para testar hipoteses com cuidado;
- reconhecer pistas falsas, dados incompletos e caminhos que parecem obvios mas nao levam ao fim;
- correlacionar informacoes entre varias camadas antes de avancar.

## Visibilidade por papel

A interface muda conforme a visao disponivel:

- `guest`: Dashboard, Tickets e Assets.
- `analyst`: Dashboard, Tickets, Assets e Context limitado.
- `operator`: Dashboard, Context, Gateway, Legacy, Files Vault, Tickets, Assets e Health.

A conta inicial publica e apenas `guest`. As demais visoes fazem parte da progressao do lab e nao devem ser tratadas como atalho inicial.

## Escopo educacional

O BlackGate exercita:

- reconhecimento manual de aplicacao corporativa;
- enumeracao de rotas e metadados;
- diferencas entre interface, APIs e contexto operacional;
- analise de respostas e status codes;
- confianca entre componentes simulados;
- validacao de hipoteses com proxy/repeater;
- correlacao entre pistas parciais;
- identificacao de decoys e mensagens ambiguas;
- progressao manual por fases ate o boss final.

## O que nao existe aqui

O lab nao implementa exploracao real de sistema operacional, shell, upload, Redis, banco externo, pivot real ou chamadas externas para a internet. Os fluxos internos sao simulados em memoria para treinamento local.

## Documentacao

- `README.md`: descricao publica sem spoilers criticos.
- `STUDENT-GUIDE.md`: orientacao de estudo sem entregar a solucao.
- `WALKTHROUGH.md`: passo a passo completo com spoilers.
- `VALIDATION.md`: checklist manual e tecnico para validar o lab.
