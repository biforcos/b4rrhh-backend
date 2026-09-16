# B4RRHH — HR & Payroll Engine

> A personnel administration system and configurable payroll engine built around domain-driven design, hexagonal architecture, and temporal data integrity — with zero tolerance for shortcuts.

## What you get when you clone this

**An empty product.** Start PostgreSQL and run it (see *Running the project* below): Flyway
applies the 133 migrations and leaves you a schema, three rule systems, 187 catalogue
entities (85 of them `ESP`, including one real collective agreement from the Spanish BOE),
38 payroll-engine concepts with their assignments and salary tables — and **zero
employees**. Nothing is calculated, because there is nobody to calculate.

Filling it is a documented, reproducible run, not a dump you are handed:
**`FABRICAR-SEMILLA.md`, in the `b4rrhh/deploy` repository**. Seven steps — blank database,
migrations, bulk hire, full-month calculation — and it states up front the row counts that
have to come out, so you can tell a good seed from a plausible one.

What this product is when you take the public demo away, and what it still lacks, is in
`PRODUCTO.md` (workspace root).

---

## What problem does this solve?

HR and payroll systems tend to collapse under one of two failure modes:

- **Over-engineering early**: event buses, microservices, CQRS — before the domain is understood.
- **Under-engineering late**: CRUD controllers that accumulate business logic until the system is unmaintainable.

B4RRHH takes a different path. It models **the actual domain** — employee lifecycle, temporal employment data, a graph-based payroll calculation engine — before introducing any infrastructure abstraction. The architecture follows from the domain, not from a framework tutorial.

---

## Architecture at a glance

```
bounded context → vertical (domain slice) → hexagonal layer
```

```
com.b4rrhh.employee.contract.application.usecase.ReplaceContractFromDateService
           ^^^^^^^^ ^^^^^^^^ ^^^^^^^^^^^
           context  vertical     layer
```

Two bounded contexts:

| Context | Verticals |
|---------|-----------|
| `employee` | employee, presence, contact, address, identifier, contract, labor\_classification, cost\_center, work\_center, working\_time, payroll\_input |
| `rulesystem` | company, company\_profile, work\_center, catalog\_binding, catalog\_option, rule\_entity |

Hard rules enforced throughout:
- Domain classes have **zero** Spring or JPA imports
- Business logic never touches controllers or repositories
- APIs expose **business keys only** — no surrogate IDs, ever

---

## The Payroll Engine

This is the core of the system. It is not a calculator. It is a **configurable, graph-based execution engine** for payroll concepts.

### Eight modules with clear responsibilities

```
payroll_engine/
├── concept      — concept catalog: calculation type, operands, feed relations
├── object       — payroll objects (assignable salary tables)
├── table        — salary value lookup tables
├── eligibility  — determines which concepts apply to a payroll unit
├── dependency   — builds the DAG of concept dependencies with cycle detection
├── planning     — resolves topological execution order
├── segment      — splits the calculation period at intra-period changes
└── execution    — runs calculators per segment, accumulates results
```

### Calculation types

Every payroll concept declares exactly how it computes its value:

| Type | Description |
|------|-------------|
| `DIRECT_AMOUNT` | Fixed amount from salary table lookup |
| `RATE_BY_QUANTITY` | Rate × quantity (e.g. hours worked) |
| `PERCENTAGE` | Percentage of another concept's result |
| `AGGREGATE` | Sum of other concepts |
| `ENGINE_PROVIDED` | Value injected by the engine (SS group, payroll type) |
| `EMPLOYEE_INPUT` | Employee-declared value (e.g. voluntary pension) |
| `GREATEST` | max(computed, floor) — used for SS contribution floors |
| `LEAST` | min(computed, cap) — used for SS contribution caps |

### Execution flow

```
Payroll Unit
    │
    ▼
Eligibility Filter          ← which concepts apply to this employee?
    │
    ▼
Dependency Graph Build      ← DAG construction, cycle detection
    │
    ▼
Topological Sort            ← resolves execution order across the graph
    │
    ▼
Segmentation                ← splits month at mid-period changes
    │  (contract change on the 15th → two independent segments)
    ▼
Calculator Execution        ← runs per segment, per concept, in dependency order
    │
    ▼
Result Accumulation         ← merges segments into final payroll totals
```

No concept executes before its dependencies. No hardcoded payroll logic anywhere in the codebase.

```mermaid
flowchart TB
    Launch[["POST /payrolls/launch"]]

    subgraph Orchestration["Launch Orchestration"]
        Run[calculation_run\ncreated + persisted]
        Claim[calculation_claim\nunit-level mutex]
    end

    subgraph Selection["Population Resolution"]
        Employees[Employee records]
        Presences[Presence periods]
        Inputs[Payroll inputs]
    end

    subgraph Engine["Payroll Engine"]
        direction TB
        Eligibility["Eligibility Filter\nwhich concepts apply?"]
        Graph["Dependency Graph\nDAG + cycle detection"]
        Topo["Topological Sort\nexecution order"]
        Segments["Segmentation\nsplit at mid-period changes"]
        Calc["Calculator Execution\nper segment · per concept · in order"]
    end

    subgraph Config["Engine Configuration (DB)"]
        Concepts["Concepts\n8 calculation types"]
        Assignments[Assignments]
        Tables[Salary tables]
        Feeds[Feed relations]
        Operands[Operands]
    end

    subgraph Result["Payroll Result"]
        Root[payroll root\nimmutable]
        Lines[concept lines]
        Snapshot[context snapshot]
        Status["status workflow\nNOT_VALID → CALCULATED → CLOSED"]
    end

    Launch --> Run
    Run --> Claim
    Claim --> Selection
    Selection --> Eligibility
    Config --> Eligibility
    Eligibility --> Graph
    Feeds --> Graph
    Operands --> Graph
    Graph --> Topo
    Topo --> Segments
    Segments --> Calc
    Tables --> Calc
    Calc --> Root
    Root --> Lines & Snapshot & Status
```

### Concurrent safety

Payroll launches are protected by an explicit domain model — not a `synchronized` block:

- **`calculation_run`** — persisted record of a launch: population, progress, status
- **`calculation_claim`** — unit-level mutex; two concurrent runs cannot calculate the same payroll unit

---

## Temporal data integrity

Most employee data is historized. The system enforces this without exception.

### Strong Timeline Replace

A reusable pattern (`StrongTimelineReplacePlanner`) governs any `replaceFromDate` operation across all temporal verticals:

```
Existing periods:     [Jan ─────────── Jun] [Jul ─────────── Dec]
replaceFromDate(Mar):
Result:               [Jan ── Feb] [Mar ──── Jun] [Jul ─────────── Dec]
                                    ^^^^^^^^^^^^
                                    new period, boundaries recalculated
```

Three outcomes — all handled, none silently ignored:

- **Exact match** — new period replaces existing at the same start date
- **Split** — existing period is divided; new period takes over from effective date
- **No coverage** — rejected; gaps in the timeline are a domain error

This same logic governs contracts, labor classifications, cost centers, work centers, and working time.

---

## Employee lifecycle as explicit workflows

Creating an employee is not a POST to `/employees`. It is a **Hire workflow** that coordinates multiple verticals atomically:

```
HireEmployee
├── create employee record
├── open presence period
├── assign contract
├── assign labor classification
├── assign cost center
├── assign work center
└── assign working time
```

`TerminateEmployee` and `RehireEmployee` follow the same discipline. Each workflow enforces cross-vertical consistency and temporal rules that cannot be expressed as independent CRUD operations.

---

## API design

The API operates entirely on **functional identifiers**:

```http
GET  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contract
PUT  /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/labor-classification/replace-from-date
GET  /payrolls/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/{payrollPeriodCode}/{payrollTypeCode}/{presenceNumber}
```

No `id` path variable exists anywhere in this API. This is intentional and documented in [ADR-001](docs/architecture/adr/ADR-001-vertical-architecture-and-api-identity.md).

---

## Architecture Decision Records

Every non-obvious decision is documented. There are currently **31 ADRs** covering:

- Vertical architecture and API identity strategy
- Employee business key design
- Rule entity metamodel
- Employee lifecycle workflows
- Strong timeline replace pattern
- Employee journey model
- Cost center design
- Company as enriched, rule-anchored catalog
- Payroll status workflow and state machine
- Payroll root model (immutable result, not editable record)
- Concurrent launch orchestration with calculation\_run and claim
- Hierarchical authorization model
- UI interaction contracts per vertical

Full bundle: [`docs/architecture/adr/ADR_BUNDLE.md`](docs/architecture/adr/ADR_BUNDLE.md)

---

## Domain coverage — Spain, Régimen General

The system models real Spanish HR and payroll law through the concept graph:

- **Grupo de cotización** (SS contribution group, 1–11) — drives salary tables and contribution rates
- **SS employer contributions**: contingencias comunes, desempleo (empresa), FOGASA, formación profesional, MEI
- **SS worker deductions**: CC trabajador (4.70%), desempleo (1.55%), FP (0.10%), MEI (0.11%)
- **IRPF withholding** as a line on the payslip
- **Convenio colectivo** — agreement category profiles with real salary tables
- Topes de cotización (contribution floors and caps via `GREATEST` / `LEAST`)

Two honest caveats, because the shape and the values are not equally finished. The
**shape** is data: what feeds what, in what order, and at what scope, all lives in the
concept graph. The **rates** are not: ten `ENGINE_PROVIDED` calculators return a constant
written in Java, and the IRPF one is a flat 15% placeholder that never looks at the
employee's declared tax situation. The parameterised table (`payroll_engine.ss_cotizacion_tipos`,
nine contingencies with validity dates) exists and nothing reads it. Only the two
contribution caps read from a table. See `PRODUCTO.md` §2 in the workspace root.

---

## By the numbers

Counted against the tree on 2026-09-16, not from memory.

| Metric | Value |
|--------|-------|
| Java source files | 1,675 |
| Test files | 405 |
| Flyway migrations | 133 |
| Architecture Decision Records | 65 |
| Payroll engine modules | 9 |
| Calculation types | 8 |
| Bounded contexts | 5, plus a deliberately small `shared` |
| Employee domain verticals | 18 |
| Declared API paths / operations | 91 / 152 |

---

## Running the project

**Requirements:** Java 21, Docker

```bash
# 1. Start PostgreSQL
cd docker/postgres && docker compose up -d

# 2. Run the application (Flyway migrations run automatically on startup)

# Linux / macOS / Git Bash:
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Windows PowerShell (the -D flag must be quoted):
mvn spring-boot:run "-Dspring-boot.run.profiles=local"

# Alternatively, set the env var (works everywhere):
# $env:SPRING_PROFILES_ACTIVE='local'; mvn spring-boot:run   (PowerShell)
# SPRING_PROFILES_ACTIVE=local mvn spring-boot:run            (bash)

# 3. Run all tests. They run against REAL PostgreSQL, not H2: Testcontainers
#    starts one (so Docker must be running), or set TEST_DB_HOST to supply
#    your own — which is what the pipeline does.
mvn test

# Run a specific test class
mvn test -Dtest=CalculatePayrollUnitServiceTest
```

Credentials: `b4rrhh / b4rrhh` at `localhost:5432/b4rrhh`

---

## Project structure

```
src/main/java/com/b4rrhh/
├── employee/           ← 11 domain verticals
├── rulesystem/         ← catalog and configuration management
├── payroll/            ← payroll domain (launch, run, claim, result)
├── payroll_engine/     ← 8-module graph-based calculation engine
├── authorization/      ← JWT + role-based access (ADMIN, HR_MANAGER, HR_VIEWER)
└── shared/             ← minimal cross-cutting abstractions

src/main/resources/
└── db/migration/       ← 91 Flyway migrations (schema evolution + seed data)

openapi/
└── personnel-administration-api.yaml   ← source of truth for the API contract

docs/
└── architecture/adr/   ← 31 Architecture Decision Records
```

---

## Tech stack

- **Java 21** — records, sealed classes, pattern matching
- **Spring Boot 3.3** — web, data JPA, security
- **PostgreSQL** — primary store
- **Flyway** — schema versioning and seed data
- **Docker** — local database
- **H2** — test isolation (no external dependencies in CI)

---

## Status

Active development. The payroll engine is the current focus: concept graph construction, segmentation, and the SS/IRPF concept chain all work end to end — with the rate caveat noted under *Domain coverage*. The launch orchestration model (`calculation_run` + `calculation_claim`) is implemented and runs the whole workforce in one go.

This is a solo project. The pace is deliberate — correctness over speed.

---

## Why this exists

Most HR backends I have seen were either too simple (CRUD over employee tables) or too complex (event sourcing, CQRS, microservices) for a domain that doesn't need that complexity yet.

This project is an exploration of what it looks like to model a genuinely complex domain — temporal employment data, payroll calculation graphs, lifecycle workflows — with enough discipline that the system stays comprehensible as it grows.

The answer, so far: vertical slices + hexagonal architecture + explicit temporal patterns + documented decisions.

---

## License

This project is distributed under a **Business Source License (BSL)**.

Source code is publicly visible for learning, evaluation, and non-commercial use.  
Commercial use — including SaaS, hosted services, or revenue-generating products — is **not permitted** without an explicit commercial license.

See [`LICENSE.md`](LICENSE.md) for full terms and [`NOTICE.md`](NOTICE.md) for authorship details.

For commercial licensing inquiries, contact the author.
