# Guia do Aluno — Lab 08: ObsidianPay Mobile

Bem-vindo(a) ao ObsidianPay, uma carteira digital fictícia usada para treino de
segurança mobile. Na **Fase 3** já existe um **app Android base** (Kotlin +
Compose) que consome a API mobile do lab. **O APK final ainda não foi publicado**
— você roda o app a partir do código-fonte no Android Studio.

> Este documento é público e **não** contém solução nem flags.

---

## 1. Iniciar o backend

A partir da pasta do lab:

```bash
cd lab-08-obsidianpay
docker compose up --build
```

A API ObsidianPay Mobile sobe em `http://127.0.0.1:8102`.

Para parar:

```bash
docker compose down
```

## 2. Verificar se o lab está online

```bash
curl -s http://127.0.0.1:8102/health
```

Você deve ver um JSON com `"status": "ok"` e `"expectedPort": 8102`.

Explore também a raiz e a config pública:

```bash
curl -s http://127.0.0.1:8102/
curl -s http://127.0.0.1:8102/api/mobile/config
```

Para autenticar, use a conta de teste `guest` / `guest123` em
`POST /api/mobile/login` e utilize o token retornado em endpoints protegidos.
Os comandos completos estão em [VALIDATION.md](./VALIDATION.md).

---

## 2.1 Rodar o app Android (Fase 3)

Com o backend no ar (`127.0.0.1:8102`):

1. Abra `lab-08-obsidianpay/android-app` no **Android Studio**.
2. Espere o **Gradle sync** terminar (baixa as dependências).
3. Crie/escolha um **emulador** (Pixel, API 24+) e rode o app.
4. No emulador, o app fala com `http://10.0.2.2:8102` (alias para o
   `127.0.0.1` da sua máquina) — não é preciso configurar nada.
5. Faça login com `guest` / `guest123` (já vem preenchido).
6. Navegue pelas telas: Início, Recibos, Cartões, Suporte, Prévia de
   transferência e Configuração.
7. Abra recibos e cartões (inclusive pelo campo "abrir por ID"), envie um sync
   de suporte, gere uma prévia de transferência.
8. Toque em **QR Payment** e cole/digite um payload no formato do app, por ex.:
   - `obsidianpay://transfer?toUserId=2001&amount=10&memo=lunch`
   - `obsidianpay://support?topic=mobile&message=hello`
   - `obsidianpay://receipt?id=1002`
9. Em **Suporte**, abra o **Web Support** (portal de suporte em WebView). O
   portal mostra o tópico/mensagem e, quando aberto dentro do app, traz uma área
   de **diagnóstico assistido** ("Show bridge info", "Show session summary").
   Observe o portal e use o fluxo de suporte normalmente.
10. (Opcional) Dispare um deep link pelo terminal com `adb` — veja
    [android-app/README.md](./android-app/README.md).

> Os deep links, o QR Payment e o Web Support são recursos legítimos do app.
> Observe como a entrada digitada/colada é interpretada e para onde leva, e o que
> o portal de suporte consegue mostrar dentro do app. (Este guia não explica como
> abusar disso; a investigação é sua.)

> **Estado local/offline:** como um app real, o ObsidianPay guarda cache local
> (sessão, perfil, config, recibos, cartões) para funcionar melhor. A Início tem
> uma área interna "Local State" que mostra o que o app mantém no dispositivo.
> Vale observar **o que** fica guardado e **onde** — isso faz parte do raciocínio
> de segurança mobile. (Este guia não diz como extrair nada; a investigação é sua.)

> **Integração Android (fluxos internos):** como muitos apps reais, o ObsidianPay
> tem fluxos internos de operações/diagnóstico e expõe alguns **componentes
> Android** para integração com o sistema (telas e ganchos internos, além de um
> provedor de notas de suporte). No estudo de um app Android, vale sempre olhar o
> `AndroidManifest` e perguntar **quais** componentes ficam acessíveis a outros
> apps e **o que** cada um faz com o estado local. (Este guia não entrega os
> comandos de exploração; a investigação é sua.)

> **Device Trust:** a Início tem uma tela **Device Trust** (checagem de
> segurança/atestação do dispositivo). Ela faz parte do produto — rode-a e
> observe o resultado. Apps mobile às vezes montam credenciais/assinaturas
> **localmente** no cliente; a análise estática (JADX/apktool/`strings`) é a
> ferramenta natural para entender **como** isso é feito. Este guia não entrega
> segredos, assinatura nem rotas internas completas — descobrir o "como" é a sua
> parte do exercício. (Sem flags.)

> **Security Check:** a Início também tem uma tela **Security Check**. Ela
> executa uma checagem de segurança local do dispositivo e pode enviar um
> relatório de ambiente para o servidor. Como de costume: rode, observe o
> resultado e o que fica salvo localmente. (Este guia não explica como contornar
> a checagem — a investigação é sua.)

> **Secure Vault:** a Início tem uma tela **Secure Vault**. Ela representa um
> cofre de segurança interno do app — protegido por autenticação local. Você pode
> verificar a disponibilidade de biometria, desbloquear o vault e interagir com o
> backend. Como de costume: use o fluxo normalmente, observe o que fica salvo
> localmente e questione o que o servidor efetivamente valida. (Este guia não
> explica o mecanismo de bypass — a investigação é sua.)

> **App Integrity:** a Início tem uma tela **App Integrity**. Ela executa uma
> checagem de integridade do app (native gate, score de tamper, assinatura,
> pacote) e pode enviar um relatório de integridade ao backend. Como de costume:
> rode os checks, observe o que é registrado localmente e questione o que o
> servidor efetivamente valida. Apps que dependem de checagens client-side de
> integridade têm um ponto de investigação interessante. (Este guia não explica
> como contornar as checagens — a investigação é sua.)

Detalhes e build via linha de comando em
[android-app/README.md](./android-app/README.md).

## 3. Como pensar no Lab 8 (visão futura)

Quando o app Android chegar, a ideia é trabalhar como em um pentest mobile real:

- **Recon:** entenda o produto antes de atacá-lo. O que o app faz? Quais telas,
  fluxos e dados existem?
- **Análise estática:** o que dá para aprender do binário/código sem executar?
- **Análise dinâmica:** o que acontece em runtime e no tráfego de rede?
- **Cliente ↔ servidor:** muitos achados no app só fazem sentido quando ligados
  ao comportamento da API. Pense na cadeia inteira.

Use OWASP **MASVS/MASTG** como bússola conceitual. O foco é raciocínio manual,
não rodar ferramenta e copiar saída.

### Investigação manual da API (Fase 2)

A API já está rica o suficiente para começar a investigar **agora**, mesmo sem o
app. Pratique o método, não a resposta:

- **Mapeie a superfície.** Que endpoints existem? O que cada um aceita e devolve?
  Comece pelo que a config e os fluxos de suporte revelam.
- **Pense como o app pensaria.** Que requisições um cliente mobile faria? O que o
  servidor confia que veio do app?
- **Questione cada resposta.** Os dados retornados são só os seus? Os campos que
  você consegue enviar são só os esperados? O que acontece nas bordas (IDs de
  outros objetos, campos extras, headers especiais, valores estranhos)?
- **Ligue os pontos.** Um detalhe vazado aqui pode habilitar algo ali. Anote.

O objetivo é descobrir sozinho(a). Este guia **não** entrega payloads nem o
caminho — isso faz parte do desafio.

## 4. Ferramentas recomendadas

Para as fases com o app Android:

- Android Studio + Emulator
- `adb`
- Burp Suite (proxy/intercept)
- JADX (decompilação)
- apktool (desmontagem/recompilação)
- Frida (instrumentação dinâmica)
- objection (exploração runtime)

Na Fase 1, basta Docker e `curl`.

## 4.1 Ferramentas opcionais para fase mobile avançada (Fase 13)

Para análise dinâmica avançada (instrumentação em runtime), as ferramentas acima
continuam sendo as mesmas. Esta fase adiciona material de suporte específico:

- **`docs/mobile-pentest/SETUP.md`** — guia de ambiente (Frida, objection, adb, JADX, apktool, Burp Suite).
- **`docs/mobile-pentest/PLAYBOOK.md`** — sequência de tarefas de pentest manual.
- **`tools/frida/`** — scripts didáticos específicos do lab (5 scripts, um por domínio).
- **`tools/adb/`** — playbook de comandos ADB comentados.

O uso dessas ferramentas é **opcional** — as análises estáticas e os fluxos de
UI do app já entregam boa parte do exercício. Frida/objection são necessários
para a camada de instrumentação em runtime (bypass de checks client-side).

> Sem solução completa aqui. O uso das ferramentas e a interpretação dos
> resultados fazem parte do exercício.

---

## 4.2 Desafio final — Challenge Chain

O Lab 08 tem uma **cadeia oficial de CTF** com **9 estágios** e pontuação. Esta
seção explica **como** participar e registrar progresso — **sem** entregar a
solução de nenhum estágio. Detalhes públicos de pontuação estão em
[docs/CHALLENGE-SCORING.md](./docs/CHALLENGE-SCORING.md).

### Objetivo final

**Objetivo final:** resolver, na ordem que fizer sentido para você, os 9 estágios
da cadeia `obsidianpay-mobile-final-chain`, encontrando a flag de cada trilha,
submetendo-a com a evidência, e por fim destravar a **etapa final do operador**
(`stage-09-final-operator-chain`), que consolida o domínio das trilhas internas.
O placar completo é `2100` pontos.

> Você descobre as flags **resolvendo** cada trilha. Elas não estão em nenhum
> arquivo público; "achar no código" não é o objetivo e não vale.

### Como pensar na investigação mobile

- **Entenda o produto primeiro.** O que cada tela faz? Que dados o app guarda e
  que requisições ele envia? Recon antes de exploração.
- **Estático × dinâmico.** O que dá para aprender do código/binário sem rodar
  (JADX/apktool/`strings`) e o que só aparece em runtime/tráfego (Burp/Frida/adb)?
- **Cliente ↔ servidor.** Muitos achados só fazem sentido ligando o app ao
  comportamento da API. O que o servidor **confia** que veio do app?
- **Questione cada checagem.** Onde a confiança é client-side? O que o servidor
  valida de fato? Onde um valor afirmado pelo cliente é aceito sem verificação?

### Ordem sugerida de análise

A progressão didática sugerida (não é rígida) acompanha a dificuldade crescente:

1. Mobile recon / config review (mais fácil)
2. Insecure local storage
3. Exported Android components
4. WebView JavaScript bridge
5. Legacy device trust
6. Biometric vault
7. Network / pinning review
8. App integrity / NativeGate
9. Final operator chain (mais difícil — destrava ao consolidar as anteriores)

### Como registrar progresso

Todos os endpoints da cadeia exigem um **Bearer token** (login `guest`/`guest123`).

**Ver progresso** — estado por estágio, dicas e evidência esperada (nunca retorna
flags):

```
GET /api/mobile/challenge/progress
Authorization: Bearer <token>
```

**Submeter a flag de um estágio** — substitua `<flag_redacted>` pela flag que
você encontrou:

```
POST /api/mobile/challenge/submit
Authorization: Bearer <token>
Content-Type: application/json

{
  "stageId": "stage-01-recon",
  "flag": "<flag_redacted>",
  "evidence": "descrição curta de como você obteve a flag"
}
```

- **Correto:** `accepted: true` com `pointsAwarded` e `totalScore`.
- **Incorreto:** `accepted: false`.
- **Idempotente:** reenviar uma flag já aceita não duplica pontos.

**Ver o placar:**

```
GET /api/mobile/challenge/scoreboard
Authorization: Bearer <token>
```

Retorna `totalScore`, `solvedStages`, `completionPercent` e `finalUnlocked`.

### Como registrar evidências

Use o campo `evidence` do submit para anotar **como** chegou à flag (endpoint,
header, payload, hook/patch, captura de tráfego). Mantenha uma linha objetiva por
estágio e guarde artefatos em `evidence/` quando fizer sentido. **Não** cole a
flag dentro do texto de evidência — ela já vai no campo `flag`.

### Dicas graduais

- Cada estágio expõe **duas dicas** (`hintLevel1`, `hintLevel2`) e um resumo
  público via `GET /api/mobile/challenge/progress`. Comece pela dica de nível 1.
- Vários checkpoints são **disparados por um header de revisão** ou um **campo de
  body** específico — pense em "modo de auditoria/revisão" das rotas que você já
  conhece. Os valores exatos fazem parte do desafio.
- Quando a trilha for de app (componentes exportados, bridge, device trust,
  vault, integridade), correlacione o que você vê no app/`adb`/Frida com o que o
  backend aceita.
- A etapa final só responde quando você prova que dominou as trilhas internas — e
  só com o estado de confiança correto. Os requisitos exatos fazem parte do
  desafio.

### Troubleshooting

- **`/health` não responde:** confirme `docker compose up` e a porta `8102`.
- **`401`/sem dados:** seu token expirou — refaça login `guest`/`guest123`.
- **Submit `accepted:false`:** confira o `stageId` e a flag exata (sem espaços).
- **Emulador não conecta:** o app deve usar `http://10.0.2.2:8102`.
- **Celular físico não conecta:** use a tela **API Host** com `http://<IP_DO_PC>:8102`.
- **Placar zerou:** o scoreboard é em memória e reinicia junto com o backend.

---

## Passo a passo manual sugerido

Esta é uma trilha **prática e manual**, no estilo "trabalho de formiga": cada
passo é uma estação de investigação. Ela **não entrega flags nem soluções** — diz
o que fazer e o que observar, e deixa o "como" da exploração com você. Faça os
passos na ordem que fizer sentido e **registre evidências** ao longo do caminho.

> Tudo aqui é **local e autorizado** (`127.0.0.1:8102` / emulador
> `10.0.2.2:8102`). Nada deste lab deve ser usado contra apps ou sistemas reais.

### 1. Preparar o backend

- Entre na pasta `lab-08-obsidianpay`.
- Suba o ambiente: `docker compose up --build` (use `-d` para rodar ao fundo).
- Confirme a saúde do serviço: `curl -s http://127.0.0.1:8102/health`.
- Confirme que a API responde na **porta 8102** (campo `expectedPort`).

### 2. Preparar o app

- Abra o **Android Studio**.
- Abra a pasta `android-app` e espere o **Gradle sync** terminar.
- Rode o app em um **emulador** (Pixel, API 24+).
- Lembre que, no emulador, o app fala com `http://10.0.2.2:8102` (alias para o
  `127.0.0.1` do host).
- Em **celular físico**, use a tela **API Host** apontando para `http://<IP_DO_PC>:8102`.

### 3. Login inicial

- Faça login com `guest` / `guest123` (já vem preenchido).
- Confirme que a tela **Home** carrega e lista os fluxos do app.

### 4. Recon mobile

- Olhe a **config** do app/API e observe endpoints e caminhos citados.
- Mapeie as telas e o que cada fluxo envia/recebe.
- **Registre evidências** das pistas encontradas (sem usar scanner).

### 5. Armazenamento local

- Use o app normalmente para **gerar estado** (abra recibos, cartões, suporte).
- Procure dados em **SharedPreferences**, **SQLite**, **cache** e **filesDir**.
- Registre **onde** achou tokens, perfis, cache e artefatos.

### 6. API e objetos previsíveis

- Compare as **listas** com os **detalhes** por ID.
- Teste IDs previsíveis de forma **controlada** (no seu ambiente local).
- Registre a diferença entre acessar **seu próprio objeto** e o de **outro perfil**.

### 7. WebView e bridge

- Abra o **Web Support** (portal de suporte em WebView).
- Observe a **bridge** disponível à página e **identifique os métodos expostos**.
- Registre o **impacto** do que a bridge consegue mostrar dentro do app.

### 8. Deep links e QR

- Teste deep links `obsidianpay://` (transfer/support/receipt).
- Processe **payloads** pela tela QR Payment.
- Registre os **eventos** que o app gera ao interpretar essa entrada.

### 9. Componentes exportados

- Use o **ADB local** para interagir com os componentes do app.
- Teste a **Activity**, o **Receiver** e o **Provider** exportados.
- Cada componente, quando acionado, deixa uma **prova** no estado local; o
  **ContentProvider** consolida as provas. Junte as três provas e envie-as ao
  **checkpoint** do backend (com um token válido) para destravar a flag deste
  estágio — você **não** precisa abrir nenhum arquivo de código do backend.
- Registre as evidências de cada interação (saídas do ADB + resposta do checkpoint).

### 10. Reverse engineering

- Abra o APK no **JADX**/**apktool**.
- Procure **secrets**, **signing**, **routes** e **hints** no código.
- Registre os achados de análise estática.

### 11. Device Trust

- Entenda os **headers** que o fluxo Device Trust monta no cliente.
- Compare a **assinatura esperada** com o que o backend aceita.
- Registre **por que** confiar no que o cliente afirma é frágil.

### 12. Root / Emulator / Biometric / Integrity

- Observe as telas **Security Check**, **Secure Vault** e **App Integrity**.
- Use os **scripts Frida do lab** quando aplicável (instrumentação controlada).
- Registre **quais** checagens são apenas locais (client-side).

### 13. Network / Pinning / API Host

- Entenda a diferença entre **emulador** e **celular físico**.
- **Burp** é opcional (proxy/intercept) para observar o tráfego local.
- Registre o **network profile** observado (cleartext, pinning report-only).

### 14. Submissão de progresso

- Consulte `GET /api/mobile/challenge/progress` para ver os 9 estágios e dicas.
- Submeta cada flag em `POST /api/mobile/challenge/submit` — no corpo, use o campo
  `flag` com `<flag_redacted>` neste guia (a flag real você obtém **resolvendo** a
  trilha, nunca procurando no código).
- Acompanhe `GET /api/mobile/challenge/scoreboard`.
- **Não revele flags** em anotações públicas.

### 15. Evidências finais

Para cada estágio resolvido, junte evidência objetiva e reprodutível:

- print ou texto da **requisição/resposta**;
- **tela** do app;
- **arquivo local** (prefs/SQLite/cache);
- **comando ADB** usado;
- trecho de **JADX**;
- **resposta do scoreboard**.

> Este passo a passo é deliberadamente investigativo: ele aponta as estações, mas
> a descoberta é sua. Comandos prontos (curl/adb/frida) e a solução completa só
> existem em `WALKTHROUGH.md`, que é **material de instrutor**.

---

## 5. O que NÃO fazer

- ❌ **Não** use estas técnicas contra apps, sistemas ou pessoas reais.
- ❌ **Não** rode scanners agressivos/automáticos — o lab é manual por design.
- ❌ **Não** procure flags no repositório. Não há flags reais nos arquivos
  públicos; "achar" no código não é o objetivo e não vale.
- ❌ **Não** ataque nada fora de `127.0.0.1:8102`.

Mantenha tudo **local** e **autorizado**.

---

## 6. Status desta fase

- ✅ API mobile rica disponível (porta 8102) — **Fase 2**
- ✅ Conta de teste `guest` / `guest123`
- ✅ Contratos mobile: perfil, recibos, cartões, suporte, transfer preview, WebView
- ✅ **Secure Vault** com fluxo de autenticação local (biometria + fallback) — **Fase 10**
- ✅ **API Host override** para emulador e celular físico; scaffold de pinning — **Fase 11**
- ✅ **App Integrity** com NativeGate e TamperCheck scaffold — **Fase 12**
- ✅ **Dynamic Instrumentation** (scripts Frida, playbook ADB) — **Fase 13**
- ✅ **Final Challenge Chain** — 9 estágios, scoring local e submissão de flags (`docs/CHALLENGE-SCORING.md`) — **Fase 14**
- 🔜 APK Android (próximas fases)
- 🔜 Integração app ↔ API e cadeias completas

---

## 7. Testando em emulador e celular físico (Fase 11)

O app foi construído para funcionar em dois ambientes sem rebuild:

**Android Emulator:**
O emulador Android usa `10.0.2.2` como alias para o `127.0.0.1` do computador.
A URL padrão do app já aponta para esse endereço. Nenhuma configuração extra é
necessária.

**Celular físico (dispositivo real na mesma rede):**
O celular não consegue alcançar `127.0.0.1` do seu computador. Para conectar:
1. Descubra o IP do seu PC na rede local (ex.: `192.168.0.50`).
2. No app, abra a tela **API Host** (no menu principal).
3. Digite a URL com o IP do PC: `http://192.168.0.50:8102`.
4. Toque **Save Base URL**.

O override é salvo localmente no app e mantido entre reinicializações.

> O objetivo dessas configurações é facilitar o ambiente de teste, não entregar
> uma solução. Explore o que o app registra localmente quando você troca a URL.

> A API já é um alvo de verdade para análise manual. Explore os contratos,
> valide o ambiente e entenda o produto. O componente Android — onde mora boa
> parte do desafio — vem a seguir.

---

## 8. Fechamento — guia prático do aluno

Esta seção reúne o essencial para você começar com o pé direito e entregar um
trabalho bem documentado. **Sem solução e sem flags** — só método.

### Antes de começar

- **Suba o backend** (`docker compose up --build -d`) e confirme `/health` em
  `http://127.0.0.1:8102`.
- **Faça o build do app** no Android Studio e instale num emulador (API 24+).
  O passo a passo está em [docs/ANDROID-BUILD-CHECKLIST.md](./docs/ANDROID-BUILD-CHECKLIST.md).
- **Confirme o API Host:** emulador usa `http://10.0.2.2:8102`; celular físico
  usa `http://<IP_DO_PC>:8102` via tela **API Host**.
- **Tenha as ferramentas à mão:** `adb`, Burp Suite, JADX, apktool, Frida,
  objection (ver seção 4). Ambiente de pentest detalhado em
  [docs/mobile-pentest/SETUP.md](./docs/mobile-pentest/SETUP.md).
- **Leia a pontuação pública:** [docs/CHALLENGE-SCORING.md](./docs/CHALLENGE-SCORING.md).

### Checklist do aluno

- [ ] Backend no ar (`/health` ok) e login `guest` / `guest123` funcionando.
- [ ] App instalado e Home carregando no emulador (ou celular físico).
- [ ] Token obtido (`POST /api/mobile/login`) para os endpoints autenticados.
- [ ] `GET /api/mobile/challenge/progress` consultado para ver os 9 estágios e as dicas.
- [ ] Recon feito (telas, storage local, config, fluxos do app) antes de atacar.
- [ ] Para cada estágio: flag obtida **resolvendo a trilha**, não procurando no código.
- [ ] Flag submetida em `POST /api/mobile/challenge/submit` com `evidence`.
- [ ] Scoreboard acompanhado (`GET /api/mobile/challenge/scoreboard`).
- [ ] Evidências guardadas em `evidence/` quando fizer sentido.

### Quando pedir ajuda

- Releia as **duas dicas** de cada estágio (`hintLevel1`, `hintLevel2`) via
  `GET /api/mobile/challenge/progress` antes de pedir ajuda.
- Se travar **mais de ~30 min** sem progresso novo em um estágio, peça uma dica
  ao instrutor — descrevendo **o que já tentou**, não pedindo a resposta.
- Em problema de ambiente (app não conecta, porta ocupada, emulador), volte ao
  **Troubleshooting** (seção 4.2) e ao checklist de build antes de pedir ajuda.

### Como evitar falsos positivos

- **Não confunda "achar no repositório" com resolver.** Não há flags reais nos
  arquivos públicos; uma string parecida em doc/código **não vale**.
- **Confirme com o backend.** Uma flag só conta quando `POST /challenge/submit`
  responde `accepted: true`. `accepted: false` = flag errada para aquele estágio.
- **Cuidado com o `stageId`.** Submeter a flag certa no `stageId` errado falha.
- **Espaços e caixa.** Copie a flag exata, sem espaços extras.
- **Idempotência ≠ erro.** Reenviar uma flag já aceita devolve `duplicate: true`
  e `pointsAwarded: 0` — é o comportamento esperado, não uma falha.
- **Resultado client-side não é prova.** O app pode mostrar "desbloqueado" sem o
  servidor concordar — valide sempre pela resposta do backend.

### O que entregar como evidência

Para cada estágio resolvido, registre uma evidência objetiva:

- **No submit:** preencha o campo `evidence` com **como** chegou à flag (endpoint,
  header, payload, hook/patch, captura de tráfego) — uma linha por estágio.
- **Não cole a flag** dentro do texto de evidência — ela já vai no campo `flag`
  (use `<flag_redacted>` se precisar referenciar uma flag em anotações públicas).
- **Artefatos:** salve logs, capturas (Burp), scripts Frida usados e saídas de
  `adb` em `evidence/`, nomeando por estágio (ex.: `stage-04-bridge.txt`).
- **Reprodutibilidade:** a evidência deve permitir que outra pessoa repita o passo
  decisivo.
