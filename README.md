# Ecossistema de Microsserviços com Autenticação MFA e Controle de Acesso (RBAC)

Este repositório contém a implementação prática de um ecossistema distribuído de microsserviços voltado para a autenticação segura em duas etapas (MFA) via código OTP enviado por e-mail, gerenciamento dinâmico de perfis e controle de acesso baseado em regras (Roles). Projeto desenvolvido para a disciplina de Desenvolvimento Web 3.

---

## 1. Descrição da Arquitetura

O sistema adota o padrão de arquitetura de microsserviços, sendo totalmente desacoplado e orientado a eventos assíncronos para o disparo de notificações.

* **Frontend (Node.js + Express + EJS):** Alocado na porta `3000`, gerencia as interfaces visuais do usuário, controla o fluxo de sessões locais e atua como proxy reverso resiliente para as rotas protegidas da Dashboard.
* **UserService (Spring Boot + Spring Security):** Alocado na porta `8081`, funciona como o núcleo (Core) da aplicação, gerenciando a persistência de dados, regras de negócio de usuários, validação estrutural e criptografia.
* **EmailService (Spring Boot Worker):** Alocado na porta `8082`, atua de forma assíncrona consumindo as mensagens da fila de mensageria para efetuar o disparo real dos códigos OTP para o Gmail do usuário.
* **Mensageria (RabbitMQ / CloudAMQP):** Agente intermediário responsável pelo gerenciamento da fila assíncrona `default.email`, garantindo que o `UserService` não sofra gargalos ou dependência direta do envio de e-mails.

---

## 2. Pré-requisitos

Para inicializar o ecossistema localmente, certifique-se de possuir os seguintes componentes instalados e configurados:

* **Java Development Kit (JDK):** Versão 21 ou superior configurada.
* **Node.js & NPM:** Versão v22 ou superior estável.
* **MySQL Server:** Versão 8.0 ou superior com o serviço ativo.
* **Conta CloudAMQP:** Instância ativa de um broker RabbitMQ na nuvem.
* **Conta do Gmail:** Configurada previamente com uma *App Password* (Senha de App de 16 dígitos) para o protocolo SMTP seguro.

---

## 3. Instruções de Configuração e Ambiente

### Banco de Dados (MySQL)
Antes de rodar a aplicação, acesse o seu terminal MySQL ou Workbench e crie o schema que receberá as tabelas geradas automaticamente pelo Hibernate:

```sql
CREATE DATABASE ms_user;
```

### Variáveis de Ambiente e Propriedades
Certifique-se de configurar os arquivos `application.properties` correspondentes de cada microsserviço Java com as credenciais corretas:

#### 3.1. UserService (Core)
Configure as credenciais do banco de dados MySQL e o endereço de conexão com o RabbitMQ no arquivo `UserService/secrets/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ms_user?useSSL=false&serverTimezone=UTC
spring.datasource.username=seu_usuario_mysql
spring.datasource.password=sua_senha_mysql
spring.rabbitmq.addresses=amqps://seu_usuario_cloudamqp:sua_senha@broker.cloudamqp.com/vhost
```

#### 3.2. EmailService (Worker)
Configure as credenciais do servidor SMTP do Gmail para envio de e-mails e a mesma fila do RabbitMQ no arquivo `EmailService/email/src/main/resources/application.properties`:

```properties
spring.rabbitmq.addresses=amqps://seu_usuario_cloudamqp:sua_senha@broker.cloudamqp.com/vhost
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=seu_email_remetente@gmail.com
spring.mail.password=sua_app_password_de_16_digitos
```

## 4. Passos para Executar os Serviços
### Execução Automatizada (Recomendado)
O ecossistema conta com um script de inicialização única via PowerShell. Para abrir e rodar todos os três serviços em terminais isolados de forma automática, abra o terminal na raiz do projeto e execute:
```bash
./iniciar.ps1
```

### Execução Manual
Caso prefira rodar os serviços manualmente, abra três terminais distintos e execute os comandos abaixo na ordem indicada:
* Terminal 1 - Frontend:
```bash
cd Frontend
npm install
node server.js
```

* Terminal 2 - UserService (Core):
```bash
cd UserService/secrets
cmd.exe /c mvnw.cmd spring-boot:run
```

* Terminal 3 - EmailService (Worker):
```bash
cd EmailService/email
cmd.exe /c mvnw.cmd spring-boot:run
```

* Após a inicialização completa, acesse o sistema pelo seu navegador na URL: `http://localhost:3000/login`

## 5. Capturas de Tela do Fluxo Completo
Abaixo constam as evidências visuais do fluxo de ponta a ponta em pleno funcionamento local:

* 5.1 Tela de Cadastro do usuário
![Cadastro de Usuário](./docs/screenshots/tela-cadastro.png)

* 5.2 Tela de Login
![Cadastro de Login](./docs/screenshots/tela-login.png)

* 5.3 Tela de Autenticação MFA
![Autenticação MFA](./docs/screenshots/tela-cod-verificacao.png)

* 5.4 Recebimento do Código OTP (Caixa de Entrada)
![Código OTP](./docs/screenshots/tela-email.png)

* 5.5 Tela Complementar de Perfil (/register)
![Completar Perfil](./docs/screenshots/tela-completar-perfil.png)

* 5.6 Dashboard Protegida e Validação de Endpoints
![Dashboard](./docs/screenshots/tela-dashboard.png)

* 5.7 Persistência dos Dados no MySQL
![Dados MySQL](./docs/screenshots/tela-mysql.png)