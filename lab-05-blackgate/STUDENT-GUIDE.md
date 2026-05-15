# Student Guide - Lab 5 BlackGate

## Introducao

Voce recebeu a credencial inicial de uma conta comum no **BlackGate Operations Console**. A aplicacao simula uma plataforma corporativa com tickets, ativos, contexto operacional, gateway, componentes legados e fluxos de aprovacao.

O lab comeca pequeno de proposito. A primeira tela nao mostra tudo, o menu nao entrega todos os caminhos e algumas respostas parecem incompletas ate serem comparadas com outras superficies.

## Escopo

O unico alvo autorizado e:

```text
http://localhost:8096
```

Use o lab apenas localmente com Docker.

## Credencial inicial

```text
guest / guest123
```

## Como estudar

Use o navegador como ponto de partida. Um proxy HTTP com repeater ajuda muito, porque o lab foi feito para observar requisicoes, repetir testes, alterar pequenas partes e comparar respostas.

Ferramentas recomendadas:

- navegador;
- proxy HTTP;
- repeater;
- inspetor de rede;
- decoder local;
- anotacoes manuais.

Evite transformar o estudo em uma sequencia de comandos prontos. O valor do lab esta em perceber a conexao entre as pistas.

## O que observar primeiro

1. Entre com a conta `guest`.
2. Observe que o menu inicial e limitado.
3. Abra Dashboard, Tickets e Assets.
4. Anote nomes, estados, mensagens e diferencas de linguagem.
5. Compare o que aparece na interface com o que aparece nas respostas HTTP.
6. Teste rotas obvias e observe status codes diferentes.
7. Procure diferencas entre dados visiveis, dados parciais e dados ausentes.

## Visibilidade por papel

A interface muda conforme a visao disponivel:

- `guest` ve Dashboard, Tickets e Assets.
- `analyst` ve Dashboard, Tickets, Assets e um Context limitado.
- `operator` ve a superficie operacional mais ampla.

Essa diferenca e uma pista importante. Ver um menu a mais nao significa que a fase esta resolvida. O lab diferencia identidade publica, sessao web, contexto operacional e fluxos internos.

## Mentalidade do lab

O BlackGate e um labirinto manual. Em cada fase, tente responder:

- O que mudou na resposta?
- O status code faz sentido?
- A mensagem parece generica ou especifica?
- A interface esta escondendo algo que a API ainda deixa inferir?
- O dado encontrado e atual, legado, falso ou parcial?
- Esta pista se conecta a tickets, assets, contexto, gateway, legado ou aprovacao?

Nem todo caminho obvio e util. Algumas pistas sao falsas, antigas ou incompletas. `robots.txt`, sitemap, mapas de rotas e configuracoes publicas ajudam no reconhecimento, mas nao entregam necessariamente o caminho real.

## Dicas sem spoiler

- Comece pelo que o `guest` realmente ve.
- Compare visoes quando o lab sugerir papeis ou contextos diferentes.
- Observe diferencas entre sessao web e contexto operacional.
- Leia tickets e assets como pistas, nao apenas como conteudo decorativo.
- Use o repeater para testar uma hipotese por vez.
- Preserve evidencias: nomes internos, mensagens de erro, status codes e campos recorrentes.
- Desconfie de credenciais antigas ou blocos muito obvios.
- Diferencie caminhos publicos, caminhos operacionais e caminhos legados.
- Quando uma resposta disser que algo esta bloqueado, compare com outro caminho antes de concluir que acabou.
- Se uma etapa produzir um identificador, guarde-o; ele pode fazer sentido apenas mais tarde.

## O que evitar

- Nao rode scanners barulhentos.
- Nao ataque sistemas fora do Docker local.
- Nao assuma que a conta inicial deve ter todos os menus.
- Nao procure shell, upload, Redis, banco externo ou command injection real.
- Nao trate hostnames internos do cenario como alvos reais.
- Nao pule direto para o walkthrough se quiser praticar a cadeia.

## Quando travar

Volte para as evidencias:

- tickets visiveis;
- assets visiveis;
- respostas JSON publicas;
- headers e cookies;
- diferencas entre 400, 401, 403 e 404;
- nomes que aparecem em mais de um lugar;
- mensagens que parecem vagas demais para serem acidentais.

O caminho final exige correlacao. Uma pista isolada raramente basta.
