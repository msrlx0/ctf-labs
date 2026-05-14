# Student Guide - Lab 05 BlackGate

## Introdução

Você recebeu acesso comum ao **BlackGate Operations Console**, uma plataforma corporativa usada para controlar acessos internos, tickets de segurança, ativos monitorados e alertas operacionais.

Na **Fase 6 — Credential Reuse / Legacy Panel Labyrinth**, o objetivo é correlacionar reconhecimento, contexto operacional, gateway interno simulado, Files Vault e realm legado de manutenção. A fase inclui decoys, respostas ambíguas e credenciais obsoletas; ainda não existe command injection, Redis, worker, shell ou exploração admin completa.

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
- Página `/legacy`.
- Página `/files-vault`.
- Cards do dashboard.
- Tickets com nomes de serviços e revisões pendentes.
- Inventário de ativos internos fictícios.
- Arquivo JavaScript público.
- Comentários discretos no HTML.
- Endpoints públicos de status, versão e configuração.
- Diferenças entre interface, sessão, contexto operacional, gateway e APIs JSON.

## Primeiros passos sugeridos

1. Acesse `http://localhost:8096`.
2. Faça login com uma conta comum.
3. Navegue por `/dashboard`, `/context`, `/gateway`, `/legacy`, `/files-vault`, `/tickets` e `/assets`.
4. Observe arquivos estáticos, comentários, cookies e respostas JSON.
5. Relacione textos do dashboard com tickets, assets, contexto e gateway.
6. Anote nomes internos, componentes legados e estados de migração.

## Phase 2 Recon Tips

Procure sinais de enumeração em:

- arquivos estáticos;
- políticas públicas;
- status e versão;
- configuração exposta;
- mapas parciais de rotas;
- diagnósticos limitados.

## Phase 3 — Weak Token / Role Escalation

Dicas sem spoiler:

- Relacione usuário autenticado, role e contexto operacional.
- Procure referências a contexto em superfícies públicas e autenticadas.
- Diagnósticos limitados podem mudar conforme metadados enviados.
- Procure rotas relacionadas a compatibilidade.
- Observe se tokens de contexto parecem opacos ou estruturados.
- Diferencie role da sessão e role do contexto legado.

## Phase 4 — Gateway Trust / SSRF Setup

Dicas sem spoiler:

- Reaproveite descobertas da Fase 3.
- Contexto operacional pode influenciar workflows de compatibilidade.
- Relacione gateway, assets e metadados.
- Observe hostnames internos e estados de manutenção.
- Valide sinais simples antes de assumir acesso amplo.
- Nem todo host ou path interno responde igual.
- Pense em SSRF controlado/simulado: o gateway resolve upstreams internos permitidos sem request real para internet.
- Hosts externos devem ser bloqueados.

## Phase 5 — Files Vault / Controlled File Read

Dicas sem spoiler:

- Reaproveite o contexto operator da Fase 3.
- Reaproveite o fluxo de gateway da Fase 4.
- Comece por sinais de metadata antes de tentar documentos.
- Observe diferenças entre catálogo, download nomeado e leitura legada.
- Observe diferença entre nome de arquivo e path completo.
- Procure pistas de migração e compatibilidade.
- Pense em normalização de path.
- Valide caminhos públicos antes de tentar algo fora do catálogo.
- Uma resposta pública bem-sucedida costuma ensinar o formato da próxima tentativa.

## Phase 6 — Credential Reuse / Legacy Panel

Dicas sem spoiler:

- Nem toda credencial encontrada é útil.
- Nem todo login usa o mesmo realm.
- O login público e o painel legado não compartilham identidade.
- Arquivos de migração podem conter entradas antigas, falsas ou desativadas.
- Diferencie public console, gateway context e legacy maintenance realm.
- Se um endpoint diz `interactive login disabled`, procure o fluxo de manutenção.
- Preserve query strings ao atravessar camadas intermediárias.
- Encode URLs internas quando houver parâmetros.
- Mensagens de erro parecidas podem apontar para realms diferentes.

## Dicas leves de enumeração

- Nem toda pista aparece como link no menu.
- Tickets operacionais costumam citar nomes de serviços importantes.
- Ativos internos podem revelar fronteiras de confiança.
- Arquivos JavaScript públicos podem conter metadados de frontend.
- Um endpoint de health raramente é sensível sozinho, mas ajuda a validar serviço, versão e escopo.
- Observe diferenças entre contas comuns.
- Observe diferença entre 400, 401, 403 e 404 nas APIs.
- Mensagens de erro para path ausente, path bloqueado e documento inexistente podem não significar a mesma coisa.
- Não confie no primeiro bloco de credenciais encontrado em arquivo antigo.

## O que evitar

- Não rode scanners barulhentos.
- Não tente atacar sistemas fora do Docker local.
- Não procure uma flag final nesta fase.
- Não assuma que a conta administrativa está disponível agora.
- Não trate nomes internos como alvos externos reais.
- Não tente command injection, upload, Redis, worker ou shell; essas cadeias ainda não fazem parte desta fase.
- Não trate credenciais de CTF como credenciais reais.
