# B4RRHH — backend

**B4RRHH is a personnel administration system and a configurable payroll engine.**
Employment history is temporal by construction — the domain itself refuses overlaps and
gaps instead of hoping the database will catch them — and payroll is computed from a
dependency graph that is configuration rather than code, so any amount on a payslip can be
opened all the way down to the step that produced it.

This repository is the backend: the domain, the engine, and the OpenAPI contract that the
backoffice and the designer generate their clients from. Everything else — the other
repositories and the documents they share — starts at the workspace repository, which is
[`../README.md`](../README.md) once it is laid out beside this one. **That repository is
not mirrored to GitHub**, so if you arrived from
[github.com/biforcos](https://github.com/biforcos) this page is the way in, and the
siblings to lay out beside it are `b4rrhh-backend`, `b4rrhh-frontend`, `b4rrhh-designer`
and `b4rrhh-workforce-loader`, cloned into `b4rrhh_backend`, `b4rrhh_frontend`,
`b4rrhh_designer` and `b4rrhh_workforce_loader`.

---

## What you get when you clone

**An empty product.** Start the database and run it: Flyway applies the migrations and
leaves you a schema, the rule systems, the `ESP` catalogue — including one real collective
agreement from the Spanish BOE — and the payroll engine's concepts with their assignments
and salary tables. And **nobody**. Nothing is calculated, because there is no one to
calculate.

Filling it is a documented run, not a dump you are handed: `FABRICAR-SEMILLA.md`, in the
`b4rrhh/deploy` repository. It states the row counts that have to come out before you
start, so you can tell a good seed from a plausible one.

## Running it

**You need** a JDK 21 or newer and Docker.

```bash
# 1. PostgreSQL and MinIO (employee photos go straight to the object store)
cd docker/postgres && docker compose up -d

# 2. The application. Flyway migrates on startup.
mvn spring-boot:run -Dspring-boot.run.profiles=local

# On PowerShell the -D argument has to be quoted whole, or the shell eats it:
#   mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The database is `b4rrhh / b4rrhh` at `localhost:5432/b4rrhh`. The `local` profile opens a
development token endpoint; there is no user table behind it, and what that means is in
`PRODUCTO.md` §2 in the workspace root.

```bash
mvn test                  # everything
mvn test -Dtest=SomeTest  # one class — quote the whole -D on PowerShell
```

Tests run against **real PostgreSQL**, not H2. Testcontainers starts one, so Docker must
be running; or set `TEST_DB_HOST` and supply your own, which is what the pipeline does.
The schema under test is the real Flyway schema: no test declares a table.

---

## The architecture

One package name carries the whole map:

```
com.b4rrhh.employee.contract.application.usecase.ReplaceContractFromDateService
           ^^^^^^^^ ^^^^^^^^ ^^^^^^^^^^^
           context  vertical    layer
```

| Bounded context | What lives there |
|---|---|
| `employee` | The facts about a person, sliced into verticals: presence, contract, address, cost centre, working time, absences, journey, lifecycle… |
| `rulesystem` | The configurable catalogue: rule systems, entity types and entities, companies, agreements, catalogue bindings. |
| `payroll_engine` | The metamodel — how a payroll is calculated: concepts, operands, feeds, eligibility, the dependency graph, planning, execution. |
| `payroll` | An already calculated payroll: the result, its concept lines, its runs, its steps. |
| `authorization` | Hierarchical resources, semantic actions, reusable permission profiles. |
| `shared` | Deliberately small. |

The border between the last two payroll contexts is ADR-042, and it is one sentence: what
defines **how** a payroll is calculated belongs to `payroll_engine`; an **already
calculated** payroll belongs to `payroll`.

Inside a vertical: `domain/model` and `domain/port`, `application/usecase`,
`infrastructure/persistence` and `infrastructure/web`. No JPA entity or Spring Data
repository ever appears in a domain package, and the API never exposes a domain object.
Keeping Spring itself out of the domain is convention and review rather than a test, which
is the honest version: the rule is not free.

## Time is inside the model, not beside it

Most employee data is a timeline, and several verticals write through the same planner
(`employee/temporal/`). It holds two invariants that the domain enforces and the database
does not: **no overlap**, and **no gap inside the presence**.

That is why `replaceFromDate` is not an update. It plans: an exact match replaces, a
mid-period change splits and recalculates boundaries, and something that would leave a
hole is rejected as a domain error rather than written and regretted.

ADR-057 is the decision underneath: a time series is governed by its invariants, not by
the operations offered on it.

## Lifecycle is a workflow, not a POST

Hiring is not `POST /employees`. Hire, terminate and rehire are orchestrated flows built
on the participant pattern (ADR-047): the orchestrating service knows nothing about the
verticals, and each vertical registers a participant with an explicit `order()`. Adding a
vertical to a flow means writing a participant, never editing the orchestrator.

**The order is load-bearing.** On termination the presence closes *first*, and everything
derived from it closes after. Closing from the inside out — like a destructor — is the
intuitive reading, and it is the one that used to be here: it does not work, because every
other vertical validates its periods against the presence, so while the presence still ran
to 9999-12-31 the coverage never added up and closing the contract was rejected as a gap.
`TerminationCoversEveryPresenceVerticalTest` reads the source tree and fails if a closable
vertical that depends on presence is missing from the flow.

## The payroll engine

It calculates by graph, not by a service per concept.

A concept declares how it computes — a direct amount, a rate by quantity, a percentage of
another concept, an aggregate, a greatest or a least, a value the engine provides, a value
the employee declares — and which other concepts feed it. Eligibility is resolved from
assignments, dependencies are expanded, the graph is checked for cycles, and the
topological plan is built **once** per execution.

How often each concept is then evaluated is its own declaration. A `SEGMENT` concept is
evaluated once per temporal segment and its results composed; a `PERIOD` concept is
evaluated once over the whole period. So a month split in two by a working-time change
leaves **more calculation steps than there are concepts** — which is why the unit of the
execution trace is a step and not a concept (`payroll.payroll_calculation_step`), and why
a payslip line knows which steps it merges.

Two rules that took a while to find their words:

- **No operand crosses from segment to period** (ADR-058). If it did, the same number
  would mean two things depending on where you read it.
- **Rounding happens where a rate is applied, and nothing is rounded twice** (ADR-066).

**A new payroll concept is parameterisation, not Java**: an object, a concept, its operands
or feeds, and an assignment. The one exception is the `ENGINE_PROVIDED` technical
calculators, which may resolve values looked up or derived from the execution context —
rates, limits, days — and must never calculate an economic concept.

The shape is data. The **rates are not yet**: several of those calculators still return a
constant written in Java while the parameterised table sits there unread, and the IRPF one
is a flat placeholder. That, and everything else this does not do yet, is written down
honestly in `PRODUCTO.md` §2 — read it before believing any of the above is finished.

## A receipt is a view of what the engine calculated

`NOT_VALID → CALCULATED → EXPLICIT_VALIDATED → DEFINITIVE`. Invalidating returns to
`NOT_VALID`, and recalculating only leaves from there; nothing moves inside a definitive
receipt. The engine decides whether a receipt is *valid*; people decide whether it is
*closed* (ADR-059). Invalidating asks for a reason, because it is a decision about
something that was fine; closing does not, because it keeps the one the receipt had.

Launching is accepted, not awaited (ADR-060): a run is persisted, and a per-unit claim
means two concurrent runs cannot calculate the same payroll unit. First one in wins, and
the one that loses says so (ADR-065). The regulation a run loaded is immutable inside it
(ADR-061).

## The API

There is **one** contract: `openapi/personnel-administration-api.yaml`.

There used to be two, and they drifted in silence for five months until they left an
endpoint served and invisible to the generated client. They were merged, and the rule that
remains is: *what is served is declared, and declared in one place.*
`TheTwoContractsNeverDivergeInSilenceTest` is what keeps it that way — it checks that what
the backend serves is declared, and that there is still only one file.

Identity is functional. Business keys, never surrogate ids:

```http
GET /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contract
PUT /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/labor-classification/replace-from-date
```

No `id` path variable exists anywhere in it (ADR-001). Errors carry a code, a message and
a detail (ADR-064).

Two repositories consume this contract and version their own copy of it, checking it
against `main` on every build. Keeping them up to date is a manual step that happens
somewhere else, so this repository does not rely on anyone remembering:
`openapi/avisar-consumidores.py` looks at each consumer's copy after a contract change and
names the one that is behind, with the command that fixes it.

## Where the decisions are

Every non-obvious decision has an ADR, in `docs/architecture/adr/`, regenerated into
[`ADR_BUNDLE.md`](docs/architecture/adr/ADR_BUNDLE.md). Read it before proposing an
architectural change — most of what looks missing was decided, and says why.

If an ADR and the tree disagree, **the tree wins**: the thing to do is write the note or
the successor ADR that says so, not implement a stale document.

## Tech

Java 21, Spring Boot, PostgreSQL, Flyway, MinIO for photos, Docker for both locally.

## Where the backlog is

The threads that produced these decisions live in a **private Gitea** and are not
mirrored: the Issues tab here is empty, and a `(#93)` or a `b4rrhh/backend#91` in a commit
message points at something you cannot open from GitHub. It is a known limitation, and it
leaves in reach the half that is worth more anyway — **the why is written inside the
commit**, not behind the link.

## License

Business Source License. The source is visible for learning and evaluation; commercial use
— SaaS, hosted services, revenue-generating products — needs an explicit licence. See
[`LICENSE.md`](LICENSE.md) and [`NOTICE.md`](NOTICE.md).
