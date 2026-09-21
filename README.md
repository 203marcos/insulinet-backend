# Insulinet API (Java)

API REST do **Insulinet**, uma aplicação para controle de estoque de insulina, registro de doses e estimativa de autonomia com base no histórico de consumo.

Reimplementação em Java/Spring Boot do backend original em Python (FastAPI), mantendo os mesmos endpoints, regras de negócio, autenticação e schema de banco de dados.

## Tecnologias

- Java 21
- Spring Boot 3
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Flyway
- JWT (JJWT)
- Argon2 (Spring Security Crypto)
- Resend (envio de e-mail)
- JUnit 5
- Mockito
- Testcontainers
- Maven
- Docker / Docker Compose

## Funcionalidades

- Cadastro e autenticação de usuários
- Recuperação e redefinição de senha
- Cadastro e edição de insulinas
- Controle de entradas e ajustes de estoque
- Registro individual e em lote de doses
- Histórico de movimentações
- Cálculo do estoque atual
- Estimativa de consumo médio e projeção de dias restantes
- Isolamento dos dados por usuário

## Estrutura

```text
src/
├── main/
│   ├── java/com/insulinet/api/
│   │   ├── controller/   # recebe requisições HTTP, valida entrada, chama services
│   │   ├── service/      # regras de negócio e orquestração
│   │   ├── repository/   # persistência (Spring Data JPA)
│   │   ├── model/
│   │   │   ├── entity/   # entidades JPA
│   │   │   ├── dto/      # requests/responses da API
│   │   │   └── enums/
│   │   ├── mapper/       # Entity <-> DTO
│   │   ├── client/       # integrações externas (Resend)
│   │   ├── security/     # JWT
│   │   ├── config/       # configuração Spring (security, cors, jackson, etc.)
│   │   └── exception/    # exceções de domínio e handler global
│   └── resources/
│       ├── application.yml
│       ├── application-local.yml
│       ├── application-test.yml
│       └── db/migration/ # migrations Flyway
└── test/
    └── java/com/insulinet/api/
```

## Configuração local

Clone o repositório e entre na pasta do projeto.

Crie um arquivo `.env` a partir do `.env.example`:

```bash
cp .env.example .env
```

Nunca versione o arquivo `.env`.

### Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET_KEY` | Chave HS256 (mínimo 32 caracteres) |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | Validade do token de acesso (padrão 1440) |
| `PASSWORD_RESET_EXPIRE_MINUTES` | Validade do token de redefinição de senha (padrão 30) |
| `APP_TIMEZONE` | Fuso horário usado para doses e projeção (padrão `America/Fortaleza`) |
| `RESEND_API_KEY` | Chave da API do Resend, usada para enviar e-mail de recuperação de senha |
| `EMAIL_FROM` | Remetente do e-mail de recuperação |
| `FRONTEND_URL` | Usado para montar o link de redefinição de senha |
| `CORS_ORIGINS` | Origens permitidas, separadas por vírgula |
| `PORT` | Porta HTTP exposta pela aplicação (padrão 8000) |

> Nota para quem migra do backend Python: lá a conexão era um único `DATABASE_URL`; aqui ela é dividida em `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`.

## Executando com Docker (recomendado)

```bash
docker compose up --build
```

Isso inicia o PostgreSQL e a aplicação. As migrations Flyway são aplicadas automaticamente na inicialização.

API local:

```text
http://localhost:8000
```

## Executando localmente sem Docker

Requer Java 21 e um PostgreSQL acessível (configurado via `.env`/variáveis de ambiente).

```bash
./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test
```

Inclui testes unitários (services) e de integração (Testcontainers, sobe um PostgreSQL descartável automaticamente). É necessário ter o Docker em execução para os testes de integração.

## Principais endpoints

### Autenticação

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/forgot-password
POST /api/auth/reset-password
```

### Usuário

```text
GET /api/users/me
```

### Insulinas

```text
POST  /api/insulins
GET   /api/insulins
PATCH /api/insulins/{insulin_id}
GET   /api/insulins/{insulin_id}/history
GET   /api/insulins/{insulin_id}/summary
```

### Estoque

```text
POST  /api/insulins/{insulin_id}/stock
GET   /api/insulins/{insulin_id}/stock
GET   /api/insulins/{insulin_id}/containers
POST  /api/insulins/{insulin_id}/containers/{container_id}/discard
POST  /api/insulins/{insulin_id}/adjustments
PATCH /api/insulins/{insulin_id}/stock/{movement_id}
```

### Doses

```text
POST   /api/insulins/{insulin_id}/doses
PATCH  /api/insulins/{insulin_id}/doses/{dose_id}
DELETE /api/insulins/{insulin_id}/doses/{dose_id}
POST   /api/insulins/{insulin_id}/dose-batches
```

## Segurança

- Hash de senhas com Argon2id (Spring Security Crypto)
- Tokens JWT (HS256) para autenticação
- Associação dos registros ao usuário autenticado
- Variáveis sensíveis fora do controle de versão
- Configuração explícita de CORS

## Diferenças conhecidas em relação ao backend Python

- Registrar/editar/lançar doses em lote para uma insulina inativa retorna `409 Conflict` (no Python isso era permitido; apenas operações de estoque bloqueavam).
- `DATABASE_URL` (Python) foi dividido em `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`.

## Status

Projeto em migração a partir do backend Python (`insulinet-backend`), mantido como referência durante o processo.
