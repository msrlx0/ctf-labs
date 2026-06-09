# Guia do Aluno — Lab 08: ObsidianPay Mobile

Bem-vindo(a) ao ObsidianPay, uma carteira digital fictícia usada para treino de
segurança mobile. Este guia te orienta na **Fase 1**, que estabelece o backend e
a fundação do laboratório. **O aplicativo Android ainda não foi entregue** — ele
chega em fases futuras.

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

- ✅ Backend mobile disponível (porta 8102)
- ✅ Conta de teste `guest` / `guest123`
- 🔜 APK Android (próximas fases)
- 🔜 Desafios e cadeias do app

> Por enquanto, familiarize-se com a API, valide o ambiente e entenda o produto.
> O componente Android — onde mora a maior parte do desafio — vem a seguir.
