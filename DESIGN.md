# Design Document — Encurtador de Links

**Versão:** 1.0  
**Data:** 2026-02-24  
**Autor:** Iagos  
**Status:** Implementado

---

## 1. Contexto e Objetivo

Construir um serviço de encurtamento de URLs que seja:

- **Performático:** sub-milissegundo para redirecionamentos (caminho quente via cache)
- **Escalável:** suporte a alto volume de leituras sem degradar o banco de dados
- **Resistente a falhas:** sem single point of failure nas leituras
- **Manutenível:** código bem desacoplado, fácil de testar e evoluir

---

## 2. Requisitos

### Funcionais
| # | Requisito |
|---|---|
| RF-01 | `POST /api/urls` — recebe URL original, retorna código curto (Base62, 8 chars) |
| RF-02 | `GET /{codigo}` — redireciona (HTTP 302) para a URL original |
| RF-03 | URLs têm TTL de 24 horas |
| RF-04 | URL inválida retorna HTTP 422 (formato errado ou esquema não `http/https`) |
| RF-05 | Código inexistente ou expirado retorna HTTP 404 |

### Não-Funcionais
| # | Requisito |
|---|---|
| RNF-01 | p99 de redirecionamento < 10ms (cache hit) |
| RNF-02 | Suporte a leituras distribuídas via múltiplas réplicas PostgreSQL |
| RNF-03 | Zero downtime em deploys (rolling update via Kubernetes) |
| RNF-04 | Sem dados sensíveis em logs ou variáveis de ambiente no código |

---

## 3. Arquitetura do Sistema

### 3.1 Diagrama de Infraestrutura

```
                    ┌─────────────────────────────┐
                    │         Kong Gateway         │
                    │  • Rate Limiting             │
                    │  • Roteamento                │
                    │  • Observabilidade           │
                    └──────────────┬──────────────┘
                                   │ :8000
                    ┌──────────────▼──────────────┐
                    │       Spring Boot App        │
                    │  • Arquitetura Hexagonal     │
                    │  • Java 21, Spring Boot 4    │
                    └──┬──────────────────────┬───┘
                       │                      │
          ┌────────────▼──────┐   ┌───────────▼────────┐
          │    Redis Cache     │   │  PostgreSQL Cluster │
          │  • allkeys-lru     │   │                     │
          │  • 256MB max       │   │  ┌─ Primary  (RW)   │
          │  • TTL: 24h        │   │  ├─ Réplica 1 (RO)  │
          └───────────────────┘   │  ├─ Réplica 2 (RO)  │
                                   │  └─ Réplica 3 (RO)  │
                                   └─────────────────────┘
```

### 3.2 Arquitetura da Aplicação — Hexagonal (Ports & Adapters)

```
┌─────────────────────────────────────── Aplicação ────────────────────────────────────────┐
│                                                                                            │
│   ┌──────────────┐          ┌───────────────────────────┐         ┌───────────────────┐  │
│   │ Adapters IN  │          │       Application         │         │  Adapters OUT     │  │
│   │              │          │                           │         │                   │  │
│   │ UrlController│──calls──▶│  CriarUrlUseCase (port)   │         │                   │  │
│   │  POST/GET    │          │  CriarUrlService (impl)   │──uses──▶│ UrlRepositoryPort │  │
│   │              │          │                           │         │ (UrlJpaAdapter)   │  │
│   │              │          │  RedirecionarUrlUseCase   │         │                   │  │
│   │              │          │  RedirecionarUrlService   │──uses──▶│    CachePort      │  │
│   └──────────────┘          └───────────────────────────┘         │ (RedisCacheAdapter│  │
│                                         │                         └───────────────────┘  │
│                               ┌─────────▼──────────┐                                     │
│                               │     Domain (puro)   │                                     │
│                               │  UrlEncurtada record │                                    │
│                               │  Exceções de domínio │                                    │
│                               └─────────────────────┘                                     │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Decisões de Design (ADRs)

### ADR-01: Geração de código — Base62 sobre UUID

**Contexto:** Precisamos de um identificador curto, único e URL-safe.

**Decisão:** Usar os primeiros 8 caracteres do UUID em Base62 (`a-z`, `A-Z`, `0-9`).

**Alternativas consideradas:**
| Opção | Prós | Contras |
|---|---|---|
| **Base62 sobre UUID** ✅ | Curto (8 chars), sem dependências, espaço de ~218 trilhões | Colisão teórica (extremamente improvável) |
| Hash MD5/SHA | Determinístico | Mais longo, colisões possíveis |
| Counter auto-increment | O mais curto | Previsível, expõe volume de dados |
| NanoID | Criptograficamente seguro | Dependência externa |

**Consequência:** Probabilidade de colisão em 8 chars Base62 ≈ 1 em 218 trilhões. Acceptable para os requisitos atuais.

---

### ADR-02: Cache-Aside para redirecionamentos

**Contexto:** Redirecionamentos são a operação mais frequente. Precisamos de latência mínima.

**Decisão:** Padrão **Cache-Aside** com Redis.

**Fluxo:**
```
GET /{codigo}
  1. Busca no Redis → HIT (>95% das requisições) → redirect 302 (~0.1ms)
  2. MISS → lê da réplica PostgreSQL → armazena no Redis → redirect 302 (~3ms)
```

**Alternativas:**
| Padrão | Prós | Contras |
|---|---|---|
| **Cache-Aside** ✅ | Simples, sem lock, dados no cache quando precisados | Latência maior no 1º acesso |
| Write-Through | Cache sempre consistente | Escreve no cache mesmo para URLs nunca acessadas |
| Read-Through | Lógica de cache no proxy | Maior complexidade, precisa de biblioteca |

**TTL:** 24 horas — balanceia frescor dos dados e taxa de cache hit.

---

### ADR-03: Roteamento Primary/Réplica automático

**Contexto:** Leituras não precisam do primary. Queremos escalar horizontalmente sem mudar o código de negócio.

**Decisão:** `AbstractRoutingDataSource` do Spring — detecta `@Transactional(readOnly=true)` e roteia para réplica automaticamente.

```java
// O desenvolvedor só precisa marcar a transação:
@Transactional               // → PRIMARY (escrita)
@Transactional(readOnly=true) // → Réplica (round-robin automático)
```

**Round-robin com `AtomicInteger`:** garante distribuição uniforme entre as 3 réplicas sem estado externo.

---

### ADR-04: Arquitetura Hexagonal

**Contexto:** Queremos testabilidade e facilidade de trocar tecnologias de infra no futuro.

**Decisão:** Ports & Adapters (Hexagonal).

**Vantagens práticas obtidas:**
- `contextLoads()` usa `@MockitoBean` nos ports — sem banco ou Redis no CI
- Trocar PostgreSQL por outro banco = só mudar `UrlJpaAdapter`
- Domínio (`UrlEncurtada` record) sem uma linha de anotação de framework

**Regra de dependência:** `adapter → application → domain` (nunca ao contrário).

---

### ADR-05: `record` Java para entidade de domínio

**Contexto:** Entidades de domínio devem ser imutáveis por design.

**Decisão:** `record UrlEncurtada(...)` em vez de classe com getters/setters.

**Benefício:** imutabilidade garantida pelo compilador, `equals`/`hashCode`/`toString` gerados automaticamente. A `UrlEntity` JPA é uma classe separada, confinada ao pacote `persistence`.

---

## 5. Modelo de Dados

### Tabela `url_encurtada`

```sql
CREATE TABLE url_encurtada (
    id         BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    codigo     VARCHAR(10)   NOT NULL UNIQUE,  -- índice único automático
    url_original VARCHAR(2048) NOT NULL,
    criada_em  TIMESTAMP NOT NULL,
    expira_em  TIMESTAMP                       -- NULL = nunca expira
);

CREATE UNIQUE INDEX idx_url_encurtada_codigo ON url_encurtada(codigo);
```

**Por que `BIGINT` e não `UUID` como PK?**
- Menor espaço em disco e índice mais eficiente
- O `codigo` Base62 é o identificador público — a PK é apenas interna

---

## 6. Fluxos Detalhados

### 6.1 Encurtar URL (escrita)

```
POST /api/urls
     │
     ├─ [Bean Validation] @Valid @NotBlank @Pattern → 400 se inválido
     │
     ▼
UrlController.criar()
     │
     ▼
CriarUrlService.criar(urlOriginal)
     ├─ validarUrl()  → UrlInvalidaException (422) se esquema ≠ http/https
     ├─ GeradorCodigoBase62.gerar()  → "aBcD1234"
     ├─ UrlEncurtada.nova(codigo, url, expiraEm)
     │
     ├─ @Transactional → RoutingDataSource → PRIMARY
     │     └─ UrlJpaAdapter.salvar()
     │
     └─ RedisCacheAdapter.armazenar("url:aBcD1234", urlOriginal, TTL=24h)
     │
     ▼
201 Created { codigo, urlCurta, urlOriginal }
```

### 6.2 Redirecionar (leitura — caminho crítico)

```
GET /aBcD1234
     │
     ▼
UrlController.redirecionar("aBcD1234")
     │
     ▼
RedirecionarUrlService.buscarPorCodigo("aBcD1234")
     │
     ├─ RedisCacheAdapter.buscar("url:aBcD1234")
     │     ├─ HIT  → UrlEncurtada(codigo, urlOriginal) → redirect 302  ← ~0.1ms
     │     └─ MISS ↓
     │
     ├─ @Transactional(readOnly=true) → RoutingDataSource → RÉPLICA (round-robin)
     │     └─ UrlJpaAdapter.buscarPorCodigo("aBcD1234")
     │           ├─ Não encontrado → UrlNaoEncontradaException → 404
     │           └─ Encontrado ↓
     │
     ├─ urlEncurtada.expirou() → true → 404
     │
     ├─ RedisCacheAdapter.armazenar(...)  ← repopula para próximas requisições
     │
     └─ redirect 302 { Location: urlOriginal }  ← ~3-5ms
```

---

## 7. Pipeline CI/CD

```
push para main
      │
      ▼
Job 1: Build & Test
  ./mvnw test
  ./mvnw package -DskipTests
      │
      ▼ (apenas se Job 1 passou)
Job 2: Docker Build & Push
  docker build -t ghcr.io/usuario/encurtador:sha-{commit}
  docker push → GitHub Container Registry
      │
      ▼ (apenas se Job 2 passou)
Job 3: Helm Deploy
  helm upgrade --install encurtador ./helm/encurtador \
    --set image.tag=sha-{commit} \
    --set secrets.dbPassword=${{ secrets.DB_PASSWORD }}
```

**PRs** executam apenas o Job 1 (build & test) — segurança antes do merge.

---

## 8. Decisões Futuras / Evolução

| Item | Decisão atual | Evolução planejada |
|---|---|---|
| **Secrets** | GitHub Secrets | AWS Secrets Manager + external-secrets-operator |
| **Colisões de código** | Não tratadas (probabilidade negligenciável) | Retry com novo código (loop até 3x) |
| **Analytics** | Não implementado | Kafka → contador de cliques por URL |
| **Autenticação** | Sem autenticação | Kong plugin `key-auth` ou `jwt` |
| **Expiração** | TTL fixo de 24h | TTL configurável por usuário |
| **URLs customizadas** | Não implementado | `POST /api/urls` com campo `codigoCustom` opcional |
