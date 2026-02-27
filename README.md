<div align="center">
<h1>🔗 Encurtador de Links</h1>
<p>Encurtador de URLs de alto desempenho com <strong>Arquitetura Hexagonal</strong> e <strong>System Design</strong></p>

![Java](https://img.shields.io/badge/Java-21-orange?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-green?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)
![Kong](https://img.shields.io/badge/Kong-3.9-teal?logo=kong)
![Docker](https://img.shields.io/badge/Docker-Compose-blue?logo=docker)
</div>

---

## 📋 Índice

- [Visão Geral](#-visão-geral)
- [Arquitetura](#-arquitetura)
- [Pré-requisitos](#-pré-requisitos)
- [Execução Local (Dev)](#-execução-local-dev--sem-docker)
- [Execução com Docker](#-execução-com-docker-compose)
- [Endpoints e Swagger](#-endpoints-e-swagger)
- [Testes](#-testes)
- [Estrutura do Projeto](#-estrutura-do-projeto)
- [CI/CD](#-cicd)
- [Variáveis de Ambiente](#-variáveis-de-ambiente)

---

## 🎯 Visão Geral

API REST para encurtamento de URLs que aplica conceitos de **System Design**:

| Tecnologia | Função |
|---|---|
| **Spring Boot 4** + Java 21 | Aplicação principal |
| **PostgreSQL 16** (1 primary + 3 réplicas) | Persistência com streaming replication |
| **Redis 7** | Cache com padrão Cache-Aside (TTL 24h) |
| **Kong 3.9** | API Gateway (rate limiting, roteamento) |
| **GitHub Actions** | CI/CD (build → docker → helm deploy) |
| **Helm** | Packaging para Kubernetes |

---

## 🏛️ Arquitetura

```
Cliente → Kong (8000) → Spring Boot App (8080)
                              │
                    ┌─────────┴──────────┐
                    ▼                    ▼
               Redis Cache         PostgreSQL
               (leitura 1ª)        ┌─ Primary (escrita)
                                   ├─ Réplica 1 (leitura)
                                   ├─ Réplica 2 (leitura)
                                   └─ Réplica 3 (leitura)
```

**Padrão de roteamento automático:**
```java
@Transactional               // → PostgreSQL PRIMARY
@Transactional(readOnly=true) // → Réplica (round-robin)
```

---

## ✅ Pré-requisitos

| Ferramenta | Versão mínima | Download |
|---|---|---|
| **Java (JDK)** | 21 | [temurin.adoptium.net](https://adoptium.net) |
| **Docker Desktop** | 4.x | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** | qualquer | [git-scm.com](https://git-scm.com/) |

> **Maven** não precisa ser instalado — o projeto inclui o **Maven Wrapper** (`./mvnw`).

---

## 🚀 Execução Local (Dev — sem Docker)

Usa **H2 em memória** — zero configuração necessária.

```bash
# 1. Clone o repositório
git clone https://github.com/seu-usuario/encurtador.git
cd encurtador

# 2. Suba a aplicação com perfil dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows PowerShell:
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Acesse:
- **API:** `http://localhost:8080`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **H2 Console:** `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:encurtadordb`
  - User: `sa` | Senha: (vazio)

---

## 🐳 Execução com Docker Compose

Sobe toda a infraestrutura: Kong, PostgreSQL (primary + réplicas), Redis.

### Passo 1 — Copiar e configurar o `.env`

```bash
# Crie o arquivo .env (já existe um template no repositório)
cp .env.example .env   # ou edite diretamente o .env
```

Conteúdo do `.env`:
```env
DB_USER=encurtador
DB_PASSWORD=encurtador123
REDIS_PASSWORD=redis123
```

### Passo 2 — Subir a infraestrutura

```bash
docker compose up -d
```

Aguarde todos os serviços ficarem `healthy` (~30 segundos):
```bash
docker compose ps
```

### Passo 3 — Subir a aplicação apontando para o Docker

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker

# Windows PowerShell:
.\mvnw spring-boot:run "-Dspring-boot.run.profiles=docker"
```

### Passo 4 — Testar via Kong

A aplicação está acessível diretamente na porta 8080 e via Kong na porta 8000.

```bash
# Criar URL curta (via Kong)
curl -X POST http://localhost:8000/api/urls \
  -H "Content-Type: application/json" \
  -d '{"urlOriginal": "https://www.google.com"}'

# Resposta:
# {"codigo":"aBcD1234","urlCurta":"http://localhost:8080/aBcD1234","urlOriginal":"https://www.google.com"}

# Redirecionar (via Kong)
curl -L http://localhost:8000/aBcD1234
```

### Parar tudo

```bash
docker compose down
# Para remover volumes também:
docker compose down -v
```

---

## 🐋 Build e execução via Dockerfile

```bash
# Build da imagem
docker build -t encurtador:local .

# Executar (apontando para os serviços do Docker Compose)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-primary:5432/encurtador \
  -e SPRING_DATASOURCE_PASSWORD=encurtador123 \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_DATA_REDIS_PASSWORD=redis123 \
  --network encurtador_encurtador-net \
  encurtador:local
```

---

## 📖 Endpoints e Swagger

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `POST` | `/api/urls` | Cria URL encurtada | 201 |
| `GET` | `/{codigo}` | Redireciona para URL original | 302 |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`

**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Exemplo com curl

```bash
# Encurtar
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"urlOriginal": "https://github.com"}'

# Redirecionar (segue o 302 com -L)
curl -Lv http://localhost:8080/{codigo}
```

### Exemplo com HTTPie

```bash
http POST localhost:8080/api/urls urlOriginal=https://github.com
http -v localhost:8080/{codigo}
```

---

## 🧪 Testes

```bash
# Rodar todos os testes
./mvnw test

# Windows PowerShell:
.\mvnw test
```

> Os testes não precisam de banco ou Redis — usam `@MockitoBean` nos ports de saída.

---

## 📁 Estrutura do Projeto

```
encurtador/
├── .github/workflows/ci.yml          # Pipeline CI/CD
├── docker/
│   └── postgres/                     # Scripts de replicação PostgreSQL
│       ├── postgresql.conf
│       ├── pg_hba.conf
│       ├── primary/init.sh
│       └── replica/init-replica.sh
├── helm/encurtador/                  # Helm chart para Kubernetes
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
├── src/main/java/com/encurtador/
│   ├── domain/                       # Núcleo — zero dependências externas
│   ├── application/                  # Casos de uso + Ports
│   ├── adapter/                      # REST, JPA, Redis
│   └── infrastructure/              # RoutingDataSource, OpenAPI config
├── src/main/resources/
│   ├── application.properties        # Configuração base
│   ├── application-dev.properties    # Perfil dev (H2)
│   └── application-docker.properties # Perfil docker (PostgreSQL + Redis)
├── docker-compose.yml                # Infraestrutura completa
├── Dockerfile                        # Multi-stage build
├── .env                              # Credenciais locais (não commitar!)
└── pom.xml
```

---

## ⚙️ CI/CD

O pipeline do **GitHub Actions** roda automaticamente em push para `main`:

```
push main
  └── Job 1: Build & Test (mvnw test)
        └── Job 2: Docker Build & Push (ghcr.io)
              └── Job 3: helm upgrade --install (Kubernetes)
```

### Secrets necessários no GitHub

Vá em *Settings → Secrets and variables → Actions* e adicione:

| Secret | Descrição | Como obter |
|---|---|---|
| `KUBECONFIG` | kubeconfig em base64 | `cat ~/.kube/config \| base64` |
| `DB_PASSWORD` | Senha PostgreSQL | Definida no seu ambiente |
| `REDIS_PASSWORD` | Senha Redis | Definida no seu ambiente |

> `GITHUB_TOKEN` (para o GHCR) é gerado automaticamente pelo GitHub Actions.

---

## 🔐 Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil ativo: `dev` ou `docker` |
| `SPRING_DATASOURCE_URL` | H2 (dev) | JDBC URL do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `encurtador` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | — | Senha do banco |
| `SPRING_DATA_REDIS_HOST` | `localhost` | Host do Redis |
| `SPRING_DATA_REDIS_PASSWORD` | — | Senha do Redis |

---

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feat/minha-feature`
3. Commit: `git commit -m 'feat: adiciona minha feature'`
4. Push: `git push origin feat/minha-feature`
5. Abra um Pull Request para `main`
