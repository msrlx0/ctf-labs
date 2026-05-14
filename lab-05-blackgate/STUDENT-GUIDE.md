# Student Guide - Lab 05 BlackGate

## Introdução

Você recebeu acesso comum ao **BlackGate Operations Console**, uma plataforma corporativa usada para controlar acessos internos, tickets de segurança, ativos monitorados e alertas operacionais.

Nesta Fase 1, o objetivo não é obter uma flag final. O foco é reconhecer a aplicação, entender a superfície inicial e aprender a separar navegação normal de pistas úteis para fases posteriores.

## Escopo

O único alvo autorizado é:

```text
http://localhost:8096
```

Execute o lab somente localmente com Docker.

## O que observar

- Tela de login e contas comuns.
- Cookie de sessão após autenticação.
- Papel do usuário logado.
- Cards do dashboard.
- Tickets com nomes de serviços e revisões pendentes.
- Inventário de ativos internos fictícios.
- Arquivo JavaScript público.
- Comentários discretos no HTML.
- Endpoint `/health`.

## Primeiros passos sugeridos

1. Acesse `http://localhost:8096`.
2. Faça login com uma conta comum.
3. Navegue por `/dashboard`, `/tickets` e `/assets`.
4. Abra DevTools e observe Network, Application/Storage e Sources.
5. Veja quais arquivos estáticos são carregados.
6. Compare os textos do dashboard com os tickets e ativos.
7. Anote nomes de hosts internos e componentes legados.

## Dicas leves de enumeração

- Nem toda pista aparece como link no menu.
- Tickets operacionais costumam citar nomes de serviços importantes.
- Ativos internos podem revelar fronteiras de confiança.
- Arquivos JavaScript públicos podem conter metadados de frontend.
- Um endpoint de health raramente é sensível sozinho, mas ajuda a validar serviço, versão e escopo.

## O que evitar

- Não rode scanners barulhentos.
- Não tente atacar sistemas fora do Docker local.
- Não procure uma flag final nesta fase.
- Não assuma que a conta administrativa está disponível agora.
- Não trate nomes internos como alvos externos reais.
