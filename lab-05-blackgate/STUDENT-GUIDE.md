# Student Guide - Lab 05 BlackGate

## Introducao

Voce recebeu acesso comum ao **BlackGate Operations Console**, uma plataforma corporativa usada para controlar acessos internos, tickets de seguranca, ativos monitorados e alertas operacionais.

Na **Fase 7 - Report Workflow Abuse / Queue Preparation**, o objetivo e correlacionar reconhecimento, contexto operacional, gateway interno simulado, Files Vault, realm legado de manutencao e um workflow de reports parcialmente migrado. A fase inclui decoys, respostas ambiguas, templates escondidos e jobs que podem parecer sucesso parcial.

Ainda nao existe command injection, upload, Redis, worker real, shell ou exploracao admin completa.

## Escopo

O unico alvo autorizado e:

```text
http://localhost:8096
```

Execute o lab somente localmente com Docker.

## O que observar

- Tela de login e contas comuns.
- Cookie de sessao apos autenticacao.
- Papel do usuario logado.
- Pagina `/context`.
- Pagina `/gateway`.
- Pagina `/legacy`.
- Pagina `/files-vault`.
- Cards do dashboard.
- Tickets com nomes de servicos e revisoes pendentes.
- Inventario de ativos internos ficticios.
- Arquivo JavaScript publico.
- Comentarios discretos no HTML.
- Endpoints publicos de status, versao e configuracao.
- Diferencas entre interface, sessao, contexto operacional, gateway e APIs JSON.
- Estados de migracao que aparecem em mais de uma camada.

## Primeiros passos sugeridos

1. Acesse `http://localhost:8096`.
2. Faca login com uma conta comum.
3. Navegue por `/dashboard`, `/context`, `/gateway`, `/legacy`, `/files-vault`, `/tickets` e `/assets`.
4. Observe arquivos estaticos, comentarios, cookies e respostas JSON.
5. Relacione textos do dashboard com tickets, assets, contexto e gateway.
6. Anote nomes internos, componentes legados e estados de migracao.

## Phase 2 Recon Tips

Procure sinais de enumeracao em:

- arquivos estaticos;
- politicas publicas;
- status e versao;
- configuracao exposta;
- mapas parciais de rotas;
- diagnosticos limitados.

## Phase 3 - Weak Token / Role Escalation

Dicas sem spoiler:

- Relacione usuario autenticado, role e contexto operacional.
- Procure referencias a contexto em superficies publicas e autenticadas.
- Diagnosticos limitados podem mudar conforme metadados enviados.
- Procure rotas relacionadas a compatibilidade.
- Observe se tokens de contexto parecem opacos ou estruturados.
- Diferencie role da sessao e role do contexto legado.

## Phase 4 - Gateway Trust / SSRF Setup

Dicas sem spoiler:

- Reaproveite descobertas da Fase 3.
- Contexto operacional pode influenciar workflows de compatibilidade.
- Relacione gateway, assets e metadados.
- Observe hostnames internos e estados de manutencao.
- Valide sinais simples antes de assumir acesso amplo.
- Nem todo host ou path interno responde igual.
- Pense em SSRF controlado/simulado: o gateway resolve upstreams internos permitidos sem request real para internet.
- Hosts externos devem ser bloqueados.

## Phase 5 - Files Vault / Controlled File Read

Dicas sem spoiler:

- Reaproveite o contexto operator da Fase 3.
- Reaproveite o fluxo de gateway da Fase 4.
- Comece por sinais de metadata antes de tentar documentos.
- Observe diferencas entre catalogo, download nomeado e leitura legada.
- Observe diferenca entre nome de arquivo e path completo.
- Procure pistas de migracao e compatibilidade.
- Pense em normalizacao de path.
- Valide caminhos publicos antes de tentar algo fora do catalogo.
- Uma resposta publica bem-sucedida costuma ensinar o formato da proxima tentativa.

## Phase 6 - Credential Reuse / Legacy Panel

Dicas sem spoiler:

- Nem toda credencial encontrada e util.
- Nem todo login usa o mesmo realm.
- O login publico e o painel legado nao compartilham identidade.
- Arquivos de migracao podem conter entradas antigas, falsas ou desativadas.
- Diferencie public console, gateway context e legacy maintenance realm.
- Se um endpoint diz `interactive login disabled`, procure o fluxo de manutencao.
- Preserve query strings ao atravessar camadas intermediarias.
- Encode URLs internas quando houver parametros.
- Mensagens de erro parecidas podem apontar para realms diferentes.

## Phase 7 - Report Workflow Abuse / Queue Preparation

Dicas sem spoiler:

- Nem todo template listado e tudo que existe.
- Workflows legados podem ter modos de migracao.
- Diferencie preview, create e queue.
- Leia notas internas com atencao.
- Combinacoes erradas podem parecer sucesso parcial.
- Jobs aceitos nem sempre sao processados imediatamente.
- Um workflow pode aceitar configuracoes que nao renderiza de forma sincrona.
- Arquivos antigos podem explicar a diferenca entre fila padrao, fila legada e fila de manutencao.

## Dicas leves de enumeracao

- Nem toda pista aparece como link no menu.
- Tickets operacionais costumam citar nomes de servicos importantes.
- Ativos internos podem revelar fronteiras de confianca.
- Arquivos JavaScript publicos podem conter metadados de frontend.
- Um endpoint de health raramente e sensivel sozinho, mas ajuda a validar servico, versao e escopo.
- Observe diferencas entre contas comuns.
- Observe diferenca entre 400, 401, 403 e 404 nas APIs.
- Mensagens de erro para path ausente, path bloqueado e documento inexistente podem nao significar a mesma coisa.
- Nao confie no primeiro bloco de credenciais encontrado em arquivo antigo.

## O que evitar

- Nao rode scanners barulhentos.
- Nao tente atacar sistemas fora do Docker local.
- Nao assuma que a conta administrativa esta disponivel agora.
- Nao trate nomes internos como alvos externos reais.
- Nao tente command injection, upload, Redis, worker real ou shell; essas cadeias ainda nao fazem parte desta fase.
- Nao trate credenciais de CTF como credenciais reais.
