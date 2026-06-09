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
