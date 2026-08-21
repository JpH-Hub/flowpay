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


## Collection de Testes (Bruno) & Guia de Simulação

A API possui uma collection pronta na pasta `bruno/` contendo as requisições organizadas para testar todas as regras de roteamento, capacidade e fila.

### Como importar no Bruno
1. Abra o aplicativo **Bruno**.
2. Clique em **Open Collection**.
3. Selecione a pasta `bruno/` localizada na raiz deste repositório.

### Estrutura da Collection
- **`01 - Criar Ticket/`**
    - `criar - Cartões`: Dispara requisições com palavras-chave de Cartões.
    - `criar - Empréstimos`: Dispara requisições com palavras-chave de Empréstimos.
    - `criar - Outros`: Dispara requisições com qualquer outro assunto.
- **`02 - Fechamento/`**
    - `fechar - Ticket`: Executa o `PATCH /tickets/{id}/close`.

---

### Roteiro de Teste Passo a Passo (Validando Capacidades)

Para testar o comportamento completo do sistema em um único time (ex: **Cartões**), execute as requisições na seguinte sequência:

#### 1. Preenchendo a Capacidade dos Atendentes (`IN_SERVICE`)
Execute a requisição **`criar - Cartões` 9 vezes** consecutivas (alterando ligeiramente o `conversationRef` se necessário).
- **Resultado esperado:** Os 9 chamados retornarão status `201 Created` com `"status": "IN_SERVICE"`.
- **O que aconteceu:** Os 3 atendentes do time de Cartões receberam 3 chamados cada, atingindo o limite máximo individual.

#### 2. Testando a Fila de Espera (`QUEUED`)
Execute a requisição **`criar - Cartões` mais 3 vezes** (10ª, 11ª e 12ª chamadas).
- **Resultado esperado:** O retorno será status `201 Created`, porém com `"status": "QUEUED"` e `"agentId": null`.
- **O que aconteceu:** Como todos os atendentes estavam ocupados, o sistema direcionou os chamados para a fila do time de Cartões, preenchendo as 3 vagas da fila.

#### 3. Testando o Estouro da Fila (`REJECTED`)
Execute a requisição **`criar - Cartões` pela 13ª vez**.
- **Resultado esperado:** O retorno será status `201 Created` com `"status": "REJECTED"` e `"agentId": null`.
- **O que aconteceu:** O time atingiu a capacidade máxima total (9 em atendimento + 3 na fila = 12). O 13º chamado foi recusado automaticamente pela regra de negócio.

#### 4. Testando a Promoção Automática da Fila
Pegue o `id` de um dos primeiros chamados criados (status `IN_SERVICE`) e rode a requisição **`fechar - Ticket`** passando esse `id`.
- **Resultado esperado:** O chamado selecionado passa para `"status": "CLOSED"`.
- **O que aconteceu:** O atendente vinculado foi liberado e o sistema promoveu instantaneamente o chamado **`QUEUED` mais antigo** para `"IN_SERVICE"`, atribuindo-o ao mesmo atendente sem necessidade de intervenção manual.

### Testando Cenários de Erro (Validações e Exceções)

Você pode simular os principais fluxos de exceção da API disparando as requisições abaixo:

#### 1. Referência de Conversa Duplicada (`409 Conflict`)
- **Como testar:** Envie um `POST /tickets` com um `conversationRef` que já foi enviado anteriormente (ex.: `"WHATS-123"`).
- **Resposta da API (`409 Conflict`):**
  ```json
  {
    "error": "Já existe um chamado ativo para esta conversa."
  }


## CI

O pipeline no GitHub Actions executa testes unitários, testes de integração/E2E (com PostgreSQL) e gera o JAR da aplicação.

## Monitoramento

- GET /monitoring: retorna o histórico recente de tickets finalizados (status CLOSED ou REJECTED), ordenado do mais recente para o mais antigo. A resposta contém: id, chatRef, subject, status, agentName (pode ser null para REJECTED), teamName, closedAt, rejectedAt, rejectionReason. Por hora o endpoint retorna os últimos 20 registros.

