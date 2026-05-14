# Student Guide - Lab 05 BlackGate

## Introdução

Você recebeu acesso comum ao **BlackGate Operations Console**, uma plataforma corporativa usada para controlar acessos internos, tickets de segurança, ativos monitorados e alertas operacionais.

Na **Fase 4 — Gateway Trust / SSRF Setup**, o objetivo é correlacionar reconhecimento, contexto operacional e gateway interno simulado. Ainda não existe flag final, file read, command injection ou exploração admin completa.

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
- Página `/context`.
- Página `/gateway`.
- Cards do dashboard.
- Tickets com nomes de serviços e revisões pendentes.
- Inventário de ativos internos fictícios.
- Arquivo JavaScript público.
- Comentários discretos no HTML.
- Endpoints públicos de status, versão e configuração.
- Diferenças entre interface, sessão, contexto, gateway e APIs JSON.

## Primeiros passos sugeridos

1. Acesse `http://localhost:8096`.
2. Faça login com uma conta comum.
3. Navegue por `/dashboard`, `/context`, `/gateway`, `/tickets` e `/assets`.
4. Abra DevTools e observe Network, Application/Storage e Sources.
5. Veja quais arquivos estáticos são carregados.
6. Compare os textos do dashboard com os tickets, assets, contexto e gateway.
7. Anote nomes de hostnames internos, componentes legados e headers citados.

## Phase 2 Recon Tips

Verifique manualmente:

- `/robots.txt`
- `/.well-known/security.txt`
- `/security-policy`
- `/api/status`
- `/api/version`
- `/api/client-config`
- `/api/routes`
- `/debug/ping`
- arquivos JS públicos

## Phase 3 — Weak Token / Role Escalation

Dicas sem spoiler:

- Compare o usuário autenticado com o contexto operacional.
- Verifique `/api/client-config` e procure referências a contexto.
- Use `/debug/ping` com headers já conhecidos.
- Procure endpoints relacionados a `context`.
- Observe se tokens de contexto parecem opacos ou estruturados.
- Use a rota de verificação antes de tentar endpoints `operator`.
- Diferencie role da sessão e role do contexto legado.

## Phase 4 — Gateway Trust / SSRF Setup

Dicas sem spoiler:

- Reaproveite descobertas da Fase 3.
- Contexto operator desbloqueia endpoints de compatibilidade.
- Compare gateway metadata e client config.
- Observe hosts internos em assets e metadata.
- Teste endpoints de health antes de tentar metadata.
- Nem todo host ou path interno responde igual.
- Pense em SSRF controlado/simulado: o gateway resolve upstreams internos permitidos sem request real para internet.
- Hosts externos devem ser bloqueados.

## Dicas leves de enumeração

- Nem toda pista aparece como link no menu.
- Tickets operacionais costumam citar nomes de serviços importantes.
- Ativos internos podem revelar fronteiras de confiança.
- Arquivos JavaScript públicos podem conter metadados de frontend.
- Um endpoint de health raramente é sensível sozinho, mas ajuda a validar serviço, versão e escopo.
- Compare uma conta `guest` com uma conta `operator` ou `analyst`.
- Observe diferença entre 400, 401, 403 e 404 nas APIs.

## O que evitar

- Não rode scanners barulhentos.
- Não tente atacar sistemas fora do Docker local.
- Não procure uma flag final nesta fase.
- Não assuma que a conta administrativa está disponível agora.
- Não trate nomes internos como alvos externos reais.
- Não tente file read, command injection, upload, Redis ou traversal; essas cadeias ainda não fazem parte da Fase 4.
