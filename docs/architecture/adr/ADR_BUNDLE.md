# ADR Bundle

> Fichero generado automáticamente. No editar a mano.
> Fecha de generación: 2026-08-30 08:44:52

---

## Índice

- [ADR-001-vertical-architecture-and-api-identity.md](#file-adr-001-vertical-architecture-and-api-identity-md)
- [ADR-002-employee-contact-vertical.md](#file-adr-002-employee-contact-vertical-md)
- [ADR-003-rule-entity-metamodel-strategy.md](#file-adr-003-rule-entity-metamodel-strategy-md)
- [ADR-004-employee-business-key-strategy.md](#file-adr-004-employee-business-key-strategy-md)
- [ADR-005-arquitectura_por_verticales_y_reglas_api.md](#file-adr-005-arquitectura-por-verticales-y-reglas-api-md)
- [ADR-006_rule_entity_type_domain.md](#file-adr-006-rule-entity-type-domain-md)
- [ADR-007-employee-lifecycle-workflows.md](#file-adr-007-employee-lifecycle-workflows-md)
- [ADR-008-strong-timeline-replace-pattern.md](#file-adr-008-strong-timeline-replace-pattern-md)
- [ADR-009-journey.md](#file-adr-009-journey-md)
- [ADR-010-employee-frontend-editing.md](#file-adr-010-employee-frontend-editing-md)
- [ADR-011-shared-lookup-decision-matrix-and-guidelines.md](#file-adr-011-shared-lookup-decision-matrix-and-guidelines-md)
- [ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md](#file-adr-012-racionalizaci-n-de-naming-y-alcance-sem-ntico-de-rule-entity-type-md)
- [ADR-013-Mantenimiento-de-rule_entity.md](#file-adr-013-mantenimiento-de-rule-entity-md)
- [ADR-014-employee-frontend-ui.md](#file-adr-014-employee-frontend-ui-md)
- [ADR-015-Binding-de-catalogos-por-recurso-y-campo.md](#file-adr-015-binding-de-catalogos-por-recurso-y-campo-md)
- [ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md](#file-adr-016-anatomia-visual-y-patrones-de-interacci-n-de-la-ficha-de-empleado-md)
- [ADR-017-Cost-center-design.md](#file-adr-017-cost-center-design-md)
- [ADR-018-hiring-an-employee.md](#file-adr-018-hiring-an-employee-md)
- [ADR-019-employee-delete-administrativo.md](#file-adr-019-employee-delete-administrativo-md)
- [ADR-020-work-center-replace-from-date.md](#file-adr-020-work-center-replace-from-date-md)
- [ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md](#file-adr-021-company-como-catalogo-enriquecido-y-anclado-a-rule-entity-md)
- [ADR-022-Global-message-and-feedback-policy.md](#file-adr-022-global-message-and-feedback-policy-md)
- [ADR-023-UI-interaction-contracts-per-vertical.md](#file-adr-023-ui-interaction-contracts-per-vertical-md)
- [ADR-024_autorizacion_jerarquica_B4RRHH.md](#file-adr-024-autorizacion-jerarquica-b4rrhh-md)
- [ADR-025-subject-roles.md.md](#file-adr-025-subject-roles-md-md)
- [ADR-026-payroll-status-workflow.md.md](#file-adr-026-payroll-status-workflow-md-md)
- [ADR-027-payroll-root-model.md.md](#file-adr-027-payroll-root-model-md-md)
- [ADR-029-payroll-calculate-contract-stub.md](#file-adr-029-payroll-calculate-contract-stub-md)
- [ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md](#file-adr-030-payroll-launch-calculation-run-claim-and-internal-calculator-orchestration-md)
- [ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md](#file-adr-031-modelo-f-sico-de-payroll-launch--calculation-run-claims-y-mensajes-md)
- [ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md](#file-adr-032-payroll-launch-workflow--s-ncrono--con-run-persistido-y-claims-por-unidad--estado-md)
- [ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md](#file-adr-033-payrollobject-como-ra-z-metamodelo-can-nica-del-motor-n-mina-md)
- [ADR-034-Modelo-semántico-de-PayrollConcept.md](#file-adr-034-modelo-sem-ntico-de-payrollconcept-md)
- [ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md](#file-adr-036-tipolog-as-can-nicas-de-c-lculo-de-payrollconcept-md)
- [ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md](#file-adr-037-sources-y-resoluci-n-de-operandos-en-payrollconcept-md)
- [ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md](#file-adr-038-estrategias-de-agregaci-n-y-relaciones-de-alimentaci-n-en-payrollconcept-md)
- [ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md](#file-adr-039-modelo-dependencias-y-grafo-de-c-lculo-de-payrollconcept-md)
- [ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md](#file-adr-040-macro-grafo-activaci-n-de-conceptos-y-plan-de-c-lculo-efectivo-md)
- [ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md](#file-adr-041-segmentaci-n-temporal--mbito-de-ejecuci-n-y-c-lculo-por-tramos-en-payrollconcept-md)
- [ADR-042-Separación-entre-payrol-y-payroll_engine.md](#file-adr-042-separaci-n-entre-payrol-y-payroll-engine-md)
- [ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md](#file-adr-043-agreement-profile-y-activaci-n-de-payroll-basada-en-contexto-md)
- [ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md](#file-adr-044-primer-c-lculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-m-nimo-md)
- [ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md](#file-adr-045-ejecucion-elegible-real-basada-en-concept-assignment-y-plan-de-calculo-md)
- [ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md](#file-adr-046-conceptos-t-cnicos-base-de-per-odo-y-presencia-en-n-mina-md)
- [ADR-047-lifecycle-workflow-participant-pattern.md](#file-adr-047-lifecycle-workflow-participant-pattern-md)
- [ADR-048-modelo-de-cotizacion-ss-e-irpf.md](#file-adr-048-modelo-de-cotizacion-ss-e-irpf-md)
- [ADR-049-arquitectura-de-informacion-del-frontend.md](#file-adr-049-arquitectura-de-informacion-del-frontend-md)
- [ADR-050-esqueleto-de-pagina.md](#file-adr-050-esqueleto-de-pagina-md)
- [ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md](#file-adr-051-el-contenedor-deriva-del-modo-de-mantenimiento-md)
- [ADR-28-payroll-calculation-launch-semantics.md](#file-adr-28-payroll-calculation-launch-semantics-md)

---


---

# FILE: ADR-001-vertical-architecture-and-api-identity.md
<a name="file-adr-001-vertical-architecture-and-api-identity-md"></a>

<!-- BEGIN FILE: ADR-001-vertical-architecture-and-api-identity.md -->

# ADR — Arquitectura por verticales y reglas de identidad API en B4RRHH

## Estado
Propuesta adoptada como guía de refactor y convención base del proyecto.

## Objetivo
Definir de forma inequívoca cómo debe organizarse el código en B4RRHH, cómo deben diseñarse las APIs y qué decisiones deben seguirse al crear o refactorizar verticales funcionales, para evitar desviaciones de implementación al trabajar con Copilot o al crecer el proyecto.

---

# 1. Contexto

B4RRHH está evolucionando desde una estructura inicialmente más centrada en capas globales (`application`, `domain`, `infrastructure`) hacia un modelo donde el negocio ya no es un único bloque homogéneo, sino un conjunto de verticales funcionales dentro de bounded contexts claros.

En la práctica, ya existen varios subdominios o verticales relevantes:

- `employee.employee`
- `employee.presence`
- `employee.contact`
- `rulesystem.rule_system`
- `rulesystem.rule_entity_type`
- `rulesystem.rule_entity`

A medida que el proyecto crezca, aparecerán más verticales y recursos relacionados con el empleado, por ejemplo:

- `employee.address`
- `employee.document`
- `employee.assignment`
- `employee.bank_account`
- `employee.compensation`
- etc.

La estructura actual mezcla dos criterios de organización:

1. organización por capas globales
2. organización por verticales con capas internas

Esa mezcla genera asimetrías, dificulta la navegación, favorece decisiones inconsistentes en API y aumenta la probabilidad de que Copilot implemente nuevos verticales siguiendo patrones incorrectos.

Este ADR fija el modelo objetivo.

---

# 2. Decisión arquitectónica principal

## 2.1. Regla principal

**En B4RRHH, el código se organiza primero por vertical/subdominio, y dentro de cada vertical se aplica arquitectura hexagonal.**

Eso significa que el eje principal del scaffolding es el negocio, no las capas globales.

## 2.2. Consecuencia práctica

No se debe seguir creciendo con una estructura donde, dentro de un mismo bounded context, convivan simultáneamente:

- paquetes raíz por capa (`application`, `domain`, `infrastructure`)
- y paquetes raíz por vertical (`presence`, `contact`, etc.)

Ese híbrido sólo se tolera como estado transitorio durante la migración.

## 2.3. Modelo objetivo

Cada bounded context se organiza en verticales. Cada vertical contiene sus propias capas hexagonales:

- `application`
- `domain`
- `infrastructure`

Opcionalmente puede tener subpaquetes como:

- `application.usecase`
- `application.port`
- `application.service`
- `domain.model`
- `domain.port`
- `domain.exception`
- `infrastructure.persistence`
- `infrastructure.web`
- `infrastructure.web.dto`

---

# 3. Estructura objetivo del proyecto

## 3.1. Estructura conceptual de alto nivel

```text
com.b4rrhh
  employee
    employee
    presence
    contact
    shared
  rulesystem
  shared
```

## 3.2. Estructura objetivo detallada para `employee`

```text
com.b4rrhh.employee
  employee
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  presence
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  contact
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  shared
    application
    domain
    infrastructure
```

## 3.3. Estado de `rulesystem`

`rulesystem` puede mantenerse temporalmente con su estructura actual si no compensa refactorizarlo ahora mismo.

No obstante, **todo vertical nuevo dentro de `employee` debe seguir ya el modelo vertical-first**, y los verticales existentes deben migrarse de forma incremental.

---

# 4. Regla de identidad en APIs

## 4.1. Regla obligatoria del proyecto

**Todas las APIs de B4RRHH deben trabajar con códigos funcionales de dominio. Nunca con IDs técnicos como identidad pública del recurso.**

Esta es una convención global del proyecto y aplica a todos los bounded contexts y verticales.

## 4.2. Qué significa “código funcional”

Son identificadores de negocio estables y significativos, por ejemplo:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `contactTypeCode`
- `ruleEntityTypeCode`
- `ruleEntityCode`

## 4.3. Qué no debe exponerse en la API

No deben utilizarse como identidad pública en paths ni en la semántica de la API:

- `id`
- `employeeId`
- `contactId`
- `presenceId`
- claves surrogate de base de datos
- UUIDs técnicos sin valor de negocio

Los IDs técnicos pueden existir y seguir existiendo para:

- persistencia
- joins
- rendimiento
- claves primarias internas
- simplificación de adapters y repositorios

Pero no deben dirigir la forma de la API pública.

## 4.4. Regla de consistencia

No se permite mezclar en una misma API:

- recurso padre identificado por business key
- recurso hijo identificado por id técnico

Tampoco al revés.

Si un recurso tiene identidad funcional clara, la API debe expresarla.

---

# 5. Regla de modelado de recursos

## 5.1. Los recursos se modelan por su identidad funcional real

Al diseñar un vertical, primero debe responderse a estas preguntas:

1. ¿cuál es la identidad funcional del recurso?
2. ¿qué campos forman parte de esa identidad?
3. ¿qué campos son mutables?
4. ¿qué campos son meramente persistentes o técnicos?
5. ¿el recurso es historizado o no?
6. ¿hay unicidad por tipo, período o combinación de códigos?

## 5.2. No confundir identidad con persistencia

Si un recurso tiene un `id` técnico en base de datos, eso no implica que su identidad de negocio sea ese `id`.

Ejemplo:

- `employee.contact` puede tener columna `id`
- pero su identidad funcional puede ser `employee + contactTypeCode`

## 5.3. Los endpoints deben expresar el dominio

Cuando una regla de negocio diga “sólo puede existir uno por tipo”, la API debe tender a expresarlo como tal, en lugar de simular una colección anónima de filas con `id`.

---

# 6. Convenciones específicas para el bounded context `employee`

## 6.1. Verticales actuales

Dentro de `employee`, por ahora se consideran verticales explícitos:

- `employee`
- `presence`
- `contact`

A futuro podrán añadirse otros verticales del mismo nivel.

## 6.2. Regla de naming

Se prioriza naming orientado a negocio y no a artefacto técnico.

Buenos ejemplos:

- `CreateContactUseCase`
- `UpdateContactService`
- `ContactRepository`
- `ContactBusinessKeyController`
- `PresenceCatalogValidator`

Evitar nombres que consoliden decisiones incorrectas de identidad, por ejemplo:

- `GetContactByIdUseCase`
- `DeletePresenceByIdUseCase`
- `EmployeeIdController`

salvo que el caso sea estrictamente interno y no forme parte de la API pública.

## 6.3. `shared` dentro de `employee`

El paquete `employee.shared` sólo debe contener elementos verdaderamente transversales al bounded context y sin pertenencia clara a un vertical concreto.

No debe convertirse en un cajón desastre.

Se debe evitar mover a `shared`:

- lógica de dominio específica de un vertical
- validaciones concretas de un recurso
- DTOs
- queries o repositorios de un subdominio concreto

---

# 7. Caso de referencia: `employee.contact`

Este vertical se usará como patrón canónico del refactor.

## 7.1. Naturaleza del recurso

`employee.contact` representa medios de contacto actuales de un empleado.

## 7.2. Reglas funcionales acordadas

- no historizado
- un solo contacto por tipo por empleado
- `contact_type_code` validado contra `rulesystem.rule_entity`
- `rule_entity_type_code = EMPLOYEE_CONTACT_TYPE`
- tipos de contacto definidos por `rule_system`
- `contact_value` obligatorio
- validación ligera del valor según tipo
- borrado físico

## 7.3. Identidad funcional del contacto

La identidad funcional del recurso es:

- empleado
- `contactTypeCode`

El contacto no se identifica funcionalmente por un `contactId` técnico.

## 7.4. Mutabilidad

- `contactTypeCode`: **inmutable** tras creación
- `contactValue`: **mutable**

## 7.5. Persistencia

Puede existir una tabla como:

- `employee.contact(id, employee_id, contact_type_code, contact_value, created_at, updated_at)`

con restricción:

- `unique(employee_id, contact_type_code)`

Eso es correcto siempre que se entienda que:

- `id` es técnico
- la identidad funcional del recurso no es ese `id`

## 7.6. API objetivo para `employee.contact`

La API debe trabajar exclusivamente con business keys del empleado y del tipo de contacto.

### Endpoints objetivo

```text
POST   /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
PUT    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
```

## 7.7. DTOs recomendados

### CreateContactRequest
Debe contener:

- `contactTypeCode`
- `contactValue`

### UpdateContactRequest
Debe contener sólo:

- `contactValue`

No debe permitir cambiar `contactTypeCode`.

### ContactResponse
Debe evitar exponer IDs técnicos como identidad principal del recurso. Si un campo técnico se mantiene temporalmente por motivos internos, debe tratarse como excepción transitoria, no como convención.

## 7.8. Estructura objetivo del paquete `contact`

```text
com.b4rrhh.employee.contact
  application
    port
      EmployeeContactLookupPort.java
      EmployeeContactContext.java
    service
      ContactCatalogValidator.java
    usecase
      ContactRuleEntityTypeCodes.java
      CreateContactCommand.java
      CreateContactService.java
      CreateContactUseCase.java
      DeleteContactCommand.java
      DeleteContactService.java
      DeleteContactUseCase.java
      GetContactByBusinessKeyService.java
      GetContactByBusinessKeyUseCase.java
      ListEmployeeContactsService.java
      ListEmployeeContactsUseCase.java
      UpdateContactCommand.java
      UpdateContactService.java
      UpdateContactUseCase.java

  domain
    model
      Contact.java
    port
      ContactRepository.java
    exception
      ContactAlreadyExistsException.java
      ContactCatalogValueInvalidException.java
      ContactEmployeeNotFoundException.java
      ContactNotFoundException.java
      ContactRuleSystemNotFoundException.java
      ContactValueInvalidException.java

  infrastructure
    persistence
      ContactEntity.java
      ContactPersistenceAdapter.java
      EmployeeContactLookupAdapter.java
      SpringDataContactRepository.java
    web
      ContactBusinessKeyController.java
      ContactExceptionHandler.java
      dto
        ContactErrorResponse.java
        ContactResponse.java
        CreateContactRequest.java
        UpdateContactRequest.java
```

---

# 8. Caso de referencia: `employee.presence`

`employee.presence` debe tender al mismo modelo arquitectónico que `employee.contact`, aunque sus reglas funcionales sean distintas.

## 8.1. Naturaleza

- vertical hermano de `employee.contact`
- no un subpaquete accidental dentro de una arquitectura por capas globales

## 8.2. Acción recomendada

Una vez estabilizado `contact` como patrón, `presence` debe revisarse para alinearse con la misma convención:

- vertical en primer nivel del bounded context `employee`
- hexagonal interna
- endpoints públicos basados en business keys

---

# 9. Validación contra catálogos (`rule_entity`)

## 9.1. Principio

Las validaciones de catálogo deben seguir el metamodelo existente del proyecto.

## 9.2. Regla

Cuando un campo de un vertical representa un código parametrizable, debe validarse contra `rulesystem.rule_entity` usando:

- `ruleSystemCode` correcto
- `ruleEntityTypeCode` correcto
- `code` correcto

## 9.3. Sobre activo y vigencia

Es aceptable reutilizar una validación genérica común que además compruebe:

- activo
- vigencia temporal

si eso forma parte de la infraestructura compartida del metamodelo.

Pero debe entenderse como:

- una política de validación técnica compartida
- no necesariamente como una característica específica del vertical en cuestión

No debe complicarse el modelo funcional del recurso sólo por heredar esa validación compartida.

---

# 10. Regla sobre seeds por `rule_system`

## 10.1. Decisión actual

Los catálogos como `EMPLOYEE_CONTACT_TYPE` pueden repetirse por `rule_system`, aunque hoy los valores coincidan entre sistemas.

## 10.2. Justificación

Se evita introducir por ahora una jerarquía más compleja de catálogos globales / por país / por familia.

## 10.3. Consecuencia

Es válido sembrar valores por cada `rule_system` existente en una migración inicial.

## 10.4. Deuda conocida

Debe definirse en el futuro cómo escalar esto cuando se creen nuevos `rule_system`:

- seed automático al alta
- proceso operativo
- estrategia de bootstrap
- otro mecanismo

Esta deuda no invalida el diseño actual, pero debe permanecer visible.

---

# 11. Reglas de diseño para Copilot

Estas reglas deben incluirse en prompts de implementación o refactor.

## 11.1. Reglas obligatorias

1. Organiza el código primero por vertical/subdominio.
2. Dentro de cada vertical, aplica arquitectura hexagonal.
3. No mezcles paquetes raíz por capa y por vertical dentro de un mismo bounded context.
4. Nunca expongas IDs técnicos en APIs públicas si existe una identidad funcional clara.
5. Usa siempre códigos funcionales en paths y contratos OpenAPI.
6. Mantén los IDs técnicos sólo en persistencia y wiring interno.
7. Cuando un recurso tenga unicidad funcional por combinación de códigos, exprésala en el diseño del endpoint.
8. No permitas mutar campos que formen parte de la identidad funcional.
9. Actualiza OpenAPI, casos de uso, adapters, tests y documentación de recurso en cada refactor.
10. No introduzcas historización si el recurso no la requiere.

## 11.2. Antipatrones a evitar

Copilot no debe:

- crear un nuevo paquete raíz suelto al lado de `application`, `domain`, `infrastructure` cuando el bounded context ya tiene verticales
- exponer endpoints por `{id}` cuando el dominio ya tiene business keys claras
- usar DTOs de update que permitan modificar campos identificativos
- tratar una tabla con surrogate key como si esa surrogate key fuera automáticamente la identidad del recurso
- diseñar recursos como listas de filas genéricas cuando el negocio habla de “uno por tipo” o “uno por combinación de códigos”

---

# 12. Estrategia de migración recomendada

## 12.1. No hacer big bang global

No se recomienda un megarrefactor de todo el proyecto en una sola iteración.

## 12.2. Orden recomendado

1. fijar este ADR como convención
2. refactorizar `employee.contact`
3. usar `employee.contact` como patrón canónico
4. alinear `employee.presence`
5. consolidar `employee.employee` si procede
6. aplicar la convención a nuevos verticales

## 12.3. Regla para nuevos desarrollos

Mientras existan áreas aún no migradas, cualquier vertical nuevo debe ya nacer con la estructura objetivo.

---

# 13. Checklist de revisión para cualquier vertical nuevo

Antes de aceptar una implementación, revisar:

## 13.1. Arquitectura

- ¿el vertical está organizado como vertical autónomo con capas internas?
- ¿se ha evitado mezclar vertical raíz con capas raíz del mismo bounded context?

## 13.2. API

- ¿los endpoints usan business keys?
- ¿hay algún `{id}` técnico expuesto sin necesidad?
- ¿la identidad del path expresa el dominio real?

## 13.3. Dominio

- ¿la identidad funcional está clara?
- ¿qué campos son inmutables?
- ¿qué campos son mutables?
- ¿la unicidad real del negocio está modelada?

## 13.4. Persistencia

- ¿el id técnico queda encapsulado?
- ¿hay unique constraints alineadas con la identidad funcional?

## 13.5. OpenAPI

- ¿los schemas reflejan las reglas de mutabilidad?
- ¿los DTOs de update evitan modificar campos identitarios?

## 13.6. Tests

- ¿hay tests de caso feliz?
- ¿hay tests de duplicado/unicidad?
- ¿hay tests de ownership o pertenencia al recurso padre?
- ¿hay tests de validación de catálogo?
- ¿hay tests de integración con constraints reales de BD?

---

# 14. Prompt base para Copilot — creación/refactor de verticales en B4RRHH

```text
You are working in the B4RRHH project.

Mandatory project conventions:
- Organize code first by business vertical/subdomain, not by global architectural layer.
- Inside each vertical, use hexagonal architecture.
- Public APIs must always use functional business codes, never technical database IDs.
- Technical IDs may exist only for persistence and internal wiring.
- If a resource has a clear functional identity, the REST API must express it explicitly.
- Fields that are part of the functional identity are immutable after creation unless explicitly stated otherwise.
- Always keep OpenAPI, use cases, adapters, tests and resource documentation aligned.

Target package organization pattern:
- com.b4rrhh.<bounded-context>.<vertical>.application...
- com.b4rrhh.<bounded-context>.<vertical>.domain...
- com.b4rrhh.<bounded-context>.<vertical>.infrastructure...

Avoid these mistakes:
- Do not create a new root package for a vertical alongside application/domain/infrastructure inside the same bounded context.
- Do not expose endpoints by technical id when business keys exist.
- Do not allow update DTOs to modify identity fields.
- Do not confuse surrogate database keys with domain identity.

When implementing or refactoring a vertical:
1. Identify the functional business key.
2. Design the REST paths using those business keys.
3. Keep technical ids only in persistence.
4. Define immutable vs mutable fields explicitly.
5. Add database constraints aligned with domain uniqueness.
6. Add tests for duplicates, ownership, catalog validation and persistence constraints.
```

---

# 15. Prompt específico para refactorizar `employee.contact`

```text
Refactor the employee.contact vertical in B4RRHH to comply with the project architecture and API identity rules.

Project rules:
- Code must be organized first by vertical, then by hexagonal layers.
- Public APIs must use functional business codes only, never technical IDs.

Current target architecture:
- com.b4rrhh.employee.contact.application...
- com.b4rrhh.employee.contact.domain...
- com.b4rrhh.employee.contact.infrastructure...

Do not leave contact classes under a mixed structure that combines root layer packages and root vertical packages inside the employee bounded context.

Employee functional identity:
- ruleSystemCode
- employeeTypeCode
- employeeNumber

Contact functional identity within an employee:
- contactTypeCode

Domain rules:
- One contact per contact type per employee.
- contact_type_code is immutable after creation.
- contact_value is mutable.
- Keep technical ids only for persistence.
- Keep unique(employee_id, contact_type_code) in the database.
- Keep contact catalog validation using EMPLOYEE_CONTACT_TYPE.
- Keep the resource non-historized.

Required REST API:
- POST   /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
- GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
- GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
- PUT    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
- DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}

OpenAPI rules:
- CreateContactRequest must contain contactTypeCode and contactValue.
- UpdateContactRequest must contain only contactValue.
- Do not use employeeId or contactId in public API paths.
- Review response DTOs to avoid exposing technical IDs unless strictly internal.

Implementation tasks:
- Move packages to the new structure.
- Rename use cases/services that still express technical-id semantics.
- Replace GetContactById with business-key based retrieval.
- Ensure ownership is enforced through employee business key + contactTypeCode.
- Update controllers, adapters, repository contracts, tests and documentation.
- Keep existing valid persistence model where possible.
```

---

# 16. Consecuencia final

A partir de este ADR:

- el patrón objetivo en `employee` es vertical-first
- las APIs del proyecto se diseñan siempre con business keys
- `employee.contact` se toma como primer vertical a refactorizar con esta convención
- cualquier nuevo vertical debe seguir ya estas reglas desde su nacimiento

Este documento debe usarse como referencia base para diseño humano, revisión técnica y prompts a Copilot.


<!-- END FILE: ADR-001-vertical-architecture-and-api-identity.md -->


---

# FILE: ADR-002-employee-contact-vertical.md
<a name="file-adr-002-employee-contact-vertical-md"></a>

<!-- BEGIN FILE: ADR-002-employee-contact-vertical.md -->

# ADR-002 — Employee Contact Vertical

## Status
Accepted

## Context

The B4RRHH project models employee-related information as a set of vertical resources
inside the `employee` bounded context.

Each resource represents a distinct functional aspect of the employee domain
and follows the architectural rules defined in:

ADR-001 — Vertical architecture and API identity rules.

The `employee.contact` vertical represents the contact channels currently associated
with an employee.

Typical examples include:

- email
- phone
- mobile
- company mobile
- internal extension

These contact types are configurable through the metamodel
(`rulesystem.rule_entity`) using the entity type:

EMPLOYEE_CONTACT_TYPE

---

# Functional Definition

`employee.contact` represents **current contact channels of an employee**.

This resource is **not historized**.

It behaves as a **set of slots per contact type**.

Each employee may have **at most one contact per contact type**.

Example:

| employee | type | value |
|--------|------|------|
| EMP 0001 | EMAIL | john@corp.com |
| EMP 0001 | MOBILE | 600123123 |
| EMP 0001 | EXTENSION | 1234 |

Invalid:

| employee | type | value |
|--------|------|------|
| EMP 0001 | EMAIL | john@corp.com |
| EMP 0001 | EMAIL | john.personal@gmail.com |

---

# Structural Properties

| Property | Value |
|--------|------|
| historized | false |
| occurrence_type | MULTIPLE |
| simultaneous_occurrences | MULTIPLE |
| lifecycle_strategy | DELETE |
| delete_policy | PHYSICAL |

---

# Functional Identity

The functional identity of a contact is:

employee + contactTypeCode

Where employee identity is:

ruleSystemCode + employeeTypeCode + employeeNumber

Therefore the full functional identity is conceptually:

ruleSystemCode + employeeTypeCode + employeeNumber + contactTypeCode

The contact **is not identified by a technical ID**.

---

# Mutability Rules

| Field | Mutable |
|-----|------|
| contactTypeCode | ❌ No |
| contactValue | ✔ Yes |

Changing the contact type is not allowed.

If a different type is needed:

1. delete existing contact
2. create new contact

---

# Persistence Model

Typical persistence structure:

employee.contact

Columns:

| column | description |
|------|-------------|
| id | technical surrogate key |
| employee_id | FK to employee.employee |
| contact_type_code | contact type |
| contact_value | contact data |
| created_at | timestamp |
| updated_at | timestamp |

Database constraint:

unique(employee_id, contact_type_code)

The `id` column is **technical only**.

It must not define the public identity of the resource.

---

# Catalog Validation

`contact_type_code` must be validated against:

rulesystem.rule_entity

Using:

rule_entity_type_code = EMPLOYEE_CONTACT_TYPE

Validation must ensure:

- rule system matches employee rule system
- entity exists
- entity is active
- entity is within validity period

---

# REST API Identity

APIs must use **business keys only**.

Employee identity:

ruleSystemCode  
employeeTypeCode  
employeeNumber

Contact identity:

contactTypeCode

---

# REST Endpoints

POST   /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts  
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts  
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}  
PUT    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}  
DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}

Technical identifiers must not appear in the API.

---

# DTO Design

### CreateContactRequest

contactTypeCode  
contactValue

### UpdateContactRequest

contactValue

`contactTypeCode` must not be mutable.

---

# Error Conditions

Typical errors include:

- employee not found
- contact type not found
- contact type not valid for rule system
- contact already exists for employee
- contact not found
- invalid contact value

---

# Validation Rules

Examples:

### EMAIL

- must contain '@'
- reasonable length
- trimmed

### PHONE / MOBILE

- digits and allowed characters
- normalized format
- trimmed

### EXTENSION

- numeric
- short length

Validation should remain **lightweight** and not attempt to fully validate
international phone formats.

---

# Relationship With Other Verticals

`employee.contact` is independent from:

- `employee.presence`
- `employee.contract`
- `employee.address`

It represents **current communication channels**, not employment history.

---

# Migration Note

Initial implementations exposed technical IDs in API paths.

This ADR establishes the transition to business-key based APIs.

Existing endpoints may temporarily coexist during migration.

---

# Role in the Architecture

This vertical serves as the **reference implementation** for:

- vertical-first architecture
- hexagonal layering inside verticals
- API identity based on domain keys
- metamodel catalog validation

Future verticals in the `employee` bounded context should follow this pattern.
<!-- END FILE: ADR-002-employee-contact-vertical.md -->


---

# FILE: ADR-003-rule-entity-metamodel-strategy.md
<a name="file-adr-003-rule-entity-metamodel-strategy-md"></a>

<!-- BEGIN FILE: ADR-003-rule-entity-metamodel-strategy.md -->

# ADR-003 — Rule Entity Metamodel Strategy

## Status
Accepted

## Context

B4RRHH uses a configurable catalog metamodel to avoid hardcoding many domain values
inside each vertical.

The current metamodel is based on these core resources:

- `rulesystem.rule_system`
- `rulesystem.rule_entity_type`
- `rulesystem.rule_entity`

This metamodel is already used by verticals such as `employee.presence`
and `employee.contact`, where domain codes are validated against catalog values
defined per rule system.

The project has also adopted two major architectural conventions:

1. code is organized by vertical first, with hexagonal layers inside each vertical
2. public APIs must use business keys and functional codes, never technical IDs

Those conventions are defined in ADR-001. `employee.contact` is defined as the
reference vertical in ADR-002. fileciteturn8file0 fileciteturn8file1

As the project grows, the metamodel becomes a foundational mechanism, so its
strategy must be made explicit to avoid ambiguity and accidental redesigns.

---

# 1. Decision

B4RRHH will keep a **catalog metamodel based on rule systems**, where catalog
values are represented through `rule_entity` records grouped by
`rule_entity_type` and scoped by `rule_system`.

The metamodel is not an incidental implementation detail. It is a **core domain
mechanism** used to make functional codes configurable by rule system.

---

# 2. Core Resources

## 2.1. `rulesystem.rule_system`

Represents a functional system of rules, usually associated with a country,
regulatory context or equivalent domain partition.

Examples:

- `ESP`
- `PRT`
- other future systems

## 2.2. `rulesystem.rule_entity_type`

Represents the category of configurable values.

Examples:

- `EMPLOYEE_CONTACT_TYPE`
- `EMPLOYEE_ENTRY_REASON`
- `EMPLOYEE_EXIT_REASON`
- future domain types

## 2.3. `rulesystem.rule_entity`

Represents an actual catalog value inside a rule system and type.

Examples:

- `EMPLOYEE_CONTACT_TYPE` + `ESP` + `EMAIL`
- `EMPLOYEE_CONTACT_TYPE` + `ESP` + `MOBILE`
- `EMPLOYEE_CONTACT_TYPE` + `PRT` + `EMAIL`

---

# 3. Main Modeling Rule

Catalog values are defined **per rule system**, even if the same code appears in
multiple rule systems.

This means that values like `EMAIL`, `PHONE`, `MOBILE`, etc. may be repeated for
different rule systems.

This duplication is intentional.

It reflects that:

- the business scope is the rule system
- semantics may diverge in the future
- validation must remain explicit and local to the rule system
- the project currently avoids introducing a higher-level hierarchy of
  universal/global/regional catalog families

---

# 4. What Is Explicitly Rejected

For now, B4RRHH does **not** introduce an additional metamodel layer such as:

- global entities
- common/shared entities across all rule systems
- country families
- inheritance between rule systems
- fallback from one rule system to another
- multi-level catalog resolution

Those ideas may appear later if truly needed, but they are explicitly out of
scope now.

The current strategy prefers **duplication with clarity** over abstraction with
ambiguity.

---

# 5. Validation Rule

Whenever a domain field represents a configurable code, it must be validated
against `rulesystem.rule_entity` using the full functional context:

- `ruleSystemCode`
- `ruleEntityTypeCode`
- `code`

Validation may additionally check:

- active flag
- validity period

That additional validation is acceptable as shared infrastructure policy, but it
must not distort the domain model of each vertical. This is already consistent
with the project guidance on catalog validation reuse. fileciteturn8file0

---

# 6. API Identity Rule for Metamodel Resources

The public API for metamodel resources must also follow the project-wide rule:
use functional business codes, never technical IDs. ADR-001 makes this mandatory
for the whole project. fileciteturn8file0

Therefore:

- `rule_system` is identified by `ruleSystemCode`
- `rule_entity_type` is identified by `ruleEntityTypeCode`
- `rule_entity` is identified functionally by:
  - `ruleSystemCode`
  - `ruleEntityTypeCode`
  - `code`

Technical database IDs may exist internally, but they must not drive API paths.

---

# 7. Search and Retrieval Semantics

For `rule_entity`, the preferred public API semantics are progressive filtering
by business codes, not technical-ID lookup.

Valid query styles include:

- list all entities for a rule system
- list all entities for a rule system and entity type
- get a specific entity by rule system + entity type + code

Typical examples:

- `GET /rule-entities?ruleSystemCode=ESP`
- `GET /rule-entities?ruleSystemCode=ESP&ruleEntityTypeCode=EMPLOYEE_CONTACT_TYPE`
- `GET /rule-entities?ruleSystemCode=ESP&ruleEntityTypeCode=EMPLOYEE_CONTACT_TYPE&code=EMAIL`

This is consistent with the already agreed rulesystem API direction in the
project context.

---

# 8. Seed Strategy

## 8.1. Current Accepted Strategy

When a new entity type such as `EMPLOYEE_CONTACT_TYPE` is introduced, initial
migration scripts may seed values for every existing `rule_system`.

This is valid and accepted.

## 8.2. Known Limitation

This approach only guarantees bootstrap for rule systems that already exist at
migration time.

It does not automatically solve what happens when a new `rule_system` is created
later.

## 8.3. Known Architectural Debt

A follow-up mechanism must eventually be chosen for new rule systems, for example:

- application service that bootstraps default entities when a rule system is created
- explicit operational script
- administrative endpoint
- other controlled bootstrap process

This debt must remain visible, but it does not invalidate the current model.

---

# 9. Ownership of Catalog Semantics

The metamodel is owned by the `rulesystem` bounded context.

Other bounded contexts such as `employee` consume the metamodel through business
codes and validation ports.

This means:

- `employee` does not redefine the semantics of catalog storage
- `rulesystem` remains the canonical owner of catalog configuration
- verticals such as `employee.contact` only declare which entity type they depend on

This is aligned with the updated employee resource catalog, where `employee.contact`
declares `contact_type_code` as catalog-backed and immutable. fileciteturn8file2

---

# 10. Naming Conventions

The following naming conventions are preferred:

- `ruleSystemCode`
- `ruleEntityTypeCode`
- `code`

Avoid introducing parallel alternative names for the same functional meaning
unless there is a very strong domain reason.

Within verticals, constants such as:

- `EMPLOYEE_CONTACT_TYPE`

should be defined once and reused.

---

# 11. Design Rules for Copilot

When implementing features that interact with the metamodel, Copilot must follow
these rules:

1. Treat `rule_system`, `rule_entity_type` and `rule_entity` as business resources.
2. Use business codes in APIs and use cases.
3. Do not expose technical IDs in public contracts.
4. Validate catalog-backed domain fields with:
   - rule system
   - entity type
   - code
5. Do not invent extra abstraction layers such as global entities or catalog inheritance.
6. Accept duplicated values across rule systems as valid and intentional.
7. Keep seed logic explicit and visible.
8. Do not move catalog semantics into random `shared` utility packages.
9. Keep the metamodel inside the `rulesystem` bounded context.
10. When a vertical uses a catalog code, document the associated `ruleEntityTypeCode`.

---

# 12. Consequences

From this ADR onwards:

- the rule entity metamodel is a first-class strategic mechanism of the project
- duplication of catalog values across rule systems is intentional
- APIs for metamodel resources must use functional business codes
- verticals must validate configurable codes through the metamodel
- future work may improve bootstrap for new rule systems, but without introducing
  hidden hierarchy levels prematurely

This ADR must be used together with:

- ADR-001 — vertical architecture and API identity rules
- ADR-002 — employee.contact vertical

<!-- END FILE: ADR-003-rule-entity-metamodel-strategy.md -->


---

# FILE: ADR-004-employee-business-key-strategy.md
<a name="file-adr-004-employee-business-key-strategy-md"></a>

<!-- BEGIN FILE: ADR-004-employee-business-key-strategy.md -->

# ADR-004 — Employee Business Key Strategy

## Status
Accepted

## Context

B4RRHH models the employee domain using functional business identity instead of
technical persistence identity as the primary public reference.

The project has already adopted these rules:

- APIs must use business keys, never technical IDs
- code is organized by vertical first
- employee-related resources live inside the `employee` bounded context
- child resources must inherit employee identity through the employee business key

The employee identity has evolved from:

    ruleSystemCode + employeeNumber

to:

    ruleSystemCode + employeeTypeCode + employeeNumber

This ADR formalizes that decision and its consequences.

---

# 1. Decision

The canonical functional identity of an employee in B4RRHH is:

    ruleSystemCode + employeeTypeCode + employeeNumber

This is the official employee business key for:

- public APIs
- domain logic
- lookups across verticals
- future integrations
- functional references between bounded contexts

Technical database IDs may still exist internally, but they are not part of the
public identity model.

---

# 2. Rationale

## 2.1. Avoid ambiguity

The same employee number may need to exist for different employee types inside
the same rule system.

Examples:

- ESP + EMP + 0001
- ESP + EXT + 0001
- ESP + JUB + 0001

If `employeeTypeCode` is omitted, these become ambiguous.

## 2.2. Preserve business meaning

The identity must reflect how the organization distinguishes employee populations.

`employeeTypeCode` is not decorative metadata. It is part of the business identity.

## 2.3. Support future scalability

This identity model scales better to:

- internal employees
- external collaborators
- retirees
- temporary populations
- country-specific employee classes

---

# 3. Scope

This strategy applies to all resources that reference an employee functionally.

That includes at least:

- `employee.employee`
- `employee.presence`
- `employee.contact`
- future employee verticals such as:
  - address
  - contract
  - assignment
  - compensation
  - document
  - absence

Whenever an API needs to identify an employee, it must use the 3-part business key.

---

# 4. API Rule

Public APIs must identify an employee using:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`

Examples:

    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}
    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/presences

The following are not valid as canonical public identity:

- `employeeId`
- technical UUIDs
- 2-part employee key without employeeTypeCode

---

# 5. Persistence Rule

The persistence layer may still use a technical surrogate key such as:

- `employee.id`

This is allowed for:

- foreign keys
- joins
- performance
- indexing
- adapter internals

However, the database must also enforce the functional uniqueness of the employee
through a unique constraint based on:

    (rule_system_code, employee_type_code, employee_number)

---

# 6. Child Resource Rule

Every employee child resource must conceptually inherit employee identity through
the employee business key.

Examples:

## 6.1. Contact

Functional identity:

    employee + contactTypeCode

Expanded:

    ruleSystemCode + employeeTypeCode + employeeNumber + contactTypeCode

## 6.2. Presence

Functional identity:

    employee + presenceNumber

Expanded:

    ruleSystemCode + employeeTypeCode + employeeNumber + presenceNumber

## 6.3. Future verticals

    employee + addressNumber
    employee + contractNumber
    employee + assignmentNumber
    employee + absenceNumber

This prevents mixed semantics such as:

- parent by business key
- child by technical ID

That pattern is explicitly rejected.

---

# 7. Integration Rule

Whenever another bounded context or external integration references an employee,
the preferred functional reference must be the 3-part business key.

If internal systems need technical IDs, those may exist as local persistence
concerns, but they must not replace the canonical business identity model.

---

# 8. Migration Guidance

When migrating legacy resources:

1. add `employeeTypeCode` to domain model
2. update unique constraints
3. update repository business-key lookups
4. update controllers and OpenAPI paths
5. update child vertical lookup adapters
6. update tests
7. remove or deprecate 2-part key endpoints

This is the expected migration path for verticals such as `presence`.

---

# 9. Design Rules for Copilot

When Copilot implements or refactors employee-related code, it must follow these rules:

1. Treat `ruleSystemCode + employeeTypeCode + employeeNumber` as the employee identity.
2. Do not design public APIs around `employeeId`.
3. Do not implement new employee child resources using only the old 2-part key.
4. Ensure child resources derive their identity from the employee business key.
5. Keep technical IDs inside persistence/adapters only.
6. Update OpenAPI, tests and migrations consistently when the employee key is involved.

---

# 10. Consequences

From this ADR onwards:

- the employee 3-part key is the canonical identity model
- any remaining 2-part employee-key APIs are transitional debt
- any employee API using technical IDs is non-canonical
- all new employee verticals must be designed around the 3-part key

This ADR must be used together with:

- ADR-001 — vertical architecture and API identity rules
- ADR-002 — employee.contact vertical
- ADR-003 — rule entity metamodel strategy

<!-- END FILE: ADR-004-employee-business-key-strategy.md -->


---

# FILE: ADR-005-arquitectura_por_verticales_y_reglas_api.md
<a name="file-adr-005-arquitectura-por-verticales-y-reglas-api-md"></a>

<!-- BEGIN FILE: ADR-005-arquitectura_por_verticales_y_reglas_api.md -->

# ADR — Arquitectura por verticales y reglas de identidad API en B4RRHH

## Estado
Propuesta adoptada como guía de refactor y convención base del proyecto.

## Objetivo
Definir de forma inequívoca cómo debe organizarse el código en B4RRHH, cómo deben diseñarse las APIs y qué decisiones deben seguirse al crear o refactorizar verticales funcionales, para evitar desviaciones de implementación al trabajar con Copilot o al crecer el proyecto.

---

# 1. Contexto

B4RRHH está evolucionando desde una estructura inicialmente más centrada en capas globales (`application`, `domain`, `infrastructure`) hacia un modelo donde el negocio ya no es un único bloque homogéneo, sino un conjunto de verticales funcionales dentro de bounded contexts claros.

En la práctica, ya existen varios subdominios o verticales relevantes:

- `employee.employee`
- `employee.presence`
- `employee.contact`
- `rulesystem.rule_system`
- `rulesystem.rule_entity_type`
- `rulesystem.rule_entity`

A medida que el proyecto crezca, aparecerán más verticales y recursos relacionados con el empleado, por ejemplo:

- `employee.address`
- `employee.document`
- `employee.assignment`
- `employee.bank_account`
- `employee.compensation`
- etc.

La estructura actual mezcla dos criterios de organización:

1. organización por capas globales
2. organización por verticales con capas internas

Esa mezcla genera asimetrías, dificulta la navegación, favorece decisiones inconsistentes en API y aumenta la probabilidad de que Copilot implemente nuevos verticales siguiendo patrones incorrectos.

Este ADR fija el modelo objetivo.

---

# 2. Decisión arquitectónica principal

## 2.1. Regla principal

**En B4RRHH, el código se organiza primero por vertical/subdominio, y dentro de cada vertical se aplica arquitectura hexagonal.**

Eso significa que el eje principal del scaffolding es el negocio, no las capas globales.

## 2.2. Consecuencia práctica

No se debe seguir creciendo con una estructura donde, dentro de un mismo bounded context, convivan simultáneamente:

- paquetes raíz por capa (`application`, `domain`, `infrastructure`)
- y paquetes raíz por vertical (`presence`, `contact`, etc.)

Ese híbrido sólo se tolera como estado transitorio durante la migración.

## 2.3. Modelo objetivo

Cada bounded context se organiza en verticales. Cada vertical contiene sus propias capas hexagonales:

- `application`
- `domain`
- `infrastructure`

Opcionalmente puede tener subpaquetes como:

- `application.usecase`
- `application.port`
- `application.service`
- `domain.model`
- `domain.port`
- `domain.exception`
- `infrastructure.persistence`
- `infrastructure.web`
- `infrastructure.web.dto`

---

# 3. Estructura objetivo del proyecto

## 3.1. Estructura conceptual de alto nivel

```text
com.b4rrhh
  employee
    employee
    presence
    contact
    shared
  rulesystem
  shared
```

## 3.2. Estructura objetivo detallada para `employee`

```text
com.b4rrhh.employee
  employee
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  presence
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  contact
    application
      port
      service
      usecase
    domain
      model
      port
      exception
    infrastructure
      persistence
      web
        dto

  shared
    application
    domain
    infrastructure
```

## 3.3. Estado de `rulesystem`

`rulesystem` puede mantenerse temporalmente con su estructura actual si no compensa refactorizarlo ahora mismo.

No obstante, **todo vertical nuevo dentro de `employee` debe seguir ya el modelo vertical-first**, y los verticales existentes deben migrarse de forma incremental.

---

# 4. Regla de identidad en APIs

## 4.1. Regla obligatoria del proyecto

**Todas las APIs de B4RRHH deben trabajar con códigos funcionales de dominio. Nunca con IDs técnicos como identidad pública del recurso.**

Esta es una convención global del proyecto y aplica a todos los bounded contexts y verticales.

## 4.2. Qué significa “código funcional”

Son identificadores de negocio estables y significativos, por ejemplo:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `contactTypeCode`
- `ruleEntityTypeCode`
- `ruleEntityCode`

## 4.3. Qué no debe exponerse en la API

No deben utilizarse como identidad pública en paths ni en la semántica de la API:

- `id`
- `employeeId`
- `contactId`
- `presenceId`
- claves surrogate de base de datos
- UUIDs técnicos sin valor de negocio

Los IDs técnicos pueden existir y seguir existiendo para:

- persistencia
- joins
- rendimiento
- claves primarias internas
- simplificación de adapters y repositorios

Pero no deben dirigir la forma de la API pública.

## 4.4. Regla de consistencia

No se permite mezclar en una misma API:

- recurso padre identificado por business key
- recurso hijo identificado por id técnico

Tampoco al revés.

Si un recurso tiene identidad funcional clara, la API debe expresarla.

---

# 5. Regla de modelado de recursos

## 5.1. Los recursos se modelan por su identidad funcional real

Al diseñar un vertical, primero debe responderse a estas preguntas:

1. ¿cuál es la identidad funcional del recurso?
2. ¿qué campos forman parte de esa identidad?
3. ¿qué campos son mutables?
4. ¿qué campos son meramente persistentes o técnicos?
5. ¿el recurso es historizado o no?
6. ¿hay unicidad por tipo, período o combinación de códigos?

## 5.2. No confundir identidad con persistencia

Si un recurso tiene un `id` técnico en base de datos, eso no implica que su identidad de negocio sea ese `id`.

Ejemplo:

- `employee.contact` puede tener columna `id`
- pero su identidad funcional puede ser `employee + contactTypeCode`

## 5.3. Los endpoints deben expresar el dominio

Cuando una regla de negocio diga “sólo puede existir uno por tipo”, la API debe tender a expresarlo como tal, en lugar de simular una colección anónima de filas con `id`.

---

# 6. Convenciones específicas para el bounded context `employee`

## 6.1. Verticales actuales

Dentro de `employee`, por ahora se consideran verticales explícitos:

- `employee`
- `presence`
- `contact`

A futuro podrán añadirse otros verticales del mismo nivel.

## 6.2. Regla de naming

Se prioriza naming orientado a negocio y no a artefacto técnico.

Buenos ejemplos:

- `CreateContactUseCase`
- `UpdateContactService`
- `ContactRepository`
- `ContactBusinessKeyController`
- `PresenceCatalogValidator`

Evitar nombres que consoliden decisiones incorrectas de identidad, por ejemplo:

- `GetContactByIdUseCase`
- `DeletePresenceByIdUseCase`
- `EmployeeIdController`

salvo que el caso sea estrictamente interno y no forme parte de la API pública.

## 6.3. `shared` dentro de `employee`

El paquete `employee.shared` sólo debe contener elementos verdaderamente transversales al bounded context y sin pertenencia clara a un vertical concreto.

No debe convertirse en un cajón desastre.

Se debe evitar mover a `shared`:

- lógica de dominio específica de un vertical
- validaciones concretas de un recurso
- DTOs
- queries o repositorios de un subdominio concreto

---

# 7. Caso de referencia: `employee.contact`

Este vertical se usará como patrón canónico del refactor.

## 7.1. Naturaleza del recurso

`employee.contact` representa medios de contacto actuales de un empleado.

## 7.2. Reglas funcionales acordadas

- no historizado
- un solo contacto por tipo por empleado
- `contact_type_code` validado contra `rulesystem.rule_entity`
- `rule_entity_type_code = EMPLOYEE_CONTACT_TYPE`
- tipos de contacto definidos por `rule_system`
- `contact_value` obligatorio
- validación ligera del valor según tipo
- borrado físico

## 7.3. Identidad funcional del contacto

La identidad funcional del recurso es:

- empleado
- `contactTypeCode`

El contacto no se identifica funcionalmente por un `contactId` técnico.

## 7.4. Mutabilidad

- `contactTypeCode`: **inmutable** tras creación
- `contactValue`: **mutable**

## 7.5. Persistencia

Puede existir una tabla como:

- `employee.contact(id, employee_id, contact_type_code, contact_value, created_at, updated_at)`

con restricción:

- `unique(employee_id, contact_type_code)`

Eso es correcto siempre que se entienda que:

- `id` es técnico
- la identidad funcional del recurso no es ese `id`

## 7.6. API objetivo para `employee.contact`

La API debe trabajar exclusivamente con business keys del empleado y del tipo de contacto.

### Endpoints objetivo

```text
POST   /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
PUT    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
```

## 7.7. DTOs recomendados

### CreateContactRequest
Debe contener:

- `contactTypeCode`
- `contactValue`

### UpdateContactRequest
Debe contener sólo:

- `contactValue`

No debe permitir cambiar `contactTypeCode`.

### ContactResponse
Debe evitar exponer IDs técnicos como identidad principal del recurso. Si un campo técnico se mantiene temporalmente por motivos internos, debe tratarse como excepción transitoria, no como convención.

## 7.8. Estructura objetivo del paquete `contact`

```text
com.b4rrhh.employee.contact
  application
    port
      EmployeeContactLookupPort.java
      EmployeeContactContext.java
    service
      ContactCatalogValidator.java
    usecase
      ContactRuleEntityTypeCodes.java
      CreateContactCommand.java
      CreateContactService.java
      CreateContactUseCase.java
      DeleteContactCommand.java
      DeleteContactService.java
      DeleteContactUseCase.java
      GetContactByBusinessKeyService.java
      GetContactByBusinessKeyUseCase.java
      ListEmployeeContactsService.java
      ListEmployeeContactsUseCase.java
      UpdateContactCommand.java
      UpdateContactService.java
      UpdateContactUseCase.java

  domain
    model
      Contact.java
    port
      ContactRepository.java
    exception
      ContactAlreadyExistsException.java
      ContactCatalogValueInvalidException.java
      ContactEmployeeNotFoundException.java
      ContactNotFoundException.java
      ContactRuleSystemNotFoundException.java
      ContactValueInvalidException.java

  infrastructure
    persistence
      ContactEntity.java
      ContactPersistenceAdapter.java
      EmployeeContactLookupAdapter.java
      SpringDataContactRepository.java
    web
      ContactBusinessKeyController.java
      ContactExceptionHandler.java
      dto
        ContactErrorResponse.java
        ContactResponse.java
        CreateContactRequest.java
        UpdateContactRequest.java
```

---

# 8. Caso de referencia: `employee.presence`

`employee.presence` debe tender al mismo modelo arquitectónico que `employee.contact`, aunque sus reglas funcionales sean distintas.

## 8.1. Naturaleza

- vertical hermano de `employee.contact`
- no un subpaquete accidental dentro de una arquitectura por capas globales

## 8.2. Acción recomendada

Una vez estabilizado `contact` como patrón, `presence` debe revisarse para alinearse con la misma convención:

- vertical en primer nivel del bounded context `employee`
- hexagonal interna
- endpoints públicos basados en business keys

---

# 9. Validación contra catálogos (`rule_entity`)

## 9.1. Principio

Las validaciones de catálogo deben seguir el metamodelo existente del proyecto.

## 9.2. Regla

Cuando un campo de un vertical representa un código parametrizable, debe validarse contra `rulesystem.rule_entity` usando:

- `ruleSystemCode` correcto
- `ruleEntityTypeCode` correcto
- `code` correcto

## 9.3. Sobre activo y vigencia

Es aceptable reutilizar una validación genérica común que además compruebe:

- activo
- vigencia temporal

si eso forma parte de la infraestructura compartida del metamodelo.

Pero debe entenderse como:

- una política de validación técnica compartida
- no necesariamente como una característica específica del vertical en cuestión

No debe complicarse el modelo funcional del recurso sólo por heredar esa validación compartida.

---

# 10. Regla sobre seeds por `rule_system`

## 10.1. Decisión actual

Los catálogos como `EMPLOYEE_CONTACT_TYPE` pueden repetirse por `rule_system`, aunque hoy los valores coincidan entre sistemas.

## 10.2. Justificación

Se evita introducir por ahora una jerarquía más compleja de catálogos globales / por país / por familia.

## 10.3. Consecuencia

Es válido sembrar valores por cada `rule_system` existente en una migración inicial.

## 10.4. Deuda conocida

Debe definirse en el futuro cómo escalar esto cuando se creen nuevos `rule_system`:

- seed automático al alta
- proceso operativo
- estrategia de bootstrap
- otro mecanismo

Esta deuda no invalida el diseño actual, pero debe permanecer visible.

---

# 11. Reglas de diseño para Copilot

Estas reglas deben incluirse en prompts de implementación o refactor.

## 11.1. Reglas obligatorias

1. Organiza el código primero por vertical/subdominio.
2. Dentro de cada vertical, aplica arquitectura hexagonal.
3. No mezcles paquetes raíz por capa y por vertical dentro de un mismo bounded context.
4. Nunca expongas IDs técnicos en APIs públicas si existe una identidad funcional clara.
5. Usa siempre códigos funcionales en paths y contratos OpenAPI.
6. Mantén los IDs técnicos sólo en persistencia y wiring interno.
7. Cuando un recurso tenga unicidad funcional por combinación de códigos, exprésala en el diseño del endpoint.
8. No permitas mutar campos que formen parte de la identidad funcional.
9. Actualiza OpenAPI, casos de uso, adapters, tests y documentación de recurso en cada refactor.
10. No introduzcas historización si el recurso no la requiere.

## 11.2. Antipatrones a evitar

Copilot no debe:

- crear un nuevo paquete raíz suelto al lado de `application`, `domain`, `infrastructure` cuando el bounded context ya tiene verticales
- exponer endpoints por `{id}` cuando el dominio ya tiene business keys claras
- usar DTOs de update que permitan modificar campos identificativos
- tratar una tabla con surrogate key como si esa surrogate key fuera automáticamente la identidad del recurso
- diseñar recursos como listas de filas genéricas cuando el negocio habla de “uno por tipo” o “uno por combinación de códigos”

---

# 12. Estrategia de migración recomendada

## 12.1. No hacer big bang global

No se recomienda un megarrefactor de todo el proyecto en una sola iteración.

## 12.2. Orden recomendado

1. fijar este ADR como convención
2. refactorizar `employee.contact`
3. usar `employee.contact` como patrón canónico
4. alinear `employee.presence`
5. consolidar `employee.employee` si procede
6. aplicar la convención a nuevos verticales

## 12.3. Regla para nuevos desarrollos

Mientras existan áreas aún no migradas, cualquier vertical nuevo debe ya nacer con la estructura objetivo.

---

# 13. Checklist de revisión para cualquier vertical nuevo

Antes de aceptar una implementación, revisar:

## 13.1. Arquitectura

- ¿el vertical está organizado como vertical autónomo con capas internas?
- ¿se ha evitado mezclar vertical raíz con capas raíz del mismo bounded context?

## 13.2. API

- ¿los endpoints usan business keys?
- ¿hay algún `{id}` técnico expuesto sin necesidad?
- ¿la identidad del path expresa el dominio real?

## 13.3. Dominio

- ¿la identidad funcional está clara?
- ¿qué campos son inmutables?
- ¿qué campos son mutables?
- ¿la unicidad real del negocio está modelada?

## 13.4. Persistencia

- ¿el id técnico queda encapsulado?
- ¿hay unique constraints alineadas con la identidad funcional?

## 13.5. OpenAPI

- ¿los schemas reflejan las reglas de mutabilidad?
- ¿los DTOs de update evitan modificar campos identitarios?

## 13.6. Tests

- ¿hay tests de caso feliz?
- ¿hay tests de duplicado/unicidad?
- ¿hay tests de ownership o pertenencia al recurso padre?
- ¿hay tests de validación de catálogo?
- ¿hay tests de integración con constraints reales de BD?

---

# 14. Prompt base para Copilot — creación/refactor de verticales en B4RRHH

```text
You are working in the B4RRHH project.

Mandatory project conventions:
- Organize code first by business vertical/subdomain, not by global architectural layer.
- Inside each vertical, use hexagonal architecture.
- Public APIs must always use functional business codes, never technical database IDs.
- Technical IDs may exist only for persistence and internal wiring.
- If a resource has a clear functional identity, the REST API must express it explicitly.
- Fields that are part of the functional identity are immutable after creation unless explicitly stated otherwise.
- Always keep OpenAPI, use cases, adapters, tests and resource documentation aligned.

Target package organization pattern:
- com.b4rrhh.<bounded-context>.<vertical>.application...
- com.b4rrhh.<bounded-context>.<vertical>.domain...
- com.b4rrhh.<bounded-context>.<vertical>.infrastructure...

Avoid these mistakes:
- Do not create a new root package for a vertical alongside application/domain/infrastructure inside the same bounded context.
- Do not expose endpoints by technical id when business keys exist.
- Do not allow update DTOs to modify identity fields.
- Do not confuse surrogate database keys with domain identity.

When implementing or refactoring a vertical:
1. Identify the functional business key.
2. Design the REST paths using those business keys.
3. Keep technical ids only in persistence.
4. Define immutable vs mutable fields explicitly.
5. Add database constraints aligned with domain uniqueness.
6. Add tests for duplicates, ownership, catalog validation and persistence constraints.
```

---

# 15. Prompt específico para refactorizar `employee.contact`

```text
Refactor the employee.contact vertical in B4RRHH to comply with the project architecture and API identity rules.

Project rules:
- Code must be organized first by vertical, then by hexagonal layers.
- Public APIs must use functional business codes only, never technical IDs.

Current target architecture:
- com.b4rrhh.employee.contact.application...
- com.b4rrhh.employee.contact.domain...
- com.b4rrhh.employee.contact.infrastructure...

Do not leave contact classes under a mixed structure that combines root layer packages and root vertical packages inside the employee bounded context.

Employee functional identity:
- ruleSystemCode
- employeeTypeCode
- employeeNumber

Contact functional identity within an employee:
- contactTypeCode

Domain rules:
- One contact per contact type per employee.
- contact_type_code is immutable after creation.
- contact_value is mutable.
- Keep technical ids only for persistence.
- Keep unique(employee_id, contact_type_code) in the database.
- Keep contact catalog validation using EMPLOYEE_CONTACT_TYPE.
- Keep the resource non-historized.

Required REST API:
- POST   /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
- GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts
- GET    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
- PUT    /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}
- DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts/{contactTypeCode}

OpenAPI rules:
- CreateContactRequest must contain contactTypeCode and contactValue.
- UpdateContactRequest must contain only contactValue.
- Do not use employeeId or contactId in public API paths.
- Review response DTOs to avoid exposing technical IDs unless strictly internal.

Implementation tasks:
- Move packages to the new structure.
- Rename use cases/services that still express technical-id semantics.
- Replace GetContactById with business-key based retrieval.
- Ensure ownership is enforced through employee business key + contactTypeCode.
- Update controllers, adapters, repository contracts, tests and documentation.
- Keep existing valid persistence model where possible.
```

---

# 16. Consecuencia final

A partir de este ADR:

- el patrón objetivo en `employee` es vertical-first
- las APIs del proyecto se diseñan siempre con business keys
- `employee.contact` se toma como primer vertical a refactorizar con esta convención
- cualquier nuevo vertical debe seguir ya estas reglas desde su nacimiento

Este documento debe usarse como referencia base para diseño humano, revisión técnica y prompts a Copilot.


<!-- END FILE: ADR-005-arquitectura_por_verticales_y_reglas_api.md -->


---

# FILE: ADR-006_rule_entity_type_domain.md
<a name="file-adr-006-rule-entity-type-domain-md"></a>

<!-- BEGIN FILE: ADR-006_rule_entity_type_domain.md -->

# ADR --- Rule System as Employee Regulatory Context Root

## Status

Proposed

## Context

In the B4RRHH domain model, the functional identity of an employee is
defined as:

    ruleSystemCode + employeeTypeCode + employeeNumber

Example:

    XXX-EMP-00001

This means that the employee belongs to a **rule system context**
(`XXX`), which determines the set of functional rules that apply to that
employee.

Originally, `rule_system` values resembled country codes (e.g., ESP,
PRT), which created a misleading semantic association between:

    rule_system ≠ country

In reality, a **rule system represents the regulatory or functional
context governing the employee**, not necessarily a geographical entity.

As the system grows, different categories of rule-driven catalogs are
appearing:

Examples:

Common catalogs - COUNTRY - ADDRESS_TYPE

Labour / HR catalogs - COMPANY - WORK_CENTER

Payroll catalogs - CONTRIBUTION_GROUP - PAYROLL_AREA

Currently, `rule_entity_type` has no attribute indicating the
**functional domain** to which the catalog belongs.

This limits the expressiveness of the metamodel and makes it harder to
reason about rule ownership and scope.

------------------------------------------------------------------------

## Problem

The current metamodel structure is:

    rule_system
    rule_entity_type
    rule_entity

`rule_entity_type` is flat and does not convey the functional scope of
the catalog it represents.

As the system evolves, catalogs naturally belong to different
**functional domains**, such as:

-   COMMON (cross-domain)
-   LABORAL / HR
-   PAYROLL

Without modeling this explicitly, the catalog layer risks becoming an
unstructured set of rule types.

------------------------------------------------------------------------

## Proposed Approach

Introduce a **functional domain classification** for `rule_entity_type`.

Add a new attribute:

    rule_entity_type.domain_code

Example values:

    COMMON
    LABORAL
    PAYROLL

Each `rule_entity_type` belongs to exactly one domain.

Example:

  rule_entity_type     domain_code
  -------------------- -------------
  COUNTRY              COMMON
  ADDRESS_TYPE         COMMON
  COMPANY              LABORAL
  WORK_CENTER          LABORAL
  CONTRIBUTION_GROUP   PAYROLL

This introduces semantic structure without introducing full hierarchical
complexity.

------------------------------------------------------------------------

## Rationale

This approach provides several benefits:

### 1. Improves semantic clarity

Catalog types become grouped by functional domain rather than remaining
an undifferentiated list.

### 2. Maintains backward compatibility

The existing schema and relationships remain intact.

No changes are required for:

    rule_entity
    employee identity

### 3. Enables future evolution

The domain classification could later evolve into a dedicated structure
such as:

    rule_domain

Or support hierarchical rule resolution strategies.

### 4. Avoids premature hierarchy implementation

This ADR deliberately **does not introduce a hierarchical rule system
model yet**.

The domain classification is a lightweight step toward a richer model.

------------------------------------------------------------------------

## Non‑Goals

This ADR does **not** introduce:

-   rule system hierarchies
-   rule inheritance
-   catalog override resolution
-   domain-specific query logic

These topics remain open for future design exploration.

------------------------------------------------------------------------

## Future Evolution (Potential)

Possible future enhancements may include:

-   Introducing a `rule_domain` table
-   Defining hierarchical rule contexts
-   Supporting rule inheritance across domains
-   Separating global vs context-specific catalogs

Example conceptual model:

    rule_system (context root)
        ├── COMMON
        ├── LABORAL
        └── PAYROLL

Where catalogs are attached to domains rather than directly to rule
systems.

------------------------------------------------------------------------

## Consequences

### Positive

-   Adds clarity to catalog semantics
-   Keeps the current metamodel stable
-   Enables better reasoning about rule ownership

### Negative

-   Adds a new attribute to `rule_entity_type`
-   Requires classification decisions when adding new catalog types

------------------------------------------------------------------------

## Open Questions

1.  Should `domain_code` remain an enum-like field or evolve into a
    table (`rule_domain`)?

2.  Should rule domains eventually support inheritance between rule
    systems?

3.  Should some domains be global (shared across rule systems)?

These questions are intentionally deferred until the rule model matures
further.

<!-- END FILE: ADR-006_rule_entity_type_domain.md -->


---

# FILE: ADR-007-employee-lifecycle-workflows.md
<a name="file-adr-007-employee-lifecycle-workflows-md"></a>

<!-- BEGIN FILE: ADR-007-employee-lifecycle-workflows.md -->

# ADR — Employee Lifecycle Workflows (Hire / Terminate / Rehire)

## Status

Proposed

## Context

El sistema B4RRHH ha sido diseñado siguiendo una arquitectura basada en verticales funcionales independientes (contacts, addresses, identifiers, presence, etc.), todas ellas relacionadas con el empleado mediante business key.

Este enfoque ha permitido:

* Separación clara de responsabilidades
* Evolución independiente de cada vertical
* APIs limpias y desacopladas
* Integración progresiva en frontend mediante composición de bloques

Sin embargo, este modelo presenta una limitación desde el punto de vista funcional:

> El ciclo de vida del empleado no se corresponde con la creación y mantenimiento manual de múltiples verticales independientes.

En la práctica, acciones como contratar, despedir o recontratar a un empleado implican:

* creación o modificación coordinada de múltiples verticales
* reglas de coherencia temporal (fechas efectivas)
* validaciones transversales
* significado funcional único (no técnico)

Actualmente, el sistema permitiría modelar estas acciones como una secuencia de operaciones independientes (crear employee, luego presence, luego assignment, etc.), lo cual:

* no refleja el dominio real
* degrada la experiencia de usuario
* aumenta el riesgo de inconsistencias

Por tanto, se identifica la necesidad de introducir una nueva capa de **operaciones de negocio compuestas**, que representen el ciclo de vida del empleado.

---

## Decision

Se introduce el concepto de **Employee Lifecycle Workflows**, como una capa funcional por encima de las verticales existentes.

Estos workflows representan acciones de negocio completas que afectan a múltiples partes del modelo de empleado de forma coordinada.

### Workflows iniciales definidos

* **Hire Employee**
* **Terminate Employee**
* **Rehire Employee**

Estos workflows:

* no sustituyen a las verticales existentes
* no alteran el modelo de datos base
* actúan como orquestadores de operaciones sobre múltiples verticales

---

## Design Principles

### 1. Separación entre recursos y acciones

Se distingue claramente entre:

* **Recursos de dominio**
  (employee, presence, contact, identifier, etc.)

* **Acciones de negocio**
  (hire, terminate, rehire)

Los workflows no son recursos persistentes, sino casos de uso.

---

### 2. Orquestación coherente

Cada workflow:

* ejecuta múltiples operaciones sobre distintas verticales
* garantiza consistencia funcional (fechas, estados, relaciones)
* evita que el usuario tenga que ensamblar manualmente el estado del empleado

---

### 3. Persistencia desacoplada

Las verticales existentes:

* mantienen su diseño actual
* siguen siendo accesibles de forma independiente
* siguen siendo la base del modelo

Los workflows no introducen nuevas tablas “monolíticas”.

---

### 4. UX orientada a intención

El sistema debe permitir que el usuario piense en términos de:

* “contratar empleado”
* “despedir empleado”

y no en:

* “crear presence”
* “crear assignment”
* “actualizar estado”

---

### 5. Transparencia

Los workflows deben:

* ser explícitos en qué operaciones realizan
* evitar efectos ocultos
* permitir trazabilidad futura

---

## Workflow Definitions

### 1. Hire Employee

#### Descripción

Inicia la vida laboral de un empleado en el sistema.

#### Operaciones implicadas

* creación de employee core
* creación de primera presence
* creación de asignación organizativa inicial (work center, cost center, etc.)
* inicialización de estado laboral

#### Datos mínimos esperados (orientativo)

* employeeNumber
* employeeTypeCode
* ruleSystemCode
* nombre y apellidos
* fecha de entrada
* entryReasonCode
* companyCode
* workCenter (u otra asignación organizativa mínima)

#### Reglas clave

* todas las entidades iniciales deben compartir coherencia temporal
* debe existir una presence activa tras el proceso
* el empleado queda en estado funcional válido

---

### 2. Terminate Employee

#### Descripción

Finaliza la relación laboral de un empleado.

#### Operaciones implicadas

* cierre de presence activa
* registro de fecha de salida
* registro de exitReasonCode
* cierre o ajuste de asignaciones vigentes

#### Reglas clave

* no puede existir más de una presence activa
* tras la terminación no debe quedar ninguna presence abierta
* se preserva el histórico completo

---

### 3. Rehire Employee

#### Descripción

Reincorpora a un empleado previamente terminado.

#### Operaciones implicadas

* creación de nueva presence
* creación de nuevas asignaciones iniciales
* reutilización del employee existente

#### Reglas clave

* no se crea un nuevo employee
* se mantiene histórico de presencias anteriores
* la nueva presence debe ser coherente con las anteriores

---

## API Considerations (Future)

Se prevé la introducción de endpoints específicos para workflows, por ejemplo:

* `POST /employees/hire`
* `POST /employees/{employeeId}/terminate`
* `POST /employees/{employeeId}/rehire`

Estos endpoints:

* encapsularán la lógica de orquestación
* recibirán payloads orientados a negocio
* no expondrán directamente detalles internos de cada vertical

---

## UI Considerations (Future)

Los workflows se expondrán como acciones de primer nivel en la navegación:

* Employee

  * Ficha (visualización y mantenimiento)
  * Contratar (Hire)
  * Despedir (Terminate)
  * Recontratar (Rehire)

Cada workflow se implementará como:

* pantalla dedicada o flujo guiado (no modal simple)
* formulario estructurado por bloques
* validación previa antes de ejecución

---

## Consequences

### Positivas

* Mejor alineación con el dominio real
* Mejora significativa de UX
* Reducción de inconsistencias funcionales
* Reutilización del modelo existente
* Escalabilidad para nuevas acciones de negocio

### Negativas / Riesgos

* Incremento de complejidad en capa de aplicación
* Necesidad de definir reglas de negocio claras
* Posible duplicidad si no se gobierna bien la relación entre workflows y verticales

---

## Alternatives Considered

### 1. Mantener solo operaciones CRUD por vertical

Descartado:

* no representa el dominio real
* UX pobre
* alto riesgo de inconsistencias

### 2. Convertir employee en un agregado monolítico

Descartado:

* rompe la arquitectura modular actual
* reduce flexibilidad
* dificulta evolución

---

## Open Questions

* Definición exacta del mínimo necesario para cada workflow
* Gestión de validaciones complejas entre verticales
* Estrategia de versionado de workflows
* Auditoría y trazabilidad de ejecuciones

---

## Summary

El empleado no debe modelarse únicamente como un conjunto de datos, sino como un objeto con ciclo de vida.

La introducción de **Employee Lifecycle Workflows** permite:

* mantener la arquitectura modular existente
* añadir una capa funcional coherente con el negocio
* mejorar significativamente la experiencia de usuario

Este ADR establece la base conceptual para futuras implementaciones de Hire, Terminate y Rehire en el sistema B4RRHH.

<!-- END FILE: ADR-007-employee-lifecycle-workflows.md -->


---

# FILE: ADR-008-strong-timeline-replace-pattern.md
<a name="file-adr-008-strong-timeline-replace-pattern-md"></a>

<!-- BEGIN FILE: ADR-008-strong-timeline-replace-pattern.md -->

# B4RRHH – Strong Timeline Replace Pattern

## 1. Context

En múltiples verticales del dominio employee (por ejemplo:

* labor_classification
* contract)

existe una operación común:

**replaceFromDate(effectiveDate)**

Esta operación:

* sustituye el valor activo a partir de una fecha
* respeta la continuidad temporal
* puede implicar división de periodos existentes

Estas verticales pertenecen al tipo:

**STRONG_TIMELINE**

---

## 2. Problema

La lógica de replaceFromDate incluye una parte repetida en varias verticales:

* detección de tramo que cubre la fecha
* distinción entre:

  * exact match (startDate == effectiveDate)
  * split (fecha dentro del tramo)
  * no covering period
* cálculo de nuevos límites temporales

Antes, esta lógica estaba duplicada en cada vertical.

---

## 3. Decisión

Se introduce un helper técnico reutilizable:

### StrongTimelineReplacePlanner

Este componente:

* recibe una lista ordenada de DateRange
* recibe una effectiveDate
* devuelve un plan de reemplazo (StrongTimelineReplacePlan)

---

## 4. Modelo

### ReplaceMode

* NO_COVERING
* EXACT_START
* SPLIT

### StrongTimelineReplacePlan

Describe el resultado del análisis temporal:

* tipo de operación
* tramo afectado
* posibles nuevos rangos temporales

---

## 5. Responsabilidades

### Planner (helper técnico)

Responsable de:

* analizar geometría temporal
* decidir tipo de operación
* calcular fechas derivadas

NO es responsable de:

* validaciones de negocio
* catálogo
* relaciones
* persistencia
* excepciones de dominio

---

### Servicios de vertical

Siguen siendo responsables de:

* construir agregados
* validar reglas de negocio
* validar no solape
* validar coverage completo
* persistir cambios

---

## 6. Patrón de uso

Para cualquier vertical STRONG_TIMELINE:

1. Cargar histórico ordenado
2. Convertir a DateRange
3. Invocar planner:
   → StrongTimelineReplacePlan
4. Aplicar lógica de dominio según el plan:

   * EXACT_START → update
   * SPLIT → close + create
   * NO_COVERING → decidir comportamiento
5. Construir projected history
6. Validar timeline
7. Persistir

---

## 7. Cuándo usar este patrón

Aplicar StrongTimelineReplacePlanner SOLO cuando:

* la vertical es STRONG_TIMELINE
* existe operación replaceFromDate
* hay garantía de:

  * no solape
  * un único activo por fecha

Ejemplos:

* contract ✅
* labor_classification ✅

No aplicar directamente a:

* cost_center (DISTRIBUTED_TIMELINE)
* verticales sin cobertura completa

---

## 8. Beneficios

* elimina duplicación de lógica temporal crítica
* mejora legibilidad de servicios
* introduce lenguaje común
* reduce errores en operaciones de split

---

## 9. Regla de evolución

Este helper:

* puede evolucionar si aparece en ≥ 3 verticales
* NO debe convertirse en:

  * engine genérico
  * framework configurable
  * capa de negocio

---

## 10. Decisión futura

Si nuevas verticales STRONG_TIMELINE aparecen:

→ deben reutilizar este planner

Si aparecen variaciones significativas:

→ evaluar extensión del planner, no duplicación

---

## 11. Estado

Patrón activo y recomendado.

<!-- END FILE: ADR-008-strong-timeline-replace-pattern.md -->


---

# FILE: ADR-009-journey.md
<a name="file-adr-009-journey-md"></a>

<!-- BEGIN FILE: ADR-009-journey.md -->

Design Principles
1. Journey debe contar la historia del empleado

Un journey debe responder a:

qué pasó

cuándo pasó

cómo se interpreta funcionalmente

por qué ese evento importa

No debe limitarse a agrupar verticales en paralelo.

2. El frontend no debe inferir semántica de negocio compleja

El frontend no debe deducir por sí solo si algo es:

un alta

una recontratación

un cambio de contrato

un cambio de clasificación

Esa interpretación debe resolverse en backend, dentro del read model.

3. Journey es una proyección read-only de UI

Journey V2 no sustituye:

verticales canónicas

endpoints de escritura

recursos de dominio independientes

Es una proyección agregada para experiencia de usuario.

4. Tracks siguen teniendo valor

La vista actual por tracks sigue siendo útil para:

inspección técnica

validación funcional

representación por vertical

Por tanto, no debe considerarse un fracaso ni descartarse.

Current Model Reclassification

El modelo actualmente expuesto bajo journey debe reinterpretarse como:

Employee Tracks

Employee History Tracks

o naming equivalente

No se recomienda seguir llamándolo “journey” en su estado actual.

Journey V2 — Target Model
Shape propuesto
{
  "employee": {
    "ruleSystemCode": "ESP",
    "employeeTypeCode": "INTERNAL",
    "employeeNumber": "EMP010",
    "displayName": "Juan Antonio Biforcos Amor"
  },
  "events": [
    {
      "eventDate": "2023-01-10",
      "eventType": "HIRE",
      "trackCode": "PRESENCE",
      "title": "Alta en la empresa",
      "subtitle": "ES01 · período #1",
      "status": "completed",
      "isCurrent": false,
      "details": {
        "companyCode": "ES01",
        "entryReasonCode": "HIRING"
      }
    }
  ]
}
Event Model

Cada evento debe incluir, como mínimo:

eventDate

eventType

trackCode

title

subtitle

status

isCurrent

details

Campo eventDate

Fecha efectiva del evento en la línea temporal.

Campo eventType

Tipo de evento normalizado y entendible para frontend.

Campo trackCode

Origen funcional del evento:

PRESENCE

CONTRACT

LABOR_CLASSIFICATION

WORK_CENTER

COST_CENTER

etc.

Campo title

Texto principal listo para UI.

Campo subtitle

Contexto breve y útil para lectura rápida.

Campo status

Estado visual del evento:

completed

current

future

u otro conjunto acotado

Campo isCurrent

Flag explícito para eventos actualmente vigentes o activos.

Campo details

Información adicional libre y limitada, para tooltips, badges o ampliación contextual.

Initial Event Types
Presence

HIRE

REHIRE

TERMINATION

PRESENCE_START

PRESENCE_END

Contract

CONTRACT_START

CONTRACT_CHANGE

CONTRACT_END

Labor Classification

LABOR_CLASSIFICATION_START

LABOR_CLASSIFICATION_CHANGE

LABOR_CLASSIFICATION_END

Future

WORK_CENTER_CHANGE

COST_CENTER_CHANGE

ASSIGNMENT_CHANGE

Backend Interpretation Rules

El backend debe encargarse de transformar ocurrencias de verticales en eventos funcionales.

Ejemplos

una nueva presence con motivo de entrada inicial puede generar HIRE

una nueva presence posterior a una terminación puede generar REHIRE

el cierre de una presence con motivo adecuado puede generar TERMINATION

un cambio de contrato genera CONTRACT_CHANGE

una nueva clasificación laboral genera LABOR_CLASSIFICATION_CHANGE

El frontend no debe deducir esta semántica a partir de details.

Relationship with Lifecycle Workflows

Journey V2 se alinea directamente con el ADR de lifecycle workflows:

Hire

Terminate

Rehire

Esto permite que la timeline agregada represente:

estados

transiciones

hitos funcionales del ciclo de vida del empleado

y no solo snapshots por vertical.

UI Considerations
Objetivo de Journey V2

Permitir una representación clara de:

la historia laboral del empleado

eventos relevantes

cambios significativos

estado actual dentro de la secuencia

Recomendación visual

Se recomienda una timeline orientada a eventos cronológicos, preferentemente:

vertical

o híbrida compacta

No se considera óptimo reutilizar directamente la representación actual por tracks para este objetivo.

Tracks en UI

La vista por tracks puede seguir existiendo como:

vista técnica

vista avanzada

o modo alternativo de inspección

Pero no como definición principal de “journey”.

Migration Strategy
Phase 1 — Reclassify current model

aceptar que el modelo actual es tracks

ajustar naming interno/documentación si procede

mantener compatibilidad

Phase 2 — Design Journey V2 contract

definir EmployeeJourneyV2Response

definir catálogo inicial de eventType

acordar reglas de agregación en backend

Phase 3 — Implement backend projection

construir la proyección events[]

reutilizando verticales existentes

sin alterar recursos canónicos

Phase 4 — Adapt frontend

nuevo client / mapper / store / UI para journey V2

timeline realmente cronológica

dejar la vista actual por tracks como opcional o técnica

Consequences
Positivas

naming más honesto

mejor alineación semántica

journey verdaderamente útil para UI

menos lógica interpretativa en frontend

mejor encaje con lifecycle workflows

Negativas / Costes

hay que diseñar un segundo read model

el backend debe añadir reglas de interpretación

puede convivir temporalmente más de una proyección agregada

habrá que ajustar frontend para el nuevo shape

Alternatives Considered
1. Mantener el shape actual y mejorarlo solo en frontend

Descartado como solución final:

obliga al frontend a inferir demasiada semántica

sigue sin representar bien un journey

2. Renombrar simplemente el endpoint actual y no hacer V2

Insuficiente:

arregla naming

no resuelve la necesidad de una timeline funcional de eventos

3. Convertir journey en agregado completo de dominio

Descartado:

journey debe seguir siendo una proyección read-only

no debe sustituir a verticales canónicas

Summary

El modelo actual no representa un journey, sino una vista histórica por tracks.

Se decide:

reclasificar conceptualmente el modelo actual como tracks

diseñar Journey V2 como una proyección cronológica de eventos

mantener separadas:

la vista técnica por tracks

la vista funcional de journey

Esto permitirá construir una timeline realmente útil para frontend, alineada con el ciclo de vida del empleado y con la semántica de negocio del dominio.
<!-- END FILE: ADR-009-journey.md -->


---

# FILE: ADR-010-employee-frontend-editing.md
<a name="file-adr-010-employee-frontend-editing-md"></a>

<!-- BEGIN FILE: ADR-010-employee-frontend-editing.md -->

# ADR — Employee Frontend Editing Pattern by Vertical Maintenance Mode

## Status
PROPOSED

---

## Contexto

El frontend de B4RRHH ya permite visualizar la ficha de empleado basada en verticales independientes:

- Arquitectura: client → mapper → gateway → store → UI
- Backend basado en business keys (no IDs técnicos)
- Verticales con distinta naturaleza:
  - Datos simples (contactos, identificadores)
  - Datos temporales (direcciones)
  - Datos complejos (presence, contracts)

Actualmente la UI es read-only y se requiere introducir edición.

---

## Problema

Diseñar un patrón de edición que:
- Sea consistente
- Respete el dominio
- Evite sobreingeniería
- Escale a futuro

Además, en verticales temporales, debe distinguirse entre:
- cambio funcional real
- corrección administrativa de una ocurrencia mal capturada

Sin esa distinción, el frontend puede acabar representando errores de captura como si fueran eventos reales de negocio.

---

## Decisión

Se adopta el patrón:

## Editable Resource Block by Maintenance Mode

---

## 1. Unidad de interacción: BLOQUE

Cada vertical se representa como un bloque autónomo.

Reglas:
- Independiente
- Con su propio estado
- Sin edición global de ficha

---

## 2. Maintenance Mode

Cada vertical define su modo de mantenimiento:

### SLOT
Para verticales tipo lista simple o “slot por tipo”.

Uso típico:
- contactos
- identificadores

Características:
- Alta
- Edición
- Eliminación
- Operación normalmente centrada en una fila

---

### TEMPORAL_APPEND_CLOSE
Para verticales historizados cuyo cambio funcional normal se expresa mediante:
- alta de una nueva ocurrencia
- cierre de la ocurrencia vigente

Uso típico:
- direcciones

Características:
- No modela un update directo como cambio normal
- El histórico se preserva por append + close
- La semántica principal es temporal, no CRUD clásico

#### Nota importante
TEMPORAL_APPEND_CLOSE **no implica** que toda modificación de una ocurrencia deba resolverse siempre con “cerrar y crear otra”.

Debe distinguirse entre:

##### a) Cambio funcional real
Ejemplos:
- el empleado se muda
- cambia la dirección efectiva desde una fecha
- hay un reemplazo de la ocurrencia vigente

En estos casos:
- add / append
- close
- eventualmente replace

##### b) Corrección administrativa
Ejemplos:
- calle mal escrita
- portal erróneo
- país o código postal mal informado
- error de captura reciente

En estos casos, cerrar y recrear puede:
- ensuciar el histórico
- generar falsos eventos de negocio
- introducir ruido funcional o de auditoría

Por tanto:

- TEMPORAL_APPEND_CLOSE **por defecto no incluye corrección**
- pero **puede ampliarse** con una operación explícita de `correct` si el dominio y el backend la soportan

#### Regla de frontend
El frontend **no inventará** semánticas de corrección si el backend no expone una operación compatible.

---

### WORKFLOW
Para verticales cuya modificación requiere acciones de negocio, no CRUD directo.

Uso típico:
- presence
- contracts
- labor classification

Características:
- No se presentan como edición genérica
- Se accionan mediante flujos explícitos
- Ejemplos futuros:
  - hire
  - termination
  - rehire
  - replace from date

---

### READONLY
Para verticales puramente informativos o todavía no abiertos a mantenimiento.

---

## 3. Modelo de interacción

Cada bloque tiene:

- displayMode: read | edit | create | busy | error
- maintenanceMode
- supportedActions

Ejemplo:
- contact → maintenanceMode = SLOT
- identifier → maintenanceMode = SLOT
- address → maintenanceMode = TEMPORAL_APPEND_CLOSE

---

## 4. UX

Reglas generales:
- Edición por bloque
- Una sola sesión activa por bloque
- Preferiblemente una única sesión de edición en toda la ficha en V1
- Operaciones por fila cuando aplique
- Feedback simple, discreto y local al bloque

### Principio de honestidad UX
La UI debe mostrar la acción real soportada por el dominio:
- Editar
- Añadir
- Eliminar
- Cerrar
- Corregir
- Lanzar workflow

No debe usarse “Editar” como verbo universal si la semántica real es otra.

---

## 5. Persistencia

- Backend es la fuente de verdad
- Tras mutación exitosa:
  - refresh del bloque o de la ficha
- No se introduce lógica rica de reconstrucción local si no aporta valor claro

---

## 6. Consecuencias

### Positivas
- Consistencia visual
- Respeto a la semántica del dominio
- Escalabilidad hacia workflows
- Permite distinguir entre cambio funcional y corrección administrativa
- Evita forzar CRUD donde no encaja

### Negativas
- Más componentes específicos por tipo de bloque
- Menos reutilización artificial
- Algunos verticales requerirán discusión explícita sobre si soportan `correct`

---

## 7. Alternativas descartadas

### Form builder genérico
Rechazado por pérdida de semántica y exceso de abstracción.

### Edición global de ficha
Rechazado por complejidad, peor control de estado y mal encaje con un dominio verticalizado.

### Tratar todos los temporales como append/close puro
Rechazado como regla universal porque puede convertir errores administrativos en falsos cambios funcionales.

---

## 8. Aplicación inicial

- Contactos → SLOT
- Identificadores → SLOT
- Direcciones → TEMPORAL_APPEND_CLOSE

### Decisión específica para V1 de direcciones
En V1:
- Añadir nueva dirección
- Cerrar dirección existente
- Sin corrección inline de ocurrencia existente, salvo que backend exponga operación específica

Esto se considera una decisión de alcance, no una verdad permanente del patrón.

---

## 9. Futuro

Posibles evoluciones:
- Introducción formal de operación `correct` en verticales temporales
- Integración de workflows
- Refinamiento de `supportedActions` por vertical
- ADR complementario si se consolida distinción explícita entre:
  - correction
  - replacement
  - close

---

## 10. Resumen ejecutivo

El frontend de empleado no se modelará como un gran formulario, sino como una composición de bloques autónomos.

Cada bloque declara un maintenance mode.

La edición no se unifica por “tipo de formulario”, sino por “familia de comportamiento” del vertical.

Para verticales temporales:
- el cambio funcional normal puede expresarse como append/close
- pero la corrección administrativa no debe confundirse automáticamente con un cambio de negocio

El frontend respetará siempre la semántica realmente soportada por backend.

<!-- END FILE: ADR-010-employee-frontend-editing.md -->


---

# FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md
<a name="file-adr-011-shared-lookup-decision-matrix-and-guidelines-md"></a>

<!-- BEGIN FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md -->

# B4RRHH — Matriz de adopción del patrón shared lookup y guía de diseño

Fecha: 2026-03-21

## 1. Objetivo

Este documento fija dos cosas:

1. una **matriz práctica** para decidir qué verticales de `employee` deben adoptar el patrón shared de lookup por business key;
2. una **guía de diseño estable** para que tanto una persona como Copilot mantengan la misma disciplina al crear o refactorizar verticales futuras.

El contexto actual es que ya existe un soporte shared mínimo de persistencia con `EmployeeBusinessKeyLookupSupport` y `EmployeeOwnedLookupSupport`, y ya se está usando en `contact`, `identifier` y `address` para resolver employee por business key y mapear a su contexto sin duplicar plumbing técnico. fileciteturn12file4 fileciteturn12file7 fileciteturn12file3 fileciteturn12file0 fileciteturn12file2

---

## 2. Regla madre

En B4RRHH, el código debe organizarse **primero por vertical y luego por capas**, y las APIs públicas deben trabajar con **business keys**, no con IDs técnicos. Los IDs técnicos deben quedarse en persistencia. fileciteturn12file15 fileciteturn12file16

Además, `employee.shared` sólo debe contener piezas **realmente transversales y técnicas**. No debe convertirse en un cajón de semántica de negocio. fileciteturn12file15

Traducido a esta decisión concreta:

- **sí** a helpers pequeños y explícitos para lookup transversal repetido;
- **no** a repositorios universales, engines genéricos o shared con vocabulario funcional de un vertical.

---

## 3. Qué patrón se considera ya consolidado

A día de hoy, el patrón compartido que se considera válido es éste:

1. resolver `EmployeeEntity` por business key (`ruleSystemCode`, `employeeTypeCode`, `employeeNumber`);
2. resolver opcionalmente la variante con lock (`for update`);
3. delegar en una lambda o función local del vertical;
4. mantener el mapping a `EmployeeXContext` o la excepción del vertical en el propio vertical.

Ese patrón está ya expresado en:

- `EmployeeBusinessKeyLookupSupport`, que delega en `SpringDataEmployeeRepository.findByBusinessKey(...)` y `findByBusinessKeyForUpdate(...)`; fileciteturn12file4 fileciteturn12file18
- `EmployeeOwnedLookupSupport`, que compone el lookup del employee con una función `ownedLookup`, tanto en modo `Optional` como en modo `OrThrow`; fileciteturn12file7 fileciteturn12file12
- `EmployeeContactLookupAdapter`, `EmployeeIdentifierLookupAdapter` y `EmployeeAddressLookupAdapter`, que ya usan ese soporte compartido y hacen sólo el mapping explícito a su contexto. fileciteturn12file3 fileciteturn12file0 fileciteturn12file2

---

## 4. Matriz de adopción por vertical

### 4.1 Resumen ejecutivo

| Vertical | Estado recomendado | Motivo corto |
|---|---|---|
| `contact` | Ya adoptado | lookup de owner puro y mapping simple |
| `identifier` | Ya adoptado | lookup de owner puro y mapping simple |
| `address` | Ya adoptado | lookup de owner puro y mapping simple; semántica temporal queda fuera |
| `presence` | Candidato fuerte siguiente | patrón de employee-context probablemente muy parecido |
| `workcenter` | Candidato medio | posible encaje para employee-context, pero revisar mezcla con validaciones de presencia |
| `cost_center` | Candidato medio | posible encaje para employee-context, pero revisar mezcla con temporalidad y porcentaje |
| `contract` | Esperar | vertical más cargado de timeline y replace semantics |
| `labor_classification` | Esperar | vertical más cargado de timeline, cobertura y relaciones |
| `journey` | No aplicar este patrón | es vertical de lectura/proyección, no de ownership lookup estándar |
| `employee` raíz | No aplica | es el owner, no un child vertical |

### 4.2 Lectura detallada

#### `contact` — Ya adoptado

Encaja perfectamente porque el adapter sólo resuelve employee por business key y mapea a `EmployeeContactContext`. No se mete negocio del vertical en shared. fileciteturn12file3

#### `identifier` — Ya adoptado

Mismo caso que `contact`: lookup puro del owner y mapping local. fileciteturn12file0

#### `address` — Ya adoptado

El refactor ha eliminado `EntityManager` y SQL nativo del adapter de lookup y lo ha alineado con el mismo patrón de `contact` e `identifier`. La temporalidad de `address` sigue viviendo fuera de este helper. fileciteturn12file11 fileciteturn12file2 fileciteturn12file14

#### `presence` — Candidato fuerte siguiente

Por estructura, es muy probable que tenga el mismo patrón de resolver employee owner y construir `EmployeePresenceContext`. Si el adapter se parece a `contact`/`identifier`/`address`, debería entrar. La condición es no mezclar ahí reglas como overlap, presencia activa o cierre. Además, en la arquitectura objetivo `presence` debe tender al mismo modelo vertical-first que `contact`. fileciteturn12file17

#### `workcenter` — Candidato medio

Puede encajar si existe un `EmployeeWorkCenterLookupAdapter` que sólo resuelva contexto de employee. Debe quedarse fuera cualquier lógica de cobertura respecto a presence, gaps o consistencia. Si el adapter mezcla lookup y validación de cobertura, primero hay que separarlo.

#### `cost_center` — Candidato medio

Misma idea que `workcenter`. Puede encajar para la parte de owner lookup, pero sólo si la lógica de asignación, porcentaje y restricciones temporales permanece fuera.

#### `contract` — Esperar

Aquí el riesgo de contaminar el refactor con lógica temporal es alto: `replaceFromDate`, cobertura de presence, subtype relation, cierre, update con semántica fuerte. Mejor no meterlo aún en esta ola.

#### `labor_classification` — Esperar

Caso parecido a `contract`: reglas temporales más ricas y mayor probabilidad de que lookup y negocio estén más acoplados.

#### `journey` — No aplicar

`journey` es un vertical de lectura/proyección. Su problema no es ownership lookup de un child resource estándar, sino composición de tracks y eventos. Este patrón no le aporta gran cosa.

---

## 5. Regla de decisión rápida

Una vertical **debe entrar** en el patrón shared si se cumplen estas cinco:

1. el adapter necesita resolver employee por business key;
2. la parte repetida es claramente técnica;
3. el resultado es un contexto o un owned lookup simple;
4. el shared no necesita aprender vocabulario del vertical;
5. el adapter queda más legible después del cambio.

Una vertical **no debe entrar todavía** si pasa cualquiera de estas cuatro:

1. el refactor arrastra reglas temporales o de negocio;
2. obliga a meter semántica funcional del vertical en `shared`;
3. hace el código más mágico o más difícil de depurar;
4. la identidad funcional del recurso hijo todavía no está del todo clara.

---

## 6. Qué sí puede vivir en `employee.shared`

### Sí

- lookup de `EmployeeEntity` por business key; fileciteturn12file4
- variante con lock (`for update`); fileciteturn12file4
- composición técnica `employee -> ownedLookup`; fileciteturn12file7
- utilidades técnicas pequeñas y explícitas que se repiten igual en varios verticales.

### No

- `contactTypeCode`, `identifierTypeCode`, `addressTypeCode`, `addressNumber`, etc.;
- validaciones de catálogo del vertical;
- lógica de overlap, coverage, split, replace, close o correct;
- excepciones de dominio genéricas que sustituyan a las del vertical;
- repositorios universales tipo `EmployeeOwnedRepository<T, K>`.

---

## 7. Convenciones de diseño que deben quedar escritas

### 7.1 Identidad

La identidad pública siempre debe expresarse con business keys. En `employee`, eso significa al menos:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber` fileciteturn12file15 fileciteturn12file16

Los IDs técnicos sólo deben vivir en persistencia. fileciteturn12file15

### 7.2 Organización del código

El patrón objetivo sigue siendo:

- `com.b4rrhh.<bounded-context>.<vertical>.application`
- `com.b4rrhh.<bounded-context>.<vertical>.domain`
- `com.b4rrhh.<bounded-context>.<vertical>.infrastructure` fileciteturn12file15

### 7.3 Regla de abstracción

Extraer helper sólo cuando:

- el patrón ya se repite;
- la variación está entendida;
- la abstracción hace el código más simple, no más listo.

### 7.4 Mapping

El mapping de `EmployeeEntity -> EmployeeXContext` debe seguir siendo **local al adapter del vertical**, como ya pasa en `contact`, `identifier` y `address`. fileciteturn12file3 fileciteturn12file0 fileciteturn12file2

### 7.5 Tests mínimos al introducir una vertical en este patrón

Cada adapter que adopte el patrón debe tener al menos:

- caso feliz en lookup normal;
- caso feliz en lookup for-update;
- `Optional.empty()` cuando no existe employee en lookup normal;
- `Optional.empty()` cuando no existe employee en lookup for-update.

Eso ya está aplicado en `address`, y de forma equivalente en `contact` e `identifier`. fileciteturn12file5 fileciteturn12file8 fileciteturn12file9

---

## 8. Guardarraíles para diseñar verticales nuevas

Cuando nazca una vertical nueva bajo `employee`, aplicar este checklist antes de escribir código:

1. **Identidad funcional**: ¿cuál es la business key pública del recurso?
2. **Ownership**: ¿ese recurso cuelga de employee por business key?
3. **Naturaleza**: ¿es `SLOT`, `TEMPORAL_APPEND_CLOSE`, workflow u otra familia? Esto es importante también para frontend y UX. En V1, por ejemplo, `contact` e `identifier` se tratan como `SLOT` y `address` como `TEMPORAL_APPEND_CLOSE`. fileciteturn12file19
4. **Lookup técnico**: ¿hay un adapter de contexto que sólo resuelve employee? Si sí, debe usar el patrón shared.
5. **Reglas de negocio**: ¿qué debe quedarse fuera de shared sí o sí?
6. **DTOs y endpoints**: ¿están formulados por business keys y no por IDs técnicos?
7. **Tests**: ¿se están probando ownership, duplicados, validación y constraints? fileciteturn12file15

---

## 9. Prompt base recomendado para Copilot

### Uso

Este bloque sirve como cabecera de contexto para cualquier refactor o implementación futura en verticales `employee`.

```text
You are working in the B4RRHH project.

Mandatory project rules:
- Architecture is vertical-first inside each bounded context.
- Public APIs must use functional business keys, never technical IDs.
- Technical IDs must remain inside persistence.
- employee.shared may contain only truly transversal technical support.
- Do NOT move vertical-specific business rules into shared.
- Prefer small explicit helpers over generic frameworks.
- Keep mappings local to the vertical adapter when they express vertical context.
- Introduce abstractions only when the pattern is already repeated and variation is understood.

Current shared lookup pattern already accepted:
- EmployeeBusinessKeyLookupSupport resolves EmployeeEntity by business key, including for-update lookup.
- EmployeeOwnedLookupSupport composes employee lookup with a local owned lookup function.
- contact, identifier and address already use this pattern for employee-context lookup.

Design guardrails:
- Do not create universal repositories.
- Do not create generic domain exceptions that replace vertical exceptions.
- Do not move overlap, temporal, coverage, replace, close or correction semantics into shared.
- Keep EmployeeEntity -> EmployeeXContext mapping local to the vertical adapter.
- Any new vertical must first define its functional identity and maintenance model.

When deciding whether a vertical should adopt the shared lookup pattern, use this rule:
Adopt it only if the duplicated code is clearly technical owner lookup and the result is simpler after refactoring.
```

---

## 10. Decisión operativa recomendada desde hoy

Orden sugerido para próximas revisiones:

1. `presence`
2. `workcenter`
3. `cost_center`
4. parar y reevaluar
5. dejar `contract` y `labor_classification` para una discusión separada

La razón es simple: conviene seguir capturando el patrón donde el beneficio es alto y el riesgo semántico es bajo, y frenar antes de entrar en verticales donde la lógica temporal fuerte pueda contaminar la abstracción.

---

## 11. Resumen ejecutivo

- El patrón shared de lookup ya está consolidado para `contact`, `identifier` y `address`. fileciteturn12file3 fileciteturn12file0 fileciteturn12file2
- El patrón correcto es pequeño: resolver employee por business key, componer una lambda local y dejar mapping/excepciones en el vertical. fileciteturn12file4 fileciteturn12file7
- `presence` es el siguiente candidato natural.
- `workcenter` y `cost_center` son candidatos posibles, pero sólo para la parte de owner lookup.
- `contract` y `labor_classification` deben esperar.
- Este documento debe usarse como criterio de diseño y como prólogo de prompts para Copilot.

<!-- END FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md -->


---

# FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md
<a name="file-adr-012-racionalizaci-n-de-naming-y-alcance-sem-ntico-de-rule-entity-type-md"></a>

<!-- BEGIN FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md -->

ADR — Racionalización de naming y alcance semántico de rule_entity_type en B4RRHH
Estado

Propuesto

Contexto

B4RRHH ya dispone de un metamodelo funcional basado en:

rule_system
rule_entity_type
rule_entity

Este metamodelo está empezando a exponerse y utilizarse de forma real desde frontend mediante una pantalla de catálogos, lo que ha hecho visible una tensión de diseño:

algunos rule_entity_type fueron nombrados inicialmente desde la vertical o caso de uso donde aparecieron primero
al crecer el sistema, se observa que ciertos conceptos no pertenecen realmente a una sola vertical, sino que son reutilizables en varias partes del dominio

Ejemplo típico:

un nombre como EMPLOYEE_PRESENCE_COMPANY puede haber sido razonable en una iteración temprana
pero al madurar el dominio, “company” aparece como concepto reutilizable también en otras verticales o workflows
por tanto, el naming anterior queda demasiado estrecho

Además, los seeds iniciales de rule_entity y sus labels visibles pueden haber sido definidos con una orientación más técnica o provisional que funcional.

Esto no invalida el modelo actual, pero sí revela una deuda semántica normal de maduración.

La arquitectura general del proyecto prioriza:

vertical-first
business keys en APIs
naming orientado a negocio y estable
separación clara entre dominio y detalle técnico
Problema

Sin una guía explícita, el catálogo corre el riesgo de evolucionar como una mezcla de:

conceptos reutilizables del dominio
conceptos específicos de una vertical
labels provisionales de seeds
nombres demasiado pegados a una implementación temporal

Esto genera varios riesgos:

semántica inconsistente
duplicidad futura de tipos de entidad
dificultad para reutilizar catálogos transversales
prompts peores para Copilot
APIs y validadores atados a nombres demasiado concretos
Decisión

Se adopta una convención explícita para diseñar y revisar rule_entity_type y rule_entity:

1. Un rule_entity_type debe nombrar el concepto funcional real, no el primer lugar donde se usó

Ejemplos:

preferir COMPANY
evitar EMPLOYEE_PRESENCE_COMPANY si el concepto “company” es reutilizable
2. Los tipos de entidad se clasifican por alcance semántico
A. Domain reusable catalog

Conceptos reutilizables en más de una vertical o bounded context relacionado.

Ejemplos:

COMPANY
WORK_CENTER
COST_CENTER
COUNTRY
B. Employee-specific catalog

Conceptos propios del bounded context employee, pero no de una única vertical técnica.

Ejemplos:

EMPLOYEE_CONTACT_TYPE
EMPLOYEE_IDENTIFIER_TYPE
EMPLOYEE_ADDRESS_TYPE
C. Lifecycle-specific catalog

Conceptos ligados a una acción o transición funcional del ciclo de vida laboral.

Ejemplos:

EMPLOYEE_ENTRY_REASON
EMPLOYEE_EXIT_REASON
3. El naming debe seguir el criterio de reutilización máxima razonable

Regla práctica:

si el concepto puede ser usado de forma natural por varias verticales, debe nombrarse de forma genérica
si el concepto solo tiene sentido en un contexto funcional específico, puede nombrarse de forma específica
no debe usarse un prefijo de vertical solo porque el primer consumidor pertenezca a esa vertical
4. rule_entity.code debe ser estable y funcional

El code:

debe ser estable
debe evitar ruido técnico
no debe incorporar accidentalmente detalles de UI o de implementación
5. rule_entity.name debe tratarse como label funcional visible

El name:

no es la identidad
puede evolucionar para mejorar claridad funcional
debe pensarse como literal entendible por usuario/negocio
6. description se reserva para contexto adicional, no para sustituir al nombre

La descripción:

amplía
no corrige un name pobre
no debe convertirse en el único lugar donde vive la semántica
No objetivos

Este ADR no introduce todavía:

renombrado masivo inmediato de tipos existentes
migraciones globales de seeds
jerarquías complejas entre tipos
nuevo modelo de persistencia
UI para mantenimiento de rule_entity_type
Estrategia de aplicación
1. No hacer big bang

No se recomienda un renombrado inmediato de todos los tipos actuales.

2. Aplicación a futuro

A partir de este ADR:

todo rule_entity_type nuevo debe pasar por esta revisión semántica
Copilot debe recibir esta regla en prompts de backend y metamodelo
los nombres nuevos no deben quedar estrechamente acoplados a la primera vertical consumidora
3. Revisión incremental de deuda existente

Los tipos actuales que hayan quedado demasiado específicos se documentarán como deuda semántica y se revisarán cuando compense funcionalmente.

Checklist para nuevos rule_entity_type

Antes de crear uno nuevo, revisar:

¿Describe un concepto reutilizable o una regla local?
¿Ese concepto podría ser consumido por otra vertical en los próximos pasos?
¿El nombre está reflejando el dominio o la implementación actual?
¿Estamos poniendo prefijo de vertical por necesidad real o por comodidad momentánea?
¿El name visible es suficientemente funcional para usuario/negocio?
Ejemplos orientativos
Buenos candidatos a naming genérico
COMPANY
WORK_CENTER
COST_CENTER
COUNTRY
Buenos candidatos a naming específico
EMPLOYEE_CONTACT_TYPE
EMPLOYEE_IDENTIFIER_TYPE
EMPLOYEE_ENTRY_REASON
EMPLOYEE_EXIT_REASON
Sospechosos a revisar
tipos cuyo nombre empiece por una vertical concreta pero describan un concepto reutilizable
tipos cuyo name visible parezca una explicación provisional y no una etiqueta funcional
Consecuencias positivas
mejor semántica de dominio
mayor reutilización de catálogos
menor duplicidad futura
prompts más precisos
mejor UX en la pantalla de catálogos
Consecuencias negativas
aparece deuda visible en nombres ya existentes
obliga a pensar más antes de crear nuevos tipos
en el futuro puede requerir migraciones o aliases si se decide racionalizar nombres existentes
<!-- END FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md -->


---

# FILE: ADR-013-Mantenimiento-de-rule_entity.md
<a name="file-adr-013-mantenimiento-de-rule-entity-md"></a>

<!-- BEGIN FILE: ADR-013-Mantenimiento-de-rule_entity.md -->

ADR — Mantenimiento de rule_entity en B4RRHH
Estado

Propuesto

Contexto

B4RRHH ya dispone de un metamodelo funcional basado en:

rule_system
rule_entity_type
rule_entity

Actualmente el contrato expone para rule_entity:

POST /rule-entities
GET /rule-entities con filtros por business keys

y el modelo público incluye:

ruleSystemCode
ruleEntityTypeCode
code
name
description
active
startDate
endDate

La pantalla de catálogos ya permite:

seleccionar rule_system
seleccionar rule_entity_type
listar rule_entity
crear nuevos valores de catálogo

Sin embargo, todavía no existe una estrategia explícita de mantenimiento para:

corregir una ocurrencia existente
cerrar su vigencia
eliminar ocurrencias erróneas sin uso

Además, B4RRHH ya distingue en frontend entre:

edición tipo SLOT
mantenimiento temporal
corrección administrativa frente a cambio funcional real

También existe ya una decisión previa de naming/semántica: rule_entity_type debe nombrar el concepto funcional real y rule_entity.code debe ser estable, mientras que name actúa como label funcional visible

Problema

Si rule_entity se trata como un CRUD plano, aparecen varios riesgos:

confusión entre identidad y datos corregibles
pérdida de histórico semántico
borrados peligrosos de valores de catálogo ya usados por empleados u otros recursos
un frontend que muestra verbos genéricos sin reflejar la semántica real del dominio

Por el contrario, si se prohíbe todo mantenimiento salvo el alta, el catálogo queda operativamente incompleto.

Es necesario definir:

qué constituye la identidad funcional de una ocurrencia de rule_entity
qué operaciones canónicas existen
qué se puede corregir
cuándo procede cerrar
si existe DELETE, en qué condiciones
Decisión

Se adopta para rule_entity un modelo de mantenimiento de catálogo con vigencia y borrado excepcional restringido.

1. Naturaleza funcional

rule_entity se modela como un catálogo parametrizable con vigencia temporal ligera:

puede tener histórico por código
no exige cobertura continua
no debe tratarse como CRUD plano
no debe confundirse corrección administrativa con cambio funcional
2. Identidad funcional

La identidad funcional de una ocurrencia de rule_entity será:

ruleSystemCode
ruleEntityTypeCode
code
startDate

Esta combinación identifica una ocurrencia concreta del valor de catálogo.

3. Campos inmutables

Una vez creada la ocurrencia, no podrán modificarse:

ruleSystemCode
ruleEntityTypeCode
code
startDate
4. Campos corregibles

Podrán corregirse:

name
description
endDate
5. Tratamiento de active

active se considera preferentemente un dato derivado/read-model a partir de la vigencia real.

Mientras el contrato público lo mantenga, backend podrá seguir retornándolo, pero el mantenimiento canónico no debe apoyarse en editar active de forma arbitraria si eso duplica la semántica de endDate.

6. Operaciones canónicas
6.1 Crear

Se mantiene:

POST /rule-entities
6.2 Consultar una ocurrencia concreta

Se añade una lectura canónica por business key completa:

GET /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
6.3 Corregir una ocurrencia existente

Se añade una operación de corrección administrativa:

PUT /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}

Esta operación corrige la misma ocurrencia y no crea una nueva.

Campos permitidos en request:

name
description
endDate
6.4 Cerrar vigencia

Se añade una operación explícita de cierre:

POST /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}/close

Request:

endDate

Su semántica es cerrar la vigencia de la ocurrencia existente.

6.5 Eliminar

Se admite DELETE, pero como operación excepcional y restringida, no como verbo principal de mantenimiento:

DELETE /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}

Su semántica es borrado físico de una ocurrencia de catálogo solo si backend demuestra que no está usada.

Reglas de borrado

DELETE solo estará permitido si se cumplen todas las condiciones siguientes:

La ocurrencia existe.
La comprobación se realiza dentro del rule_system de la ocurrencia.
La rule_entity no está referenciada por ningún recurso de negocio existente que dependa de ella.
La comprobación de referencias debe hacerse en backend, nunca en frontend.
Si existen referencias, la operación falla con conflicto de negocio y no degrada a soft delete implícito.
Política de referencias

Se considera “referenciada” una rule_entity cuando su código está siendo usado por cualquier recurso real que la valide o consuma en ese rule_system.

Ejemplos típicos:

companyCode en presence
contactTypeCode en contacts
identifierTypeCode en identifiers
addressTypeCode en addresses
workCenterCode en work centers
costCenterCode en cost centers
contractCode o contractSubtypeCode en contracts
agreementCode o agreementCategoryCode en labor classifications

La comprobación exacta dependerá del ruleEntityTypeCode y de los verticales que consuman ese catálogo.

Reglas de dominio adicionales
no puede haber solape de vigencia para la misma combinación ruleSystemCode + ruleEntityTypeCode + code
endDate no puede ser menor que startDate
una corrección administrativa no debe alterar la identidad funcional
un cierre expresa fin de vigencia, no borrado
un cambio funcional normal puede resolverse como cierre de la ocurrencia vigente y alta de una nueva ocurrencia
Semántica de frontend

El frontend de catálogos debe exponer acciones honestas y alineadas con backend:

Crear
Editar
entendido como corrección administrativa de la misma ocurrencia
Cerrar
Eliminar
solo cuando backend lo soporte y sujeto a error si existen referencias

El frontend no debe:

editar rule_entity_type
cambiar la identidad funcional de una ocurrencia
simular borrados si backend no los confirma
ocultar el motivo de rechazo cuando una entity no puede borrarse por estar en uso
Errores esperados
404 Not Found

Cuando la ocurrencia concreta no exista.

409 Conflict

Cuando:

haya referencias activas o históricas que impidan el borrado
el cierre o corrección rompa reglas temporales
se intente dejar la ocurrencia en un estado inconsistente
No objetivos

Este ADR no introduce todavía:

mantenimiento frontend de rule_entity_type
renombrado masivo de tipos o seeds
versionado complejo de catálogos
soft delete genérico
cascadas automáticas de cleanup
Consecuencias positivas
mantenimiento realista de rule_entity
histórico preservado cuando corresponde
borrado físico posible para errores sin uso
menor riesgo de destruir datos referenciados
frontend con verbos honestos y semánticos
Consecuencias negativas
backend necesita lógica de comprobación de referencias
DELETE deja de ser trivial
algunos casos requerirán decidir si aplicar correct, close o delete
aparece coste de diseño por tipo de catálogo consumidor
Estrategia de implementación
Fase 1

Backend:

GET by business key
PUT correct
POST close
DELETE con comprobación de referencias
Fase 2

Frontend:

abrir detalle/edición de ocurrencia concreta
soportar editar
soportar cerrar
soportar eliminar con confirmación ligera
Fase 3

Refinamiento:

mensajes de conflicto por entidad en uso
posible visibilidad de motivo de bloqueo
tests por tipo consumidor
Resumen

rule_entity no se gestionará como CRUD plano.

Su mantenimiento canónico en B4RRHH será:

create
get by business key
correct
close
delete restringido

DELETE existirá, pero únicamente como operación excepcional y segura, protegida por validación backend de ausencia total de referencias dentro del rule_system.
<!-- END FILE: ADR-013-Mantenimiento-de-rule_entity.md -->


---

# FILE: ADR-014-employee-frontend-ui.md
<a name="file-adr-014-employee-frontend-ui-md"></a>

<!-- BEGIN FILE: ADR-014-employee-frontend-ui.md -->

# ADR — Employee Frontend Section System and Visual Identity

## 1. Objetivo
Definir un sistema visual coherente y reutilizable para la ficha de empleado basado en verticales.

## 2. Principios
- La ficha es composición de verticales
- Consistencia visual transversal
- Backend-driven UI
- No formularios monolíticos

## 3. Unidad base: Section Shell
Componente base que define:
- Header (título + acciones)
- Body (contenido)
- Footer (estado: loading/error/success)

## 4. Maintenance modes → UI
- SLOT → lista editable
- TEMPORAL_APPEND_CLOSE → histórico con activo
- WORKFLOW → acciones guiadas
- READONLY → solo lectura

## 5. Contratos base

### SectionUiState
- mode
- dirty
- busy
- errorMessage
- successMessage

### SectionCapabilities
- canCreate
- canEdit
- canDelete
- canClose
- canCorrect
- canLaunchWorkflow

## 6. Componentes base
- employee-section-shell
- editable-slot-section
- temporal-section

## 7. Tokens visuales
- spacing
- border radius
- colores base

## 8. Reglas Copilot
- No crear componentes genéricos universales
- No mezclar lógica de negocio en UI
- Reutilizar shell y contratos

## 9. Estrategia
1. Implementar shell
2. Aplicar a contacts
3. Reutilizar en identifiers
4. Extender a temporales

<!-- END FILE: ADR-014-employee-frontend-ui.md -->


---

# FILE: ADR-015-Binding-de-catalogos-por-recurso-y-campo.md
<a name="file-adr-015-binding-de-catalogos-por-recurso-y-campo-md"></a>

<!-- BEGIN FILE: ADR-015-Binding-de-catalogos-por-recurso-y-campo.md -->

# ADR-015 — Binding de catálogos por recurso y campo

## Estado
Propuesto

## Contexto
B4RRHH ya dispone de un metamodelo funcional de catálogos en el bounded context `rulesystem`:
- `rule_system`
- `rule_entity_type`
- `rule_entity`

Además:
- ya existen endpoints de `rule_entity` filtrables por business keys;
- ya existe un caso dependiente real para edición: `GET /labor-classification-catalog/agreement-categories`;
- `rule_entity_type` debe nombrar conceptos funcionales reutilizables (por ejemplo `COMPANY`, `WORK_CENTER`, `COST_CENTER`);
- frontend no debe asumir semántica de metamodelo compleja ni convertirse en renderizador genérico de formularios.

El problema de producto actual tiene dos necesidades simultáneas:
1. mostrar labels/literales visibles en frontend, no solo códigos;
2. conocer qué catálogo aplica a un campo concreto de un recurso para pedir opciones válidas por `rule_system`.

## Problema
Hoy, el sistema valida códigos de catálogo en verticales concretos, pero no existe un diccionario backend explícito y reusable que responda de forma simple:
- qué catálogo corresponde a cada campo;
- si ese catálogo se resuelve de forma directa, dependiente o custom.

Sin este diccionario aparecen dos riesgos no deseados:
- construir un "GET de la muerte" que devuelva todas las `rule_entities` para que frontend infiera todo;
- codificar manualmente vertical por vertical y campo por campo en Angular.

## Decisión
Se adopta una solución backend-first, pequeña y evolutiva basada en binding recurso/campo -> catálogo aplicable.

### Decisiones fijadas
1. Introducir la tabla `resource_field_catalog_binding`.
2. Clasificar bindings en `DIRECT`, `DEPENDENT`, `CUSTOM`.
3. El binding define **qué catálogo aplica**, no **cómo renderizar formularios**.
4. Para lectura, preferir read models enriquecidos con `code + name`.
5. Para edición, frontend consulta bindings y consume opciones directas o endpoints específicos según el caso.
6. No introducir en esta fase un motor universal de dependencias.
7. No introducir un endpoint masivo de todas las `rule_entities`.

## Diseño Propuesto
### Persistencia
Tabla: `rulesystem.resource_field_catalog_binding`

Campos:
- `resourceCode`
- `fieldCode`
- `ruleEntityTypeCode`
- `catalogKind` (`DIRECT` | `DEPENDENT` | `CUSTOM`)
- `dependsOnFieldCode`
- `customResolverCode`
- `active`
- `createdAt`
- `updatedAt`

Identidad funcional recomendada:
- `resourceCode + fieldCode`.

Relación recomendada:
- `ruleEntityTypeCode` referencia por business key a `rule_entity_type.code` cuando aplique.

### Reglas de consistencia
- `DIRECT` => `ruleEntityTypeCode` obligatorio y `dependsOnFieldCode` nulo.
- `DEPENDENT` => `ruleEntityTypeCode` obligatorio y `dependsOnFieldCode` obligatorio.
- `CUSTOM` => `customResolverCode` obligatorio.

### Semántica de resolución
- `DIRECT`: opciones por `ruleSystemCode + ruleEntityTypeCode`.
- `DEPENDENT`: opciones por endpoint específico del caso de negocio.
- `CUSTOM`: resolución específica controlada por backend, explícita por `customResolverCode`.

## API Propuesta (primera iteración)
### 1) Consultar bindings de un recurso
`GET /catalog-bindings/{resourceCode}`

Respuesta mínima:
- `resourceCode`
- `bindings[]` con:
  - `fieldCode`
  - `catalogKind`
  - `ruleEntityTypeCode` (nullable)
  - `dependsOnFieldCode` (nullable)
  - `customResolverCode` (nullable)
  - `active`

### 2) Obtener opciones de catálogo directo
`GET /catalog-options/direct?ruleSystemCode=...&ruleEntityTypeCode=...&referenceDate=...&q=...`

Respuesta mínima:
- `items[]` con:
  - `code`
  - `name`
  - `active`
  - `startDate`
  - `endDate`

### 3) Casos dependientes
Mantener endpoints específicos cuando compense (por ejemplo `labor-classification-catalog/agreement-categories`).

## Reglas de Uso
### Lectura
Backend enriquece read models con labels visibles sin delegar inferencias al frontend.

Ejemplos:
- `workCenterCode` + `workCenterName`
- `agreementCode` + `agreementName`
- `agreementCategoryCode` + `agreementCategoryName`

### Edición
Frontend:
1. consulta binding por `resourceCode`;
2. para `DIRECT`, pide opciones directas por `ruleSystemCode`;
3. para `DEPENDENT`/`CUSTOM`, usa endpoint específico del caso.

No se pretende frontend dinámico universal.

## No Objetivos
Este ADR no pretende resolver todavía:
- un form builder genérico;
- un motor universal de dependencias entre campos;
- inferencia automática de UI desde metamodelo completo;
- un endpoint masivo que exponga todas las `rule_entities` para que frontend deduzca semántica;
- relajar reglas de vertical-first o mover lógica de dominio a Angular.

## Consecuencias
### Positivas
- Evita acoplamiento manual campo a campo en frontend.
- Mantiene el control semántico en backend.
- Permite crecimiento incremental por verticales sin arquitectura astronauta.
- Reutiliza metamodelo existente y business keys.

### Costes
- Introduce una tabla más en metamodelo de consumo.
- Requiere gobierno de seeds de bindings.
- Exige disciplina para distinguir `DIRECT` vs `DEPENDENT` vs `CUSTOM`.

## Casos Iniciales
Bindings iniciales a registrar:
- `employee.presence` / `companyCode` -> `COMPANY` (`DIRECT`)
- `employee.work_center` / `workCenterCode` -> `WORK_CENTER` (`DIRECT`)
- `employee.cost_center` / `costCenterCode` -> `COST_CENTER` (`DIRECT`)
- `employee.contact` / `contactTypeCode` -> `EMPLOYEE_CONTACT_TYPE` (`DIRECT`)
- `employee.identifier` / `identifierTypeCode` -> `EMPLOYEE_IDENTIFIER_TYPE` (`DIRECT`)
- `employee.address` / `addressTypeCode` -> `EMPLOYEE_ADDRESS_TYPE` (`DIRECT`)
- `employee.labor_classification` / `agreementCode` -> `AGREEMENT` (`DIRECT`)
- `employee.labor_classification` / `agreementCategoryCode` -> `AGREEMENT_CATEGORY` (`DEPENDENT`, `dependsOnFieldCode=agreementCode`)

## Plan por Fases
### Fase 1
- Crear tabla `resource_field_catalog_binding` con restricciones de consistencia.
- Seed inicial de casos `DIRECT` y caso `DEPENDENT` de labor classification.
- Exponer `GET /catalog-bindings/{resourceCode}`.

### Fase 2
- Exponer `GET /catalog-options/direct`.
- Empezar enriquecimiento `code + name` en read models prioritarios (work center, labor classification).

### Fase 3
- Integrar consumo en frontend para edición guiada por binding.
- Extender gradualmente a más verticales y consolidar endpoints dependientes puntuales.

## Riesgos a Evitar
- Convertir el binding en framework genérico de formularios.
- Duplicar semántica de negocio en frontend.
- Diseñar una API universal compleja antes de validar casos reales.
- Introducir IDs técnicos en contratos públicos.
- Romper vertical-first moviendo reglas de dominio fuera de sus verticales.

<!-- END FILE: ADR-015-Binding-de-catalogos-por-recurso-y-campo.md -->


---

# FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md
<a name="file-adr-016-anatomia-visual-y-patrones-de-interacci-n-de-la-ficha-de-empleado-md"></a>

<!-- BEGIN FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md -->

ADR — Anatomía visual y patrones de interacción de la ficha de empleado
1. Estado

PROPOSED → TARGET: ACCEPTED

2. Contexto

El frontend de B4RRHH ha evolucionado hacia una arquitectura por verticales, con una clara separación entre:

dominio (backend)
contrato (OpenAPI)
frontend desacoplado (Angular)

Las decisiones previas relevantes establecen que:

la ficha de empleado es una composición de secciones autónomas
la edición se rige por maintenance modes (SLOT, TEMPORAL_APPEND_CLOSE, WORKFLOW, READONLY)
el frontend no debe inferir semántica compleja, sino consumirla del backend
las acciones deben ser semánticamente honestas, evitando CRUD genérico

Sin embargo, el estado actual de la UI:

transmite una sensación de “pantalla técnica”
carece de una anatomía visual consolidada
no expresa de forma clara el ciclo de vida del empleado
no diferencia visualmente tipos de información (actual vs histórico vs workflow)

Existe el riesgo de:

aplicar mejoras estéticas locales sin coherencia global
introducir abstracciones genéricas que rompan la semántica del dominio
degradar la experiencia al crecer en verticales
3. Problema

Se requiere definir una arquitectura de experiencia y anatomía visual coherente, que:

exprese correctamente el dominio (lifecycle del empleado)
escale con nuevas verticales
mantenga la semántica de negocio
evite caer en formularios genéricos o UI técnica
4. Decisión
4.1 La ficha como composición estructurada

La ficha de empleado se consolida como:

Una composición de secciones autónomas con una jerarquía visual clara y consistente

Estructura base:

Cabecera de empleado (contexto)
Estado actual
Datos operativos (SLOT)
Datos históricos (TEMPORAL)
Acciones de negocio (WORKFLOW)
Timeline lateral persistente
4.2 Cabecera como componente de producto

Se introduce un componente de cabecera que:

muestra identidad completa del empleado
muestra estado derivado (Activo / Inactivo)
expone contexto actual (empresa, centro, fechas)
incluye contacto básico inline
expone acciones principales dependientes de estado

Regla clave:

La cabecera debe permitir entender el estado del empleado sin navegar ni leer bloques inferiores.

4.3 Contratación como punto de entrada

Se establece que:

la acción primaria del sistema es Nueva contratación
no se expone “crear empleado” como acción independiente

Regla:

La contratación inicial crea simultáneamente la identidad del empleado y su primera relación laboral (presence).

Consecuencia:

el lifecycle se modela en torno a:
contratar
terminar
recontratar
4.4 Lifecycle centrado en presence

Se establece que:

El ciclo de vida del empleado se representa mediante presences, no mediante estados internos del empleado.

La UI:

refleja el estado derivado (activo/inactivo)
no introduce estados artificiales
no separa artificialmente “persona” y “relación” en la experiencia
4.5 Timeline como contexto lateral persistente

Se introduce un componente de timeline con estas reglas:

en escritorio:
aparece como panel lateral derecho persistente
en móvil:
se reubica al final de la ficha

Características:

representa el lifecycle completo
no es una tabla
no compite con el contenido principal
proporciona contexto continuo

Regla:

El timeline es contexto, no contenido principal.

4.6 Separación visual por familias funcionales

Cada tipo de mantenimiento se representa con un patrón visual distinto:

SLOT
datos actuales
lectura limpia
sin tablas
edición localizada
TEMPORAL_APPEND_CLOSE
ocurrencia actual destacada
histórico secundario
acciones: añadir / cerrar / corregir
WORKFLOW
no parece formulario
acciones de negocio explícitas
lenguaje semántico
READONLY
lectura pura
sin affordances engañosas
4.7 Shell común de sección

Todas las secciones comparten un shell visual:

título
acciones
contenido
estado (loading/error/success)

Pero:

La lógica interna no se unifica en un componente genérico.

4.8 UI semánticamente honesta

Se prohíbe el uso de:

“Editar” como verbo universal
acciones técnicas (create/update/delete)

Se obliga a usar:

Añadir
Eliminar
Cerrar
Corregir
Contratar
Terminar
Recontratar
4.9 No uso de form builders genéricos

Se establece explícitamente:

no se implementará un motor genérico de formularios
no se trasladará la semántica de negocio al frontend
5. Anatomía visual objetivo
Layout escritorio
contenido principal (izquierda)
timeline lateral (derecha)
Layout móvil
contenido en flujo
timeline al final
Jerarquía
Cabecera
Estado actual
Datos SLOT
Datos TEMPORAL
Acciones
Timeline
6. Consecuencias
Positivas
sensación de producto profesional
coherencia entre verticales
escalabilidad real
mejor alineación con dominio
reducción de deuda futura
Negativas
refactor inicial de UI existente
mayor disciplina en frontend
necesidad de mantener consistencia
7. Alternativas descartadas
mejoras visuales locales sin blueprint
uso de librerías UI como solución completa
componente universal configurable
formulario único editable
timeline como tabla
8. Plan de implementación
Fase 0 — Consolidación
aprobar ADR
documentar patrones
alinear naming
Fase 1 — Foundation
employee-page-header
employee-section-shell
tokens visuales
badges y estados
Fase 2 — SLOT
consolidar contacto
consolidar identificadores
Fase 3 — TEMPORAL
rediseñar address
introducir patrón histórico
Fase 4 — Layout
implementar layout con timeline lateral
responsive
Fase 5 — Timeline
implementar timeline discreto
integrar presence
Fase 6 — Preparación workflows
preparar patrón workflow
integrar acciones lifecycle
9. Reglas para Copilot (CRÍTICO)

Copilot debe:

respetar la anatomía de sección
no introducir componentes genéricos
no alterar semántica de negocio
usar naming consistente
priorizar claridad sobre reutilización excesiva

Copilot NO debe:

crear form builders
introducir lógica de negocio en frontend
usar “edit” como acción universal
mezclar tipos de mantenimiento
🎯 Resultado esperado

Una UI que:

no parece técnica
no parece CRUD
expresa claramente el dominio
escala sin romperse
puede evolucionar hacia producto completo de RRHH
<!-- END FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md -->


---

# FILE: ADR-017-Cost-center-design.md
<a name="file-adr-017-cost-center-design-md"></a>

<!-- BEGIN FILE: ADR-017-Cost-center-design.md -->

ADR — Employee Cost Center Vertical
Estado

Propuesto

Contexto

B4RRHH modela el dominio de empleado mediante verticales funcionales independientes dentro del bounded context employee, siguiendo arquitectura vertical-first, hexagonal interna y APIs públicas basadas exclusivamente en business keys. El empleado se identifica funcionalmente por:

ruleSystemCode
employeeTypeCode
employeeNumber

y los recursos hijos deben derivar su identidad desde esa business key, sin exponer IDs técnicos en la API pública.

En la evolución del mapa de verticales del proyecto, cost_center ya aparece identificado como una vertical de tipo:

DISTRIBUTED_TIMELINE
catálogo simple
reglas temporales: MULTI_ACTIVE + SUM<=100

y pendiente todavía de aterrizar operativamente.

Además, el lenguaje temporal común del proyecto distingue claramente entre:

STRONG_TIMELINE
FLEXIBLE_TIMELINE
DISTRIBUTED_TIMELINE

reservando para esta última los casos multi-activos y con reglas agregadas, típicamente basadas en porcentajes. El propio patrón de StrongTimelineReplacePlanner indica expresamente que no debe aplicarse directamente a cost_center, porque esa vertical no es single-active ni de cobertura completa.

También existe ya una decisión explícita de naming de catálogos reutilizables: cuando el concepto es transversal o reusable, el rule_entity_type debe nombrar el concepto funcional real. COST_CENTER aparece como ejemplo claro de catálogo reusable y además ya está previsto como binding directo para employee.cost_center / costCenterCode.

Por último, el dominio de lifecycle ya establece que TERMINATION debe cerrar o ajustar asignaciones vigentes del empleado, preservando el histórico y sin dejar restos abiertos tras la terminación.

Problema

employee.cost_center no encaja correctamente ni como:

CRUD plano por filas
ni como simple clon de work_center
ni como STRONG_TIMELINE

porque su semántica real no es “una única asignación activa”, sino una distribución organizativa que puede tener varias líneas simultáneas activas para una misma fecha.

Ejemplo funcional válido:

50% en CC_A
50% en CC_B

vigentes a la vez desde la misma fecha.

Esto introduce necesidades específicas que no aparecen en verticales single-active:

permitir multi-actividad simultánea;
impedir que la suma de porcentajes supere 100 en un momento dado;
evitar mezclas incoherentes de líneas paralelas con fechas de inicio distintas;
definir una unidad funcional de cambio más fuerte que “una fila aislada”;
aclarar cómo impacta TERMINATION;
definir operaciones canónicas honestas, evitando un CRUD fila a fila que rompa la consistencia agregada.
Decisión

Se adopta para employee.cost_center un modelo de vertical de tipo DISTRIBUTED_TIMELINE, cuyo elemento funcional real no es una fila aislada, sino una ventana de distribución de centros de coste para un empleado.

La vertical:

será historizada;
permitirá múltiples líneas activas simultáneamente;
validará catálogo COST_CENTER;
exigirá contención dentro de una presence;
impondrá que la suma de porcentajes activos no supere 100;
y, cuando exista más de una línea paralela activa, exigirá que todas compartan la misma startDate.

La unidad funcional de cambio será la distribución vigente desde una fecha, no la edición arbitraria de una línea individual.

Definición funcional

employee.cost_center representa la distribución de imputación organizativa de un empleado entre uno o varios centros de coste, con vigencia temporal.

No modela simplemente “una asignación más”, sino el reparto funcional del empleado entre centros de coste para un periodo dado.

Ejemplos válidos:

100% CC_FINANCE
60% CC_IT + 40% CC_SHARED
50% CC_OPS + 50% CC_TRANSFORMATION

Ejemplos inválidos:

80% CC_A + 30% CC_B
50% CC_A desde 01/04 y 50% CC_B desde 15/04, coexistiendo en la misma vigencia
una línea fuera de presence activa
porcentajes 0 o negativos
Tipo de vertical

Clasificación formal:

bounded context: employee
vertical: cost_center
tipo: DISTRIBUTED_TIMELINE
catálogo: SIMPLE
reglas temporales:
MULTI_ACTIVE
SUM_PERCENTAGE_LTE_100
CONTAINED_IN_PRESENCE
PARALLEL_WINDOW_SAME_START_DATE

Esto consolida la vertical en el cluster distribuido del proyecto y evita tratarla como una variación accidental de work_center.

Identidad funcional
Identidad del empleado
ruleSystemCode
employeeTypeCode
employeeNumber
Identidad del recurso

A nivel de API pública de escritura, la unidad funcional relevante será una ventana de distribución identificada por:

empleado
startDate

Es decir, conceptualmente:

ruleSystemCode
employeeTypeCode
employeeNumber
startDate
Nota importante

Internamente, la persistencia puede seguir usando filas individuales con:

id técnico
costCenterAssignmentNumber técnico/funcional interno

pero esos identificadores no definen la identidad pública canónica del recurso.

La razón es funcional: cuando varias líneas comparten una misma distribución vigente, el dominio no las trata como historias autónomas, sino como partes de una única ventana de distribución.

Ventana de distribución

Se introduce el concepto explícito de:

Cost Center Distribution Window

Una ventana de distribución es el conjunto de líneas de cost_center que:

pertenecen al mismo empleado;
comparten la misma startDate;
forman una distribución activa o histórica coherente;
y se validan conjuntamente como una única unidad funcional.

Consecuencias:

crear una distribución = crear una ventana;
sustituir una distribución = cerrar ventana anterior y crear una nueva;
cerrar una distribución = cerrar todas las líneas de la ventana;
TERMINATION actúa sobre la ventana activa completa, no sobre una línea suelta.
Propiedades estructurales
Propiedad	Valor
historized	true
occurrence_type	MULTIPLE
simultaneous_occurrences	MULTIPLE_ACTIVE
lifecycle_strategy	CLOSE
delete_policy	FORBIDDEN
maintenance_style	DISTRIBUTED_WINDOW
Campos funcionales de línea

Cada línea de distribución contiene:

costCenterCode
allocationPercentage
startDate
endDate

Campos enriquecidos de lectura:

costCenterName
isCurrent

Campos derivados de la ventana:

windowStartDate
windowEndDate
totalAllocationPercentage
Validación de catálogo

costCenterCode debe validarse contra rulesystem.rule_entity usando:

ruleEntityTypeCode = COST_CENTER

Esto se alinea con la convención de naming de catálogos reutilizables y con el binding ya previsto para:

employee.cost_center / costCenterCode -> COST_CENTER (DIRECT)

La validación debe comprobar:

ruleSystemCode correcto
existencia del código
activo
vigencia temporal aplicable
Reglas de dominio
1. Contención en presence

Toda línea de cost_center debe estar completamente contenida en una presence válida del empleado.

No se permiten líneas:

antes del inicio de la presence que las contiene;
después del final de la presence;
ni abiertas más allá de una presence cerrada.
2. Multi-actividad permitida

Puede haber múltiples líneas activas simultáneamente para una misma fecha, siempre que pertenezcan a la misma ventana de distribución.

3. Suma máxima de porcentaje

Para cualquier fecha dada, la suma de allocationPercentage de todas las líneas activas del empleado no puede superar 100.

Regla:

permitido: total < 100
permitido: total = 100
prohibido: total > 100
4. Misma fecha de inicio en paralelo

Si existe más de una línea activa simultáneamente dentro de una misma distribución, todas deben compartir exactamente la misma startDate.

Ejemplo válido:

CC_A 50% desde 2026-04-01
CC_B 50% desde 2026-04-01

Ejemplo inválido:

CC_A 50% desde 2026-04-01
CC_B 50% desde 2026-04-15
5. No mezcla incoherente de ventanas activas

No puede coexistir, para una misma fecha, una línea activa perteneciente a una ventana de distribución distinta.

Dicho de otro modo: para un empleado, la distribución activa en una fecha debe ser interpretable como una única ventana funcional.

6. Porcentaje válido por línea

allocationPercentage debe cumplir:

> 0
<= 100
7. Integridad temporal

endDate no puede ser anterior a startDate.

8. Sin edición arbitraria de identidad

No se permite mutar, en una corrección administrativa, los campos que redefinen funcionalmente una línea histórica de forma que rompan la semántica de la ventana.

Operaciones canónicas

No se adopta un CRUD plano por fila.

Las operaciones canónicas del vertical serán orientadas a ventana.

1. Crear distribución

Crea una nueva ventana de distribución desde una fecha.

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/distributions

Request
startDate
items[]
costCenterCode
allocationPercentage
2. Consultar histórico de distribuciones

GET /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers

3. Consultar distribución vigente

GET /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/current

4. Sustituir distribución desde fecha

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/replace-from-date

Request
effectiveDate
items[]
costCenterCode
allocationPercentage

Semántica:

cerrar la ventana activa previa si cubre la fecha;
crear nueva ventana desde effectiveDate;
validar projected timeline distribuida.
5. Cerrar distribución

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/distributions/{startDate}/close

Request
endDate

Semántica:

cerrar todas las líneas de la ventana identificada por startDate.
Operaciones descartadas

Se rechazan como canónicas:

PUT por línea aislada
DELETE físico por línea
edición arbitraria de una fila dentro de una distribución multi-línea activa
endpoints públicos por id
endpoints públicos por costCenterAssignmentNumber

porque esas operaciones empujan el modelo hacia una semántica de filas independientes que no representa bien el dominio.

Relación con replace patterns

employee.cost_center puede necesitar una operación funcional tipo replaceFromDate, pero no debe reutilizar directamente el StrongTimelineReplacePlanner, ya que este patrón está reservado a verticales STRONG_TIMELINE con:

single active
no overlap
full coverage

y cost_center es explícitamente un caso distinto.

Si aparece lógica temporal repetida, podrá introducirse en el futuro un helper ligero específico para distribuciones, por ejemplo:

CostCenterDistributionProjector
DistributedTimelineWindowPlanner

pero no debe crearse todavía un framework genérico ni un motor abstracto.

Relación con TERMINATION

TERMINATION debe cerrar la distribución activa completa del empleado.

Regla funcional:

al terminar un empleado en terminationDate,
se identifican todas las líneas activas de cost_center en esa fecha,
y todas deben quedar cerradas con esa fecha de fin.

Consecuencias:

no puede quedar ninguna línea abierta después de la terminación;
no se redistribuyen porcentajes;
no se corrigen líneas;
no se fuerza una suma distinta;
simplemente se cierra la ventana activa.

Si no existe distribución activa en la fecha de terminación, no se considera error funcional por sí mismo.

Esto encaja con la semántica general de lifecycle workflows y con la necesidad de cerrar asignaciones vigentes sin dejar residuos abiertos.

Relación con Journey

En Journey / timeline, un cambio de cost_center debe interpretarse como evento funcional por ventana de distribución, no como cascada de eventos aislados por cada fila técnica.

Evento esperado:

COST_CENTER_CHANGE

La interpretación debe hacerse en backend y no delegarse al frontend.

Persistencia

Persistencia recomendada: tabla por líneas.

Ejemplo conceptual:

employee.cost_center

Campos:

id
employee_id
cost_center_assignment_number
cost_center_code
allocation_percentage
start_date
end_date
created_at
updated_at
Restricciones mínimas de base de datos
PK técnica por id
unique (employee_id, cost_center_assignment_number)
check allocation_percentage > 0 and allocation_percentage <= 100
check end_date is null or start_date < end_date
Nota

Las reglas:

suma <= 100
misma startDate en multi-activo
una única ventana funcional activa por fecha

deben validarse en dominio / servicio de aplicación, no intentarse imponer únicamente con constraints SQL.

API y OpenAPI

La API pública debe seguir las reglas generales del proyecto:

business keys del empleado
sin IDs técnicos
sin mezclar parent por business key e hijo por id técnico

La OpenAPI debe reflejar:

operaciones por ventana
requests con items[]
DTOs claros y honestos
sin introducir update DTOs que permitan mutar identidad funcional como si fuera CRUD genérico.
Read models recomendados

La lectura debe exponer labels enriquecidas:

costCenterCode
costCenterName

y agrupar claramente la ventana actual e histórico.

Ejemplo conceptual de respuesta:

{
  "employee": {
    "ruleSystemCode": "ESP",
    "employeeTypeCode": "EMP",
    "employeeNumber": "000123"
  },
  "currentDistribution": {
    "startDate": "2026-04-01",
    "endDate": null,
    "totalAllocationPercentage": 100,
    "items": [
      {
        "costCenterCode": "CC_A",
        "costCenterName": "Administración",
        "allocationPercentage": 50
      },
      {
        "costCenterCode": "CC_B",
        "costCenterName": "Transformación",
        "allocationPercentage": 50
      }
    ]
  }
}

Esto se alinea con la estrategia general del proyecto de enriquecer lectura con code + name y no obligar al frontend a reconstruir semántica desde catálogos masivos.

Frontend

Esta vertical no debe tratarse como:

SLOT
ni tabla CRUD genérica

La semántica de UI recomendada es una sección temporal/distribuida con:

distribución actual destacada
histórico de distribuciones
acciones honestas:
Añadir distribución
Sustituir distribución desde fecha
Cerrar distribución

No deben usarse como acciones primarias:

editar fila
borrar fila
update técnico
grid CRUD

Esto es coherente con el principio general de UX honesta y con la prohibición de usar “Editar” como verbo universal cuando la semántica real es otra.

Consecuencias positivas
mejor alineación con el dominio real;
evita modelado accidental por filas;
simplifica TERMINATION;
facilita Journey semántico;
hace más clara la UI;
prepara futuras abstracciones de distributed timeline;
mantiene consistencia con el mapa y lenguaje ya definidos en B4RRHH.
Costes / riesgos
requiere lógica de validación agregada, no trivial;
introduce una noción nueva de ventana funcional;
obliga a resistir la tentación de implementar CRUD simple por línea;
puede requerir helper técnico específico en el futuro si aparecen más verticales distribuidas;
la corrección administrativa de histórico deberá definirse con cuidado si algún día se habilita.
Alternativas consideradas
1. Modelarlo como clon de work_center

Descartado.

work_center es una asignación flexible historizada, pero cost_center tiene una semántica distribuida basada en porcentaje y paralelismo.

2. Modelarlo como CRUD por fila

Descartado.

Rompe la unidad funcional de distribución, genera estados incoherentes y hace más difícil validar suma, ventanas y termination.

3. Tratarlo como STRONG_TIMELINE

Descartado.

No es single-active y el planner de strong timeline no aplica directamente.

Resumen

employee.cost_center se define en B4RRHH como una vertical DISTRIBUTED_TIMELINE que modela la distribución temporal del empleado entre uno o varios centros de coste.

La unidad funcional de mantenimiento no será una fila aislada, sino una ventana de distribución identificada por empleado + startDate.

Reglas clave:

multi-activo permitido;
suma activa <= 100;
contención en presence;
líneas paralelas con la misma startDate;
cierre completo en TERMINATION;
catálogo COST_CENTER;
operaciones canónicas orientadas a crear, sustituir y cerrar distribuciones
<!-- END FILE: ADR-017-Cost-center-design.md -->


---

# FILE: ADR-018-hiring-an-employee.md
<a name="file-adr-018-hiring-an-employee-md"></a>

<!-- BEGIN FILE: ADR-018-hiring-an-employee.md -->

ADR — Employee Lifecycle Workflow: Hire Employee V1
Estado

Propuesto

Contexto

B4RRHH modela el empleado mediante una arquitectura basada en verticales independientes (presence, work_center, cost_center, contract, etc.), todas ellas accesibles mediante APIs por business key:

ruleSystemCode
employeeTypeCode
employeeNumber

Sin embargo, el ciclo de vida del empleado no se corresponde con la manipulación aislada de estas verticales, sino con acciones de negocio compuestas como:

contratar
terminar
recontratar

Estas acciones implican:

creación coordinada de múltiples verticales
coherencia temporal
validaciones transversales
una semántica funcional única

El ADR de lifecycle ya establece que estas acciones deben modelarse como workflows de negocio, no como secuencias de operaciones CRUD.

Además, la arquitectura de frontend define que estas acciones deben exponerse como WORKFLOW, no como edición genérica de datos.

Problema

Actualmente el sistema permite:

crear employee
crear presence
crear asignaciones organizativas
etc.

pero no existe una operación unificada de contratación.

Esto implica:

mala UX (el usuario tiene que ensamblar el empleado manualmente)
riesgo de inconsistencias temporales
pérdida de semántica de negocio
dificultad para evolucionar el lifecycle
Decisión

Se introduce el workflow:

Hire Employee V1

como una operación de negocio compuesta que:

crea el empleado
inicializa su relación laboral
establece su contexto organizativo inicial
garantiza coherencia temporal completa

Todo ello en una única operación orquestada.

Principios de diseño
1. Orientado a intención de negocio

El usuario no crea recursos técnicos.

El usuario ejecuta:

“Contratar empleado”

2. Presence como eje del lifecycle

El lifecycle del empleado se representa mediante presence.

Por tanto:

el Hire crea la primera presence
sin presence no hay relación laboral
3. Fecha central única

Se define:

hireDate es la fecha efectiva central del workflow

Regla:

Todas las entidades creadas deben compartir coherencia temporal:

presence.startDate = hireDate
work_center.startDate = hireDate
cost_center.startDate = hireDate
contract.startDate = hireDate
labor_classification.startDate = hireDate

No se permiten fechas divergentes en V1.

4. Orquestación única

El workflow:

ejecuta múltiples operaciones internas
se expone como una única operación externa
garantiza consistencia funcional
5. Sin exposición de IDs técnicos

La API:

usa exclusivamente business keys
no expone IDs internos
no mezcla identidades técnicas
6. Backend interpreta la semántica

El backend:

decide qué significa “HIRE”
construye el estado resultante
prepara los datos para UI

El frontend no deduce semántica compleja.

Alcance V1
Incluido

El workflow crea:

Employee core
Primera presence
Asignación organizativa inicial:
work center
cost center (opcional en V1)
Relación laboral inicial:
contract
labor classification
No incluido en V1
contactos
direcciones
identificadores
correcciones avanzadas
escenarios multi-fecha
edición parcial del workflow
API
Endpoint

Opción recomendada:

POST /employee-lifecycle/hire

Alternativa válida:

POST /employees/hire
Request

Ejemplo:

{
  "ruleSystemCode": "ESP",
  "employeeTypeCode": "EMP",
  "employeeNumber": "000123",

  "firstName": "Juan",
  "lastName1": "Pérez",
  "lastName2": "García",
  "preferredName": "Juan",

  "hireDate": "2026-04-01",
  "entryReasonCode": "HIRING",
  "companyCode": "COMP01",

  "workCenterCode": "WC01",

  "costCenterDistribution": {
    "items": [
      {
        "costCenterCode": "CC01",
        "allocationPercentage": 100
      }
    ]
  },

  "contract": {
    "contractTypeCode": "FULL",
    "contractSubtypeCode": "STD"
  },

  "laborClassification": {
    "agreementCode": "AGR01",
    "agreementCategoryCode": "CAT01"
  }
}
Response

Debe devolver un estado agregado listo para UI:

{
  "employee": {
    "ruleSystemCode": "ESP",
    "employeeTypeCode": "EMP",
    "employeeNumber": "000123",
    "displayName": "Juan Pérez García",
    "status": "ACTIVE"
  },
  "presence": {
    "startDate": "2026-04-01",
    "companyCode": "COMP01"
  },
  "workCenter": {
    "workCenterCode": "WC01"
  },
  "costCenter": {
    "startDate": "2026-04-01",
    "items": [...]
  },
  "contract": {...},
  "laborClassification": {...}
}
Validaciones
1. Employee
no debe existir previamente
si existe → 409 Conflict
2. Catálogos

Validar:

companyCode
workCenterCode
costCenterCode
entryReasonCode
contractType/subtype
agreement/category
3. Relaciones dependientes
agreementCategory depende de agreement
contractSubtype depende de contractType
etc.
4. Presence
debe crearse correctamente
no puede haber otra presence activa
5. Cost Center
suma <= 100
misma startDate
catálogo válido
contenido en presence
6. Coherencia temporal
todas las entidades deben respetar hireDate
no se permiten offsets en V1
Orquestación interna

Orden recomendado:

validar request
validar catálogos
validar dependencias
crear employee
crear presence
crear work center
crear cost center (si viene)
crear contract
crear labor classification
construir response

Todo dentro de un único servicio de aplicación.

Relación con TERMINATION

Este workflow deja al empleado en estado:

presence activa
asignaciones activas

Lo que permite que:

TERMINATION cierre correctamente todas las verticales activas

Sin estados intermedios incoherentes.

Relación con REHIRE

Diferencias clave:

Aspecto	Hire	Rehire
Employee	se crea	ya existe
Presence	primera	nueva
Histórico	vacío	preservado
Relación con Journey

El Hire debe generar un evento:

HIRE

El backend es responsable de esta interpretación.

El frontend no debe inferirlo.

Frontend

El workflow:

se expone como acción principal: “Contratar”
se implementa como pantalla dedicada
no como modal simple
no como formulario genérico

Patrón:

WORKFLOW
no SLOT
no TEMPORAL_APPEND_CLOSE
Consecuencias positivas
UX alineada con negocio
consistencia temporal garantizada
reducción de errores
base sólida para lifecycle completo
integración natural con Journey
Costes
mayor complejidad en capa application
necesidad de validaciones transversales
mayor esfuerzo inicial
Alternativas descartadas
CRUD por vertical

Descartado:

no refleja dominio
propenso a inconsistencias
Employee como agregado monolítico

Descartado:

rompe arquitectura vertical
reduce flexibilidad
Workflow parcial

Descartado:

deja estados intermedios inválidos
Resumen

Hire Employee V1 introduce una operación de negocio compuesta que:

crea el empleado
establece su relación laboral
define su contexto organizativo inicial
garantiza coherencia temporal

Todo ello en una única operación orquestada, alineada con el modelo de verticales y con el ciclo de vida real del empleado.
<!-- END FILE: ADR-018-hiring-an-employee.md -->


---

# FILE: ADR-019-employee-delete-administrativo.md
<a name="file-adr-019-employee-delete-administrativo-md"></a>

<!-- BEGIN FILE: ADR-019-employee-delete-administrativo.md -->

# ADR-019 — Borrado administrativo de employee con cascada técnica controlada

## Estado
Propuesto

## Contexto

B4RRHH modela el dominio de empleado mediante:

- identidad pública por business key:
  - ruleSystemCode
  - employeeTypeCode
  - employeeNumber
- verticales hijas funcionales del empleado
- lifecycle ordinario basado en:
  - hire
  - terminate
  - rehire

Además, el proyecto distingue entre:

- operaciones funcionales normales del ciclo de vida
- operaciones administrativas o técnicas excepcionales

Hasta ahora, el modelo conceptual del recurso `employee.employee` no se ha orientado al borrado como operación canónica de negocio, sino a conservación de identidad e histórico. Sin embargo, existen escenarios legítimos donde un borrado administrativo sí tiene sentido, por ejemplo:

- alta creada por error
- empleado que finalmente no llega a incorporarse
- datos de prueba o limpieza controlada de entornos no productivos
- reversión temprana de una contratación todavía sin efectos descendentes relevantes

Al mismo tiempo, no se quiere permitir un borrado indiscriminado ni delegar toda la semántica del delete a la base de datos.

Se necesita una decisión explícita sobre:

- existencia o no de endpoint de borrado
- naturaleza funcional de ese borrado
- relación entre validación de aplicación y cascada física en persistencia
- preparación del modelo para futuras restricciones de elegibilidad

## Decisión

Se introduce una operación explícita de **borrado administrativo de employee**.

### Naturaleza de la operación

El borrado de employee:

- **no forma parte del lifecycle ordinario**
- **no sustituye a terminate**
- **no representa un flujo funcional normal**
- se modela como una **operación administrativa excepcional**

### Identidad del endpoint

La operación se expone por business key del empleado:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`

### Regla inicial de elegibilidad (V1)

En esta primera versión, el borrado se permitirá únicamente cuando:

- el empleado exista

Si el empleado no existe:

- la operación devolverá `404 Not Found`

### Reglas futuras explícitamente previstas

Aunque en V1 no se aplican bloqueos funcionales adicionales, esta operación queda diseñada para soportar en el futuro validaciones como:

- no permitir borrado si el empleado tiene nómina calculada
- no permitir borrado si existen efectos descendentes relevantes
- no permitir borrado si el empleado ya superó cierto punto funcional del ciclo de vida
- otras reglas de elegibilidad administrativa

Si en el futuro una regla impide el borrado:

- la operación deberá devolver `409 Conflict`

## Persistencia

Cuando el borrado sea autorizado por la capa de aplicación, la eliminación física del empleado podrá apoyarse en **cascada técnica de base de datos** sobre las verticales hijas dependientes por `employee_id`.

Principio:

- la **aplicación decide si se puede borrar**
- la **base de datos ejecuta el borrado relacional completo**

La cascada en base de datos se considera una decisión de persistencia y consistencia técnica, no una definición de semántica de negocio.

## API propuesta

```text
DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}
<!-- END FILE: ADR-019-employee-delete-administrativo.md -->


---

# FILE: ADR-020-work-center-replace-from-date.md
<a name="file-adr-020-work-center-replace-from-date-md"></a>

<!-- BEGIN FILE: ADR-020-work-center-replace-from-date.md -->

# ADR-020 — Cambio canónico de work center mediante replace-from-date

## Estado
Propuesto

## Contexto

B4RRHH modela sus recursos de empleado mediante verticales funcionales historizadas y APIs públicas basadas en business keys del empleado:

- ruleSystemCode
- employeeTypeCode
- employeeNumber

Dentro del mapa actual de verticales, `employee.work_center` se clasifica como una vertical temporal con restricciones de:

- no solape
- contención dentro de presence
- una única asignación vigente compatible en cada fecha

Aunque inicialmente puede existir una operación canónica de creación de work center, la experiencia real de uso ha demostrado que el cambio funcional habitual de centro de trabajo no puede modelarse de forma segura como un simple `create` aislado cuando ya existe una asignación abierta.

En un escenario real, si un empleado ya tiene un work center vigente y se desea cambiarlo con fecha efectiva X:

- no puede abrirse uno nuevo en X dejando el anterior abierto
- el anterior debe cerrarse en X - 1
- el nuevo debe comenzar en X

Por tanto, la operación funcional real no es “añadir otra fila”, sino **sustituir la ventana vigente desde una fecha**.

Esta necesidad se ha hecho visible especialmente al ejecutar simulación masiva con `workforce_loader`, donde la operación de creación directa genera conflictos funcionales que no aparecían en pruebas pequeñas.

## Problema

Usar únicamente una operación de creación para representar un cambio de work center provoca varios riesgos:

- solapes temporales
- necesidad de que el consumidor implemente lógica de cierre previa
- duplicación de semántica de dominio fuera del backend
- inconsistencias entre consumidores (frontend, loader, workflows)

Esto es contrario a la estrategia del proyecto, donde:

- el backend debe exponer operaciones canónicas de negocio
- el consumidor no debe reconstruir reglas temporales complejas por su cuenta

## Decisión

Se introduce una operación canónica para `employee.work_center` orientada a cambio funcional por fecha efectiva:

## `replace-from-date`

Su semántica será:

1. localizar la asignación de work center vigente en la fecha efectiva, si existe;
2. cerrarla en `effectiveDate - 1`;
3. crear la nueva asignación desde `effectiveDate`;
4. validar no solape, contención en presence y coherencia temporal completa.

## Naturaleza de la operación

`replace-from-date`:

- no es un CRUD genérico
- no sustituye a la operación de creación inicial
- representa el cambio funcional habitual de centro de trabajo

## API propuesta

```text
POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/work-centers/replace-from-date
```

Request mínima orientativa
{
  "effectiveDate": "2026-04-01",
  "workCenterCode": "WC02"
}
Reglas de negocio
1. Contención en presence

La nueva asignación debe estar contenida dentro de una presence válida del empleado.

2. No solape

No puede quedar más de una asignación de work center incompatible en la misma fecha.

3. Cierre implícito de la vigente

Si existe una asignación vigente en effectiveDate, debe cerrarse en effectiveDate - 1.

4. Creación de nueva asignación

La nueva asignación comienza en effectiveDate.

5. Operación idempotente semántica no requerida

No se exige idempotencia funcional estricta en V1, pero sí una validación clara de conflictos.

Consecuencias
Positivas
expresa la semántica real del cambio de centro
evita que frontend o loader implementen lógica temporal propia
mantiene la coherencia con otras operaciones temporales del sistema
facilita simulación masiva y workflows futuros
Negativas
añade una operación específica más al vertical
obliga a definir claramente el comportamiento cuando no existe work center vigente
Decisión de alcance para V1

En V1:

si existe work center vigente en la fecha efectiva, se cierra y se crea el nuevo
si no existe vigente, la operación podrá crear directamente la nueva asignación siempre que el contexto temporal sea válido
no se introducen todavía estrategias avanzadas de corrección administrativa
Relación con otras verticales

Esta decisión acerca work_center a un patrón de sustitución temporal por fecha efectiva, aunque sin convertirlo automáticamente en STRONG_TIMELINE.

No se afirma que work_center y contract sean idénticos como verticales, pero sí que ambos requieren una operación canónica de sustitución temporal cuando el cambio funcional afecta a una asignación vigente.

<!-- END FILE: ADR-020-work-center-replace-from-date.md -->


---

# FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md
<a name="file-adr-021-company-como-catalogo-enriquecido-y-anclado-a-rule-entity-md"></a>

<!-- BEGIN FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md -->

ADR — COMPANY como catálogo reutilizable enriquecido mediante profile y anclado técnicamente a rule_entity
Estado

Propuesto

Contexto

B4RRHH dispone de un metamodelo funcional en el bounded context rulesystem basado en:

rule_system
rule_entity_type
rule_entity

Este metamodelo ya se utiliza como base de validación y parametrización de múltiples verticales del sistema.

Además, el proyecto ya ha fijado varias decisiones relevantes:

1. Los conceptos reutilizables deben nombrarse por su significado funcional real

Se ha decidido que un rule_entity_type debe nombrar el concepto funcional real y no la primera vertical donde apareció. En ese marco, COMPANY es un ejemplo explícito de catálogo reutilizable de dominio, junto con WORK_CENTER, COST_CENTER o COUNTRY.

2. rule_entity no debe tratarse como un CRUD plano

El mantenimiento de rule_entity ya se ha definido como catálogo con vigencia temporal ligera, con identidad funcional basada en:

ruleSystemCode
ruleEntityTypeCode
code
startDate

y con operaciones canónicas de create, get by business key, correct, close y delete restringido.

3. Las APIs públicas del proyecto deben usar business keys, mientras que los IDs técnicos quedan encapsulados en persistencia

Esta regla ya está consolidada en el proyecto y se aplica de forma clara en el modelo de empleado: la identidad pública es funcional, mientras que la persistencia usa claves técnicas para FKs, joins y wiring interno.

4. En el dominio de empleado, el patrón canónico distingue entre identidad pública y persistencia técnica

Por ejemplo, employee.contact se identifica públicamente por employee + contactTypeCode, mientras que internamente la tabla usa id técnico y FK a employee.employee.id. Ese id técnico no define la identidad funcional del recurso, pero sí su anclaje persistente.

Problema

COMPANY nace correctamente como un catálogo reutilizable del metamodelo. Sin embargo, al evolucionar el producto aparece una necesidad real: una empresa no solo necesita:

código
literal visible
vigencia

sino también una ficha ampliada con datos ricos, por ejemplo:

nombre legal
identificador fiscal
dirección

Esto genera una tensión de diseño.

Si COMPANY se mantiene exclusivamente como rule_entity

El modelo queda demasiado pobre para soportar información empresarial básica.

Si COMPANY se promociona inmediatamente a una nueva vertical/autonomía completa

Se corre el riesgo de introducir complejidad prematura y de abrir una familia entera de subdominios (organization.company, organization.work_center, organization.cost_center, etc.) antes de que exista una necesidad operativa clara.

Si se modela la ampliación rica solo con business keys y sin anclaje técnico interno

Se introduciría una excepción innecesaria respecto a la filosofía ya consolidada en el proyecto, que separa:

identidad pública funcional
identidad interna/persistente técnica

El sistema necesita una solución intermedia, evolutiva y coherente con las decisiones ya tomadas.

Decisión

Se adopta para COMPANY el siguiente modelo:

1. COMPANY seguirá siendo un catálogo reutilizable del metamodelo

COMPANY se mantiene como rule_entity_type reutilizable y sus ocurrencias continúan viviendo en rulesystem.rule_entity.

Su responsabilidad sigue siendo:

identidad catalogal funcional
código reutilizable
label visible
vigencia
activación

Esto preserva el papel de COMPANY como concepto reusable en múltiples verticales y workflows.

2. La ficha ampliada de empresa no se modelará dentro de rule_entity

Los datos ricos de empresa no se introducirán como extensión ad hoc de rule_entity.

Se crea un recurso complementario específico para la ampliación rica de la empresa.

Nombre conceptual adoptado:

company_profile

Su responsabilidad es representar la ficha ampliada de una empresa sin alterar la naturaleza catalogal base de rule_entity.

3. company_profile se anclará técnicamente a rule_entity.id

La relación interna se resuelve mediante FK técnica a la ocurrencia base de rule_entity de tipo COMPANY.

Es decir:

la identidad pública seguirá usando business keys
la persistencia interna usará un anclaje técnico estable
Regla adoptada

company_profile referencia internamente a la empresa base mediante:

company_rule_entity_id → FK a rulesystem.rule_entity.id

Esto sigue la misma filosofía ya utilizada en el modelo de empleado:

business key fuera
FK técnica dentro
4. Alcance funcional V1 de company_profile

Para la primera iteración, company_profile solo cubrirá:

legalName
taxIdentifier
dirección

La dirección podrá modelarse inicialmente como campos simples embebidos en el profile.

No se incluyen todavía en V1:

numeración de empleados
teléfonos
emails
contactos por tipo
políticas avanzadas
subverticales de company
5. Teléfono y email quedan explícitamente fuera de V1

Aunque podrían modelarse como columnas simples, se decide no hacerlo en esta fase.

Justificación:

el proyecto ya ha consolidado en employee.contact un patrón semántico claro para canales de contacto: slot por tipo, validación por catálogo y separación respecto a la ficha base del sujeto.
introducir phone y email como dos campos planos en company_profile sería una simplificación aceptable a muy corto plazo, pero introduciría una asimetría conceptual innecesaria.
se prefiere aplazar esta decisión hasta que exista necesidad real de contacto empresarial, momento en el cual podrá evaluarse si procede una solución equivalente a contactos por tipo.
6. No se crea todavía una nueva vertical/autonomía completa de organización

Esta decisión no introduce todavía:

bounded context organization
vertical completa organization.company
subverticales como organization.company.contact, organization.company.address, organization.company.numbering_policy

La decisión actual se limita a:

mantener COMPANY como catálogo reutilizable
permitir una ampliación rica controlada mediante company_profile
Diseño funcional adoptado
Naturaleza de COMPANY

COMPANY pasa a entenderse como un concepto de dos capas:

A. Capa catalogal canónica

Representada por rulesystem.rule_entity

Responsabilidad:

identidad reusable
code
name
vigencia
activación
B. Capa de profile enriquecido

Representada por company_profile

Responsabilidad:

ficha ampliada
datos operativos básicos
evolución gradual sin contaminar el metamodelo
Identidad
Identidad pública de la empresa

La identidad pública funcional sigue siendo:

ruleSystemCode
companyCode

donde:

companyCode es el rule_entity.code
ruleEntityTypeCode = COMPANY
Identidad interna de persistencia

La persistencia interna se apoya en:

rulesystem.rule_entity.id como root técnico base del concepto catalogal
company_profile.id como PK técnica propia del profile
company_profile.company_rule_entity_id como FK técnica única hacia rule_entity.id
Persistencia recomendada
Tabla base existente

rulesystem.rule_entity

Con ocurrencias de:

ruleEntityTypeCode = COMPANY
Nueva tabla propuesta

company_profile

Campos iniciales recomendados:

id
company_rule_entity_id
legal_name
tax_identifier
street
city
postal_code
region_code
country_code
created_at
updated_at
Restricciones recomendadas
PK técnica en company_profile.id
FK obligatoria:
company_rule_entity_id -> rulesystem.rule_entity.id
unique:
company_rule_entity_id
validación de que la rule_entity referenciada sea de tipo COMPANY
API pública
Principio general

Las APIs públicas siguen usando business keys, nunca IDs técnicos. Esto mantiene coherencia con la convención general del proyecto.

API de catálogo base

Se mantiene el mantenimiento canónico de rule_entity ya definido:

POST /rule-entities
GET /rule-entities
GET /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
PUT /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
POST /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}/close
DELETE /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
API de profile enriquecido

Se introduce una API específica orientada a la ficha ampliada de empresa.

Endpoints recomendados:

GET /companies/{ruleSystemCode}/{companyCode}/profile
PUT /companies/{ruleSystemCode}/{companyCode}/profile

Opcionalmente, si compensa por ergonomía:

GET /companies/{ruleSystemCode}/{companyCode}

como endpoint agregado de lectura enriquecida.

Reglas de dominio
1. Separación de responsabilidades
rule_entity define la identidad catalogal canónica
company_profile define la ampliación rica
2. No duplicar semántica de identidad

company_profile no define una nueva identidad pública de empresa.

3. No mezclar identidad pública y wiring interno
el exterior usa ruleSystemCode + companyCode
el interior usa FK técnica a rule_entity.id
4. company_profile no reemplaza a rule_entity

No puede existir empresa operativamente válida sin su base catalogal correspondiente.

5. La vigencia canónica sigue residiendo en rule_entity

No se traslada a company_profile una lógica temporal propia en esta fase.

6. taxIdentifier podrá evolucionar

En V1 se modela como dato simple, pero el diseño permite introducir más adelante validaciones específicas por país o regla sin romper la arquitectura.

Relación con el crecimiento de rule_system

Esta decisión se considera importante porque establece una vía general para la evolución del metamodelo.

Patrón emergente

Un concepto del metamodelo puede recorrer estas fases:

Fase 1 — Catálogo puro

Solo requiere:

code
name
vigencia
Fase 2 — Catálogo + profile enriquecido

El concepto sigue siendo reusable, pero necesita ficha ampliada.

Fase 3 — Vertical/autonomía plena

Solo cuando además aparecen:

operaciones canónicas propias
invariantes fuertes propias
UX específica de mantenimiento
procesos donde el concepto es sujeto funcional

Esta progresión evita dos errores:

dejar conceptos ricos empobrecidos en el catálogo
convertir demasiado pronto cualquier catálogo importante en un subdominio grande
Consecuencias positivas
mantiene a COMPANY como concepto reusable y estable del metamodelo
evita sobrecargar rule_entity con atributos ricos no propios de un catálogo
mantiene coherencia con la filosofía general del proyecto: business key fuera, surrogate key dentro
abre una senda de crecimiento sana para otros conceptos del rulesystem
permite enriquecer “datos de empresa” sin crear todavía una arquitectura organizativa prematura
se alinea con patrones ya conocidos en sistemas como HRAccess, donde el identificador técnico del catálogo funciona como anclaje FK en estructuras derivadas
Costes / riesgos
introduce una nueva tabla y lógica de resolución adicional
obliga a mantener clara la frontera entre catálogo y profile
deja abierta una futura decisión sobre contactos empresariales
puede requerir refactor si en el futuro COMPANY adquiere procesos y operaciones suficientes para convertirse en vertical plena
Alternativas consideradas
1. Mantener todo en rule_entity

Descartado.

Se queda corto para modelar datos empresariales básicos y empuja a usar el catálogo como contenedor genérico.

2. Crear ya organization.company como vertical plena

Descartado por prematuro.

No hay todavía suficientes operaciones, invariantes ni semántica propia para justificar esa promoción.

3. Modelar company_profile solo con business keys y sin FK técnica

Descartado.

Rompe innecesariamente la filosofía ya consolidada en el proyecto respecto a la separación entre identidad pública y persistencia interna.

4. Meter phone/email como campos planos en V1

Aplazado.

Posible, pero no deseable mientras no se aclare la estrategia de contactos empresariales.

No objetivos

Este ADR no introduce todavía:

modelo de numeración de empleados
vertical de contactos de empresa
vertical de direcciones de empresa con historización
bounded context organization
jerarquía organizativa
promoción de COMPANY a aggregate root autónomo
sincronización automática compleja entre catálogo y profile más allá de su relación estructural
Estrategia recomendada de implementación
Fase 1
mantener COMPANY en rule_entity
crear tabla company_profile
FK única a rule_entity.id
exponer lectura y actualización del profile
Fase 2
enriquecer frontend de “datos de empresa”
mostrar lectura agregada catálogo + profile
introducir validaciones ligeras de taxIdentifier
Fase 3
evaluar contactos empresariales
evaluar si algunos conceptos de company merecen profile adicional o vertical propia
Fase 4
revisar si COMPANY sigue siendo “catálogo + profile” o si ya ha madurado hasta necesitar vertical/autonomía plena
Resumen ejecutivo

COMPANY seguirá siendo en B4RRHH un catálogo reutilizable del metamodelo (rule_entity), porque su identidad canónica y su reutilización transversal así lo justifican.

Sin embargo, cuando la empresa necesite una ficha ampliada, esta no se modelará dentro de rule_entity, sino mediante un recurso complementario company_profile.

company_profile se anclará internamente mediante FK técnica a rulesystem.rule_entity.id, preservando la misma filosofía que ya se usa en employee: business keys en la API pública, IDs técnicos solo en persistencia.

La primera versión del profile se limitará a:

nombre legal
identificador fiscal
dirección

y dejará fuera, de momento, numeración y contactos empresariales.

Esta decisión no crea todavía un nuevo universo organization.*, pero sí fija una vía muy importante para el crecimiento del rulesystem: catálogo reusable → profile enriquecido → posible vertical plena solo si el dominio lo exige.
<!-- END FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md -->


---

# FILE: ADR-022-Global-message-and-feedback-policy.md
<a name="file-adr-022-global-message-and-feedback-policy-md"></a>

<!-- BEGIN FILE: ADR-022-Global-message-and-feedback-policy.md -->

# ADR-0XX — Global Message & Feedback Policy

## Status

Accepted

## Context

The application has evolved into a complex, multi-vertical UI (employee, contacts, addresses, working_time, etc.) with:

* independent sections
* multiple interaction points
* backend-driven validations and business rules

Previously, user feedback (errors, success, warnings) was:

* duplicated across components
* inconsistently displayed
* sometimes invisible or easy to miss
* tightly coupled to local UI sections

This created confusion:

* users did not know where to look for feedback
* messages lacked hierarchy
* visual noise increased with complexity

A unified, system-level feedback mechanism is required.

---

## Decision

Introduce a **Global Floating Message System** as the single source of truth for application feedback.

This system is:

* **global** → not tied to any specific section
* **floating** → overlays UI, does not affect layout
* **centralized** → managed via a shared service
* **hierarchical** → distinguishes between message types and scopes

---

## Core Principles

### 1. Single Source of Truth

All operation-level feedback MUST go through the global message system.

No duplicated messages across components.

---

### 2. Non-Intrusive Overlay

Messages:

* MUST NOT modify page layout
* MUST NOT push content down
* MUST float above the UI

---

### 3. Clear Separation of Concerns

| Message Type            | Location   |
| ----------------------- | ---------- |
| Operation success       | Global     |
| Backend errors          | Global     |
| Business rule conflicts | Global     |
| Submit errors           | Global     |
| Warnings                | Global     |
| Inline field validation | Local only |

---

### 4. Predictable User Experience

The user MUST always know:

> “If something important happened, I look at the global message layer.”

---

## Message Types

### Success

* lightweight
* auto-dismiss
* visually subtle
* includes visible timeout indicator

### Error

* sticky
* requires attention
* visually stronger but not aggressive
* may include navigation to affected section

### Warning

* visible but less dominant than error
* may allow continuation

### Info

* optional
* low priority

---

## Behavior Rules

### Entry / Exit

Messages MUST have:

* smooth entry animation (fade + slight movement)
* smooth exit animation
* no abrupt appearance/disappearance

---

### Auto-dismiss

* success messages auto-dismiss with visible progress
* errors remain until dismissed or resolved

---

### Stacking

* limit visible messages (max 2–3)
* group or summarize if necessary

---

### Navigation

If a message is linked to a section:

* user can navigate via “Go to section”
* system may:

  * activate tab
  * scroll to section
  * highlight briefly

---

## Publication Rules

### MUST publish to global system

* create/update/delete operations
* backend validation errors
* business conflicts
* workflow errors (hire, terminate, etc.)

### MUST NOT remain only local

Examples:

* “Ya existe un contacto para ese tipo”
* “Invalid working_time configuration”

These MUST be global.

---

### Local-only feedback

Allowed only for:

* field validation while typing
* input-level hints
* invalid/touched states

---

## Anti-Patterns (Forbidden)

* duplicated messages (global + local banner)
* full-width banners inside sections for operation results
* messages that modify layout flow
* silent failures (no visible feedback)

---

## Implementation

### GlobalMessageService

Responsible for:

* publishing messages
* managing lifecycle
* deduplication
* stacking rules

### app-global-message-rail

Responsible for:

* rendering floating overlay
* animations
* user interaction
* navigation hooks

---

## Consequences

### Positive

* consistent UX
* predictable feedback model
* reduced duplication
* scalable across verticals
* Copilot-friendly (clear rules)

### Trade-offs

* requires refactoring of existing components
* forces discipline in message publishing
* initial overhead to standardize

---

## Future Improvements

* grouping by section/vertical
* message prioritization
* accessibility enhancements (ARIA/live regions)
* analytics on user interactions with messages

---

## Summary

Feedback is no longer a UI detail.

It is a **system-level capability**.

All meaningful application feedback must be:

> centralized, visible, predictable, and non-intrusive.

<!-- END FILE: ADR-022-Global-message-and-feedback-policy.md -->


---

# FILE: ADR-023-UI-interaction-contracts-per-vertical.md
<a name="file-adr-023-ui-interaction-contracts-per-vertical-md"></a>

<!-- BEGIN FILE: ADR-023-UI-interaction-contracts-per-vertical.md -->

# ADR-0XY — UI Interaction Contracts per Vertical

## Status

Accepted

---

## Context

The application is structured in independent verticals:

* contacts
* addresses
* identifiers
* working_time
* contract
* labor_classification
* etc.

Each vertical:

* manages its own UI state
* performs operations (create/update/delete)
* interacts with backend services
* produces user feedback (success, errors, validation)

Without a formal contract, verticals tend to:

* implement feedback inconsistently
* mix local and global messages arbitrarily
* duplicate logic
* break UX predictability

A clear **interaction contract per vertical** is required.

---

## Decision

Each vertical MUST follow a standardized **UI interaction contract**:

> A vertical does not decide how feedback is shown.
> It only decides **what happened**.

Feedback rendering is delegated to the global system.

---

## Core Interaction Model

Every user action in a vertical follows this flow:

1. User action (click / submit)
2. Local UI state changes (loading, disabling inputs)
3. Backend call
4. Result handling:

   * success → publish global message
   * error → publish global message
   * validation → mark fields locally + optionally publish global
5. UI stabilization

---

## Standard Interaction Phases

### 1. Idle

* no pending operation
* inputs enabled

---

### 2. Processing

* triggered by user action
* UI MUST:

  * disable relevant inputs
  * show loading state (button spinner, etc.)
* MUST NOT show global message yet

---

### 3. Success

On successful operation:

* MUST call `GlobalMessageService.success(...)`
* MUST NOT render local success banner
* MAY:

  * reset form
  * refresh list/data
  * focus relevant UI area

---

### 4. Error

On operation failure:

* MUST call `GlobalMessageService.error(...)`
* MUST NOT render generic local error banners
* MUST:

  * re-enable inputs
* MAY:

  * highlight affected section
  * keep user input intact

---

### 5. Validation

Two types:

#### a) Inline validation (client-side)

* handled locally
* shown at field level
* does NOT go to global system

#### b) Backend/business validation

* MUST be published globally
* MAY also:

  * mark fields invalid
  * show inline hints

Example:

* “contactValue invalid” → global + field highlight
* “duplicate contact type” → global (not just local banner)

---

## Message Publishing Contract

Each vertical MUST use the global service:

```ts
messageService.success(...)
messageService.error(...)
messageService.warning(...)
```

A vertical MUST NOT:

* render global-like banners locally
* bypass the message system

---

## Section Awareness

When publishing messages, verticals SHOULD include:

* `sectionId`
* optional `fieldId`

This enables:

* navigation ("Go to section")
* scroll behavior
* contextual highlighting

---

## UI Responsibilities by Layer

### Vertical Component

Responsible for:

* capturing user interaction
* managing local UI state (loading, form state)
* invoking backend
* publishing message events

NOT responsible for:

* deciding how messages are rendered
* displaying global feedback

---

### Global Message System

Responsible for:

* rendering feedback
* animation
* stacking
* navigation
* lifecycle (auto-dismiss, sticky)

---

## Anti-Patterns (Forbidden)

* local success banners
* duplicated error messages (global + local)
* silent failures
* mixing rendering logic inside verticals
* inconsistent handling between verticals

---

## UX Consistency Rules

All verticals MUST behave consistently:

| Action            | Behavior               |
| ----------------- | ---------------------- |
| Create success    | Global success message |
| Update success    | Global success message |
| Delete success    | Global success message |
| Backend error     | Global error message   |
| Business conflict | Global error message   |
| Field invalid     | Local inline error     |

---

## Example — Contacts Vertical

### Create Contact

#### Success

* publish global success
* reset form
* refresh contact list

#### Error (duplicate type)

* publish global error
* keep form values
* optionally mark field

#### Validation (email format)

* local inline error only

---

## Consequences

### Positive

* consistent UX across all verticals
* clear separation of responsibilities
* easier maintenance
* Copilot can follow predictable patterns
* scalable to new verticals

---

### Trade-offs

* requires refactoring existing verticals
* stricter discipline in UI development
* less “freedom” inside components

---

## Evolution

Future enhancements may include:

* standardized helper hooks for verticals
* base abstract component for interaction handling
* unified error mapping from backend → UI
* analytics on user interaction failures

---

## Summary

Verticals do not control feedback presentation.

They only emit **interaction outcomes**.

The system controls how feedback is displayed.

---

## Golden Rule

> If a vertical performs an operation,
> it MUST publish the outcome to the global system.

<!-- END FILE: ADR-023-UI-interaction-contracts-per-vertical.md -->


---

# FILE: ADR-024_autorizacion_jerarquica_B4RRHH.md
<a name="file-adr-024-autorizacion-jerarquica-b4rrhh-md"></a>

<!-- BEGIN FILE: ADR-024_autorizacion_jerarquica_B4RRHH.md -->

# ADR — Modelo de autorización jerárquica para recursos funcionales en B4RRHH

## Estado
Propuesto

## Decisión principal
La autorización en B4RRHH se modela como un dominio propio basado en:
- roles funcionales
- recursos funcionales jerárquicos
- acciones semánticas
- perfiles de permiso reutilizables
- políticas rol-recurso con herencia y overrides

## Nombre recomendado
`authorization` (preferible a `security` para no mezclar autenticación con autorización)

## Contexto
B4RRHH ya modela el dominio por verticales funcionales, business keys públicas y operaciones honestas. Falta cerrar la autorización con el mismo rigor.

El problema no es solo “añadir roles”, sino resolver:
- lectura global pero mantenimiento parcial por vertical
- workflows permitidos para unos roles y prohibidos para otros
- defaults razonables para recursos nuevos
- extensión a `employee`, `rulesystem` y futuros bounded contexts

## Problema
Un modelo simple de roles por endpoint o por CRUD puro no encaja bien porque:
- la unidad natural del proyecto es el recurso funcional, no el endpoint
- el dominio usa acciones como `CLOSE`, `CORRECT`, `EXECUTE`
- habrá más bounded contexts además de `employee`
- no se quiere reconfigurar cada rol cada vez que nazca una vertical

## Decisión
Se introduce un bounded context técnico-funcional `authorization`.

La cadena lógica del modelo será:

`rol -> política sobre recurso -> perfil de permiso -> acciones permitidas`

El recurso asegurado vive dentro de un árbol jerárquico. Ejemplo:

- `employee`
  - `employee.employee`
  - `employee.contact`
  - `employee.identifier`
  - `employee.address`
  - `employee.presence`
  - `employee.work_center`
  - `employee.working_time`
  - `employee.cost_center`
  - `employee.contract`
  - `employee.labor_classification`
  - `employee.lifecycle`
    - `employee.lifecycle.hire`
    - `employee.lifecycle.terminate`
    - `employee.lifecycle.rehire`
- `rulesystem`
  - `rulesystem.rule_system`
  - `rulesystem.rule_entity_type`
  - `rulesystem.rule_entity`

Los workflows se tratan como recursos de primera clase.

## Principios
1. Separación fuerte entre autenticación y autorización.
2. Recurso funcional como unidad canónica de control.
3. Acciones semánticas honestas.
4. Default + override.
5. Escalabilidad transversal.
6. No modelar autorización por campo como regla general.

## Modelo relacional propuesto

### `authorization.role`
Define roles funcionales.

Campos principales:
- `code`
- `name`
- `description`
- `active`

Ejemplos:
- `ADMIN`
- `HR_MANAGER`
- `HR_OPERATOR`
- `AUDITOR`
- `CATALOG_MANAGER`
- `READONLY`

### `authorization.secured_resource`
Catálogo jerárquico de recursos protegidos.

Campos principales:
- `resource_code`
- `parent_resource_code`
- `bounded_context_code`
- `resource_kind`
- `resource_family_code`
- `name`
- `description`
- `active`

`resource_kind` recomendado:
- `BOUNDED_CONTEXT`
- `VERTICAL`
- `WORKFLOW`
- `GROUP`
- `ADMIN_RESOURCE`

#### Sobre `resource_family_code`

`resource_family_code` es un agrupador funcional de recursos. **No es jerarquía** (eso lo modela `parent_resource_code`) — es agrupación semántica transversal.

**Para qué sirve:**
- Simplificar autorización: en vez de definir 50 reglas por recurso, se definen 5 reglas por familia.
- Evitar explosión de políticas: un rol puede autorizarse sobre una familia entera.
- Permitir reglas transversales: "RRHH ve todo lo de datos de empleado", "Finanzas solo lo económico".

**Familias iniciales recomendadas:**

| `resource_family_code` | Recursos que agrupa |
|------------------------|---------------------|
| `EMPLOYEE_DATA` | employee, contact, identifier, address, contract, labor_classification, presence |
| `ORGANIZATION` | work_center, cost_center |
| `LIFECYCLE` | lifecycle, lifecycle.hire, lifecycle.terminate, lifecycle.rehire |
| `MASTER_DATA` | rulesystem, rule_entity_type, rule_entity |
| `ADMINISTRATION` | authorization y sus sub-recursos |

**Reglas de uso:**
- Todo `secured_resource` debe declarar su `resource_family_code`.
- El catálogo de familias es cerrado y se gobierna mediante ADR o enum en código.
- Las políticas pueden definirse sobre familias en el futuro (extensión de V1, no en V1).
- En V1 `resource_family_code` es campo informativo/filtro de UI; la evaluación jerárquica no lo usa directamente.

### `authorization.action`
Catálogo de acciones.

Acciones iniciales recomendadas:
- `READ`
- `CREATE`
- `UPDATE`
- `DELETE`
- `CLOSE`
- `CORRECT`
- `EXECUTE`
- `ADMIN`

### `authorization.permission_profile`
Perfil reusable de permisos.

Perfiles iniciales recomendados:
- `NONE`
- `READ_ONLY`
- `SLOT_MAINTAINER`
- `TEMPORAL_MAINTAINER`
- `WORKFLOW_EXECUTOR`
- `FULL_CONTROL`

### `authorization.permission_profile_action`
Tabla de composición perfil -> acción.

PK compuesta:
- `permission_profile_code`
- `action_code`

### `authorization.role_resource_policy`
Tabla central del modelo.

Campos principales:
- `role_code`
- `resource_code`
- `permission_profile_code`
- `propagation_mode`
- `active`

PK compuesta:
- `(role_code, resource_code)`

`propagation_mode` recomendado:
- `THIS_RESOURCE_ONLY`
- `THIS_RESOURCE_AND_CHILDREN`

### `authorization.user_role_assignment` (aplazado a V2)
Solo necesaria si B4RRHH persiste roles internos. En V1 los roles del sujeto se extraen del JWT emitido por el IdP externo — B4RRHH no gestiona la asignación de roles, solo la lee del token.

Campos principales (futuros):
- `subject_code`
- `role_code`
- `assignment_origin`
- `active`

## Reglas de modelado
- Todo recurso nuevo que requiera autorización debe registrarse en `authorization.secured_resource`.
- Todo recurso debe declarar, siempre que exista, un `parent_resource_code`.
- Todo recurso debe declarar su `resource_family_code`.
- Los workflows se modelan como recursos de tipo `WORKFLOW`.
- Las políticas se definen sobre recursos, no sobre endpoints.
- La ausencia de permiso implica denegación.
- `NONE` es un perfil que no concede ninguna acción. No es un deny con precedencia sobre otros roles — si otro rol del sujeto concede la acción por otro camino del árbol, la evaluación devuelve ALLOW. `NONE` solo deniega cuando es el único perfil aplicable.
- En V1 no se introducen deny explícitos con precedencia sobre grants de otros roles.
- La autorización por campo queda fuera del modelo base.

## Algoritmo de evaluación
Entrada:
- sujeto autenticado
- roles efectivos
- `resource_code`
- `action_code`

Resolución:
1. Buscar política exacta para `role_code + resource_code`.
2. Si no existe, subir al padre.
3. Repetir hasta la raíz.
4. Cuando se encuentre una política, resolver el perfil.
5. Comprobar si el perfil contiene la acción.
6. Si algún rol concede, permitir.
7. Si ninguno concede, denegar.

Reglas de precedencia:
- el recurso más cercano gana sobre ancestros más lejanos
- la coincidencia exacta gana sobre la heredada
- basta una concesión positiva para permitir
- ausencia de concesión = deny por defecto

## Ejemplos de políticas

### AUDITOR
- `AUDITOR` sobre `employee` -> `READ_ONLY` con propagación a hijos
- `AUDITOR` sobre `rulesystem` -> `READ_ONLY` con propagación a hijos

### HR_OPERATOR
- `HR_OPERATOR` sobre `employee` -> `READ_ONLY` con propagación a hijos
- `HR_OPERATOR` sobre `employee.contact` -> `SLOT_MAINTAINER`
- `HR_OPERATOR` sobre `employee.identifier` -> `SLOT_MAINTAINER`
- `HR_OPERATOR` sobre `employee.address` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.work_center` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.working_time` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.lifecycle.hire` -> `WORKFLOW_EXECUTOR`
- `HR_OPERATOR` sobre `employee.lifecycle.terminate` -> `NONE`
- `HR_OPERATOR` sobre `employee.lifecycle.rehire` -> `NONE`

### HR_MANAGER
- `HR_MANAGER` sobre `employee` -> `READ_ONLY` con propagación a hijos
- `HR_MANAGER` sobre `employee.contact` -> `SLOT_MAINTAINER`
- `HR_MANAGER` sobre `employee.identifier` -> `SLOT_MAINTAINER`
- `HR_MANAGER` sobre `employee.address` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.work_center` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.working_time` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.lifecycle` -> `WORKFLOW_EXECUTOR` con propagación a hijos

### CATALOG_MANAGER
- `CATALOG_MANAGER` sobre `rulesystem.rule_entity` -> `FULL_CONTROL`
- `CATALOG_MANAGER` sobre `rulesystem.rule_entity_type` -> `READ_ONLY`
- `CATALOG_MANAGER` sobre `rulesystem.rule_system` -> `READ_ONLY`

### ADMIN
- `ADMIN` sobre `employee` -> `FULL_CONTROL` con propagación a hijos
- `ADMIN` sobre `rulesystem` -> `FULL_CONTROL` con propagación a hijos
- `ADMIN` sobre `authorization` -> `FULL_CONTROL` con propagación a hijos

## Ejemplo completo de resolución
Caso: `HR_OPERATOR` intenta ejecutar `employee.lifecycle.terminate` con acción `EXECUTE`.

Resolución:
1. Existe política exacta sobre `employee.lifecycle.terminate`.
2. El perfil es `NONE`.
3. `NONE` no contiene `EXECUTE`.
4. Resultado: denegado.

Caso: `HR_MANAGER` intenta ejecutar `employee.lifecycle.terminate` con acción `EXECUTE`.

Resolución:
1. No existe política exacta.
2. Se sube al padre `employee.lifecycle`.
3. Existe perfil `WORKFLOW_EXECUTOR` con propagación a hijos.
4. `WORKFLOW_EXECUTOR` contiene `EXECUTE`.
5. Resultado: permitido.

## Reglas de crecimiento
Cuando nazca una vertical nueva, por ejemplo `employee.bank_account`:
1. se registra en `authorization.secured_resource`
2. se cuelga de `employee`
3. hereda permisos por defecto
4. solo se añade override si el recurso necesita trato especial

## Integración
### Backend
- Spring Security valida el JWT Bearer en cada request (Resource Server con clave simétrica HS256 en V1).
- Los roles del sujeto se extraen del claim `roles` del JWT y se cargan como `GrantedAuthority` en el `SecurityContext`.
- B4RRHH resuelve autorización por `resource_code + action_code` consultando su propio bounded context `authorization`.
- Se expone `POST /authorization/evaluate` que recibe `{ resourceCode, actionCode }` y evalúa con los roles del JWT autenticado.
- La seguridad real vive en backend.

### Frontend
- Puede consultar `POST /authorization/evaluate` para derivar capacidades UI como `canEditContacts` o `canExecuteTerminate`.
- La ocultación de acciones es UX, no seguridad real.

## No objetivos de V1
- CRUD API para gestionar roles, recursos, perfiles y políticas (se gestiona por Flyway).
- Asignación de roles a sujetos desde la API (tabla `user_role_assignment` aplazada).
- Deny explícito con precedencia sobre grants de otros roles.
- Autorización aplicada automáticamente en los endpoints de `employee` (interceptores Spring Security). En V1 solo existe el endpoint de evaluación explícita.

## No objetivos
- autorización contextual por instancia concreta
- seguridad por `ruleSystemCode`, `companyCode` o manager scope en V1
- autorización por campo como modelo base
- detalle del login OIDC y ciclo de vida del token
- deny explícitos con precedencias complejas

## Consecuencias
### Positivas
- alinea autorización con el lenguaje funcional de B4RRHH
- evita acoplar permisos a endpoints
- permite defaults razonables
- admite overrides finos
- trata workflows y verticales bajo un mismo marco
- deja base sólida para auditoría futura

### Costes
- aparece un bounded context adicional
- hay que gobernar el árbol de recursos
- la evaluación jerárquica debe estar muy bien testeada

## Plan de implantación
1. Crear schema `authorization` y semillas base.
2. Implementar evaluación jerárquica en backend.
3. Integrar roles efectivos desde JWT u origen externo.
4. Exponer capacidades derivadas al frontend.
5. Extender a `rulesystem` y futuros bounded contexts.
6. Evaluar futuras extensiones: auditoría, contexto, datos sensibles.

## Resumen ejecutivo
B4RRHH debe modelar la autorización como un dominio propio, separado de la autenticación, apoyado en recursos funcionales jerárquicos. Los roles no conceden permisos sobre endpoints, sino perfiles de permiso sobre recursos del árbol funcional del sistema.

La combinación de recurso jerárquico, perfil reusable y propagación al árbol permite exactamente el equilibrio buscado:
- permisos por defecto razonables
- overrides explícitos para recursos sensibles o workflows concretos
- crecimiento limpio sin mantenimiento infernal

<!-- END FILE: ADR-024_autorizacion_jerarquica_B4RRHH.md -->


---

# FILE: ADR-025-subject-roles.md.md
<a name="file-adr-025-subject-roles-md-md"></a>

<!-- BEGIN FILE: ADR-025-subject-roles.md.md -->

ADR — Identidad por Subject y Asignación Interna de Roles en B4RRHH

Estado: Propuesto

## Contexto
B4RRHH dispone de autenticación JWT y un modelo de autorización interno basado en roles, recursos, perfiles y políticas.
Actualmente, los roles se transportan en el JWT, lo cual mezcla identidad y autorización.

## Problema
Se necesita un modelo coherente que:
- Separe autenticación de autorización
- Permita operación en local sin IdP externo
- Evite usar el JWT como fuente de verdad de roles

## Decisión
B4RRHH utilizará:
- JWT como fuente de identidad (subject)
- Base de datos como fuente de roles

Se introduce la tabla:
authz.subject_role_assignment

## Modelo
Campos:
- subject_code
- role_code
- active
- assignment_origin
- created_at
- updated_at

Clave primaria:
(subject_code, role_code)

## Flujo
1. El frontend obtiene un JWT con subject
2. Backend autentica el token
3. Backend extrae subject
4. Backend resuelve roles desde BD
5. Backend evalúa permisos

## Consecuencias
Positivas:
- Separación clara de responsabilidades
- Preparado para futuro IdP
- Coherencia del modelo

Negativas:
- Nueva tabla y servicio
- Mayor complejidad inicial

## No objetivos
- No se introduce login con contraseña
- No se introduce dominio user
- No se integra IdP externo en esta fase

## Evolución futura
Integración con proveedor externo manteniendo autorización interna.

## Sobre subjeect_code
subject_code representa la identidad autenticada del actor y se trata como identificador opaco; no se normaliza por case y no se interpreta como business key.
<!-- END FILE: ADR-025-subject-roles.md.md -->


---

# FILE: ADR-026-payroll-status-workflow.md.md
<a name="file-adr-026-payroll-status-workflow-md-md"></a>

<!-- BEGIN FILE: ADR-026-payroll-status-workflow.md.md -->

# ADR — Payroll Status Workflow and Recalculation Guardrails

## Estado
Propuesto

## Contexto
Se necesita controlar estrictamente cuándo una nómina puede recalcularse.

## Estados
- NOT_VALID
- CALCULATED
- EXPLICIT_VALIDATED
- DEFINITIVE

## Regla central
Solo las nóminas en `NOT_VALID` pueden ser recalculadas (borradas y recreadas).

## Semántica

### NOT_VALID
- Resultado inválido o invalidado manualmente
- Único estado recalcable

### CALCULATED
- Resultado válido provisional
- No recalculable sin pasar a NOT_VALID

### EXPLICIT_VALIDATED
- Validada manualmente
- Bloqueada frente a recálculo automático

### DEFINITIVE
- Final, inmutable

## Transiciones

### Desde NOT_VALID
- -> CALCULATED (cálculo OK)
- -> NOT_VALID (cálculo sigue inválido)
- NO -> EXPLICIT_VALIDATED
- NO -> DEFINITIVE

### Desde CALCULATED
- -> NOT_VALID (invalidación)
- -> EXPLICIT_VALIDATED
- -> DEFINITIVE

### Desde EXPLICIT_VALIDATED
- -> NOT_VALID (manual)
- -> DEFINITIVE

### Desde DEFINITIVE
- sin salida

## statusReasonCode
Ejemplos:
- ENGINE_INVALID
- USER_INVALIDATED
- MASS_RECALC_REQUEST

## Regla operativa
El motor solo borra/recrea nóminas en NOT_VALID.

## Resumen
Workflow seguro que evita recálculos accidentales mediante invalidación explícita previa.

<!-- END FILE: ADR-026-payroll-status-workflow.md.md -->


---

# FILE: ADR-027-payroll-root-model.md.md
<a name="file-adr-027-payroll-root-model-md-md"></a>

<!-- BEGIN FILE: ADR-027-payroll-root-model.md.md -->

# ADR — Payroll Root Model (`payroll.payroll`)

## Estado
Propuesto

## Contexto
B4RRHH organiza por verticales y usa business keys en APIs. `employee.presence` identifica una relación laboral (empleado + presenceNumber).
La nómina debe modelarse como **resultado de cálculo**, no como documento ni CRUD editable.

## Decisión
Se crea el bounded context `payroll` (schema propio) y la raíz:
- `payroll.payroll`

Representa el resultado funcional de una nómina para:
- empleado
- período de nómina
- tipo de nómina
- presencia

No es:
- documento PDF
- recurso editable
- entidad corregible in place

## Identidad funcional
- ruleSystemCode
- employeeTypeCode
- employeeNumber
- payrollPeriodCode
- payrollTypeCode
- presenceNumber

Ejemplo:
ESP + EMP + 0001 + 202501 + ORD + 2

## Campos raíz
- status
- statusReasonCode
- calculatedAt
- calculationEngineCode
- calculationEngineVersion

(No incluir totales agregados ni notas)

## Recursos hijos
### payroll_concept
- lineNumber
- conceptCode
- conceptLabel
- amount
- quantity?
- rate?
- conceptNatureCode
- originPeriodCode?
- displayOrder

### payroll_context_snapshot
- snapshotTypeCode
- sourceVerticalCode
- sourceBusinessKeyJson
- snapshotPayloadJson

## Reglas
- FK hijas con ON DELETE CASCADE
- No edición manual
- Sustitución por borrado + recreación
- Unicidad por business key

## Resumen
`payroll.payroll` es un resultado materializado, no editable, regenerable por cálculo, con conceptos y snapshots dependientes.

<!-- END FILE: ADR-027-payroll-root-model.md.md -->


---

# FILE: ADR-029-payroll-calculate-contract-stub.md
<a name="file-adr-029-payroll-calculate-contract-stub-md"></a>

<!-- BEGIN FILE: ADR-029-payroll-calculate-contract-stub.md -->

# ADR — Payroll Calculate Contract (Initial Stub Calculator)

## Estado
Propuesto

## Contexto

B4RRHH ya ha decidido que `payroll.payroll` es un resultado materializado, no editable, con business key funcional basada en empleado + período + tipo + presencia, y que los estados del resultado gobiernan si una nómina puede o no ser sustituida. fileciteturn4file1 fileciteturn4file0

También se ha decidido ahora que el launch de nómina sólo resuelve y orquesta unidades elegibles, delegando el cálculo real a otro caso de uso especializado.

El proyecto, además, exige:

- arquitectura vertical-first;
- APIs públicas por business keys;
- naming semántico;
- evitar sobreingeniería prematura. fileciteturn4file10 fileciteturn4file12 fileciteturn4file13

En esta fase todavía no existe un motor de reglas de nómina real. Sin embargo, hace falta un componente de cálculo inicial que permita probar:

- el flujo launch -> calculate;
- la sustitución por borrado + recreación;
- la creación de `payroll.payroll`;
- la generación de conceptos;
- la generación de snapshots;
- el tratamiento de estados `CALCULATED` y `NOT_VALID`.

## Problema

Se necesita un contrato de cálculo inicial que permita construir un **stub calculator útil**, suficientemente real para validar el pipeline técnico y funcional, pero deliberadamente pequeño para no anticipar todavía el motor de reglas.

Ese cálculo inicial debe:

- recibir unidades explícitas ya resueltas;
- no decidir poblaciones objetivo;
- materializar resultados en `payroll`;
- poder generar resultados `CALCULATED` y `NOT_VALID`;
- ser sustituible en el futuro por el motor real sin romper la semántica externa.

## Decisión

Se introduce el contrato de **Payroll Calculate** como caso de uso/endpoint especializado que recibe una lista explícita de unidades de cálculo y materializa resultados de nómina.

`calculate`:

- no resuelve la población objetivo;
- no selecciona elegibles por sí mismo como responsabilidad principal;
- no es todavía un motor declarativo de reglas;
- actúa como calculador inicial del sistema;
- podrá empezar implementado como **stub calculator**.

## Definición funcional

`calculate` recibe una colección cerrada de unidades de cálculo y, para cada una de ellas:

1. valida precondiciones mínimas;
2. elimina la nómina previa sólo si existe y es sustituible según reglas;
3. crea una nueva `payroll.payroll`;
4. crea conceptos de nómina de prueba o cálculo básico;
5. crea snapshots contextuales mínimos;
6. persiste el resultado final en estado:
   - `CALCULATED`, si el cálculo concluye correctamente;
   - `NOT_VALID`, si detecta una invalidez funcional del cálculo.

## Unidad de entrada

La unidad mínima de entrada es:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `payrollPeriodCode`
- `payrollTypeCode`
- `presenceNumber`
- `calculationEngineCode`
- `calculationEngineVersion`

### Regla

`calculate` debe trabajar con unidades **explícitas**.

No debe aceptar un payload ambiguo que implique “resolver toda una población”. Esa responsabilidad pertenece al launch.

## Responsabilidades de calculate

`calculate` debe:

- cargar el contexto funcional mínimo de la unidad;
- comprobar la existencia previa de `payroll.payroll`;
- aplicar la política de sustitución;
- crear nueva raíz `payroll.payroll`;
- crear `payroll_concept`;
- crear `payroll_context_snapshot`;
- devolver resultado por unidad.

`calculate` no debe todavía:

- implementar reglas salariales complejas;
- modelar convenios reales;
- resolver retroactividad completa;
- introducir DSLs o engines genéricos;
- depender de un metamodelo complejo de reglas.

## Política de sustitución

Para cada unidad explícita:

### Si no existe nómina previa
- crear una nueva `payroll.payroll`.

### Si existe y está `NOT_VALID`
- eliminar la raíz previa;
- dejar que `ON DELETE CASCADE` elimine conceptos y snapshots; fileciteturn4file6
- crear una nueva `payroll.payroll`.

### Si existe y está `CALCULATED`, `EXPLICIT_VALIDATED` o `DEFINITIVE`
- no sustituirla;
- devolver resultado de unidad ignorada/no procesada, según shape final del contrato.

## Contrato de salida

`calculate` debe devolver resultado por unidad.

Campos orientativos:

- identity de la unidad;
- `processed = true/false`;
- `resultStatus = CALCULATED | NOT_VALID | SKIPPED`;
- motivo cuando no se procese;
- business key final de la nómina generada, si aplica.

## Stub calculator inicial

Se adopta explícitamente una estrategia de implementación por fases.

### Fase inicial permitida

El primer `calculate` puede generar una nómina artificial pero funcionalmente útil.

Ejemplo mínimo:

- crear `payroll.payroll`;
- generar 2 conceptos de prueba;
- generar 1 o 2 snapshots de contexto;
- persistir en `CALCULATED`.

También puede contemplarse una condición de prueba que genere `NOT_VALID` cuando falte algún dato mínimo requerido.

### Objetivo de esta fase

No hacer nómina real todavía.

El objetivo es validar el pipeline:

- endpoints;
- wiring;
- persistencia;
- borrado y recreación;
- estados;
- snapshots;
- conceptos;
- respuesta funcional.

## Conceptos de prueba

En esta fase, `payroll_concept` puede contener conceptos semilla o de demostración.

Ejemplo conceptual:

- `BASE_TEST`
- `DEVENGO_TEST`

Los nombres y códigos deben seguir una convención de negocio estable y no reforzar identidades técnicas equivocadas. El proyecto prioriza nombres de negocio y códigos funcionales estables. fileciteturn4file12

## Snapshots mínimos

`calculate` debe poblar al menos snapshots básicos para demostrar el diseño ya aprobado de `payroll_context_snapshot`. fileciteturn4file6

Ejemplos iniciales razonables:

- `EMPLOYEE_CORE`
- `PRESENCE`
- opcionalmente `WORKING_TIME` o `CONTRACT` cuando sea barato de recuperar

No es obligatorio arrancar con todos los snapshots futuros.

## Reglas de error / invalidez

En esta fase se distinguen dos clases:

### 1. Error técnico
Ejemplo:
- fallo de persistencia;
- error inesperado de infraestructura.

Esto debe reportarse como error técnico del proceso.

### 2. Resultado funcional `NOT_VALID`
Ejemplo:
- falta dato mínimo requerido para construir el cálculo stub;
- inconsistencia funcional detectada por el calculador.

En este caso sí puede persistirse una `payroll.payroll` con `status = NOT_VALID`, coherente con el workflow ya aprobado. fileciteturn4file0

## Forma de exposición

A falta de contrato final, se admiten dos estrategias:

### Opción A — calculate sólo como caso de uso interno
Útil si launch es el único endpoint externo.

### Opción B — calculate también como endpoint explícito
Útil para pruebas con Postman y validación incremental del pipeline.

En esta fase, se acepta la opción B por su valor práctico para acelerar aprendizaje y validación del flujo.

Nombre conceptual recomendado:

- `POST /payroll/calculations/calculate`

## Qué se rechaza explícitamente

Se rechaza en esta fase:

- introducir un motor declarativo de reglas;
- mezclar calculate con resolución de población;
- convertir calculate en un endpoint de “hazlo todo” sin unidades explícitas;
- bloquear el diseño futuro con un contrato demasiado acoplado al stub.

## Consecuencias

### Positivas

- permite probar el flujo completo desde muy pronto;
- separa orquestación de cálculo;
- facilita sustitución futura por motor real;
- valida conceptos y snapshots sin esperar al dominio salarial completo.

### Costes

- exige mantener disciplina para que el stub no se convierta en solución definitiva;
- habrá que evolucionar el contrato interno del calculador en fases posteriores;
- el primer resultado no representará todavía nómina real.

## Resumen

En B4RRHH, `calculate` es el caso de uso especializado que recibe unidades explícitas ya resueltas y materializa `payroll.payroll` con conceptos y snapshots.

En la primera iteración, puede implementarse como un **stub calculator útil**, orientado a validar el flujo técnico y funcional, no a resolver todavía el motor real de reglas de nómina.

<!-- END FILE: ADR-029-payroll-calculate-contract-stub.md -->


---

# FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md
<a name="file-adr-030-payroll-launch-calculation-run-claim-and-internal-calculator-orchestration-md"></a>

<!-- BEGIN FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md -->

ADR — Payroll Launch, Calculation Run, Claim and Internal Calculator Orchestration
Estado

Propuesto

Contexto

El bounded context payroll ya ha fijado una base importante:

la raíz funcional es payroll.payroll;
su identidad funcional es:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber;
payroll.payroll representa un resultado materializado de cálculo, no un CRUD editable;
sus hijos (payroll_concept, payroll_context_snapshot) dependen completamente de la raíz y deben eliminarse por cascade;
los estados de nómina gobiernan si una nómina puede o no ser sustituida;
una nómina existente solo es recalculable si está en NOT_VALID;
una unidad sin nómina previa también debe ser elegible para cálculo inicial.

También se ha decidido ya que el endpoint actual POST /payrolls/calculate no representa el futuro motor real, sino un stub temporal de validación de pipeline, donde el cliente aún aporta conceptos y snapshots explícitamente para poder probar el flujo de extremo a extremo antes de diseñar el lanzador real y el calculador definitivo.

En paralelo, B4RRHH tiene reglas de arquitectura muy claras:

primero se organiza por vertical/subdominio y dentro de cada vertical se aplica arquitectura hexagonal;
las APIs públicas deben usar business keys, no IDs técnicos;
cuando una operación no encaja como CRUD plano, debe modelarse como workflow/caso de uso explícito y no como recurso falso o tabla oportunista.

Al hablar del lanzamiento de nómina aparecen dos problemas de diseño que no deben mezclarse:

qué significa lanzar un cálculo;
cómo evitar que dos lanzamientos simultáneos procesen la misma unidad de cálculo.

Además, si el lanzamiento solo devuelve un body HTTP efímero, se pierde una capacidad que será útil muy pronto:

consultar desde frontend cómo va un cálculo;
saber cuántas unidades se han procesado;
ver qué se ha omitido, qué se ha reclamado, qué terminó en CALCULATED, qué quedó en NOT_VALID y qué falló.

Por todo ello, hace falta un modelo explícito para:

el workflow de launch;
la persistencia de los runs;
la exclusión concurrente por unidad de cálculo;
el desacoplamiento del calculador real respecto del endpoint público.
Problema

Se necesita definir una arquitectura de lanzamiento de nómina que:

permita lanzar cálculos sobre una población objetivo;
resuelva y expanda dicha población a unidades reales de cálculo;
filtre elegibilidad sin recalcular resultados protegidos;
permita concurrencia segura;
deje preparada la paralelización futura;
permita seguimiento de progreso;
desacople el launch del motor de cálculo real;
evite convertir el cálculo actual stub en contrato definitivo por accidente.
Decisión

Se adopta una arquitectura de orquestación de cálculo basada en cuatro piezas distintas:

payroll.payroll
Resultado materializado de una unidad de cálculo.
payroll.calculation_run
Recurso técnico-operativo persistido que representa una ejecución de lanzamiento.
payroll.calculation_claim
Recurso técnico de exclusión concurrente por unidad de cálculo.
calculate como caso de uso interno especializado, no como contrato público canónico del motor definitivo.

El lanzamiento de nómina se modela como un workflow explícito que:

crea un calculation_run;
resuelve población objetivo;
expande a unidades de cálculo;
determina elegibilidad;
intenta adquirir claims por unidad;
delega el cálculo efectivo a un calculador interno;
registra progreso y resumen.
Principio madre

Launch no calcula; launch coordina.
Calculate no decide población; calculate materializa una unidad.

Esta separación es obligatoria.

Definiciones principales
1. Unidad de cálculo

La unidad mínima de cálculo es:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber

Esta unidad es coherente con la business key de payroll.payroll ya adoptada y con el hecho de que una presencia concreta representa una relación laboral concreta del empleado.

Regla

El launch siempre trabaja con una colección de unidades de cálculo, no con empleados abstractos.

2. Population target vs eligible units

Se distinguen dos conceptos.

Población objetivo

Es el conjunto de empleados o ámbitos que el usuario quiere lanzar.

Ejemplos:

un empleado concreto;
una lista explícita de empleados;
todos los empleados de un scope determinado.
Unidades elegibles

Son las unidades de cálculo que realmente pueden entrar al cálculo.

Una unidad es elegible si:

no existe payroll.payroll previa para su business key, o
existe y su estado actual es NOT_VALID.

Una unidad no es elegible si existe y está en:

CALCULATED
EXPLICIT_VALIDATED
DEFINITIVE

Esta regla mantiene el guardarraíl funcional ya fijado para payroll y evita recálculos accidentales sobre resultados vigentes o protegidos.

3. payroll.calculation_run

Se introduce un recurso técnico-operativo persistido:

payroll.calculation_run
Naturaleza

No es una nómina.
No es un resultado de negocio final.
No sustituye a payroll.payroll.

Representa una ejecución de lanzamiento.

Objetivo

Permitir:

seguimiento del progreso;
trazabilidad del lanzamiento;
resumen persistido;
futura consulta desde frontend;
base para asincronía o paralelización posterior.
Campos mínimos recomendados
id técnico interno
ruleSystemCode
payrollPeriodCode
payrollTypeCode
calculationEngineCode
calculationEngineVersion
requestedAt
requestedBy nullable
status
targetSelectionJson
campos agregados de resumen o summaryJson
Estados recomendados del run
REQUESTED
RUNNING
COMPLETED
COMPLETED_WITH_ERRORS
FAILED
Regla

La máquina de estados de calculation_run es independiente de la máquina de estados de payroll.payroll.

No deben mezclarse.

4. payroll.calculation_claim

Se introduce un recurso técnico de exclusión concurrente:

payroll.calculation_claim
Naturaleza

No representa negocio visible al usuario final.
No sustituye a locks de BD del aggregate.
No es un recurso funcional público.

Su misión es impedir que dos runs distintos procesen simultáneamente la misma unidad de cálculo.

Claim key

La identidad funcional del claim es exactamente la unidad de cálculo:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber
Campos recomendados
id técnico
claim key completa
runId
claimedAt
claimedBy nullable
Restricción obligatoria

Debe existir una restricción única por claim key completa.

Regla de adquisición

La adquisición del claim debe ser atómica.

No se recomienda una lógica de:

leer si existe
y luego insertar

por riesgo de carrera.

La lógica correcta es:

intentar insertar claim;
si inserta, la unidad queda reclamada por ese run;
si falla por unicidad, esa unidad ya está siendo procesada por otro run y debe ignorarse o marcarse como no reclamada.
Regla de limpieza

Al finalizar el procesamiento de la unidad, el claim se elimina.

Extensión futura

Más adelante puede añadirse:

expiresAt
recuperación de claims huérfanos
housekeeping periódico

pero no es requisito de esta decisión base.

5. payroll.calculation_run_item

Se recomienda introducir, ya desde la base o en una fase muy cercana, una tabla hija de seguimiento fino:

payroll.calculation_run_item
Naturaleza

Representa el estado de una unidad concreta dentro de un run concreto.

Objetivo

Permitir:

trazabilidad por unidad;
saber qué pasó con cada cálculo;
alimentar frontend con progreso real;
distinguir skip, claim conflict, calculated, not valid, error, etc.
Campos recomendados
id
runId
calculation key completa
status
reasonCode nullable
processedAt nullable
message nullable opcional
Estados sugeridos
CANDIDATE
NOT_ELIGIBLE
CLAIMED
SKIPPED_ALREADY_CLAIMED
CALCULATED
NOT_VALID
ERROR
Regla

calculation_run_item pertenece al seguimiento del run, no al dominio raíz de payroll result.

Semántica de Launch
Definición

Lanzar nómina significa:

crear una ejecución persistida de cálculo, resolver una población objetivo, expandirla a unidades de cálculo, filtrar unidades elegibles, intentar reclamar cada unidad de forma exclusiva, delegar el cálculo efectivo al calculador interno y registrar el progreso y resultado del proceso.

Responsabilidades obligatorias del launch

El launch debe:

crear calculation_run;
validar contexto de ejecución;
resolver la población objetivo;
expandirla a unidades de cálculo;
determinar elegibilidad;
crear run_items o equivalente lógico;
intentar adquirir claim por unidad elegible;
delegar en calculate interno;
consolidar estados por unidad;
actualizar resumen y estado final del run.

El launch no debe:

generar conceptos él mismo;
implementar reglas salariales;
convertirse en motor de cálculo;
depender de HTTP interno al propio backend si launch y calculate viven en el mismo servicio.
Input mínimo del launch

Se recomienda que el launch reciba al menos:

ruleSystemCode
payrollPeriodCode
payrollTypeCode
calculationEngineCode
calculationEngineVersion
targetSelection
targetSelection

Debe permitir al menos:

empleado concreto
lista explícita
ámbito masivo simple

El shape contractual exacto podrá evolucionar, pero el launch debe conservar esta responsabilidad de resolución.

Output del launch

El launch no debe limitarse a devolver “201 created” con un resumen efímero.

Debe devolver, al menos:

runId
estado inicial o final del run
resumen agregado

y permitir después consultar el run persistido.

Calculate interno
Decisión clave

El futuro calculate no debe consolidarse como endpoint público canónico.

El endpoint actual de calculate se acepta solo como stub temporal de validación de pipeline, tal como ya está documentado en OpenAPI.

La decisión de fondo es:

el cálculo serio será largo, cambiante y costoso de desarrollar;
por tanto, el launch no debe acoplarse a un endpoint público rígido del motor.
Regla arquitectónica

El launch debe invocar un caso de uso interno de cálculo, no un endpoint HTTP del propio backend.

Ejemplo conceptual:

CalculatePayrollUnitUseCase
Responsabilidad del calculate interno

Recibir una unidad explícita y materializar un resultado:

creando payroll.payroll si no existe;
sustituyéndola si existe y es recalculable;
generando CALCULATED o NOT_VALID según corresponda.
Importante

El calculate interno no resuelve poblaciones.
Eso pertenece exclusivamente al launch.

Concurrencia
Decisión principal

La concurrencia se gobierna mediante payroll.calculation_claim, no mediante el aggregate payroll.payroll.

Justificación

La concurrencia aquí es un problema del workflow de ejecución, no de la identidad del recurso raíz.

Reglas
dos runs pueden existir simultáneamente;
dos runs no pueden procesar simultáneamente la misma unidad de cálculo;
si una unidad ya está reclamada por otro run, el launch actual debe marcarla como no reclamada / ya en curso y seguir adelante.
Diseño objetivo vs implementación inicial
Diseño objetivo

Exclusión por unidad de cálculo.

Implementación inicial recomendada

La propia claim table ya permite ese diseño desde la primera iteración, por lo que no se considera necesario arrancar con un bloqueo global de launch.

Consecuencia

La paralelización futura queda abierta desde el primer día, aunque inicialmente el procesamiento interno pueda seguir siendo secuencial.

Relación entre claim y business key de payroll

La restricción única de payroll.payroll por business key sigue siendo obligatoria y valiosa, pero no se considera mecanismo principal de coordinación concurrente.

Papel de la unique en payroll
protege integridad final del resultado;
actúa como última línea de defensa.
Papel del claim
evita que dos runs intenten procesar simultáneamente la misma unidad.

Por tanto:

la unique de payroll no sustituye a la claim table;
la claim table no sustituye a la unique del root.

Ambas son necesarias y cumplen papeles distintos.

Procesamiento secuencial vs paralelo
Regla base

El ADR no obliga a que el launch sea síncrono o asíncrono, ni a que procese secuencial o paralelamente.

Lo que sí fija es la semántica.

V1 aceptable
run persistido
claims por unidad
procesamiento secuencial dentro del launch
resumen final persistido
Evolución natural
paralelización por chunks o workers
asíncrono
polling desde frontend del estado del run
reintentos por unidad

El diseño aquí debe soportar esas evoluciones sin rehacer la semántica.

API pública recomendada
Endpoints canónicos de resultado

Se mantienen por business key:

GET payroll by business key
invalidate
explicit-validate
finalize

Esto sigue la convención general del proyecto de usar business keys públicas y acciones explícitas cuando el dominio lo pide.

Endpoint de launch

Se recomienda un endpoint público explícito de negocio, por ejemplo:

POST /payroll/calculation-runs/launch

o naming equivalente claramente orientado a ejecución.

Lectura de run

Se recomienda poder consultar:

GET /payroll/calculation-runs/{runId}

y eventualmente listar runs recientes o items asociados.

Calculate

El endpoint actual de calculate no se considera canónico a futuro. Su continuidad se limita a la fase stub/pre-launch ya documentada.

Qué se rechaza explícitamente

Se rechaza:

tratar launch como CRUD;
hacer que launch invoque HTTP contra su propio backend como arquitectura permanente;
bloquear necesariamente todo el sistema a un solo launch global;
usar solo la unique de payroll como solución de concurrencia;
mezclar calculation_run con payroll.payroll;
convertir calculation_run en una nueva raíz funcional de negocio;
fijar ya el contrato definitivo del motor de cálculo real;
acoplar la semántica de launch al stub actual de calculate.
Consecuencias
Positivas
separación limpia entre resultado, ejecución, concurrencia y motor;
base sólida para meses de evolución sin rehacer el modelo;
posibilidad de seguimiento de runs desde frontend;
paralelización futura preparada desde el diseño;
acoplamiento bajo entre launch y motor real;
protección real frente a colisiones concurrentes.
Costes
introduce recursos técnicos adicionales (calculation_run, calculation_claim, probablemente calculation_run_item);
exige disciplina para no mezclar estados de run con estados de payroll;
añade trabajo de persistencia y de resumen/progreso.
Plan recomendado por fases
Fase 1
introducir calculation_run
introducir calculation_claim
introducir launch
mantener procesamiento secuencial
calculate sigue siendo interno
persistir resumen básico del run
Fase 2
introducir calculation_run_item
seguimiento fino por unidad
consulta desde frontend del progreso
Fase 3
paralelización real
workers o executor
asincronía y polling más rico
posible housekeeping de claims
Resumen ejecutivo

En B4RRHH, el lanzamiento de nómina no se modelará como un simple POST que calcula y devuelve un body efímero.

Se adopta una arquitectura en la que:

payroll.payroll sigue siendo el resultado materializado por unidad;
payroll.calculation_run representa una ejecución persistida de lanzamiento;
payroll.calculation_claim garantiza exclusión concurrente por unidad de cálculo;
calculate será un caso de uso interno especializado y desacoplado del contrato público final;
el launch coordina, reclama, delega y registra;
la semántica queda preparada tanto para una V1 secuencial como para una evolución futura paralelizable y observable.
<!-- END FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md -->


---

# FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md
<a name="file-adr-031-modelo-f-sico-de-payroll-launch--calculation-run-claims-y-mensajes-md"></a>

<!-- BEGIN FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md -->

ADR — Modelo físico de payroll launch, calculation run, claims y mensajes
Estado

Propuesto

Contexto

El bounded context payroll ya tiene fijadas varias decisiones estructurales:

payroll.payroll es la raíz funcional del resultado materializado de nómina;
su business key es:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber;
payroll.payroll no es un CRUD editable, sino un resultado de cálculo;
los hijos payroll_concept y payroll_context_snapshot dependen completamente de la raíz y se eliminan por cascade;
el estado de la nómina (NOT_VALID, CALCULATED, EXPLICIT_VALIDATED, DEFINITIVE) vive en la propia payroll y gobierna si puede ser sustituida o no;
una unidad sin nómina previa también debe ser elegible para cálculo inicial.

También se ha consolidado otra separación importante:

launch coordina;
calculate materializa una unidad;
la concurrencia no debe gobernarse dentro del aggregate payroll.payroll, sino en una capa técnica de ejecución;
el proyecto prefiere workflows explícitos cuando una operación no encaja como CRUD plano.

Durante el diseño del lanzamiento apareció una discusión relevante sobre el detalle por unidad de ejecución.

Se descarta como núcleo inicial una tabla obligatoria de calculation_run_item porque:

generaría una fila por unidad para cada run;
puede producir mucho volumen con poco valor persistente;
muchos errores funcionales pertenecen realmente a la nómina materializada y no al run;
tras un nuevo cálculo de esa unidad, gran parte de ese detalle pierde relevancia operativa.

En cambio, sí se considera útil distinguir dos tipos de mensajes:

mensajes adheridos a la nómina
pertenecen al resultado materializado y deben vivir como vertical hija de payroll.payroll;
mensajes del run
pertenecen a la ejecución técnica del launch y pueden existir incluso cuando no se materializa una nueva payroll.
Problema

Se necesita un modelo físico que permita:

persistir ejecuciones de launch;
seguir su progreso general;
impedir concurrencia simultánea sobre la misma unidad de cálculo;
registrar mensajes operativos/técnicos del run;
registrar mensajes funcionales o revisables adheridos a una payroll;
mantener separado:
el resultado materializado (payroll.payroll)
de la ejecución técnica (calculation_run, calculation_claim, calculation_run_message).

El modelo debe ser suficiente para meses de evolución, sin fijar todavía el motor real de reglas.

Decisión

Se adopta dentro del schema payroll el siguiente modelo físico base:

payroll.payroll
resultado materializado de una unidad de cálculo. Ya existente.
payroll.payroll_warning
mensajes funcionales adheridos a una nómina concreta.
payroll.calculation_run
ejecución técnica persistida de un launch.
payroll.calculation_claim
exclusión concurrente por unidad de cálculo.
payroll.calculation_run_message
mensajes operativos, técnicos o de exclusión del propio run.

Se decide no introducir payroll.calculation_run_item como tabla obligatoria en la base inicial.

Principio estructural

La payroll persiste resultado.
La payroll_warning persiste mensajes funcionales de ese resultado.
El run persiste la ejecución.
El claim persiste exclusión concurrente.
El run_message persiste incidencias y mensajes de la ejecución.

Cada pieza resuelve un problema distinto.

1. Tabla payroll.calculation_run
Propósito

Representar una ejecución técnica de lanzamiento de nómina.

No es una payroll.
No es una raíz funcional de negocio.
No sustituye a payroll.payroll.

Sirve para:

trazabilidad operativa;
seguimiento desde backend y frontend;
resumen persistido del lanzamiento;
futura asincronía o paralelización.
Columnas propuestas
id bigint generated always as identity primary key
rule_system_code varchar(5) not null
payroll_period_code varchar(30) not null
payroll_type_code varchar(30) not null
calculation_engine_code varchar(50) not null
calculation_engine_version varchar(50) not null
requested_at timestamp not null
requested_by varchar(100) null
status varchar(30) not null
target_selection_json json not null
total_candidates integer not null default 0
total_eligible integer not null default 0
total_claimed integer not null default 0
total_skipped_not_eligible integer not null default 0
total_skipped_already_claimed integer not null default 0
total_calculated integer not null default 0
total_not_valid integer not null default 0
total_errors integer not null default 0
started_at timestamp null
finished_at timestamp null
summary_json json null
created_at timestamp not null default now()
updated_at timestamp not null default now()
Justificación
Contexto del run
rule_system_code
payroll_period_code
payroll_type_code
calculation_engine_code
calculation_engine_version

definen el marco operativo del lanzamiento.

target_selection_json

Se persiste en JSON porque representa la selección objetivo del launch y todavía no compensa fijar un modelo relacional complejo para todas sus variantes.

Contadores agregados

Se mantienen como columnas explícitas porque permiten:

seguimiento rápido;
respuesta de UI;
observabilidad del run;
resumen estable sin depender de una tabla hija por unidad.
summary_json

Se admite para detalles flexibles adicionales, sin forzar migraciones por cada refinamiento menor del resumen.

Estados recomendados del run
REQUESTED
RUNNING
COMPLETED
COMPLETED_WITH_ERRORS
FAILED
Restricciones recomendadas
checks de no negatividad en contadores
check (finished_at is null or started_at is not null)
Índices recomendados
(rule_system_code, payroll_period_code, payroll_type_code)
(status)
(requested_at desc)
2. Tabla payroll.calculation_claim
Propósito

Persistir la exclusión concurrente por unidad de cálculo.

Su misión es impedir que dos runs distintos procesen al mismo tiempo la misma unidad:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber
Columnas propuestas
id bigint generated always as identity primary key
run_id bigint not null
rule_system_code varchar(5) not null
employee_type_code varchar(30) not null
employee_number varchar(15) not null
payroll_period_code varchar(30) not null
payroll_type_code varchar(30) not null
presence_number integer not null
claimed_at timestamp not null
claimed_by varchar(100) null
FK recomendada
fk_calculation_claim_run
run_id -> payroll.calculation_run(id)
on delete cascade
Restricción clave

Debe existir una unique fuerte por la calculation key completa:

(rule_system_code, employee_type_code, employee_number, payroll_period_code, payroll_type_code, presence_number)
Regla de adquisición

La adquisición del claim debe ser atómica mediante insert.

No se acepta como patrón base:

leer si existe;
luego insertar.
Semántica de vida
si el insert entra, la unidad queda reclamada;
si falla por unique, la unidad ya está en curso en otro run;
al terminar de procesar la unidad, el claim se elimina.
Por qué no guardar status en claim

Porque claim no es una mini máquina de estados.
Su única misión es representar posesión exclusiva temporal de una unidad.

Índice recomendado
(run_id)
3. Tabla payroll.payroll_warning
Propósito

Persistir mensajes funcionales adheridos a una nómina concreta.

No representan incidencias del run, sino mensajes del resultado materializado.

Pueden incluir:

errores funcionales;
avisos;
observaciones;
cosas a revisar por usuario;
mensajes no bloqueantes pero relevantes.
Naturaleza semántica

Se adopta el término warning de forma deliberada para no encerrar la semántica en “error”.

El diseño debe permitir:

payroll NOT_VALID con warnings de severidad ERROR;
payroll CALCULATED con warnings de severidad WARNING;
payroll con mensajes informativos futuros.
Columnas propuestas
id bigint generated always as identity primary key
payroll_id bigint not null
warning_code varchar(50) not null
severity_code varchar(20) not null
message varchar(500) not null
details_json json null
FK recomendada
fk_payroll_warning_payroll
payroll_id -> payroll.payroll(id)
on delete cascade
Restricción única recomendada

No fijaría una unique demasiado agresiva de entrada.

Podría existir más de un warning con el mismo warning_code si en el futuro aparece necesidad de varias ocurrencias contextualizadas.
Si se quiere una deduplicación ligera, preferiría resolverla en dominio antes que forzarla ya en esquema.

Por qué no tiene created_at

Se decide explícitamente no añadir created_at.

Justificación:

payroll_warning nace y muere con la payroll.payroll;
el instante relevante ya está representado por payroll.calculated_at;
añadir otro timestamp duplicaría semántica sin aportar valor real.
Severidades recomendadas
INFO
WARNING
ERROR
Índices recomendados
(payroll_id)
opcionalmente (severity_code) si más adelante se consulta mucho por severidad
4. Tabla payroll.calculation_run_message
Propósito

Persistir mensajes del propio run.

Representa:

incidencias operativas;
errores técnicos;
descartes por claim;
descartes por no elegibilidad;
mensajes de ejecución no adheribles a una payroll concreta.

Ejemplos:

“unidad descartada por claim activo”
“unidad omitida por estado EXPLICIT_VALIDATED”
“error técnico en acceso a BD”
“fallo al resolver población”
“run completado con conflictos parciales”
Regla semántica

calculation_run_message no reemplaza a payroll_warning.

payroll_warning

mensaje funcional del resultado de nómina

calculation_run_message

mensaje operativo/técnico/de ejecución del run

Columnas propuestas
id bigint generated always as identity primary key
run_id bigint not null
message_code varchar(50) not null
severity_code varchar(20) not null
message varchar(500) not null
details_json json null
rule_system_code varchar(5) null
employee_type_code varchar(30) null
employee_number varchar(15) null
payroll_period_code varchar(30) null
payroll_type_code varchar(30) null
presence_number integer null
created_at timestamp not null default now()
FK recomendada
fk_calculation_run_message_run
run_id -> payroll.calculation_run(id)
on delete cascade
Justificación de la calculation key nullable

Se permite asociar un mensaje del run a:

una ejecución global;
o a una unidad concreta dentro del run.

Por eso la calculation key es nullable:

si el mensaje es global, queda vacía;
si el mensaje se refiere a una unidad concreta, se rellena.

Esto evita la necesidad de una tabla run_item obligatoria por cada unidad.

Severidades recomendadas
INFO
WARNING
ERROR
Índices recomendados
(run_id)
(run_id, severity_code)
opcionalmente (run_id, employee_type_code, employee_number) si más adelante se necesita drill-down por empleado
5. Relación con payroll.payroll
Regla estructural

payroll.payroll permanece como resultado materializado y no absorbe campos de launch, run, claim ni mensajes operativos.

No se deben añadir a payroll.payroll cosas como:

estado del run;
claim status;
resumen de ejecución;
mensajes técnicos del launch.
Lo que sí absorbe

Sí absorbe:

su estado funcional (status);
su razón (statusReasonCode);
y sus payroll_warning.

Esto mantiene coherente la separación entre:

resultado de negocio materializado
ejecución técnica que lo produjo
6. Decisión explícita sobre calculation_run_item
Decisión

Se decide no introducir payroll.calculation_run_item como tabla base obligatoria.

Justificación
1. Volumen

Generaría una fila por unidad y por run, con mucho crecimiento potencial para poco valor si la mayoría de unidades se comportan normalmente.

2. Relevancia temporal

Una vez existe un nuevo run sobre la misma unidad, buena parte del detalle fino del item anterior pierde valor operativo.

3. Errores funcionales

Los errores funcionales importantes pertenecen a la nómina materializada y deben vivir en payroll.payroll mediante payroll_warning, no en el run.

4. Observabilidad suficiente para V1

La combinación de:

calculation_run
calculation_claim
calculation_run_message
payroll_warning

proporciona una observabilidad suficientemente rica sin necesidad de una tabla hija obligatoria por unidad.

Evolución futura posible

No se prohíbe introducir calculation_run_item más adelante si la observabilidad operativa futura lo justifica.

Pero no forma parte del núcleo inicial.

7. JSON vs relacional
JSON permitido

Se admite JSON en:

target_selection_json
summary_json
details_json de warnings
details_json de run messages

porque ahí la variación todavía no compensa fijarla toda en columnas.

Relacional obligatorio

Se exige modelado relacional explícito en:

calculation key del claim
contexto base del run
estado del run
referencias payroll/run
contadores agregados

porque esas piezas sí son núcleo estable del diseño.

8. Restricciones e índices recomendados completos
calculation_run
pk (id)
índices:
(rule_system_code, payroll_period_code, payroll_type_code)
(status)
(requested_at desc)
calculation_claim
pk (id)
fk run_id -> calculation_run(id) on delete cascade
unique:
calculation key completa
índice:
(run_id)
payroll_warning
pk (id)
fk payroll_id -> payroll(id) on delete cascade
índice:
(payroll_id)
calculation_run_message
pk (id)
fk run_id -> calculation_run(id) on delete cascade
índices:
(run_id)
(run_id, severity_code)
9. Qué se rechaza explícitamente

Se rechaza en este modelo físico:

usar solo la unique de payroll.payroll como solución de concurrencia;
meter mensajes técnicos del run dentro de payroll.payroll;
convertir calculation_claim en una tabla de workflow compleja;
introducir calculation_run_item por inercia sin haber demostrado valor real;
fijar ya el contrato definitivo del motor de cálculo;
acoplar launch al endpoint stub actual de calculate, que sigue siendo temporal y no canónico
10. Consecuencias
Positivas
separación muy limpia entre resultado, mensajes funcionales, ejecución y concurrencia;
menos volumen estructural que con una tabla obligatoria de run items;
observabilidad suficiente para V1;
posibilidad de seguimiento desde frontend;
motor real desacoplado del workflow;
base sólida para meses de evolución.
Costes
añade cuatro tablas nuevas respecto al payroll root original;
obliga a distinguir bien mensajes funcionales vs mensajes de run;
deja para una fase futura el drill-down total por unidad si algún día se necesita.
11. Estrategia de implementación recomendada
Fase 1
crear calculation_run
crear calculation_claim
crear payroll_warning
crear calculation_run_message
Fase 2
implementar launch síncrono
persistir run + summary + messages
mantener calculate como caso de uso interno
Fase 3
exponer lectura de runs y mensajes
permitir seguimiento desde frontend
Fase 4
revaluar si la observabilidad futura justifica calculation_run_item
añadir housekeeping de claims si hace falta
introducir paralelización real
12. Resumen ejecutivo

Se adopta para payroll un modelo físico donde:

payroll.payroll sigue siendo el resultado materializado;
payroll.payroll_warning concentra mensajes funcionales adheridos a la nómina;
payroll.calculation_run representa el launch persistido;
payroll.calculation_claim garantiza exclusión concurrente por unidad;
payroll.calculation_run_message concentra mensajes operativos/técnicos del run;
calculation_run_item no forma parte del núcleo inicial;
el sistema queda preparado para diseñar launch sin acoplarlo prematuramente al motor real.
<!-- END FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md -->


---

# FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md
<a name="file-adr-032-payroll-launch-workflow--s-ncrono--con-run-persistido-y-claims-por-unidad--estado-md"></a>

<!-- BEGIN FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md -->

ADR — Payroll Launch Workflow (síncrono, con run persistido y claims por unidad)
Estado

Propuesto

Contexto

El bounded context payroll ya dispone de:

payroll.payroll como resultado materializado de una unidad de cálculo, identificado por:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber
payroll.calculation_run como persistencia del lanzamiento técnico;
payroll.calculation_claim como exclusión concurrente por unidad;
payroll.payroll_warning para mensajes funcionales adheridos a la nómina;
payroll.calculation_run_message para mensajes operativos o técnicos del run.

También se ha fijado ya que:

una unidad es elegible para cálculo si no existe nómina previa o si existe y está NOT_VALID;
una nómina en CALCULATED, EXPLICIT_VALIDATED o DEFINITIVE no debe recalcularse automáticamente;
el endpoint actual POST /payrolls/calculate sigue siendo un stub temporal de validación del pipeline y no el contrato final del motor real.

El proyecto, además, exige que cuando una operación no encaja como CRUD plano se modele como workflow explícito y que el naming refleje semántica de negocio real.

Problema

Se necesita implementar el launch de nómina como workflow real, de forma que:

reciba un contexto de lanzamiento;
resuelva una población objetivo;
la expanda a unidades reales de cálculo;
filtre elegibilidad;
adquiera claims por unidad de forma segura;
invoque el calculador interno;
actualice el run persistido;
deje trazabilidad suficiente para consulta posterior.

Todo esto debe hacerse sin:

convertir el launch en motor de cálculo;
acoplarlo al endpoint público stub actual;
fijar todavía el contrato del motor real de reglas;
introducir asincronía o paralelización real en la primera iteración.
Decisión

Se adopta un Payroll Launch Workflow síncrono, con estas características:

crea un calculation_run persistido;
resuelve la población objetivo;
expande a unidades de cálculo;
determina elegibilidad;
intenta adquirir claim por cada unidad elegible;
invoca un caso de uso interno de cálculo por unidad;
actualiza contadores y estado del run;
registra mensajes del run cuando proceda;
devuelve runId y resumen del resultado.
Principio madre

Launch coordina.
Calculate materializa.
Claim excluye.
Run resume.

Unidad mínima de cálculo

La unidad mínima de cálculo queda fijada como:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber

Esta unidad coincide con la identidad funcional de payroll.payroll y con la semántica ya fijada del dominio.

Input del launch

El launch debe recibir al menos:

ruleSystemCode
payrollPeriodCode
payrollTypeCode
calculationEngineCode
calculationEngineVersion
targetSelection
targetSelection

Debe permitir al menos estas variantes iniciales:

un empleado concreto
una lista explícita de empleados
una selección masiva simple dentro de un ruleSystemCode

No se fija aún un DSL complejo de filtros.

Output del launch

El launch debe devolver:

runId
status
contadores agregados
timestamps principales
opcionalmente resumen

Y el sistema debe permitir consultar después el run persistido.

Flujo del launch
1. Crear run

Persistir calculation_run en estado:

REQUESTED

con:

contexto de ejecución
targetSelectionJson
contadores a cero
2. Cambiar a RUNNING

Al comenzar la ejecución real:

status = RUNNING
startedAt = now
3. Resolver población objetivo

Transformar targetSelection en empleados concretos.

4. Expandir a unidades de cálculo

Por cada empleado objetivo, resolver las presences relevantes para:

payrollPeriodCode
payrollTypeCode

y generar unidades explícitas:

empleado + periodo + tipo + presencia
5. Determinar elegibilidad

Una unidad es elegible si:

no existe payroll.payroll, o
existe y está NOT_VALID

Si existe y está en:

CALCULATED
EXPLICIT_VALIDATED
DEFINITIVE

la unidad no es elegible.

Estas unidades no elegibles:

no se calculan
incrementan totalSkippedNotEligible
pueden generar calculation_run_message cuando compense
6. Intentar adquirir claim

Por cada unidad elegible:

intentar insertar calculation_claim

Si el insert:

entra: la unidad queda reclamada por este run
falla por unique: la unidad ya está en curso en otro run

Si está ya reclamada:

no se calcula
incrementa totalSkippedAlreadyClaimed
se registra calculation_run_message con contexto de unidad
7. Invocar cálculo interno

Para cada unidad con claim adquirido:

invocar un caso de uso interno de cálculo por unidad

No debe hacerse HTTP interno contra el endpoint stub actual.

8. Interpretar resultado

El cálculo interno puede producir:

CALCULATED
NOT_VALID
error técnico

Entonces:

CALCULATED incrementa totalCalculated
NOT_VALID incrementa totalNotValid
error técnico incrementa totalErrors y genera calculation_run_message
9. Liberar claim

El claim de la unidad debe eliminarse al terminar su procesamiento, tanto si sale bien como si falla.

10. Cerrar run

Al finalizar todas las unidades:

finishedAt = now
status = COMPLETED si no hubo errores técnicos
status = COMPLETED_WITH_ERRORS si hubo errores técnicos parciales
status = FAILED solo si el launch falla globalmente antes de completar su ciclo mínimo
Política de concurrencia
Regla principal

La concurrencia se gobierna exclusivamente mediante payroll.calculation_claim.

La unique de payroll.payroll sigue siendo una defensa final de integridad, pero no es el mecanismo principal de coordinación.

Regla operativa

Dos launches simultáneos:

pueden coexistir;
no pueden procesar al mismo tiempo la misma unidad de cálculo.
Implementación base

La adquisición del claim se hace con insert atómico sobre la calculation key completa.

Naturaleza del cálculo interno

El launch no debe depender del endpoint público stub actual.

Debe usar un caso de uso interno del estilo:

CalculatePayrollUnitUseCase

o naming equivalente, orientado a negocio y no a detalle técnico, siguiendo la guía de naming del proyecto.

Responsabilidad del cálculo interno
materializar una unidad explícita
crear/reemplazar payroll.payroll según reglas ya fijadas
persistir payroll_warning cuando proceda
devolver resultado funcional de la unidad
Lo que no hace
no resuelve población
no gestiona claims
no crea runs
no resume progreso global
Mensajes del run

calculation_run_message se usa para:

errores técnicos;
unidades omitidas por claim;
unidades omitidas por no elegibilidad cuando interese dejar rastro;
problemas de resolución de población;
incidencias globales del launch.

No debe usarse para modelar errores funcionales propios de la nómina.
Esos pertenecen a payroll_warning.

Severidades recomendadas

Para calculation_run_message:

INFO
WARNING
ERROR

Para payroll_warning:

INFO
WARNING
ERROR

La diferencia no está en la severidad, sino en la pertenencia semántica:

run
vs payroll
Primera iteración aceptada

La primera iteración del launch será:

síncrona
secuencial
con run persistido
con claims por unidad
con cálculo interno por unidad
sin paralelización real
sin asincronía
sin workers
Justificación

Esto permite validar:

semántica
integración
counters
exclusión concurrente
wiring

sin abrir todavía el melón del motor real ni de la ejecución distribuida.

API pública recomendada
Crear launch

POST /payroll/calculation-runs/launch

Leer run

GET /payroll/calculation-runs/{runId}

Lecturas futuras opcionales
listar runs recientes
listar mensajes de run

No se considera canónico exponer todavía el cálculo interno como API pública definitiva.

Qué se rechaza explícitamente

Se rechaza:

que launch invoque por HTTP al mismo backend como arquitectura permanente;
que launch haga de motor de cálculo;
que calculate resuelva población objetivo;
que la concurrencia se gobierne solo por la unique de payroll.payroll;
que el endpoint stub actual de calculate se tome como contrato final del motor;
introducir ya paralelización real o asincronía obligatoria.
Consecuencias
Positivas
launch claro y desacoplado
concurrencia segura por unidad
run consultable desde frontend
base sana para paralelización futura
separación nítida entre ejecución y resultado
Costes
más wiring en aplicación
gestión explícita de claims
necesidad de mantener contadores y estados del run
Resumen ejecutivo

Se adopta un launch síncrono y secuencial que:

crea un calculation_run
resuelve población
expande a unidades
filtra elegibilidad
adquiere calculation_claim
invoca un cálculo interno por unidad
actualiza contadores y estado del run
registra mensajes del run

Todo ello sin acoplar todavía el workflow al motor real de nómina.
<!-- END FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md -->


---

# FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md
<a name="file-adr-033-payrollobject-como-ra-z-metamodelo-can-nica-del-motor-n-mina-md"></a>

<!-- BEGIN FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md -->

ADR — PayrollObject como raíz metamodelo canónica del motor de nómina
Estado

Propuesto

Contexto

El diseño del motor de nómina de B4RRHH está empezando a consolidarse alrededor de varios tipos de elementos configurables del dominio de payroll.

Inicialmente, la conversación se ha centrado en los conceptos de nómina, pero rápidamente han aparecido también otros candidatos naturales del mismo espacio funcional, como:

tablas
constantes
futuros objetos auxiliares o parametrizables del motor

Si el modelo parte directamente de payrollConcept como raíz, existe el riesgo de:

sobredimensionar el concepto de nómina para que absorba responsabilidades que no le pertenecen
acabar creando metamodelos paralelos inconsistentes para tablas, constantes y otros elementos
mezclar identidad común con semántica específica de un subtipo concreto

Por tanto, antes de profundizar en el modelado específico de los conceptos, es necesario fijar una raíz metamodelo común para todos los objetos configurables del motor.

Decisión

Se introduce PayrollObject como raíz metamodelo canónica del motor de nómina.

Todo elemento configurable del metamodelo de payroll deberá modelarse primero como un PayrollObject, con una identidad funcional común basada en business keys.

La business key canónica de PayrollObject será:

ruleSystemCode
objectTypeCode
objectCode

PayrollObject actuará como raíz común para distintos tipos de objeto del dominio payroll, incluyendo al menos:

CONCEPT
TABLE
CONSTANT

El atributo canónico de identidad del objeto será objectCode.

Cuando se trabaje dentro de un subtipo concreto, podrán usarse alias semánticos de contexto, por ejemplo:

conceptCode
tableCode
constantCode

Sin embargo, esos nombres no definen identidades alternativas ni nuevas business keys. Son únicamente proyecciones semánticas del mismo objectCode dentro del contexto de cada subtipo.

Consecuencias
Positivas
Se fija una raíz común clara para el metamodelo del motor de nómina.
Se evita construir un modelo demasiado centrado exclusivamente en conceptos.
Se facilita la incorporación futura de tablas, constantes y otros objetos parametrizables sin rediseñar la base del modelo.
Se mantiene coherencia con las reglas generales de B4RRHH, donde la identidad pública se expresa mediante business keys funcionales y no mediante IDs técnicos.
Se separa correctamente la identidad común del objeto de la semántica específica de cada subtipo.
Costes o limitaciones
Obliga a introducir una capa de abstracción adicional antes de modelar los subtipos concretos.
Requiere disciplina para no contaminar el modelo raíz con propiedades específicas de PayrollConcept u otros tipos.
Puede parecer más abstracto al inicio que arrancar directamente desde payrollConcept, aunque a medio plazo reduce deuda semántica.
No objetivos

Este ADR no define todavía:

las propiedades específicas de PayrollConcept
el modelo de versionado de los objetos de payroll
la estrategia de cálculo
la segmentación intrames
las reglas de cálculo ni su representación
la implementación física en base de datos o APIs

Este ADR solo fija la raíz metamodelo común y su identidad funcional.

Resumen ejecutivo

El motor de nómina de B4RRHH no se modelará partiendo directamente de conceptos aislados, sino desde una raíz metamodelo común llamada PayrollObject.

La identidad funcional canónica será:

ruleSystemCode
objectTypeCode
objectCode

Los subtipos como PayrollConcept, PayrollTable o PayrollConstant heredarán esa identidad común.

Los nombres como conceptCode o tableCode se consideran alias semánticos contextuales del objectCode, no nuevas business keys.
<!-- END FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md -->


---

# FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md
<a name="file-adr-034-modelo-sem-ntico-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md -->

# ADR-034 — Modelo semántico de `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-033 introduce `PayrollObject` como raíz metamodelo canónica del motor de nómina y fija su identidad funcional común mediante la business key:

- `ruleSystemCode`
- `objectTypeCode`
- `objectCode`

Dentro de ese metamodelo común, el tipo de objeto `CONCEPT` requiere un modelo semántico propio que permita distinguir claramente:

- la identidad común heredada de `PayrollObject`
- la naturaleza estable del concepto de nómina
- las características mutables o versionables (definidas en ADRs posteriores)

Sin esta separación, existe el riesgo de mezclar en un mismo nivel:

- identidad
- tipo de cálculo
- presentación en recibo
- parámetros de cálculo
- fuentes de datos
- efectos funcionales

Esto dificultaría la trazabilidad, la retroactividad y la comprensión funcional del sistema de nómina.

El proyecto B4RRHH sigue principios claros:

- uso de business keys funcionales
- separación entre identidad estable y detalle mutable
- naming orientado a negocio
- evitar IDs técnicos en APIs

---

## Decisión

Se introduce `PayrollConcept` como subtipo semántico de `PayrollObject`.

`PayrollConcept` **no define una business key propia**.  
Hereda la identidad canónica de `PayrollObject`:

- `ruleSystemCode`
- `objectTypeCode`
- `objectCode`

Cuando `objectTypeCode = CONCEPT`, `objectCode` podrá nombrarse como `conceptCode` a nivel semántico, sin crear una nueva identidad.

---

## Propiedades maestras de `PayrollConcept`

Las siguientes propiedades definen la **naturaleza semántica estable** del concepto:

- `conceptMnemonic`
- `calculationType`
- `functionalNature`
- `resultCompositionMode`
- `payslipOrderCode`

El modelo queda preparado para incorporar en el futuro:

- `functionalSubnature` (clasificación funcional secundaria)

---

## Significado de las propiedades

### `conceptMnemonic`
Alias semántico legible del concepto.

Uso:
- reglas
- documentación
- trazabilidad
- debugging

No forma parte de la business key.

---

### `calculationType`
Define la naturaleza del cálculo del concepto.

Ejemplos:

- `DIRECT_AMOUNT`
- `QUANTITY_BY_RATE`
- `PRESENCE_VALUED`
- `AGGREGATE`
- `TECHNICAL_DERIVED`

---

### `functionalNature`
Define el papel funcional dentro de la nómina.

Valores iniciales:

- `EARNING`
- `DEDUCTION`
- `EMPLOYER_CHARGE`
- `BASE`
- `TOTAL`
- `TECHNICAL`

---

### `resultCompositionMode`
Define cómo se combinan múltiples resultados parciales del concepto dentro de una nómina.

Evita asumir que siempre debe existir una única línea final.

---

### `payslipOrderCode`

Define la posición lógica en el recibo.

Reglas:

- `NULL` → no se muestra en recibo
- valor informado → se muestra

Ordenación:


payslipOrderCode + objectCode


Sustituye conceptualmente a un `visibleInPayslip`.

---

## Regla crítica de inmutabilidad

`calculationType` es **inmutable**.

Si un concepto cambia su naturaleza de cálculo:

➡️ **NO se versiona**  
➡️ **Se crea un concepto nuevo**

Motivo:

- coherencia histórica
- retroactividad fiable
- trazabilidad clara
- comprensión funcional

---

## Consecuencias

### Positivas

- separación clara entre identidad y semántica
- modelo más robusto frente a cambios
- mejor trazabilidad y retro
- base sólida para evolución futura
- coherencia con arquitectura B4RRHH

---

### Costes

- algunos cambios requieren nuevos conceptos
- mayor disciplina de modelado
- separación más estricta entre semántica y parametrización

---

## No objetivos

Este ADR **NO define**:

- versionado de conceptos
- reglas de cálculo
- segmentación intrames
- relaciones con tablas o constantes
- implementación en BBDD
- APIs

---

## Resumen ejecutivo

`PayrollConcept` es un subtipo semántico de `PayrollObject` sin identidad propia adicional.

Su núcleo estable está formado por:

- `conceptMnemonic`
- `calculationType`
- `functionalNature`
- `resultCompositionMode`
- `payslipOrderCode`

`calculationType` es inmutable.

Los cambios en la naturaleza del concepto implican la creación de un nuevo concepto.
<!-- END FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md -->


---

# FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md
<a name="file-adr-036-tipolog-as-can-nicas-de-c-lculo-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md -->

# ADR-036 — Tipologías canónicas de cálculo de `PayrollConcept`

## Estado
Aceptado

---

## Contexto

El proyecto B4RRHH define un motor de nómina basado en un metamodelo de objetos (`PayrollObject`), donde los conceptos de nómina (`PayrollConcept`) representan unidades funcionales de cálculo dentro de una nómina.

Una de las decisiones clave del motor es evitar implementar lógica específica por concepto mediante código, y en su lugar permitir que los conceptos se configuren a partir de un conjunto limitado de tipologías de cálculo y reglas de composición.

Sin una tipología clara:

- el sistema tendería a crecer mediante lógica específica por concepto;
- se perdería la capacidad de configuración;
- aumentaría la deuda técnica;
- se dificultaría la trazabilidad y la retroactividad.

Por tanto, es necesario definir un conjunto reducido, estable y expresivo de **tipos de cálculo canónicos** que cubran la mayoría de casos reales sin inflar el modelo.

---

## Decisión

Se definen las siguientes tipologías canónicas de cálculo para `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Cada tipo representa una **forma fundamental de cálculo**, no un caso de negocio concreto.

---

## Principio rector

El tipo de cálculo describe el **operador principal** del concepto.

No describe:
- el origen de los datos;
- la semántica concreta del concepto (ej. “salario base”, “IRPF”);
- ni la forma específica en que se obtienen sus operandos.

---

## Tipologías definidas

### 1. `DIRECT_AMOUNT`

#### Definición
El resultado del concepto es un importe directo ya resuelto.

#### Forma general

resultado = amount


#### Características
- No depende de otros operandos estructurados.
- Representa un valor final ya calculado o informado.

#### Ejemplos
- ajuste manual
- plus fijo mensual
- cuantía fija por tabla
- regularización directa

---

### 2. `RATE_BY_QUANTITY`

#### Definición
El resultado del concepto se obtiene como el producto de una cantidad por un precio.

#### Forma general

resultado = quantity × rate


#### Características
- Generaliza múltiples casos de negocio:
  - días × precio día
  - horas × precio hora
  - unidades × tarifa
- No define cómo se obtienen `quantity` ni `rate`.

#### Ejemplos
- salario base diario
- horas extra
- plus por día trabajado
- dietas
- kilometraje

#### Nota importante
Conceptos tradicionalmente considerados como “basados en presencia” se modelan como casos particulares de este tipo, donde la cantidad representa días computables.

---

### 3. `PERCENTAGE`

#### Definición
El resultado del concepto se obtiene aplicando un porcentaje sobre una base.

#### Forma general

resultado = base × percentage


#### Características
- Separa claramente la base del porcentaje.
- No define cómo se obtiene el porcentaje.

#### Ejemplos
- cotización a la seguridad social
- IRPF
- complementos porcentuales

#### Nota importante
Incluso cuando el porcentaje se obtiene mediante lógica compleja (ej. IRPF), el concepto sigue perteneciendo a esta tipología.  
La complejidad se desplaza a la obtención del porcentaje, no al tipo de cálculo.

---

### 4. `AGGREGATE`

#### Definición
El resultado del concepto se obtiene combinando resultados de otros conceptos ya calculados, pudiendo invertir el signo de cada contribuyente individualmente.

#### Forma general

resultado = SUM(feed_i × sign_i)

donde `sign_i` es +1 si la relación de feed tiene `invert_sign = false`, y −1 si tiene `invert_sign = true`.

#### El flag `invert_sign` en la relación de feed

El flag `invert_sign` reside en la **relación de feed** (no en el concepto fuente). Esto permite que un mismo concepto contribuya positivamente a un agregado y negativamente a otro:

| Concepto fuente       | Feeds aggregate | `invert_sign` | Efecto         |
|-----------------------|-----------------|---------------|----------------|
| 101 SALARIO_BASE      | 970             | false         | + importe      |
| 101 SALARIO_BASE      | 990             | false         | + importe      |
| concepto deducción X  | 980             | false         | + importe      |
| concepto deducción X  | 990             | true          | − importe      |

#### Modelo de grafo plano (flat graph)

Los conceptos hoja alimentan **directamente** tanto su agregado lateral (devengos o deducciones) como el agregado de líquido neto (990). El concepto 990 **nunca depende de 970 ni de 980**; agrega los mismos conceptos hoja que ellos.

```
EARNING leaf ──────────►  970 (invert=false)
                    └────►  990 (invert=false)

DEDUCTION leaf ─────────►  980 (invert=false)
                    └────►  990 (invert=true)
```

Esto evita dependencias en cascada entre agregados y garantiza que el grafo de cálculo sea siempre un DAG sin nodos intermedios de agregado encadenados.

#### Activación explícita

Los conceptos AGGREGATE **no se activan automáticamente** por el hecho de que sus fuentes estén activadas. Requieren una fila explícita en `payroll_object_activation`, igual que el resto de conceptos.

#### Características
- No opera sobre datos primarios, sino sobre resultados previos.
- Representa composición o acumulación con signo controlado a nivel de relación.

#### Ejemplos
- total devengos (970)
- total deducciones (980)
- líquido a pagar / neto (990)
- bases de cotización
- bases fiscales

#### Nota importante
La estrategia de agregación con signo queda definida aquí. La forma de registrar las relaciones de feed y persistirlas se trata en los ADRs de infraestructura del motor de nómina.

---

## `FunctionalNature` para conceptos agregados totales

Los conceptos de tipo `AGGREGATE` que representan totales de nómina reciben valores específicos en el enum `FunctionalNature` para que el frontend pueda distinguirlos de las líneas de detalle al renderizar el recibo de salario.

Se añaden los siguientes valores al enum:

| Valor              | Semántica                                      | Concepto típico |
|--------------------|------------------------------------------------|-----------------|
| `TOTAL_EARNING`    | Suma de todos los devengos                     | 970             |
| `TOTAL_DEDUCTION`  | Suma de todas las deducciones                  | 980             |
| `NET_PAY`          | Líquido a pagar (devengos − deducciones)       | 990             |

Estos tres valores coexisten con los valores preexistentes (`EARNING`, `DEDUCTION`, `BASE`, `INFORMATIONAL`).

La `FunctionalNature` es un atributo de presentación/semántica del concepto; no altera la lógica de cálculo. Un concepto con `calculationType = AGGREGATE` y `functionalNature = NET_PAY` ejecuta exactamente la misma operación `SUM(feed_i × sign_i)` que cualquier otro AGGREGATE.

---

## Reglas de diseño

### 1. Minimalismo tipológico

No se crearán nuevos tipos de cálculo por cada caso de negocio frecuente.

Ejemplo descartado:
- `PRESENCE_BASED`

Motivo:
- no representa una operación distinta;
- describe una forma de obtener un operando (`quantity`), no un tipo de cálculo.

---

### 2. Separación de responsabilidades

Se separan claramente:

- tipo de cálculo → define la operación
- resolución de operandos → define de dónde salen los datos

Esta separación es fundamental para:

- evitar explosión de tipos;
- permitir configuración;
- facilitar reutilización.

---

### 3. Composicionalidad

Los tipos de cálculo deben permitir que los operandos provengan de resultados de otros conceptos.

Esto habilita:

- conceptos técnicos intermedios;
- cadenas de cálculo reutilizables;
- construcción incremental del resultado de nómina.

---

### 4. Inmutabilidad del tipo

El `calculationType` de un `PayrollConcept` es inmutable.

#### Consecuencia
Si un concepto cambia su naturaleza de cálculo:
- no se versiona;
- se crea un nuevo concepto.

#### Motivación
- preservar coherencia histórica;
- evitar ambigüedad semántica;
- simplificar retroactividad.

---

## Consecuencias

### Positivas

- modelo estable y predecible;
- reducción drástica de lógica específica por concepto;
- alta capacidad de configuración;
- base sólida para evolución del motor;
- alineación con arquitectura hexagonal y metamodelo del proyecto.

---

### Costes

- necesidad de modelar correctamente operandos y sources;
- mayor esfuerzo inicial de diseño;
- algunos casos complejos requerirán conceptos técnicos adicionales en lugar de lógica directa.

---

## No objetivos

Este ADR no define:

- cómo se resuelven los operandos (`sources`);
- cómo se versionan las reglas;
- el orden de ejecución de los conceptos;
- el modelo de persistencia;
- la API de configuración.

---

## Resumen ejecutivo

Se establece un conjunto mínimo y completo de tipologías de cálculo para `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Estas tipologías representan las formas fundamentales de cálculo del motor y permiten modelar la mayoría de los conceptos de nómina mediante configuración, sin necesidad de lógica específica por concepto.

El modelo se apoya en la separación entre:

- operación (tipo de cálculo)
- resolución de datos (operandos)

lo que habilita un motor flexible, composicional y extensible.
<!-- END FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md -->


---

# FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md
<a name="file-adr-037-sources-y-resoluci-n-de-operandos-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md -->

Vamos a por el siguiente bloque clave. Este ADR es el que convierte las tipologías en motor real configurable.

# ADR-037 — Sources y resolución de operandos en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-036 define las tipologías canónicas de cálculo de `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Estas tipologías describen únicamente la **forma del cálculo**, pero no especifican:

- de dónde provienen los valores necesarios;
- cómo se resuelven los operandos en tiempo de ejecución.

Sin una capa explícita de resolución de operandos:

- el sistema tendería a introducir lógica específica por concepto;
- se perdería configurabilidad;
- se dificultaría la reutilización;
- aumentaría el acoplamiento entre cálculo y origen de datos.

Por tanto, es necesario definir un modelo claro de **sources de operandos** que permita desacoplar completamente:

- la operación (tipo de cálculo)
- el origen de los datos

---

## Decisión

Se introduce el concepto de **source de operando**, que define el origen del valor utilizado en un cálculo.

Cada operando de un `PayrollConcept` se resuelve mediante:

- un `sourceType`
- una referencia asociada (según el tipo)

---

## Principio rector

Un operando no contiene un valor directo, sino una **instrucción de resolución**.

---

## Sources canónicos iniciales

Se definen los siguientes tipos de source:

- `INPUT`
- `CONSTANT`
- `TABLE`
- `CONCEPT`
- `EMPLOYEE_DATA`
- `PERIOD_DATA`
- `SEGMENT_DATA`

---

## Definición de cada source

### 1. `INPUT`

#### Descripción
Valor informado externamente para el cálculo.

#### Ejemplos
- horas extra introducidas
- unidades manuales
- importes excepcionales

#### Uso típico
- `quantity`
- `amount`

---

### 2. `CONSTANT`

#### Descripción
Valor fijo parametrizado en el sistema.

#### Ejemplos
- importe fijo mensual
- porcentaje fijo
- divisor estándar (ej. 30)

#### Uso típico
- `rate`
- `percentage`
- `amount`

---

### 3. `TABLE`

#### Descripción
Valor obtenido a partir de una tabla parametrizada.

#### Ejemplos
- salario por categoría
- tarifa por hora
- porcentaje por tramo

#### Uso típico
- `rate`
- `percentage`
- `amount`

---

### 4. `CONCEPT`

#### Descripción
Valor obtenido a partir del resultado de otro `PayrollConcept`.

#### Ejemplos
- `BASE_CC`
- `BASE_IRPF`
- `DIAS_PRESENCIA`
- `PRECIO_DIA`

#### Uso típico
- cualquier operando

#### Nota clave
Este source habilita la **composición del motor**, permitiendo construir cadenas de cálculo reutilizables.

---

### 5. `EMPLOYEE_DATA`

#### Descripción
Dato estructural del empleado.

#### Ejemplos
- porcentaje de jornada
- categoría profesional
- tipo de contrato

#### Uso típico
- inputs para tablas
- cálculo de valores derivados

---

### 6. `PERIOD_DATA`

#### Descripción
Dato asociado al período completo de cálculo.

#### Ejemplos
- días del mes
- año/mes
- número de pagas

#### Uso típico
- `quantity`
- cálculos base

---

### 7. `SEGMENT_DATA`

#### Descripción
Dato asociado a un tramo homogéneo de cálculo dentro del período.

#### Ejemplos
- días del segmento
- jornada vigente en el segmento
- condiciones activas en el tramo

#### Uso típico
- `quantity`
- cálculos intraperiodo

---

## Resolución de operandos por tipo de cálculo

---

### `DIRECT_AMOUNT`

#### Operando
- `amount`

#### Sources permitidos
- `INPUT`
- `CONSTANT`
- `TABLE`
- `CONCEPT`

---

### `RATE_BY_QUANTITY`

#### Operandos
- `quantity`
- `rate`

#### `quantitySource`
- `INPUT`
- `CONCEPT`
- `PERIOD_DATA`
- `SEGMENT_DATA`

#### `rateSource`
- `CONSTANT`
- `TABLE`
- `CONCEPT`

---

### `PERCENTAGE`

#### Operandos
- `base`
- `percentage`

#### `baseSource`
- `CONCEPT`
- `PERIOD_DATA`
- `SEGMENT_DATA`

#### `percentageSource`
- `CONSTANT`
- `TABLE`
- `CONCEPT`

---

### `AGGREGATE`

#### Operando
- `membership`

#### Nota
La resolución de miembros no se modela como source, sino mediante estrategias de agregación definidas en ADR posterior.

---

## Regla fundamental

Los tipos de cálculo **no contienen lógica de negocio específica**, sino que delegan completamente la obtención de valores en los sources.

---

## Composicionalidad del motor

El uso de `CONCEPT` como source permite:

- construir conceptos técnicos reutilizables;
- encadenar cálculos;
- separar lógica compleja en piezas simples;
- mejorar trazabilidad y debugging.

---

## Ejemplo conceptual

### Salario base


quantity = CONCEPT(DIAS_PRESENCIA)
rate = CONCEPT(PRECIO_DIA)
resultado = quantity × rate


---

### IRPF


base = CONCEPT(BASE_IRPF)
percentage = CONCEPT(TIPO_IRPF_EFECTIVO)
resultado = base × percentage


---

## Consecuencias

### Positivas

- desacoplamiento total entre cálculo y origen de datos;
- alta configurabilidad;
- reutilización de lógica;
- facilidad para introducir conceptos técnicos;
- base para motor declarativo.

---

### Costes

- necesidad de definir correctamente catálogo de conceptos técnicos;
- mayor complejidad conceptual inicial;
- necesidad de validaciones fuertes entre tipos y sources.

---

## No objetivos

Este ADR no define:

- modelo físico de persistencia de sources;
- resolución concreta de tablas;
- implementación de motor de cálculo;
- orden de ejecución de conceptos;
- versionado de parámetros.

---

## Resumen ejecutivo

Se define un modelo de resolución de operandos basado en sources tipados.

Cada operando de un concepto se resuelve mediante un source, desacoplando completamente:

- el tipo de cálculo
- el origen de los datos

Este modelo permite construir un motor composicional, reutilizable y altamente con
<!-- END FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md -->


---

# FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md
<a name="file-adr-038-estrategias-de-agregaci-n-y-relaciones-de-alimentaci-n-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md -->

# ADR-038 — Estrategias de agregación y relaciones de alimentación en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-036 define las tipologías canónicas de cálculo:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

El ADR-037 define la resolución de operandos mediante sources tipados.

Sin embargo, el tipo `AGGREGATE` requiere una definición adicional:

- cómo se determinan los conceptos que participan en el agregado;
- dónde reside la responsabilidad de dicha pertenencia;
- cómo evitar modelos frágiles basados en listas manuales.

En nómina real existen dos patrones claramente diferenciados:

1. Bases o acumulados donde cada concepto decide si participa  
2. Totales o subtotales donde la pertenencia se deriva automáticamente

Es necesario modelar ambas realidades sin mezclarlas.

---

## Decisión

Se definen dos estrategias canónicas de agregación para `AGGREGATE`:

- `FEED_BY_SOURCE`
- `SELECT_BY_RULE`

Estas estrategias determinan cómo se construye el conjunto de miembros del agregado.

---

## Principio rector

La pertenencia a un agregado puede definirse:

- desde el concepto origen (semántica declarativa del concepto)
- o desde el agregado destino (regla de selección)

Ambas aproximaciones son necesarias y no son equivalentes.

---

## Definición de `AGGREGATE`

Un `AGGREGATE` es un concepto cuyo resultado se obtiene combinando resultados de otros conceptos ya calculados.

### Operación inicial soportada
- `SUM`

---

## Estrategias de membership

---

### 1. `FEED_BY_SOURCE`

#### Definición
La pertenencia al agregado se declara en el concepto origen.

#### Modelo conceptual
Cada concepto define a qué agregados alimenta.

#### Ejemplo
- `SALARIO_BASE` alimenta `BASE_CC`
- `PLUS_TRANSPORTE` no alimenta `BASE_CC`
- `PRORRATA_EXTRA` alimenta `BASE_IRPF`

---

#### Motivación

La semántica relevante en muchos casos pertenece al concepto:

- cotiza / no cotiza
- tributa / no tributa
- alimenta base / no alimenta

Esta información es intrínseca al concepto, no al agregado.

---

#### Resolución en runtime

Para calcular un agregado:

1. se evalúan todos los conceptos;
2. se seleccionan aquellos con relación activa hacia el target;
3. se combinan según `feedMode`.

---

#### Relación de alimentación

Se introduce la relación conceptual:

### `ConceptFeedRelation`

Campos mínimos:

- `sourceConceptCode`
- `targetObjectCode`
- `feedMode`
- `feedValue` (opcional)
- `effectiveFrom`
- `effectiveTo`

---

#### Modos iniciales

- `INCLUDE` → aporta el 100% del importe
- `PERCENTAGE` → aporta un porcentaje del importe

---

#### Uso recomendado

- bases de cotización
- bases fiscales
- acumulados técnicos
- provisiones
- cualquier agregado donde la pertenencia dependa del concepto origen

---

#### Ventajas

- semántica clara y localizada;
- menor riesgo de omisiones al introducir nuevos conceptos;
- alineación con lógica de negocio real.

---

#### Costes

- la composición del agregado no es visible directamente desde el destino;
- requiere resolución inversa en runtime.

---

---

### 2. `SELECT_BY_RULE`

#### Definición
La pertenencia al agregado se define mediante una regla en el propio agregado.

---

#### Modelo conceptual
El agregado define una condición de selección sobre el conjunto de conceptos.

---

#### Ejemplos

- `TOTAL_DEVENGOS` → todos los conceptos con `functionalNature = EARNING`
- `TOTAL_DEDUCCIONES` → todos los conceptos con `functionalNature = DEDUCTION`

---

#### Motivación

Existen agregados cuya composición:

- no debe mantenerse manualmente;
- debe adaptarse automáticamente a nuevos conceptos;
- depende de la naturaleza funcional, no de decisiones individuales.

---

#### Parametrización mínima

- `selectionRuleType`
- `selectionRuleValue`

---

#### Reglas iniciales soportadas

- `BY_FUNCTIONAL_NATURE`
- `BY_FUNCTIONAL_SUBNATURE`
- `BY_EXPLICIT_CONCEPT_LIST`

---

#### Uso recomendado

- totales de recibo
- subtotales funcionales
- agrupaciones lógicas
- bloques de presentación

---

#### Ventajas

- evita mantenimiento manual;
- escala automáticamente con nuevos conceptos;
- reduce riesgo de errores por omisión.

---

#### Costes

- menor control individual por concepto;
- requiere definición clara de taxonomías funcionales.

---

## Regla clave de diseño

No se modelará `AGGREGATE` como una lista fija de miembros en todos los casos.

---

## Criterios de uso

| Tipo de agregado        | Estrategia recomendada |
|------------------------|------------------------|
| Bases (cotización)     | FEED_BY_SOURCE         |
| Bases (fiscalidad)     | FEED_BY_SOURCE         |
| Acumulados técnicos    | FEED_BY_SOURCE         |
| Totales funcionales    | SELECT_BY_RULE         |
| Subtotales             | SELECT_BY_RULE         |

---

## Interacción con otros ADR

- ADR-036 define el tipo `AGGREGATE`
- ADR-037 define cómo se resuelven operandos
- Este ADR define cómo se resuelven los miembros

---

## Consecuencias

### Positivas

- modelo robusto frente a crecimiento del catálogo;
- separación clara de responsabilidades;
- alineación con lógica real de nómina;
- soporte tanto para control fino como para automatización.

---

### Costes

- mayor complejidad conceptual;
- necesidad de implementar dos estrategias en runtime;
- necesidad de definir correctamente `functionalNature`.

---

## No objetivos

Este ADR no define:

- ejecución del motor de cálculo;
- orden de evaluación de conceptos;
- resolución de conflictos entre feeds;
- filtros avanzados o condiciones complejas;
- modelo físico de persistencia.

---

## Resumen ejecutivo

Se establecen dos estrategias complementarias para la construcción de agregados:

- `FEED_BY_SOURCE`: la pertenencia se declara en el concepto origen  
- `SELECT_BY_RULE`: la pertenencia se define mediante reglas en el agregado

Ambas estrategias son necesarias para modelar correctamente:

- bases técnicas (controladas por concepto)
- totales funcionales (derivados automáticamente)

Este modelo evita listas manuales frágiles y permite construir un motor de nómina flexible, escalable y alineado con el dominio.
<!-- END FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md -->


---

# FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md
<a name="file-adr-039-modelo-dependencias-y-grafo-de-c-lculo-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md -->

# ADR-039 — Modelo de dependencias y grafo de cálculo de `PayrollConcept`

## Estado
Propuesto

---

## Contexto

Los ADR previos han establecido:

- ADR-036 — Tipologías canónicas de cálculo (`DIRECT_AMOUNT`, `RATE_BY_QUANTITY`, `PERCENTAGE`, `AGGREGATE`)
- ADR-037 — Resolución de operandos mediante sources tipados
- ADR-038 — Estrategias de agregación (`FEED_BY_SOURCE`, `SELECT_BY_RULE`)

Estas decisiones permiten que un `PayrollConcept`:

- consuma resultados de otros conceptos (`source = CONCEPT`);
- participe en agregados;
- sea utilizado como base o componente de otros cálculos.

Como consecuencia, el conjunto de conceptos deja de ser independiente y pasa a formar una red de relaciones.

Es necesario formalizar esta red como un **modelo explícito de dependencias**, base para cualquier estrategia de ejecución posterior.

---

## Decisión

Se define un modelo explícito de dependencias entre `PayrollConcept` y su representación como un **grafo dirigido de cálculo**.

---

## Definición de dependencia

Se establece que:

> Un `PayrollConcept` A depende de otro concepto B si el cálculo de A requiere que B haya sido previamente calculado.

Esta relación se representa como una arista dirigida:


B → A


donde B debe evaluarse antes que A.

---

## Tipos de dependencia

Se definen tres tipos canónicos de dependencia.

---

### 1. `OPERAND_DEPENDENCY`

#### Definición
Se produce cuando un operando de un concepto se resuelve mediante `source = CONCEPT`.

#### Ejemplos
- `SALARIO_BASE` depende de `DIAS_PRESENCIA`
- `SALARIO_BASE` depende de `PRECIO_DIA`
- `IRPF` depende de `BASE_IRPF`
- `IRPF` depende de `TIPO_IRPF_EFECTIVO`

#### Origen
ADR-037 — Sources y resolución de operandos

#### Naturaleza
- explícita
- declarada directamente en la configuración del concepto

---

### 2. `FEED_DEPENDENCY`

#### Definición
Se produce cuando un `AGGREGATE` con estrategia `FEED_BY_SOURCE` recibe alimentación desde conceptos origen.

#### Ejemplos
- `BASE_CC` depende de `SALARIO_BASE`
- `BASE_IRPF` depende de `PRORRATA_EXTRA`

#### Origen
ADR-038 — Estrategias de agregación

#### Naturaleza
- derivada de relaciones de alimentación (`ConceptFeedRelation`)
- definida en el concepto origen

---

### 3. `SELECTION_DEPENDENCY`

#### Definición
Se produce cuando un `AGGREGATE` con estrategia `SELECT_BY_RULE` depende de los conceptos que cumplen su regla de selección.

#### Ejemplos
- `TOTAL_DEVENGOS` depende de todos los conceptos con `functionalNature = EARNING`
- `TOTAL_DEDUCCIONES` depende de todos los conceptos con `functionalNature = DEDUCTION`

#### Origen
ADR-038 — Estrategias de agregación

#### Naturaleza
- derivada
- dependiente del catálogo de conceptos y del contexto de evaluación

---

## Grafo de cálculo

### Definición

El conjunto de conceptos y sus dependencias forma un **grafo dirigido de cálculo** donde:

- los nodos representan `PayrollConcept`
- las aristas representan dependencias

---

### Interpretación

Una arista:


B → A


significa:

> El concepto B debe ser evaluado antes que el concepto A.

---

## Propiedades del grafo

---

### 1. Aciclicidad

El grafo debe ser un **grafo dirigido acíclico (DAG)**.

#### Consecuencia
No se permiten ciclos de dependencias entre conceptos.

#### Ejemplo inválido
- `BASE_CC` depende de `TOTAL_DEVENGOS`
- `TOTAL_DEVENGOS` depende de `SALARIO_BASE`
- `SALARIO_BASE` depende de `BASE_CC`

---

### 2. Dependencias explícitas o derivables

Toda dependencia debe ser:

- explícita (operandos con `source = CONCEPT`)
- o derivable (feeds o reglas de selección)

#### Regla
No se permiten dependencias implícitas o no declaradas.

---

### 3. Completitud estructural

El sistema debe ser capaz de:

- construir el conjunto completo de dependencias
- a partir de la configuración del modelo

antes de cualquier ejecución.

---

### 4. Independencia del contexto de ejecución

El modelo de dependencias es una propiedad estructural del sistema y:

- no depende de un empleado concreto
- no depende de una nómina concreta

---

## Grafo configurado y grafo efectivo

Se distinguen dos niveles de representación.

---

### Grafo configurado

Representa:

- todas las dependencias potenciales derivadas del metamodelo

Uso:

- validación estructural
- detección de ciclos
- análisis de impacto
- tooling

---

### Grafo efectivo

Representa:

- las dependencias realmente activas en una ejecución concreta

Uso:

- ejecución del cálculo
- trazabilidad
- debugging

---

## Construcción del grafo

El grafo se construye a partir de:

1. dependencias por operandos (`source = CONCEPT`)
2. relaciones de alimentación (`FEED_BY_SOURCE`)
3. reglas de selección (`SELECT_BY_RULE`)

---

## Consecuencias

---

### Positivas

- orden de cálculo derivable automáticamente  
- detección temprana de ciclos  
- trazabilidad completa del cálculo  
- base para ejecución declarativa  
- desacoplamiento entre conceptos  

---

### Costes

- mayor complejidad conceptual  
- necesidad de validación estructural  
- necesidad de herramientas de inspección del grafo  

---

## Riesgos identificados

---

### 1. Ciclos indirectos

Dependencias encadenadas pueden generar ciclos no triviales.

---

### 2. Dependencias mal definidas

Errores en configuración pueden generar dependencias inexistentes o incoherentes.

---

### 3. Selección dinámica no controlada

`SELECT_BY_RULE` debe mantenerse dentro de un conjunto acotado de reglas para evitar comportamientos impredecibles.

---

## No objetivos

Este ADR no define:

- estrategia de ejecución del grafo  
- orden de evaluación concreto  
- paralelización  
- caching  
- segmentación temporal  
- activación contextual de conceptos  

---

## Resumen ejecutivo

Se establece que los `PayrollConcept` forman un grafo dirigido de dependencias donde:

- los nodos representan conceptos  
- las aristas representan relaciones de dependencia  

Se definen tres tipos de dependencia:

- `OPERAND_DEPENDENCY`
- `FEED_DEPENDENCY`
- `SELECTION_DEPENDENCY`

El grafo debe ser acíclico, explícito y completamente derivable de la configuración.

Este modelo constituye la base para la futura ejecución del motor de cálculo.
<!-- END FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md -->


---

# FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md
<a name="file-adr-040-macro-grafo-activaci-n-de-conceptos-y-plan-de-c-lculo-efectivo-md"></a>

<!-- BEGIN FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md -->

# ADR-040 — Macro-grafo, activación de conceptos y plan de cálculo efectivo

## Estado
Implementado (ver ADR-045)

---

## Contexto

El ADR-039 define que los `PayrollConcept` forman un grafo dirigido acíclico (DAG) basado en dependencias:

- `OPERAND_DEPENDENCY`
- `FEED_DEPENDENCY`
- `SELECTION_DEPENDENCY`

Sin embargo, este grafo:

- representa **todas las dependencias posibles**
- no distingue qué conceptos deben calcularse en una ejecución concreta

Para poder ejecutar el motor, es necesario definir:

1. cómo se determina qué conceptos participan
2. cómo se reduce el grafo al subconjunto relevante
3. cómo se obtiene un orden de cálculo válido

---

## Decisión

Se introduce un modelo de **macro-grafo + activación + plan efectivo** en tres fases:

1. **Macro-grafo configurado**
2. **Activación de conceptos**
3. **Plan de cálculo efectivo**

---

## 1. Macro-grafo configurado

### Definición

El macro-grafo es:

> el grafo completo de todos los `PayrollConcept` y sus dependencias estructurales

Incluye:

- todos los conceptos definidos en el sistema
- todas las dependencias posibles derivadas del modelo

---

### Propiedades

- es global al `ruleSystem`
- es independiente de empleado o periodo
- es estático salvo cambios de configuración
- es validado estructuralmente (ciclos, coherencia)

---

### Uso

- validación del sistema
- análisis de impacto
- tooling (visualización, debugging)
- base para generación de planes efectivos

---

## 2. Activación de conceptos

### Definición

La activación determina:

> qué conceptos deben calcularse en una ejecución concreta

---

### Tipos de activación

Se definen tres mecanismos canónicos:

---

#### 2.1 Activación explícita (`EXPLICIT`)

Conceptos solicitados directamente por el sistema.

#### Ejemplos

- cálculo de `NETO_A_PERCIBIR`
- cálculo de `TOTAL_DEVENGOS`

---

#### 2.2 Activación por dependencia (`DEPENDENCY`)

Se activan todos los conceptos necesarios para calcular los conceptos explícitos.

#### Regla

Si A está activado y A depende de B, entonces B se activa.

---

#### 2.3 Activación por selección (`SELECTION`)

Se activan conceptos seleccionados dinámicamente por reglas de agregación.

#### Ejemplos

- todos los conceptos con `functionalNature = EARNING`
- todos los conceptos marcados como cotizables

---

### Resultado de la activación

Se obtiene:

> un subconjunto de nodos del macro-grafo llamado **conjunto activo de conceptos**

---

## 3. Subgrafo efectivo

### Definición

El subgrafo efectivo es:

> el grafo inducido por el conjunto activo de conceptos

Incluye:

- todos los nodos activados
- todas las dependencias entre ellos

---

### Propiedades

- es un subgrafo del macro-grafo
- sigue siendo acíclico
- es específico de una ejecución

---

## 4. Plan de cálculo efectivo

### Definición

El plan de cálculo es:

> una ordenación válida de los conceptos activos que respeta todas las dependencias

---

### Construcción

Se obtiene mediante una **ordenación topológica** del subgrafo efectivo.

---

### Propiedades

- todo concepto se evalúa después de sus dependencias
- no existe ambigüedad en el orden relativo necesario
- puede existir más de un orden válido

---

### Representación

El plan puede representarse como:

- lista ordenada de conceptos
- niveles de cálculo (capas paralelizables)
- pipeline de ejecución

---

## Ejemplo conceptual

Dado el objetivo:


NETO_A_PERCIBIR


### Activación

Se activan:

- NETO_A_PERCIBIR
- TOTAL_DEVENGOS
- TOTAL_DEDUCCIONES
- SALARIO_BASE
- IRPF
- BASE_IRPF
- ...
Subgrafo efectivo

Se construye el grafo con esos nodos y sus dependencias.

Plan resultante (ejemplo)
DIAS_PRESENCIA
PRECIO_DIA
SALARIO_BASE
BASE_IRPF
TIPO_IRPF_EFECTIVO
IRPF
TOTAL_DEVENGOS
TOTAL_DEDUCCIONES
NETO_A_PERCIBIR
Separación de responsabilidades

Este ADR establece una separación clara:

Fase	Responsabilidad
Macro-grafo	modelo estructural
Activación	qué calcular
Subgrafo	reducción del problema
Plan	cómo ordenarlo
Consecuencias
Positivas
ejecución derivada automáticamente
desacoplamiento total entre definición y ejecución
capacidad de calcular subconjuntos
base para paralelización futura
trazabilidad clara
Costes
necesidad de construir subgrafos dinámicos
necesidad de resolver activación correctamente
mayor complejidad conceptual
Riesgos
1. Activación incompleta

Si falta un concepto necesario → fallo en ejecución.

2. Activación excesiva

Activar conceptos innecesarios → coste de cálculo innecesario.

3. Reglas de selección mal definidas

Pueden activar conjuntos inesperados de conceptos.

No objetivos

Este ADR no define:

cómo se calcula cada concepto
cómo se gestionan segmentos temporales
cómo se cachean resultados
cómo se ejecuta en paralelo
cómo se materializan resultados
Relación con ADRs previos
ADR-036 → define tipos de cálculo
ADR-037 → define sources y operandos
ADR-038 → define agregación
ADR-039 → define dependencias

Este ADR define:

cómo todo lo anterior se convierte en un plan ejecutable

Resumen ejecutivo

El sistema se modela como:

un macro-grafo completo de conceptos
un proceso de activación que determina qué calcular
un subgrafo efectivo reducido
un plan de cálculo derivado por ordenación topológica

Este enfoque permite ejecutar el motor de forma declarativa, predecible y extensible.
<!-- END FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md -->


---

# FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md
<a name="file-adr-041-segmentaci-n-temporal--mbito-de-ejecuci-n-y-c-lculo-por-tramos-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md -->

# ADR-041 — Segmentación temporal, ámbito de ejecución y cálculo por tramos en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

Los ADR previos establecen:

- ADR-036 — Tipologías de cálculo de `PayrollConcept`
- ADR-037 — Resolución de operandos mediante sources
- ADR-038 — Estrategias de agregación (`FEED_BY_SOURCE`, `SELECT_BY_RULE`)
- ADR-039 — Modelo de dependencias y grafo de cálculo (DAG)
- ADR-040 — Macro-grafo, activación y plan efectivo

Estos elementos permiten definir qué calcular y en qué orden.

Sin embargo, en nómina real, durante un mismo período pueden producirse cambios que afectan al cálculo:

- jornada laboral  
- salario  
- contrato  
- centro de trabajo  
- situaciones de alta/baja  
- otras condiciones relevantes  

Esto implica que el cálculo no puede realizarse como una única ejecución homogénea.

---

## Decisión

Se introduce un modelo de:

1. **segmentación temporal del período**
2. **ejecución del plan de cálculo por segmento**
3. **clasificación de conceptos por ámbito temporal (`executionScope`)**
4. **consolidación de resultados a nivel de período**

---

## 1. Modelo temporal

### 1.1 `CalculationPeriod`

Representa el período global de la nómina:

- `periodStart`
- `periodEnd`

---

### 1.2 `CalculationSegment`

Representa un subtramo homogéneo dentro del período:

- `segmentStart`
- `segmentEnd`

---

### Propiedad clave

Dentro de un segmento:

> Las condiciones relevantes para el cálculo permanecen constantes.

---

## 2. Segmentación

### Definición

El período se divide en:

> un conjunto ordenado de segmentos contiguos, no solapados y exhaustivos

---

### Propiedades

- cubren completamente el período  
- no se solapan  
- son deterministas  
- son reproducibles  

---

### Origen de los cortes

Los segmentos se generan por cambios en condiciones relevantes del cálculo:

- datos del empleado  
- asignaciones  
- condiciones contractuales  
- otros factores que afectan al cálculo  

---

### Regla importante

Un segmento puede estar delimitado por múltiples cambios simultáneos.

#### Consecuencia

No se modela una única “fuente del segmento”.

---

## 3. Contexto de ejecución por segmento

Cada ejecución del cálculo se realiza con un contexto temporal enriquecido:

- `periodStart`
- `periodEnd`
- `segmentStart`
- `segmentEnd`
- `isFirstSegment`
- `isLastSegment`

---

## 4. Relación con el grafo de cálculo

### Regla fundamental

> La segmentación no modifica la topología del grafo de cálculo.

---

### Implicación

- el macro-grafo y el plan efectivo son únicos  
- se reutilizan para todos los segmentos  

---

### Ejecución

El plan de cálculo:

> se ejecuta una vez por cada segmento con distinto contexto temporal

---

## 5. Ámbito de ejecución del concepto

Se introduce la propiedad:

# `executionScope`

---

### Definición

Define el nivel temporal en el que se evalúa un concepto.

---

### Valores iniciales

- `SEGMENT`
- `PERIOD`

---

### Regla fuerte

> `executionScope` es una propiedad inmutable del concepto.

#### Consecuencia

Cambiar el ámbito implica crear un nuevo concepto.

---

### Interpretación

#### `SEGMENT`

El concepto se evalúa en cada segmento.

Ejemplos:

- salario base  
- horas trabajadas  
- pluses proporcionales  

---

#### `PERIOD`

El concepto se evalúa una única vez para todo el período.

Ejemplos:

- totales  
- agregados finales  
- ciertos cálculos acumulados  

---

## 6. Ejecución segmentada

### Proceso

1. Se construyen los segmentos del período  
2. Se ejecuta el plan de cálculo para cada segmento (`executionScope = SEGMENT`)  
3. Se obtienen resultados parciales  
4. Se consolidan los resultados a nivel de período  
5. Se evalúan conceptos de `executionScope = PERIOD`

---

## 7. Consolidación

### Definición

Proceso de agregación de resultados de segmentos.

---

### Ejemplos

- suma de importes segmentados  
- construcción de bases  
- preparación de datos para conceptos de período  

---

### Nota

La consolidación es un paso previo a la evaluación de conceptos de ámbito `PERIOD`.

---

## 8. Trazabilidad y reproducibilidad

### Regla clave

> La segmentación utilizada en un cálculo debe ser determinista, reproducible y auditable.

---

### Decisión

Los segmentos forman parte del:

> **snapshot técnico del cálculo de nómina**

---

### Consecuencia

Es posible:

- reconstruir cómo se calculó la nómina  
- explicar los tramos utilizados  
- garantizar coherencia en retroactividad  

---

## 9. Validaciones

---

### 9.1 Validación de segmentación

Debe garantizar:

- cobertura completa del período  
- ausencia de solapamientos  
- orden correcto  

---

### 9.2 Validación de ejecución

Debe garantizar:

- coherencia entre `executionScope` y uso del concepto  
- disponibilidad de datos necesarios en cada segmento  
- correcta consolidación  

---

## 10. Riesgos identificados

---

### 10.1 Segmentación no determinista

Provoca inconsistencias en recalculaciones.

---

### 10.2 Uso incorrecto de `executionScope`

Puede generar:

- doble cálculo  
- omisiones  
- incoherencias  

---

### 10.3 Mala clasificación de conceptos

Asignar incorrectamente `SEGMENT` o `PERIOD` rompe la lógica del cálculo.

---

### 10.4 Explicación simplificada de cortes

Asociar un único motivo a un segmento puede ser incorrecto.

---

## 11. No objetivos

Este ADR no define:

- algoritmo de generación de segmentos  
- optimización de ejecución  
- paralelización  
- caching  
- persistencia detallada de estructuras internas  

---

## 12. Insight clave

El cálculo de nómina evoluciona de:

> una ejecución única del grafo

a:

> la ejecución del mismo plan de cálculo sobre múltiples contextos temporales homogéneos, seguida de una consolidación

---

## 13. Conclusión

Se establece que:

- el período se segmenta en tramos homogéneos  
- el mismo plan de cálculo se ejecuta por segmento  
- los conceptos se clasifican por ámbito temporal (`executionScope`)  
- los resultados se consolidan a nivel de período  
- la segmentación es determinista y trazable  

Este modelo permite:

- soportar cambios intraperiodo  
- mantener coherencia en retroactividad  
- preservar un único grafo de cálculo  
- garantizar trazabilidad completa del resultado  
<!-- END FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md -->


---

# FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md
<a name="file-adr-042-separaci-n-entre-payrol-y-payroll-engine-md"></a>

<!-- BEGIN FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md -->

# ADR-042 — Separación entre `payroll` y `payroll_engine`

## Estado
Propuesto

---

## Contexto

El diseño del motor de nómina de B4RRHH ha evolucionado desde un enfoque potencialmente basado en lógica específica por concepto hacia un modelo configurable basado en:

- `PayrollObject`
- `PayrollConcept`
- tipologías de cálculo
- sources y operandos
- estrategias de agregación
- grafo de dependencias
- segmentación temporal

En paralelo, el bounded context `payroll` ya existe para modelar:

- nóminas calculadas
- estados de nómina
- runs de cálculo
- claims
- mensajes
- snapshots del cálculo

A medida que madura el metamodelo del motor, aparece una separación semántica clara entre:

1. **la definición de cómo se calcula una nómina**
2. **la persistencia del resultado de una nómina calculada**

Mezclar ambas naturalezas en el mismo schema o subdominio introduce ambigüedad de diseño.

---

## Decisión

Se separan explícitamente dos ámbitos:

- `payroll`
- `payroll_engine`

---

## 1. Ámbito `payroll`

`payroll` modela la nómina calculada como resultado de negocio.

Incluye, entre otros:

- payroll root
- líneas calculadas
- estados
- calculation runs
- claims
- mensajes
- snapshots técnicos del cálculo
- segmentos utilizados en una ejecución concreta

### Naturaleza
Resultado materializado del cálculo.

---

## 2. Ámbito `payroll_engine`

`payroll_engine` modela el metamodelo y la configuración técnico-funcional del motor.

Incluye, entre otros:

- `PayrollObject`
- `PayrollConcept`
- feeds entre conceptos
- tablas
- constantes
- tipologías de cálculo
- scopes de ejecución
- metadatos de resolución

### Naturaleza
Definición estructural de cómo se calcula una nómina.

---

## Regla principal

> La configuración y metamodelo del motor de nómina no deben persistirse en el mismo ámbito semántico que las nóminas calculadas.

---

## Consecuencias

### Positivas

- separación clara entre configuración y resultado
- mejor trazabilidad
- mejor capacidad de gobierno del motor
- menor contaminación semántica del bounded context `payroll`
- base más limpia para evolución futura

### Costes

- aparece un nuevo ámbito de diseño
- exige modelar explícitamente la relación entre runtime del motor y resultado calculado

---

## Regla operativa

Los artefactos persistentes que definan **cómo se calcula** una nómina pertenecen a `payroll_engine`.

Los artefactos persistentes que representen **una nómina ya calculada** pertenecen a `payroll`.

---

## No objetivos

Este ADR no define todavía:

- estructura física detallada de schemas
- APIs de mantenimiento del motor
- estrategia de despliegue
- separación en repositorios o servicios

---

## Resumen ejecutivo

Se establece una frontera explícita:

- `payroll` = resultado calculado
- `payroll_engine` = definición del motor

Esto evita mezclar metamodelo y cálculo materializado en el mismo dominio y prepara una base más limpia para la implementación.
<!-- END FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md -->


---

# FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md
<a name="file-adr-043-agreement-profile-y-activaci-n-de-payroll-basada-en-contexto-md"></a>

<!-- BEGIN FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md -->

ADR-043 — Agreement Profile y Activación de Payroll basada en Contexto
Estado

Propuesto

Contexto

El sistema B4RRHH actualmente modela el convenio colectivo (AGREEMENT) como una entidad de catálogo dentro de rule_entity, junto con su relación con categorías (AGREEMENT_CATEGORY).

Este modelo es suficiente para validaciones básicas, pero insuficiente para:

representar información real de negocio del convenio (código oficial, jornada anual, etc.)
alimentar lógica derivada (ej. cálculo de jornada del empleado)
servir como contexto de configuración para el motor de nómina

En paralelo, el motor de nómina (payroll) se está diseñando en torno a un metamodelo de objetos configurables (PayrollObject), donde:

los conceptos (PAYROLL_CONCEPT) representan cálculos
las tablas (TABLE) representan fuentes de datos parametrizadas
las constantes (CONSTANT) representan valores fijos

Además, se identifica la necesidad de que distintos contextos de negocio (rule system, convenio, empresa, etc.):

activen conceptos de nómina aplicables
vinculen fuentes de datos (tablas, constantes)

Finalmente, se detecta una deuda técnica en employee.working_time, donde las horas anuales se encuentran fijadas de forma estática (ej. 2000 horas), cuando en realidad dependen del convenio aplicable.

Problema

Se necesita:

Enriquecer el convenio sin romper el modelo existente basado en rule_entity
Permitir que el convenio participe en la configuración efectiva del cálculo de nómina
Introducir un mecanismo genérico y escalable para:
activar conceptos de nómina
vincular fuentes de datos (tablas)
Definir una estructura eficiente para almacenar datos parametrizados de tablas (ej. salario base por categoría)
Resolver la dependencia entre convenio y jornada laboral del empleado
Evitar:
proliferación de tablas específicas por tipo de entidad (agreement_triggers, etc.)
sobreabstracción prematura del motor de nómina
Decisión
1. Mantener AGREEMENT como catálogo base
AGREEMENT permanece como rule_entity
No se modifica su identidad funcional
El código funcional del convenio será, preferentemente, el código oficial real

Ejemplo:

ruleSystemCode = ESP
agreementCode = 99002405011982
2. Introducir agreement_profile como enriquecimiento

Se crea una nueva entidad:

agreement_profile

Identidad funcional
(ruleSystemCode, agreementCode)
Campos principales
officialAgreementNumber
displayName
shortName
annualHours
active
createdAt
updatedAt
Propósito
Enriquecer el convenio con datos de negocio
Servir como fuente para lógica derivada (ej. jornada del empleado)
3. Derivar la jornada del empleado desde el convenio

Se establece la regla:

employee.working_time no contiene una constante fija de horas anuales;
las horas se derivan del agreement_profile vigente en la fecha de aplicación.

Flujo:

Cambio en working_time
Resolución de convenio/categoría vigente
Lectura de annualHours desde agreement_profile
Cálculo y persistencia de valores derivados
4. Introducir activación de objetos de nómina por contexto

Se crea una tabla genérica:

payroll_object_activation

Campos
ruleSystemCode
ownerTypeCode (RULE_SYSTEM, AGREEMENT, COMPANY, etc.)
ownerCode
targetObjectTypeCode
targetObjectCode
active
Propósito

Permitir que un contexto de negocio active conceptos de nómina.

Restricción V1

Solo se permite:

targetObjectTypeCode = PAYROLL_CONCEPT
Ejemplo
AGREEMENT 99002405011982 → PAYROLL_CONCEPT SALARIO_BASE
5. Introducir binding de objetos de nómina por contexto

Se crea una segunda tabla genérica:

payroll_object_binding

Campos
ruleSystemCode
ownerTypeCode
ownerCode
bindingRoleCode
boundObjectTypeCode
boundObjectCode
active
Propósito

Permitir que un contexto vincule fuentes de datos a roles funcionales.

Ejemplo
AGREEMENT 99002405011982 → BASE_SALARY_TABLE → TABLE SB_RETAIL
Nota

bindingRoleCode es obligatorio para distinguir semántica.

6. Mantener separación semántica: activation vs binding

Se decide explícitamente:

activation ≠ binding
No se utiliza una única tabla genérica para ambos conceptos

Motivo:

semántica distinta
validación distinta
mantenimiento más claro
7. Mantener precedencia fuera de datos (V1)

La precedencia entre contextos:

Ejemplo:

RULE_SYSTEM > AGREEMENT > COMPANY > WORK_CENTER

No se modela en base de datos en V1.

Se define como:

política del motor
fijada en código
validada mediante tests
8. Modelar TABLE como PayrollObject

Se mantiene la decisión:

TABLE es un PayrollObject
BK canónica:
ruleSystemCode + objectTypeCode + objectCode
9. Introducir estructura común de filas de tabla

Se crea una estructura física común:

payroll_table_row

Campos
ruleSystemCode
tableCode
searchCode
startDate
endDate
annualValue
monthlyValue
dailyValue
hourlyValue
active
Identidad funcional
(ruleSystemCode, tableCode, searchCode, startDate)
Propósito
Lookup eficiente por clave + fecha
Soporte para tablas típicas de nómina:
salario base
plus convenio
antigüedad
10. Estrategia de valores

Cada tabla define un valueBasis:

Ejemplo:

MONTHLY_MASTER
ANNUAL_MASTER

Se establece:

un valor rector
valores derivados persistidos
11. Relación convenio → tabla

Se define mediante binding, no por estructura interna de la tabla.

Ejemplo:

AGREEMENT → BASE_SALARY_TABLE → TABLE SB_RETAIL

La tabla:

no necesita incluir agreementCode en su clave
puede reutilizarse o especializarse libremente
12. Estrategia de evolución

Se permite que en el futuro existan nuevos tipos de objeto payroll:

Ejemplo:

COMPLEX_TABLE

Motivación:

no forzar todos los casos en un único modelo de tabla
permitir crecimiento sin romper diseño base
Consecuencias
Positivas
Enriquecimiento del convenio sin romper catálogo existente
Eliminación de constantes duras (ej. 2000 horas)
Integración natural convenio ↔ payroll
Modelo escalable basado en contextos
Evita proliferación de tablas específicas por tipo de entidad
Separación clara entre:
activación (qué se calcula)
binding (de dónde salen los datos)
Lookup de tablas eficiente y uniforme
Negativas / Riesgos
Introducción de dos nuevas tablas genéricas (activation y binding)
Necesidad de disciplina en bindingRoleCode
Riesgo de sobreuso de TABLE para casos complejos
Precedencia no configurable en V1 (requiere cambios de código)
No objetivos (V1)
Modelado completo de versiones de convenio
Modelado genérico de tablas multiclave complejas
Engine de reglas declarativas completo
Precedencia configurable en base de datos
Activación de objetos distintos de PAYROLL_CONCEPT
UI avanzada de configuración payroll
Estrategia de implementación
Crear agreement_profile
Integrar annualHours en working_time
Crear payroll_object_activation
Crear payroll_object_binding
Crear TABLE + payroll_table_row para salario base
Activar SALARIO_BASE desde convenio
Resolver salario base en payroll usando:
convenio
categoría
tabla vinculada
fecha
Nota operativa

Se recomienda iniciar el uso de:

convenios reales (código oficial)
datos reales de tablas salariales

Manteniendo datos actuales como:

entorno de test
fallback

Esto permitirá validar el modelo con casos reales desde el inicio.

🧠 Cierre

La idea central de este ADR es:

El convenio no calcula nómina, pero sí define el contexto que activa qué se calcula y con qué datos.

Y el sistema se organiza en torno a tres pilares:

Contexto (agreement, company, etc.)
Activación (conceptos)
Binding (fuentes)
<!-- END FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md -->


---

# FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md
<a name="file-adr-044-primer-c-lculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-m-nimo-md"></a>

<!-- BEGIN FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md -->

ADR — Primer cálculo real de salario base mediante conceptos tipados y grafo mínimo
Estado

Implementado (ver ADR-045)

Contexto

En iteraciones recientes se ha construido un slice funcional que permite:

activar conceptos por convenio
vincular tablas mediante binding
resolver filas temporales por convenio/categoría/fecha
integrar conceptos reales en el lanzador de nómina

Ese trabajo ha sido útil para validar:

activation
binding
payroll_table_row
resolución temporal
integración con el launch

Sin embargo, la implementación actual de conceptos como BASE_SALARY o PLUS_CONVENIO se ha materializado mediante servicios concretos por concepto. Ese enfoque ha servido como spike técnico, pero no representa la arquitectura objetivo del motor de nómina.

El bundle de diseño ya fijaba una línea distinta: los conceptos de nómina deben resolverse por tipología de cálculo y por sources tipados, no por servicios hardcodeados por concepto. En particular, el bundle ya contempla tipologías como DIRECT_AMOUNT, RATE_BY_QUANTITY, PERCENTAGE y AGGREGATE, y además permite que un operando se resuelva a partir de otro CONCEPT. El ejemplo conceptual del salario base ya estaba descrito como una composición de cantidad y precio.

Se necesita, por tanto, una reconducción controlada:

aprovechar las piezas ya validadas
dejar de codificar conceptos de negocio “a mano”
empezar a ejecutar un mini grafo real
Problema

Se quiere obtener una primera nómina no fake con un salario base mínimo pero real, disparado desde el convenio y resuelto mediante conceptos configurados en base de datos.

Ese primer caso debe ser lo bastante simple para ser implementable en pocas iteraciones, pero lo bastante correcto como para validar la arquitectura del motor.

Decisión
1. Separar explícitamente concepto de negocio y concepto técnico

Se distinguen dos familias de conceptos:

Conceptos de negocio

Son los que representan líneas reales de nómina y pueden persistirse como resultado final.

Ejemplo:

101 - SALARIO_BASE
Conceptos técnicos

Son nodos auxiliares de cálculo, reutilizables, y no tienen por qué persistirse como líneas finales de nómina.

Ejemplos:

D01 - DIAS_PRESENCIA
P01 - PRECIO_DIA_TEORICO

Los conceptos técnicos podrán ser reutilizados por varios conceptos de negocio futuros.

2. El salario base piloto se modela como RATE_BY_QUANTITY

El primer concepto real de negocio será:

101 - SALARIO_BASE

Su tipología será:

RATE_BY_QUANTITY

Semántica:

quantity = CONCEPT(D01)
rate = CONCEPT(P01)

Resultado:

101 = P01 × D01

Esto sigue la línea ya fijada en el bundle para salario base como combinación de cantidad y precio.

3. D01 - DIAS_PRESENCIA se introduce como concepto técnico temporalmente simplificado

Se define:

D01 - DIAS_PRESENCIA

Tipología inicial:

DIRECT_AMOUNT

Valor inicial:

30

Esta simplificación es deliberada.
No se calcularán todavía días reales de presencia ni segmentación.

Objetivo de esta iteración:

validar la dependencia técnica
validar la ejecución por grafo
no bloquear el avance por la falta de cálculo temporal detallado

En iteraciones futuras, D01 podrá evolucionar para resolverse por segmento o por ventana real de presencia sin cambiar la estructura del concepto 101.

4. P01 - PRECIO_DIA_TEORICO se introduce como concepto técnico valorizado por tabla binded

Se define:

P01 - PRECIO_DIA_TEORICO

Tipología inicial:

DIRECT_AMOUNT resuelto por source TABLE
o, equivalentemente, un nodo técnico cuyo valor proviene de lookup a tabla binded

Valor:

daily_value de payroll_table_row

Lookup por:

convenio aplicable
categoría aplicable
fecha efectiva

El binding del convenio apuntará a la tabla salarial correspondiente, y P01 se resolverá leyendo daily_value de la fila vigente.

Esto aprovecha directamente la estructura ya existente en payroll_table_row, que ya contiene daily_value. No se requiere derivar el precio diario a partir del mensual. Eso permite un primer caso limpio y escalable.

5. El convenio dispara el concepto final de negocio, no los nodos técnicos como líneas finales

Para el primer caso:

el convenio activa 101 - SALARIO_BASE

El motor, al resolver 101, podrá descubrir y ejecutar sus dependencias:

D01
P01

Pero el resultado persistido en la nómina será, en esta iteración:

101 - SALARIO_BASE

Los conceptos técnicos podrán existir:

como nodos de ejecución
como trazabilidad futura
como snapshot si más adelante interesa

Pero no se consideran todavía líneas finales de nómina.

6. La ejecución se hará mediante un grafo mínimo, no mediante servicios concretos por concepto de negocio

Se abandona como dirección arquitectónica final la idea de:

CalculateBaseSalaryService
CalculateAgreementPlusService
etc.

Esos servicios se reinterpretan como spikes o resolvedores transitorios útiles para validar piezas del pipeline.

La dirección correcta pasa a ser:

resolver conceptos por tipología
resolver operandos por source
permitir que un concepto dependa de otro CONCEPT

Para la primera iteración no se construirá un motor genérico completo, pero sí un mini dispatcher suficiente para ejecutar:

DIRECT_AMOUNT
RATE_BY_QUANTITY

y para resolver operandos tipo:

CONCEPT
TABLE
7. El lanzador de nómina deberá enchufarse al mini grafo real

El endpoint de cálculo / lanzador ya existente dejará de inyectar un concepto fake para este caso y pasará a:

identificar que el convenio dispara 101 - SALARIO_BASE
resolver el mini grafo:
D01
P01
101
persistir 101 como línea final real de nómina

El camino fake podrá mantenerse temporalmente como fallback o modo alternativo, pero el caso piloto de salario base debe pasar a ejecutarse ya con grafo mínimo real.

Consecuencias
Positivas
Se reconduce el diseño hacia el motor real sin tirar piezas útiles.
Se valida el uso real de:
trigger por convenio
binding de tabla
source CONCEPT
source TABLE
tipología RATE_BY_QUANTITY
Se evita seguir creando servicios por concepto como arquitectura final.
Se prepara una base escalable:
mañana D01 podrá calcularse por segmento
mañana P02 podrá depender de P01 y J01
mañana se podrán introducir coeficientes de jornada sin reescribir 101
Negativas / Costes
Lo ya implementado como cálculo directo de BASE_SALARY y PLUS_CONVENIO pasa a ser transitorio.
Hay que introducir un primer wiring real de dependencias entre conceptos.
El launch tendrá que dejar de pensar en “conceptos hardcodeados” y empezar a ejecutar un mini plan de cálculo.
No objetivos de esta iteración

No se pretende todavía:

calcular días reales de presencia
aplicar coeficiente real de jornada
introducir segmentación temporal
modelar dependencias arbitrarias complejas
persistir todos los conceptos técnicos como líneas de nómina
construir un motor genérico completo con todas las tipologías posibles
resolver plus convenio dentro de este mismo salto
Diseño mínimo resultante
Conceptos iniciales
101 - SALARIO_BASE
tipo: RATE_BY_QUANTITY
quantity source: CONCEPT(D01)
rate source: CONCEPT(P01)
D01 - DIAS_PRESENCIA
tipo: DIRECT_AMOUNT
valor inicial: 30
P01 - PRECIO_DIA_TEORICO
tipo: DIRECT_AMOUNT / valor resuelto por TABLE
source: tabla binded por convenio
campo usado: daily_value
Resultado final
línea final persistida: 101 - SALARIO_BASE
Estrategia de implementación
Paso 1

Sembrar en base de datos:

101
D01
P01
sus tipologías
sus relaciones/dependencias
el binding de tabla para P01
Paso 2

Crear el wiring mínimo del mini grafo:

resolver 101
descubrir dependencias
resolver D01
resolver P01
calcular 101
Paso 3

Enchufar ese mini cálculo al lanzador de nómina existente

Paso 4

Persistir 101 como concepto final real en la tabla de resultados

Nota de transición

Los servicios actuales tipo CalculateBaseSalaryService o CalculateAgreementPlusService no se consideran el diseño final del motor. Se mantienen únicamente como apoyo temporal o como material de transición mientras el primer camino real basado en tipologías y dependencias queda operativo.

Decisión práctica inmediata

La siguiente iteración no se enfocará en añadir más conceptos directos.
Se enfocará en conseguir que el lanzador calcule un único concepto real final (101 - SALARIO_BASE) mediante:

trigger de convenio
mini grafo
conceptos técnicos
tipologías mínimas
lookup a tabla binded
<!-- END FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md -->


---

# FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md
<a name="file-adr-045-ejecucion-elegible-real-basada-en-concept-assignment-y-plan-de-calculo-md"></a>

<!-- BEGIN FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md -->

# ADR-045 — Ejecución elegible real basada en `concept_assignment` y plan de cálculo

## Estado

Implementado

---

## Contexto

ADR-044 estableció la dirección técnica: abandonar servicios hardcodeados por concepto y ejecutar nómina mediante un grafo mínimo de conceptos configurados en base de datos. La primera iteración de ese diseño se implementó con un stub PoC en `CalculatePayrollUnitService.calculateEligibleReal()` que calculaba **únicamente el concepto "101"** mediante una llamada directa a `PayrollConceptGraphCalculator`.

Ese stub sirvió para validar las piezas del pipeline (convenio, binding, tablas, resolución temporal) pero nunca fue el diseño final. Para avanzar hacia una nómina real había que:

1. Determinar qué conceptos aplican a un empleado desde parametrización (no hardcode)
2. Expandir las dependencias transitivas necesarias para el cálculo
3. Construir un plan de ejecución en orden topológico
4. Ejecutar el plan completo, no solo un concepto
5. Persistir solo los conceptos con presencia en recibo

En paralelo, se habían diseñado las tablas `payroll_engine.concept_assignment` (elegibilidad por contexto) y el pipeline `BuildEligibleExecutionPlanUseCase` (construcción del plan), pero ninguno estaba enchufado al lanzador.

---

## Decisión

### 1. `concept_assignment` como fuente de elegibilidad canónica

La tabla `payroll_engine.concept_assignment` es la fuente oficial de qué conceptos aplican a un contexto dado:

| Columna | Semántica |
|---|---|
| `rule_system_code` | Ámbito del sistema de reglas |
| `concept_code` | Concepto elegible |
| `company_code` | Wildcardeable (null = aplica a todas las empresas) |
| `agreement_code` | Convenio aplicable |
| `employee_type_code` | Wildcardeable (null = aplica a todos los tipos) |
| `valid_from` / `valid_to` | Vigencia temporal |
| `priority` | Resolución de conflictos cuando hay múltiples asignaciones para el mismo concepto |

La elegibilidad se evalúa mediante `EmployeeAssignmentContext` (ruleSystemCode, companyCode, agreementCode, employeeTypeCode) en la fecha de referencia (fin de periodo).

**Divergencia de ADR-043**: ADR-043 proponía `payroll_object_activation` como mecanismo de activación de conceptos por contexto. Esa tabla existe en el modelo pero no fue el camino tomado para la ejecución elegible. `concept_assignment` es el mecanismo real en producción para el motor `payroll_engine`. `payroll_object_activation` queda para futuros casos de uso distintos si los hubiera.

### 2. Eliminación del stub PoC en `CalculatePayrollUnitService`

`calculateEligibleReal()` ya no hardcodea el concepto "101". En su lugar:

1. Obtiene el contexto de asignación del empleado desde `PayrollLaunchEligibleInputContext`
2. Llama a `BuildEligibleExecutionPlanUseCase.build(assignmentContext, periodEnd)`
3. Itera el plan resultante (`EligibleExecutionPlanResult.executionPlan()`) en orden topológico
4. Aplica la lógica de cálculo según `calculationType` de cada `ConceptExecutionPlanEntry`
5. Filtra por `payslipOrderCode != null` para decidir qué conceptos se persisten

### 3. Corrección del grafo de dependencias: edges de operandos

`DefaultConceptDependencyGraphService` ahora añade aristas `OPERAND_DEPENDENCY` para conceptos `RATE_BY_QUANTITY` y `PERCENTAGE`. Sin estas aristas, el grafo solo tenía aristas `FEED_DEPENDENCY`, lo que hacía que la ordenación topológica fuera incorrecta para conceptos con operandos de tipo `CONCEPT`.

Regla: un concepto `X` de tipo `RATE_BY_QUANTITY` cuyos operandos son `CONCEPT(D01)` y `CONCEPT(P01)` tiene dependencias `OPERAND_DEPENDENCY` de `X→D01` y `X→P01`. Estas aristas garantizan que D01 y P01 se calculen antes que X.

### 4. Expansión BFS con discovery por operandos

`DefaultEligibleConceptExpansionService` realiza la expansión del conjunto elegible en dos fases:

- **Feed-based discovery**: descubre conceptos adicionales accesibles por relaciones `FEED_DEPENDENCY`, pero solo incluye conceptos de tipo `PayrollObjectTypeCode.CONCEPT` (no tablas ni constantes).
- **Operand-based discovery**: para cada concepto con operandos de tipo `CONCEPT`, incluye los conceptos técnicos referenciados como operandos (ej. D01 y P01 para el concepto 101).

Los conceptos descubiertos por expansión (D01, P01) no están en `concept_assignment` y no son "elegibles" en sentido de negocio, pero son necesarios para el cálculo. El resultado distingue `eligibleConcepts` (asignados directamente) de `expandedConcepts` (conjunto completo incluyendo técnicos).

### 5. `payslipOrderCode` como filtro de persistencia

Un concepto calculado se persiste como línea de nómina si y solo si su `payslipOrderCode` no es null. El valor de `payslipOrderCode` determina además el orden de presentación en el recibo.

Los conceptos técnicos (D01, P01) tienen `payslipOrderCode = null` → se calculan pero no se persisten.  
Los conceptos de negocio (101, 970, 990) tienen `payslipOrderCode` establecido → se persisten y aparecen en el recibo.

### 6. Conceptos AGGREGATE 970, 980, 990

Se introducen tres conceptos de tipo `AGGREGATE`:

| Código | Mnemónico | Rol funcional | `payslipOrderCode` |
|---|---|---|---|
| 970 | TOTAL_DEVENGOS | `TOTAL_EARNING` | 970 |
| 980 | TOTAL_DEDUCCIONES | `TOTAL_DEDUCTION` | 980 |
| 990 | LIQUIDO_A_PAGAR | `NET_PAY` | 990 |

Sus fuentes provienen de relaciones `FEED_DEPENDENCY` desde los conceptos que los alimentan (ej. 101 → 970 y 101 → 990). La ejecución suma los importes de sus fuentes, aplicando inversión de signo si `invertSign = true`.

El concepto 980 (TOTAL_DEDUCCIONES) no se siembra en `concept_assignment` mientras no haya conceptos de deducción reales, ya que el plan builder lanzaría `MissingAggregateSourcesException` al no encontrar feed sources.

---

## Consecuencias

### Positivas

- El lanzador de nómina ya no contiene lógica de negocio específica por concepto. La parametrización en base de datos dicta completamente qué se calcula.
- Añadir un nuevo concepto elegible es solo insertar una fila en `concept_assignment` y definir las dependencias/fuentes correspondientes.
- La nómina persiste 970 y 990 además de 101, dando una vista real de devengos totales y líquido a pagar.
- El pipeline completo (elegibilidad → expansión → grafo → plan → ejecución) está cubierto por tests unitarios e integración E2E.

### Costes / Restricciones

- `concept_assignment` debe estar correctamente sembrado para cada convenio. Si está vacío, no se calcula ningún concepto (sin error implícito: la nómina se persistirá con 0 líneas).
- Los conceptos AGGREGATE con `concept_assignment` activo pero sin feed sources lanzarán `MissingAggregateSourcesException` en tiempo de construcción del plan.
- El modo `MINIMAL_REAL` queda retirado (`UnsupportedOperationException`). Solo `ELIGIBLE_REAL` y `FAKE` son modos operativos.

---

## Relación con ADRs previos

| ADR | Relación |
|---|---|
| ADR-036 | Define `CalculationType` — ahora todos los tipos (DIRECT_AMOUNT, RATE_BY_QUANTITY, PERCENTAGE, AGGREGATE) se ejecutan en el mismo dispatcher |
| ADR-038 | Define las relaciones FEED_DEPENDENCY — ahora usadas en tiempo de ejecución para AGGREGATE |
| ADR-039 | Define el grafo de dependencias — complementado con aristas OPERAND_DEPENDENCY |
| ADR-040 | Define el modelo conceptual de macro-grafo + activación + plan — este ADR documenta su implementación real |
| ADR-043 | Propuso `payroll_object_activation` como mecanismo de activación — desplazado por `concept_assignment` |
| ADR-044 | Inició la dirección del grafo mínimo real — este ADR completa y generaliza esa dirección |

<!-- END FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md -->


---

# FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md
<a name="file-adr-046-conceptos-t-cnicos-base-de-per-odo-y-presencia-en-n-mina-md"></a>

<!-- BEGIN FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md -->

# ADR-046 — Conceptos técnicos base de período y presencia en nómina

## Estado

Aceptado.

## Contexto

El motor de nómina de B4RRHH ya permite calcular conceptos económicos y agregados como:

- SALARIO_BASE
- TOTAL_DEVENGOS
- TOTAL_DEDUCCIONES
- NETO

Sin embargo, algunos valores necesarios para calcular correctamente una nómina mensual todavía están implícitos o calculados en duro.

Ejemplos:

- días reales del mes
- días teóricos de nómina
- días de presencia del empleado en el período
- días de presencia por subperíodo o segmento

Estos valores no son conceptos económicos visibles en el recibo, pero sí son datos fundamentales para explicar y trazar el cálculo.

## Problema

Si estos valores permanecen ocultos dentro del cálculo:

- el salario base no es completamente trazable;
- se dificulta depurar altas, bajas y meses incompletos;
- se complica evolucionar hacia cálculo por tramos;
- no queda claro qué cantidad de días ha alimentado cada concepto económico.

Además, no todos los conceptos del motor pueden depender de otros conceptos configurados.

En algún punto existen conceptos fundamentales que nacen directamente del contexto de ejecución.

## Decisión

Se introducen conceptos técnicos base de nómina con la nomenclatura D01/D02/D03, más cómoda para negocio.

Estos conceptos:

- se calculan durante la ejecución mediante clases Java específicas (`CalculationType.JAVA_PROVIDED`);
- tienen `FunctionalNature.TECHNICAL` para distinguirlos de conceptos económicos;
- pueden alimentar otros conceptos como operandos del grafo;
- no se muestran en el recibo de nómina (`payslipOrderCode = null`);
- no representan devengos ni deducciones.

## Conceptos técnicos

### D01 — DIAS_DEVENGO

Días de devengo mensual del segmento. Valor que se usa como operando QUANTITY de SALARIO_BASE.

Regla V1:

```
D01 = min(daysInSegment, 30)
```

Ejemplos:

- empleado activo todo abril (30 días): 30
- empleado activo todo marzo (31 días): 30 (topado)
- alta el 10 de marzo (22 días en segmento): 22
- baja el 20 de febrero (20 días en segmento): 20

Sustituye al anterior D01 (DIRECT_AMOUNT, CONSTANT=30 fijo), que no contemplaba meses parciales.

### D02 — DIAS_MES_NOMINA

Días teóricos del mes de nómina. Denominador convencional para el cálculo mensual.

Regla V1:

```
D02 = 30 (siempre)
```

Febrero, meses de 31 días y cualquier mes se tratan como 30 a efectos de nómina mensual.

### D03 — DIAS_MES_REALES

Días naturales reales del mes de cálculo.

Regla V1:

```
D03 = periodStart.lengthOfMonth()
```

Ejemplos:

- enero: 31
- febrero: 28 o 29 (año bisiesto)
- abril: 30

## Relación con SALARIO_BASE

Para salario base mensual V1:

```
SALARIO_BASE = D01 × P01
```

Donde `P01` (PRECIO_DIA) se obtiene de tabla de tarifas por categoría de convenio.

Con D01 dinámico por segmento, el empleado con alta el 10 de marzo cobra proporcionalmente
(`22 × P01`) en lugar del mes completo (`30 × P01`).

## Implementación

`CalculationType.JAVA_PROVIDED` identifica estos conceptos en el grafo. El `DefaultSegmentExecutionEngine`
y el `CalculatePayrollUnitService` despachan a la implementación registrada por Spring mediante
`List<TechnicalConceptCalculator>`.

Calculadores registrados:

| Concepto | Clase                              |
|----------|------------------------------------|
| D01      | `AccrualDaysConceptCalculator`     |
| D02      | `PayrollMonthDaysConceptCalculator`|
| D03      | `CalendarDaysConceptCalculator`    |

La interfaz `TechnicalConceptCalculator` recibe un `TechnicalConceptSegmentData` (solo fechas y
`daysInSegment`) para mantener el contrato estrecho y fácilmente testeable.

## Regla de visibilidad

Los conceptos técnicos base tienen `payslipOrderCode = null`. No se persisten como líneas de recibo.
Son calculados y disponibles en el estado de ejecución para ser operandos de otros conceptos.

## Regla arquitectónica

Las clases `TechnicalConceptCalculator` no deben convertirse en un motor paralelo.

Solo pueden calcular conceptos técnicos fundamentales derivados de:

- período de nómina;
- calendario mensual;
- presencia del empleado;
- segmentos temporales efectivos.

No deben calcular conceptos económicos como salario base, antigüedad, nocturnidad, IRPF, totales o neto.

## Segmentación futura

Cuando exista cálculo por subperíodos, D01 ya está preparado: opera sobre `daysInSegment`
(el segmento activo), no sobre `daysInPeriod`. D02 y D03 son invariantes del período y
devuelven el mismo valor en todos los segmentos de un mismo mes.

## Consecuencias positivas

- Mejora la trazabilidad del cálculo de SALARIO_BASE.
- Elimina el hardcode de `D01_FIXED_30`.
- Prepara el motor para altas, bajas y cambios de situación dentro del mes.
- Permite explicar el salario base con meses parciales.
- Mantiene limpio el recibo.
- Encaja con el grafo de conceptos sin forzar que todo sea configurable.

## Riesgos

**1. Abusar de JAVA_PROVIDED**

No todo helper interno debe convertirse en concepto técnico. Solo deben modelarse
valores relevantes para explicar el cálculo.

**2. Crear un segundo motor en Java**

Las clases técnicas no deben calcular conceptos económicos.

**3. Naming ambiguo**

Se adopta la nomenclatura D01/D02/D03 por ser más cómoda para negocio y no generar
confusión entre DIAS_MES_REALES (D03) y DIAS_MES_NOMINA (D02).

<!-- END FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md -->


---

# FILE: ADR-047-lifecycle-workflow-participant-pattern.md
<a name="file-adr-047-lifecycle-workflow-participant-pattern-md"></a>

<!-- BEGIN FILE: ADR-047-lifecycle-workflow-participant-pattern.md -->

# ADR-047 — Lifecycle Workflow Participant Pattern (Hire / Terminate)

## Estado

Propuesto

---

## Contexto

ADR-007 y ADR-018 establecieron que los workflows de ciclo de vida (Hire, Terminate, Rehire) se implementan como servicios de orquestación en la capa de aplicación: un único `@Transactional` que coordina múltiples verticales y garantiza coherencia temporal.

Esa decisión sigue siendo correcta. El problema es la **implementación concreta** que ha emergido de ella.

`HireEmployeeService` actualmente:

- Tiene **10 dependencias inyectadas** en el constructor
- Ocupa **358 líneas**
- Ejecuta **8 sub-operaciones** secuenciales dentro del método `hire()`
- `HireEmployeeExceptionHandler` importa excepciones de **6 verticales distintos**

El patrón real que ha emergido es este:

```
HireEmployeeService → CreatePresenceUseCase
                    → CreateContractUseCase
                    → CreateWorkCenterUseCase
                    → CreateCostCenterDistributionUseCase
                    → CreateWorkingTimeUseCase
                    → CreateLaborClassificationUseCase
                    → NextEmployeeNumberPort
                    → WorkCenterCompanyValidator
                    → EmployeeTypeCatalogValidator
```

Cada nuevo vertical que necesita participar en el hire obliga a:

1. Añadir una dependencia al constructor de `HireEmployeeService`
2. Añadir la llamada dentro del método `hire()`
3. Añadir un `@ExceptionHandler` en `HireEmployeeExceptionHandler` para las excepciones de ese vertical

Esto viola Open/Closed: el servicio más crítico del sistema cambia con cada feature nueva.

El patrón se reproduce en `TerminateEmployeeService` y `RehireEmployeeService`.

---

## Problema

El diseño actual acopla el servicio orquestador con cada vertical que participa en el workflow. Añadir un nuevo vertical al hire no es una operación local — requiere modificar el servicio central.

Esto no es un problema de complejidad (el hire ES complejo por dominio), sino un problema de **dirección de las dependencias**.

---

## Decisión

Se introduce el **Lifecycle Workflow Participant Pattern**:

> En vez de que el servicio de lifecycle conozca cada vertical, cada vertical se registra como participante del workflow.

### 1. Puerto `HireParticipant`

```java
// employee/lifecycle/application/port/HireParticipant.java
public interface HireParticipant {
    int order();
    void participate(HireContext ctx);
}
```

`order()` garantiza la secuencia de ejecución. Los valores de orden son:

| Orden | Participante | Razón |
|-------|-------------|-------|
| 10 | EmployeeCoreParticipant | El employee debe existir antes que todo |
| 20 | PresenceParticipant | Require employee.id |
| 30 | WorkCenterParticipant | Require employee.id |
| 40 | CostCenterParticipant | Require employee.id (opcional) |
| 50 | ContractParticipant | Require employee.id |
| 60 | LaborClassificationParticipant | Require employee.id |
| 70 | WorkingTimeParticipant | Require employee.id |

### 2. Objeto de contexto `HireContext`

`HireContext` viaja por todos los participantes. Cada uno lee lo que necesita del comando y escribe su resultado:

```java
// employee/lifecycle/application/model/HireContext.java
public class HireContext {
    private final HireEmployeeCommand command;
    private final String employeeNumber;      // generado antes de los participantes
    private Employee employee;
    private Presence presence;
    private WorkCenter workCenter;
    private CostCenterDistribution costCenterDistribution;
    private Contract contract;
    private LaborClassification laborClassification;
    private WorkingTime workingTime;

    // getters y setters
    public HireEmployeeResult toResult() { ... }
}
```

### 3. `HireEmployeeService` tras el refactor

```java
@Service
public class HireEmployeeService implements HireEmployeeUseCase {

    private final List<HireParticipant> participants;
    private final NextEmployeeNumberPort nextEmployeeNumberPort;
    private final HireEmployeePreConditionValidator validator;

    public HireEmployeeService(
            List<HireParticipant> participants,
            NextEmployeeNumberPort nextEmployeeNumberPort,
            HireEmployeePreConditionValidator validator) {
        this.participants = participants.stream()
                .sorted(Comparator.comparingInt(HireParticipant::order))
                .toList();
        this.nextEmployeeNumberPort = nextEmployeeNumberPort;
        this.validator = validator;
    }

    @Override
    @Transactional
    public HireEmployeeResult hire(HireEmployeeCommand command) {
        validator.validate(command);
        String employeeNumber = nextEmployeeNumberPort.consumeNext(
                normalizeCode(command.ruleSystemCode()));
        HireContext ctx = new HireContext(command, employeeNumber);
        participants.forEach(p -> p.participate(ctx));
        return ctx.toResult();
    }
}
```

**Añadir un nuevo vertical al hire = crear un fichero `@Component` que implementa `HireParticipant`. `HireEmployeeService` nunca vuelve a cambiar.**

### 4. Extracción de `HireEmployeePreConditionValidator`

Las validaciones previas al hire (tipo de empleado, coherencia work center / company) se extraen a un servicio dedicado con sus propios tests unitarios:

```java
// employee/lifecycle/application/service/HireEmployeePreConditionValidator.java
@Component
public class HireEmployeePreConditionValidator {
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;
    private final WorkCenterCompanyValidator workCenterCompanyValidator;

    public void validate(HireEmployeeCommand command) { ... }
}
```

### 5. Exception handlers descentralizados

Con el patrón participant, cada vertical puede manejar sus propias excepciones mediante un `@RestControllerAdvice` scoped a `HireEmployeeController`:

```java
// workcenter/infrastructure/web/WorkCenterHireExceptionHandler.java
@RestControllerAdvice(assignableTypes = HireEmployeeController.class)
@Order(10)
public class WorkCenterHireExceptionHandler {
    @ExceptionHandler(WorkCenterCatalogValueInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handle(...) { ... }
}
```

`HireEmployeeExceptionHandler` queda únicamente con las excepciones propias del lifecycle (autonumeración, validaciones transversales).

### 6. El mismo patrón para TERMINATION

```java
// employee/lifecycle/application/port/TerminationParticipant.java
public interface TerminationParticipant {
    int order();
    void participate(TerminationContext ctx);
}
```

`TerminationContext` lleva el `Employee` existente, la fecha de terminación, el motivo, y acumula los resultados del cierre de cada vertical (presence.endDate, contract.endDate, etc.).

El orden en termination es inverso a hire en semántica (cerrar desde fuera hacia dentro):

| Orden | Participante |
|-------|-------------|
| 10 | WorkingTimeTerminationParticipant |
| 20 | WorkCenterTerminationParticipant |
| 30 | CostCenterTerminationParticipant |
| 40 | ContractTerminationParticipant |
| 50 | LaborClassificationTerminationParticipant |
| 60 | PresenceTerminationParticipant | ← cierra presence al final |

Presence es la última en cerrarse porque es el eje del lifecycle (ADR-018): cerrarla primero implicaría que el empleado está "fuera" mientras aún tiene contratos abiertos.

---

## Invariantes que NO cambian

- **La transacción sigue siendo única.** Todos los participantes se ejecutan dentro del `@Transactional` del servicio orquestador. Si cualquier participante lanza, Spring hace rollback de todo. El comportamiento transaccional es idéntico al actual.
- **No se introducen domain events asíncronos.** Los eventos síncronos de Spring (`@EventListener`) son una alternativa válida al participant port, pero añaden indirección sin ventaja real en este contexto. Se descarta.
- **No se introducen sagas ni consistencia eventual.** El hire necesita devolver el número de matrícula generado en la misma petición HTTP. La atomicidad no es negociable.
- **El orden sigue siendo explícito y auditable.** `order()` como entero en la interfaz es deliberadamente simple — no hay grafos de dependencias ni resolución dinámica. Si el orden cambia, se ve en el diff.

---

## Consecuencias positivas

- `HireEmployeeService` y `TerminateEmployeeService` se vuelven estables — no cambian al añadir nuevos verticales
- Cada vertical es dueño de su lógica de participación en el workflow (cohesión)
- Los tests de cada participante son unitarios y pequeños
- `HireEmployeeExceptionHandler` pierde el conocimiento de verticales externos
- La misma base sirve para Rehire sin duplicar la orquestación

## Consecuencias negativas / riesgos

- El orden de participación es implícito al leer el código del servicio — hay que consultar las implementaciones para ver la secuencia completa. **Mitigación:** documentar el orden en `HireContext` con un comment de referencia.
- Spring inyecta `List<HireParticipant>` en el orden que descubre los beans, por lo que el `order()` explícito es crítico — un test de contexto debe verificar el orden en cada release.
- La migración de `HireEmployeeService` al patrón debe hacerse gradualmente (un participante cada vez) para no introducir regresiones.

---

## Alternativas descartadas

### Mantener el orquestador actual y aceptar el crecimiento

Descartado:

- 10 dependencias hoy, 15 en 12 meses
- Cada feature de lifecycle toca el servicio más crítico del sistema
- No hay punto de estabilización natural

### Domain events asíncronos (ApplicationEventPublisher + @TransactionalEventListener)

Descartado:

- Añade indirección sin ventaja en un contexto single-service
- El orden de los handlers es menos explícito
- El debugging es más complejo
- Para lograr el mismo orden de operaciones se necesita `@Order` igualmente

### Saga pattern (compensating transactions)

Descartado:

- Consistencia eventual no es apropiada para hire
- El número de matrícula debe estar disponible sincrónicamente
- Añade infraestructura de saga que no existe ni se necesita

### Employee como agregado monolítico

Descartado desde ADR-002 y ADR-007. La arquitectura vertical se mantiene.

---

## Relación con ADRs anteriores

| ADR | Relación |
|-----|----------|
| ADR-007 | Este ADR evoluciona la implementación de los workflows definidos allí. Los workflows siguen siendo orquestados y transaccionales. |
| ADR-018 | La sección "Orquestación interna" de ADR-018 queda reemplazada por este patrón. El modelo de datos, la API y las validaciones no cambian. |

---

## Resumen

El Lifecycle Workflow Participant Pattern invierte la dependencia entre el orquestador y los verticales:

- Antes: el servicio conoce cada vertical
- Después: cada vertical se registra en el servicio

La transacción, el orden de ejecución y el contrato de API permanecen inalterados. Lo que cambia es que añadir un nuevo vertical al hire o al terminate se convierte en una operación local (crear un fichero), no en una modificación del servicio central.

<!-- END FILE: ADR-047-lifecycle-workflow-participant-pattern.md -->


---

# FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md
<a name="file-adr-048-modelo-de-cotizacion-ss-e-irpf-md"></a>

<!-- BEGIN FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md -->

# ADR-048 — Modelo de cotización de Seguridad Social e IRPF

## Estado

Aceptado. **Documenta una implementación ya existente**, no propone una nueva.

El grafo de cotización se construyó entre V77 y V91 sin ADR que lo respaldara. Este
documento reconstruye la decisión tal como está en el código a 25 de agosto de 2026 y separa
explícitamente lo que es decisión firme de lo que es provisional. Escribirlo tarde tiene un
coste: partes de este ADR describen elecciones que se tomaron sobre la marcha y que quizá no
se habrían tomado igual con la discusión delante. Se señalan como tales.

---

## Contexto

Los ADR-033 a ADR-046 fijan el metamodelo del motor: `PayrollObject` como raíz,
`PayrollConcept` con tipología de cálculo, operandos resueltos por source, agregación por
feed, grafo acíclico, plan topológico y segmentación temporal.

Sobre esa base había que calcular lo que convierte un devengo en una nómina de verdad: la
cotización a la Seguridad Social y la retención de IRPF. Eso obligó a resolver cuatro cosas
que el metamodelo no cubría:

1. **Topes.** La base de cotización no es el devengo: es el devengo recortado entre un tope
   máximo y un tope mínimo que dependen del grupo de cotización del empleado.
2. **Coste de empresa.** Hay conceptos que se calculan y se muestran pero no descuentan del
   líquido.
3. **Valores que no salen de otro concepto.** Un tipo de cotización o un tope no se calcula:
   se consulta.
4. **Prorrateo de los topes cuando el mes está partido en segmentos.**

---

## Decisión

**La cotización se modela dentro del mismo grafo de conceptos que el resto de la nómina.**
No hay motor paralelo, ni servicio de cotización, ni lógica de negocio fuera del grafo. Un
tope es un nodo; un tipo es un nodo; la base de cotización es un concepto con su código.

De ahí se derivan las cuatro decisiones concretas de este ADR.

### 1. Los topes se aplican con dos tipologías nuevas: `LEAST` y `GREATEST`

ADR-036 declaró cuatro tipologías canónicas y avisó de que no se crearían más por cada caso
de negocio. Aquí se añaden dos, y la justificación es la que el propio ADR-036 exige: no
describen un caso de negocio, describen **operadores** que faltaban.

```
B_CC_MAX = LEAST(B01, P_TOPE_MAX)          ← el devengo, recortado por arriba
B_CC     = GREATEST(B_CC_MAX, P_TOPE_MIN)  ← y después levantado por abajo
```

Cada una toma dos operandos con roles nuevos, `LEFT` y `RIGHT`, añadidos a `OperandRole`.

**El orden importa y es deliberado**: primero el techo, después el suelo. La consecuencia
funcional es que un empleado cuya base queda por debajo del mínimo de su grupo cotiza por el
mínimo aunque haya devengado menos.

### 2. La base de cotización es un concepto, no un campo calculado

```
B01   BASE_COTIZABLE   AGGREGATE   alimentado hoy únicamente por 101
B_CC  BASE_COTIZACION_COTIZ        la base ya recortada, que usan todos los tipos
```

Todos los conceptos de cotización —de trabajador y de empresa— cuelgan de `B_CC`, nunca de
`B01`. V88 reescribió el operando `BASE` del concepto 700 justamente para eso.

Que la base sea un nodo del grafo y no un valor interno es lo que permite explicar una
nómina: se puede preguntar cuánto valía la base y por qué.

### 3. El coste de empresa se calcula, se muestra y no descuenta

Los conceptos 720–724 tienen `functionalNature = INFORMATIONAL` y **no tienen relación de
feed hacia 980**. Se calculan sobre la misma `B_CC`, aparecen en el recibo con su
`payslipOrderCode`, y no tocan el líquido.

Es la aplicación directa de la regla de ADR-036: la naturaleza funcional es presentación y
semántica; quien decide si un importe resta es la relación de feed, no el tipo de cálculo.

### 4. Los valores que se consultan entran como nodos `ENGINE_PROVIDED`

`ENGINE_PROVIDED` (renombrado desde `JAVA_PROVIDED` en V89) identifica conceptos cuyo valor
lo produce una clase Java que implementa `TechnicalConceptCalculator`, registrada por Spring
en `TechnicalConceptCalculatorRegistry` bajo su código de concepto.

Se mantiene la regla de ADR-046: **estas clases no pueden calcular conceptos económicos.**
Solo resuelven valores que se consultan o se derivan del contexto de ejecución — tipos,
topes, días, coeficientes. La frontera es la que separa «consultar un dato» de «calcular una
nómina».

---

## El grafo implementado

```
                      101 SALARIO_BASE
                        │        │
                        │        └──────────────► 970 TOTAL_DEVENGOS ──┐
                        ▼                                              │
                  B01 BASE_COTIZABLE                                   │
                        │                                              │
        P_TOPE_MAX ──►  LEAST  ──► B_CC_MAX                            │
                                      │                                │
        P_TOPE_MIN ──► GREATEST ──►  B_CC                              │
                                      │                                ▼
      ┌───────────────────────────────┼────────────────────┐      990 LIQUIDO
      │            trabajador         │      empresa       │           ▲
      ▼                               ▼                    ▼           │
  700 CC 4,70 %                720 CC 23,60 %                          │
  703 DESEMPLEO 1,55 %         721 DESEMPLEO 7,05 %                    │
  701 FP 0,10 %                722 FP 0,60 %          (INFORMATIONAL,  │
  702 MEI 0,11 %               723 FOGASA 0,20 %       sin feed a 980) │
  800 IRPF 15,00 %             724 MEI 0,58 %                          │
      │                                                                │
      └──────────────► 980 TOTAL_DEDUCCIONES ──────────(invert_sign)───┘
```

### Conceptos

| Código | Mnemónico | Tipo | Naturaleza | Fórmula | Recibo |
|---|---|---|---|---|---|
| `B01` | BASE_COTIZABLE | AGGREGATE | BASE | ← 101 | no |
| `B_CC_MAX` | BASE_COTIZACION_MAX | LEAST | BASE | min(B01, P_TOPE_MAX) | no |
| `B_CC` | BASE_COTIZACION_COTIZ | GREATEST | BASE | max(B_CC_MAX, P_TOPE_MIN) | no |
| `700` | CC_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 4,70 % | 700 |
| `703` | DESEMPLEO_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 1,55 % | 703 |
| `701` | FP_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 0,10 % | 701 |
| `702` | MEI_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC × 0,11 % | 702 |
| `800` | RETENCION_IRPF | PERCENTAGE | DEDUCTION | B01 × 15,00 % | 800 |
| `720` | SS_CC_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 23,60 % | 720 |
| `721` | SS_DESEMPLEO_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 7,05 % | 721 |
| `722` | SS_FP_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,60 % | 722 |
| `723` | SS_FOGASA_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,20 % | 723 |
| `724` | SS_MEI_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC × 0,58 % | 724 |

Los nodos `P_*` (`P_TOPE_MAX`, `P_TOPE_MIN`, `P_SS_CC`, `P_SS_DESEMPLEO`, `P_FP_TRAB`,
`P_MEI_TRAB`, `P_IRPF`, `P_SS_CC_EMP`, `P_SS_DESEMPLEO_EMP`, `P_SS_FP_EMP`,
`P_SS_FOGASA_EMP`, `P_SS_MEI_EMP`) son todos `ENGINE_PROVIDED` con naturaleza `TECHNICAL` y
sin línea de recibo.

> **Nota**: el concepto 800 (IRPF) toma como base `B01`, no `B_CC`. Es correcto —la base de
> retención no es la de cotización— pero conviene tenerlo presente: hoy coinciden porque
> ambas se alimentan solo del salario base, y dejarán de coincidir en cuanto B01 crezca.

---

## Topes: origen, vigencia y prorrateo

Los topes viven en `payroll_engine.ss_cotizacion_topes`, con vigencia por fecha:

```
(rule_system_code, grupo_code, period_type, base_min, base_max, valid_from, valid_to)
```

Sembrada con los valores TGSS de 2025: grupos 01–07 en `MENSUAL`, grupos 08–11 en `DIARIO`,
tope máximo único de 4.909,50 €/mes y 163,65 €/día.

**El grupo de cotización y el tipo de nómina del empleado salen de
`rulesystem.agreement_category_profile`** (V85, sembrada en V87), que asocia cada categoría
del convenio a su grupo y a `MENSUAL` o `DIARIO`. La cadena completa es:

```
empleado → labor_classification → categoría de convenio → agreement_category_profile
        → (grupo de cotización, tipo de nómina) → ss_cotizacion_topes
```

### Prorrateo por segmento

`TopeMaxCotizacionCalculator` y `TopeMinCotizacionCalculator` no devuelven el tope del mes:
devuelven la parte que corresponde al segmento.

```
MENSUAL:  tope × díasDelSegmento / díasDelPeríodo
DIARIO:   tope × díasDelSegmento
```

Y por eso V90 les puso `result_composition_mode = ACCUMULATE`: los trozos de todos los
segmentos se suman y el resultado consolidado es el que recortan `B_CC_MAX` y `B_CC`.

Es la decisión menos evidente de todo el modelo y merece quedar escrita: **los topes son lo
único que se calcula por segmento y se acumula, mientras que todos los conceptos de
cotización tienen `executionScope = PERIOD`.** Si alguien cambia el ámbito de un concepto de
cotización a `SEGMENT` sin entender esto, el recorte deja de cuadrar.

---

## Lo que es firme y lo que es provisional

### Firme

- La cotización vive en el grafo. No hay motor paralelo.
- Los topes se aplican con operadores del grafo (`LEAST`/`GREATEST`), no con condicionales en
  Java.
- El coste de empresa es informativo y no alimenta 980.
- La base de cotización es un concepto con identidad propia.
- El grupo de cotización se deriva de la categoría del convenio, no se informa a mano.

### Provisional — y conscientemente

1. **Los tipos están escritos en Java.** `payroll_engine.ss_cotizacion_tipos` existe, está
   creada y sembrada con los tipos de 2025 por contingencia… y **no la lee nadie**. Los diez
   tipos son constantes en sus respectivas clases `*RateCalculator`. Es la deuda más grave de
   este modelo: cuando la TGSS cambie los tipos habrá que tocar código y desplegar, teniendo
   la tabla al lado.
2. **El IRPF es un 15 % fijo.** `IrpfWithholdingRateCalculator` lo dice en su javadoc: es un
   marcador de posición. El tipo real depende de los datos fiscales del empleado y se
   regulariza. La vertical `employee.tax_information` ya existe (V95); no está conectada.
3. **`P_SS` (6,35 % todo en uno) quedó muerto** al partirse el 700 en V91. El concepto y su
   `SsContributionRateCalculator` siguen en el árbol sin que nadie los referencie.
4. **No hay accidentes de trabajo (725).** Requiere la tarifa por CNAE de cada empresa; queda
   aplazado explícitamente desde V88.
5. **`B01` solo se alimenta del salario base.** Falta la prorrata de pagas extra y cualquier
   otro devengo cotizable. Mientras eso siga así, la base de cotización de esta nómina no es
   la real.
6. **Todo está sembrado para un único convenio** (99002405011982) y un único rule system.

---

## Invariantes

- Ningún concepto de cotización toma su base de `B01`: todos cuelgan de `B_CC`. La excepción
  documentada es 800 (IRPF), cuya base es otra por definición.
- Los conceptos de empresa nunca alimentan 980.
- `P_TOPE_MAX` y `P_TOPE_MIN` deben mantener `ACCUMULATE`; el resto de la cotización, `PERIOD`.
- Un tipo nuevo de cotización se añade como concepto `ENGINE_PROVIDED` + concepto
  `PERCENTAGE` + relación de feed, nunca como rama dentro de un calculador existente.
- Las clases `TechnicalConceptCalculator` no calculan conceptos económicos.

---

## Deuda que este ADR deja abierta

| # | Deuda | Dónde |
|---|---|---|
| 1 | `ss_cotizacion_tipos` no la lee nadie; los tipos son constantes Java | los 10 `*RateCalculator` |
| 2 | IRPF fijo al 15 %, con `employee.tax_information` ya disponible | `IrpfWithholdingRateCalculator` |
| 3 | `P_SS` y `SsContributionRateCalculator` muertos desde V91 | `payroll_engine` |
| 4 | Sin AT/EP (725): falta tarifa CNAE por empresa | V88, aplazado |
| 5 | `B01` incompleta: sin prorrata de extras ni otros devengos cotizables | grafo de feeds |

---

## Relación con ADR anteriores

| ADR | Relación |
|---|---|
| ADR-036 | Amplía las tipologías canónicas de cuatro a seis con `LEAST` y `GREATEST`, por el criterio que el propio ADR-036 fija: son operadores, no casos de negocio |
| ADR-038 | La separación trabajador/empresa se resuelve con `FEED_BY_SOURCE`: quien alimenta 980 descuenta, quien no, informa |
| ADR-041 | El prorrateo de topes por segmento con consolidación `ACCUMULATE` es la aplicación concreta de la segmentación temporal |
| ADR-045 | La elegibilidad de todos estos conceptos se resuelve por `concept_assignment` |
| ADR-046 | `ENGINE_PROVIDED` es el `JAVA_PROVIDED` de ese ADR, renombrado en V89; la regla de que estas clases no calculan conceptos económicos se mantiene intacta |

<!-- END FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md -->


---

# FILE: ADR-049-arquitectura-de-informacion-del-frontend.md
<a name="file-adr-049-arquitectura-de-informacion-del-frontend-md"></a>

<!-- BEGIN FILE: ADR-049-arquitectura-de-informacion-del-frontend.md -->

# ADR-049 — Arquitectura de información del frontend

## Estado
Aceptado

## Contexto

La aplicación creció por verticales, y la navegación creció con ella. El resultado, medido sobre
el árbol actual:

- `shared/ui` contiene un sistema de maestro-detalle completo y documentado, y lo usan solo
  `company` (791 líneas de html/scss) y `work-center` (596). `employee` son **7.530 líneas en 256
  ficheros** y no usa casi nada de él. El sistema se validó en la periferia y nunca llegó al centro.
- El menú calca los bounded contexts del backend, no la jornada de nadie.
- «Catálogos» es una entrada de menú que nombra un **mecanismo de almacenamiento**, no un concepto
  del negocio. ADR-012 ya observó que exponer el metamodelo por una pantalla de catálogos «ha hecho
  visible una tensión de diseño» y que hay `rule_entity_type` nombrados desde la vertical donde el
  concepto apareció primero. No es casualidad: una pantalla organizada por mecanismo obliga a
  nombrar cada concepto por su mecanismo.
- `rule_system`, que ADR-003 define como el contexto regulatorio, aparece como la última entrada del
  menú bajo «Configuración», en inglés.

## Decisión

### 1. Cuatro grupos

| Grupo | Contenido | Criterio |
|---|---|---|
| Empleados | El directorio y la ficha | El core |
| Organización | Empresas, centros de trabajo, centros de coste | Lo decide la empresa |
| Sociedad | Convenios, reglamentación | Viene impuesto de fuera |
| Nóminas | Ejecuciones y sus recibos | El trabajo batch |

La separación Organización / Sociedad no es cosmética: distingue lo que la empresa decide de lo que
le imponen, y eso cambia lo que la interfaz debe permitir. En Organización se edita; en Sociedad se
consultan datos con vigencia que no se pueden cambiar.

**Test de colocación de cualquier entidad: ¿quién manda sobre este dato, nosotros o el BOE?**

### 2. El sistema de reglas es ámbito, no destino

`rule_system` sale de la lista de navegación y pasa al cromo, junto a la marca, visible siempre.
Casi todo lo que se muestra está dentro de uno —catálogos, convenios, la ficha, la nómina—, igual
que el ejercicio en un programa de contabilidad. Con un solo sistema activo se muestra igualmente.

Administrar sistemas de reglas sigue existiendo como tarea, pero es otra cosa y va en otro sitio.

### 3. Los maestros se colocan por significado, no por mecanismo

Que algo se guarde como `rule_entity` y otra cosa tenga agregado propio es un hecho del esquema,
invisible para quien usa la aplicación. Quien busca *tipos de contrato* y quien busca *centros de
trabajo* están haciendo lo mismo: decidir qué se le puede poner a un empleado.

- **Se mantiene** la pantalla genérica dirigida por metadatos. Es la ganancia del metamodelo.
- **Desaparece** «Catálogos» como grupo de menú. Cada maestro se lista bajo Organización o bajo
  Sociedad según el test anterior, servido por el mismo componente genérico.
- Como un maestro nuevo aparece **declarando**, sin tocar código, **el menú de esa sección debe
  generarse** desde los `rule_entity_type`. Si añadir un maestro obliga a editar el menú a mano, se
  pierde la propiedad que hacía valioso el metamodelo. Esto exige metadatos de agrupación y
  visibilidad en `rule_entity_type` (b4rrhh/backend#15) y es el mismo campo que ADR-012 necesita
  para pagar su deuda semántica.

### 4. El modelo de página es directorio + página completa

Se descarta el maestro-detalle permanente como patrón general. La ficha del empleado no cabe en un
panel de detalle: el área laboral ya necesita todo el ancho para sus tablas de periodos, y un panel
fijo cuesta unos 350 px en la pantalla que más ancho necesita.

`company` y `work-center` se mudan al modelo de la ficha, no al revés. Las piezas de maestro-detalle
de `shared/ui` quedan superadas y **no deben usarse en pantallas nuevas**.

### 5. La selección múltiple es una cola de trabajo

Lo que hace falta no es ver la lista todo el rato: es no perder la selección. Se resuelve en el raíl
de la ficha, que muestra la cola con los nombres cuando se llega desde una selección, y solo la
identidad cuando se llega a uno solo. Coste de ancho: cero cuando no hay cola.

**La cola no escala a cientos, y no debe fingir que sí.** Revisar cien fichas de una en una es la
herramienta equivocada: eso se responde añadiendo la columna al directorio y ordenando por ella.

## Consecuencias

- El directorio necesita un modelo de lectura más rico del que hay
  (`EmployeeDirectoryItemResponse` hoy solo devuelve claves, `displayName`, `status` y
  `workCenterCode`). La fase 4 tiene una mitad de backend.
- El reparto fino de maestros entre Organización y Sociedad **no sale limpio**: hay tipos con
  opciones de las dos clases. Cuando un tipo no caiga claramente de un lado, es señal de que ese
  tipo son dos.
- ADR-004 sigue abierto. Esta decisión no depende de la forma de la business key.

## Fuera de alcance

La pantalla de inicio (bandeja de trabajo frente a panel de métricas) sigue sin decidir.

<!-- END FILE: ADR-049-arquitectura-de-informacion-del-frontend.md -->


---

# FILE: ADR-050-esqueleto-de-pagina.md
<a name="file-adr-050-esqueleto-de-pagina-md"></a>

<!-- BEGIN FILE: ADR-050-esqueleto-de-pagina.md -->

# ADR-050 — Esqueleto de página

## Estado
Aceptado

## Contexto

Cada pantalla se inventa su disposición. La prueba más limpia es el panel «Historial» de la ficha
del empleado: aparece en las tres áreas —resumen, personales, laborales— en **tres posiciones
distintas, con tres anchos distintos**, sin alinearse con nada de lo que tiene debajo. Si un
componente no sabe dónde va, es que no hay ningún sitio donde deba ir.

De ahí sale todo lo demás:

- El área laboral es una página de dos columnas donde la izquierda tiene una caja y **600 px de nada**.
- El área personal deja el 85 % de la pantalla en blanco.
- El directorio ocupa poco más de la mitad del ancho disponible y desperdicia el resto.
- Las acciones de página («Calcular nómina», «Acciones») viven **dentro de una card**, en el sitio de
  un dato.
- El raíl de identidad se colapsa a iconos en un área y va con etiquetas en las otras dos, de forma
  que en la pantalla de resumen **desaparece el nombre del empleado**.

Ninguno de estos es un fallo de estilo. Son síntomas de que no hay un plano.

## Decisión

Existe **un único esqueleto de página** al que las pantallas se acogen, con cuatro huecos nombrados:

| Hueco | Qué lleva | Notas |
|---|---|---|
| `identidad` | Quién o qué se está mirando, y las acciones de página | Franja superior, ancho completo |
| `raíl` | Índice de la página y, si la hay, la cola de trabajo | Izquierda, plegable como una unidad |
| `principal` | El contenido | Gobierna las columnas |
| `contextual` | Paneles secundarios: historial, ayuda, auditoría | Derecha, **plegado solo si no cabe** |

### Reglas

1. **Las acciones de página van en `identidad`**, nunca dentro de una card del contenido.
2. **`contextual` se pliega por defecto solo cuando no cabe.** El estado inicial lo decide el ancho
   disponible: si desplegarlo deja a `principal` por encima de la medida de lectura, se abre; si la
   deja por debajo, se pliega. Medido en la ficha del empleado, pantalla de 2560 px: con el
   contextual cerrado la caja de contenido de `principal` mide 1980 px contra una medida de
   1400 px, o sea **580 px de papel muerto**; con el contextual abierto mide 1569 px, todavía
   169 px por encima de la medida. Abrirlo no aprieta el contenido: ocupa sitio que sobraba.
   «Plegado siempre» era una regla escrita antes de medir; el ancho es un dato, no una preferencia.
   El estado que elija el usuario se recuerda y manda sobre el inicial.
3. **El raíl se pliega entero**, no por partes. Índice y cola viven o desaparecen juntos.
4. **El menú principal se pliega a iconos**, y su estado se recuerda. Es la única navegación que
   admite plegarse a iconos, porque son pocos destinos usados a diario y se reconocen por la forma.
   **El índice del raíl no se pliega a iconos**: es un sumario que se lee de reojo, sus conceptos son
   vecinos entre sí —centro de trabajo y centro de coste, convenio y reglamentación— y obligar a
   pasar el ratón por cada uno lo convierte en una adivinanza. Se aprieta con tipografía, no con
   iconos.
5. **El índice informa, no solo navega**: lleva el recuento de cada sección y marca las vacías.
   Pero no todas igual: **en gris cuando el vacío es normal, en ocre de aviso cuando el vacío es un
   dato que debería estar**. Una lista de identificadores vacía es una lista vacía; un empleado sin
   centro de coste es una anomalía que alguien tiene que arreglar. Gris dice «no hay nada que ver»,
   ocre dice «falta algo», y son dos mensajes distintos que hasta ahora se escribían con el mismo
   color. Cuál de los dos le toca a cada sección **lo decide el dominio y lo declara la sección**;
   la pantalla no lo adivina del recuento.

   Lo excepcional se ve porque lo normal calla.

   Esa regla es sobre **repetición**, y por tanto vale en listas, no en fichas. En el directorio,
   250 insignias «Activo» idénticas son ruido: no distinguen a nadie y tapan las tres que dicen
   «Baja». En la ficha de un solo registro no hay repetición, luego no hay ruido, y el silencio ya no
   se lee como normalidad sino como ambigüedad: si el estado desaparece cuando es «Activo», quien
   mira no sabe si está activo o si el dato no ha llegado. **En listas se calla lo normal; en la
   ficha de un registro el estado se dice siempre**, aunque sea el estado de todos.
6. **La identidad no cambia entre secciones de la misma entidad.** Nunca puede desaparecer el nombre
   de lo que se está mirando.
7. **Todo hueco que recuerde su estado tiene que enseñar la salida desde ese estado.** El plegado se
   guarda en `localStorage`, así que un control que solo se ve estando desplegado no deja al usuario
   sin el panel: lo deja sin el panel **para siempre**, también al recargar, también en la sesión
   siguiente y en cualquier otra ficha del mismo tipo. El botón de plegar y el de desplegar son el
   mismo botón y tiene que verse en los dos estados.

   Esto no es hipotético. El raíl plegado llevaba `overflow: hidden` y se recortaba su propio botón,
   que va `absolute` sobre la costura. Quien lo plegó una vez dejó de ver el índice, la cola de
   trabajo y buena parte del rediseño, y no tenía forma de saber que seguían ahí: la pantalla no
   parecía rota, parecía que no se había hecho nada. Un estado recordado sin salida visible no es un
   defecto de un control, es una pantalla que miente sobre lo que existe.

### El ancho

Ancho completo no es la respuesta automática: una tabla de 2.500 px es ilegible porque el ojo pierde
la fila. **La medida de lectura manda sobre el ancho disponible**, y donde sobre espacio se usa para
poner cosas al lado, no para estirar. El esqueleto decide esto una vez, no cada pantalla.

## Consecuencias

- Migrar una pantalla al esqueleto **no debe obligar a reescribir su contenido**. Si obliga, el
  esqueleto está mal y hay que revisar este ADR. Esa restricción permite desplegar el esqueleto antes
  de rediseñar nada.
- Las clases de contenedor propias de cada feature dejan de tener sentido. Ver ADR-051.
- Los huecos se dimensionan una vez, en el esqueleto. Ninguna pantalla ajusta anchos por su cuenta.

## Fuera de alcance

Qué aspecto tiene cada bloque dentro de `principal`. Eso es ADR-051.

<!-- END FILE: ADR-050-esqueleto-de-pagina.md -->


---

# FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md
<a name="file-adr-051-el-contenedor-deriva-del-modo-de-mantenimiento-md"></a>

<!-- BEGIN FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md -->

# ADR-051 — El contenedor deriva del modo de mantenimiento

## Estado
Aceptado

## Contexto

Hoy el contenedor de una sección lo elige quien escribe la pantalla. El resultado, en una sola
pantalla —el área laboral de la ficha— es que **la presencia es una card y el contrato, la jornada y
el convenio no lo son**, siendo los cuatro del mismo rango y las cuatro secciones temporales. Ninguna
de las dos elecciones codifica nada: no se puede deducir por qué una tiene caja y las otras no.

Por debajo hay **19 ficheros `.scss`** que declaran cada uno su propia caja: `.presence-card`,
`.labor-section`, `.work-center-section`, `.work-center-detail-panel`, `.cost-center-section`,
`.dnf-card`, `.journey-presence-card`, `.header-box`, `.identity-panel`, `.list-panel`,
`.employee-timeline-panel`, `.tax-information-section`, `.working-time-section`,
`.labor-classification-section`, `.rehire-section`, `.panel`, `.section-card` y algunas más.

Ninguna se escribió de mala fe: cada una es alguien resolviendo el mismo problema otra vez.

Además, la presencia —que en el dominio gobierna sobre las demás verticales, hasta el punto de que
el flujo de cese la cierra la primera (ADR-047)— es visualmente lo menos importante de su pantalla.

## Decisión

**El modo de mantenimiento de una sección determina su contenedor, su cabecera y su acción.** El
vocabulario ya existe en ADR-010 y ADR-016: `SLOT`, `TEMPORAL_APPEND_CLOSE`, `WORKFLOW`, `READONLY`.

Consecuencia directa: **dos secciones con el mismo modo se ven iguales**. Al verla, ya se sabe qué se
puede hacer con ella, antes de leer su contenido. Hoy eso no es posible.

### Reglas

1. **Un modo, un tratamiento.** Presencia, contrato, jornada y convenio son todas
   `TEMPORAL_APPEND_CLOSE` y comparten caja, cabecera y acción. Lo único que distingue a la presencia
   es una marca de que **gobierna** sobre las demás.
2. **Los contenedores viven en `shared/ui`.** Una feature no declara cajas. Se pondrá un candado en
   el pipeline cuando la primera sección esté migrada.
3. **Nada de contenedor dentro de contenedor.** El caso actual —tres cards, cada una con una caja
   gris dentro para decir que no hay datos— es el ejemplo a no repetir. El estado vacío es contenido
   de la sección, no otra sección.
4. **El código nunca va solo.** `420` se muestra como «Indefinido a tiempo parcial» con el código
   debajo, en gris y en monoespaciada. Quien conoce el catálogo sigue leyendo el número; quien no,
   entiende la fila. Requiere el binding de literales de ADR-015.
5. **Las fechas en formato local**, con `formatDisplayDate`. Las tablas de periodos las muestran hoy
   en ISO pese a que ese trabajo se hizo.

### Corolario: la ficha del empleado

La ficha tiene **dos naturalezas, no cinco áreas hermanas**:

- **La relación laboral**: presencia, contrato, jornada, convenio, centro de trabajo, centro de
  coste. Todo son vigencias y **se solapan entre sí**. Se presenta como un eje temporal con sus
  carriles debajo, en una sola página.
- **La persona**: contactos, identificadores, foto. Sin eje temporal, o con otro.

Partir la primera en pestañas hermanas esconde justo lo que hay que ver, porque **los solapes ocurren
entre las pestañas**. Desaparece el área «Resumen»: el resumen *es* la línea de tiempo.

### Nota (frontend#25): de dónde viene la jerarquía

«Un modo, un tratamiento» produce páginas uniformes cuando la entrada es uniforme: las seis
secciones de la relación laboral comparten modo, luego se ven iguales, y una página donde todo se
ve igual no tiene jerarquía. La regla se mantiene. La jerarquía viene de otros dos sitios:

- **De dentro de la sección**: lo vigente manda sobre lo cerrado. La fila en vigor es la única en
  tinta plena; las cerradas, apagadas. La historia no se pliega: con el estado de hoy arriba ya es
  secundaria por posición, y esconderla añadiría un clic a algo que se abre a menudo. La jerarquía
  se consigue bajando su peso, no ocultándola.
- **De la página**: primero el estado actual, después la historia. La relación lleva un bloque
  «Hoy» entre la línea de vida y las secciones —una línea por vigencia, en el mismo orden que los
  carriles del eje, el índice del raíl y las secciones, con el valor, su código y desde cuándo
  rige—, y las tablas van debajo como historia. Es el borde derecho de la línea de vida, escrito.
  Y es donde aparece lo anómalo: sin centro de coste se lee «sin asignar» en tono de aviso.

Corolarios: una marca de modo que llevan todas las secciones de una página no informa de nada y se
quita (queda la de la que **gobierna**, que solo lleva una); el énfasis de la caja se invierte: la
lleva el bloque «Hoy» y la pierden las secciones de historia, que quedan en título, regla y tabla.
La caja dejó de significar algo en cuanto la tuvieron todas; dársela solo a lo que importa es lo
que crea la jerarquía.

## Consecuencias

- Añadir una vertical temporal deja de ser una decisión de diseño: hereda el tratamiento de su modo.
- Las 19 clases propias se van borrando a medida que se migra cada sección. Nada de big bang: cada
  sección migrada borra la suya.
- Si una sección necesita un tratamiento que su modo no da, o el modo está mal asignado o falta un
  modo. En ningún caso se resuelve con una clase nueva en la feature.

## Fuera de alcance

La escala del eje temporal —cómo se representa una relación de veinte años con un cambio de dos
semanas— sigue sin resolver, y es el riesgo conocido de la implementación.

<!-- END FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md -->


---

# FILE: ADR-28-payroll-calculation-launch-semantics.md
<a name="file-adr-28-payroll-calculation-launch-semantics-md"></a>

<!-- BEGIN FILE: ADR-28-payroll-calculation-launch-semantics.md -->

# ADR — Payroll Calculation Launch Semantics

## Estado
Propuesto

## Contexto

B4RRHH organiza el código por vertical/subdominio y exige APIs públicas basadas en business keys, nunca en IDs técnicos. Además, cuando una operación no encaja como CRUD plano, el proyecto favorece modelarla como una acción de negocio o workflow explícito. fileciteturn4file10 fileciteturn4file12 fileciteturn4file8

En el bounded context `payroll` ya se ha decidido que:

- la raíz funcional es `payroll.payroll`;
- su identidad funcional es:
  - `ruleSystemCode`
  - `employeeTypeCode`
  - `employeeNumber`
  - `payrollPeriodCode`
  - `payrollTypeCode`
  - `presenceNumber`;
- la nómina es un resultado materializado, no editable, regenerable por cálculo;
- las hijas cuelgan con `ON DELETE CASCADE`;
- solo `NOT_VALID` es estado recalcable entre las nóminas ya existentes. fileciteturn4file1 fileciteturn4file6

También se ha fijado que `employee.presence` es un recurso funcional identificado por business key ampliada `ruleSystemCode + employeeTypeCode + employeeNumber + presenceNumber`, y que las acciones de negocio compuestas deben vivir como workflows por encima de los recursos canónicos. fileciteturn4file14 fileciteturn4file16 fileciteturn4file8

Al empezar a hablar de cálculo de nómina aparece una tensión natural:

- una cosa es el **modelo de datos del resultado** (`payroll.payroll`);
- otra cosa distinta es el **lanzamiento del cálculo**.

Si ambas cosas se mezclan demasiado pronto, el diseño queda borroso y se dificulta la evolución futura del motor de reglas.

## Problema

Se necesita definir qué significa técnicamente “lanzar nómina” sin entrar todavía en el motor real de reglas de cálculo.

El sistema debe poder:

- recibir un período y un tipo de nómina;
- resolver una población objetivo;
- expandir esa población a unidades reales de cálculo;
- decidir cuáles son elegibles;
- delegar el cálculo efectivo a otro caso de uso especializado;
- devolver un resumen de ejecución.

Además, el launch no debe recalcular indiscriminadamente:

- una nómina existente en `CALCULATED` no debe tocarse;
- una nómina existente en `EXPLICIT_VALIDATED` no debe tocarse;
- una nómina `DEFINITIVE` jamás debe tocarse;
- una unidad sin nómina previa sí debe calcularse;
- una unidad con nómina previa en `NOT_VALID` sí debe recalcularse.

## Decisión

Se introduce la semántica de **Payroll Calculation Launch** como workflow de aplicación dentro del bounded context `payroll`.

El launch:

- **no es** la raíz funcional del dominio;
- **no es** un CRUD;
- **no es** todavía un recurso persistente canónico tipo `payroll_run`;
- **no implementa** por sí mismo el motor de cálculo;
- **resuelve y orquesta** qué unidades deben intentarse calcular.

### Regla principal

`launch` resuelve la lista de unidades de cálculo elegibles y delega el cálculo efectivo a un caso de uso/endpoint especializado de cálculo.

## Definición funcional

Lanzar nómina significa:

> ejecutar un workflow que, para un `ruleSystemCode`, `payrollPeriodCode`, `payrollTypeCode` y una población objetivo determinada, resuelve las unidades de cálculo candidato, considera elegibles las que no tienen nómina previa o la tienen en `NOT_VALID`, delega el cálculo efectivo a un componente especializado y devuelve un resumen de ejecución.

## Unidad funcional de cálculo

La unidad mínima de cálculo es:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `payrollPeriodCode`
- `payrollTypeCode`
- `presenceNumber`

Justificación:

- `payroll.payroll` ya está anclada a una presencia concreta; fileciteturn4file1
- `presence` tiene identidad pública propia dentro del empleado; fileciteturn4file14 fileciteturn4file16
- dos presencias distintas en el mismo mes representan nóminas independientes.

El launch trabaja con una colección de estas unidades, no con “empleados enteros” de forma opaca.

## Población objetivo vs población elegible

Se distinguen dos conceptos:

### 1. Población objetivo

Es el conjunto de empleados o ámbitos sobre los que el usuario desea lanzar el cálculo.

Ejemplos posibles:

- un empleado;
- una lista explícita de empleados;
- todos los empleados de un `ruleSystemCode`;
- futuros filtros más ricos.

### 2. Población elegible

Es el conjunto de unidades de cálculo que realmente pueden entrar al cálculo efectivo.

Una unidad es elegible si:

- **no existe** `payroll.payroll` para su business key funcional; o
- **existe** y su `status = NOT_VALID`.

Una unidad no es elegible si existe y su estado es:

- `CALCULATED`
- `EXPLICIT_VALIDATED`
- `DEFINITIVE`

## Responsabilidades del launch

El launch debe:

1. recibir el contexto de ejecución;
2. resolver la población objetivo;
3. expandirla a unidades de cálculo candidatas;
4. comprobar existencia y estado de `payroll.payroll`;
5. construir la lista final de unidades elegibles;
6. delegar el cálculo efectivo;
7. consolidar un resumen de ejecución.

El launch no debe:

- generar directamente conceptos de nómina;
- decidir reglas salariales;
- prorratear;
- aplicar retroactividad real;
- convertirse en el motor de cálculo.

## Contexto mínimo de ejecución

El launch debe trabajar al menos con:

- `ruleSystemCode`
- `payrollPeriodCode`
- `payrollTypeCode`
- `calculationEngineCode`
- `calculationEngineVersion`
- `targetSelection`

Los dos campos de engine son obligatorios por coherencia con el modelo raíz ya adoptado para `payroll.payroll`. fileciteturn4file1

## targetSelection

`targetSelection` representa la población objetivo.

No se fija todavía un único shape contractual cerrado, pero el modelo debe permitir al menos:

- cálculo de un empleado concreto;
- cálculo de una lista explícita;
- cálculo masivo por ámbito.

El diseño exacto del payload se cerrará en OpenAPI posterior.

## Delegación al cálculo efectivo

El launch no implementa el cálculo. Delegará en un caso de uso/endpoint especializado, en adelante `calculate`.

Esta separación permite:

- probar el flujo completo antes de tener motor real;
- evolucionar el componente de cálculo sin rediseñar el launch;
- distinguir claramente entre orquestación y cálculo.

## Resultado del launch

El launch debe devolver un resumen explícito de ejecución.

Campos esperables del resumen:

- total de candidatos detectados;
- total de unidades elegibles;
- total de unidades no elegibles por estado;
- total de unidades calculadas con resultado `CALCULATED`;
- total de unidades calculadas con resultado `NOT_VALID`;
- total de errores técnicos;
- detalle opcional por unidad.

No se decide todavía persistir este resumen como recurso canónico.

## Qué se rechaza explícitamente

Se rechaza en esta fase:

- modelar `launch` como CRUD;
- mezclar launch y cálculo efectivo en la misma semántica;
- recalcular cualquier nómina encontrada dentro de la población objetivo;
- introducir ya un `payroll_run` como centro del dominio;
- abrir todavía un repositorio/microservicio separado sólo para el cálculo.

## Relación con el workflow de estados

Este ADR no sustituye al ADR de estados de nómina.

Se complementa con él:

- `NOT_VALID` sigue siendo el estado que autoriza la sustitución de una nómina existente; fileciteturn4file0
- además, una unidad sin nómina previa es también elegible para cálculo.

## API conceptual inicial

A falta de OpenAPI definitivo, se recomienda un endpoint de negocio del estilo:

- `POST /payroll/calculations/launch`

El nombre debe seguir semántica de negocio, no nomenclatura técnica vaga. El proyecto prioriza nombres orientados a negocio y paths por business keys cuando aplica. fileciteturn4file12

## Consecuencias

### Positivas

- separa claramente modelo y proceso;
- permite probar el flujo completo sin motor real;
- protege de recálculos accidentales;
- deja abierta evolución futura del motor;
- encaja con el patrón del proyecto de workflows explícitos. fileciteturn4file8

### Costes

- introduce un caso de uso adicional;
- exige resolver correctamente la expansión de población a presencias;
- obliga a diseñar un resumen de ejecución útil.

## Resumen

En B4RRHH, `launch` no calcula la nómina por sí mismo.

`launch` es el workflow que:

- resuelve la población objetivo;
- expande a unidades reales de cálculo;
- considera elegibles las unidades sin nómina previa o con nómina `NOT_VALID`;
- delega el cálculo efectivo;
- devuelve un resumen explícito del proceso.

<!-- END FILE: ADR-28-payroll-calculation-launch-semantics.md -->

