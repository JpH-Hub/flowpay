# FlowPay API

API responsável por rotear chamados para o time correto, gerenciar filas e alocar atendentes disponíveis.

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

Testes de integração e E2E (exige o container `postgres-test` na porta `5434`):

```bash
docker compose up -d postgres-test
./gradlew integrationTest
```

## Endpoints

### `POST /tickets`

Cria um chamado e atribui a um atendente ou fila.

**Request body:**

```json
{
  "conversationRef": "WHATS-123",
  "subject": "Problema com meu Cartão de Crédito"
}
```

**Response `201 Created`:**

```json
{
  "id": 1,
  "conversationRef": "WHATS-123",
  "subject": "Problema com meu Cartão de Crédito",
  "team": "Cartões",
  "status": "IN_SERVICE",
  "agentId": 1
}
```

**Erros:**

| Status | Cenário |
|---|---|
| `400` | Campos obrigatórios ausentes ou em branco |
| `409` | `conversationRef` já existe |

### `PATCH /tickets/{id}/close`

Finaliza um chamado em atendimento ou na fila.

**Response `200 OK`:**

```json
{
  "ticketId": 1,
  "status": "CLOSED"
}
```

**Erros:**

| Status | Cenário |
|---|---|
| `400` | Ticket já fechado, rejeitado ou status inválido |
| `404` | Ticket não encontrado |

## Como a API funciona

### Capacidade do sistema

| Recurso | Limite |
|---|---|
| Times | 3 (Cartões, Empréstimos, Outros Assuntos) |
| Atendentes por time | 3 |
| Chamados por atendente | 3 simultâneos |
| Chamados em atendimento por time | 9 (3 atendentes × 3 chamados) |
| Chamados na fila por time | 3 |
| Total gerenciado | 36 (27 em atendimento + 9 na fila) |

### Status dos chamados (`TicketStatus`)

| Status | Descrição |
|---|---|
| `IN_SERVICE` | Em atendimento, com atendente atribuído |
| `QUEUED` | Na fila, aguardando atendente livre no time |
| `CLOSED` | Finalizado |
| `REJECTED` | Recusado (fila cheia) |

### Roteamento por assunto (`TeamRoutingService`)

O time é determinado automaticamente a partir do campo `subject`:

| Palavra-chave no assunto | Time |
|---|---|
| `cartão` / `cartao` | Cartões |
| `empréstimo` / `emprestimo` | Empréstimos |
| Demais casos | Outros Assuntos |

Acentos são normalizados (ex.: `"Cartão"` e `"cartao"` roteiam para Cartões).

### Fluxo de atribuição (`TicketService.assignTicket`)

Entrada: `conversationRef`, `subject`.

1. Determina o **time** a partir do `subject`.
2. Se houver atendente com menos de 3 chamados em atendimento no time → status `IN_SERVICE`.
3. Se todos estiverem no limite e a fila **do time** tiver vaga (< 3) → status `QUEUED`.
4. Se a fila do time estiver cheia → status `REJECTED`.

### Fluxo de finalização (`TicketService.closeTicket`)

1. Chamados `IN_SERVICE` ou `QUEUED` podem ser fechados.
2. Ao fechar um chamado em atendimento, o atendente é liberado.
3. Se existir chamado na fila do mesmo time, o mais antigo é promovido para `IN_SERVICE`.

### Camadas implementadas

```
Controller  →  TicketController
Service     →  TicketService, TeamRoutingService
Repository  →  TeamRepository, AgentRepository, TicketRepository
Database    →  PostgreSQL + Flyway (V1 schema, V2 seed)
```

## Estrutura do banco

- **teams** — equipes de atendimento
- **agents** — atendentes vinculados a um time
- **tickets** — chamados com referência de conversa, assunto, status, time e atendente

## CI

O pipeline no GitHub Actions executa testes unitários, testes de integração/E2E (com PostgreSQL) e gera o JAR da aplicação.
