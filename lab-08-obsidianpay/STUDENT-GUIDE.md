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
- 🔜 APK Android (próximas fases)
- 🔜 Integração app ↔ API e cadeias completas

> A API já é um alvo de verdade para análise manual. Explore os contratos,
> valide o ambiente e entenda o produto. O componente Android — onde mora boa
> parte do desafio — vem a seguir.
