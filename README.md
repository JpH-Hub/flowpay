# FlowPay API

API responsável por atribuir chamados a um atendente disponível. O chamado chega de outra API já com o time definido; esta API apenas gerencia a fila e a alocação de atendentes.

## Requisitos

- Java 21
- Docker e Docker Compose
- Gradle (ou `./gradlew` incluso no projeto)

## Como subir o ambiente

### 1. Banco de dados

Suba os containers PostgreSQL (dev na porta `5433`, testes na `5434`):

```bash
docker compose up -d
```

### 2. Aplicação

Com o banco rodando, inicie a API:

```bash
./gradlew bootRun
```

A aplicação conecta em `jdbc:postgresql://localhost:5433/flowpay_db`. As migrations Flyway rodam automaticamente na subida.

## Como rodar os testes

Testes unitários (sem banco):

```bash
./gradlew test
```

Testes de integração (exige o container `postgres-test` na porta `5434`):

```bash
docker compose up -d postgres-test
./gradlew integrationTest
```

## Como a API funciona hoje

### Capacidade do sistema

| Recurso | Limite |
|---|---|
| Times | 3 (Cartões, Empréstimos, Outros Assuntos) |
| Atendentes por time | 3 |
| Chamados em atendimento | 9 (1 por atendente) |
| Chamados na fila | 3 (global) |
| Total gerenciado | 12 |

### Status dos chamados (`TicketStatus`)

| Status | Descrição |
|---|---|
| `IN_SERVICE` | Em atendimento, com atendente atribuído |
| `QUEUED` | Na fila, aguardando atendente livre no time |
| `CLOSED` | Finalizado |
| `REJECTED` | Recusado (fila cheia) |

### Fluxo de atribuição (`TicketService.assignTicket`)

Simula a abertura de um chamado que já veio de outra API com time definido.

Entrada: `conversationRef`, `subject`, `teamId`.

1. Valida se o **time** (`teamId`) existe.
2. Se houver atendente livre no time → status `IN_SERVICE`.
3. Se todos estiverem ocupados e a fila global tiver vaga (< 3) → status `QUEUED`.
4. Se a fila estiver cheia → status `REJECTED`.

O `subject` é apenas informativo; o roteamento usa exclusivamente o `teamId`.

### Fluxo de finalização (`TicketService.closeTicket`)

1. Chamados `IN_SERVICE` ou `QUEUED` podem ser fechados.
2. Ao fechar um chamado em atendimento, o atendente é liberado.
3. Se existir chamado na fila do mesmo time, o mais antigo é promovido para `IN_SERVICE`.

### Camadas implementadas

```
Controller  →  (não implementado)
Service     →  TicketService
Repository  →  TeamRepository, AgentRepository, TicketRepository
Database    →  PostgreSQL + Flyway (V1 schema, V2 seed)
```

A camada REST ainda não existe. A lógica de negócio está disponível via `TicketService` e pode ser consumida por controllers futuros.

## Estrutura do banco

- **teams** — equipes de atendimento
- **agents** — atendentes vinculados a um time
- **tickets** — chamados com referência de conversa, assunto, status, time e atendente

## CI

O pipeline no GitHub Actions executa testes unitários, testes de integração (com PostgreSQL) e gera o JAR da aplicação.
