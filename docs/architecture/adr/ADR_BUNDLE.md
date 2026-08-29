# ADR Bundle

> Fichero generado automÃ¡ticamente. No editar a mano.
> Fecha de generaciÃ³n: 2026-08-29 22:28:28

---

## Ãndice

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

# ADR â€” Arquitectura por verticales y reglas de identidad API en B4RRHH

## Estado
Propuesta adoptada como guÃ­a de refactor y convenciÃ³n base del proyecto.

## Objetivo
Definir de forma inequÃ­voca cÃ³mo debe organizarse el cÃ³digo en B4RRHH, cÃ³mo deben diseÃ±arse las APIs y quÃ© decisiones deben seguirse al crear o refactorizar verticales funcionales, para evitar desviaciones de implementaciÃ³n al trabajar con Copilot o al crecer el proyecto.

---

# 1. Contexto

B4RRHH estÃ¡ evolucionando desde una estructura inicialmente mÃ¡s centrada en capas globales (`application`, `domain`, `infrastructure`) hacia un modelo donde el negocio ya no es un Ãºnico bloque homogÃ©neo, sino un conjunto de verticales funcionales dentro de bounded contexts claros.

En la prÃ¡ctica, ya existen varios subdominios o verticales relevantes:

- `employee.employee`
- `employee.presence`
- `employee.contact`
- `rulesystem.rule_system`
- `rulesystem.rule_entity_type`
- `rulesystem.rule_entity`

A medida que el proyecto crezca, aparecerÃ¡n mÃ¡s verticales y recursos relacionados con el empleado, por ejemplo:

- `employee.address`
- `employee.document`
- `employee.assignment`
- `employee.bank_account`
- `employee.compensation`
- etc.

La estructura actual mezcla dos criterios de organizaciÃ³n:

1. organizaciÃ³n por capas globales
2. organizaciÃ³n por verticales con capas internas

Esa mezcla genera asimetrÃ­as, dificulta la navegaciÃ³n, favorece decisiones inconsistentes en API y aumenta la probabilidad de que Copilot implemente nuevos verticales siguiendo patrones incorrectos.

Este ADR fija el modelo objetivo.

---

# 2. DecisiÃ³n arquitectÃ³nica principal

## 2.1. Regla principal

**En B4RRHH, el cÃ³digo se organiza primero por vertical/subdominio, y dentro de cada vertical se aplica arquitectura hexagonal.**

Eso significa que el eje principal del scaffolding es el negocio, no las capas globales.

## 2.2. Consecuencia prÃ¡ctica

No se debe seguir creciendo con una estructura donde, dentro de un mismo bounded context, convivan simultÃ¡neamente:

- paquetes raÃ­z por capa (`application`, `domain`, `infrastructure`)
- y paquetes raÃ­z por vertical (`presence`, `contact`, etc.)

Ese hÃ­brido sÃ³lo se tolera como estado transitorio durante la migraciÃ³n.

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

**Todas las APIs de B4RRHH deben trabajar con cÃ³digos funcionales de dominio. Nunca con IDs tÃ©cnicos como identidad pÃºblica del recurso.**

Esta es una convenciÃ³n global del proyecto y aplica a todos los bounded contexts y verticales.

## 4.2. QuÃ© significa â€œcÃ³digo funcionalâ€

Son identificadores de negocio estables y significativos, por ejemplo:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `contactTypeCode`
- `ruleEntityTypeCode`
- `ruleEntityCode`

## 4.3. QuÃ© no debe exponerse en la API

No deben utilizarse como identidad pÃºblica en paths ni en la semÃ¡ntica de la API:

- `id`
- `employeeId`
- `contactId`
- `presenceId`
- claves surrogate de base de datos
- UUIDs tÃ©cnicos sin valor de negocio

Los IDs tÃ©cnicos pueden existir y seguir existiendo para:

- persistencia
- joins
- rendimiento
- claves primarias internas
- simplificaciÃ³n de adapters y repositorios

Pero no deben dirigir la forma de la API pÃºblica.

## 4.4. Regla de consistencia

No se permite mezclar en una misma API:

- recurso padre identificado por business key
- recurso hijo identificado por id tÃ©cnico

Tampoco al revÃ©s.

Si un recurso tiene identidad funcional clara, la API debe expresarla.

---

# 5. Regla de modelado de recursos

## 5.1. Los recursos se modelan por su identidad funcional real

Al diseÃ±ar un vertical, primero debe responderse a estas preguntas:

1. Â¿cuÃ¡l es la identidad funcional del recurso?
2. Â¿quÃ© campos forman parte de esa identidad?
3. Â¿quÃ© campos son mutables?
4. Â¿quÃ© campos son meramente persistentes o tÃ©cnicos?
5. Â¿el recurso es historizado o no?
6. Â¿hay unicidad por tipo, perÃ­odo o combinaciÃ³n de cÃ³digos?

## 5.2. No confundir identidad con persistencia

Si un recurso tiene un `id` tÃ©cnico en base de datos, eso no implica que su identidad de negocio sea ese `id`.

Ejemplo:

- `employee.contact` puede tener columna `id`
- pero su identidad funcional puede ser `employee + contactTypeCode`

## 5.3. Los endpoints deben expresar el dominio

Cuando una regla de negocio diga â€œsÃ³lo puede existir uno por tipoâ€, la API debe tender a expresarlo como tal, en lugar de simular una colecciÃ³n anÃ³nima de filas con `id`.

---

# 6. Convenciones especÃ­ficas para el bounded context `employee`

## 6.1. Verticales actuales

Dentro de `employee`, por ahora se consideran verticales explÃ­citos:

- `employee`
- `presence`
- `contact`

A futuro podrÃ¡n aÃ±adirse otros verticales del mismo nivel.

## 6.2. Regla de naming

Se prioriza naming orientado a negocio y no a artefacto tÃ©cnico.

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

salvo que el caso sea estrictamente interno y no forme parte de la API pÃºblica.

## 6.3. `shared` dentro de `employee`

El paquete `employee.shared` sÃ³lo debe contener elementos verdaderamente transversales al bounded context y sin pertenencia clara a un vertical concreto.

No debe convertirse en un cajÃ³n desastre.

Se debe evitar mover a `shared`:

- lÃ³gica de dominio especÃ­fica de un vertical
- validaciones concretas de un recurso
- DTOs
- queries o repositorios de un subdominio concreto

---

# 7. Caso de referencia: `employee.contact`

Este vertical se usarÃ¡ como patrÃ³n canÃ³nico del refactor.

## 7.1. Naturaleza del recurso

`employee.contact` representa medios de contacto actuales de un empleado.

## 7.2. Reglas funcionales acordadas

- no historizado
- un solo contacto por tipo por empleado
- `contact_type_code` validado contra `rulesystem.rule_entity`
- `rule_entity_type_code = EMPLOYEE_CONTACT_TYPE`
- tipos de contacto definidos por `rule_system`
- `contact_value` obligatorio
- validaciÃ³n ligera del valor segÃºn tipo
- borrado fÃ­sico

## 7.3. Identidad funcional del contacto

La identidad funcional del recurso es:

- empleado
- `contactTypeCode`

El contacto no se identifica funcionalmente por un `contactId` tÃ©cnico.

## 7.4. Mutabilidad

- `contactTypeCode`: **inmutable** tras creaciÃ³n
- `contactValue`: **mutable**

## 7.5. Persistencia

Puede existir una tabla como:

- `employee.contact(id, employee_id, contact_type_code, contact_value, created_at, updated_at)`

con restricciÃ³n:

- `unique(employee_id, contact_type_code)`

Eso es correcto siempre que se entienda que:

- `id` es tÃ©cnico
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
Debe contener sÃ³lo:

- `contactValue`

No debe permitir cambiar `contactTypeCode`.

### ContactResponse
Debe evitar exponer IDs tÃ©cnicos como identidad principal del recurso. Si un campo tÃ©cnico se mantiene temporalmente por motivos internos, debe tratarse como excepciÃ³n transitoria, no como convenciÃ³n.

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

`employee.presence` debe tender al mismo modelo arquitectÃ³nico que `employee.contact`, aunque sus reglas funcionales sean distintas.

## 8.1. Naturaleza

- vertical hermano de `employee.contact`
- no un subpaquete accidental dentro de una arquitectura por capas globales

## 8.2. AcciÃ³n recomendada

Una vez estabilizado `contact` como patrÃ³n, `presence` debe revisarse para alinearse con la misma convenciÃ³n:

- vertical en primer nivel del bounded context `employee`
- hexagonal interna
- endpoints pÃºblicos basados en business keys

---

# 9. ValidaciÃ³n contra catÃ¡logos (`rule_entity`)

## 9.1. Principio

Las validaciones de catÃ¡logo deben seguir el metamodelo existente del proyecto.

## 9.2. Regla

Cuando un campo de un vertical representa un cÃ³digo parametrizable, debe validarse contra `rulesystem.rule_entity` usando:

- `ruleSystemCode` correcto
- `ruleEntityTypeCode` correcto
- `code` correcto

## 9.3. Sobre activo y vigencia

Es aceptable reutilizar una validaciÃ³n genÃ©rica comÃºn que ademÃ¡s compruebe:

- activo
- vigencia temporal

si eso forma parte de la infraestructura compartida del metamodelo.

Pero debe entenderse como:

- una polÃ­tica de validaciÃ³n tÃ©cnica compartida
- no necesariamente como una caracterÃ­stica especÃ­fica del vertical en cuestiÃ³n

No debe complicarse el modelo funcional del recurso sÃ³lo por heredar esa validaciÃ³n compartida.

---

# 10. Regla sobre seeds por `rule_system`

## 10.1. DecisiÃ³n actual

Los catÃ¡logos como `EMPLOYEE_CONTACT_TYPE` pueden repetirse por `rule_system`, aunque hoy los valores coincidan entre sistemas.

## 10.2. JustificaciÃ³n

Se evita introducir por ahora una jerarquÃ­a mÃ¡s compleja de catÃ¡logos globales / por paÃ­s / por familia.

## 10.3. Consecuencia

Es vÃ¡lido sembrar valores por cada `rule_system` existente en una migraciÃ³n inicial.

## 10.4. Deuda conocida

Debe definirse en el futuro cÃ³mo escalar esto cuando se creen nuevos `rule_system`:

- seed automÃ¡tico al alta
- proceso operativo
- estrategia de bootstrap
- otro mecanismo

Esta deuda no invalida el diseÃ±o actual, pero debe permanecer visible.

---

# 11. Reglas de diseÃ±o para Copilot

Estas reglas deben incluirse en prompts de implementaciÃ³n o refactor.

## 11.1. Reglas obligatorias

1. Organiza el cÃ³digo primero por vertical/subdominio.
2. Dentro de cada vertical, aplica arquitectura hexagonal.
3. No mezcles paquetes raÃ­z por capa y por vertical dentro de un mismo bounded context.
4. Nunca expongas IDs tÃ©cnicos en APIs pÃºblicas si existe una identidad funcional clara.
5. Usa siempre cÃ³digos funcionales en paths y contratos OpenAPI.
6. MantÃ©n los IDs tÃ©cnicos sÃ³lo en persistencia y wiring interno.
7. Cuando un recurso tenga unicidad funcional por combinaciÃ³n de cÃ³digos, exprÃ©sala en el diseÃ±o del endpoint.
8. No permitas mutar campos que formen parte de la identidad funcional.
9. Actualiza OpenAPI, casos de uso, adapters, tests y documentaciÃ³n de recurso en cada refactor.
10. No introduzcas historizaciÃ³n si el recurso no la requiere.

## 11.2. Antipatrones a evitar

Copilot no debe:

- crear un nuevo paquete raÃ­z suelto al lado de `application`, `domain`, `infrastructure` cuando el bounded context ya tiene verticales
- exponer endpoints por `{id}` cuando el dominio ya tiene business keys claras
- usar DTOs de update que permitan modificar campos identificativos
- tratar una tabla con surrogate key como si esa surrogate key fuera automÃ¡ticamente la identidad del recurso
- diseÃ±ar recursos como listas de filas genÃ©ricas cuando el negocio habla de â€œuno por tipoâ€ o â€œuno por combinaciÃ³n de cÃ³digosâ€

---

# 12. Estrategia de migraciÃ³n recomendada

## 12.1. No hacer big bang global

No se recomienda un megarrefactor de todo el proyecto en una sola iteraciÃ³n.

## 12.2. Orden recomendado

1. fijar este ADR como convenciÃ³n
2. refactorizar `employee.contact`
3. usar `employee.contact` como patrÃ³n canÃ³nico
4. alinear `employee.presence`
5. consolidar `employee.employee` si procede
6. aplicar la convenciÃ³n a nuevos verticales

## 12.3. Regla para nuevos desarrollos

Mientras existan Ã¡reas aÃºn no migradas, cualquier vertical nuevo debe ya nacer con la estructura objetivo.

---

# 13. Checklist de revisiÃ³n para cualquier vertical nuevo

Antes de aceptar una implementaciÃ³n, revisar:

## 13.1. Arquitectura

- Â¿el vertical estÃ¡ organizado como vertical autÃ³nomo con capas internas?
- Â¿se ha evitado mezclar vertical raÃ­z con capas raÃ­z del mismo bounded context?

## 13.2. API

- Â¿los endpoints usan business keys?
- Â¿hay algÃºn `{id}` tÃ©cnico expuesto sin necesidad?
- Â¿la identidad del path expresa el dominio real?

## 13.3. Dominio

- Â¿la identidad funcional estÃ¡ clara?
- Â¿quÃ© campos son inmutables?
- Â¿quÃ© campos son mutables?
- Â¿la unicidad real del negocio estÃ¡ modelada?

## 13.4. Persistencia

- Â¿el id tÃ©cnico queda encapsulado?
- Â¿hay unique constraints alineadas con la identidad funcional?

## 13.5. OpenAPI

- Â¿los schemas reflejan las reglas de mutabilidad?
- Â¿los DTOs de update evitan modificar campos identitarios?

## 13.6. Tests

- Â¿hay tests de caso feliz?
- Â¿hay tests de duplicado/unicidad?
- Â¿hay tests de ownership o pertenencia al recurso padre?
- Â¿hay tests de validaciÃ³n de catÃ¡logo?
- Â¿hay tests de integraciÃ³n con constraints reales de BD?

---

# 14. Prompt base para Copilot â€” creaciÃ³n/refactor de verticales en B4RRHH

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

# 15. Prompt especÃ­fico para refactorizar `employee.contact`

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

- el patrÃ³n objetivo en `employee` es vertical-first
- las APIs del proyecto se diseÃ±an siempre con business keys
- `employee.contact` se toma como primer vertical a refactorizar con esta convenciÃ³n
- cualquier nuevo vertical debe seguir ya estas reglas desde su nacimiento

Este documento debe usarse como referencia base para diseÃ±o humano, revisiÃ³n tÃ©cnica y prompts a Copilot.



<!-- END FILE: ADR-001-vertical-architecture-and-api-identity.md -->


---

# FILE: ADR-002-employee-contact-vertical.md
<a name="file-adr-002-employee-contact-vertical-md"></a>

<!-- BEGIN FILE: ADR-002-employee-contact-vertical.md -->

# ADR-002 â€” Employee Contact Vertical

## Status
Accepted

## Context

The B4RRHH project models employee-related information as a set of vertical resources
inside the `employee` bounded context.

Each resource represents a distinct functional aspect of the employee domain
and follows the architectural rules defined in:

ADR-001 â€” Vertical architecture and API identity rules.

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
| contactTypeCode | âŒ No |
| contactValue | âœ” Yes |

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

# ADR-003 â€” Rule Entity Metamodel Strategy

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
reference vertical in ADR-002. îˆ€fileciteîˆ‚turn8file0îˆ îˆ€fileciteîˆ‚turn8file1îˆ

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
with the project guidance on catalog validation reuse. îˆ€fileciteîˆ‚turn8file0îˆ

---

# 6. API Identity Rule for Metamodel Resources

The public API for metamodel resources must also follow the project-wide rule:
use functional business codes, never technical IDs. ADR-001 makes this mandatory
for the whole project. îˆ€fileciteîˆ‚turn8file0îˆ

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
declares `contact_type_code` as catalog-backed and immutable. îˆ€fileciteîˆ‚turn8file2îˆ

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

- ADR-001 â€” vertical architecture and API identity rules
- ADR-002 â€” employee.contact vertical


<!-- END FILE: ADR-003-rule-entity-metamodel-strategy.md -->


---

# FILE: ADR-004-employee-business-key-strategy.md
<a name="file-adr-004-employee-business-key-strategy-md"></a>

<!-- BEGIN FILE: ADR-004-employee-business-key-strategy.md -->

# ADR-004 â€” Employee Business Key Strategy

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

- ADR-001 â€” vertical architecture and API identity rules
- ADR-002 â€” employee.contact vertical
- ADR-003 â€” rule entity metamodel strategy


<!-- END FILE: ADR-004-employee-business-key-strategy.md -->


---

# FILE: ADR-005-arquitectura_por_verticales_y_reglas_api.md
<a name="file-adr-005-arquitectura-por-verticales-y-reglas-api-md"></a>

<!-- BEGIN FILE: ADR-005-arquitectura_por_verticales_y_reglas_api.md -->

# ADR â€” Arquitectura por verticales y reglas de identidad API en B4RRHH

## Estado
Propuesta adoptada como guÃ­a de refactor y convenciÃ³n base del proyecto.

## Objetivo
Definir de forma inequÃ­voca cÃ³mo debe organizarse el cÃ³digo en B4RRHH, cÃ³mo deben diseÃ±arse las APIs y quÃ© decisiones deben seguirse al crear o refactorizar verticales funcionales, para evitar desviaciones de implementaciÃ³n al trabajar con Copilot o al crecer el proyecto.

---

# 1. Contexto

B4RRHH estÃ¡ evolucionando desde una estructura inicialmente mÃ¡s centrada en capas globales (`application`, `domain`, `infrastructure`) hacia un modelo donde el negocio ya no es un Ãºnico bloque homogÃ©neo, sino un conjunto de verticales funcionales dentro de bounded contexts claros.

En la prÃ¡ctica, ya existen varios subdominios o verticales relevantes:

- `employee.employee`
- `employee.presence`
- `employee.contact`
- `rulesystem.rule_system`
- `rulesystem.rule_entity_type`
- `rulesystem.rule_entity`

A medida que el proyecto crezca, aparecerÃ¡n mÃ¡s verticales y recursos relacionados con el empleado, por ejemplo:

- `employee.address`
- `employee.document`
- `employee.assignment`
- `employee.bank_account`
- `employee.compensation`
- etc.

La estructura actual mezcla dos criterios de organizaciÃ³n:

1. organizaciÃ³n por capas globales
2. organizaciÃ³n por verticales con capas internas

Esa mezcla genera asimetrÃ­as, dificulta la navegaciÃ³n, favorece decisiones inconsistentes en API y aumenta la probabilidad de que Copilot implemente nuevos verticales siguiendo patrones incorrectos.

Este ADR fija el modelo objetivo.

---

# 2. DecisiÃ³n arquitectÃ³nica principal

## 2.1. Regla principal

**En B4RRHH, el cÃ³digo se organiza primero por vertical/subdominio, y dentro de cada vertical se aplica arquitectura hexagonal.**

Eso significa que el eje principal del scaffolding es el negocio, no las capas globales.

## 2.2. Consecuencia prÃ¡ctica

No se debe seguir creciendo con una estructura donde, dentro de un mismo bounded context, convivan simultÃ¡neamente:

- paquetes raÃ­z por capa (`application`, `domain`, `infrastructure`)
- y paquetes raÃ­z por vertical (`presence`, `contact`, etc.)

Ese hÃ­brido sÃ³lo se tolera como estado transitorio durante la migraciÃ³n.

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

**Todas las APIs de B4RRHH deben trabajar con cÃ³digos funcionales de dominio. Nunca con IDs tÃ©cnicos como identidad pÃºblica del recurso.**

Esta es una convenciÃ³n global del proyecto y aplica a todos los bounded contexts y verticales.

## 4.2. QuÃ© significa â€œcÃ³digo funcionalâ€

Son identificadores de negocio estables y significativos, por ejemplo:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `contactTypeCode`
- `ruleEntityTypeCode`
- `ruleEntityCode`

## 4.3. QuÃ© no debe exponerse en la API

No deben utilizarse como identidad pÃºblica en paths ni en la semÃ¡ntica de la API:

- `id`
- `employeeId`
- `contactId`
- `presenceId`
- claves surrogate de base de datos
- UUIDs tÃ©cnicos sin valor de negocio

Los IDs tÃ©cnicos pueden existir y seguir existiendo para:

- persistencia
- joins
- rendimiento
- claves primarias internas
- simplificaciÃ³n de adapters y repositorios

Pero no deben dirigir la forma de la API pÃºblica.

## 4.4. Regla de consistencia

No se permite mezclar en una misma API:

- recurso padre identificado por business key
- recurso hijo identificado por id tÃ©cnico

Tampoco al revÃ©s.

Si un recurso tiene identidad funcional clara, la API debe expresarla.

---

# 5. Regla de modelado de recursos

## 5.1. Los recursos se modelan por su identidad funcional real

Al diseÃ±ar un vertical, primero debe responderse a estas preguntas:

1. Â¿cuÃ¡l es la identidad funcional del recurso?
2. Â¿quÃ© campos forman parte de esa identidad?
3. Â¿quÃ© campos son mutables?
4. Â¿quÃ© campos son meramente persistentes o tÃ©cnicos?
5. Â¿el recurso es historizado o no?
6. Â¿hay unicidad por tipo, perÃ­odo o combinaciÃ³n de cÃ³digos?

## 5.2. No confundir identidad con persistencia

Si un recurso tiene un `id` tÃ©cnico en base de datos, eso no implica que su identidad de negocio sea ese `id`.

Ejemplo:

- `employee.contact` puede tener columna `id`
- pero su identidad funcional puede ser `employee + contactTypeCode`

## 5.3. Los endpoints deben expresar el dominio

Cuando una regla de negocio diga â€œsÃ³lo puede existir uno por tipoâ€, la API debe tender a expresarlo como tal, en lugar de simular una colecciÃ³n anÃ³nima de filas con `id`.

---

# 6. Convenciones especÃ­ficas para el bounded context `employee`

## 6.1. Verticales actuales

Dentro de `employee`, por ahora se consideran verticales explÃ­citos:

- `employee`
- `presence`
- `contact`

A futuro podrÃ¡n aÃ±adirse otros verticales del mismo nivel.

## 6.2. Regla de naming

Se prioriza naming orientado a negocio y no a artefacto tÃ©cnico.

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

salvo que el caso sea estrictamente interno y no forme parte de la API pÃºblica.

## 6.3. `shared` dentro de `employee`

El paquete `employee.shared` sÃ³lo debe contener elementos verdaderamente transversales al bounded context y sin pertenencia clara a un vertical concreto.

No debe convertirse en un cajÃ³n desastre.

Se debe evitar mover a `shared`:

- lÃ³gica de dominio especÃ­fica de un vertical
- validaciones concretas de un recurso
- DTOs
- queries o repositorios de un subdominio concreto

---

# 7. Caso de referencia: `employee.contact`

Este vertical se usarÃ¡ como patrÃ³n canÃ³nico del refactor.

## 7.1. Naturaleza del recurso

`employee.contact` representa medios de contacto actuales de un empleado.

## 7.2. Reglas funcionales acordadas

- no historizado
- un solo contacto por tipo por empleado
- `contact_type_code` validado contra `rulesystem.rule_entity`
- `rule_entity_type_code = EMPLOYEE_CONTACT_TYPE`
- tipos de contacto definidos por `rule_system`
- `contact_value` obligatorio
- validaciÃ³n ligera del valor segÃºn tipo
- borrado fÃ­sico

## 7.3. Identidad funcional del contacto

La identidad funcional del recurso es:

- empleado
- `contactTypeCode`

El contacto no se identifica funcionalmente por un `contactId` tÃ©cnico.

## 7.4. Mutabilidad

- `contactTypeCode`: **inmutable** tras creaciÃ³n
- `contactValue`: **mutable**

## 7.5. Persistencia

Puede existir una tabla como:

- `employee.contact(id, employee_id, contact_type_code, contact_value, created_at, updated_at)`

con restricciÃ³n:

- `unique(employee_id, contact_type_code)`

Eso es correcto siempre que se entienda que:

- `id` es tÃ©cnico
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
Debe contener sÃ³lo:

- `contactValue`

No debe permitir cambiar `contactTypeCode`.

### ContactResponse
Debe evitar exponer IDs tÃ©cnicos como identidad principal del recurso. Si un campo tÃ©cnico se mantiene temporalmente por motivos internos, debe tratarse como excepciÃ³n transitoria, no como convenciÃ³n.

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

`employee.presence` debe tender al mismo modelo arquitectÃ³nico que `employee.contact`, aunque sus reglas funcionales sean distintas.

## 8.1. Naturaleza

- vertical hermano de `employee.contact`
- no un subpaquete accidental dentro de una arquitectura por capas globales

## 8.2. AcciÃ³n recomendada

Una vez estabilizado `contact` como patrÃ³n, `presence` debe revisarse para alinearse con la misma convenciÃ³n:

- vertical en primer nivel del bounded context `employee`
- hexagonal interna
- endpoints pÃºblicos basados en business keys

---

# 9. ValidaciÃ³n contra catÃ¡logos (`rule_entity`)

## 9.1. Principio

Las validaciones de catÃ¡logo deben seguir el metamodelo existente del proyecto.

## 9.2. Regla

Cuando un campo de un vertical representa un cÃ³digo parametrizable, debe validarse contra `rulesystem.rule_entity` usando:

- `ruleSystemCode` correcto
- `ruleEntityTypeCode` correcto
- `code` correcto

## 9.3. Sobre activo y vigencia

Es aceptable reutilizar una validaciÃ³n genÃ©rica comÃºn que ademÃ¡s compruebe:

- activo
- vigencia temporal

si eso forma parte de la infraestructura compartida del metamodelo.

Pero debe entenderse como:

- una polÃ­tica de validaciÃ³n tÃ©cnica compartida
- no necesariamente como una caracterÃ­stica especÃ­fica del vertical en cuestiÃ³n

No debe complicarse el modelo funcional del recurso sÃ³lo por heredar esa validaciÃ³n compartida.

---

# 10. Regla sobre seeds por `rule_system`

## 10.1. DecisiÃ³n actual

Los catÃ¡logos como `EMPLOYEE_CONTACT_TYPE` pueden repetirse por `rule_system`, aunque hoy los valores coincidan entre sistemas.

## 10.2. JustificaciÃ³n

Se evita introducir por ahora una jerarquÃ­a mÃ¡s compleja de catÃ¡logos globales / por paÃ­s / por familia.

## 10.3. Consecuencia

Es vÃ¡lido sembrar valores por cada `rule_system` existente en una migraciÃ³n inicial.

## 10.4. Deuda conocida

Debe definirse en el futuro cÃ³mo escalar esto cuando se creen nuevos `rule_system`:

- seed automÃ¡tico al alta
- proceso operativo
- estrategia de bootstrap
- otro mecanismo

Esta deuda no invalida el diseÃ±o actual, pero debe permanecer visible.

---

# 11. Reglas de diseÃ±o para Copilot

Estas reglas deben incluirse en prompts de implementaciÃ³n o refactor.

## 11.1. Reglas obligatorias

1. Organiza el cÃ³digo primero por vertical/subdominio.
2. Dentro de cada vertical, aplica arquitectura hexagonal.
3. No mezcles paquetes raÃ­z por capa y por vertical dentro de un mismo bounded context.
4. Nunca expongas IDs tÃ©cnicos en APIs pÃºblicas si existe una identidad funcional clara.
5. Usa siempre cÃ³digos funcionales en paths y contratos OpenAPI.
6. MantÃ©n los IDs tÃ©cnicos sÃ³lo en persistencia y wiring interno.
7. Cuando un recurso tenga unicidad funcional por combinaciÃ³n de cÃ³digos, exprÃ©sala en el diseÃ±o del endpoint.
8. No permitas mutar campos que formen parte de la identidad funcional.
9. Actualiza OpenAPI, casos de uso, adapters, tests y documentaciÃ³n de recurso en cada refactor.
10. No introduzcas historizaciÃ³n si el recurso no la requiere.

## 11.2. Antipatrones a evitar

Copilot no debe:

- crear un nuevo paquete raÃ­z suelto al lado de `application`, `domain`, `infrastructure` cuando el bounded context ya tiene verticales
- exponer endpoints por `{id}` cuando el dominio ya tiene business keys claras
- usar DTOs de update que permitan modificar campos identificativos
- tratar una tabla con surrogate key como si esa surrogate key fuera automÃ¡ticamente la identidad del recurso
- diseÃ±ar recursos como listas de filas genÃ©ricas cuando el negocio habla de â€œuno por tipoâ€ o â€œuno por combinaciÃ³n de cÃ³digosâ€

---

# 12. Estrategia de migraciÃ³n recomendada

## 12.1. No hacer big bang global

No se recomienda un megarrefactor de todo el proyecto en una sola iteraciÃ³n.

## 12.2. Orden recomendado

1. fijar este ADR como convenciÃ³n
2. refactorizar `employee.contact`
3. usar `employee.contact` como patrÃ³n canÃ³nico
4. alinear `employee.presence`
5. consolidar `employee.employee` si procede
6. aplicar la convenciÃ³n a nuevos verticales

## 12.3. Regla para nuevos desarrollos

Mientras existan Ã¡reas aÃºn no migradas, cualquier vertical nuevo debe ya nacer con la estructura objetivo.

---

# 13. Checklist de revisiÃ³n para cualquier vertical nuevo

Antes de aceptar una implementaciÃ³n, revisar:

## 13.1. Arquitectura

- Â¿el vertical estÃ¡ organizado como vertical autÃ³nomo con capas internas?
- Â¿se ha evitado mezclar vertical raÃ­z con capas raÃ­z del mismo bounded context?

## 13.2. API

- Â¿los endpoints usan business keys?
- Â¿hay algÃºn `{id}` tÃ©cnico expuesto sin necesidad?
- Â¿la identidad del path expresa el dominio real?

## 13.3. Dominio

- Â¿la identidad funcional estÃ¡ clara?
- Â¿quÃ© campos son inmutables?
- Â¿quÃ© campos son mutables?
- Â¿la unicidad real del negocio estÃ¡ modelada?

## 13.4. Persistencia

- Â¿el id tÃ©cnico queda encapsulado?
- Â¿hay unique constraints alineadas con la identidad funcional?

## 13.5. OpenAPI

- Â¿los schemas reflejan las reglas de mutabilidad?
- Â¿los DTOs de update evitan modificar campos identitarios?

## 13.6. Tests

- Â¿hay tests de caso feliz?
- Â¿hay tests de duplicado/unicidad?
- Â¿hay tests de ownership o pertenencia al recurso padre?
- Â¿hay tests de validaciÃ³n de catÃ¡logo?
- Â¿hay tests de integraciÃ³n con constraints reales de BD?

---

# 14. Prompt base para Copilot â€” creaciÃ³n/refactor de verticales en B4RRHH

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

# 15. Prompt especÃ­fico para refactorizar `employee.contact`

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

- el patrÃ³n objetivo en `employee` es vertical-first
- las APIs del proyecto se diseÃ±an siempre con business keys
- `employee.contact` se toma como primer vertical a refactorizar con esta convenciÃ³n
- cualquier nuevo vertical debe seguir ya estas reglas desde su nacimiento

Este documento debe usarse como referencia base para diseÃ±o humano, revisiÃ³n tÃ©cnica y prompts a Copilot.



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

    rule_system â‰  country

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

## Nonâ€‘Goals

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
        â”œâ”€â”€ COMMON
        â”œâ”€â”€ LABORAL
        â””â”€â”€ PAYROLL

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

# ADR â€” Employee Lifecycle Workflows (Hire / Terminate / Rehire)

## Status

Proposed

## Context

El sistema B4RRHH ha sido diseÃ±ado siguiendo una arquitectura basada en verticales funcionales independientes (contacts, addresses, identifiers, presence, etc.), todas ellas relacionadas con el empleado mediante business key.

Este enfoque ha permitido:

* SeparaciÃ³n clara de responsabilidades
* EvoluciÃ³n independiente de cada vertical
* APIs limpias y desacopladas
* IntegraciÃ³n progresiva en frontend mediante composiciÃ³n de bloques

Sin embargo, este modelo presenta una limitaciÃ³n desde el punto de vista funcional:

> El ciclo de vida del empleado no se corresponde con la creaciÃ³n y mantenimiento manual de mÃºltiples verticales independientes.

En la prÃ¡ctica, acciones como contratar, despedir o recontratar a un empleado implican:

* creaciÃ³n o modificaciÃ³n coordinada de mÃºltiples verticales
* reglas de coherencia temporal (fechas efectivas)
* validaciones transversales
* significado funcional Ãºnico (no tÃ©cnico)

Actualmente, el sistema permitirÃ­a modelar estas acciones como una secuencia de operaciones independientes (crear employee, luego presence, luego assignment, etc.), lo cual:

* no refleja el dominio real
* degrada la experiencia de usuario
* aumenta el riesgo de inconsistencias

Por tanto, se identifica la necesidad de introducir una nueva capa de **operaciones de negocio compuestas**, que representen el ciclo de vida del empleado.

---

## Decision

Se introduce el concepto de **Employee Lifecycle Workflows**, como una capa funcional por encima de las verticales existentes.

Estos workflows representan acciones de negocio completas que afectan a mÃºltiples partes del modelo de empleado de forma coordinada.

### Workflows iniciales definidos

* **Hire Employee**
* **Terminate Employee**
* **Rehire Employee**

Estos workflows:

* no sustituyen a las verticales existentes
* no alteran el modelo de datos base
* actÃºan como orquestadores de operaciones sobre mÃºltiples verticales

---

## Design Principles

### 1. SeparaciÃ³n entre recursos y acciones

Se distingue claramente entre:

* **Recursos de dominio**
  (employee, presence, contact, identifier, etc.)

* **Acciones de negocio**
  (hire, terminate, rehire)

Los workflows no son recursos persistentes, sino casos de uso.

---

### 2. OrquestaciÃ³n coherente

Cada workflow:

* ejecuta mÃºltiples operaciones sobre distintas verticales
* garantiza consistencia funcional (fechas, estados, relaciones)
* evita que el usuario tenga que ensamblar manualmente el estado del empleado

---

### 3. Persistencia desacoplada

Las verticales existentes:

* mantienen su diseÃ±o actual
* siguen siendo accesibles de forma independiente
* siguen siendo la base del modelo

Los workflows no introducen nuevas tablas â€œmonolÃ­ticasâ€.

---

### 4. UX orientada a intenciÃ³n

El sistema debe permitir que el usuario piense en tÃ©rminos de:

* â€œcontratar empleadoâ€
* â€œdespedir empleadoâ€

y no en:

* â€œcrear presenceâ€
* â€œcrear assignmentâ€
* â€œactualizar estadoâ€

---

### 5. Transparencia

Los workflows deben:

* ser explÃ­citos en quÃ© operaciones realizan
* evitar efectos ocultos
* permitir trazabilidad futura

---

## Workflow Definitions

### 1. Hire Employee

#### DescripciÃ³n

Inicia la vida laboral de un empleado en el sistema.

#### Operaciones implicadas

* creaciÃ³n de employee core
* creaciÃ³n de primera presence
* creaciÃ³n de asignaciÃ³n organizativa inicial (work center, cost center, etc.)
* inicializaciÃ³n de estado laboral

#### Datos mÃ­nimos esperados (orientativo)

* employeeNumber
* employeeTypeCode
* ruleSystemCode
* nombre y apellidos
* fecha de entrada
* entryReasonCode
* companyCode
* workCenter (u otra asignaciÃ³n organizativa mÃ­nima)

#### Reglas clave

* todas las entidades iniciales deben compartir coherencia temporal
* debe existir una presence activa tras el proceso
* el empleado queda en estado funcional vÃ¡lido

---

### 2. Terminate Employee

#### DescripciÃ³n

Finaliza la relaciÃ³n laboral de un empleado.

#### Operaciones implicadas

* cierre de presence activa
* registro de fecha de salida
* registro de exitReasonCode
* cierre o ajuste de asignaciones vigentes

#### Reglas clave

* no puede existir mÃ¡s de una presence activa
* tras la terminaciÃ³n no debe quedar ninguna presence abierta
* se preserva el histÃ³rico completo

---

### 3. Rehire Employee

#### DescripciÃ³n

Reincorpora a un empleado previamente terminado.

#### Operaciones implicadas

* creaciÃ³n de nueva presence
* creaciÃ³n de nuevas asignaciones iniciales
* reutilizaciÃ³n del employee existente

#### Reglas clave

* no se crea un nuevo employee
* se mantiene histÃ³rico de presencias anteriores
* la nueva presence debe ser coherente con las anteriores

---

## API Considerations (Future)

Se prevÃ© la introducciÃ³n de endpoints especÃ­ficos para workflows, por ejemplo:

* `POST /employees/hire`
* `POST /employees/{employeeId}/terminate`
* `POST /employees/{employeeId}/rehire`

Estos endpoints:

* encapsularÃ¡n la lÃ³gica de orquestaciÃ³n
* recibirÃ¡n payloads orientados a negocio
* no expondrÃ¡n directamente detalles internos de cada vertical

---

## UI Considerations (Future)

Los workflows se expondrÃ¡n como acciones de primer nivel en la navegaciÃ³n:

* Employee

  * Ficha (visualizaciÃ³n y mantenimiento)
  * Contratar (Hire)
  * Despedir (Terminate)
  * Recontratar (Rehire)

Cada workflow se implementarÃ¡ como:

* pantalla dedicada o flujo guiado (no modal simple)
* formulario estructurado por bloques
* validaciÃ³n previa antes de ejecuciÃ³n

---

## Consequences

### Positivas

* Mejor alineaciÃ³n con el dominio real
* Mejora significativa de UX
* ReducciÃ³n de inconsistencias funcionales
* ReutilizaciÃ³n del modelo existente
* Escalabilidad para nuevas acciones de negocio

### Negativas / Riesgos

* Incremento de complejidad en capa de aplicaciÃ³n
* Necesidad de definir reglas de negocio claras
* Posible duplicidad si no se gobierna bien la relaciÃ³n entre workflows y verticales

---

## Alternatives Considered

### 1. Mantener solo operaciones CRUD por vertical

Descartado:

* no representa el dominio real
* UX pobre
* alto riesgo de inconsistencias

### 2. Convertir employee en un agregado monolÃ­tico

Descartado:

* rompe la arquitectura modular actual
* reduce flexibilidad
* dificulta evoluciÃ³n

---

## Open Questions

* DefiniciÃ³n exacta del mÃ­nimo necesario para cada workflow
* GestiÃ³n de validaciones complejas entre verticales
* Estrategia de versionado de workflows
* AuditorÃ­a y trazabilidad de ejecuciones

---

## Summary

El empleado no debe modelarse Ãºnicamente como un conjunto de datos, sino como un objeto con ciclo de vida.

La introducciÃ³n de **Employee Lifecycle Workflows** permite:

* mantener la arquitectura modular existente
* aÃ±adir una capa funcional coherente con el negocio
* mejorar significativamente la experiencia de usuario

Este ADR establece la base conceptual para futuras implementaciones de Hire, Terminate y Rehire en el sistema B4RRHH.


<!-- END FILE: ADR-007-employee-lifecycle-workflows.md -->


---

# FILE: ADR-008-strong-timeline-replace-pattern.md
<a name="file-adr-008-strong-timeline-replace-pattern-md"></a>

<!-- BEGIN FILE: ADR-008-strong-timeline-replace-pattern.md -->

# B4RRHH â€“ Strong Timeline Replace Pattern

## 1. Context

En mÃºltiples verticales del dominio employee (por ejemplo:

* labor_classification
* contract)

existe una operaciÃ³n comÃºn:

**replaceFromDate(effectiveDate)**

Esta operaciÃ³n:

* sustituye el valor activo a partir de una fecha
* respeta la continuidad temporal
* puede implicar divisiÃ³n de periodos existentes

Estas verticales pertenecen al tipo:

**STRONG_TIMELINE**

---

## 2. Problema

La lÃ³gica de replaceFromDate incluye una parte repetida en varias verticales:

* detecciÃ³n de tramo que cubre la fecha
* distinciÃ³n entre:

  * exact match (startDate == effectiveDate)
  * split (fecha dentro del tramo)
  * no covering period
* cÃ¡lculo de nuevos lÃ­mites temporales

Antes, esta lÃ³gica estaba duplicada en cada vertical.

---

## 3. DecisiÃ³n

Se introduce un helper tÃ©cnico reutilizable:

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

Describe el resultado del anÃ¡lisis temporal:

* tipo de operaciÃ³n
* tramo afectado
* posibles nuevos rangos temporales

---

## 5. Responsabilidades

### Planner (helper tÃ©cnico)

Responsable de:

* analizar geometrÃ­a temporal
* decidir tipo de operaciÃ³n
* calcular fechas derivadas

NO es responsable de:

* validaciones de negocio
* catÃ¡logo
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

## 6. PatrÃ³n de uso

Para cualquier vertical STRONG_TIMELINE:

1. Cargar histÃ³rico ordenado
2. Convertir a DateRange
3. Invocar planner:
   â†’ StrongTimelineReplacePlan
4. Aplicar lÃ³gica de dominio segÃºn el plan:

   * EXACT_START â†’ update
   * SPLIT â†’ close + create
   * NO_COVERING â†’ decidir comportamiento
5. Construir projected history
6. Validar timeline
7. Persistir

---

## 7. CuÃ¡ndo usar este patrÃ³n

Aplicar StrongTimelineReplacePlanner SOLO cuando:

* la vertical es STRONG_TIMELINE
* existe operaciÃ³n replaceFromDate
* hay garantÃ­a de:

  * no solape
  * un Ãºnico activo por fecha

Ejemplos:

* contract âœ…
* labor_classification âœ…

No aplicar directamente a:

* cost_center (DISTRIBUTED_TIMELINE)
* verticales sin cobertura completa

---

## 8. Beneficios

* elimina duplicaciÃ³n de lÃ³gica temporal crÃ­tica
* mejora legibilidad de servicios
* introduce lenguaje comÃºn
* reduce errores en operaciones de split

---

## 9. Regla de evoluciÃ³n

Este helper:

* puede evolucionar si aparece en â‰¥ 3 verticales
* NO debe convertirse en:

  * engine genÃ©rico
  * framework configurable
  * capa de negocio

---

## 10. DecisiÃ³n futura

Si nuevas verticales STRONG_TIMELINE aparecen:

â†’ deben reutilizar este planner

Si aparecen variaciones significativas:

â†’ evaluar extensiÃ³n del planner, no duplicaciÃ³n

---

## 11. Estado

PatrÃ³n activo y recomendado.


<!-- END FILE: ADR-008-strong-timeline-replace-pattern.md -->


---

# FILE: ADR-009-journey.md
<a name="file-adr-009-journey-md"></a>

<!-- BEGIN FILE: ADR-009-journey.md -->

Design Principles
1. Journey debe contar la historia del empleado

Un journey debe responder a:

quÃ© pasÃ³

cuÃ¡ndo pasÃ³

cÃ³mo se interpreta funcionalmente

por quÃ© ese evento importa

No debe limitarse a agrupar verticales en paralelo.

2. El frontend no debe inferir semÃ¡ntica de negocio compleja

El frontend no debe deducir por sÃ­ solo si algo es:

un alta

una recontrataciÃ³n

un cambio de contrato

un cambio de clasificaciÃ³n

Esa interpretaciÃ³n debe resolverse en backend, dentro del read model.

3. Journey es una proyecciÃ³n read-only de UI

Journey V2 no sustituye:

verticales canÃ³nicas

endpoints de escritura

recursos de dominio independientes

Es una proyecciÃ³n agregada para experiencia de usuario.

4. Tracks siguen teniendo valor

La vista actual por tracks sigue siendo Ãºtil para:

inspecciÃ³n tÃ©cnica

validaciÃ³n funcional

representaciÃ³n por vertical

Por tanto, no debe considerarse un fracaso ni descartarse.

Current Model Reclassification

El modelo actualmente expuesto bajo journey debe reinterpretarse como:

Employee Tracks

Employee History Tracks

o naming equivalente

No se recomienda seguir llamÃ¡ndolo â€œjourneyâ€ en su estado actual.

Journey V2 â€” Target Model
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
      "subtitle": "ES01 Â· perÃ­odo #1",
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

Cada evento debe incluir, como mÃ­nimo:

eventDate

eventType

trackCode

title

subtitle

status

isCurrent

details

Campo eventDate

Fecha efectiva del evento en la lÃ­nea temporal.

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

Contexto breve y Ãºtil para lectura rÃ¡pida.

Campo status

Estado visual del evento:

completed

current

future

u otro conjunto acotado

Campo isCurrent

Flag explÃ­cito para eventos actualmente vigentes o activos.

Campo details

InformaciÃ³n adicional libre y limitada, para tooltips, badges o ampliaciÃ³n contextual.

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

una nueva presence posterior a una terminaciÃ³n puede generar REHIRE

el cierre de una presence con motivo adecuado puede generar TERMINATION

un cambio de contrato genera CONTRACT_CHANGE

una nueva clasificaciÃ³n laboral genera LABOR_CLASSIFICATION_CHANGE

El frontend no debe deducir esta semÃ¡ntica a partir de details.

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

Permitir una representaciÃ³n clara de:

la historia laboral del empleado

eventos relevantes

cambios significativos

estado actual dentro de la secuencia

RecomendaciÃ³n visual

Se recomienda una timeline orientada a eventos cronolÃ³gicos, preferentemente:

vertical

o hÃ­brida compacta

No se considera Ã³ptimo reutilizar directamente la representaciÃ³n actual por tracks para este objetivo.

Tracks en UI

La vista por tracks puede seguir existiendo como:

vista tÃ©cnica

vista avanzada

o modo alternativo de inspecciÃ³n

Pero no como definiciÃ³n principal de â€œjourneyâ€.

Migration Strategy
Phase 1 â€” Reclassify current model

aceptar que el modelo actual es tracks

ajustar naming interno/documentaciÃ³n si procede

mantener compatibilidad

Phase 2 â€” Design Journey V2 contract

definir EmployeeJourneyV2Response

definir catÃ¡logo inicial de eventType

acordar reglas de agregaciÃ³n en backend

Phase 3 â€” Implement backend projection

construir la proyecciÃ³n events[]

reutilizando verticales existentes

sin alterar recursos canÃ³nicos

Phase 4 â€” Adapt frontend

nuevo client / mapper / store / UI para journey V2

timeline realmente cronolÃ³gica

dejar la vista actual por tracks como opcional o tÃ©cnica

Consequences
Positivas

naming mÃ¡s honesto

mejor alineaciÃ³n semÃ¡ntica

journey verdaderamente Ãºtil para UI

menos lÃ³gica interpretativa en frontend

mejor encaje con lifecycle workflows

Negativas / Costes

hay que diseÃ±ar un segundo read model

el backend debe aÃ±adir reglas de interpretaciÃ³n

puede convivir temporalmente mÃ¡s de una proyecciÃ³n agregada

habrÃ¡ que ajustar frontend para el nuevo shape

Alternatives Considered
1. Mantener el shape actual y mejorarlo solo en frontend

Descartado como soluciÃ³n final:

obliga al frontend a inferir demasiada semÃ¡ntica

sigue sin representar bien un journey

2. Renombrar simplemente el endpoint actual y no hacer V2

Insuficiente:

arregla naming

no resuelve la necesidad de una timeline funcional de eventos

3. Convertir journey en agregado completo de dominio

Descartado:

journey debe seguir siendo una proyecciÃ³n read-only

no debe sustituir a verticales canÃ³nicas

Summary

El modelo actual no representa un journey, sino una vista histÃ³rica por tracks.

Se decide:

reclasificar conceptualmente el modelo actual como tracks

diseÃ±ar Journey V2 como una proyecciÃ³n cronolÃ³gica de eventos

mantener separadas:

la vista tÃ©cnica por tracks

la vista funcional de journey

Esto permitirÃ¡ construir una timeline realmente Ãºtil para frontend, alineada con el ciclo de vida del empleado y con la semÃ¡ntica de negocio del dominio.

<!-- END FILE: ADR-009-journey.md -->


---

# FILE: ADR-010-employee-frontend-editing.md
<a name="file-adr-010-employee-frontend-editing-md"></a>

<!-- BEGIN FILE: ADR-010-employee-frontend-editing.md -->

# ADR â€” Employee Frontend Editing Pattern by Vertical Maintenance Mode

## Status
PROPOSED

---

## Contexto

El frontend de B4RRHH ya permite visualizar la ficha de empleado basada en verticales independientes:

- Arquitectura: client â†’ mapper â†’ gateway â†’ store â†’ UI
- Backend basado en business keys (no IDs tÃ©cnicos)
- Verticales con distinta naturaleza:
  - Datos simples (contactos, identificadores)
  - Datos temporales (direcciones)
  - Datos complejos (presence, contracts)

Actualmente la UI es read-only y se requiere introducir ediciÃ³n.

---

## Problema

DiseÃ±ar un patrÃ³n de ediciÃ³n que:
- Sea consistente
- Respete el dominio
- Evite sobreingenierÃ­a
- Escale a futuro

AdemÃ¡s, en verticales temporales, debe distinguirse entre:
- cambio funcional real
- correcciÃ³n administrativa de una ocurrencia mal capturada

Sin esa distinciÃ³n, el frontend puede acabar representando errores de captura como si fueran eventos reales de negocio.

---

## DecisiÃ³n

Se adopta el patrÃ³n:

## Editable Resource Block by Maintenance Mode

---

## 1. Unidad de interacciÃ³n: BLOQUE

Cada vertical se representa como un bloque autÃ³nomo.

Reglas:
- Independiente
- Con su propio estado
- Sin ediciÃ³n global de ficha

---

## 2. Maintenance Mode

Cada vertical define su modo de mantenimiento:

### SLOT
Para verticales tipo lista simple o â€œslot por tipoâ€.

Uso tÃ­pico:
- contactos
- identificadores

CaracterÃ­sticas:
- Alta
- EdiciÃ³n
- EliminaciÃ³n
- OperaciÃ³n normalmente centrada en una fila

---

### TEMPORAL_APPEND_CLOSE
Para verticales historizados cuyo cambio funcional normal se expresa mediante:
- alta de una nueva ocurrencia
- cierre de la ocurrencia vigente

Uso tÃ­pico:
- direcciones

CaracterÃ­sticas:
- No modela un update directo como cambio normal
- El histÃ³rico se preserva por append + close
- La semÃ¡ntica principal es temporal, no CRUD clÃ¡sico

#### Nota importante
TEMPORAL_APPEND_CLOSE **no implica** que toda modificaciÃ³n de una ocurrencia deba resolverse siempre con â€œcerrar y crear otraâ€.

Debe distinguirse entre:

##### a) Cambio funcional real
Ejemplos:
- el empleado se muda
- cambia la direcciÃ³n efectiva desde una fecha
- hay un reemplazo de la ocurrencia vigente

En estos casos:
- add / append
- close
- eventualmente replace

##### b) CorrecciÃ³n administrativa
Ejemplos:
- calle mal escrita
- portal errÃ³neo
- paÃ­s o cÃ³digo postal mal informado
- error de captura reciente

En estos casos, cerrar y recrear puede:
- ensuciar el histÃ³rico
- generar falsos eventos de negocio
- introducir ruido funcional o de auditorÃ­a

Por tanto:

- TEMPORAL_APPEND_CLOSE **por defecto no incluye correcciÃ³n**
- pero **puede ampliarse** con una operaciÃ³n explÃ­cita de `correct` si el dominio y el backend la soportan

#### Regla de frontend
El frontend **no inventarÃ¡** semÃ¡nticas de correcciÃ³n si el backend no expone una operaciÃ³n compatible.

---

### WORKFLOW
Para verticales cuya modificaciÃ³n requiere acciones de negocio, no CRUD directo.

Uso tÃ­pico:
- presence
- contracts
- labor classification

CaracterÃ­sticas:
- No se presentan como ediciÃ³n genÃ©rica
- Se accionan mediante flujos explÃ­citos
- Ejemplos futuros:
  - hire
  - termination
  - rehire
  - replace from date

---

### READONLY
Para verticales puramente informativos o todavÃ­a no abiertos a mantenimiento.

---

## 3. Modelo de interacciÃ³n

Cada bloque tiene:

- displayMode: read | edit | create | busy | error
- maintenanceMode
- supportedActions

Ejemplo:
- contact â†’ maintenanceMode = SLOT
- identifier â†’ maintenanceMode = SLOT
- address â†’ maintenanceMode = TEMPORAL_APPEND_CLOSE

---

## 4. UX

Reglas generales:
- EdiciÃ³n por bloque
- Una sola sesiÃ³n activa por bloque
- Preferiblemente una Ãºnica sesiÃ³n de ediciÃ³n en toda la ficha en V1
- Operaciones por fila cuando aplique
- Feedback simple, discreto y local al bloque

### Principio de honestidad UX
La UI debe mostrar la acciÃ³n real soportada por el dominio:
- Editar
- AÃ±adir
- Eliminar
- Cerrar
- Corregir
- Lanzar workflow

No debe usarse â€œEditarâ€ como verbo universal si la semÃ¡ntica real es otra.

---

## 5. Persistencia

- Backend es la fuente de verdad
- Tras mutaciÃ³n exitosa:
  - refresh del bloque o de la ficha
- No se introduce lÃ³gica rica de reconstrucciÃ³n local si no aporta valor claro

---

## 6. Consecuencias

### Positivas
- Consistencia visual
- Respeto a la semÃ¡ntica del dominio
- Escalabilidad hacia workflows
- Permite distinguir entre cambio funcional y correcciÃ³n administrativa
- Evita forzar CRUD donde no encaja

### Negativas
- MÃ¡s componentes especÃ­ficos por tipo de bloque
- Menos reutilizaciÃ³n artificial
- Algunos verticales requerirÃ¡n discusiÃ³n explÃ­cita sobre si soportan `correct`

---

## 7. Alternativas descartadas

### Form builder genÃ©rico
Rechazado por pÃ©rdida de semÃ¡ntica y exceso de abstracciÃ³n.

### EdiciÃ³n global de ficha
Rechazado por complejidad, peor control de estado y mal encaje con un dominio verticalizado.

### Tratar todos los temporales como append/close puro
Rechazado como regla universal porque puede convertir errores administrativos en falsos cambios funcionales.

---

## 8. AplicaciÃ³n inicial

- Contactos â†’ SLOT
- Identificadores â†’ SLOT
- Direcciones â†’ TEMPORAL_APPEND_CLOSE

### DecisiÃ³n especÃ­fica para V1 de direcciones
En V1:
- AÃ±adir nueva direcciÃ³n
- Cerrar direcciÃ³n existente
- Sin correcciÃ³n inline de ocurrencia existente, salvo que backend exponga operaciÃ³n especÃ­fica

Esto se considera una decisiÃ³n de alcance, no una verdad permanente del patrÃ³n.

---

## 9. Futuro

Posibles evoluciones:
- IntroducciÃ³n formal de operaciÃ³n `correct` en verticales temporales
- IntegraciÃ³n de workflows
- Refinamiento de `supportedActions` por vertical
- ADR complementario si se consolida distinciÃ³n explÃ­cita entre:
  - correction
  - replacement
  - close

---

## 10. Resumen ejecutivo

El frontend de empleado no se modelarÃ¡ como un gran formulario, sino como una composiciÃ³n de bloques autÃ³nomos.

Cada bloque declara un maintenance mode.

La ediciÃ³n no se unifica por â€œtipo de formularioâ€, sino por â€œfamilia de comportamientoâ€ del vertical.

Para verticales temporales:
- el cambio funcional normal puede expresarse como append/close
- pero la correcciÃ³n administrativa no debe confundirse automÃ¡ticamente con un cambio de negocio

El frontend respetarÃ¡ siempre la semÃ¡ntica realmente soportada por backend.


<!-- END FILE: ADR-010-employee-frontend-editing.md -->


---

# FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md
<a name="file-adr-011-shared-lookup-decision-matrix-and-guidelines-md"></a>

<!-- BEGIN FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md -->

# B4RRHH â€” Matriz de adopciÃ³n del patrÃ³n shared lookup y guÃ­a de diseÃ±o

Fecha: 2026-03-21

## 1. Objetivo

Este documento fija dos cosas:

1. una **matriz prÃ¡ctica** para decidir quÃ© verticales de `employee` deben adoptar el patrÃ³n shared de lookup por business key;
2. una **guÃ­a de diseÃ±o estable** para que tanto una persona como Copilot mantengan la misma disciplina al crear o refactorizar verticales futuras.

El contexto actual es que ya existe un soporte shared mÃ­nimo de persistencia con `EmployeeBusinessKeyLookupSupport` y `EmployeeOwnedLookupSupport`, y ya se estÃ¡ usando en `contact`, `identifier` y `address` para resolver employee por business key y mapear a su contexto sin duplicar plumbing tÃ©cnico. îˆ€fileciteîˆ‚turn12file4îˆ îˆ€fileciteîˆ‚turn12file7îˆ îˆ€fileciteîˆ‚turn12file3îˆ îˆ€fileciteîˆ‚turn12file0îˆ îˆ€fileciteîˆ‚turn12file2îˆ

---

## 2. Regla madre

En B4RRHH, el cÃ³digo debe organizarse **primero por vertical y luego por capas**, y las APIs pÃºblicas deben trabajar con **business keys**, no con IDs tÃ©cnicos. Los IDs tÃ©cnicos deben quedarse en persistencia. îˆ€fileciteîˆ‚turn12file15îˆ îˆ€fileciteîˆ‚turn12file16îˆ

AdemÃ¡s, `employee.shared` sÃ³lo debe contener piezas **realmente transversales y tÃ©cnicas**. No debe convertirse en un cajÃ³n de semÃ¡ntica de negocio. îˆ€fileciteîˆ‚turn12file15îˆ

Traducido a esta decisiÃ³n concreta:

- **sÃ­** a helpers pequeÃ±os y explÃ­citos para lookup transversal repetido;
- **no** a repositorios universales, engines genÃ©ricos o shared con vocabulario funcional de un vertical.

---

## 3. QuÃ© patrÃ³n se considera ya consolidado

A dÃ­a de hoy, el patrÃ³n compartido que se considera vÃ¡lido es Ã©ste:

1. resolver `EmployeeEntity` por business key (`ruleSystemCode`, `employeeTypeCode`, `employeeNumber`);
2. resolver opcionalmente la variante con lock (`for update`);
3. delegar en una lambda o funciÃ³n local del vertical;
4. mantener el mapping a `EmployeeXContext` o la excepciÃ³n del vertical en el propio vertical.

Ese patrÃ³n estÃ¡ ya expresado en:

- `EmployeeBusinessKeyLookupSupport`, que delega en `SpringDataEmployeeRepository.findByBusinessKey(...)` y `findByBusinessKeyForUpdate(...)`; îˆ€fileciteîˆ‚turn12file4îˆ îˆ€fileciteîˆ‚turn12file18îˆ
- `EmployeeOwnedLookupSupport`, que compone el lookup del employee con una funciÃ³n `ownedLookup`, tanto en modo `Optional` como en modo `OrThrow`; îˆ€fileciteîˆ‚turn12file7îˆ îˆ€fileciteîˆ‚turn12file12îˆ
- `EmployeeContactLookupAdapter`, `EmployeeIdentifierLookupAdapter` y `EmployeeAddressLookupAdapter`, que ya usan ese soporte compartido y hacen sÃ³lo el mapping explÃ­cito a su contexto. îˆ€fileciteîˆ‚turn12file3îˆ îˆ€fileciteîˆ‚turn12file0îˆ îˆ€fileciteîˆ‚turn12file2îˆ

---

## 4. Matriz de adopciÃ³n por vertical

### 4.1 Resumen ejecutivo

| Vertical | Estado recomendado | Motivo corto |
|---|---|---|
| `contact` | Ya adoptado | lookup de owner puro y mapping simple |
| `identifier` | Ya adoptado | lookup de owner puro y mapping simple |
| `address` | Ya adoptado | lookup de owner puro y mapping simple; semÃ¡ntica temporal queda fuera |
| `presence` | Candidato fuerte siguiente | patrÃ³n de employee-context probablemente muy parecido |
| `workcenter` | Candidato medio | posible encaje para employee-context, pero revisar mezcla con validaciones de presencia |
| `cost_center` | Candidato medio | posible encaje para employee-context, pero revisar mezcla con temporalidad y porcentaje |
| `contract` | Esperar | vertical mÃ¡s cargado de timeline y replace semantics |
| `labor_classification` | Esperar | vertical mÃ¡s cargado de timeline, cobertura y relaciones |
| `journey` | No aplicar este patrÃ³n | es vertical de lectura/proyecciÃ³n, no de ownership lookup estÃ¡ndar |
| `employee` raÃ­z | No aplica | es el owner, no un child vertical |

### 4.2 Lectura detallada

#### `contact` â€” Ya adoptado

Encaja perfectamente porque el adapter sÃ³lo resuelve employee por business key y mapea a `EmployeeContactContext`. No se mete negocio del vertical en shared. îˆ€fileciteîˆ‚turn12file3îˆ

#### `identifier` â€” Ya adoptado

Mismo caso que `contact`: lookup puro del owner y mapping local. îˆ€fileciteîˆ‚turn12file0îˆ

#### `address` â€” Ya adoptado

El refactor ha eliminado `EntityManager` y SQL nativo del adapter de lookup y lo ha alineado con el mismo patrÃ³n de `contact` e `identifier`. La temporalidad de `address` sigue viviendo fuera de este helper. îˆ€fileciteîˆ‚turn12file11îˆ îˆ€fileciteîˆ‚turn12file2îˆ îˆ€fileciteîˆ‚turn12file14îˆ

#### `presence` â€” Candidato fuerte siguiente

Por estructura, es muy probable que tenga el mismo patrÃ³n de resolver employee owner y construir `EmployeePresenceContext`. Si el adapter se parece a `contact`/`identifier`/`address`, deberÃ­a entrar. La condiciÃ³n es no mezclar ahÃ­ reglas como overlap, presencia activa o cierre. AdemÃ¡s, en la arquitectura objetivo `presence` debe tender al mismo modelo vertical-first que `contact`. îˆ€fileciteîˆ‚turn12file17îˆ

#### `workcenter` â€” Candidato medio

Puede encajar si existe un `EmployeeWorkCenterLookupAdapter` que sÃ³lo resuelva contexto de employee. Debe quedarse fuera cualquier lÃ³gica de cobertura respecto a presence, gaps o consistencia. Si el adapter mezcla lookup y validaciÃ³n de cobertura, primero hay que separarlo.

#### `cost_center` â€” Candidato medio

Misma idea que `workcenter`. Puede encajar para la parte de owner lookup, pero sÃ³lo si la lÃ³gica de asignaciÃ³n, porcentaje y restricciones temporales permanece fuera.

#### `contract` â€” Esperar

AquÃ­ el riesgo de contaminar el refactor con lÃ³gica temporal es alto: `replaceFromDate`, cobertura de presence, subtype relation, cierre, update con semÃ¡ntica fuerte. Mejor no meterlo aÃºn en esta ola.

#### `labor_classification` â€” Esperar

Caso parecido a `contract`: reglas temporales mÃ¡s ricas y mayor probabilidad de que lookup y negocio estÃ©n mÃ¡s acoplados.

#### `journey` â€” No aplicar

`journey` es un vertical de lectura/proyecciÃ³n. Su problema no es ownership lookup de un child resource estÃ¡ndar, sino composiciÃ³n de tracks y eventos. Este patrÃ³n no le aporta gran cosa.

---

## 5. Regla de decisiÃ³n rÃ¡pida

Una vertical **debe entrar** en el patrÃ³n shared si se cumplen estas cinco:

1. el adapter necesita resolver employee por business key;
2. la parte repetida es claramente tÃ©cnica;
3. el resultado es un contexto o un owned lookup simple;
4. el shared no necesita aprender vocabulario del vertical;
5. el adapter queda mÃ¡s legible despuÃ©s del cambio.

Una vertical **no debe entrar todavÃ­a** si pasa cualquiera de estas cuatro:

1. el refactor arrastra reglas temporales o de negocio;
2. obliga a meter semÃ¡ntica funcional del vertical en `shared`;
3. hace el cÃ³digo mÃ¡s mÃ¡gico o mÃ¡s difÃ­cil de depurar;
4. la identidad funcional del recurso hijo todavÃ­a no estÃ¡ del todo clara.

---

## 6. QuÃ© sÃ­ puede vivir en `employee.shared`

### SÃ­

- lookup de `EmployeeEntity` por business key; îˆ€fileciteîˆ‚turn12file4îˆ
- variante con lock (`for update`); îˆ€fileciteîˆ‚turn12file4îˆ
- composiciÃ³n tÃ©cnica `employee -> ownedLookup`; îˆ€fileciteîˆ‚turn12file7îˆ
- utilidades tÃ©cnicas pequeÃ±as y explÃ­citas que se repiten igual en varios verticales.

### No

- `contactTypeCode`, `identifierTypeCode`, `addressTypeCode`, `addressNumber`, etc.;
- validaciones de catÃ¡logo del vertical;
- lÃ³gica de overlap, coverage, split, replace, close o correct;
- excepciones de dominio genÃ©ricas que sustituyan a las del vertical;
- repositorios universales tipo `EmployeeOwnedRepository<T, K>`.

---

## 7. Convenciones de diseÃ±o que deben quedar escritas

### 7.1 Identidad

La identidad pÃºblica siempre debe expresarse con business keys. En `employee`, eso significa al menos:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber` îˆ€fileciteîˆ‚turn12file15îˆ îˆ€fileciteîˆ‚turn12file16îˆ

Los IDs tÃ©cnicos sÃ³lo deben vivir en persistencia. îˆ€fileciteîˆ‚turn12file15îˆ

### 7.2 OrganizaciÃ³n del cÃ³digo

El patrÃ³n objetivo sigue siendo:

- `com.b4rrhh.<bounded-context>.<vertical>.application`
- `com.b4rrhh.<bounded-context>.<vertical>.domain`
- `com.b4rrhh.<bounded-context>.<vertical>.infrastructure` îˆ€fileciteîˆ‚turn12file15îˆ

### 7.3 Regla de abstracciÃ³n

Extraer helper sÃ³lo cuando:

- el patrÃ³n ya se repite;
- la variaciÃ³n estÃ¡ entendida;
- la abstracciÃ³n hace el cÃ³digo mÃ¡s simple, no mÃ¡s listo.

### 7.4 Mapping

El mapping de `EmployeeEntity -> EmployeeXContext` debe seguir siendo **local al adapter del vertical**, como ya pasa en `contact`, `identifier` y `address`. îˆ€fileciteîˆ‚turn12file3îˆ îˆ€fileciteîˆ‚turn12file0îˆ îˆ€fileciteîˆ‚turn12file2îˆ

### 7.5 Tests mÃ­nimos al introducir una vertical en este patrÃ³n

Cada adapter que adopte el patrÃ³n debe tener al menos:

- caso feliz en lookup normal;
- caso feliz en lookup for-update;
- `Optional.empty()` cuando no existe employee en lookup normal;
- `Optional.empty()` cuando no existe employee en lookup for-update.

Eso ya estÃ¡ aplicado en `address`, y de forma equivalente en `contact` e `identifier`. îˆ€fileciteîˆ‚turn12file5îˆ îˆ€fileciteîˆ‚turn12file8îˆ îˆ€fileciteîˆ‚turn12file9îˆ

---

## 8. GuardarraÃ­les para diseÃ±ar verticales nuevas

Cuando nazca una vertical nueva bajo `employee`, aplicar este checklist antes de escribir cÃ³digo:

1. **Identidad funcional**: Â¿cuÃ¡l es la business key pÃºblica del recurso?
2. **Ownership**: Â¿ese recurso cuelga de employee por business key?
3. **Naturaleza**: Â¿es `SLOT`, `TEMPORAL_APPEND_CLOSE`, workflow u otra familia? Esto es importante tambiÃ©n para frontend y UX. En V1, por ejemplo, `contact` e `identifier` se tratan como `SLOT` y `address` como `TEMPORAL_APPEND_CLOSE`. îˆ€fileciteîˆ‚turn12file19îˆ
4. **Lookup tÃ©cnico**: Â¿hay un adapter de contexto que sÃ³lo resuelve employee? Si sÃ­, debe usar el patrÃ³n shared.
5. **Reglas de negocio**: Â¿quÃ© debe quedarse fuera de shared sÃ­ o sÃ­?
6. **DTOs y endpoints**: Â¿estÃ¡n formulados por business keys y no por IDs tÃ©cnicos?
7. **Tests**: Â¿se estÃ¡n probando ownership, duplicados, validaciÃ³n y constraints? îˆ€fileciteîˆ‚turn12file15îˆ

---

## 9. Prompt base recomendado para Copilot

### Uso

Este bloque sirve como cabecera de contexto para cualquier refactor o implementaciÃ³n futura en verticales `employee`.

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

## 10. DecisiÃ³n operativa recomendada desde hoy

Orden sugerido para prÃ³ximas revisiones:

1. `presence`
2. `workcenter`
3. `cost_center`
4. parar y reevaluar
5. dejar `contract` y `labor_classification` para una discusiÃ³n separada

La razÃ³n es simple: conviene seguir capturando el patrÃ³n donde el beneficio es alto y el riesgo semÃ¡ntico es bajo, y frenar antes de entrar en verticales donde la lÃ³gica temporal fuerte pueda contaminar la abstracciÃ³n.

---

## 11. Resumen ejecutivo

- El patrÃ³n shared de lookup ya estÃ¡ consolidado para `contact`, `identifier` y `address`. îˆ€fileciteîˆ‚turn12file3îˆ îˆ€fileciteîˆ‚turn12file0îˆ îˆ€fileciteîˆ‚turn12file2îˆ
- El patrÃ³n correcto es pequeÃ±o: resolver employee por business key, componer una lambda local y dejar mapping/excepciones en el vertical. îˆ€fileciteîˆ‚turn12file4îˆ îˆ€fileciteîˆ‚turn12file7îˆ
- `presence` es el siguiente candidato natural.
- `workcenter` y `cost_center` son candidatos posibles, pero sÃ³lo para la parte de owner lookup.
- `contract` y `labor_classification` deben esperar.
- Este documento debe usarse como criterio de diseÃ±o y como prÃ³logo de prompts para Copilot.


<!-- END FILE: ADR-011-shared-lookup-decision-matrix-and-guidelines.md -->


---

# FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md
<a name="file-adr-012-racionalizaci-n-de-naming-y-alcance-sem-ntico-de-rule-entity-type-md"></a>

<!-- BEGIN FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md -->

ADR â€” RacionalizaciÃ³n de naming y alcance semÃ¡ntico de rule_entity_type en B4RRHH
Estado

Propuesto

Contexto

B4RRHH ya dispone de un metamodelo funcional basado en:

rule_system
rule_entity_type
rule_entity

Este metamodelo estÃ¡ empezando a exponerse y utilizarse de forma real desde frontend mediante una pantalla de catÃ¡logos, lo que ha hecho visible una tensiÃ³n de diseÃ±o:

algunos rule_entity_type fueron nombrados inicialmente desde la vertical o caso de uso donde aparecieron primero
al crecer el sistema, se observa que ciertos conceptos no pertenecen realmente a una sola vertical, sino que son reutilizables en varias partes del dominio

Ejemplo tÃ­pico:

un nombre como EMPLOYEE_PRESENCE_COMPANY puede haber sido razonable en una iteraciÃ³n temprana
pero al madurar el dominio, â€œcompanyâ€ aparece como concepto reutilizable tambiÃ©n en otras verticales o workflows
por tanto, el naming anterior queda demasiado estrecho

AdemÃ¡s, los seeds iniciales de rule_entity y sus labels visibles pueden haber sido definidos con una orientaciÃ³n mÃ¡s tÃ©cnica o provisional que funcional.

Esto no invalida el modelo actual, pero sÃ­ revela una deuda semÃ¡ntica normal de maduraciÃ³n.

La arquitectura general del proyecto prioriza:

vertical-first
business keys en APIs
naming orientado a negocio y estable
separaciÃ³n clara entre dominio y detalle tÃ©cnico
Problema

Sin una guÃ­a explÃ­cita, el catÃ¡logo corre el riesgo de evolucionar como una mezcla de:

conceptos reutilizables del dominio
conceptos especÃ­ficos de una vertical
labels provisionales de seeds
nombres demasiado pegados a una implementaciÃ³n temporal

Esto genera varios riesgos:

semÃ¡ntica inconsistente
duplicidad futura de tipos de entidad
dificultad para reutilizar catÃ¡logos transversales
prompts peores para Copilot
APIs y validadores atados a nombres demasiado concretos
DecisiÃ³n

Se adopta una convenciÃ³n explÃ­cita para diseÃ±ar y revisar rule_entity_type y rule_entity:

1. Un rule_entity_type debe nombrar el concepto funcional real, no el primer lugar donde se usÃ³

Ejemplos:

preferir COMPANY
evitar EMPLOYEE_PRESENCE_COMPANY si el concepto â€œcompanyâ€ es reutilizable
2. Los tipos de entidad se clasifican por alcance semÃ¡ntico
A. Domain reusable catalog

Conceptos reutilizables en mÃ¡s de una vertical o bounded context relacionado.

Ejemplos:

COMPANY
WORK_CENTER
COST_CENTER
COUNTRY
B. Employee-specific catalog

Conceptos propios del bounded context employee, pero no de una Ãºnica vertical tÃ©cnica.

Ejemplos:

EMPLOYEE_CONTACT_TYPE
EMPLOYEE_IDENTIFIER_TYPE
EMPLOYEE_ADDRESS_TYPE
C. Lifecycle-specific catalog

Conceptos ligados a una acciÃ³n o transiciÃ³n funcional del ciclo de vida laboral.

Ejemplos:

EMPLOYEE_ENTRY_REASON
EMPLOYEE_EXIT_REASON
3. El naming debe seguir el criterio de reutilizaciÃ³n mÃ¡xima razonable

Regla prÃ¡ctica:

si el concepto puede ser usado de forma natural por varias verticales, debe nombrarse de forma genÃ©rica
si el concepto solo tiene sentido en un contexto funcional especÃ­fico, puede nombrarse de forma especÃ­fica
no debe usarse un prefijo de vertical solo porque el primer consumidor pertenezca a esa vertical
4. rule_entity.code debe ser estable y funcional

El code:

debe ser estable
debe evitar ruido tÃ©cnico
no debe incorporar accidentalmente detalles de UI o de implementaciÃ³n
5. rule_entity.name debe tratarse como label funcional visible

El name:

no es la identidad
puede evolucionar para mejorar claridad funcional
debe pensarse como literal entendible por usuario/negocio
6. description se reserva para contexto adicional, no para sustituir al nombre

La descripciÃ³n:

amplÃ­a
no corrige un name pobre
no debe convertirse en el Ãºnico lugar donde vive la semÃ¡ntica
No objetivos

Este ADR no introduce todavÃ­a:

renombrado masivo inmediato de tipos existentes
migraciones globales de seeds
jerarquÃ­as complejas entre tipos
nuevo modelo de persistencia
UI para mantenimiento de rule_entity_type
Estrategia de aplicaciÃ³n
1. No hacer big bang

No se recomienda un renombrado inmediato de todos los tipos actuales.

2. AplicaciÃ³n a futuro

A partir de este ADR:

todo rule_entity_type nuevo debe pasar por esta revisiÃ³n semÃ¡ntica
Copilot debe recibir esta regla en prompts de backend y metamodelo
los nombres nuevos no deben quedar estrechamente acoplados a la primera vertical consumidora
3. RevisiÃ³n incremental de deuda existente

Los tipos actuales que hayan quedado demasiado especÃ­ficos se documentarÃ¡n como deuda semÃ¡ntica y se revisarÃ¡n cuando compense funcionalmente.

Checklist para nuevos rule_entity_type

Antes de crear uno nuevo, revisar:

Â¿Describe un concepto reutilizable o una regla local?
Â¿Ese concepto podrÃ­a ser consumido por otra vertical en los prÃ³ximos pasos?
Â¿El nombre estÃ¡ reflejando el dominio o la implementaciÃ³n actual?
Â¿Estamos poniendo prefijo de vertical por necesidad real o por comodidad momentÃ¡nea?
Â¿El name visible es suficientemente funcional para usuario/negocio?
Ejemplos orientativos
Buenos candidatos a naming genÃ©rico
COMPANY
WORK_CENTER
COST_CENTER
COUNTRY
Buenos candidatos a naming especÃ­fico
EMPLOYEE_CONTACT_TYPE
EMPLOYEE_IDENTIFIER_TYPE
EMPLOYEE_ENTRY_REASON
EMPLOYEE_EXIT_REASON
Sospechosos a revisar
tipos cuyo nombre empiece por una vertical concreta pero describan un concepto reutilizable
tipos cuyo name visible parezca una explicaciÃ³n provisional y no una etiqueta funcional
Consecuencias positivas
mejor semÃ¡ntica de dominio
mayor reutilizaciÃ³n de catÃ¡logos
menor duplicidad futura
prompts mÃ¡s precisos
mejor UX en la pantalla de catÃ¡logos
Consecuencias negativas
aparece deuda visible en nombres ya existentes
obliga a pensar mÃ¡s antes de crear nuevos tipos
en el futuro puede requerir migraciones o aliases si se decide racionalizar nombres existentes

<!-- END FILE: ADR-012-Racionalización-de-naming-y-alcance-semántico-de-rule_entity_type.md -->


---

# FILE: ADR-013-Mantenimiento-de-rule_entity.md
<a name="file-adr-013-mantenimiento-de-rule-entity-md"></a>

<!-- BEGIN FILE: ADR-013-Mantenimiento-de-rule_entity.md -->

ADR â€” Mantenimiento de rule_entity en B4RRHH
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

y el modelo pÃºblico incluye:

ruleSystemCode
ruleEntityTypeCode
code
name
description
active
startDate
endDate

La pantalla de catÃ¡logos ya permite:

seleccionar rule_system
seleccionar rule_entity_type
listar rule_entity
crear nuevos valores de catÃ¡logo

Sin embargo, todavÃ­a no existe una estrategia explÃ­cita de mantenimiento para:

corregir una ocurrencia existente
cerrar su vigencia
eliminar ocurrencias errÃ³neas sin uso

AdemÃ¡s, B4RRHH ya distingue en frontend entre:

ediciÃ³n tipo SLOT
mantenimiento temporal
correcciÃ³n administrativa frente a cambio funcional real

TambiÃ©n existe ya una decisiÃ³n previa de naming/semÃ¡ntica: rule_entity_type debe nombrar el concepto funcional real y rule_entity.code debe ser estable, mientras que name actÃºa como label funcional visible

Problema

Si rule_entity se trata como un CRUD plano, aparecen varios riesgos:

confusiÃ³n entre identidad y datos corregibles
pÃ©rdida de histÃ³rico semÃ¡ntico
borrados peligrosos de valores de catÃ¡logo ya usados por empleados u otros recursos
un frontend que muestra verbos genÃ©ricos sin reflejar la semÃ¡ntica real del dominio

Por el contrario, si se prohÃ­be todo mantenimiento salvo el alta, el catÃ¡logo queda operativamente incompleto.

Es necesario definir:

quÃ© constituye la identidad funcional de una ocurrencia de rule_entity
quÃ© operaciones canÃ³nicas existen
quÃ© se puede corregir
cuÃ¡ndo procede cerrar
si existe DELETE, en quÃ© condiciones
DecisiÃ³n

Se adopta para rule_entity un modelo de mantenimiento de catÃ¡logo con vigencia y borrado excepcional restringido.

1. Naturaleza funcional

rule_entity se modela como un catÃ¡logo parametrizable con vigencia temporal ligera:

puede tener histÃ³rico por cÃ³digo
no exige cobertura continua
no debe tratarse como CRUD plano
no debe confundirse correcciÃ³n administrativa con cambio funcional
2. Identidad funcional

La identidad funcional de una ocurrencia de rule_entity serÃ¡:

ruleSystemCode
ruleEntityTypeCode
code
startDate

Esta combinaciÃ³n identifica una ocurrencia concreta del valor de catÃ¡logo.

3. Campos inmutables

Una vez creada la ocurrencia, no podrÃ¡n modificarse:

ruleSystemCode
ruleEntityTypeCode
code
startDate
4. Campos corregibles

PodrÃ¡n corregirse:

name
description
endDate
5. Tratamiento de active

active se considera preferentemente un dato derivado/read-model a partir de la vigencia real.

Mientras el contrato pÃºblico lo mantenga, backend podrÃ¡ seguir retornÃ¡ndolo, pero el mantenimiento canÃ³nico no debe apoyarse en editar active de forma arbitraria si eso duplica la semÃ¡ntica de endDate.

6. Operaciones canÃ³nicas
6.1 Crear

Se mantiene:

POST /rule-entities
6.2 Consultar una ocurrencia concreta

Se aÃ±ade una lectura canÃ³nica por business key completa:

GET /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
6.3 Corregir una ocurrencia existente

Se aÃ±ade una operaciÃ³n de correcciÃ³n administrativa:

PUT /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}

Esta operaciÃ³n corrige la misma ocurrencia y no crea una nueva.

Campos permitidos en request:

name
description
endDate
6.4 Cerrar vigencia

Se aÃ±ade una operaciÃ³n explÃ­cita de cierre:

POST /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}/close

Request:

endDate

Su semÃ¡ntica es cerrar la vigencia de la ocurrencia existente.

6.5 Eliminar

Se admite DELETE, pero como operaciÃ³n excepcional y restringida, no como verbo principal de mantenimiento:

DELETE /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}

Su semÃ¡ntica es borrado fÃ­sico de una ocurrencia de catÃ¡logo solo si backend demuestra que no estÃ¡ usada.

Reglas de borrado

DELETE solo estarÃ¡ permitido si se cumplen todas las condiciones siguientes:

La ocurrencia existe.
La comprobaciÃ³n se realiza dentro del rule_system de la ocurrencia.
La rule_entity no estÃ¡ referenciada por ningÃºn recurso de negocio existente que dependa de ella.
La comprobaciÃ³n de referencias debe hacerse en backend, nunca en frontend.
Si existen referencias, la operaciÃ³n falla con conflicto de negocio y no degrada a soft delete implÃ­cito.
PolÃ­tica de referencias

Se considera â€œreferenciadaâ€ una rule_entity cuando su cÃ³digo estÃ¡ siendo usado por cualquier recurso real que la valide o consuma en ese rule_system.

Ejemplos tÃ­picos:

companyCode en presence
contactTypeCode en contacts
identifierTypeCode en identifiers
addressTypeCode en addresses
workCenterCode en work centers
costCenterCode en cost centers
contractCode o contractSubtypeCode en contracts
agreementCode o agreementCategoryCode en labor classifications

La comprobaciÃ³n exacta dependerÃ¡ del ruleEntityTypeCode y de los verticales que consuman ese catÃ¡logo.

Reglas de dominio adicionales
no puede haber solape de vigencia para la misma combinaciÃ³n ruleSystemCode + ruleEntityTypeCode + code
endDate no puede ser menor que startDate
una correcciÃ³n administrativa no debe alterar la identidad funcional
un cierre expresa fin de vigencia, no borrado
un cambio funcional normal puede resolverse como cierre de la ocurrencia vigente y alta de una nueva ocurrencia
SemÃ¡ntica de frontend

El frontend de catÃ¡logos debe exponer acciones honestas y alineadas con backend:

Crear
Editar
entendido como correcciÃ³n administrativa de la misma ocurrencia
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

haya referencias activas o histÃ³ricas que impidan el borrado
el cierre o correcciÃ³n rompa reglas temporales
se intente dejar la ocurrencia en un estado inconsistente
No objetivos

Este ADR no introduce todavÃ­a:

mantenimiento frontend de rule_entity_type
renombrado masivo de tipos o seeds
versionado complejo de catÃ¡logos
soft delete genÃ©rico
cascadas automÃ¡ticas de cleanup
Consecuencias positivas
mantenimiento realista de rule_entity
histÃ³rico preservado cuando corresponde
borrado fÃ­sico posible para errores sin uso
menor riesgo de destruir datos referenciados
frontend con verbos honestos y semÃ¡nticos
Consecuencias negativas
backend necesita lÃ³gica de comprobaciÃ³n de referencias
DELETE deja de ser trivial
algunos casos requerirÃ¡n decidir si aplicar correct, close o delete
aparece coste de diseÃ±o por tipo de catÃ¡logo consumidor
Estrategia de implementaciÃ³n
Fase 1

Backend:

GET by business key
PUT correct
POST close
DELETE con comprobaciÃ³n de referencias
Fase 2

Frontend:

abrir detalle/ediciÃ³n de ocurrencia concreta
soportar editar
soportar cerrar
soportar eliminar con confirmaciÃ³n ligera
Fase 3

Refinamiento:

mensajes de conflicto por entidad en uso
posible visibilidad de motivo de bloqueo
tests por tipo consumidor
Resumen

rule_entity no se gestionarÃ¡ como CRUD plano.

Su mantenimiento canÃ³nico en B4RRHH serÃ¡:

create
get by business key
correct
close
delete restringido

DELETE existirÃ¡, pero Ãºnicamente como operaciÃ³n excepcional y segura, protegida por validaciÃ³n backend de ausencia total de referencias dentro del rule_system.

<!-- END FILE: ADR-013-Mantenimiento-de-rule_entity.md -->


---

# FILE: ADR-014-employee-frontend-ui.md
<a name="file-adr-014-employee-frontend-ui-md"></a>

<!-- BEGIN FILE: ADR-014-employee-frontend-ui.md -->

# ADR â€” Employee Frontend Section System and Visual Identity

## 1. Objetivo
Definir un sistema visual coherente y reutilizable para la ficha de empleado basado en verticales.

## 2. Principios
- La ficha es composiciÃ³n de verticales
- Consistencia visual transversal
- Backend-driven UI
- No formularios monolÃ­ticos

## 3. Unidad base: Section Shell
Componente base que define:
- Header (tÃ­tulo + acciones)
- Body (contenido)
- Footer (estado: loading/error/success)

## 4. Maintenance modes â†’ UI
- SLOT â†’ lista editable
- TEMPORAL_APPEND_CLOSE â†’ histÃ³rico con activo
- WORKFLOW â†’ acciones guiadas
- READONLY â†’ solo lectura

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
- No crear componentes genÃ©ricos universales
- No mezclar lÃ³gica de negocio en UI
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

# ADR-015 â€” Binding de catÃ¡logos por recurso y campo

## Estado
Propuesto

## Contexto
B4RRHH ya dispone de un metamodelo funcional de catÃ¡logos en el bounded context `rulesystem`:
- `rule_system`
- `rule_entity_type`
- `rule_entity`

AdemÃ¡s:
- ya existen endpoints de `rule_entity` filtrables por business keys;
- ya existe un caso dependiente real para ediciÃ³n: `GET /labor-classification-catalog/agreement-categories`;
- `rule_entity_type` debe nombrar conceptos funcionales reutilizables (por ejemplo `COMPANY`, `WORK_CENTER`, `COST_CENTER`);
- frontend no debe asumir semÃ¡ntica de metamodelo compleja ni convertirse en renderizador genÃ©rico de formularios.

El problema de producto actual tiene dos necesidades simultÃ¡neas:
1. mostrar labels/literales visibles en frontend, no solo cÃ³digos;
2. conocer quÃ© catÃ¡logo aplica a un campo concreto de un recurso para pedir opciones vÃ¡lidas por `rule_system`.

## Problema
Hoy, el sistema valida cÃ³digos de catÃ¡logo en verticales concretos, pero no existe un diccionario backend explÃ­cito y reusable que responda de forma simple:
- quÃ© catÃ¡logo corresponde a cada campo;
- si ese catÃ¡logo se resuelve de forma directa, dependiente o custom.

Sin este diccionario aparecen dos riesgos no deseados:
- construir un "GET de la muerte" que devuelva todas las `rule_entities` para que frontend infiera todo;
- codificar manualmente vertical por vertical y campo por campo en Angular.

## DecisiÃ³n
Se adopta una soluciÃ³n backend-first, pequeÃ±a y evolutiva basada en binding recurso/campo -> catÃ¡logo aplicable.

### Decisiones fijadas
1. Introducir la tabla `resource_field_catalog_binding`.
2. Clasificar bindings en `DIRECT`, `DEPENDENT`, `CUSTOM`.
3. El binding define **quÃ© catÃ¡logo aplica**, no **cÃ³mo renderizar formularios**.
4. Para lectura, preferir read models enriquecidos con `code + name`.
5. Para ediciÃ³n, frontend consulta bindings y consume opciones directas o endpoints especÃ­ficos segÃºn el caso.
6. No introducir en esta fase un motor universal de dependencias.
7. No introducir un endpoint masivo de todas las `rule_entities`.

## DiseÃ±o Propuesto
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

RelaciÃ³n recomendada:
- `ruleEntityTypeCode` referencia por business key a `rule_entity_type.code` cuando aplique.

### Reglas de consistencia
- `DIRECT` => `ruleEntityTypeCode` obligatorio y `dependsOnFieldCode` nulo.
- `DEPENDENT` => `ruleEntityTypeCode` obligatorio y `dependsOnFieldCode` obligatorio.
- `CUSTOM` => `customResolverCode` obligatorio.

### SemÃ¡ntica de resoluciÃ³n
- `DIRECT`: opciones por `ruleSystemCode + ruleEntityTypeCode`.
- `DEPENDENT`: opciones por endpoint especÃ­fico del caso de negocio.
- `CUSTOM`: resoluciÃ³n especÃ­fica controlada por backend, explÃ­cita por `customResolverCode`.

## API Propuesta (primera iteraciÃ³n)
### 1) Consultar bindings de un recurso
`GET /catalog-bindings/{resourceCode}`

Respuesta mÃ­nima:
- `resourceCode`
- `bindings[]` con:
  - `fieldCode`
  - `catalogKind`
  - `ruleEntityTypeCode` (nullable)
  - `dependsOnFieldCode` (nullable)
  - `customResolverCode` (nullable)
  - `active`

### 2) Obtener opciones de catÃ¡logo directo
`GET /catalog-options/direct?ruleSystemCode=...&ruleEntityTypeCode=...&referenceDate=...&q=...`

Respuesta mÃ­nima:
- `items[]` con:
  - `code`
  - `name`
  - `active`
  - `startDate`
  - `endDate`

### 3) Casos dependientes
Mantener endpoints especÃ­ficos cuando compense (por ejemplo `labor-classification-catalog/agreement-categories`).

## Reglas de Uso
### Lectura
Backend enriquece read models con labels visibles sin delegar inferencias al frontend.

Ejemplos:
- `workCenterCode` + `workCenterName`
- `agreementCode` + `agreementName`
- `agreementCategoryCode` + `agreementCategoryName`

### EdiciÃ³n
Frontend:
1. consulta binding por `resourceCode`;
2. para `DIRECT`, pide opciones directas por `ruleSystemCode`;
3. para `DEPENDENT`/`CUSTOM`, usa endpoint especÃ­fico del caso.

No se pretende frontend dinÃ¡mico universal.

## No Objetivos
Este ADR no pretende resolver todavÃ­a:
- un form builder genÃ©rico;
- un motor universal de dependencias entre campos;
- inferencia automÃ¡tica de UI desde metamodelo completo;
- un endpoint masivo que exponga todas las `rule_entities` para que frontend deduzca semÃ¡ntica;
- relajar reglas de vertical-first o mover lÃ³gica de dominio a Angular.

## Consecuencias
### Positivas
- Evita acoplamiento manual campo a campo en frontend.
- Mantiene el control semÃ¡ntico en backend.
- Permite crecimiento incremental por verticales sin arquitectura astronauta.
- Reutiliza metamodelo existente y business keys.

### Costes
- Introduce una tabla mÃ¡s en metamodelo de consumo.
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
- Integrar consumo en frontend para ediciÃ³n guiada por binding.
- Extender gradualmente a mÃ¡s verticales y consolidar endpoints dependientes puntuales.

## Riesgos a Evitar
- Convertir el binding en framework genÃ©rico de formularios.
- Duplicar semÃ¡ntica de negocio en frontend.
- DiseÃ±ar una API universal compleja antes de validar casos reales.
- Introducir IDs tÃ©cnicos en contratos pÃºblicos.
- Romper vertical-first moviendo reglas de dominio fuera de sus verticales.


<!-- END FILE: ADR-015-Binding-de-catalogos-por-recurso-y-campo.md -->


---

# FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md
<a name="file-adr-016-anatomia-visual-y-patrones-de-interacci-n-de-la-ficha-de-empleado-md"></a>

<!-- BEGIN FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md -->

ADR â€” AnatomÃ­a visual y patrones de interacciÃ³n de la ficha de empleado
1. Estado

PROPOSED â†’ TARGET: ACCEPTED

2. Contexto

El frontend de B4RRHH ha evolucionado hacia una arquitectura por verticales, con una clara separaciÃ³n entre:

dominio (backend)
contrato (OpenAPI)
frontend desacoplado (Angular)

Las decisiones previas relevantes establecen que:

la ficha de empleado es una composiciÃ³n de secciones autÃ³nomas
la ediciÃ³n se rige por maintenance modes (SLOT, TEMPORAL_APPEND_CLOSE, WORKFLOW, READONLY)
el frontend no debe inferir semÃ¡ntica compleja, sino consumirla del backend
las acciones deben ser semÃ¡nticamente honestas, evitando CRUD genÃ©rico

Sin embargo, el estado actual de la UI:

transmite una sensaciÃ³n de â€œpantalla tÃ©cnicaâ€
carece de una anatomÃ­a visual consolidada
no expresa de forma clara el ciclo de vida del empleado
no diferencia visualmente tipos de informaciÃ³n (actual vs histÃ³rico vs workflow)

Existe el riesgo de:

aplicar mejoras estÃ©ticas locales sin coherencia global
introducir abstracciones genÃ©ricas que rompan la semÃ¡ntica del dominio
degradar la experiencia al crecer en verticales
3. Problema

Se requiere definir una arquitectura de experiencia y anatomÃ­a visual coherente, que:

exprese correctamente el dominio (lifecycle del empleado)
escale con nuevas verticales
mantenga la semÃ¡ntica de negocio
evite caer en formularios genÃ©ricos o UI tÃ©cnica
4. DecisiÃ³n
4.1 La ficha como composiciÃ³n estructurada

La ficha de empleado se consolida como:

Una composiciÃ³n de secciones autÃ³nomas con una jerarquÃ­a visual clara y consistente

Estructura base:

Cabecera de empleado (contexto)
Estado actual
Datos operativos (SLOT)
Datos histÃ³ricos (TEMPORAL)
Acciones de negocio (WORKFLOW)
Timeline lateral persistente
4.2 Cabecera como componente de producto

Se introduce un componente de cabecera que:

muestra identidad completa del empleado
muestra estado derivado (Activo / Inactivo)
expone contexto actual (empresa, centro, fechas)
incluye contacto bÃ¡sico inline
expone acciones principales dependientes de estado

Regla clave:

La cabecera debe permitir entender el estado del empleado sin navegar ni leer bloques inferiores.

4.3 ContrataciÃ³n como punto de entrada

Se establece que:

la acciÃ³n primaria del sistema es Nueva contrataciÃ³n
no se expone â€œcrear empleadoâ€ como acciÃ³n independiente

Regla:

La contrataciÃ³n inicial crea simultÃ¡neamente la identidad del empleado y su primera relaciÃ³n laboral (presence).

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
no separa artificialmente â€œpersonaâ€ y â€œrelaciÃ³nâ€ en la experiencia
4.5 Timeline como contexto lateral persistente

Se introduce un componente de timeline con estas reglas:

en escritorio:
aparece como panel lateral derecho persistente
en mÃ³vil:
se reubica al final de la ficha

CaracterÃ­sticas:

representa el lifecycle completo
no es una tabla
no compite con el contenido principal
proporciona contexto continuo

Regla:

El timeline es contexto, no contenido principal.

4.6 SeparaciÃ³n visual por familias funcionales

Cada tipo de mantenimiento se representa con un patrÃ³n visual distinto:

SLOT
datos actuales
lectura limpia
sin tablas
ediciÃ³n localizada
TEMPORAL_APPEND_CLOSE
ocurrencia actual destacada
histÃ³rico secundario
acciones: aÃ±adir / cerrar / corregir
WORKFLOW
no parece formulario
acciones de negocio explÃ­citas
lenguaje semÃ¡ntico
READONLY
lectura pura
sin affordances engaÃ±osas
4.7 Shell comÃºn de secciÃ³n

Todas las secciones comparten un shell visual:

tÃ­tulo
acciones
contenido
estado (loading/error/success)

Pero:

La lÃ³gica interna no se unifica en un componente genÃ©rico.

4.8 UI semÃ¡nticamente honesta

Se prohÃ­be el uso de:

â€œEditarâ€ como verbo universal
acciones tÃ©cnicas (create/update/delete)

Se obliga a usar:

AÃ±adir
Eliminar
Cerrar
Corregir
Contratar
Terminar
Recontratar
4.9 No uso de form builders genÃ©ricos

Se establece explÃ­citamente:

no se implementarÃ¡ un motor genÃ©rico de formularios
no se trasladarÃ¡ la semÃ¡ntica de negocio al frontend
5. AnatomÃ­a visual objetivo
Layout escritorio
contenido principal (izquierda)
timeline lateral (derecha)
Layout mÃ³vil
contenido en flujo
timeline al final
JerarquÃ­a
Cabecera
Estado actual
Datos SLOT
Datos TEMPORAL
Acciones
Timeline
6. Consecuencias
Positivas
sensaciÃ³n de producto profesional
coherencia entre verticales
escalabilidad real
mejor alineaciÃ³n con dominio
reducciÃ³n de deuda futura
Negativas
refactor inicial de UI existente
mayor disciplina en frontend
necesidad de mantener consistencia
7. Alternativas descartadas
mejoras visuales locales sin blueprint
uso de librerÃ­as UI como soluciÃ³n completa
componente universal configurable
formulario Ãºnico editable
timeline como tabla
8. Plan de implementaciÃ³n
Fase 0 â€” ConsolidaciÃ³n
aprobar ADR
documentar patrones
alinear naming
Fase 1 â€” Foundation
employee-page-header
employee-section-shell
tokens visuales
badges y estados
Fase 2 â€” SLOT
consolidar contacto
consolidar identificadores
Fase 3 â€” TEMPORAL
rediseÃ±ar address
introducir patrÃ³n histÃ³rico
Fase 4 â€” Layout
implementar layout con timeline lateral
responsive
Fase 5 â€” Timeline
implementar timeline discreto
integrar presence
Fase 6 â€” PreparaciÃ³n workflows
preparar patrÃ³n workflow
integrar acciones lifecycle
9. Reglas para Copilot (CRÃTICO)

Copilot debe:

respetar la anatomÃ­a de secciÃ³n
no introducir componentes genÃ©ricos
no alterar semÃ¡ntica de negocio
usar naming consistente
priorizar claridad sobre reutilizaciÃ³n excesiva

Copilot NO debe:

crear form builders
introducir lÃ³gica de negocio en frontend
usar â€œeditâ€ como acciÃ³n universal
mezclar tipos de mantenimiento
ðŸŽ¯ Resultado esperado

Una UI que:

no parece tÃ©cnica
no parece CRUD
expresa claramente el dominio
escala sin romperse
puede evolucionar hacia producto completo de RRHH

<!-- END FILE: ADR-016-Anatomia-visual-y-patrones-de-interacción-de-la-ficha-de-empleado.md -->


---

# FILE: ADR-017-Cost-center-design.md
<a name="file-adr-017-cost-center-design-md"></a>

<!-- BEGIN FILE: ADR-017-Cost-center-design.md -->

ADR â€” Employee Cost Center Vertical
Estado

Propuesto

Contexto

B4RRHH modela el dominio de empleado mediante verticales funcionales independientes dentro del bounded context employee, siguiendo arquitectura vertical-first, hexagonal interna y APIs pÃºblicas basadas exclusivamente en business keys. El empleado se identifica funcionalmente por:

ruleSystemCode
employeeTypeCode
employeeNumber

y los recursos hijos deben derivar su identidad desde esa business key, sin exponer IDs tÃ©cnicos en la API pÃºblica.

En la evoluciÃ³n del mapa de verticales del proyecto, cost_center ya aparece identificado como una vertical de tipo:

DISTRIBUTED_TIMELINE
catÃ¡logo simple
reglas temporales: MULTI_ACTIVE + SUM<=100

y pendiente todavÃ­a de aterrizar operativamente.

AdemÃ¡s, el lenguaje temporal comÃºn del proyecto distingue claramente entre:

STRONG_TIMELINE
FLEXIBLE_TIMELINE
DISTRIBUTED_TIMELINE

reservando para esta Ãºltima los casos multi-activos y con reglas agregadas, tÃ­picamente basadas en porcentajes. El propio patrÃ³n de StrongTimelineReplacePlanner indica expresamente que no debe aplicarse directamente a cost_center, porque esa vertical no es single-active ni de cobertura completa.

TambiÃ©n existe ya una decisiÃ³n explÃ­cita de naming de catÃ¡logos reutilizables: cuando el concepto es transversal o reusable, el rule_entity_type debe nombrar el concepto funcional real. COST_CENTER aparece como ejemplo claro de catÃ¡logo reusable y ademÃ¡s ya estÃ¡ previsto como binding directo para employee.cost_center / costCenterCode.

Por Ãºltimo, el dominio de lifecycle ya establece que TERMINATION debe cerrar o ajustar asignaciones vigentes del empleado, preservando el histÃ³rico y sin dejar restos abiertos tras la terminaciÃ³n.

Problema

employee.cost_center no encaja correctamente ni como:

CRUD plano por filas
ni como simple clon de work_center
ni como STRONG_TIMELINE

porque su semÃ¡ntica real no es â€œuna Ãºnica asignaciÃ³n activaâ€, sino una distribuciÃ³n organizativa que puede tener varias lÃ­neas simultÃ¡neas activas para una misma fecha.

Ejemplo funcional vÃ¡lido:

50% en CC_A
50% en CC_B

vigentes a la vez desde la misma fecha.

Esto introduce necesidades especÃ­ficas que no aparecen en verticales single-active:

permitir multi-actividad simultÃ¡nea;
impedir que la suma de porcentajes supere 100 en un momento dado;
evitar mezclas incoherentes de lÃ­neas paralelas con fechas de inicio distintas;
definir una unidad funcional de cambio mÃ¡s fuerte que â€œuna fila aisladaâ€;
aclarar cÃ³mo impacta TERMINATION;
definir operaciones canÃ³nicas honestas, evitando un CRUD fila a fila que rompa la consistencia agregada.
DecisiÃ³n

Se adopta para employee.cost_center un modelo de vertical de tipo DISTRIBUTED_TIMELINE, cuyo elemento funcional real no es una fila aislada, sino una ventana de distribuciÃ³n de centros de coste para un empleado.

La vertical:

serÃ¡ historizada;
permitirÃ¡ mÃºltiples lÃ­neas activas simultÃ¡neamente;
validarÃ¡ catÃ¡logo COST_CENTER;
exigirÃ¡ contenciÃ³n dentro de una presence;
impondrÃ¡ que la suma de porcentajes activos no supere 100;
y, cuando exista mÃ¡s de una lÃ­nea paralela activa, exigirÃ¡ que todas compartan la misma startDate.

La unidad funcional de cambio serÃ¡ la distribuciÃ³n vigente desde una fecha, no la ediciÃ³n arbitraria de una lÃ­nea individual.

DefiniciÃ³n funcional

employee.cost_center representa la distribuciÃ³n de imputaciÃ³n organizativa de un empleado entre uno o varios centros de coste, con vigencia temporal.

No modela simplemente â€œuna asignaciÃ³n mÃ¡sâ€, sino el reparto funcional del empleado entre centros de coste para un periodo dado.

Ejemplos vÃ¡lidos:

100% CC_FINANCE
60% CC_IT + 40% CC_SHARED
50% CC_OPS + 50% CC_TRANSFORMATION

Ejemplos invÃ¡lidos:

80% CC_A + 30% CC_B
50% CC_A desde 01/04 y 50% CC_B desde 15/04, coexistiendo en la misma vigencia
una lÃ­nea fuera de presence activa
porcentajes 0 o negativos
Tipo de vertical

ClasificaciÃ³n formal:

bounded context: employee
vertical: cost_center
tipo: DISTRIBUTED_TIMELINE
catÃ¡logo: SIMPLE
reglas temporales:
MULTI_ACTIVE
SUM_PERCENTAGE_LTE_100
CONTAINED_IN_PRESENCE
PARALLEL_WINDOW_SAME_START_DATE

Esto consolida la vertical en el cluster distribuido del proyecto y evita tratarla como una variaciÃ³n accidental de work_center.

Identidad funcional
Identidad del empleado
ruleSystemCode
employeeTypeCode
employeeNumber
Identidad del recurso

A nivel de API pÃºblica de escritura, la unidad funcional relevante serÃ¡ una ventana de distribuciÃ³n identificada por:

empleado
startDate

Es decir, conceptualmente:

ruleSystemCode
employeeTypeCode
employeeNumber
startDate
Nota importante

Internamente, la persistencia puede seguir usando filas individuales con:

id tÃ©cnico
costCenterAssignmentNumber tÃ©cnico/funcional interno

pero esos identificadores no definen la identidad pÃºblica canÃ³nica del recurso.

La razÃ³n es funcional: cuando varias lÃ­neas comparten una misma distribuciÃ³n vigente, el dominio no las trata como historias autÃ³nomas, sino como partes de una Ãºnica ventana de distribuciÃ³n.

Ventana de distribuciÃ³n

Se introduce el concepto explÃ­cito de:

Cost Center Distribution Window

Una ventana de distribuciÃ³n es el conjunto de lÃ­neas de cost_center que:

pertenecen al mismo empleado;
comparten la misma startDate;
forman una distribuciÃ³n activa o histÃ³rica coherente;
y se validan conjuntamente como una Ãºnica unidad funcional.

Consecuencias:

crear una distribuciÃ³n = crear una ventana;
sustituir una distribuciÃ³n = cerrar ventana anterior y crear una nueva;
cerrar una distribuciÃ³n = cerrar todas las lÃ­neas de la ventana;
TERMINATION actÃºa sobre la ventana activa completa, no sobre una lÃ­nea suelta.
Propiedades estructurales
Propiedad	Valor
historized	true
occurrence_type	MULTIPLE
simultaneous_occurrences	MULTIPLE_ACTIVE
lifecycle_strategy	CLOSE
delete_policy	FORBIDDEN
maintenance_style	DISTRIBUTED_WINDOW
Campos funcionales de lÃ­nea

Cada lÃ­nea de distribuciÃ³n contiene:

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
ValidaciÃ³n de catÃ¡logo

costCenterCode debe validarse contra rulesystem.rule_entity usando:

ruleEntityTypeCode = COST_CENTER

Esto se alinea con la convenciÃ³n de naming de catÃ¡logos reutilizables y con el binding ya previsto para:

employee.cost_center / costCenterCode -> COST_CENTER (DIRECT)

La validaciÃ³n debe comprobar:

ruleSystemCode correcto
existencia del cÃ³digo
activo
vigencia temporal aplicable
Reglas de dominio
1. ContenciÃ³n en presence

Toda lÃ­nea de cost_center debe estar completamente contenida en una presence vÃ¡lida del empleado.

No se permiten lÃ­neas:

antes del inicio de la presence que las contiene;
despuÃ©s del final de la presence;
ni abiertas mÃ¡s allÃ¡ de una presence cerrada.
2. Multi-actividad permitida

Puede haber mÃºltiples lÃ­neas activas simultÃ¡neamente para una misma fecha, siempre que pertenezcan a la misma ventana de distribuciÃ³n.

3. Suma mÃ¡xima de porcentaje

Para cualquier fecha dada, la suma de allocationPercentage de todas las lÃ­neas activas del empleado no puede superar 100.

Regla:

permitido: total < 100
permitido: total = 100
prohibido: total > 100
4. Misma fecha de inicio en paralelo

Si existe mÃ¡s de una lÃ­nea activa simultÃ¡neamente dentro de una misma distribuciÃ³n, todas deben compartir exactamente la misma startDate.

Ejemplo vÃ¡lido:

CC_A 50% desde 2026-04-01
CC_B 50% desde 2026-04-01

Ejemplo invÃ¡lido:

CC_A 50% desde 2026-04-01
CC_B 50% desde 2026-04-15
5. No mezcla incoherente de ventanas activas

No puede coexistir, para una misma fecha, una lÃ­nea activa perteneciente a una ventana de distribuciÃ³n distinta.

Dicho de otro modo: para un empleado, la distribuciÃ³n activa en una fecha debe ser interpretable como una Ãºnica ventana funcional.

6. Porcentaje vÃ¡lido por lÃ­nea

allocationPercentage debe cumplir:

> 0
<= 100
7. Integridad temporal

endDate no puede ser anterior a startDate.

8. Sin ediciÃ³n arbitraria de identidad

No se permite mutar, en una correcciÃ³n administrativa, los campos que redefinen funcionalmente una lÃ­nea histÃ³rica de forma que rompan la semÃ¡ntica de la ventana.

Operaciones canÃ³nicas

No se adopta un CRUD plano por fila.

Las operaciones canÃ³nicas del vertical serÃ¡n orientadas a ventana.

1. Crear distribuciÃ³n

Crea una nueva ventana de distribuciÃ³n desde una fecha.

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/distributions

Request
startDate
items[]
costCenterCode
allocationPercentage
2. Consultar histÃ³rico de distribuciones

GET /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers

3. Consultar distribuciÃ³n vigente

GET /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/current

4. Sustituir distribuciÃ³n desde fecha

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/replace-from-date

Request
effectiveDate
items[]
costCenterCode
allocationPercentage

SemÃ¡ntica:

cerrar la ventana activa previa si cubre la fecha;
crear nueva ventana desde effectiveDate;
validar projected timeline distribuida.
5. Cerrar distribuciÃ³n

POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers/distributions/{startDate}/close

Request
endDate

SemÃ¡ntica:

cerrar todas las lÃ­neas de la ventana identificada por startDate.
Operaciones descartadas

Se rechazan como canÃ³nicas:

PUT por lÃ­nea aislada
DELETE fÃ­sico por lÃ­nea
ediciÃ³n arbitraria de una fila dentro de una distribuciÃ³n multi-lÃ­nea activa
endpoints pÃºblicos por id
endpoints pÃºblicos por costCenterAssignmentNumber

porque esas operaciones empujan el modelo hacia una semÃ¡ntica de filas independientes que no representa bien el dominio.

RelaciÃ³n con replace patterns

employee.cost_center puede necesitar una operaciÃ³n funcional tipo replaceFromDate, pero no debe reutilizar directamente el StrongTimelineReplacePlanner, ya que este patrÃ³n estÃ¡ reservado a verticales STRONG_TIMELINE con:

single active
no overlap
full coverage

y cost_center es explÃ­citamente un caso distinto.

Si aparece lÃ³gica temporal repetida, podrÃ¡ introducirse en el futuro un helper ligero especÃ­fico para distribuciones, por ejemplo:

CostCenterDistributionProjector
DistributedTimelineWindowPlanner

pero no debe crearse todavÃ­a un framework genÃ©rico ni un motor abstracto.

RelaciÃ³n con TERMINATION

TERMINATION debe cerrar la distribuciÃ³n activa completa del empleado.

Regla funcional:

al terminar un empleado en terminationDate,
se identifican todas las lÃ­neas activas de cost_center en esa fecha,
y todas deben quedar cerradas con esa fecha de fin.

Consecuencias:

no puede quedar ninguna lÃ­nea abierta despuÃ©s de la terminaciÃ³n;
no se redistribuyen porcentajes;
no se corrigen lÃ­neas;
no se fuerza una suma distinta;
simplemente se cierra la ventana activa.

Si no existe distribuciÃ³n activa en la fecha de terminaciÃ³n, no se considera error funcional por sÃ­ mismo.

Esto encaja con la semÃ¡ntica general de lifecycle workflows y con la necesidad de cerrar asignaciones vigentes sin dejar residuos abiertos.

RelaciÃ³n con Journey

En Journey / timeline, un cambio de cost_center debe interpretarse como evento funcional por ventana de distribuciÃ³n, no como cascada de eventos aislados por cada fila tÃ©cnica.

Evento esperado:

COST_CENTER_CHANGE

La interpretaciÃ³n debe hacerse en backend y no delegarse al frontend.

Persistencia

Persistencia recomendada: tabla por lÃ­neas.

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
Restricciones mÃ­nimas de base de datos
PK tÃ©cnica por id
unique (employee_id, cost_center_assignment_number)
check allocation_percentage > 0 and allocation_percentage <= 100
check end_date is null or start_date < end_date
Nota

Las reglas:

suma <= 100
misma startDate en multi-activo
una Ãºnica ventana funcional activa por fecha

deben validarse en dominio / servicio de aplicaciÃ³n, no intentarse imponer Ãºnicamente con constraints SQL.

API y OpenAPI

La API pÃºblica debe seguir las reglas generales del proyecto:

business keys del empleado
sin IDs tÃ©cnicos
sin mezclar parent por business key e hijo por id tÃ©cnico

La OpenAPI debe reflejar:

operaciones por ventana
requests con items[]
DTOs claros y honestos
sin introducir update DTOs que permitan mutar identidad funcional como si fuera CRUD genÃ©rico.
Read models recomendados

La lectura debe exponer labels enriquecidas:

costCenterCode
costCenterName

y agrupar claramente la ventana actual e histÃ³rico.

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
        "costCenterName": "AdministraciÃ³n",
        "allocationPercentage": 50
      },
      {
        "costCenterCode": "CC_B",
        "costCenterName": "TransformaciÃ³n",
        "allocationPercentage": 50
      }
    ]
  }
}

Esto se alinea con la estrategia general del proyecto de enriquecer lectura con code + name y no obligar al frontend a reconstruir semÃ¡ntica desde catÃ¡logos masivos.

Frontend

Esta vertical no debe tratarse como:

SLOT
ni tabla CRUD genÃ©rica

La semÃ¡ntica de UI recomendada es una secciÃ³n temporal/distribuida con:

distribuciÃ³n actual destacada
histÃ³rico de distribuciones
acciones honestas:
AÃ±adir distribuciÃ³n
Sustituir distribuciÃ³n desde fecha
Cerrar distribuciÃ³n

No deben usarse como acciones primarias:

editar fila
borrar fila
update tÃ©cnico
grid CRUD

Esto es coherente con el principio general de UX honesta y con la prohibiciÃ³n de usar â€œEditarâ€ como verbo universal cuando la semÃ¡ntica real es otra.

Consecuencias positivas
mejor alineaciÃ³n con el dominio real;
evita modelado accidental por filas;
simplifica TERMINATION;
facilita Journey semÃ¡ntico;
hace mÃ¡s clara la UI;
prepara futuras abstracciones de distributed timeline;
mantiene consistencia con el mapa y lenguaje ya definidos en B4RRHH.
Costes / riesgos
requiere lÃ³gica de validaciÃ³n agregada, no trivial;
introduce una nociÃ³n nueva de ventana funcional;
obliga a resistir la tentaciÃ³n de implementar CRUD simple por lÃ­nea;
puede requerir helper tÃ©cnico especÃ­fico en el futuro si aparecen mÃ¡s verticales distribuidas;
la correcciÃ³n administrativa de histÃ³rico deberÃ¡ definirse con cuidado si algÃºn dÃ­a se habilita.
Alternativas consideradas
1. Modelarlo como clon de work_center

Descartado.

work_center es una asignaciÃ³n flexible historizada, pero cost_center tiene una semÃ¡ntica distribuida basada en porcentaje y paralelismo.

2. Modelarlo como CRUD por fila

Descartado.

Rompe la unidad funcional de distribuciÃ³n, genera estados incoherentes y hace mÃ¡s difÃ­cil validar suma, ventanas y termination.

3. Tratarlo como STRONG_TIMELINE

Descartado.

No es single-active y el planner de strong timeline no aplica directamente.

Resumen

employee.cost_center se define en B4RRHH como una vertical DISTRIBUTED_TIMELINE que modela la distribuciÃ³n temporal del empleado entre uno o varios centros de coste.

La unidad funcional de mantenimiento no serÃ¡ una fila aislada, sino una ventana de distribuciÃ³n identificada por empleado + startDate.

Reglas clave:

multi-activo permitido;
suma activa <= 100;
contenciÃ³n en presence;
lÃ­neas paralelas con la misma startDate;
cierre completo en TERMINATION;
catÃ¡logo COST_CENTER;
operaciones canÃ³nicas orientadas a crear, sustituir y cerrar distribuciones

<!-- END FILE: ADR-017-Cost-center-design.md -->


---

# FILE: ADR-018-hiring-an-employee.md
<a name="file-adr-018-hiring-an-employee-md"></a>

<!-- BEGIN FILE: ADR-018-hiring-an-employee.md -->

ADR â€” Employee Lifecycle Workflow: Hire Employee V1
Estado

Propuesto

Contexto

B4RRHH modela el empleado mediante una arquitectura basada en verticales independientes (presence, work_center, cost_center, contract, etc.), todas ellas accesibles mediante APIs por business key:

ruleSystemCode
employeeTypeCode
employeeNumber

Sin embargo, el ciclo de vida del empleado no se corresponde con la manipulaciÃ³n aislada de estas verticales, sino con acciones de negocio compuestas como:

contratar
terminar
recontratar

Estas acciones implican:

creaciÃ³n coordinada de mÃºltiples verticales
coherencia temporal
validaciones transversales
una semÃ¡ntica funcional Ãºnica

El ADR de lifecycle ya establece que estas acciones deben modelarse como workflows de negocio, no como secuencias de operaciones CRUD.

AdemÃ¡s, la arquitectura de frontend define que estas acciones deben exponerse como WORKFLOW, no como ediciÃ³n genÃ©rica de datos.

Problema

Actualmente el sistema permite:

crear employee
crear presence
crear asignaciones organizativas
etc.

pero no existe una operaciÃ³n unificada de contrataciÃ³n.

Esto implica:

mala UX (el usuario tiene que ensamblar el empleado manualmente)
riesgo de inconsistencias temporales
pÃ©rdida de semÃ¡ntica de negocio
dificultad para evolucionar el lifecycle
DecisiÃ³n

Se introduce el workflow:

Hire Employee V1

como una operaciÃ³n de negocio compuesta que:

crea el empleado
inicializa su relaciÃ³n laboral
establece su contexto organizativo inicial
garantiza coherencia temporal completa

Todo ello en una Ãºnica operaciÃ³n orquestada.

Principios de diseÃ±o
1. Orientado a intenciÃ³n de negocio

El usuario no crea recursos tÃ©cnicos.

El usuario ejecuta:

â€œContratar empleadoâ€

2. Presence como eje del lifecycle

El lifecycle del empleado se representa mediante presence.

Por tanto:

el Hire crea la primera presence
sin presence no hay relaciÃ³n laboral
3. Fecha central Ãºnica

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

4. OrquestaciÃ³n Ãºnica

El workflow:

ejecuta mÃºltiples operaciones internas
se expone como una Ãºnica operaciÃ³n externa
garantiza consistencia funcional
5. Sin exposiciÃ³n de IDs tÃ©cnicos

La API:

usa exclusivamente business keys
no expone IDs internos
no mezcla identidades tÃ©cnicas
6. Backend interpreta la semÃ¡ntica

El backend:

decide quÃ© significa â€œHIREâ€
construye el estado resultante
prepara los datos para UI

El frontend no deduce semÃ¡ntica compleja.

Alcance V1
Incluido

El workflow crea:

Employee core
Primera presence
AsignaciÃ³n organizativa inicial:
work center
cost center (opcional en V1)
RelaciÃ³n laboral inicial:
contract
labor classification
No incluido en V1
contactos
direcciones
identificadores
correcciones avanzadas
escenarios multi-fecha
ediciÃ³n parcial del workflow
API
Endpoint

OpciÃ³n recomendada:

POST /employee-lifecycle/hire

Alternativa vÃ¡lida:

POST /employees/hire
Request

Ejemplo:

{
  "ruleSystemCode": "ESP",
  "employeeTypeCode": "EMP",
  "employeeNumber": "000123",

  "firstName": "Juan",
  "lastName1": "PÃ©rez",
  "lastName2": "GarcÃ­a",
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
    "displayName": "Juan PÃ©rez GarcÃ­a",
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
si existe â†’ 409 Conflict
2. CatÃ¡logos

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
catÃ¡logo vÃ¡lido
contenido en presence
6. Coherencia temporal
todas las entidades deben respetar hireDate
no se permiten offsets en V1
OrquestaciÃ³n interna

Orden recomendado:

validar request
validar catÃ¡logos
validar dependencias
crear employee
crear presence
crear work center
crear cost center (si viene)
crear contract
crear labor classification
construir response

Todo dentro de un Ãºnico servicio de aplicaciÃ³n.

RelaciÃ³n con TERMINATION

Este workflow deja al empleado en estado:

presence activa
asignaciones activas

Lo que permite que:

TERMINATION cierre correctamente todas las verticales activas

Sin estados intermedios incoherentes.

RelaciÃ³n con REHIRE

Diferencias clave:

Aspecto	Hire	Rehire
Employee	se crea	ya existe
Presence	primera	nueva
HistÃ³rico	vacÃ­o	preservado
RelaciÃ³n con Journey

El Hire debe generar un evento:

HIRE

El backend es responsable de esta interpretaciÃ³n.

El frontend no debe inferirlo.

Frontend

El workflow:

se expone como acciÃ³n principal: â€œContratarâ€
se implementa como pantalla dedicada
no como modal simple
no como formulario genÃ©rico

PatrÃ³n:

WORKFLOW
no SLOT
no TEMPORAL_APPEND_CLOSE
Consecuencias positivas
UX alineada con negocio
consistencia temporal garantizada
reducciÃ³n de errores
base sÃ³lida para lifecycle completo
integraciÃ³n natural con Journey
Costes
mayor complejidad en capa application
necesidad de validaciones transversales
mayor esfuerzo inicial
Alternativas descartadas
CRUD por vertical

Descartado:

no refleja dominio
propenso a inconsistencias
Employee como agregado monolÃ­tico

Descartado:

rompe arquitectura vertical
reduce flexibilidad
Workflow parcial

Descartado:

deja estados intermedios invÃ¡lidos
Resumen

Hire Employee V1 introduce una operaciÃ³n de negocio compuesta que:

crea el empleado
establece su relaciÃ³n laboral
define su contexto organizativo inicial
garantiza coherencia temporal

Todo ello en una Ãºnica operaciÃ³n orquestada, alineada con el modelo de verticales y con el ciclo de vida real del empleado.

<!-- END FILE: ADR-018-hiring-an-employee.md -->


---

# FILE: ADR-019-employee-delete-administrativo.md
<a name="file-adr-019-employee-delete-administrativo-md"></a>

<!-- BEGIN FILE: ADR-019-employee-delete-administrativo.md -->

# ADR-019 â€” Borrado administrativo de employee con cascada tÃ©cnica controlada

## Estado
Propuesto

## Contexto

B4RRHH modela el dominio de empleado mediante:

- identidad pÃºblica por business key:
  - ruleSystemCode
  - employeeTypeCode
  - employeeNumber
- verticales hijas funcionales del empleado
- lifecycle ordinario basado en:
  - hire
  - terminate
  - rehire

AdemÃ¡s, el proyecto distingue entre:

- operaciones funcionales normales del ciclo de vida
- operaciones administrativas o tÃ©cnicas excepcionales

Hasta ahora, el modelo conceptual del recurso `employee.employee` no se ha orientado al borrado como operaciÃ³n canÃ³nica de negocio, sino a conservaciÃ³n de identidad e histÃ³rico. Sin embargo, existen escenarios legÃ­timos donde un borrado administrativo sÃ­ tiene sentido, por ejemplo:

- alta creada por error
- empleado que finalmente no llega a incorporarse
- datos de prueba o limpieza controlada de entornos no productivos
- reversiÃ³n temprana de una contrataciÃ³n todavÃ­a sin efectos descendentes relevantes

Al mismo tiempo, no se quiere permitir un borrado indiscriminado ni delegar toda la semÃ¡ntica del delete a la base de datos.

Se necesita una decisiÃ³n explÃ­cita sobre:

- existencia o no de endpoint de borrado
- naturaleza funcional de ese borrado
- relaciÃ³n entre validaciÃ³n de aplicaciÃ³n y cascada fÃ­sica en persistencia
- preparaciÃ³n del modelo para futuras restricciones de elegibilidad

## DecisiÃ³n

Se introduce una operaciÃ³n explÃ­cita de **borrado administrativo de employee**.

### Naturaleza de la operaciÃ³n

El borrado de employee:

- **no forma parte del lifecycle ordinario**
- **no sustituye a terminate**
- **no representa un flujo funcional normal**
- se modela como una **operaciÃ³n administrativa excepcional**

### Identidad del endpoint

La operaciÃ³n se expone por business key del empleado:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`

### Regla inicial de elegibilidad (V1)

En esta primera versiÃ³n, el borrado se permitirÃ¡ Ãºnicamente cuando:

- el empleado exista

Si el empleado no existe:

- la operaciÃ³n devolverÃ¡ `404 Not Found`

### Reglas futuras explÃ­citamente previstas

Aunque en V1 no se aplican bloqueos funcionales adicionales, esta operaciÃ³n queda diseÃ±ada para soportar en el futuro validaciones como:

- no permitir borrado si el empleado tiene nÃ³mina calculada
- no permitir borrado si existen efectos descendentes relevantes
- no permitir borrado si el empleado ya superÃ³ cierto punto funcional del ciclo de vida
- otras reglas de elegibilidad administrativa

Si en el futuro una regla impide el borrado:

- la operaciÃ³n deberÃ¡ devolver `409 Conflict`

## Persistencia

Cuando el borrado sea autorizado por la capa de aplicaciÃ³n, la eliminaciÃ³n fÃ­sica del empleado podrÃ¡ apoyarse en **cascada tÃ©cnica de base de datos** sobre las verticales hijas dependientes por `employee_id`.

Principio:

- la **aplicaciÃ³n decide si se puede borrar**
- la **base de datos ejecuta el borrado relacional completo**

La cascada en base de datos se considera una decisiÃ³n de persistencia y consistencia tÃ©cnica, no una definiciÃ³n de semÃ¡ntica de negocio.

## API propuesta

```text
DELETE /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}

<!-- END FILE: ADR-019-employee-delete-administrativo.md -->


---

# FILE: ADR-020-work-center-replace-from-date.md
<a name="file-adr-020-work-center-replace-from-date-md"></a>

<!-- BEGIN FILE: ADR-020-work-center-replace-from-date.md -->

# ADR-020 â€” Cambio canÃ³nico de work center mediante replace-from-date

## Estado
Propuesto

## Contexto

B4RRHH modela sus recursos de empleado mediante verticales funcionales historizadas y APIs pÃºblicas basadas en business keys del empleado:

- ruleSystemCode
- employeeTypeCode
- employeeNumber

Dentro del mapa actual de verticales, `employee.work_center` se clasifica como una vertical temporal con restricciones de:

- no solape
- contenciÃ³n dentro de presence
- una Ãºnica asignaciÃ³n vigente compatible en cada fecha

Aunque inicialmente puede existir una operaciÃ³n canÃ³nica de creaciÃ³n de work center, la experiencia real de uso ha demostrado que el cambio funcional habitual de centro de trabajo no puede modelarse de forma segura como un simple `create` aislado cuando ya existe una asignaciÃ³n abierta.

En un escenario real, si un empleado ya tiene un work center vigente y se desea cambiarlo con fecha efectiva X:

- no puede abrirse uno nuevo en X dejando el anterior abierto
- el anterior debe cerrarse en X - 1
- el nuevo debe comenzar en X

Por tanto, la operaciÃ³n funcional real no es â€œaÃ±adir otra filaâ€, sino **sustituir la ventana vigente desde una fecha**.

Esta necesidad se ha hecho visible especialmente al ejecutar simulaciÃ³n masiva con `workforce_loader`, donde la operaciÃ³n de creaciÃ³n directa genera conflictos funcionales que no aparecÃ­an en pruebas pequeÃ±as.

## Problema

Usar Ãºnicamente una operaciÃ³n de creaciÃ³n para representar un cambio de work center provoca varios riesgos:

- solapes temporales
- necesidad de que el consumidor implemente lÃ³gica de cierre previa
- duplicaciÃ³n de semÃ¡ntica de dominio fuera del backend
- inconsistencias entre consumidores (frontend, loader, workflows)

Esto es contrario a la estrategia del proyecto, donde:

- el backend debe exponer operaciones canÃ³nicas de negocio
- el consumidor no debe reconstruir reglas temporales complejas por su cuenta

## DecisiÃ³n

Se introduce una operaciÃ³n canÃ³nica para `employee.work_center` orientada a cambio funcional por fecha efectiva:

## `replace-from-date`

Su semÃ¡ntica serÃ¡:

1. localizar la asignaciÃ³n de work center vigente en la fecha efectiva, si existe;
2. cerrarla en `effectiveDate - 1`;
3. crear la nueva asignaciÃ³n desde `effectiveDate`;
4. validar no solape, contenciÃ³n en presence y coherencia temporal completa.

## Naturaleza de la operaciÃ³n

`replace-from-date`:

- no es un CRUD genÃ©rico
- no sustituye a la operaciÃ³n de creaciÃ³n inicial
- representa el cambio funcional habitual de centro de trabajo

## API propuesta

```text
POST /employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/work-centers/replace-from-date
```

Request mÃ­nima orientativa
{
  "effectiveDate": "2026-04-01",
  "workCenterCode": "WC02"
}
Reglas de negocio
1. ContenciÃ³n en presence

La nueva asignaciÃ³n debe estar contenida dentro de una presence vÃ¡lida del empleado.

2. No solape

No puede quedar mÃ¡s de una asignaciÃ³n de work center incompatible en la misma fecha.

3. Cierre implÃ­cito de la vigente

Si existe una asignaciÃ³n vigente en effectiveDate, debe cerrarse en effectiveDate - 1.

4. CreaciÃ³n de nueva asignaciÃ³n

La nueva asignaciÃ³n comienza en effectiveDate.

5. OperaciÃ³n idempotente semÃ¡ntica no requerida

No se exige idempotencia funcional estricta en V1, pero sÃ­ una validaciÃ³n clara de conflictos.

Consecuencias
Positivas
expresa la semÃ¡ntica real del cambio de centro
evita que frontend o loader implementen lÃ³gica temporal propia
mantiene la coherencia con otras operaciones temporales del sistema
facilita simulaciÃ³n masiva y workflows futuros
Negativas
aÃ±ade una operaciÃ³n especÃ­fica mÃ¡s al vertical
obliga a definir claramente el comportamiento cuando no existe work center vigente
DecisiÃ³n de alcance para V1

En V1:

si existe work center vigente en la fecha efectiva, se cierra y se crea el nuevo
si no existe vigente, la operaciÃ³n podrÃ¡ crear directamente la nueva asignaciÃ³n siempre que el contexto temporal sea vÃ¡lido
no se introducen todavÃ­a estrategias avanzadas de correcciÃ³n administrativa
RelaciÃ³n con otras verticales

Esta decisiÃ³n acerca work_center a un patrÃ³n de sustituciÃ³n temporal por fecha efectiva, aunque sin convertirlo automÃ¡ticamente en STRONG_TIMELINE.

No se afirma que work_center y contract sean idÃ©nticos como verticales, pero sÃ­ que ambos requieren una operaciÃ³n canÃ³nica de sustituciÃ³n temporal cuando el cambio funcional afecta a una asignaciÃ³n vigente.


<!-- END FILE: ADR-020-work-center-replace-from-date.md -->


---

# FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md
<a name="file-adr-021-company-como-catalogo-enriquecido-y-anclado-a-rule-entity-md"></a>

<!-- BEGIN FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md -->

ADR â€” COMPANY como catÃ¡logo reutilizable enriquecido mediante profile y anclado tÃ©cnicamente a rule_entity
Estado

Propuesto

Contexto

B4RRHH dispone de un metamodelo funcional en el bounded context rulesystem basado en:

rule_system
rule_entity_type
rule_entity

Este metamodelo ya se utiliza como base de validaciÃ³n y parametrizaciÃ³n de mÃºltiples verticales del sistema.

AdemÃ¡s, el proyecto ya ha fijado varias decisiones relevantes:

1. Los conceptos reutilizables deben nombrarse por su significado funcional real

Se ha decidido que un rule_entity_type debe nombrar el concepto funcional real y no la primera vertical donde apareciÃ³. En ese marco, COMPANY es un ejemplo explÃ­cito de catÃ¡logo reutilizable de dominio, junto con WORK_CENTER, COST_CENTER o COUNTRY.

2. rule_entity no debe tratarse como un CRUD plano

El mantenimiento de rule_entity ya se ha definido como catÃ¡logo con vigencia temporal ligera, con identidad funcional basada en:

ruleSystemCode
ruleEntityTypeCode
code
startDate

y con operaciones canÃ³nicas de create, get by business key, correct, close y delete restringido.

3. Las APIs pÃºblicas del proyecto deben usar business keys, mientras que los IDs tÃ©cnicos quedan encapsulados en persistencia

Esta regla ya estÃ¡ consolidada en el proyecto y se aplica de forma clara en el modelo de empleado: la identidad pÃºblica es funcional, mientras que la persistencia usa claves tÃ©cnicas para FKs, joins y wiring interno.

4. En el dominio de empleado, el patrÃ³n canÃ³nico distingue entre identidad pÃºblica y persistencia tÃ©cnica

Por ejemplo, employee.contact se identifica pÃºblicamente por employee + contactTypeCode, mientras que internamente la tabla usa id tÃ©cnico y FK a employee.employee.id. Ese id tÃ©cnico no define la identidad funcional del recurso, pero sÃ­ su anclaje persistente.

Problema

COMPANY nace correctamente como un catÃ¡logo reutilizable del metamodelo. Sin embargo, al evolucionar el producto aparece una necesidad real: una empresa no solo necesita:

cÃ³digo
literal visible
vigencia

sino tambiÃ©n una ficha ampliada con datos ricos, por ejemplo:

nombre legal
identificador fiscal
direcciÃ³n

Esto genera una tensiÃ³n de diseÃ±o.

Si COMPANY se mantiene exclusivamente como rule_entity

El modelo queda demasiado pobre para soportar informaciÃ³n empresarial bÃ¡sica.

Si COMPANY se promociona inmediatamente a una nueva vertical/autonomÃ­a completa

Se corre el riesgo de introducir complejidad prematura y de abrir una familia entera de subdominios (organization.company, organization.work_center, organization.cost_center, etc.) antes de que exista una necesidad operativa clara.

Si se modela la ampliaciÃ³n rica solo con business keys y sin anclaje tÃ©cnico interno

Se introducirÃ­a una excepciÃ³n innecesaria respecto a la filosofÃ­a ya consolidada en el proyecto, que separa:

identidad pÃºblica funcional
identidad interna/persistente tÃ©cnica

El sistema necesita una soluciÃ³n intermedia, evolutiva y coherente con las decisiones ya tomadas.

DecisiÃ³n

Se adopta para COMPANY el siguiente modelo:

1. COMPANY seguirÃ¡ siendo un catÃ¡logo reutilizable del metamodelo

COMPANY se mantiene como rule_entity_type reutilizable y sus ocurrencias continÃºan viviendo en rulesystem.rule_entity.

Su responsabilidad sigue siendo:

identidad catalogal funcional
cÃ³digo reutilizable
label visible
vigencia
activaciÃ³n

Esto preserva el papel de COMPANY como concepto reusable en mÃºltiples verticales y workflows.

2. La ficha ampliada de empresa no se modelarÃ¡ dentro de rule_entity

Los datos ricos de empresa no se introducirÃ¡n como extensiÃ³n ad hoc de rule_entity.

Se crea un recurso complementario especÃ­fico para la ampliaciÃ³n rica de la empresa.

Nombre conceptual adoptado:

company_profile

Su responsabilidad es representar la ficha ampliada de una empresa sin alterar la naturaleza catalogal base de rule_entity.

3. company_profile se anclarÃ¡ tÃ©cnicamente a rule_entity.id

La relaciÃ³n interna se resuelve mediante FK tÃ©cnica a la ocurrencia base de rule_entity de tipo COMPANY.

Es decir:

la identidad pÃºblica seguirÃ¡ usando business keys
la persistencia interna usarÃ¡ un anclaje tÃ©cnico estable
Regla adoptada

company_profile referencia internamente a la empresa base mediante:

company_rule_entity_id â†’ FK a rulesystem.rule_entity.id

Esto sigue la misma filosofÃ­a ya utilizada en el modelo de empleado:

business key fuera
FK tÃ©cnica dentro
4. Alcance funcional V1 de company_profile

Para la primera iteraciÃ³n, company_profile solo cubrirÃ¡:

legalName
taxIdentifier
direcciÃ³n

La direcciÃ³n podrÃ¡ modelarse inicialmente como campos simples embebidos en el profile.

No se incluyen todavÃ­a en V1:

numeraciÃ³n de empleados
telÃ©fonos
emails
contactos por tipo
polÃ­ticas avanzadas
subverticales de company
5. TelÃ©fono y email quedan explÃ­citamente fuera de V1

Aunque podrÃ­an modelarse como columnas simples, se decide no hacerlo en esta fase.

JustificaciÃ³n:

el proyecto ya ha consolidado en employee.contact un patrÃ³n semÃ¡ntico claro para canales de contacto: slot por tipo, validaciÃ³n por catÃ¡logo y separaciÃ³n respecto a la ficha base del sujeto.
introducir phone y email como dos campos planos en company_profile serÃ­a una simplificaciÃ³n aceptable a muy corto plazo, pero introducirÃ­a una asimetrÃ­a conceptual innecesaria.
se prefiere aplazar esta decisiÃ³n hasta que exista necesidad real de contacto empresarial, momento en el cual podrÃ¡ evaluarse si procede una soluciÃ³n equivalente a contactos por tipo.
6. No se crea todavÃ­a una nueva vertical/autonomÃ­a completa de organizaciÃ³n

Esta decisiÃ³n no introduce todavÃ­a:

bounded context organization
vertical completa organization.company
subverticales como organization.company.contact, organization.company.address, organization.company.numbering_policy

La decisiÃ³n actual se limita a:

mantener COMPANY como catÃ¡logo reutilizable
permitir una ampliaciÃ³n rica controlada mediante company_profile
DiseÃ±o funcional adoptado
Naturaleza de COMPANY

COMPANY pasa a entenderse como un concepto de dos capas:

A. Capa catalogal canÃ³nica

Representada por rulesystem.rule_entity

Responsabilidad:

identidad reusable
code
name
vigencia
activaciÃ³n
B. Capa de profile enriquecido

Representada por company_profile

Responsabilidad:

ficha ampliada
datos operativos bÃ¡sicos
evoluciÃ³n gradual sin contaminar el metamodelo
Identidad
Identidad pÃºblica de la empresa

La identidad pÃºblica funcional sigue siendo:

ruleSystemCode
companyCode

donde:

companyCode es el rule_entity.code
ruleEntityTypeCode = COMPANY
Identidad interna de persistencia

La persistencia interna se apoya en:

rulesystem.rule_entity.id como root tÃ©cnico base del concepto catalogal
company_profile.id como PK tÃ©cnica propia del profile
company_profile.company_rule_entity_id como FK tÃ©cnica Ãºnica hacia rule_entity.id
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
PK tÃ©cnica en company_profile.id
FK obligatoria:
company_rule_entity_id -> rulesystem.rule_entity.id
unique:
company_rule_entity_id
validaciÃ³n de que la rule_entity referenciada sea de tipo COMPANY
API pÃºblica
Principio general

Las APIs pÃºblicas siguen usando business keys, nunca IDs tÃ©cnicos. Esto mantiene coherencia con la convenciÃ³n general del proyecto.

API de catÃ¡logo base

Se mantiene el mantenimiento canÃ³nico de rule_entity ya definido:

POST /rule-entities
GET /rule-entities
GET /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
PUT /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
POST /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}/close
DELETE /rule-entities/{ruleSystemCode}/{ruleEntityTypeCode}/{code}/{startDate}
API de profile enriquecido

Se introduce una API especÃ­fica orientada a la ficha ampliada de empresa.

Endpoints recomendados:

GET /companies/{ruleSystemCode}/{companyCode}/profile
PUT /companies/{ruleSystemCode}/{companyCode}/profile

Opcionalmente, si compensa por ergonomÃ­a:

GET /companies/{ruleSystemCode}/{companyCode}

como endpoint agregado de lectura enriquecida.

Reglas de dominio
1. SeparaciÃ³n de responsabilidades
rule_entity define la identidad catalogal canÃ³nica
company_profile define la ampliaciÃ³n rica
2. No duplicar semÃ¡ntica de identidad

company_profile no define una nueva identidad pÃºblica de empresa.

3. No mezclar identidad pÃºblica y wiring interno
el exterior usa ruleSystemCode + companyCode
el interior usa FK tÃ©cnica a rule_entity.id
4. company_profile no reemplaza a rule_entity

No puede existir empresa operativamente vÃ¡lida sin su base catalogal correspondiente.

5. La vigencia canÃ³nica sigue residiendo en rule_entity

No se traslada a company_profile una lÃ³gica temporal propia en esta fase.

6. taxIdentifier podrÃ¡ evolucionar

En V1 se modela como dato simple, pero el diseÃ±o permite introducir mÃ¡s adelante validaciones especÃ­ficas por paÃ­s o regla sin romper la arquitectura.

RelaciÃ³n con el crecimiento de rule_system

Esta decisiÃ³n se considera importante porque establece una vÃ­a general para la evoluciÃ³n del metamodelo.

PatrÃ³n emergente

Un concepto del metamodelo puede recorrer estas fases:

Fase 1 â€” CatÃ¡logo puro

Solo requiere:

code
name
vigencia
Fase 2 â€” CatÃ¡logo + profile enriquecido

El concepto sigue siendo reusable, pero necesita ficha ampliada.

Fase 3 â€” Vertical/autonomÃ­a plena

Solo cuando ademÃ¡s aparecen:

operaciones canÃ³nicas propias
invariantes fuertes propias
UX especÃ­fica de mantenimiento
procesos donde el concepto es sujeto funcional

Esta progresiÃ³n evita dos errores:

dejar conceptos ricos empobrecidos en el catÃ¡logo
convertir demasiado pronto cualquier catÃ¡logo importante en un subdominio grande
Consecuencias positivas
mantiene a COMPANY como concepto reusable y estable del metamodelo
evita sobrecargar rule_entity con atributos ricos no propios de un catÃ¡logo
mantiene coherencia con la filosofÃ­a general del proyecto: business key fuera, surrogate key dentro
abre una senda de crecimiento sana para otros conceptos del rulesystem
permite enriquecer â€œdatos de empresaâ€ sin crear todavÃ­a una arquitectura organizativa prematura
se alinea con patrones ya conocidos en sistemas como HRAccess, donde el identificador tÃ©cnico del catÃ¡logo funciona como anclaje FK en estructuras derivadas
Costes / riesgos
introduce una nueva tabla y lÃ³gica de resoluciÃ³n adicional
obliga a mantener clara la frontera entre catÃ¡logo y profile
deja abierta una futura decisiÃ³n sobre contactos empresariales
puede requerir refactor si en el futuro COMPANY adquiere procesos y operaciones suficientes para convertirse en vertical plena
Alternativas consideradas
1. Mantener todo en rule_entity

Descartado.

Se queda corto para modelar datos empresariales bÃ¡sicos y empuja a usar el catÃ¡logo como contenedor genÃ©rico.

2. Crear ya organization.company como vertical plena

Descartado por prematuro.

No hay todavÃ­a suficientes operaciones, invariantes ni semÃ¡ntica propia para justificar esa promociÃ³n.

3. Modelar company_profile solo con business keys y sin FK tÃ©cnica

Descartado.

Rompe innecesariamente la filosofÃ­a ya consolidada en el proyecto respecto a la separaciÃ³n entre identidad pÃºblica y persistencia interna.

4. Meter phone/email como campos planos en V1

Aplazado.

Posible, pero no deseable mientras no se aclare la estrategia de contactos empresariales.

No objetivos

Este ADR no introduce todavÃ­a:

modelo de numeraciÃ³n de empleados
vertical de contactos de empresa
vertical de direcciones de empresa con historizaciÃ³n
bounded context organization
jerarquÃ­a organizativa
promociÃ³n de COMPANY a aggregate root autÃ³nomo
sincronizaciÃ³n automÃ¡tica compleja entre catÃ¡logo y profile mÃ¡s allÃ¡ de su relaciÃ³n estructural
Estrategia recomendada de implementaciÃ³n
Fase 1
mantener COMPANY en rule_entity
crear tabla company_profile
FK Ãºnica a rule_entity.id
exponer lectura y actualizaciÃ³n del profile
Fase 2
enriquecer frontend de â€œdatos de empresaâ€
mostrar lectura agregada catÃ¡logo + profile
introducir validaciones ligeras de taxIdentifier
Fase 3
evaluar contactos empresariales
evaluar si algunos conceptos de company merecen profile adicional o vertical propia
Fase 4
revisar si COMPANY sigue siendo â€œcatÃ¡logo + profileâ€ o si ya ha madurado hasta necesitar vertical/autonomÃ­a plena
Resumen ejecutivo

COMPANY seguirÃ¡ siendo en B4RRHH un catÃ¡logo reutilizable del metamodelo (rule_entity), porque su identidad canÃ³nica y su reutilizaciÃ³n transversal asÃ­ lo justifican.

Sin embargo, cuando la empresa necesite una ficha ampliada, esta no se modelarÃ¡ dentro de rule_entity, sino mediante un recurso complementario company_profile.

company_profile se anclarÃ¡ internamente mediante FK tÃ©cnica a rulesystem.rule_entity.id, preservando la misma filosofÃ­a que ya se usa en employee: business keys en la API pÃºblica, IDs tÃ©cnicos solo en persistencia.

La primera versiÃ³n del profile se limitarÃ¡ a:

nombre legal
identificador fiscal
direcciÃ³n

y dejarÃ¡ fuera, de momento, numeraciÃ³n y contactos empresariales.

Esta decisiÃ³n no crea todavÃ­a un nuevo universo organization.*, pero sÃ­ fija una vÃ­a muy importante para el crecimiento del rulesystem: catÃ¡logo reusable â†’ profile enriquecido â†’ posible vertical plena solo si el dominio lo exige.

<!-- END FILE: ADR-021-COMPANY-como-catalogo-enriquecido-y-anclado-a-rule_entity.md -->


---

# FILE: ADR-022-Global-message-and-feedback-policy.md
<a name="file-adr-022-global-message-and-feedback-policy-md"></a>

<!-- BEGIN FILE: ADR-022-Global-message-and-feedback-policy.md -->

# ADR-0XX â€” Global Message & Feedback Policy

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

* **global** â†’ not tied to any specific section
* **floating** â†’ overlays UI, does not affect layout
* **centralized** â†’ managed via a shared service
* **hierarchical** â†’ distinguishes between message types and scopes

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

> â€œIf something important happened, I look at the global message layer.â€

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

* limit visible messages (max 2â€“3)
* group or summarize if necessary

---

### Navigation

If a message is linked to a section:

* user can navigate via â€œGo to sectionâ€
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

* â€œYa existe un contacto para ese tipoâ€
* â€œInvalid working_time configurationâ€

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

# ADR-0XY â€” UI Interaction Contracts per Vertical

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

   * success â†’ publish global message
   * error â†’ publish global message
   * validation â†’ mark fields locally + optionally publish global
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

* â€œcontactValue invalidâ€ â†’ global + field highlight
* â€œduplicate contact typeâ€ â†’ global (not just local banner)

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

## Example â€” Contacts Vertical

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
* less â€œfreedomâ€ inside components

---

## Evolution

Future enhancements may include:

* standardized helper hooks for verticals
* base abstract component for interaction handling
* unified error mapping from backend â†’ UI
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

# ADR â€” Modelo de autorizaciÃ³n jerÃ¡rquica para recursos funcionales en B4RRHH

## Estado
Propuesto

## DecisiÃ³n principal
La autorizaciÃ³n en B4RRHH se modela como un dominio propio basado en:
- roles funcionales
- recursos funcionales jerÃ¡rquicos
- acciones semÃ¡nticas
- perfiles de permiso reutilizables
- polÃ­ticas rol-recurso con herencia y overrides

## Nombre recomendado
`authorization` (preferible a `security` para no mezclar autenticaciÃ³n con autorizaciÃ³n)

## Contexto
B4RRHH ya modela el dominio por verticales funcionales, business keys pÃºblicas y operaciones honestas. Falta cerrar la autorizaciÃ³n con el mismo rigor.

El problema no es solo â€œaÃ±adir rolesâ€, sino resolver:
- lectura global pero mantenimiento parcial por vertical
- workflows permitidos para unos roles y prohibidos para otros
- defaults razonables para recursos nuevos
- extensiÃ³n a `employee`, `rulesystem` y futuros bounded contexts

## Problema
Un modelo simple de roles por endpoint o por CRUD puro no encaja bien porque:
- la unidad natural del proyecto es el recurso funcional, no el endpoint
- el dominio usa acciones como `CLOSE`, `CORRECT`, `EXECUTE`
- habrÃ¡ mÃ¡s bounded contexts ademÃ¡s de `employee`
- no se quiere reconfigurar cada rol cada vez que nazca una vertical

## DecisiÃ³n
Se introduce un bounded context tÃ©cnico-funcional `authorization`.

La cadena lÃ³gica del modelo serÃ¡:

`rol -> polÃ­tica sobre recurso -> perfil de permiso -> acciones permitidas`

El recurso asegurado vive dentro de un Ã¡rbol jerÃ¡rquico. Ejemplo:

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
1. SeparaciÃ³n fuerte entre autenticaciÃ³n y autorizaciÃ³n.
2. Recurso funcional como unidad canÃ³nica de control.
3. Acciones semÃ¡nticas honestas.
4. Default + override.
5. Escalabilidad transversal.
6. No modelar autorizaciÃ³n por campo como regla general.

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
CatÃ¡logo jerÃ¡rquico de recursos protegidos.

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

`resource_family_code` es un agrupador funcional de recursos. **No es jerarquÃ­a** (eso lo modela `parent_resource_code`) â€” es agrupaciÃ³n semÃ¡ntica transversal.

**Para quÃ© sirve:**
- Simplificar autorizaciÃ³n: en vez de definir 50 reglas por recurso, se definen 5 reglas por familia.
- Evitar explosiÃ³n de polÃ­ticas: un rol puede autorizarse sobre una familia entera.
- Permitir reglas transversales: "RRHH ve todo lo de datos de empleado", "Finanzas solo lo econÃ³mico".

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
- El catÃ¡logo de familias es cerrado y se gobierna mediante ADR o enum en cÃ³digo.
- Las polÃ­ticas pueden definirse sobre familias en el futuro (extensiÃ³n de V1, no en V1).
- En V1 `resource_family_code` es campo informativo/filtro de UI; la evaluaciÃ³n jerÃ¡rquica no lo usa directamente.

### `authorization.action`
CatÃ¡logo de acciones.

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
Tabla de composiciÃ³n perfil -> acciÃ³n.

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
Solo necesaria si B4RRHH persiste roles internos. En V1 los roles del sujeto se extraen del JWT emitido por el IdP externo â€” B4RRHH no gestiona la asignaciÃ³n de roles, solo la lee del token.

Campos principales (futuros):
- `subject_code`
- `role_code`
- `assignment_origin`
- `active`

## Reglas de modelado
- Todo recurso nuevo que requiera autorizaciÃ³n debe registrarse en `authorization.secured_resource`.
- Todo recurso debe declarar, siempre que exista, un `parent_resource_code`.
- Todo recurso debe declarar su `resource_family_code`.
- Los workflows se modelan como recursos de tipo `WORKFLOW`.
- Las polÃ­ticas se definen sobre recursos, no sobre endpoints.
- La ausencia de permiso implica denegaciÃ³n.
- `NONE` es un perfil que no concede ninguna acciÃ³n. No es un deny con precedencia sobre otros roles â€” si otro rol del sujeto concede la acciÃ³n por otro camino del Ã¡rbol, la evaluaciÃ³n devuelve ALLOW. `NONE` solo deniega cuando es el Ãºnico perfil aplicable.
- En V1 no se introducen deny explÃ­citos con precedencia sobre grants de otros roles.
- La autorizaciÃ³n por campo queda fuera del modelo base.

## Algoritmo de evaluaciÃ³n
Entrada:
- sujeto autenticado
- roles efectivos
- `resource_code`
- `action_code`

ResoluciÃ³n:
1. Buscar polÃ­tica exacta para `role_code + resource_code`.
2. Si no existe, subir al padre.
3. Repetir hasta la raÃ­z.
4. Cuando se encuentre una polÃ­tica, resolver el perfil.
5. Comprobar si el perfil contiene la acciÃ³n.
6. Si algÃºn rol concede, permitir.
7. Si ninguno concede, denegar.

Reglas de precedencia:
- el recurso mÃ¡s cercano gana sobre ancestros mÃ¡s lejanos
- la coincidencia exacta gana sobre la heredada
- basta una concesiÃ³n positiva para permitir
- ausencia de concesiÃ³n = deny por defecto

## Ejemplos de polÃ­ticas

### AUDITOR
- `AUDITOR` sobre `employee` -> `READ_ONLY` con propagaciÃ³n a hijos
- `AUDITOR` sobre `rulesystem` -> `READ_ONLY` con propagaciÃ³n a hijos

### HR_OPERATOR
- `HR_OPERATOR` sobre `employee` -> `READ_ONLY` con propagaciÃ³n a hijos
- `HR_OPERATOR` sobre `employee.contact` -> `SLOT_MAINTAINER`
- `HR_OPERATOR` sobre `employee.identifier` -> `SLOT_MAINTAINER`
- `HR_OPERATOR` sobre `employee.address` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.work_center` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.working_time` -> `TEMPORAL_MAINTAINER`
- `HR_OPERATOR` sobre `employee.lifecycle.hire` -> `WORKFLOW_EXECUTOR`
- `HR_OPERATOR` sobre `employee.lifecycle.terminate` -> `NONE`
- `HR_OPERATOR` sobre `employee.lifecycle.rehire` -> `NONE`

### HR_MANAGER
- `HR_MANAGER` sobre `employee` -> `READ_ONLY` con propagaciÃ³n a hijos
- `HR_MANAGER` sobre `employee.contact` -> `SLOT_MAINTAINER`
- `HR_MANAGER` sobre `employee.identifier` -> `SLOT_MAINTAINER`
- `HR_MANAGER` sobre `employee.address` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.work_center` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.working_time` -> `TEMPORAL_MAINTAINER`
- `HR_MANAGER` sobre `employee.lifecycle` -> `WORKFLOW_EXECUTOR` con propagaciÃ³n a hijos

### CATALOG_MANAGER
- `CATALOG_MANAGER` sobre `rulesystem.rule_entity` -> `FULL_CONTROL`
- `CATALOG_MANAGER` sobre `rulesystem.rule_entity_type` -> `READ_ONLY`
- `CATALOG_MANAGER` sobre `rulesystem.rule_system` -> `READ_ONLY`

### ADMIN
- `ADMIN` sobre `employee` -> `FULL_CONTROL` con propagaciÃ³n a hijos
- `ADMIN` sobre `rulesystem` -> `FULL_CONTROL` con propagaciÃ³n a hijos
- `ADMIN` sobre `authorization` -> `FULL_CONTROL` con propagaciÃ³n a hijos

## Ejemplo completo de resoluciÃ³n
Caso: `HR_OPERATOR` intenta ejecutar `employee.lifecycle.terminate` con acciÃ³n `EXECUTE`.

ResoluciÃ³n:
1. Existe polÃ­tica exacta sobre `employee.lifecycle.terminate`.
2. El perfil es `NONE`.
3. `NONE` no contiene `EXECUTE`.
4. Resultado: denegado.

Caso: `HR_MANAGER` intenta ejecutar `employee.lifecycle.terminate` con acciÃ³n `EXECUTE`.

ResoluciÃ³n:
1. No existe polÃ­tica exacta.
2. Se sube al padre `employee.lifecycle`.
3. Existe perfil `WORKFLOW_EXECUTOR` con propagaciÃ³n a hijos.
4. `WORKFLOW_EXECUTOR` contiene `EXECUTE`.
5. Resultado: permitido.

## Reglas de crecimiento
Cuando nazca una vertical nueva, por ejemplo `employee.bank_account`:
1. se registra en `authorization.secured_resource`
2. se cuelga de `employee`
3. hereda permisos por defecto
4. solo se aÃ±ade override si el recurso necesita trato especial

## IntegraciÃ³n
### Backend
- Spring Security valida el JWT Bearer en cada request (Resource Server con clave simÃ©trica HS256 en V1).
- Los roles del sujeto se extraen del claim `roles` del JWT y se cargan como `GrantedAuthority` en el `SecurityContext`.
- B4RRHH resuelve autorizaciÃ³n por `resource_code + action_code` consultando su propio bounded context `authorization`.
- Se expone `POST /authorization/evaluate` que recibe `{ resourceCode, actionCode }` y evalÃºa con los roles del JWT autenticado.
- La seguridad real vive en backend.

### Frontend
- Puede consultar `POST /authorization/evaluate` para derivar capacidades UI como `canEditContacts` o `canExecuteTerminate`.
- La ocultaciÃ³n de acciones es UX, no seguridad real.

## No objetivos de V1
- CRUD API para gestionar roles, recursos, perfiles y polÃ­ticas (se gestiona por Flyway).
- AsignaciÃ³n de roles a sujetos desde la API (tabla `user_role_assignment` aplazada).
- Deny explÃ­cito con precedencia sobre grants de otros roles.
- AutorizaciÃ³n aplicada automÃ¡ticamente en los endpoints de `employee` (interceptores Spring Security). En V1 solo existe el endpoint de evaluaciÃ³n explÃ­cita.

## No objetivos
- autorizaciÃ³n contextual por instancia concreta
- seguridad por `ruleSystemCode`, `companyCode` o manager scope en V1
- autorizaciÃ³n por campo como modelo base
- detalle del login OIDC y ciclo de vida del token
- deny explÃ­citos con precedencias complejas

## Consecuencias
### Positivas
- alinea autorizaciÃ³n con el lenguaje funcional de B4RRHH
- evita acoplar permisos a endpoints
- permite defaults razonables
- admite overrides finos
- trata workflows y verticales bajo un mismo marco
- deja base sÃ³lida para auditorÃ­a futura

### Costes
- aparece un bounded context adicional
- hay que gobernar el Ã¡rbol de recursos
- la evaluaciÃ³n jerÃ¡rquica debe estar muy bien testeada

## Plan de implantaciÃ³n
1. Crear schema `authorization` y semillas base.
2. Implementar evaluaciÃ³n jerÃ¡rquica en backend.
3. Integrar roles efectivos desde JWT u origen externo.
4. Exponer capacidades derivadas al frontend.
5. Extender a `rulesystem` y futuros bounded contexts.
6. Evaluar futuras extensiones: auditorÃ­a, contexto, datos sensibles.

## Resumen ejecutivo
B4RRHH debe modelar la autorizaciÃ³n como un dominio propio, separado de la autenticaciÃ³n, apoyado en recursos funcionales jerÃ¡rquicos. Los roles no conceden permisos sobre endpoints, sino perfiles de permiso sobre recursos del Ã¡rbol funcional del sistema.

La combinaciÃ³n de recurso jerÃ¡rquico, perfil reusable y propagaciÃ³n al Ã¡rbol permite exactamente el equilibrio buscado:
- permisos por defecto razonables
- overrides explÃ­citos para recursos sensibles o workflows concretos
- crecimiento limpio sin mantenimiento infernal


<!-- END FILE: ADR-024_autorizacion_jerarquica_B4RRHH.md -->


---

# FILE: ADR-025-subject-roles.md.md
<a name="file-adr-025-subject-roles-md-md"></a>

<!-- BEGIN FILE: ADR-025-subject-roles.md.md -->

ADR â€” Identidad por Subject y AsignaciÃ³n Interna de Roles en B4RRHH

Estado: Propuesto

## Contexto
B4RRHH dispone de autenticaciÃ³n JWT y un modelo de autorizaciÃ³n interno basado en roles, recursos, perfiles y polÃ­ticas.
Actualmente, los roles se transportan en el JWT, lo cual mezcla identidad y autorizaciÃ³n.

## Problema
Se necesita un modelo coherente que:
- Separe autenticaciÃ³n de autorizaciÃ³n
- Permita operaciÃ³n en local sin IdP externo
- Evite usar el JWT como fuente de verdad de roles

## DecisiÃ³n
B4RRHH utilizarÃ¡:
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
5. Backend evalÃºa permisos

## Consecuencias
Positivas:
- SeparaciÃ³n clara de responsabilidades
- Preparado para futuro IdP
- Coherencia del modelo

Negativas:
- Nueva tabla y servicio
- Mayor complejidad inicial

## No objetivos
- No se introduce login con contraseÃ±a
- No se introduce dominio user
- No se integra IdP externo en esta fase

## EvoluciÃ³n futura
IntegraciÃ³n con proveedor externo manteniendo autorizaciÃ³n interna.

## Sobre subjeect_code
subject_code representa la identidad autenticada del actor y se trata como identificador opaco; no se normaliza por case y no se interpreta como business key.

<!-- END FILE: ADR-025-subject-roles.md.md -->


---

# FILE: ADR-026-payroll-status-workflow.md.md
<a name="file-adr-026-payroll-status-workflow-md-md"></a>

<!-- BEGIN FILE: ADR-026-payroll-status-workflow.md.md -->

# ADR â€” Payroll Status Workflow and Recalculation Guardrails

## Estado
Propuesto

## Contexto
Se necesita controlar estrictamente cuÃ¡ndo una nÃ³mina puede recalcularse.

## Estados
- NOT_VALID
- CALCULATED
- EXPLICIT_VALIDATED
- DEFINITIVE

## Regla central
Solo las nÃ³minas en `NOT_VALID` pueden ser recalculadas (borradas y recreadas).

## SemÃ¡ntica

### NOT_VALID
- Resultado invÃ¡lido o invalidado manualmente
- Ãšnico estado recalcable

### CALCULATED
- Resultado vÃ¡lido provisional
- No recalculable sin pasar a NOT_VALID

### EXPLICIT_VALIDATED
- Validada manualmente
- Bloqueada frente a recÃ¡lculo automÃ¡tico

### DEFINITIVE
- Final, inmutable

## Transiciones

### Desde NOT_VALID
- -> CALCULATED (cÃ¡lculo OK)
- -> NOT_VALID (cÃ¡lculo sigue invÃ¡lido)
- NO -> EXPLICIT_VALIDATED
- NO -> DEFINITIVE

### Desde CALCULATED
- -> NOT_VALID (invalidaciÃ³n)
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
El motor solo borra/recrea nÃ³minas en NOT_VALID.

## Resumen
Workflow seguro que evita recÃ¡lculos accidentales mediante invalidaciÃ³n explÃ­cita previa.


<!-- END FILE: ADR-026-payroll-status-workflow.md.md -->


---

# FILE: ADR-027-payroll-root-model.md.md
<a name="file-adr-027-payroll-root-model-md-md"></a>

<!-- BEGIN FILE: ADR-027-payroll-root-model.md.md -->

# ADR â€” Payroll Root Model (`payroll.payroll`)

## Estado
Propuesto

## Contexto
B4RRHH organiza por verticales y usa business keys en APIs. `employee.presence` identifica una relaciÃ³n laboral (empleado + presenceNumber).
La nÃ³mina debe modelarse como **resultado de cÃ¡lculo**, no como documento ni CRUD editable.

## DecisiÃ³n
Se crea el bounded context `payroll` (schema propio) y la raÃ­z:
- `payroll.payroll`

Representa el resultado funcional de una nÃ³mina para:
- empleado
- perÃ­odo de nÃ³mina
- tipo de nÃ³mina
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

## Campos raÃ­z
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
- No ediciÃ³n manual
- SustituciÃ³n por borrado + recreaciÃ³n
- Unicidad por business key

## Resumen
`payroll.payroll` es un resultado materializado, no editable, regenerable por cÃ¡lculo, con conceptos y snapshots dependientes.


<!-- END FILE: ADR-027-payroll-root-model.md.md -->


---

# FILE: ADR-029-payroll-calculate-contract-stub.md
<a name="file-adr-029-payroll-calculate-contract-stub-md"></a>

<!-- BEGIN FILE: ADR-029-payroll-calculate-contract-stub.md -->

# ADR â€” Payroll Calculate Contract (Initial Stub Calculator)

## Estado
Propuesto

## Contexto

B4RRHH ya ha decidido que `payroll.payroll` es un resultado materializado, no editable, con business key funcional basada en empleado + perÃ­odo + tipo + presencia, y que los estados del resultado gobiernan si una nÃ³mina puede o no ser sustituida. îˆ€fileciteîˆ‚turn4file1îˆ îˆ€fileciteîˆ‚turn4file0îˆ

TambiÃ©n se ha decidido ahora que el launch de nÃ³mina sÃ³lo resuelve y orquesta unidades elegibles, delegando el cÃ¡lculo real a otro caso de uso especializado.

El proyecto, ademÃ¡s, exige:

- arquitectura vertical-first;
- APIs pÃºblicas por business keys;
- naming semÃ¡ntico;
- evitar sobreingenierÃ­a prematura. îˆ€fileciteîˆ‚turn4file10îˆ îˆ€fileciteîˆ‚turn4file12îˆ îˆ€fileciteîˆ‚turn4file13îˆ

En esta fase todavÃ­a no existe un motor de reglas de nÃ³mina real. Sin embargo, hace falta un componente de cÃ¡lculo inicial que permita probar:

- el flujo launch -> calculate;
- la sustituciÃ³n por borrado + recreaciÃ³n;
- la creaciÃ³n de `payroll.payroll`;
- la generaciÃ³n de conceptos;
- la generaciÃ³n de snapshots;
- el tratamiento de estados `CALCULATED` y `NOT_VALID`.

## Problema

Se necesita un contrato de cÃ¡lculo inicial que permita construir un **stub calculator Ãºtil**, suficientemente real para validar el pipeline tÃ©cnico y funcional, pero deliberadamente pequeÃ±o para no anticipar todavÃ­a el motor de reglas.

Ese cÃ¡lculo inicial debe:

- recibir unidades explÃ­citas ya resueltas;
- no decidir poblaciones objetivo;
- materializar resultados en `payroll`;
- poder generar resultados `CALCULATED` y `NOT_VALID`;
- ser sustituible en el futuro por el motor real sin romper la semÃ¡ntica externa.

## DecisiÃ³n

Se introduce el contrato de **Payroll Calculate** como caso de uso/endpoint especializado que recibe una lista explÃ­cita de unidades de cÃ¡lculo y materializa resultados de nÃ³mina.

`calculate`:

- no resuelve la poblaciÃ³n objetivo;
- no selecciona elegibles por sÃ­ mismo como responsabilidad principal;
- no es todavÃ­a un motor declarativo de reglas;
- actÃºa como calculador inicial del sistema;
- podrÃ¡ empezar implementado como **stub calculator**.

## DefiniciÃ³n funcional

`calculate` recibe una colecciÃ³n cerrada de unidades de cÃ¡lculo y, para cada una de ellas:

1. valida precondiciones mÃ­nimas;
2. elimina la nÃ³mina previa sÃ³lo si existe y es sustituible segÃºn reglas;
3. crea una nueva `payroll.payroll`;
4. crea conceptos de nÃ³mina de prueba o cÃ¡lculo bÃ¡sico;
5. crea snapshots contextuales mÃ­nimos;
6. persiste el resultado final en estado:
   - `CALCULATED`, si el cÃ¡lculo concluye correctamente;
   - `NOT_VALID`, si detecta una invalidez funcional del cÃ¡lculo.

## Unidad de entrada

La unidad mÃ­nima de entrada es:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `payrollPeriodCode`
- `payrollTypeCode`
- `presenceNumber`
- `calculationEngineCode`
- `calculationEngineVersion`

### Regla

`calculate` debe trabajar con unidades **explÃ­citas**.

No debe aceptar un payload ambiguo que implique â€œresolver toda una poblaciÃ³nâ€. Esa responsabilidad pertenece al launch.

## Responsabilidades de calculate

`calculate` debe:

- cargar el contexto funcional mÃ­nimo de la unidad;
- comprobar la existencia previa de `payroll.payroll`;
- aplicar la polÃ­tica de sustituciÃ³n;
- crear nueva raÃ­z `payroll.payroll`;
- crear `payroll_concept`;
- crear `payroll_context_snapshot`;
- devolver resultado por unidad.

`calculate` no debe todavÃ­a:

- implementar reglas salariales complejas;
- modelar convenios reales;
- resolver retroactividad completa;
- introducir DSLs o engines genÃ©ricos;
- depender de un metamodelo complejo de reglas.

## PolÃ­tica de sustituciÃ³n

Para cada unidad explÃ­cita:

### Si no existe nÃ³mina previa
- crear una nueva `payroll.payroll`.

### Si existe y estÃ¡ `NOT_VALID`
- eliminar la raÃ­z previa;
- dejar que `ON DELETE CASCADE` elimine conceptos y snapshots; îˆ€fileciteîˆ‚turn4file6îˆ
- crear una nueva `payroll.payroll`.

### Si existe y estÃ¡ `CALCULATED`, `EXPLICIT_VALIDATED` o `DEFINITIVE`
- no sustituirla;
- devolver resultado de unidad ignorada/no procesada, segÃºn shape final del contrato.

## Contrato de salida

`calculate` debe devolver resultado por unidad.

Campos orientativos:

- identity de la unidad;
- `processed = true/false`;
- `resultStatus = CALCULATED | NOT_VALID | SKIPPED`;
- motivo cuando no se procese;
- business key final de la nÃ³mina generada, si aplica.

## Stub calculator inicial

Se adopta explÃ­citamente una estrategia de implementaciÃ³n por fases.

### Fase inicial permitida

El primer `calculate` puede generar una nÃ³mina artificial pero funcionalmente Ãºtil.

Ejemplo mÃ­nimo:

- crear `payroll.payroll`;
- generar 2 conceptos de prueba;
- generar 1 o 2 snapshots de contexto;
- persistir en `CALCULATED`.

TambiÃ©n puede contemplarse una condiciÃ³n de prueba que genere `NOT_VALID` cuando falte algÃºn dato mÃ­nimo requerido.

### Objetivo de esta fase

No hacer nÃ³mina real todavÃ­a.

El objetivo es validar el pipeline:

- endpoints;
- wiring;
- persistencia;
- borrado y recreaciÃ³n;
- estados;
- snapshots;
- conceptos;
- respuesta funcional.

## Conceptos de prueba

En esta fase, `payroll_concept` puede contener conceptos semilla o de demostraciÃ³n.

Ejemplo conceptual:

- `BASE_TEST`
- `DEVENGO_TEST`

Los nombres y cÃ³digos deben seguir una convenciÃ³n de negocio estable y no reforzar identidades tÃ©cnicas equivocadas. El proyecto prioriza nombres de negocio y cÃ³digos funcionales estables. îˆ€fileciteîˆ‚turn4file12îˆ

## Snapshots mÃ­nimos

`calculate` debe poblar al menos snapshots bÃ¡sicos para demostrar el diseÃ±o ya aprobado de `payroll_context_snapshot`. îˆ€fileciteîˆ‚turn4file6îˆ

Ejemplos iniciales razonables:

- `EMPLOYEE_CORE`
- `PRESENCE`
- opcionalmente `WORKING_TIME` o `CONTRACT` cuando sea barato de recuperar

No es obligatorio arrancar con todos los snapshots futuros.

## Reglas de error / invalidez

En esta fase se distinguen dos clases:

### 1. Error tÃ©cnico
Ejemplo:
- fallo de persistencia;
- error inesperado de infraestructura.

Esto debe reportarse como error tÃ©cnico del proceso.

### 2. Resultado funcional `NOT_VALID`
Ejemplo:
- falta dato mÃ­nimo requerido para construir el cÃ¡lculo stub;
- inconsistencia funcional detectada por el calculador.

En este caso sÃ­ puede persistirse una `payroll.payroll` con `status = NOT_VALID`, coherente con el workflow ya aprobado. îˆ€fileciteîˆ‚turn4file0îˆ

## Forma de exposiciÃ³n

A falta de contrato final, se admiten dos estrategias:

### OpciÃ³n A â€” calculate sÃ³lo como caso de uso interno
Ãštil si launch es el Ãºnico endpoint externo.

### OpciÃ³n B â€” calculate tambiÃ©n como endpoint explÃ­cito
Ãštil para pruebas con Postman y validaciÃ³n incremental del pipeline.

En esta fase, se acepta la opciÃ³n B por su valor prÃ¡ctico para acelerar aprendizaje y validaciÃ³n del flujo.

Nombre conceptual recomendado:

- `POST /payroll/calculations/calculate`

## QuÃ© se rechaza explÃ­citamente

Se rechaza en esta fase:

- introducir un motor declarativo de reglas;
- mezclar calculate con resoluciÃ³n de poblaciÃ³n;
- convertir calculate en un endpoint de â€œhazlo todoâ€ sin unidades explÃ­citas;
- bloquear el diseÃ±o futuro con un contrato demasiado acoplado al stub.

## Consecuencias

### Positivas

- permite probar el flujo completo desde muy pronto;
- separa orquestaciÃ³n de cÃ¡lculo;
- facilita sustituciÃ³n futura por motor real;
- valida conceptos y snapshots sin esperar al dominio salarial completo.

### Costes

- exige mantener disciplina para que el stub no se convierta en soluciÃ³n definitiva;
- habrÃ¡ que evolucionar el contrato interno del calculador en fases posteriores;
- el primer resultado no representarÃ¡ todavÃ­a nÃ³mina real.

## Resumen

En B4RRHH, `calculate` es el caso de uso especializado que recibe unidades explÃ­citas ya resueltas y materializa `payroll.payroll` con conceptos y snapshots.

En la primera iteraciÃ³n, puede implementarse como un **stub calculator Ãºtil**, orientado a validar el flujo tÃ©cnico y funcional, no a resolver todavÃ­a el motor real de reglas de nÃ³mina.


<!-- END FILE: ADR-029-payroll-calculate-contract-stub.md -->


---

# FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md
<a name="file-adr-030-payroll-launch-calculation-run-claim-and-internal-calculator-orchestration-md"></a>

<!-- BEGIN FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md -->

ADR â€” Payroll Launch, Calculation Run, Claim and Internal Calculator Orchestration
Estado

Propuesto

Contexto

El bounded context payroll ya ha fijado una base importante:

la raÃ­z funcional es payroll.payroll;
su identidad funcional es:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber;
payroll.payroll representa un resultado materializado de cÃ¡lculo, no un CRUD editable;
sus hijos (payroll_concept, payroll_context_snapshot) dependen completamente de la raÃ­z y deben eliminarse por cascade;
los estados de nÃ³mina gobiernan si una nÃ³mina puede o no ser sustituida;
una nÃ³mina existente solo es recalculable si estÃ¡ en NOT_VALID;
una unidad sin nÃ³mina previa tambiÃ©n debe ser elegible para cÃ¡lculo inicial.

TambiÃ©n se ha decidido ya que el endpoint actual POST /payrolls/calculate no representa el futuro motor real, sino un stub temporal de validaciÃ³n de pipeline, donde el cliente aÃºn aporta conceptos y snapshots explÃ­citamente para poder probar el flujo de extremo a extremo antes de diseÃ±ar el lanzador real y el calculador definitivo.

En paralelo, B4RRHH tiene reglas de arquitectura muy claras:

primero se organiza por vertical/subdominio y dentro de cada vertical se aplica arquitectura hexagonal;
las APIs pÃºblicas deben usar business keys, no IDs tÃ©cnicos;
cuando una operaciÃ³n no encaja como CRUD plano, debe modelarse como workflow/caso de uso explÃ­cito y no como recurso falso o tabla oportunista.

Al hablar del lanzamiento de nÃ³mina aparecen dos problemas de diseÃ±o que no deben mezclarse:

quÃ© significa lanzar un cÃ¡lculo;
cÃ³mo evitar que dos lanzamientos simultÃ¡neos procesen la misma unidad de cÃ¡lculo.

AdemÃ¡s, si el lanzamiento solo devuelve un body HTTP efÃ­mero, se pierde una capacidad que serÃ¡ Ãºtil muy pronto:

consultar desde frontend cÃ³mo va un cÃ¡lculo;
saber cuÃ¡ntas unidades se han procesado;
ver quÃ© se ha omitido, quÃ© se ha reclamado, quÃ© terminÃ³ en CALCULATED, quÃ© quedÃ³ en NOT_VALID y quÃ© fallÃ³.

Por todo ello, hace falta un modelo explÃ­cito para:

el workflow de launch;
la persistencia de los runs;
la exclusiÃ³n concurrente por unidad de cÃ¡lculo;
el desacoplamiento del calculador real respecto del endpoint pÃºblico.
Problema

Se necesita definir una arquitectura de lanzamiento de nÃ³mina que:

permita lanzar cÃ¡lculos sobre una poblaciÃ³n objetivo;
resuelva y expanda dicha poblaciÃ³n a unidades reales de cÃ¡lculo;
filtre elegibilidad sin recalcular resultados protegidos;
permita concurrencia segura;
deje preparada la paralelizaciÃ³n futura;
permita seguimiento de progreso;
desacople el launch del motor de cÃ¡lculo real;
evite convertir el cÃ¡lculo actual stub en contrato definitivo por accidente.
DecisiÃ³n

Se adopta una arquitectura de orquestaciÃ³n de cÃ¡lculo basada en cuatro piezas distintas:

payroll.payroll
Resultado materializado de una unidad de cÃ¡lculo.
payroll.calculation_run
Recurso tÃ©cnico-operativo persistido que representa una ejecuciÃ³n de lanzamiento.
payroll.calculation_claim
Recurso tÃ©cnico de exclusiÃ³n concurrente por unidad de cÃ¡lculo.
calculate como caso de uso interno especializado, no como contrato pÃºblico canÃ³nico del motor definitivo.

El lanzamiento de nÃ³mina se modela como un workflow explÃ­cito que:

crea un calculation_run;
resuelve poblaciÃ³n objetivo;
expande a unidades de cÃ¡lculo;
determina elegibilidad;
intenta adquirir claims por unidad;
delega el cÃ¡lculo efectivo a un calculador interno;
registra progreso y resumen.
Principio madre

Launch no calcula; launch coordina.
Calculate no decide poblaciÃ³n; calculate materializa una unidad.

Esta separaciÃ³n es obligatoria.

Definiciones principales
1. Unidad de cÃ¡lculo

La unidad mÃ­nima de cÃ¡lculo es:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber

Esta unidad es coherente con la business key de payroll.payroll ya adoptada y con el hecho de que una presencia concreta representa una relaciÃ³n laboral concreta del empleado.

Regla

El launch siempre trabaja con una colecciÃ³n de unidades de cÃ¡lculo, no con empleados abstractos.

2. Population target vs eligible units

Se distinguen dos conceptos.

PoblaciÃ³n objetivo

Es el conjunto de empleados o Ã¡mbitos que el usuario quiere lanzar.

Ejemplos:

un empleado concreto;
una lista explÃ­cita de empleados;
todos los empleados de un scope determinado.
Unidades elegibles

Son las unidades de cÃ¡lculo que realmente pueden entrar al cÃ¡lculo.

Una unidad es elegible si:

no existe payroll.payroll previa para su business key, o
existe y su estado actual es NOT_VALID.

Una unidad no es elegible si existe y estÃ¡ en:

CALCULATED
EXPLICIT_VALIDATED
DEFINITIVE

Esta regla mantiene el guardarraÃ­l funcional ya fijado para payroll y evita recÃ¡lculos accidentales sobre resultados vigentes o protegidos.

3. payroll.calculation_run

Se introduce un recurso tÃ©cnico-operativo persistido:

payroll.calculation_run
Naturaleza

No es una nÃ³mina.
No es un resultado de negocio final.
No sustituye a payroll.payroll.

Representa una ejecuciÃ³n de lanzamiento.

Objetivo

Permitir:

seguimiento del progreso;
trazabilidad del lanzamiento;
resumen persistido;
futura consulta desde frontend;
base para asincronÃ­a o paralelizaciÃ³n posterior.
Campos mÃ­nimos recomendados
id tÃ©cnico interno
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

La mÃ¡quina de estados de calculation_run es independiente de la mÃ¡quina de estados de payroll.payroll.

No deben mezclarse.

4. payroll.calculation_claim

Se introduce un recurso tÃ©cnico de exclusiÃ³n concurrente:

payroll.calculation_claim
Naturaleza

No representa negocio visible al usuario final.
No sustituye a locks de BD del aggregate.
No es un recurso funcional pÃºblico.

Su misiÃ³n es impedir que dos runs distintos procesen simultÃ¡neamente la misma unidad de cÃ¡lculo.

Claim key

La identidad funcional del claim es exactamente la unidad de cÃ¡lculo:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber
Campos recomendados
id tÃ©cnico
claim key completa
runId
claimedAt
claimedBy nullable
RestricciÃ³n obligatoria

Debe existir una restricciÃ³n Ãºnica por claim key completa.

Regla de adquisiciÃ³n

La adquisiciÃ³n del claim debe ser atÃ³mica.

No se recomienda una lÃ³gica de:

leer si existe
y luego insertar

por riesgo de carrera.

La lÃ³gica correcta es:

intentar insertar claim;
si inserta, la unidad queda reclamada por ese run;
si falla por unicidad, esa unidad ya estÃ¡ siendo procesada por otro run y debe ignorarse o marcarse como no reclamada.
Regla de limpieza

Al finalizar el procesamiento de la unidad, el claim se elimina.

ExtensiÃ³n futura

MÃ¡s adelante puede aÃ±adirse:

expiresAt
recuperaciÃ³n de claims huÃ©rfanos
housekeeping periÃ³dico

pero no es requisito de esta decisiÃ³n base.

5. payroll.calculation_run_item

Se recomienda introducir, ya desde la base o en una fase muy cercana, una tabla hija de seguimiento fino:

payroll.calculation_run_item
Naturaleza

Representa el estado de una unidad concreta dentro de un run concreto.

Objetivo

Permitir:

trazabilidad por unidad;
saber quÃ© pasÃ³ con cada cÃ¡lculo;
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

calculation_run_item pertenece al seguimiento del run, no al dominio raÃ­z de payroll result.

SemÃ¡ntica de Launch
DefiniciÃ³n

Lanzar nÃ³mina significa:

crear una ejecuciÃ³n persistida de cÃ¡lculo, resolver una poblaciÃ³n objetivo, expandirla a unidades de cÃ¡lculo, filtrar unidades elegibles, intentar reclamar cada unidad de forma exclusiva, delegar el cÃ¡lculo efectivo al calculador interno y registrar el progreso y resultado del proceso.

Responsabilidades obligatorias del launch

El launch debe:

crear calculation_run;
validar contexto de ejecuciÃ³n;
resolver la poblaciÃ³n objetivo;
expandirla a unidades de cÃ¡lculo;
determinar elegibilidad;
crear run_items o equivalente lÃ³gico;
intentar adquirir claim por unidad elegible;
delegar en calculate interno;
consolidar estados por unidad;
actualizar resumen y estado final del run.

El launch no debe:

generar conceptos Ã©l mismo;
implementar reglas salariales;
convertirse en motor de cÃ¡lculo;
depender de HTTP interno al propio backend si launch y calculate viven en el mismo servicio.
Input mÃ­nimo del launch

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
lista explÃ­cita
Ã¡mbito masivo simple

El shape contractual exacto podrÃ¡ evolucionar, pero el launch debe conservar esta responsabilidad de resoluciÃ³n.

Output del launch

El launch no debe limitarse a devolver â€œ201 createdâ€ con un resumen efÃ­mero.

Debe devolver, al menos:

runId
estado inicial o final del run
resumen agregado

y permitir despuÃ©s consultar el run persistido.

Calculate interno
DecisiÃ³n clave

El futuro calculate no debe consolidarse como endpoint pÃºblico canÃ³nico.

El endpoint actual de calculate se acepta solo como stub temporal de validaciÃ³n de pipeline, tal como ya estÃ¡ documentado en OpenAPI.

La decisiÃ³n de fondo es:

el cÃ¡lculo serio serÃ¡ largo, cambiante y costoso de desarrollar;
por tanto, el launch no debe acoplarse a un endpoint pÃºblico rÃ­gido del motor.
Regla arquitectÃ³nica

El launch debe invocar un caso de uso interno de cÃ¡lculo, no un endpoint HTTP del propio backend.

Ejemplo conceptual:

CalculatePayrollUnitUseCase
Responsabilidad del calculate interno

Recibir una unidad explÃ­cita y materializar un resultado:

creando payroll.payroll si no existe;
sustituyÃ©ndola si existe y es recalculable;
generando CALCULATED o NOT_VALID segÃºn corresponda.
Importante

El calculate interno no resuelve poblaciones.
Eso pertenece exclusivamente al launch.

Concurrencia
DecisiÃ³n principal

La concurrencia se gobierna mediante payroll.calculation_claim, no mediante el aggregate payroll.payroll.

JustificaciÃ³n

La concurrencia aquÃ­ es un problema del workflow de ejecuciÃ³n, no de la identidad del recurso raÃ­z.

Reglas
dos runs pueden existir simultÃ¡neamente;
dos runs no pueden procesar simultÃ¡neamente la misma unidad de cÃ¡lculo;
si una unidad ya estÃ¡ reclamada por otro run, el launch actual debe marcarla como no reclamada / ya en curso y seguir adelante.
DiseÃ±o objetivo vs implementaciÃ³n inicial
DiseÃ±o objetivo

ExclusiÃ³n por unidad de cÃ¡lculo.

ImplementaciÃ³n inicial recomendada

La propia claim table ya permite ese diseÃ±o desde la primera iteraciÃ³n, por lo que no se considera necesario arrancar con un bloqueo global de launch.

Consecuencia

La paralelizaciÃ³n futura queda abierta desde el primer dÃ­a, aunque inicialmente el procesamiento interno pueda seguir siendo secuencial.

RelaciÃ³n entre claim y business key de payroll

La restricciÃ³n Ãºnica de payroll.payroll por business key sigue siendo obligatoria y valiosa, pero no se considera mecanismo principal de coordinaciÃ³n concurrente.

Papel de la unique en payroll
protege integridad final del resultado;
actÃºa como Ãºltima lÃ­nea de defensa.
Papel del claim
evita que dos runs intenten procesar simultÃ¡neamente la misma unidad.

Por tanto:

la unique de payroll no sustituye a la claim table;
la claim table no sustituye a la unique del root.

Ambas son necesarias y cumplen papeles distintos.

Procesamiento secuencial vs paralelo
Regla base

El ADR no obliga a que el launch sea sÃ­ncrono o asÃ­ncrono, ni a que procese secuencial o paralelamente.

Lo que sÃ­ fija es la semÃ¡ntica.

V1 aceptable
run persistido
claims por unidad
procesamiento secuencial dentro del launch
resumen final persistido
EvoluciÃ³n natural
paralelizaciÃ³n por chunks o workers
asÃ­ncrono
polling desde frontend del estado del run
reintentos por unidad

El diseÃ±o aquÃ­ debe soportar esas evoluciones sin rehacer la semÃ¡ntica.

API pÃºblica recomendada
Endpoints canÃ³nicos de resultado

Se mantienen por business key:

GET payroll by business key
invalidate
explicit-validate
finalize

Esto sigue la convenciÃ³n general del proyecto de usar business keys pÃºblicas y acciones explÃ­citas cuando el dominio lo pide.

Endpoint de launch

Se recomienda un endpoint pÃºblico explÃ­cito de negocio, por ejemplo:

POST /payroll/calculation-runs/launch

o naming equivalente claramente orientado a ejecuciÃ³n.

Lectura de run

Se recomienda poder consultar:

GET /payroll/calculation-runs/{runId}

y eventualmente listar runs recientes o items asociados.

Calculate

El endpoint actual de calculate no se considera canÃ³nico a futuro. Su continuidad se limita a la fase stub/pre-launch ya documentada.

QuÃ© se rechaza explÃ­citamente

Se rechaza:

tratar launch como CRUD;
hacer que launch invoque HTTP contra su propio backend como arquitectura permanente;
bloquear necesariamente todo el sistema a un solo launch global;
usar solo la unique de payroll como soluciÃ³n de concurrencia;
mezclar calculation_run con payroll.payroll;
convertir calculation_run en una nueva raÃ­z funcional de negocio;
fijar ya el contrato definitivo del motor de cÃ¡lculo real;
acoplar la semÃ¡ntica de launch al stub actual de calculate.
Consecuencias
Positivas
separaciÃ³n limpia entre resultado, ejecuciÃ³n, concurrencia y motor;
base sÃ³lida para meses de evoluciÃ³n sin rehacer el modelo;
posibilidad de seguimiento de runs desde frontend;
paralelizaciÃ³n futura preparada desde el diseÃ±o;
acoplamiento bajo entre launch y motor real;
protecciÃ³n real frente a colisiones concurrentes.
Costes
introduce recursos tÃ©cnicos adicionales (calculation_run, calculation_claim, probablemente calculation_run_item);
exige disciplina para no mezclar estados de run con estados de payroll;
aÃ±ade trabajo de persistencia y de resumen/progreso.
Plan recomendado por fases
Fase 1
introducir calculation_run
introducir calculation_claim
introducir launch
mantener procesamiento secuencial
calculate sigue siendo interno
persistir resumen bÃ¡sico del run
Fase 2
introducir calculation_run_item
seguimiento fino por unidad
consulta desde frontend del progreso
Fase 3
paralelizaciÃ³n real
workers o executor
asincronÃ­a y polling mÃ¡s rico
posible housekeeping de claims
Resumen ejecutivo

En B4RRHH, el lanzamiento de nÃ³mina no se modelarÃ¡ como un simple POST que calcula y devuelve un body efÃ­mero.

Se adopta una arquitectura en la que:

payroll.payroll sigue siendo el resultado materializado por unidad;
payroll.calculation_run representa una ejecuciÃ³n persistida de lanzamiento;
payroll.calculation_claim garantiza exclusiÃ³n concurrente por unidad de cÃ¡lculo;
calculate serÃ¡ un caso de uso interno especializado y desacoplado del contrato pÃºblico final;
el launch coordina, reclama, delega y registra;
la semÃ¡ntica queda preparada tanto para una V1 secuencial como para una evoluciÃ³n futura paralelizable y observable.

<!-- END FILE: ADR-030-Payroll-Launch-Calculation-Run-Claim-and-Internal-Calculator-Orchestration.md -->


---

# FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md
<a name="file-adr-031-modelo-f-sico-de-payroll-launch--calculation-run-claims-y-mensajes-md"></a>

<!-- BEGIN FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md -->

ADR â€” Modelo fÃ­sico de payroll launch, calculation run, claims y mensajes
Estado

Propuesto

Contexto

El bounded context payroll ya tiene fijadas varias decisiones estructurales:

payroll.payroll es la raÃ­z funcional del resultado materializado de nÃ³mina;
su business key es:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber;
payroll.payroll no es un CRUD editable, sino un resultado de cÃ¡lculo;
los hijos payroll_concept y payroll_context_snapshot dependen completamente de la raÃ­z y se eliminan por cascade;
el estado de la nÃ³mina (NOT_VALID, CALCULATED, EXPLICIT_VALIDATED, DEFINITIVE) vive en la propia payroll y gobierna si puede ser sustituida o no;
una unidad sin nÃ³mina previa tambiÃ©n debe ser elegible para cÃ¡lculo inicial.

TambiÃ©n se ha consolidado otra separaciÃ³n importante:

launch coordina;
calculate materializa una unidad;
la concurrencia no debe gobernarse dentro del aggregate payroll.payroll, sino en una capa tÃ©cnica de ejecuciÃ³n;
el proyecto prefiere workflows explÃ­citos cuando una operaciÃ³n no encaja como CRUD plano.

Durante el diseÃ±o del lanzamiento apareciÃ³ una discusiÃ³n relevante sobre el detalle por unidad de ejecuciÃ³n.

Se descarta como nÃºcleo inicial una tabla obligatoria de calculation_run_item porque:

generarÃ­a una fila por unidad para cada run;
puede producir mucho volumen con poco valor persistente;
muchos errores funcionales pertenecen realmente a la nÃ³mina materializada y no al run;
tras un nuevo cÃ¡lculo de esa unidad, gran parte de ese detalle pierde relevancia operativa.

En cambio, sÃ­ se considera Ãºtil distinguir dos tipos de mensajes:

mensajes adheridos a la nÃ³mina
pertenecen al resultado materializado y deben vivir como vertical hija de payroll.payroll;
mensajes del run
pertenecen a la ejecuciÃ³n tÃ©cnica del launch y pueden existir incluso cuando no se materializa una nueva payroll.
Problema

Se necesita un modelo fÃ­sico que permita:

persistir ejecuciones de launch;
seguir su progreso general;
impedir concurrencia simultÃ¡nea sobre la misma unidad de cÃ¡lculo;
registrar mensajes operativos/tÃ©cnicos del run;
registrar mensajes funcionales o revisables adheridos a una payroll;
mantener separado:
el resultado materializado (payroll.payroll)
de la ejecuciÃ³n tÃ©cnica (calculation_run, calculation_claim, calculation_run_message).

El modelo debe ser suficiente para meses de evoluciÃ³n, sin fijar todavÃ­a el motor real de reglas.

DecisiÃ³n

Se adopta dentro del schema payroll el siguiente modelo fÃ­sico base:

payroll.payroll
resultado materializado de una unidad de cÃ¡lculo. Ya existente.
payroll.payroll_warning
mensajes funcionales adheridos a una nÃ³mina concreta.
payroll.calculation_run
ejecuciÃ³n tÃ©cnica persistida de un launch.
payroll.calculation_claim
exclusiÃ³n concurrente por unidad de cÃ¡lculo.
payroll.calculation_run_message
mensajes operativos, tÃ©cnicos o de exclusiÃ³n del propio run.

Se decide no introducir payroll.calculation_run_item como tabla obligatoria en la base inicial.

Principio estructural

La payroll persiste resultado.
La payroll_warning persiste mensajes funcionales de ese resultado.
El run persiste la ejecuciÃ³n.
El claim persiste exclusiÃ³n concurrente.
El run_message persiste incidencias y mensajes de la ejecuciÃ³n.

Cada pieza resuelve un problema distinto.

1. Tabla payroll.calculation_run
PropÃ³sito

Representar una ejecuciÃ³n tÃ©cnica de lanzamiento de nÃ³mina.

No es una payroll.
No es una raÃ­z funcional de negocio.
No sustituye a payroll.payroll.

Sirve para:

trazabilidad operativa;
seguimiento desde backend y frontend;
resumen persistido del lanzamiento;
futura asincronÃ­a o paralelizaciÃ³n.
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
JustificaciÃ³n
Contexto del run
rule_system_code
payroll_period_code
payroll_type_code
calculation_engine_code
calculation_engine_version

definen el marco operativo del lanzamiento.

target_selection_json

Se persiste en JSON porque representa la selecciÃ³n objetivo del launch y todavÃ­a no compensa fijar un modelo relacional complejo para todas sus variantes.

Contadores agregados

Se mantienen como columnas explÃ­citas porque permiten:

seguimiento rÃ¡pido;
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
Ãndices recomendados
(rule_system_code, payroll_period_code, payroll_type_code)
(status)
(requested_at desc)
2. Tabla payroll.calculation_claim
PropÃ³sito

Persistir la exclusiÃ³n concurrente por unidad de cÃ¡lculo.

Su misiÃ³n es impedir que dos runs distintos procesen al mismo tiempo la misma unidad:

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
RestricciÃ³n clave

Debe existir una unique fuerte por la calculation key completa:

(rule_system_code, employee_type_code, employee_number, payroll_period_code, payroll_type_code, presence_number)
Regla de adquisiciÃ³n

La adquisiciÃ³n del claim debe ser atÃ³mica mediante insert.

No se acepta como patrÃ³n base:

leer si existe;
luego insertar.
SemÃ¡ntica de vida
si el insert entra, la unidad queda reclamada;
si falla por unique, la unidad ya estÃ¡ en curso en otro run;
al terminar de procesar la unidad, el claim se elimina.
Por quÃ© no guardar status en claim

Porque claim no es una mini mÃ¡quina de estados.
Su Ãºnica misiÃ³n es representar posesiÃ³n exclusiva temporal de una unidad.

Ãndice recomendado
(run_id)
3. Tabla payroll.payroll_warning
PropÃ³sito

Persistir mensajes funcionales adheridos a una nÃ³mina concreta.

No representan incidencias del run, sino mensajes del resultado materializado.

Pueden incluir:

errores funcionales;
avisos;
observaciones;
cosas a revisar por usuario;
mensajes no bloqueantes pero relevantes.
Naturaleza semÃ¡ntica

Se adopta el tÃ©rmino warning de forma deliberada para no encerrar la semÃ¡ntica en â€œerrorâ€.

El diseÃ±o debe permitir:

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
RestricciÃ³n Ãºnica recomendada

No fijarÃ­a una unique demasiado agresiva de entrada.

PodrÃ­a existir mÃ¡s de un warning con el mismo warning_code si en el futuro aparece necesidad de varias ocurrencias contextualizadas.
Si se quiere una deduplicaciÃ³n ligera, preferirÃ­a resolverla en dominio antes que forzarla ya en esquema.

Por quÃ© no tiene created_at

Se decide explÃ­citamente no aÃ±adir created_at.

JustificaciÃ³n:

payroll_warning nace y muere con la payroll.payroll;
el instante relevante ya estÃ¡ representado por payroll.calculated_at;
aÃ±adir otro timestamp duplicarÃ­a semÃ¡ntica sin aportar valor real.
Severidades recomendadas
INFO
WARNING
ERROR
Ãndices recomendados
(payroll_id)
opcionalmente (severity_code) si mÃ¡s adelante se consulta mucho por severidad
4. Tabla payroll.calculation_run_message
PropÃ³sito

Persistir mensajes del propio run.

Representa:

incidencias operativas;
errores tÃ©cnicos;
descartes por claim;
descartes por no elegibilidad;
mensajes de ejecuciÃ³n no adheribles a una payroll concreta.

Ejemplos:

â€œunidad descartada por claim activoâ€
â€œunidad omitida por estado EXPLICIT_VALIDATEDâ€
â€œerror tÃ©cnico en acceso a BDâ€
â€œfallo al resolver poblaciÃ³nâ€
â€œrun completado con conflictos parcialesâ€
Regla semÃ¡ntica

calculation_run_message no reemplaza a payroll_warning.

payroll_warning

mensaje funcional del resultado de nÃ³mina

calculation_run_message

mensaje operativo/tÃ©cnico/de ejecuciÃ³n del run

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
JustificaciÃ³n de la calculation key nullable

Se permite asociar un mensaje del run a:

una ejecuciÃ³n global;
o a una unidad concreta dentro del run.

Por eso la calculation key es nullable:

si el mensaje es global, queda vacÃ­a;
si el mensaje se refiere a una unidad concreta, se rellena.

Esto evita la necesidad de una tabla run_item obligatoria por cada unidad.

Severidades recomendadas
INFO
WARNING
ERROR
Ãndices recomendados
(run_id)
(run_id, severity_code)
opcionalmente (run_id, employee_type_code, employee_number) si mÃ¡s adelante se necesita drill-down por empleado
5. RelaciÃ³n con payroll.payroll
Regla estructural

payroll.payroll permanece como resultado materializado y no absorbe campos de launch, run, claim ni mensajes operativos.

No se deben aÃ±adir a payroll.payroll cosas como:

estado del run;
claim status;
resumen de ejecuciÃ³n;
mensajes tÃ©cnicos del launch.
Lo que sÃ­ absorbe

SÃ­ absorbe:

su estado funcional (status);
su razÃ³n (statusReasonCode);
y sus payroll_warning.

Esto mantiene coherente la separaciÃ³n entre:

resultado de negocio materializado
ejecuciÃ³n tÃ©cnica que lo produjo
6. DecisiÃ³n explÃ­cita sobre calculation_run_item
DecisiÃ³n

Se decide no introducir payroll.calculation_run_item como tabla base obligatoria.

JustificaciÃ³n
1. Volumen

GenerarÃ­a una fila por unidad y por run, con mucho crecimiento potencial para poco valor si la mayorÃ­a de unidades se comportan normalmente.

2. Relevancia temporal

Una vez existe un nuevo run sobre la misma unidad, buena parte del detalle fino del item anterior pierde valor operativo.

3. Errores funcionales

Los errores funcionales importantes pertenecen a la nÃ³mina materializada y deben vivir en payroll.payroll mediante payroll_warning, no en el run.

4. Observabilidad suficiente para V1

La combinaciÃ³n de:

calculation_run
calculation_claim
calculation_run_message
payroll_warning

proporciona una observabilidad suficientemente rica sin necesidad de una tabla hija obligatoria por unidad.

EvoluciÃ³n futura posible

No se prohÃ­be introducir calculation_run_item mÃ¡s adelante si la observabilidad operativa futura lo justifica.

Pero no forma parte del nÃºcleo inicial.

7. JSON vs relacional
JSON permitido

Se admite JSON en:

target_selection_json
summary_json
details_json de warnings
details_json de run messages

porque ahÃ­ la variaciÃ³n todavÃ­a no compensa fijarla toda en columnas.

Relacional obligatorio

Se exige modelado relacional explÃ­cito en:

calculation key del claim
contexto base del run
estado del run
referencias payroll/run
contadores agregados

porque esas piezas sÃ­ son nÃºcleo estable del diseÃ±o.

8. Restricciones e Ã­ndices recomendados completos
calculation_run
pk (id)
Ã­ndices:
(rule_system_code, payroll_period_code, payroll_type_code)
(status)
(requested_at desc)
calculation_claim
pk (id)
fk run_id -> calculation_run(id) on delete cascade
unique:
calculation key completa
Ã­ndice:
(run_id)
payroll_warning
pk (id)
fk payroll_id -> payroll(id) on delete cascade
Ã­ndice:
(payroll_id)
calculation_run_message
pk (id)
fk run_id -> calculation_run(id) on delete cascade
Ã­ndices:
(run_id)
(run_id, severity_code)
9. QuÃ© se rechaza explÃ­citamente

Se rechaza en este modelo fÃ­sico:

usar solo la unique de payroll.payroll como soluciÃ³n de concurrencia;
meter mensajes tÃ©cnicos del run dentro de payroll.payroll;
convertir calculation_claim en una tabla de workflow compleja;
introducir calculation_run_item por inercia sin haber demostrado valor real;
fijar ya el contrato definitivo del motor de cÃ¡lculo;
acoplar launch al endpoint stub actual de calculate, que sigue siendo temporal y no canÃ³nico
10. Consecuencias
Positivas
separaciÃ³n muy limpia entre resultado, mensajes funcionales, ejecuciÃ³n y concurrencia;
menos volumen estructural que con una tabla obligatoria de run items;
observabilidad suficiente para V1;
posibilidad de seguimiento desde frontend;
motor real desacoplado del workflow;
base sÃ³lida para meses de evoluciÃ³n.
Costes
aÃ±ade cuatro tablas nuevas respecto al payroll root original;
obliga a distinguir bien mensajes funcionales vs mensajes de run;
deja para una fase futura el drill-down total por unidad si algÃºn dÃ­a se necesita.
11. Estrategia de implementaciÃ³n recomendada
Fase 1
crear calculation_run
crear calculation_claim
crear payroll_warning
crear calculation_run_message
Fase 2
implementar launch sÃ­ncrono
persistir run + summary + messages
mantener calculate como caso de uso interno
Fase 3
exponer lectura de runs y mensajes
permitir seguimiento desde frontend
Fase 4
revaluar si la observabilidad futura justifica calculation_run_item
aÃ±adir housekeeping de claims si hace falta
introducir paralelizaciÃ³n real
12. Resumen ejecutivo

Se adopta para payroll un modelo fÃ­sico donde:

payroll.payroll sigue siendo el resultado materializado;
payroll.payroll_warning concentra mensajes funcionales adheridos a la nÃ³mina;
payroll.calculation_run representa el launch persistido;
payroll.calculation_claim garantiza exclusiÃ³n concurrente por unidad;
payroll.calculation_run_message concentra mensajes operativos/tÃ©cnicos del run;
calculation_run_item no forma parte del nÃºcleo inicial;
el sistema queda preparado para diseÃ±ar launch sin acoplarlo prematuramente al motor real.

<!-- END FILE: ADR-031-Modelo-físico-de-payroll-launch- calculation-run-claims-y-mensajes.md -->


---

# FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md
<a name="file-adr-032-payroll-launch-workflow--s-ncrono--con-run-persistido-y-claims-por-unidad--estado-md"></a>

<!-- BEGIN FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md -->

ADR â€” Payroll Launch Workflow (sÃ­ncrono, con run persistido y claims por unidad)
Estado

Propuesto

Contexto

El bounded context payroll ya dispone de:

payroll.payroll como resultado materializado de una unidad de cÃ¡lculo, identificado por:
ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber
payroll.calculation_run como persistencia del lanzamiento tÃ©cnico;
payroll.calculation_claim como exclusiÃ³n concurrente por unidad;
payroll.payroll_warning para mensajes funcionales adheridos a la nÃ³mina;
payroll.calculation_run_message para mensajes operativos o tÃ©cnicos del run.

TambiÃ©n se ha fijado ya que:

una unidad es elegible para cÃ¡lculo si no existe nÃ³mina previa o si existe y estÃ¡ NOT_VALID;
una nÃ³mina en CALCULATED, EXPLICIT_VALIDATED o DEFINITIVE no debe recalcularse automÃ¡ticamente;
el endpoint actual POST /payrolls/calculate sigue siendo un stub temporal de validaciÃ³n del pipeline y no el contrato final del motor real.

El proyecto, ademÃ¡s, exige que cuando una operaciÃ³n no encaja como CRUD plano se modele como workflow explÃ­cito y que el naming refleje semÃ¡ntica de negocio real.

Problema

Se necesita implementar el launch de nÃ³mina como workflow real, de forma que:

reciba un contexto de lanzamiento;
resuelva una poblaciÃ³n objetivo;
la expanda a unidades reales de cÃ¡lculo;
filtre elegibilidad;
adquiera claims por unidad de forma segura;
invoque el calculador interno;
actualice el run persistido;
deje trazabilidad suficiente para consulta posterior.

Todo esto debe hacerse sin:

convertir el launch en motor de cÃ¡lculo;
acoplarlo al endpoint pÃºblico stub actual;
fijar todavÃ­a el contrato del motor real de reglas;
introducir asincronÃ­a o paralelizaciÃ³n real en la primera iteraciÃ³n.
DecisiÃ³n

Se adopta un Payroll Launch Workflow sÃ­ncrono, con estas caracterÃ­sticas:

crea un calculation_run persistido;
resuelve la poblaciÃ³n objetivo;
expande a unidades de cÃ¡lculo;
determina elegibilidad;
intenta adquirir claim por cada unidad elegible;
invoca un caso de uso interno de cÃ¡lculo por unidad;
actualiza contadores y estado del run;
registra mensajes del run cuando proceda;
devuelve runId y resumen del resultado.
Principio madre

Launch coordina.
Calculate materializa.
Claim excluye.
Run resume.

Unidad mÃ­nima de cÃ¡lculo

La unidad mÃ­nima de cÃ¡lculo queda fijada como:

ruleSystemCode
employeeTypeCode
employeeNumber
payrollPeriodCode
payrollTypeCode
presenceNumber

Esta unidad coincide con la identidad funcional de payroll.payroll y con la semÃ¡ntica ya fijada del dominio.

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
una lista explÃ­cita de empleados
una selecciÃ³n masiva simple dentro de un ruleSystemCode

No se fija aÃºn un DSL complejo de filtros.

Output del launch

El launch debe devolver:

runId
status
contadores agregados
timestamps principales
opcionalmente resumen

Y el sistema debe permitir consultar despuÃ©s el run persistido.

Flujo del launch
1. Crear run

Persistir calculation_run en estado:

REQUESTED

con:

contexto de ejecuciÃ³n
targetSelectionJson
contadores a cero
2. Cambiar a RUNNING

Al comenzar la ejecuciÃ³n real:

status = RUNNING
startedAt = now
3. Resolver poblaciÃ³n objetivo

Transformar targetSelection en empleados concretos.

4. Expandir a unidades de cÃ¡lculo

Por cada empleado objetivo, resolver las presences relevantes para:

payrollPeriodCode
payrollTypeCode

y generar unidades explÃ­citas:

empleado + periodo + tipo + presencia
5. Determinar elegibilidad

Una unidad es elegible si:

no existe payroll.payroll, o
existe y estÃ¡ NOT_VALID

Si existe y estÃ¡ en:

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
falla por unique: la unidad ya estÃ¡ en curso en otro run

Si estÃ¡ ya reclamada:

no se calcula
incrementa totalSkippedAlreadyClaimed
se registra calculation_run_message con contexto de unidad
7. Invocar cÃ¡lculo interno

Para cada unidad con claim adquirido:

invocar un caso de uso interno de cÃ¡lculo por unidad

No debe hacerse HTTP interno contra el endpoint stub actual.

8. Interpretar resultado

El cÃ¡lculo interno puede producir:

CALCULATED
NOT_VALID
error tÃ©cnico

Entonces:

CALCULATED incrementa totalCalculated
NOT_VALID incrementa totalNotValid
error tÃ©cnico incrementa totalErrors y genera calculation_run_message
9. Liberar claim

El claim de la unidad debe eliminarse al terminar su procesamiento, tanto si sale bien como si falla.

10. Cerrar run

Al finalizar todas las unidades:

finishedAt = now
status = COMPLETED si no hubo errores tÃ©cnicos
status = COMPLETED_WITH_ERRORS si hubo errores tÃ©cnicos parciales
status = FAILED solo si el launch falla globalmente antes de completar su ciclo mÃ­nimo
PolÃ­tica de concurrencia
Regla principal

La concurrencia se gobierna exclusivamente mediante payroll.calculation_claim.

La unique de payroll.payroll sigue siendo una defensa final de integridad, pero no es el mecanismo principal de coordinaciÃ³n.

Regla operativa

Dos launches simultÃ¡neos:

pueden coexistir;
no pueden procesar al mismo tiempo la misma unidad de cÃ¡lculo.
ImplementaciÃ³n base

La adquisiciÃ³n del claim se hace con insert atÃ³mico sobre la calculation key completa.

Naturaleza del cÃ¡lculo interno

El launch no debe depender del endpoint pÃºblico stub actual.

Debe usar un caso de uso interno del estilo:

CalculatePayrollUnitUseCase

o naming equivalente, orientado a negocio y no a detalle tÃ©cnico, siguiendo la guÃ­a de naming del proyecto.

Responsabilidad del cÃ¡lculo interno
materializar una unidad explÃ­cita
crear/reemplazar payroll.payroll segÃºn reglas ya fijadas
persistir payroll_warning cuando proceda
devolver resultado funcional de la unidad
Lo que no hace
no resuelve poblaciÃ³n
no gestiona claims
no crea runs
no resume progreso global
Mensajes del run

calculation_run_message se usa para:

errores tÃ©cnicos;
unidades omitidas por claim;
unidades omitidas por no elegibilidad cuando interese dejar rastro;
problemas de resoluciÃ³n de poblaciÃ³n;
incidencias globales del launch.

No debe usarse para modelar errores funcionales propios de la nÃ³mina.
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

La diferencia no estÃ¡ en la severidad, sino en la pertenencia semÃ¡ntica:

run
vs payroll
Primera iteraciÃ³n aceptada

La primera iteraciÃ³n del launch serÃ¡:

sÃ­ncrona
secuencial
con run persistido
con claims por unidad
con cÃ¡lculo interno por unidad
sin paralelizaciÃ³n real
sin asincronÃ­a
sin workers
JustificaciÃ³n

Esto permite validar:

semÃ¡ntica
integraciÃ³n
counters
exclusiÃ³n concurrente
wiring

sin abrir todavÃ­a el melÃ³n del motor real ni de la ejecuciÃ³n distribuida.

API pÃºblica recomendada
Crear launch

POST /payroll/calculation-runs/launch

Leer run

GET /payroll/calculation-runs/{runId}

Lecturas futuras opcionales
listar runs recientes
listar mensajes de run

No se considera canÃ³nico exponer todavÃ­a el cÃ¡lculo interno como API pÃºblica definitiva.

QuÃ© se rechaza explÃ­citamente

Se rechaza:

que launch invoque por HTTP al mismo backend como arquitectura permanente;
que launch haga de motor de cÃ¡lculo;
que calculate resuelva poblaciÃ³n objetivo;
que la concurrencia se gobierne solo por la unique de payroll.payroll;
que el endpoint stub actual de calculate se tome como contrato final del motor;
introducir ya paralelizaciÃ³n real o asincronÃ­a obligatoria.
Consecuencias
Positivas
launch claro y desacoplado
concurrencia segura por unidad
run consultable desde frontend
base sana para paralelizaciÃ³n futura
separaciÃ³n nÃ­tida entre ejecuciÃ³n y resultado
Costes
mÃ¡s wiring en aplicaciÃ³n
gestiÃ³n explÃ­cita de claims
necesidad de mantener contadores y estados del run
Resumen ejecutivo

Se adopta un launch sÃ­ncrono y secuencial que:

crea un calculation_run
resuelve poblaciÃ³n
expande a unidades
filtra elegibilidad
adquiere calculation_claim
invoca un cÃ¡lculo interno por unidad
actualiza contadores y estado del run
registra mensajes del run

Todo ello sin acoplar todavÃ­a el workflow al motor real de nÃ³mina.

<!-- END FILE: ADR-032-Payroll-Launch-Workflow-(síncrono, con-run-persistido-y-claims-por-unidad)-Estado.md -->


---

# FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md
<a name="file-adr-033-payrollobject-como-ra-z-metamodelo-can-nica-del-motor-n-mina-md"></a>

<!-- BEGIN FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md -->

ADR â€” PayrollObject como raÃ­z metamodelo canÃ³nica del motor de nÃ³mina
Estado

Propuesto

Contexto

El diseÃ±o del motor de nÃ³mina de B4RRHH estÃ¡ empezando a consolidarse alrededor de varios tipos de elementos configurables del dominio de payroll.

Inicialmente, la conversaciÃ³n se ha centrado en los conceptos de nÃ³mina, pero rÃ¡pidamente han aparecido tambiÃ©n otros candidatos naturales del mismo espacio funcional, como:

tablas
constantes
futuros objetos auxiliares o parametrizables del motor

Si el modelo parte directamente de payrollConcept como raÃ­z, existe el riesgo de:

sobredimensionar el concepto de nÃ³mina para que absorba responsabilidades que no le pertenecen
acabar creando metamodelos paralelos inconsistentes para tablas, constantes y otros elementos
mezclar identidad comÃºn con semÃ¡ntica especÃ­fica de un subtipo concreto

Por tanto, antes de profundizar en el modelado especÃ­fico de los conceptos, es necesario fijar una raÃ­z metamodelo comÃºn para todos los objetos configurables del motor.

DecisiÃ³n

Se introduce PayrollObject como raÃ­z metamodelo canÃ³nica del motor de nÃ³mina.

Todo elemento configurable del metamodelo de payroll deberÃ¡ modelarse primero como un PayrollObject, con una identidad funcional comÃºn basada en business keys.

La business key canÃ³nica de PayrollObject serÃ¡:

ruleSystemCode
objectTypeCode
objectCode

PayrollObject actuarÃ¡ como raÃ­z comÃºn para distintos tipos de objeto del dominio payroll, incluyendo al menos:

CONCEPT
TABLE
CONSTANT

El atributo canÃ³nico de identidad del objeto serÃ¡ objectCode.

Cuando se trabaje dentro de un subtipo concreto, podrÃ¡n usarse alias semÃ¡nticos de contexto, por ejemplo:

conceptCode
tableCode
constantCode

Sin embargo, esos nombres no definen identidades alternativas ni nuevas business keys. Son Ãºnicamente proyecciones semÃ¡nticas del mismo objectCode dentro del contexto de cada subtipo.

Consecuencias
Positivas
Se fija una raÃ­z comÃºn clara para el metamodelo del motor de nÃ³mina.
Se evita construir un modelo demasiado centrado exclusivamente en conceptos.
Se facilita la incorporaciÃ³n futura de tablas, constantes y otros objetos parametrizables sin rediseÃ±ar la base del modelo.
Se mantiene coherencia con las reglas generales de B4RRHH, donde la identidad pÃºblica se expresa mediante business keys funcionales y no mediante IDs tÃ©cnicos.
Se separa correctamente la identidad comÃºn del objeto de la semÃ¡ntica especÃ­fica de cada subtipo.
Costes o limitaciones
Obliga a introducir una capa de abstracciÃ³n adicional antes de modelar los subtipos concretos.
Requiere disciplina para no contaminar el modelo raÃ­z con propiedades especÃ­ficas de PayrollConcept u otros tipos.
Puede parecer mÃ¡s abstracto al inicio que arrancar directamente desde payrollConcept, aunque a medio plazo reduce deuda semÃ¡ntica.
No objetivos

Este ADR no define todavÃ­a:

las propiedades especÃ­ficas de PayrollConcept
el modelo de versionado de los objetos de payroll
la estrategia de cÃ¡lculo
la segmentaciÃ³n intrames
las reglas de cÃ¡lculo ni su representaciÃ³n
la implementaciÃ³n fÃ­sica en base de datos o APIs

Este ADR solo fija la raÃ­z metamodelo comÃºn y su identidad funcional.

Resumen ejecutivo

El motor de nÃ³mina de B4RRHH no se modelarÃ¡ partiendo directamente de conceptos aislados, sino desde una raÃ­z metamodelo comÃºn llamada PayrollObject.

La identidad funcional canÃ³nica serÃ¡:

ruleSystemCode
objectTypeCode
objectCode

Los subtipos como PayrollConcept, PayrollTable o PayrollConstant heredarÃ¡n esa identidad comÃºn.

Los nombres como conceptCode o tableCode se consideran alias semÃ¡nticos contextuales del objectCode, no nuevas business keys.

<!-- END FILE: ADR-033-PayrollObject-como-raíz-metamodelo-canónica-del-motor-nómina.md -->


---

# FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md
<a name="file-adr-034-modelo-sem-ntico-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md -->

# ADR-034 â€” Modelo semÃ¡ntico de `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-033 introduce `PayrollObject` como raÃ­z metamodelo canÃ³nica del motor de nÃ³mina y fija su identidad funcional comÃºn mediante la business key:

- `ruleSystemCode`
- `objectTypeCode`
- `objectCode`

Dentro de ese metamodelo comÃºn, el tipo de objeto `CONCEPT` requiere un modelo semÃ¡ntico propio que permita distinguir claramente:

- la identidad comÃºn heredada de `PayrollObject`
- la naturaleza estable del concepto de nÃ³mina
- las caracterÃ­sticas mutables o versionables (definidas en ADRs posteriores)

Sin esta separaciÃ³n, existe el riesgo de mezclar en un mismo nivel:

- identidad
- tipo de cÃ¡lculo
- presentaciÃ³n en recibo
- parÃ¡metros de cÃ¡lculo
- fuentes de datos
- efectos funcionales

Esto dificultarÃ­a la trazabilidad, la retroactividad y la comprensiÃ³n funcional del sistema de nÃ³mina.

El proyecto B4RRHH sigue principios claros:

- uso de business keys funcionales
- separaciÃ³n entre identidad estable y detalle mutable
- naming orientado a negocio
- evitar IDs tÃ©cnicos en APIs

---

## DecisiÃ³n

Se introduce `PayrollConcept` como subtipo semÃ¡ntico de `PayrollObject`.

`PayrollConcept` **no define una business key propia**.  
Hereda la identidad canÃ³nica de `PayrollObject`:

- `ruleSystemCode`
- `objectTypeCode`
- `objectCode`

Cuando `objectTypeCode = CONCEPT`, `objectCode` podrÃ¡ nombrarse como `conceptCode` a nivel semÃ¡ntico, sin crear una nueva identidad.

---

## Propiedades maestras de `PayrollConcept`

Las siguientes propiedades definen la **naturaleza semÃ¡ntica estable** del concepto:

- `conceptMnemonic`
- `calculationType`
- `functionalNature`
- `resultCompositionMode`
- `payslipOrderCode`

El modelo queda preparado para incorporar en el futuro:

- `functionalSubnature` (clasificaciÃ³n funcional secundaria)

---

## Significado de las propiedades

### `conceptMnemonic`
Alias semÃ¡ntico legible del concepto.

Uso:
- reglas
- documentaciÃ³n
- trazabilidad
- debugging

No forma parte de la business key.

---

### `calculationType`
Define la naturaleza del cÃ¡lculo del concepto.

Ejemplos:

- `DIRECT_AMOUNT`
- `QUANTITY_BY_RATE`
- `PRESENCE_VALUED`
- `AGGREGATE`
- `TECHNICAL_DERIVED`

---

### `functionalNature`
Define el papel funcional dentro de la nÃ³mina.

Valores iniciales:

- `EARNING`
- `DEDUCTION`
- `EMPLOYER_CHARGE`
- `BASE`
- `TOTAL`
- `TECHNICAL`

---

### `resultCompositionMode`
Define cÃ³mo se combinan mÃºltiples resultados parciales del concepto dentro de una nÃ³mina.

Evita asumir que siempre debe existir una Ãºnica lÃ­nea final.

---

### `payslipOrderCode`

Define la posiciÃ³n lÃ³gica en el recibo.

Reglas:

- `NULL` â†’ no se muestra en recibo
- valor informado â†’ se muestra

OrdenaciÃ³n:


payslipOrderCode + objectCode


Sustituye conceptualmente a un `visibleInPayslip`.

---

## Regla crÃ­tica de inmutabilidad

`calculationType` es **inmutable**.

Si un concepto cambia su naturaleza de cÃ¡lculo:

âž¡ï¸ **NO se versiona**  
âž¡ï¸ **Se crea un concepto nuevo**

Motivo:

- coherencia histÃ³rica
- retroactividad fiable
- trazabilidad clara
- comprensiÃ³n funcional

---

## Consecuencias

### Positivas

- separaciÃ³n clara entre identidad y semÃ¡ntica
- modelo mÃ¡s robusto frente a cambios
- mejor trazabilidad y retro
- base sÃ³lida para evoluciÃ³n futura
- coherencia con arquitectura B4RRHH

---

### Costes

- algunos cambios requieren nuevos conceptos
- mayor disciplina de modelado
- separaciÃ³n mÃ¡s estricta entre semÃ¡ntica y parametrizaciÃ³n

---

## No objetivos

Este ADR **NO define**:

- versionado de conceptos
- reglas de cÃ¡lculo
- segmentaciÃ³n intrames
- relaciones con tablas o constantes
- implementaciÃ³n en BBDD
- APIs

---

## Resumen ejecutivo

`PayrollConcept` es un subtipo semÃ¡ntico de `PayrollObject` sin identidad propia adicional.

Su nÃºcleo estable estÃ¡ formado por:

- `conceptMnemonic`
- `calculationType`
- `functionalNature`
- `resultCompositionMode`
- `payslipOrderCode`

`calculationType` es inmutable.

Los cambios en la naturaleza del concepto implican la creaciÃ³n de un nuevo concepto.

<!-- END FILE: ADR-034-Modelo-semántico-de-PayrollConcept.md -->


---

# FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md
<a name="file-adr-036-tipolog-as-can-nicas-de-c-lculo-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md -->

# ADR-036 â€” TipologÃ­as canÃ³nicas de cÃ¡lculo de `PayrollConcept`

## Estado
Aceptado

---

## Contexto

El proyecto B4RRHH define un motor de nÃ³mina basado en un metamodelo de objetos (`PayrollObject`), donde los conceptos de nÃ³mina (`PayrollConcept`) representan unidades funcionales de cÃ¡lculo dentro de una nÃ³mina.

Una de las decisiones clave del motor es evitar implementar lÃ³gica especÃ­fica por concepto mediante cÃ³digo, y en su lugar permitir que los conceptos se configuren a partir de un conjunto limitado de tipologÃ­as de cÃ¡lculo y reglas de composiciÃ³n.

Sin una tipologÃ­a clara:

- el sistema tenderÃ­a a crecer mediante lÃ³gica especÃ­fica por concepto;
- se perderÃ­a la capacidad de configuraciÃ³n;
- aumentarÃ­a la deuda tÃ©cnica;
- se dificultarÃ­a la trazabilidad y la retroactividad.

Por tanto, es necesario definir un conjunto reducido, estable y expresivo de **tipos de cÃ¡lculo canÃ³nicos** que cubran la mayorÃ­a de casos reales sin inflar el modelo.

---

## DecisiÃ³n

Se definen las siguientes tipologÃ­as canÃ³nicas de cÃ¡lculo para `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Cada tipo representa una **forma fundamental de cÃ¡lculo**, no un caso de negocio concreto.

---

## Principio rector

El tipo de cÃ¡lculo describe el **operador principal** del concepto.

No describe:
- el origen de los datos;
- la semÃ¡ntica concreta del concepto (ej. â€œsalario baseâ€, â€œIRPFâ€);
- ni la forma especÃ­fica en que se obtienen sus operandos.

---

## TipologÃ­as definidas

### 1. `DIRECT_AMOUNT`

#### DefiniciÃ³n
El resultado del concepto es un importe directo ya resuelto.

#### Forma general

resultado = amount


#### CaracterÃ­sticas
- No depende de otros operandos estructurados.
- Representa un valor final ya calculado o informado.

#### Ejemplos
- ajuste manual
- plus fijo mensual
- cuantÃ­a fija por tabla
- regularizaciÃ³n directa

---

### 2. `RATE_BY_QUANTITY`

#### DefiniciÃ³n
El resultado del concepto se obtiene como el producto de una cantidad por un precio.

#### Forma general

resultado = quantity Ã— rate


#### CaracterÃ­sticas
- Generaliza mÃºltiples casos de negocio:
  - dÃ­as Ã— precio dÃ­a
  - horas Ã— precio hora
  - unidades Ã— tarifa
- No define cÃ³mo se obtienen `quantity` ni `rate`.

#### Ejemplos
- salario base diario
- horas extra
- plus por dÃ­a trabajado
- dietas
- kilometraje

#### Nota importante
Conceptos tradicionalmente considerados como â€œbasados en presenciaâ€ se modelan como casos particulares de este tipo, donde la cantidad representa dÃ­as computables.

---

### 3. `PERCENTAGE`

#### DefiniciÃ³n
El resultado del concepto se obtiene aplicando un porcentaje sobre una base.

#### Forma general

resultado = base Ã— percentage


#### CaracterÃ­sticas
- Separa claramente la base del porcentaje.
- No define cÃ³mo se obtiene el porcentaje.

#### Ejemplos
- cotizaciÃ³n a la seguridad social
- IRPF
- complementos porcentuales

#### Nota importante
Incluso cuando el porcentaje se obtiene mediante lÃ³gica compleja (ej. IRPF), el concepto sigue perteneciendo a esta tipologÃ­a.  
La complejidad se desplaza a la obtenciÃ³n del porcentaje, no al tipo de cÃ¡lculo.

---

### 4. `AGGREGATE`

#### DefiniciÃ³n
El resultado del concepto se obtiene combinando resultados de otros conceptos ya calculados, pudiendo invertir el signo de cada contribuyente individualmente.

#### Forma general

resultado = SUM(feed_i Ã— sign_i)

donde `sign_i` es +1 si la relaciÃ³n de feed tiene `invert_sign = false`, y âˆ’1 si tiene `invert_sign = true`.

#### El flag `invert_sign` en la relaciÃ³n de feed

El flag `invert_sign` reside en la **relaciÃ³n de feed** (no en el concepto fuente). Esto permite que un mismo concepto contribuya positivamente a un agregado y negativamente a otro:

| Concepto fuente       | Feeds aggregate | `invert_sign` | Efecto         |
|-----------------------|-----------------|---------------|----------------|
| 101 SALARIO_BASE      | 970             | false         | + importe      |
| 101 SALARIO_BASE      | 990             | false         | + importe      |
| concepto deducciÃ³n X  | 980             | false         | + importe      |
| concepto deducciÃ³n X  | 990             | true          | âˆ’ importe      |

#### Modelo de grafo plano (flat graph)

Los conceptos hoja alimentan **directamente** tanto su agregado lateral (devengos o deducciones) como el agregado de lÃ­quido neto (990). El concepto 990 **nunca depende de 970 ni de 980**; agrega los mismos conceptos hoja que ellos.

```
EARNING leaf â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º  970 (invert=false)
                    â””â”€â”€â”€â”€â–º  990 (invert=false)

DEDUCTION leaf â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º  980 (invert=false)
                    â””â”€â”€â”€â”€â–º  990 (invert=true)
```

Esto evita dependencias en cascada entre agregados y garantiza que el grafo de cÃ¡lculo sea siempre un DAG sin nodos intermedios de agregado encadenados.

#### ActivaciÃ³n explÃ­cita

Los conceptos AGGREGATE **no se activan automÃ¡ticamente** por el hecho de que sus fuentes estÃ©n activadas. Requieren una fila explÃ­cita en `payroll_object_activation`, igual que el resto de conceptos.

#### CaracterÃ­sticas
- No opera sobre datos primarios, sino sobre resultados previos.
- Representa composiciÃ³n o acumulaciÃ³n con signo controlado a nivel de relaciÃ³n.

#### Ejemplos
- total devengos (970)
- total deducciones (980)
- lÃ­quido a pagar / neto (990)
- bases de cotizaciÃ³n
- bases fiscales

#### Nota importante
La estrategia de agregaciÃ³n con signo queda definida aquÃ­. La forma de registrar las relaciones de feed y persistirlas se trata en los ADRs de infraestructura del motor de nÃ³mina.

---

## `FunctionalNature` para conceptos agregados totales

Los conceptos de tipo `AGGREGATE` que representan totales de nÃ³mina reciben valores especÃ­ficos en el enum `FunctionalNature` para que el frontend pueda distinguirlos de las lÃ­neas de detalle al renderizar el recibo de salario.

Se aÃ±aden los siguientes valores al enum:

| Valor              | SemÃ¡ntica                                      | Concepto tÃ­pico |
|--------------------|------------------------------------------------|-----------------|
| `TOTAL_EARNING`    | Suma de todos los devengos                     | 970             |
| `TOTAL_DEDUCTION`  | Suma de todas las deducciones                  | 980             |
| `NET_PAY`          | LÃ­quido a pagar (devengos âˆ’ deducciones)       | 990             |

Estos tres valores coexisten con los valores preexistentes (`EARNING`, `DEDUCTION`, `BASE`, `INFORMATIONAL`).

La `FunctionalNature` es un atributo de presentaciÃ³n/semÃ¡ntica del concepto; no altera la lÃ³gica de cÃ¡lculo. Un concepto con `calculationType = AGGREGATE` y `functionalNature = NET_PAY` ejecuta exactamente la misma operaciÃ³n `SUM(feed_i Ã— sign_i)` que cualquier otro AGGREGATE.

---

## Reglas de diseÃ±o

### 1. Minimalismo tipolÃ³gico

No se crearÃ¡n nuevos tipos de cÃ¡lculo por cada caso de negocio frecuente.

Ejemplo descartado:
- `PRESENCE_BASED`

Motivo:
- no representa una operaciÃ³n distinta;
- describe una forma de obtener un operando (`quantity`), no un tipo de cÃ¡lculo.

---

### 2. SeparaciÃ³n de responsabilidades

Se separan claramente:

- tipo de cÃ¡lculo â†’ define la operaciÃ³n
- resoluciÃ³n de operandos â†’ define de dÃ³nde salen los datos

Esta separaciÃ³n es fundamental para:

- evitar explosiÃ³n de tipos;
- permitir configuraciÃ³n;
- facilitar reutilizaciÃ³n.

---

### 3. Composicionalidad

Los tipos de cÃ¡lculo deben permitir que los operandos provengan de resultados de otros conceptos.

Esto habilita:

- conceptos tÃ©cnicos intermedios;
- cadenas de cÃ¡lculo reutilizables;
- construcciÃ³n incremental del resultado de nÃ³mina.

---

### 4. Inmutabilidad del tipo

El `calculationType` de un `PayrollConcept` es inmutable.

#### Consecuencia
Si un concepto cambia su naturaleza de cÃ¡lculo:
- no se versiona;
- se crea un nuevo concepto.

#### MotivaciÃ³n
- preservar coherencia histÃ³rica;
- evitar ambigÃ¼edad semÃ¡ntica;
- simplificar retroactividad.

---

## Consecuencias

### Positivas

- modelo estable y predecible;
- reducciÃ³n drÃ¡stica de lÃ³gica especÃ­fica por concepto;
- alta capacidad de configuraciÃ³n;
- base sÃ³lida para evoluciÃ³n del motor;
- alineaciÃ³n con arquitectura hexagonal y metamodelo del proyecto.

---

### Costes

- necesidad de modelar correctamente operandos y sources;
- mayor esfuerzo inicial de diseÃ±o;
- algunos casos complejos requerirÃ¡n conceptos tÃ©cnicos adicionales en lugar de lÃ³gica directa.

---

## No objetivos

Este ADR no define:

- cÃ³mo se resuelven los operandos (`sources`);
- cÃ³mo se versionan las reglas;
- el orden de ejecuciÃ³n de los conceptos;
- el modelo de persistencia;
- la API de configuraciÃ³n.

---

## Resumen ejecutivo

Se establece un conjunto mÃ­nimo y completo de tipologÃ­as de cÃ¡lculo para `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Estas tipologÃ­as representan las formas fundamentales de cÃ¡lculo del motor y permiten modelar la mayorÃ­a de los conceptos de nÃ³mina mediante configuraciÃ³n, sin necesidad de lÃ³gica especÃ­fica por concepto.

El modelo se apoya en la separaciÃ³n entre:

- operaciÃ³n (tipo de cÃ¡lculo)
- resoluciÃ³n de datos (operandos)

lo que habilita un motor flexible, composicional y extensible.

<!-- END FILE: ADR-036-Tipologías-canónicas-de-cálculo-de-payrollconcept.md -->


---

# FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md
<a name="file-adr-037-sources-y-resoluci-n-de-operandos-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md -->

Vamos a por el siguiente bloque clave. Este ADR es el que convierte las tipologÃ­as en motor real configurable.

# ADR-037 â€” Sources y resoluciÃ³n de operandos en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-036 define las tipologÃ­as canÃ³nicas de cÃ¡lculo de `PayrollConcept`:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

Estas tipologÃ­as describen Ãºnicamente la **forma del cÃ¡lculo**, pero no especifican:

- de dÃ³nde provienen los valores necesarios;
- cÃ³mo se resuelven los operandos en tiempo de ejecuciÃ³n.

Sin una capa explÃ­cita de resoluciÃ³n de operandos:

- el sistema tenderÃ­a a introducir lÃ³gica especÃ­fica por concepto;
- se perderÃ­a configurabilidad;
- se dificultarÃ­a la reutilizaciÃ³n;
- aumentarÃ­a el acoplamiento entre cÃ¡lculo y origen de datos.

Por tanto, es necesario definir un modelo claro de **sources de operandos** que permita desacoplar completamente:

- la operaciÃ³n (tipo de cÃ¡lculo)
- el origen de los datos

---

## DecisiÃ³n

Se introduce el concepto de **source de operando**, que define el origen del valor utilizado en un cÃ¡lculo.

Cada operando de un `PayrollConcept` se resuelve mediante:

- un `sourceType`
- una referencia asociada (segÃºn el tipo)

---

## Principio rector

Un operando no contiene un valor directo, sino una **instrucciÃ³n de resoluciÃ³n**.

---

## Sources canÃ³nicos iniciales

Se definen los siguientes tipos de source:

- `INPUT`
- `CONSTANT`
- `TABLE`
- `CONCEPT`
- `EMPLOYEE_DATA`
- `PERIOD_DATA`
- `SEGMENT_DATA`

---

## DefiniciÃ³n de cada source

### 1. `INPUT`

#### DescripciÃ³n
Valor informado externamente para el cÃ¡lculo.

#### Ejemplos
- horas extra introducidas
- unidades manuales
- importes excepcionales

#### Uso tÃ­pico
- `quantity`
- `amount`

---

### 2. `CONSTANT`

#### DescripciÃ³n
Valor fijo parametrizado en el sistema.

#### Ejemplos
- importe fijo mensual
- porcentaje fijo
- divisor estÃ¡ndar (ej. 30)

#### Uso tÃ­pico
- `rate`
- `percentage`
- `amount`

---

### 3. `TABLE`

#### DescripciÃ³n
Valor obtenido a partir de una tabla parametrizada.

#### Ejemplos
- salario por categorÃ­a
- tarifa por hora
- porcentaje por tramo

#### Uso tÃ­pico
- `rate`
- `percentage`
- `amount`

---

### 4. `CONCEPT`

#### DescripciÃ³n
Valor obtenido a partir del resultado de otro `PayrollConcept`.

#### Ejemplos
- `BASE_CC`
- `BASE_IRPF`
- `DIAS_PRESENCIA`
- `PRECIO_DIA`

#### Uso tÃ­pico
- cualquier operando

#### Nota clave
Este source habilita la **composiciÃ³n del motor**, permitiendo construir cadenas de cÃ¡lculo reutilizables.

---

### 5. `EMPLOYEE_DATA`

#### DescripciÃ³n
Dato estructural del empleado.

#### Ejemplos
- porcentaje de jornada
- categorÃ­a profesional
- tipo de contrato

#### Uso tÃ­pico
- inputs para tablas
- cÃ¡lculo de valores derivados

---

### 6. `PERIOD_DATA`

#### DescripciÃ³n
Dato asociado al perÃ­odo completo de cÃ¡lculo.

#### Ejemplos
- dÃ­as del mes
- aÃ±o/mes
- nÃºmero de pagas

#### Uso tÃ­pico
- `quantity`
- cÃ¡lculos base

---

### 7. `SEGMENT_DATA`

#### DescripciÃ³n
Dato asociado a un tramo homogÃ©neo de cÃ¡lculo dentro del perÃ­odo.

#### Ejemplos
- dÃ­as del segmento
- jornada vigente en el segmento
- condiciones activas en el tramo

#### Uso tÃ­pico
- `quantity`
- cÃ¡lculos intraperiodo

---

## ResoluciÃ³n de operandos por tipo de cÃ¡lculo

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
La resoluciÃ³n de miembros no se modela como source, sino mediante estrategias de agregaciÃ³n definidas en ADR posterior.

---

## Regla fundamental

Los tipos de cÃ¡lculo **no contienen lÃ³gica de negocio especÃ­fica**, sino que delegan completamente la obtenciÃ³n de valores en los sources.

---

## Composicionalidad del motor

El uso de `CONCEPT` como source permite:

- construir conceptos tÃ©cnicos reutilizables;
- encadenar cÃ¡lculos;
- separar lÃ³gica compleja en piezas simples;
- mejorar trazabilidad y debugging.

---

## Ejemplo conceptual

### Salario base


quantity = CONCEPT(DIAS_PRESENCIA)
rate = CONCEPT(PRECIO_DIA)
resultado = quantity Ã— rate


---

### IRPF


base = CONCEPT(BASE_IRPF)
percentage = CONCEPT(TIPO_IRPF_EFECTIVO)
resultado = base Ã— percentage


---

## Consecuencias

### Positivas

- desacoplamiento total entre cÃ¡lculo y origen de datos;
- alta configurabilidad;
- reutilizaciÃ³n de lÃ³gica;
- facilidad para introducir conceptos tÃ©cnicos;
- base para motor declarativo.

---

### Costes

- necesidad de definir correctamente catÃ¡logo de conceptos tÃ©cnicos;
- mayor complejidad conceptual inicial;
- necesidad de validaciones fuertes entre tipos y sources.

---

## No objetivos

Este ADR no define:

- modelo fÃ­sico de persistencia de sources;
- resoluciÃ³n concreta de tablas;
- implementaciÃ³n de motor de cÃ¡lculo;
- orden de ejecuciÃ³n de conceptos;
- versionado de parÃ¡metros.

---

## Resumen ejecutivo

Se define un modelo de resoluciÃ³n de operandos basado en sources tipados.

Cada operando de un concepto se resuelve mediante un source, desacoplando completamente:

- el tipo de cÃ¡lculo
- el origen de los datos

Este modelo permite construir un motor composicional, reutilizable y altamente con

<!-- END FILE: ADR-037-Sources-y-resolución-de-operandos-en-PayrollConcept.md -->


---

# FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md
<a name="file-adr-038-estrategias-de-agregaci-n-y-relaciones-de-alimentaci-n-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md -->

# ADR-038 â€” Estrategias de agregaciÃ³n y relaciones de alimentaciÃ³n en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

El ADR-036 define las tipologÃ­as canÃ³nicas de cÃ¡lculo:

- `DIRECT_AMOUNT`
- `RATE_BY_QUANTITY`
- `PERCENTAGE`
- `AGGREGATE`

El ADR-037 define la resoluciÃ³n de operandos mediante sources tipados.

Sin embargo, el tipo `AGGREGATE` requiere una definiciÃ³n adicional:

- cÃ³mo se determinan los conceptos que participan en el agregado;
- dÃ³nde reside la responsabilidad de dicha pertenencia;
- cÃ³mo evitar modelos frÃ¡giles basados en listas manuales.

En nÃ³mina real existen dos patrones claramente diferenciados:

1. Bases o acumulados donde cada concepto decide si participa  
2. Totales o subtotales donde la pertenencia se deriva automÃ¡ticamente

Es necesario modelar ambas realidades sin mezclarlas.

---

## DecisiÃ³n

Se definen dos estrategias canÃ³nicas de agregaciÃ³n para `AGGREGATE`:

- `FEED_BY_SOURCE`
- `SELECT_BY_RULE`

Estas estrategias determinan cÃ³mo se construye el conjunto de miembros del agregado.

---

## Principio rector

La pertenencia a un agregado puede definirse:

- desde el concepto origen (semÃ¡ntica declarativa del concepto)
- o desde el agregado destino (regla de selecciÃ³n)

Ambas aproximaciones son necesarias y no son equivalentes.

---

## DefiniciÃ³n de `AGGREGATE`

Un `AGGREGATE` es un concepto cuyo resultado se obtiene combinando resultados de otros conceptos ya calculados.

### OperaciÃ³n inicial soportada
- `SUM`

---

## Estrategias de membership

---

### 1. `FEED_BY_SOURCE`

#### DefiniciÃ³n
La pertenencia al agregado se declara en el concepto origen.

#### Modelo conceptual
Cada concepto define a quÃ© agregados alimenta.

#### Ejemplo
- `SALARIO_BASE` alimenta `BASE_CC`
- `PLUS_TRANSPORTE` no alimenta `BASE_CC`
- `PRORRATA_EXTRA` alimenta `BASE_IRPF`

---

#### MotivaciÃ³n

La semÃ¡ntica relevante en muchos casos pertenece al concepto:

- cotiza / no cotiza
- tributa / no tributa
- alimenta base / no alimenta

Esta informaciÃ³n es intrÃ­nseca al concepto, no al agregado.

---

#### ResoluciÃ³n en runtime

Para calcular un agregado:

1. se evalÃºan todos los conceptos;
2. se seleccionan aquellos con relaciÃ³n activa hacia el target;
3. se combinan segÃºn `feedMode`.

---

#### RelaciÃ³n de alimentaciÃ³n

Se introduce la relaciÃ³n conceptual:

### `ConceptFeedRelation`

Campos mÃ­nimos:

- `sourceConceptCode`
- `targetObjectCode`
- `feedMode`
- `feedValue` (opcional)
- `effectiveFrom`
- `effectiveTo`

---

#### Modos iniciales

- `INCLUDE` â†’ aporta el 100% del importe
- `PERCENTAGE` â†’ aporta un porcentaje del importe

---

#### Uso recomendado

- bases de cotizaciÃ³n
- bases fiscales
- acumulados tÃ©cnicos
- provisiones
- cualquier agregado donde la pertenencia dependa del concepto origen

---

#### Ventajas

- semÃ¡ntica clara y localizada;
- menor riesgo de omisiones al introducir nuevos conceptos;
- alineaciÃ³n con lÃ³gica de negocio real.

---

#### Costes

- la composiciÃ³n del agregado no es visible directamente desde el destino;
- requiere resoluciÃ³n inversa en runtime.

---

---

### 2. `SELECT_BY_RULE`

#### DefiniciÃ³n
La pertenencia al agregado se define mediante una regla en el propio agregado.

---

#### Modelo conceptual
El agregado define una condiciÃ³n de selecciÃ³n sobre el conjunto de conceptos.

---

#### Ejemplos

- `TOTAL_DEVENGOS` â†’ todos los conceptos con `functionalNature = EARNING`
- `TOTAL_DEDUCCIONES` â†’ todos los conceptos con `functionalNature = DEDUCTION`

---

#### MotivaciÃ³n

Existen agregados cuya composiciÃ³n:

- no debe mantenerse manualmente;
- debe adaptarse automÃ¡ticamente a nuevos conceptos;
- depende de la naturaleza funcional, no de decisiones individuales.

---

#### ParametrizaciÃ³n mÃ­nima

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
- agrupaciones lÃ³gicas
- bloques de presentaciÃ³n

---

#### Ventajas

- evita mantenimiento manual;
- escala automÃ¡ticamente con nuevos conceptos;
- reduce riesgo de errores por omisiÃ³n.

---

#### Costes

- menor control individual por concepto;
- requiere definiciÃ³n clara de taxonomÃ­as funcionales.

---

## Regla clave de diseÃ±o

No se modelarÃ¡ `AGGREGATE` como una lista fija de miembros en todos los casos.

---

## Criterios de uso

| Tipo de agregado        | Estrategia recomendada |
|------------------------|------------------------|
| Bases (cotizaciÃ³n)     | FEED_BY_SOURCE         |
| Bases (fiscalidad)     | FEED_BY_SOURCE         |
| Acumulados tÃ©cnicos    | FEED_BY_SOURCE         |
| Totales funcionales    | SELECT_BY_RULE         |
| Subtotales             | SELECT_BY_RULE         |

---

## InteracciÃ³n con otros ADR

- ADR-036 define el tipo `AGGREGATE`
- ADR-037 define cÃ³mo se resuelven operandos
- Este ADR define cÃ³mo se resuelven los miembros

---

## Consecuencias

### Positivas

- modelo robusto frente a crecimiento del catÃ¡logo;
- separaciÃ³n clara de responsabilidades;
- alineaciÃ³n con lÃ³gica real de nÃ³mina;
- soporte tanto para control fino como para automatizaciÃ³n.

---

### Costes

- mayor complejidad conceptual;
- necesidad de implementar dos estrategias en runtime;
- necesidad de definir correctamente `functionalNature`.

---

## No objetivos

Este ADR no define:

- ejecuciÃ³n del motor de cÃ¡lculo;
- orden de evaluaciÃ³n de conceptos;
- resoluciÃ³n de conflictos entre feeds;
- filtros avanzados o condiciones complejas;
- modelo fÃ­sico de persistencia.

---

## Resumen ejecutivo

Se establecen dos estrategias complementarias para la construcciÃ³n de agregados:

- `FEED_BY_SOURCE`: la pertenencia se declara en el concepto origen  
- `SELECT_BY_RULE`: la pertenencia se define mediante reglas en el agregado

Ambas estrategias son necesarias para modelar correctamente:

- bases tÃ©cnicas (controladas por concepto)
- totales funcionales (derivados automÃ¡ticamente)

Este modelo evita listas manuales frÃ¡giles y permite construir un motor de nÃ³mina flexible, escalable y alineado con el dominio.

<!-- END FILE: ADR-038-Estrategias-de-agregación-y-relaciones-de-alimentación-en-PayrollConcept.md -->


---

# FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md
<a name="file-adr-039-modelo-dependencias-y-grafo-de-c-lculo-de-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md -->

# ADR-039 â€” Modelo de dependencias y grafo de cÃ¡lculo de `PayrollConcept`

## Estado
Propuesto

---

## Contexto

Los ADR previos han establecido:

- ADR-036 â€” TipologÃ­as canÃ³nicas de cÃ¡lculo (`DIRECT_AMOUNT`, `RATE_BY_QUANTITY`, `PERCENTAGE`, `AGGREGATE`)
- ADR-037 â€” ResoluciÃ³n de operandos mediante sources tipados
- ADR-038 â€” Estrategias de agregaciÃ³n (`FEED_BY_SOURCE`, `SELECT_BY_RULE`)

Estas decisiones permiten que un `PayrollConcept`:

- consuma resultados de otros conceptos (`source = CONCEPT`);
- participe en agregados;
- sea utilizado como base o componente de otros cÃ¡lculos.

Como consecuencia, el conjunto de conceptos deja de ser independiente y pasa a formar una red de relaciones.

Es necesario formalizar esta red como un **modelo explÃ­cito de dependencias**, base para cualquier estrategia de ejecuciÃ³n posterior.

---

## DecisiÃ³n

Se define un modelo explÃ­cito de dependencias entre `PayrollConcept` y su representaciÃ³n como un **grafo dirigido de cÃ¡lculo**.

---

## DefiniciÃ³n de dependencia

Se establece que:

> Un `PayrollConcept` A depende de otro concepto B si el cÃ¡lculo de A requiere que B haya sido previamente calculado.

Esta relaciÃ³n se representa como una arista dirigida:


B â†’ A


donde B debe evaluarse antes que A.

---

## Tipos de dependencia

Se definen tres tipos canÃ³nicos de dependencia.

---

### 1. `OPERAND_DEPENDENCY`

#### DefiniciÃ³n
Se produce cuando un operando de un concepto se resuelve mediante `source = CONCEPT`.

#### Ejemplos
- `SALARIO_BASE` depende de `DIAS_PRESENCIA`
- `SALARIO_BASE` depende de `PRECIO_DIA`
- `IRPF` depende de `BASE_IRPF`
- `IRPF` depende de `TIPO_IRPF_EFECTIVO`

#### Origen
ADR-037 â€” Sources y resoluciÃ³n de operandos

#### Naturaleza
- explÃ­cita
- declarada directamente en la configuraciÃ³n del concepto

---

### 2. `FEED_DEPENDENCY`

#### DefiniciÃ³n
Se produce cuando un `AGGREGATE` con estrategia `FEED_BY_SOURCE` recibe alimentaciÃ³n desde conceptos origen.

#### Ejemplos
- `BASE_CC` depende de `SALARIO_BASE`
- `BASE_IRPF` depende de `PRORRATA_EXTRA`

#### Origen
ADR-038 â€” Estrategias de agregaciÃ³n

#### Naturaleza
- derivada de relaciones de alimentaciÃ³n (`ConceptFeedRelation`)
- definida en el concepto origen

---

### 3. `SELECTION_DEPENDENCY`

#### DefiniciÃ³n
Se produce cuando un `AGGREGATE` con estrategia `SELECT_BY_RULE` depende de los conceptos que cumplen su regla de selecciÃ³n.

#### Ejemplos
- `TOTAL_DEVENGOS` depende de todos los conceptos con `functionalNature = EARNING`
- `TOTAL_DEDUCCIONES` depende de todos los conceptos con `functionalNature = DEDUCTION`

#### Origen
ADR-038 â€” Estrategias de agregaciÃ³n

#### Naturaleza
- derivada
- dependiente del catÃ¡logo de conceptos y del contexto de evaluaciÃ³n

---

## Grafo de cÃ¡lculo

### DefiniciÃ³n

El conjunto de conceptos y sus dependencias forma un **grafo dirigido de cÃ¡lculo** donde:

- los nodos representan `PayrollConcept`
- las aristas representan dependencias

---

### InterpretaciÃ³n

Una arista:


B â†’ A


significa:

> El concepto B debe ser evaluado antes que el concepto A.

---

## Propiedades del grafo

---

### 1. Aciclicidad

El grafo debe ser un **grafo dirigido acÃ­clico (DAG)**.

#### Consecuencia
No se permiten ciclos de dependencias entre conceptos.

#### Ejemplo invÃ¡lido
- `BASE_CC` depende de `TOTAL_DEVENGOS`
- `TOTAL_DEVENGOS` depende de `SALARIO_BASE`
- `SALARIO_BASE` depende de `BASE_CC`

---

### 2. Dependencias explÃ­citas o derivables

Toda dependencia debe ser:

- explÃ­cita (operandos con `source = CONCEPT`)
- o derivable (feeds o reglas de selecciÃ³n)

#### Regla
No se permiten dependencias implÃ­citas o no declaradas.

---

### 3. Completitud estructural

El sistema debe ser capaz de:

- construir el conjunto completo de dependencias
- a partir de la configuraciÃ³n del modelo

antes de cualquier ejecuciÃ³n.

---

### 4. Independencia del contexto de ejecuciÃ³n

El modelo de dependencias es una propiedad estructural del sistema y:

- no depende de un empleado concreto
- no depende de una nÃ³mina concreta

---

## Grafo configurado y grafo efectivo

Se distinguen dos niveles de representaciÃ³n.

---

### Grafo configurado

Representa:

- todas las dependencias potenciales derivadas del metamodelo

Uso:

- validaciÃ³n estructural
- detecciÃ³n de ciclos
- anÃ¡lisis de impacto
- tooling

---

### Grafo efectivo

Representa:

- las dependencias realmente activas en una ejecuciÃ³n concreta

Uso:

- ejecuciÃ³n del cÃ¡lculo
- trazabilidad
- debugging

---

## ConstrucciÃ³n del grafo

El grafo se construye a partir de:

1. dependencias por operandos (`source = CONCEPT`)
2. relaciones de alimentaciÃ³n (`FEED_BY_SOURCE`)
3. reglas de selecciÃ³n (`SELECT_BY_RULE`)

---

## Consecuencias

---

### Positivas

- orden de cÃ¡lculo derivable automÃ¡ticamente  
- detecciÃ³n temprana de ciclos  
- trazabilidad completa del cÃ¡lculo  
- base para ejecuciÃ³n declarativa  
- desacoplamiento entre conceptos  

---

### Costes

- mayor complejidad conceptual  
- necesidad de validaciÃ³n estructural  
- necesidad de herramientas de inspecciÃ³n del grafo  

---

## Riesgos identificados

---

### 1. Ciclos indirectos

Dependencias encadenadas pueden generar ciclos no triviales.

---

### 2. Dependencias mal definidas

Errores en configuraciÃ³n pueden generar dependencias inexistentes o incoherentes.

---

### 3. SelecciÃ³n dinÃ¡mica no controlada

`SELECT_BY_RULE` debe mantenerse dentro de un conjunto acotado de reglas para evitar comportamientos impredecibles.

---

## No objetivos

Este ADR no define:

- estrategia de ejecuciÃ³n del grafo  
- orden de evaluaciÃ³n concreto  
- paralelizaciÃ³n  
- caching  
- segmentaciÃ³n temporal  
- activaciÃ³n contextual de conceptos  

---

## Resumen ejecutivo

Se establece que los `PayrollConcept` forman un grafo dirigido de dependencias donde:

- los nodos representan conceptos  
- las aristas representan relaciones de dependencia  

Se definen tres tipos de dependencia:

- `OPERAND_DEPENDENCY`
- `FEED_DEPENDENCY`
- `SELECTION_DEPENDENCY`

El grafo debe ser acÃ­clico, explÃ­cito y completamente derivable de la configuraciÃ³n.

Este modelo constituye la base para la futura ejecuciÃ³n del motor de cÃ¡lculo.

<!-- END FILE: ADR-039-Modelo-dependencias-y-grafo-de-cálculo-de-PayrollConcept.md -->


---

# FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md
<a name="file-adr-040-macro-grafo-activaci-n-de-conceptos-y-plan-de-c-lculo-efectivo-md"></a>

<!-- BEGIN FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md -->

# ADR-040 â€” Macro-grafo, activaciÃ³n de conceptos y plan de cÃ¡lculo efectivo

## Estado
Implementado (ver ADR-045)

---

## Contexto

El ADR-039 define que los `PayrollConcept` forman un grafo dirigido acÃ­clico (DAG) basado en dependencias:

- `OPERAND_DEPENDENCY`
- `FEED_DEPENDENCY`
- `SELECTION_DEPENDENCY`

Sin embargo, este grafo:

- representa **todas las dependencias posibles**
- no distingue quÃ© conceptos deben calcularse en una ejecuciÃ³n concreta

Para poder ejecutar el motor, es necesario definir:

1. cÃ³mo se determina quÃ© conceptos participan
2. cÃ³mo se reduce el grafo al subconjunto relevante
3. cÃ³mo se obtiene un orden de cÃ¡lculo vÃ¡lido

---

## DecisiÃ³n

Se introduce un modelo de **macro-grafo + activaciÃ³n + plan efectivo** en tres fases:

1. **Macro-grafo configurado**
2. **ActivaciÃ³n de conceptos**
3. **Plan de cÃ¡lculo efectivo**

---

## 1. Macro-grafo configurado

### DefiniciÃ³n

El macro-grafo es:

> el grafo completo de todos los `PayrollConcept` y sus dependencias estructurales

Incluye:

- todos los conceptos definidos en el sistema
- todas las dependencias posibles derivadas del modelo

---

### Propiedades

- es global al `ruleSystem`
- es independiente de empleado o periodo
- es estÃ¡tico salvo cambios de configuraciÃ³n
- es validado estructuralmente (ciclos, coherencia)

---

### Uso

- validaciÃ³n del sistema
- anÃ¡lisis de impacto
- tooling (visualizaciÃ³n, debugging)
- base para generaciÃ³n de planes efectivos

---

## 2. ActivaciÃ³n de conceptos

### DefiniciÃ³n

La activaciÃ³n determina:

> quÃ© conceptos deben calcularse en una ejecuciÃ³n concreta

---

### Tipos de activaciÃ³n

Se definen tres mecanismos canÃ³nicos:

---

#### 2.1 ActivaciÃ³n explÃ­cita (`EXPLICIT`)

Conceptos solicitados directamente por el sistema.

#### Ejemplos

- cÃ¡lculo de `NETO_A_PERCIBIR`
- cÃ¡lculo de `TOTAL_DEVENGOS`

---

#### 2.2 ActivaciÃ³n por dependencia (`DEPENDENCY`)

Se activan todos los conceptos necesarios para calcular los conceptos explÃ­citos.

#### Regla

Si A estÃ¡ activado y A depende de B, entonces B se activa.

---

#### 2.3 ActivaciÃ³n por selecciÃ³n (`SELECTION`)

Se activan conceptos seleccionados dinÃ¡micamente por reglas de agregaciÃ³n.

#### Ejemplos

- todos los conceptos con `functionalNature = EARNING`
- todos los conceptos marcados como cotizables

---

### Resultado de la activaciÃ³n

Se obtiene:

> un subconjunto de nodos del macro-grafo llamado **conjunto activo de conceptos**

---

## 3. Subgrafo efectivo

### DefiniciÃ³n

El subgrafo efectivo es:

> el grafo inducido por el conjunto activo de conceptos

Incluye:

- todos los nodos activados
- todas las dependencias entre ellos

---

### Propiedades

- es un subgrafo del macro-grafo
- sigue siendo acÃ­clico
- es especÃ­fico de una ejecuciÃ³n

---

## 4. Plan de cÃ¡lculo efectivo

### DefiniciÃ³n

El plan de cÃ¡lculo es:

> una ordenaciÃ³n vÃ¡lida de los conceptos activos que respeta todas las dependencias

---

### ConstrucciÃ³n

Se obtiene mediante una **ordenaciÃ³n topolÃ³gica** del subgrafo efectivo.

---

### Propiedades

- todo concepto se evalÃºa despuÃ©s de sus dependencias
- no existe ambigÃ¼edad en el orden relativo necesario
- puede existir mÃ¡s de un orden vÃ¡lido

---

### RepresentaciÃ³n

El plan puede representarse como:

- lista ordenada de conceptos
- niveles de cÃ¡lculo (capas paralelizables)
- pipeline de ejecuciÃ³n

---

## Ejemplo conceptual

Dado el objetivo:


NETO_A_PERCIBIR


### ActivaciÃ³n

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
SeparaciÃ³n de responsabilidades

Este ADR establece una separaciÃ³n clara:

Fase	Responsabilidad
Macro-grafo	modelo estructural
ActivaciÃ³n	quÃ© calcular
Subgrafo	reducciÃ³n del problema
Plan	cÃ³mo ordenarlo
Consecuencias
Positivas
ejecuciÃ³n derivada automÃ¡ticamente
desacoplamiento total entre definiciÃ³n y ejecuciÃ³n
capacidad de calcular subconjuntos
base para paralelizaciÃ³n futura
trazabilidad clara
Costes
necesidad de construir subgrafos dinÃ¡micos
necesidad de resolver activaciÃ³n correctamente
mayor complejidad conceptual
Riesgos
1. ActivaciÃ³n incompleta

Si falta un concepto necesario â†’ fallo en ejecuciÃ³n.

2. ActivaciÃ³n excesiva

Activar conceptos innecesarios â†’ coste de cÃ¡lculo innecesario.

3. Reglas de selecciÃ³n mal definidas

Pueden activar conjuntos inesperados de conceptos.

No objetivos

Este ADR no define:

cÃ³mo se calcula cada concepto
cÃ³mo se gestionan segmentos temporales
cÃ³mo se cachean resultados
cÃ³mo se ejecuta en paralelo
cÃ³mo se materializan resultados
RelaciÃ³n con ADRs previos
ADR-036 â†’ define tipos de cÃ¡lculo
ADR-037 â†’ define sources y operandos
ADR-038 â†’ define agregaciÃ³n
ADR-039 â†’ define dependencias

Este ADR define:

cÃ³mo todo lo anterior se convierte en un plan ejecutable

Resumen ejecutivo

El sistema se modela como:

un macro-grafo completo de conceptos
un proceso de activaciÃ³n que determina quÃ© calcular
un subgrafo efectivo reducido
un plan de cÃ¡lculo derivado por ordenaciÃ³n topolÃ³gica

Este enfoque permite ejecutar el motor de forma declarativa, predecible y extensible.

<!-- END FILE: ADR-040-Macro-grafo-activación-de-conceptos-y-plan-de-cálculo-efectivo.md -->


---

# FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md
<a name="file-adr-041-segmentaci-n-temporal--mbito-de-ejecuci-n-y-c-lculo-por-tramos-en-payrollconcept-md"></a>

<!-- BEGIN FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md -->

# ADR-041 â€” SegmentaciÃ³n temporal, Ã¡mbito de ejecuciÃ³n y cÃ¡lculo por tramos en `PayrollConcept`

## Estado
Propuesto

---

## Contexto

Los ADR previos establecen:

- ADR-036 â€” TipologÃ­as de cÃ¡lculo de `PayrollConcept`
- ADR-037 â€” ResoluciÃ³n de operandos mediante sources
- ADR-038 â€” Estrategias de agregaciÃ³n (`FEED_BY_SOURCE`, `SELECT_BY_RULE`)
- ADR-039 â€” Modelo de dependencias y grafo de cÃ¡lculo (DAG)
- ADR-040 â€” Macro-grafo, activaciÃ³n y plan efectivo

Estos elementos permiten definir quÃ© calcular y en quÃ© orden.

Sin embargo, en nÃ³mina real, durante un mismo perÃ­odo pueden producirse cambios que afectan al cÃ¡lculo:

- jornada laboral  
- salario  
- contrato  
- centro de trabajo  
- situaciones de alta/baja  
- otras condiciones relevantes  

Esto implica que el cÃ¡lculo no puede realizarse como una Ãºnica ejecuciÃ³n homogÃ©nea.

---

## DecisiÃ³n

Se introduce un modelo de:

1. **segmentaciÃ³n temporal del perÃ­odo**
2. **ejecuciÃ³n del plan de cÃ¡lculo por segmento**
3. **clasificaciÃ³n de conceptos por Ã¡mbito temporal (`executionScope`)**
4. **consolidaciÃ³n de resultados a nivel de perÃ­odo**

---

## 1. Modelo temporal

### 1.1 `CalculationPeriod`

Representa el perÃ­odo global de la nÃ³mina:

- `periodStart`
- `periodEnd`

---

### 1.2 `CalculationSegment`

Representa un subtramo homogÃ©neo dentro del perÃ­odo:

- `segmentStart`
- `segmentEnd`

---

### Propiedad clave

Dentro de un segmento:

> Las condiciones relevantes para el cÃ¡lculo permanecen constantes.

---

## 2. SegmentaciÃ³n

### DefiniciÃ³n

El perÃ­odo se divide en:

> un conjunto ordenado de segmentos contiguos, no solapados y exhaustivos

---

### Propiedades

- cubren completamente el perÃ­odo  
- no se solapan  
- son deterministas  
- son reproducibles  

---

### Origen de los cortes

Los segmentos se generan por cambios en condiciones relevantes del cÃ¡lculo:

- datos del empleado  
- asignaciones  
- condiciones contractuales  
- otros factores que afectan al cÃ¡lculo  

---

### Regla importante

Un segmento puede estar delimitado por mÃºltiples cambios simultÃ¡neos.

#### Consecuencia

No se modela una Ãºnica â€œfuente del segmentoâ€.

---

## 3. Contexto de ejecuciÃ³n por segmento

Cada ejecuciÃ³n del cÃ¡lculo se realiza con un contexto temporal enriquecido:

- `periodStart`
- `periodEnd`
- `segmentStart`
- `segmentEnd`
- `isFirstSegment`
- `isLastSegment`

---

## 4. RelaciÃ³n con el grafo de cÃ¡lculo

### Regla fundamental

> La segmentaciÃ³n no modifica la topologÃ­a del grafo de cÃ¡lculo.

---

### ImplicaciÃ³n

- el macro-grafo y el plan efectivo son Ãºnicos  
- se reutilizan para todos los segmentos  

---

### EjecuciÃ³n

El plan de cÃ¡lculo:

> se ejecuta una vez por cada segmento con distinto contexto temporal

---

## 5. Ãmbito de ejecuciÃ³n del concepto

Se introduce la propiedad:

# `executionScope`

---

### DefiniciÃ³n

Define el nivel temporal en el que se evalÃºa un concepto.

---

### Valores iniciales

- `SEGMENT`
- `PERIOD`

---

### Regla fuerte

> `executionScope` es una propiedad inmutable del concepto.

#### Consecuencia

Cambiar el Ã¡mbito implica crear un nuevo concepto.

---

### InterpretaciÃ³n

#### `SEGMENT`

El concepto se evalÃºa en cada segmento.

Ejemplos:

- salario base  
- horas trabajadas  
- pluses proporcionales  

---

#### `PERIOD`

El concepto se evalÃºa una Ãºnica vez para todo el perÃ­odo.

Ejemplos:

- totales  
- agregados finales  
- ciertos cÃ¡lculos acumulados  

---

## 6. EjecuciÃ³n segmentada

### Proceso

1. Se construyen los segmentos del perÃ­odo  
2. Se ejecuta el plan de cÃ¡lculo para cada segmento (`executionScope = SEGMENT`)  
3. Se obtienen resultados parciales  
4. Se consolidan los resultados a nivel de perÃ­odo  
5. Se evalÃºan conceptos de `executionScope = PERIOD`

---

## 7. ConsolidaciÃ³n

### DefiniciÃ³n

Proceso de agregaciÃ³n de resultados de segmentos.

---

### Ejemplos

- suma de importes segmentados  
- construcciÃ³n de bases  
- preparaciÃ³n de datos para conceptos de perÃ­odo  

---

### Nota

La consolidaciÃ³n es un paso previo a la evaluaciÃ³n de conceptos de Ã¡mbito `PERIOD`.

---

## 8. Trazabilidad y reproducibilidad

### Regla clave

> La segmentaciÃ³n utilizada en un cÃ¡lculo debe ser determinista, reproducible y auditable.

---

### DecisiÃ³n

Los segmentos forman parte del:

> **snapshot tÃ©cnico del cÃ¡lculo de nÃ³mina**

---

### Consecuencia

Es posible:

- reconstruir cÃ³mo se calculÃ³ la nÃ³mina  
- explicar los tramos utilizados  
- garantizar coherencia en retroactividad  

---

## 9. Validaciones

---

### 9.1 ValidaciÃ³n de segmentaciÃ³n

Debe garantizar:

- cobertura completa del perÃ­odo  
- ausencia de solapamientos  
- orden correcto  

---

### 9.2 ValidaciÃ³n de ejecuciÃ³n

Debe garantizar:

- coherencia entre `executionScope` y uso del concepto  
- disponibilidad de datos necesarios en cada segmento  
- correcta consolidaciÃ³n  

---

## 10. Riesgos identificados

---

### 10.1 SegmentaciÃ³n no determinista

Provoca inconsistencias en recalculaciones.

---

### 10.2 Uso incorrecto de `executionScope`

Puede generar:

- doble cÃ¡lculo  
- omisiones  
- incoherencias  

---

### 10.3 Mala clasificaciÃ³n de conceptos

Asignar incorrectamente `SEGMENT` o `PERIOD` rompe la lÃ³gica del cÃ¡lculo.

---

### 10.4 ExplicaciÃ³n simplificada de cortes

Asociar un Ãºnico motivo a un segmento puede ser incorrecto.

---

## 11. No objetivos

Este ADR no define:

- algoritmo de generaciÃ³n de segmentos  
- optimizaciÃ³n de ejecuciÃ³n  
- paralelizaciÃ³n  
- caching  
- persistencia detallada de estructuras internas  

---

## 12. Insight clave

El cÃ¡lculo de nÃ³mina evoluciona de:

> una ejecuciÃ³n Ãºnica del grafo

a:

> la ejecuciÃ³n del mismo plan de cÃ¡lculo sobre mÃºltiples contextos temporales homogÃ©neos, seguida de una consolidaciÃ³n

---

## 13. ConclusiÃ³n

Se establece que:

- el perÃ­odo se segmenta en tramos homogÃ©neos  
- el mismo plan de cÃ¡lculo se ejecuta por segmento  
- los conceptos se clasifican por Ã¡mbito temporal (`executionScope`)  
- los resultados se consolidan a nivel de perÃ­odo  
- la segmentaciÃ³n es determinista y trazable  

Este modelo permite:

- soportar cambios intraperiodo  
- mantener coherencia en retroactividad  
- preservar un Ãºnico grafo de cÃ¡lculo  
- garantizar trazabilidad completa del resultado  

<!-- END FILE: ADR-041-Segmentación-temporal-ámbito-de-ejecución-y-cálculo-por-tramos-en-PayrollConcept.md -->


---

# FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md
<a name="file-adr-042-separaci-n-entre-payrol-y-payroll-engine-md"></a>

<!-- BEGIN FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md -->

# ADR-042 â€” SeparaciÃ³n entre `payroll` y `payroll_engine`

## Estado
Propuesto

---

## Contexto

El diseÃ±o del motor de nÃ³mina de B4RRHH ha evolucionado desde un enfoque potencialmente basado en lÃ³gica especÃ­fica por concepto hacia un modelo configurable basado en:

- `PayrollObject`
- `PayrollConcept`
- tipologÃ­as de cÃ¡lculo
- sources y operandos
- estrategias de agregaciÃ³n
- grafo de dependencias
- segmentaciÃ³n temporal

En paralelo, el bounded context `payroll` ya existe para modelar:

- nÃ³minas calculadas
- estados de nÃ³mina
- runs de cÃ¡lculo
- claims
- mensajes
- snapshots del cÃ¡lculo

A medida que madura el metamodelo del motor, aparece una separaciÃ³n semÃ¡ntica clara entre:

1. **la definiciÃ³n de cÃ³mo se calcula una nÃ³mina**
2. **la persistencia del resultado de una nÃ³mina calculada**

Mezclar ambas naturalezas en el mismo schema o subdominio introduce ambigÃ¼edad de diseÃ±o.

---

## DecisiÃ³n

Se separan explÃ­citamente dos Ã¡mbitos:

- `payroll`
- `payroll_engine`

---

## 1. Ãmbito `payroll`

`payroll` modela la nÃ³mina calculada como resultado de negocio.

Incluye, entre otros:

- payroll root
- lÃ­neas calculadas
- estados
- calculation runs
- claims
- mensajes
- snapshots tÃ©cnicos del cÃ¡lculo
- segmentos utilizados en una ejecuciÃ³n concreta

### Naturaleza
Resultado materializado del cÃ¡lculo.

---

## 2. Ãmbito `payroll_engine`

`payroll_engine` modela el metamodelo y la configuraciÃ³n tÃ©cnico-funcional del motor.

Incluye, entre otros:

- `PayrollObject`
- `PayrollConcept`
- feeds entre conceptos
- tablas
- constantes
- tipologÃ­as de cÃ¡lculo
- scopes de ejecuciÃ³n
- metadatos de resoluciÃ³n

### Naturaleza
DefiniciÃ³n estructural de cÃ³mo se calcula una nÃ³mina.

---

## Regla principal

> La configuraciÃ³n y metamodelo del motor de nÃ³mina no deben persistirse en el mismo Ã¡mbito semÃ¡ntico que las nÃ³minas calculadas.

---

## Consecuencias

### Positivas

- separaciÃ³n clara entre configuraciÃ³n y resultado
- mejor trazabilidad
- mejor capacidad de gobierno del motor
- menor contaminaciÃ³n semÃ¡ntica del bounded context `payroll`
- base mÃ¡s limpia para evoluciÃ³n futura

### Costes

- aparece un nuevo Ã¡mbito de diseÃ±o
- exige modelar explÃ­citamente la relaciÃ³n entre runtime del motor y resultado calculado

---

## Regla operativa

Los artefactos persistentes que definan **cÃ³mo se calcula** una nÃ³mina pertenecen a `payroll_engine`.

Los artefactos persistentes que representen **una nÃ³mina ya calculada** pertenecen a `payroll`.

---

## No objetivos

Este ADR no define todavÃ­a:

- estructura fÃ­sica detallada de schemas
- APIs de mantenimiento del motor
- estrategia de despliegue
- separaciÃ³n en repositorios o servicios

---

## Resumen ejecutivo

Se establece una frontera explÃ­cita:

- `payroll` = resultado calculado
- `payroll_engine` = definiciÃ³n del motor

Esto evita mezclar metamodelo y cÃ¡lculo materializado en el mismo dominio y prepara una base mÃ¡s limpia para la implementaciÃ³n.

<!-- END FILE: ADR-042-Separación-entre-payrol-y-payroll_engine.md -->


---

# FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md
<a name="file-adr-043-agreement-profile-y-activaci-n-de-payroll-basada-en-contexto-md"></a>

<!-- BEGIN FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md -->

ADR-043 â€” Agreement Profile y ActivaciÃ³n de Payroll basada en Contexto
Estado

Propuesto

Contexto

El sistema B4RRHH actualmente modela el convenio colectivo (AGREEMENT) como una entidad de catÃ¡logo dentro de rule_entity, junto con su relaciÃ³n con categorÃ­as (AGREEMENT_CATEGORY).

Este modelo es suficiente para validaciones bÃ¡sicas, pero insuficiente para:

representar informaciÃ³n real de negocio del convenio (cÃ³digo oficial, jornada anual, etc.)
alimentar lÃ³gica derivada (ej. cÃ¡lculo de jornada del empleado)
servir como contexto de configuraciÃ³n para el motor de nÃ³mina

En paralelo, el motor de nÃ³mina (payroll) se estÃ¡ diseÃ±ando en torno a un metamodelo de objetos configurables (PayrollObject), donde:

los conceptos (PAYROLL_CONCEPT) representan cÃ¡lculos
las tablas (TABLE) representan fuentes de datos parametrizadas
las constantes (CONSTANT) representan valores fijos

AdemÃ¡s, se identifica la necesidad de que distintos contextos de negocio (rule system, convenio, empresa, etc.):

activen conceptos de nÃ³mina aplicables
vinculen fuentes de datos (tablas, constantes)

Finalmente, se detecta una deuda tÃ©cnica en employee.working_time, donde las horas anuales se encuentran fijadas de forma estÃ¡tica (ej. 2000 horas), cuando en realidad dependen del convenio aplicable.

Problema

Se necesita:

Enriquecer el convenio sin romper el modelo existente basado en rule_entity
Permitir que el convenio participe en la configuraciÃ³n efectiva del cÃ¡lculo de nÃ³mina
Introducir un mecanismo genÃ©rico y escalable para:
activar conceptos de nÃ³mina
vincular fuentes de datos (tablas)
Definir una estructura eficiente para almacenar datos parametrizados de tablas (ej. salario base por categorÃ­a)
Resolver la dependencia entre convenio y jornada laboral del empleado
Evitar:
proliferaciÃ³n de tablas especÃ­ficas por tipo de entidad (agreement_triggers, etc.)
sobreabstracciÃ³n prematura del motor de nÃ³mina
DecisiÃ³n
1. Mantener AGREEMENT como catÃ¡logo base
AGREEMENT permanece como rule_entity
No se modifica su identidad funcional
El cÃ³digo funcional del convenio serÃ¡, preferentemente, el cÃ³digo oficial real

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
PropÃ³sito
Enriquecer el convenio con datos de negocio
Servir como fuente para lÃ³gica derivada (ej. jornada del empleado)
3. Derivar la jornada del empleado desde el convenio

Se establece la regla:

employee.working_time no contiene una constante fija de horas anuales;
las horas se derivan del agreement_profile vigente en la fecha de aplicaciÃ³n.

Flujo:

Cambio en working_time
ResoluciÃ³n de convenio/categorÃ­a vigente
Lectura de annualHours desde agreement_profile
CÃ¡lculo y persistencia de valores derivados
4. Introducir activaciÃ³n de objetos de nÃ³mina por contexto

Se crea una tabla genÃ©rica:

payroll_object_activation

Campos
ruleSystemCode
ownerTypeCode (RULE_SYSTEM, AGREEMENT, COMPANY, etc.)
ownerCode
targetObjectTypeCode
targetObjectCode
active
PropÃ³sito

Permitir que un contexto de negocio active conceptos de nÃ³mina.

RestricciÃ³n V1

Solo se permite:

targetObjectTypeCode = PAYROLL_CONCEPT
Ejemplo
AGREEMENT 99002405011982 â†’ PAYROLL_CONCEPT SALARIO_BASE
5. Introducir binding de objetos de nÃ³mina por contexto

Se crea una segunda tabla genÃ©rica:

payroll_object_binding

Campos
ruleSystemCode
ownerTypeCode
ownerCode
bindingRoleCode
boundObjectTypeCode
boundObjectCode
active
PropÃ³sito

Permitir que un contexto vincule fuentes de datos a roles funcionales.

Ejemplo
AGREEMENT 99002405011982 â†’ BASE_SALARY_TABLE â†’ TABLE SB_RETAIL
Nota

bindingRoleCode es obligatorio para distinguir semÃ¡ntica.

6. Mantener separaciÃ³n semÃ¡ntica: activation vs binding

Se decide explÃ­citamente:

activation â‰  binding
No se utiliza una Ãºnica tabla genÃ©rica para ambos conceptos

Motivo:

semÃ¡ntica distinta
validaciÃ³n distinta
mantenimiento mÃ¡s claro
7. Mantener precedencia fuera de datos (V1)

La precedencia entre contextos:

Ejemplo:

RULE_SYSTEM > AGREEMENT > COMPANY > WORK_CENTER

No se modela en base de datos en V1.

Se define como:

polÃ­tica del motor
fijada en cÃ³digo
validada mediante tests
8. Modelar TABLE como PayrollObject

Se mantiene la decisiÃ³n:

TABLE es un PayrollObject
BK canÃ³nica:
ruleSystemCode + objectTypeCode + objectCode
9. Introducir estructura comÃºn de filas de tabla

Se crea una estructura fÃ­sica comÃºn:

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
PropÃ³sito
Lookup eficiente por clave + fecha
Soporte para tablas tÃ­picas de nÃ³mina:
salario base
plus convenio
antigÃ¼edad
10. Estrategia de valores

Cada tabla define un valueBasis:

Ejemplo:

MONTHLY_MASTER
ANNUAL_MASTER

Se establece:

un valor rector
valores derivados persistidos
11. RelaciÃ³n convenio â†’ tabla

Se define mediante binding, no por estructura interna de la tabla.

Ejemplo:

AGREEMENT â†’ BASE_SALARY_TABLE â†’ TABLE SB_RETAIL

La tabla:

no necesita incluir agreementCode en su clave
puede reutilizarse o especializarse libremente
12. Estrategia de evoluciÃ³n

Se permite que en el futuro existan nuevos tipos de objeto payroll:

Ejemplo:

COMPLEX_TABLE

MotivaciÃ³n:

no forzar todos los casos en un Ãºnico modelo de tabla
permitir crecimiento sin romper diseÃ±o base
Consecuencias
Positivas
Enriquecimiento del convenio sin romper catÃ¡logo existente
EliminaciÃ³n de constantes duras (ej. 2000 horas)
IntegraciÃ³n natural convenio â†” payroll
Modelo escalable basado en contextos
Evita proliferaciÃ³n de tablas especÃ­ficas por tipo de entidad
SeparaciÃ³n clara entre:
activaciÃ³n (quÃ© se calcula)
binding (de dÃ³nde salen los datos)
Lookup de tablas eficiente y uniforme
Negativas / Riesgos
IntroducciÃ³n de dos nuevas tablas genÃ©ricas (activation y binding)
Necesidad de disciplina en bindingRoleCode
Riesgo de sobreuso de TABLE para casos complejos
Precedencia no configurable en V1 (requiere cambios de cÃ³digo)
No objetivos (V1)
Modelado completo de versiones de convenio
Modelado genÃ©rico de tablas multiclave complejas
Engine de reglas declarativas completo
Precedencia configurable en base de datos
ActivaciÃ³n de objetos distintos de PAYROLL_CONCEPT
UI avanzada de configuraciÃ³n payroll
Estrategia de implementaciÃ³n
Crear agreement_profile
Integrar annualHours en working_time
Crear payroll_object_activation
Crear payroll_object_binding
Crear TABLE + payroll_table_row para salario base
Activar SALARIO_BASE desde convenio
Resolver salario base en payroll usando:
convenio
categorÃ­a
tabla vinculada
fecha
Nota operativa

Se recomienda iniciar el uso de:

convenios reales (cÃ³digo oficial)
datos reales de tablas salariales

Manteniendo datos actuales como:

entorno de test
fallback

Esto permitirÃ¡ validar el modelo con casos reales desde el inicio.

ðŸ§  Cierre

La idea central de este ADR es:

El convenio no calcula nÃ³mina, pero sÃ­ define el contexto que activa quÃ© se calcula y con quÃ© datos.

Y el sistema se organiza en torno a tres pilares:

Contexto (agreement, company, etc.)
ActivaciÃ³n (conceptos)
Binding (fuentes)

<!-- END FILE: ADR-043-Agreement-Profile-y-Activación-de-Payroll-basada-en-Contexto.md -->


---

# FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md
<a name="file-adr-044-primer-c-lculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-m-nimo-md"></a>

<!-- BEGIN FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md -->

ADR â€” Primer cÃ¡lculo real de salario base mediante conceptos tipados y grafo mÃ­nimo
Estado

Implementado (ver ADR-045)

Contexto

En iteraciones recientes se ha construido un slice funcional que permite:

activar conceptos por convenio
vincular tablas mediante binding
resolver filas temporales por convenio/categorÃ­a/fecha
integrar conceptos reales en el lanzador de nÃ³mina

Ese trabajo ha sido Ãºtil para validar:

activation
binding
payroll_table_row
resoluciÃ³n temporal
integraciÃ³n con el launch

Sin embargo, la implementaciÃ³n actual de conceptos como BASE_SALARY o PLUS_CONVENIO se ha materializado mediante servicios concretos por concepto. Ese enfoque ha servido como spike tÃ©cnico, pero no representa la arquitectura objetivo del motor de nÃ³mina.

El bundle de diseÃ±o ya fijaba una lÃ­nea distinta: los conceptos de nÃ³mina deben resolverse por tipologÃ­a de cÃ¡lculo y por sources tipados, no por servicios hardcodeados por concepto. En particular, el bundle ya contempla tipologÃ­as como DIRECT_AMOUNT, RATE_BY_QUANTITY, PERCENTAGE y AGGREGATE, y ademÃ¡s permite que un operando se resuelva a partir de otro CONCEPT. El ejemplo conceptual del salario base ya estaba descrito como una composiciÃ³n de cantidad y precio.

Se necesita, por tanto, una reconducciÃ³n controlada:

aprovechar las piezas ya validadas
dejar de codificar conceptos de negocio â€œa manoâ€
empezar a ejecutar un mini grafo real
Problema

Se quiere obtener una primera nÃ³mina no fake con un salario base mÃ­nimo pero real, disparado desde el convenio y resuelto mediante conceptos configurados en base de datos.

Ese primer caso debe ser lo bastante simple para ser implementable en pocas iteraciones, pero lo bastante correcto como para validar la arquitectura del motor.

DecisiÃ³n
1. Separar explÃ­citamente concepto de negocio y concepto tÃ©cnico

Se distinguen dos familias de conceptos:

Conceptos de negocio

Son los que representan lÃ­neas reales de nÃ³mina y pueden persistirse como resultado final.

Ejemplo:

101 - SALARIO_BASE
Conceptos tÃ©cnicos

Son nodos auxiliares de cÃ¡lculo, reutilizables, y no tienen por quÃ© persistirse como lÃ­neas finales de nÃ³mina.

Ejemplos:

D01 - DIAS_PRESENCIA
P01 - PRECIO_DIA_TEORICO

Los conceptos tÃ©cnicos podrÃ¡n ser reutilizados por varios conceptos de negocio futuros.

2. El salario base piloto se modela como RATE_BY_QUANTITY

El primer concepto real de negocio serÃ¡:

101 - SALARIO_BASE

Su tipologÃ­a serÃ¡:

RATE_BY_QUANTITY

SemÃ¡ntica:

quantity = CONCEPT(D01)
rate = CONCEPT(P01)

Resultado:

101 = P01 Ã— D01

Esto sigue la lÃ­nea ya fijada en el bundle para salario base como combinaciÃ³n de cantidad y precio.

3. D01 - DIAS_PRESENCIA se introduce como concepto tÃ©cnico temporalmente simplificado

Se define:

D01 - DIAS_PRESENCIA

TipologÃ­a inicial:

DIRECT_AMOUNT

Valor inicial:

30

Esta simplificaciÃ³n es deliberada.
No se calcularÃ¡n todavÃ­a dÃ­as reales de presencia ni segmentaciÃ³n.

Objetivo de esta iteraciÃ³n:

validar la dependencia tÃ©cnica
validar la ejecuciÃ³n por grafo
no bloquear el avance por la falta de cÃ¡lculo temporal detallado

En iteraciones futuras, D01 podrÃ¡ evolucionar para resolverse por segmento o por ventana real de presencia sin cambiar la estructura del concepto 101.

4. P01 - PRECIO_DIA_TEORICO se introduce como concepto tÃ©cnico valorizado por tabla binded

Se define:

P01 - PRECIO_DIA_TEORICO

TipologÃ­a inicial:

DIRECT_AMOUNT resuelto por source TABLE
o, equivalentemente, un nodo tÃ©cnico cuyo valor proviene de lookup a tabla binded

Valor:

daily_value de payroll_table_row

Lookup por:

convenio aplicable
categorÃ­a aplicable
fecha efectiva

El binding del convenio apuntarÃ¡ a la tabla salarial correspondiente, y P01 se resolverÃ¡ leyendo daily_value de la fila vigente.

Esto aprovecha directamente la estructura ya existente en payroll_table_row, que ya contiene daily_value. No se requiere derivar el precio diario a partir del mensual. Eso permite un primer caso limpio y escalable.

5. El convenio dispara el concepto final de negocio, no los nodos tÃ©cnicos como lÃ­neas finales

Para el primer caso:

el convenio activa 101 - SALARIO_BASE

El motor, al resolver 101, podrÃ¡ descubrir y ejecutar sus dependencias:

D01
P01

Pero el resultado persistido en la nÃ³mina serÃ¡, en esta iteraciÃ³n:

101 - SALARIO_BASE

Los conceptos tÃ©cnicos podrÃ¡n existir:

como nodos de ejecuciÃ³n
como trazabilidad futura
como snapshot si mÃ¡s adelante interesa

Pero no se consideran todavÃ­a lÃ­neas finales de nÃ³mina.

6. La ejecuciÃ³n se harÃ¡ mediante un grafo mÃ­nimo, no mediante servicios concretos por concepto de negocio

Se abandona como direcciÃ³n arquitectÃ³nica final la idea de:

CalculateBaseSalaryService
CalculateAgreementPlusService
etc.

Esos servicios se reinterpretan como spikes o resolvedores transitorios Ãºtiles para validar piezas del pipeline.

La direcciÃ³n correcta pasa a ser:

resolver conceptos por tipologÃ­a
resolver operandos por source
permitir que un concepto dependa de otro CONCEPT

Para la primera iteraciÃ³n no se construirÃ¡ un motor genÃ©rico completo, pero sÃ­ un mini dispatcher suficiente para ejecutar:

DIRECT_AMOUNT
RATE_BY_QUANTITY

y para resolver operandos tipo:

CONCEPT
TABLE
7. El lanzador de nÃ³mina deberÃ¡ enchufarse al mini grafo real

El endpoint de cÃ¡lculo / lanzador ya existente dejarÃ¡ de inyectar un concepto fake para este caso y pasarÃ¡ a:

identificar que el convenio dispara 101 - SALARIO_BASE
resolver el mini grafo:
D01
P01
101
persistir 101 como lÃ­nea final real de nÃ³mina

El camino fake podrÃ¡ mantenerse temporalmente como fallback o modo alternativo, pero el caso piloto de salario base debe pasar a ejecutarse ya con grafo mÃ­nimo real.

Consecuencias
Positivas
Se reconduce el diseÃ±o hacia el motor real sin tirar piezas Ãºtiles.
Se valida el uso real de:
trigger por convenio
binding de tabla
source CONCEPT
source TABLE
tipologÃ­a RATE_BY_QUANTITY
Se evita seguir creando servicios por concepto como arquitectura final.
Se prepara una base escalable:
maÃ±ana D01 podrÃ¡ calcularse por segmento
maÃ±ana P02 podrÃ¡ depender de P01 y J01
maÃ±ana se podrÃ¡n introducir coeficientes de jornada sin reescribir 101
Negativas / Costes
Lo ya implementado como cÃ¡lculo directo de BASE_SALARY y PLUS_CONVENIO pasa a ser transitorio.
Hay que introducir un primer wiring real de dependencias entre conceptos.
El launch tendrÃ¡ que dejar de pensar en â€œconceptos hardcodeadosâ€ y empezar a ejecutar un mini plan de cÃ¡lculo.
No objetivos de esta iteraciÃ³n

No se pretende todavÃ­a:

calcular dÃ­as reales de presencia
aplicar coeficiente real de jornada
introducir segmentaciÃ³n temporal
modelar dependencias arbitrarias complejas
persistir todos los conceptos tÃ©cnicos como lÃ­neas de nÃ³mina
construir un motor genÃ©rico completo con todas las tipologÃ­as posibles
resolver plus convenio dentro de este mismo salto
DiseÃ±o mÃ­nimo resultante
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
lÃ­nea final persistida: 101 - SALARIO_BASE
Estrategia de implementaciÃ³n
Paso 1

Sembrar en base de datos:

101
D01
P01
sus tipologÃ­as
sus relaciones/dependencias
el binding de tabla para P01
Paso 2

Crear el wiring mÃ­nimo del mini grafo:

resolver 101
descubrir dependencias
resolver D01
resolver P01
calcular 101
Paso 3

Enchufar ese mini cÃ¡lculo al lanzador de nÃ³mina existente

Paso 4

Persistir 101 como concepto final real en la tabla de resultados

Nota de transiciÃ³n

Los servicios actuales tipo CalculateBaseSalaryService o CalculateAgreementPlusService no se consideran el diseÃ±o final del motor. Se mantienen Ãºnicamente como apoyo temporal o como material de transiciÃ³n mientras el primer camino real basado en tipologÃ­as y dependencias queda operativo.

DecisiÃ³n prÃ¡ctica inmediata

La siguiente iteraciÃ³n no se enfocarÃ¡ en aÃ±adir mÃ¡s conceptos directos.
Se enfocarÃ¡ en conseguir que el lanzador calcule un Ãºnico concepto real final (101 - SALARIO_BASE) mediante:

trigger de convenio
mini grafo
conceptos tÃ©cnicos
tipologÃ­as mÃ­nimas
lookup a tabla binded

<!-- END FILE: ADR-044-Primer-cálculo-real-de-salario-base-mediante-conceptos-tipados-y-grafo-mínimo.md -->


---

# FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md
<a name="file-adr-045-ejecucion-elegible-real-basada-en-concept-assignment-y-plan-de-calculo-md"></a>

<!-- BEGIN FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md -->

# ADR-045 â€” EjecuciÃ³n elegible real basada en `concept_assignment` y plan de cÃ¡lculo

## Estado

Implementado

---

## Contexto

ADR-044 estableciÃ³ la direcciÃ³n tÃ©cnica: abandonar servicios hardcodeados por concepto y ejecutar nÃ³mina mediante un grafo mÃ­nimo de conceptos configurados en base de datos. La primera iteraciÃ³n de ese diseÃ±o se implementÃ³ con un stub PoC en `CalculatePayrollUnitService.calculateEligibleReal()` que calculaba **Ãºnicamente el concepto "101"** mediante una llamada directa a `PayrollConceptGraphCalculator`.

Ese stub sirviÃ³ para validar las piezas del pipeline (convenio, binding, tablas, resoluciÃ³n temporal) pero nunca fue el diseÃ±o final. Para avanzar hacia una nÃ³mina real habÃ­a que:

1. Determinar quÃ© conceptos aplican a un empleado desde parametrizaciÃ³n (no hardcode)
2. Expandir las dependencias transitivas necesarias para el cÃ¡lculo
3. Construir un plan de ejecuciÃ³n en orden topolÃ³gico
4. Ejecutar el plan completo, no solo un concepto
5. Persistir solo los conceptos con presencia en recibo

En paralelo, se habÃ­an diseÃ±ado las tablas `payroll_engine.concept_assignment` (elegibilidad por contexto) y el pipeline `BuildEligibleExecutionPlanUseCase` (construcciÃ³n del plan), pero ninguno estaba enchufado al lanzador.

---

## DecisiÃ³n

### 1. `concept_assignment` como fuente de elegibilidad canÃ³nica

La tabla `payroll_engine.concept_assignment` es la fuente oficial de quÃ© conceptos aplican a un contexto dado:

| Columna | SemÃ¡ntica |
|---|---|
| `rule_system_code` | Ãmbito del sistema de reglas |
| `concept_code` | Concepto elegible |
| `company_code` | Wildcardeable (null = aplica a todas las empresas) |
| `agreement_code` | Convenio aplicable |
| `employee_type_code` | Wildcardeable (null = aplica a todos los tipos) |
| `valid_from` / `valid_to` | Vigencia temporal |
| `priority` | ResoluciÃ³n de conflictos cuando hay mÃºltiples asignaciones para el mismo concepto |

La elegibilidad se evalÃºa mediante `EmployeeAssignmentContext` (ruleSystemCode, companyCode, agreementCode, employeeTypeCode) en la fecha de referencia (fin de periodo).

**Divergencia de ADR-043**: ADR-043 proponÃ­a `payroll_object_activation` como mecanismo de activaciÃ³n de conceptos por contexto. Esa tabla existe en el modelo pero no fue el camino tomado para la ejecuciÃ³n elegible. `concept_assignment` es el mecanismo real en producciÃ³n para el motor `payroll_engine`. `payroll_object_activation` queda para futuros casos de uso distintos si los hubiera.

### 2. EliminaciÃ³n del stub PoC en `CalculatePayrollUnitService`

`calculateEligibleReal()` ya no hardcodea el concepto "101". En su lugar:

1. Obtiene el contexto de asignaciÃ³n del empleado desde `PayrollLaunchEligibleInputContext`
2. Llama a `BuildEligibleExecutionPlanUseCase.build(assignmentContext, periodEnd)`
3. Itera el plan resultante (`EligibleExecutionPlanResult.executionPlan()`) en orden topolÃ³gico
4. Aplica la lÃ³gica de cÃ¡lculo segÃºn `calculationType` de cada `ConceptExecutionPlanEntry`
5. Filtra por `payslipOrderCode != null` para decidir quÃ© conceptos se persisten

### 3. CorrecciÃ³n del grafo de dependencias: edges de operandos

`DefaultConceptDependencyGraphService` ahora aÃ±ade aristas `OPERAND_DEPENDENCY` para conceptos `RATE_BY_QUANTITY` y `PERCENTAGE`. Sin estas aristas, el grafo solo tenÃ­a aristas `FEED_DEPENDENCY`, lo que hacÃ­a que la ordenaciÃ³n topolÃ³gica fuera incorrecta para conceptos con operandos de tipo `CONCEPT`.

Regla: un concepto `X` de tipo `RATE_BY_QUANTITY` cuyos operandos son `CONCEPT(D01)` y `CONCEPT(P01)` tiene dependencias `OPERAND_DEPENDENCY` de `Xâ†’D01` y `Xâ†’P01`. Estas aristas garantizan que D01 y P01 se calculen antes que X.

### 4. ExpansiÃ³n BFS con discovery por operandos

`DefaultEligibleConceptExpansionService` realiza la expansiÃ³n del conjunto elegible en dos fases:

- **Feed-based discovery**: descubre conceptos adicionales accesibles por relaciones `FEED_DEPENDENCY`, pero solo incluye conceptos de tipo `PayrollObjectTypeCode.CONCEPT` (no tablas ni constantes).
- **Operand-based discovery**: para cada concepto con operandos de tipo `CONCEPT`, incluye los conceptos tÃ©cnicos referenciados como operandos (ej. D01 y P01 para el concepto 101).

Los conceptos descubiertos por expansiÃ³n (D01, P01) no estÃ¡n en `concept_assignment` y no son "elegibles" en sentido de negocio, pero son necesarios para el cÃ¡lculo. El resultado distingue `eligibleConcepts` (asignados directamente) de `expandedConcepts` (conjunto completo incluyendo tÃ©cnicos).

### 5. `payslipOrderCode` como filtro de persistencia

Un concepto calculado se persiste como lÃ­nea de nÃ³mina si y solo si su `payslipOrderCode` no es null. El valor de `payslipOrderCode` determina ademÃ¡s el orden de presentaciÃ³n en el recibo.

Los conceptos tÃ©cnicos (D01, P01) tienen `payslipOrderCode = null` â†’ se calculan pero no se persisten.  
Los conceptos de negocio (101, 970, 990) tienen `payslipOrderCode` establecido â†’ se persisten y aparecen en el recibo.

### 6. Conceptos AGGREGATE 970, 980, 990

Se introducen tres conceptos de tipo `AGGREGATE`:

| CÃ³digo | MnemÃ³nico | Rol funcional | `payslipOrderCode` |
|---|---|---|---|
| 970 | TOTAL_DEVENGOS | `TOTAL_EARNING` | 970 |
| 980 | TOTAL_DEDUCCIONES | `TOTAL_DEDUCTION` | 980 |
| 990 | LIQUIDO_A_PAGAR | `NET_PAY` | 990 |

Sus fuentes provienen de relaciones `FEED_DEPENDENCY` desde los conceptos que los alimentan (ej. 101 â†’ 970 y 101 â†’ 990). La ejecuciÃ³n suma los importes de sus fuentes, aplicando inversiÃ³n de signo si `invertSign = true`.

El concepto 980 (TOTAL_DEDUCCIONES) no se siembra en `concept_assignment` mientras no haya conceptos de deducciÃ³n reales, ya que el plan builder lanzarÃ­a `MissingAggregateSourcesException` al no encontrar feed sources.

---

## Consecuencias

### Positivas

- El lanzador de nÃ³mina ya no contiene lÃ³gica de negocio especÃ­fica por concepto. La parametrizaciÃ³n en base de datos dicta completamente quÃ© se calcula.
- AÃ±adir un nuevo concepto elegible es solo insertar una fila en `concept_assignment` y definir las dependencias/fuentes correspondientes.
- La nÃ³mina persiste 970 y 990 ademÃ¡s de 101, dando una vista real de devengos totales y lÃ­quido a pagar.
- El pipeline completo (elegibilidad â†’ expansiÃ³n â†’ grafo â†’ plan â†’ ejecuciÃ³n) estÃ¡ cubierto por tests unitarios e integraciÃ³n E2E.

### Costes / Restricciones

- `concept_assignment` debe estar correctamente sembrado para cada convenio. Si estÃ¡ vacÃ­o, no se calcula ningÃºn concepto (sin error implÃ­cito: la nÃ³mina se persistirÃ¡ con 0 lÃ­neas).
- Los conceptos AGGREGATE con `concept_assignment` activo pero sin feed sources lanzarÃ¡n `MissingAggregateSourcesException` en tiempo de construcciÃ³n del plan.
- El modo `MINIMAL_REAL` queda retirado (`UnsupportedOperationException`). Solo `ELIGIBLE_REAL` y `FAKE` son modos operativos.

---

## RelaciÃ³n con ADRs previos

| ADR | RelaciÃ³n |
|---|---|
| ADR-036 | Define `CalculationType` â€” ahora todos los tipos (DIRECT_AMOUNT, RATE_BY_QUANTITY, PERCENTAGE, AGGREGATE) se ejecutan en el mismo dispatcher |
| ADR-038 | Define las relaciones FEED_DEPENDENCY â€” ahora usadas en tiempo de ejecuciÃ³n para AGGREGATE |
| ADR-039 | Define el grafo de dependencias â€” complementado con aristas OPERAND_DEPENDENCY |
| ADR-040 | Define el modelo conceptual de macro-grafo + activaciÃ³n + plan â€” este ADR documenta su implementaciÃ³n real |
| ADR-043 | Propuso `payroll_object_activation` como mecanismo de activaciÃ³n â€” desplazado por `concept_assignment` |
| ADR-044 | IniciÃ³ la direcciÃ³n del grafo mÃ­nimo real â€” este ADR completa y generaliza esa direcciÃ³n |


<!-- END FILE: ADR-045-Ejecucion-elegible-real-basada-en-concept_assignment-y-plan-de-calculo.md -->


---

# FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md
<a name="file-adr-046-conceptos-t-cnicos-base-de-per-odo-y-presencia-en-n-mina-md"></a>

<!-- BEGIN FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md -->

# ADR-046 â€” Conceptos tÃ©cnicos base de perÃ­odo y presencia en nÃ³mina

## Estado

Aceptado.

## Contexto

El motor de nÃ³mina de B4RRHH ya permite calcular conceptos econÃ³micos y agregados como:

- SALARIO_BASE
- TOTAL_DEVENGOS
- TOTAL_DEDUCCIONES
- NETO

Sin embargo, algunos valores necesarios para calcular correctamente una nÃ³mina mensual todavÃ­a estÃ¡n implÃ­citos o calculados en duro.

Ejemplos:

- dÃ­as reales del mes
- dÃ­as teÃ³ricos de nÃ³mina
- dÃ­as de presencia del empleado en el perÃ­odo
- dÃ­as de presencia por subperÃ­odo o segmento

Estos valores no son conceptos econÃ³micos visibles en el recibo, pero sÃ­ son datos fundamentales para explicar y trazar el cÃ¡lculo.

## Problema

Si estos valores permanecen ocultos dentro del cÃ¡lculo:

- el salario base no es completamente trazable;
- se dificulta depurar altas, bajas y meses incompletos;
- se complica evolucionar hacia cÃ¡lculo por tramos;
- no queda claro quÃ© cantidad de dÃ­as ha alimentado cada concepto econÃ³mico.

AdemÃ¡s, no todos los conceptos del motor pueden depender de otros conceptos configurados.

En algÃºn punto existen conceptos fundamentales que nacen directamente del contexto de ejecuciÃ³n.

## DecisiÃ³n

Se introducen conceptos tÃ©cnicos base de nÃ³mina con la nomenclatura D01/D02/D03, mÃ¡s cÃ³moda para negocio.

Estos conceptos:

- se calculan durante la ejecuciÃ³n mediante clases Java especÃ­ficas (`CalculationType.JAVA_PROVIDED`);
- tienen `FunctionalNature.TECHNICAL` para distinguirlos de conceptos econÃ³micos;
- pueden alimentar otros conceptos como operandos del grafo;
- no se muestran en el recibo de nÃ³mina (`payslipOrderCode = null`);
- no representan devengos ni deducciones.

## Conceptos tÃ©cnicos

### D01 â€” DIAS_DEVENGO

DÃ­as de devengo mensual del segmento. Valor que se usa como operando QUANTITY de SALARIO_BASE.

Regla V1:

```
D01 = min(daysInSegment, 30)
```

Ejemplos:

- empleado activo todo abril (30 dÃ­as): 30
- empleado activo todo marzo (31 dÃ­as): 30 (topado)
- alta el 10 de marzo (22 dÃ­as en segmento): 22
- baja el 20 de febrero (20 dÃ­as en segmento): 20

Sustituye al anterior D01 (DIRECT_AMOUNT, CONSTANT=30 fijo), que no contemplaba meses parciales.

### D02 â€” DIAS_MES_NOMINA

DÃ­as teÃ³ricos del mes de nÃ³mina. Denominador convencional para el cÃ¡lculo mensual.

Regla V1:

```
D02 = 30 (siempre)
```

Febrero, meses de 31 dÃ­as y cualquier mes se tratan como 30 a efectos de nÃ³mina mensual.

### D03 â€” DIAS_MES_REALES

DÃ­as naturales reales del mes de cÃ¡lculo.

Regla V1:

```
D03 = periodStart.lengthOfMonth()
```

Ejemplos:

- enero: 31
- febrero: 28 o 29 (aÃ±o bisiesto)
- abril: 30

## RelaciÃ³n con SALARIO_BASE

Para salario base mensual V1:

```
SALARIO_BASE = D01 Ã— P01
```

Donde `P01` (PRECIO_DIA) se obtiene de tabla de tarifas por categorÃ­a de convenio.

Con D01 dinÃ¡mico por segmento, el empleado con alta el 10 de marzo cobra proporcionalmente
(`22 Ã— P01`) en lugar del mes completo (`30 Ã— P01`).

## ImplementaciÃ³n

`CalculationType.JAVA_PROVIDED` identifica estos conceptos en el grafo. El `DefaultSegmentExecutionEngine`
y el `CalculatePayrollUnitService` despachan a la implementaciÃ³n registrada por Spring mediante
`List<TechnicalConceptCalculator>`.

Calculadores registrados:

| Concepto | Clase                              |
|----------|------------------------------------|
| D01      | `AccrualDaysConceptCalculator`     |
| D02      | `PayrollMonthDaysConceptCalculator`|
| D03      | `CalendarDaysConceptCalculator`    |

La interfaz `TechnicalConceptCalculator` recibe un `TechnicalConceptSegmentData` (solo fechas y
`daysInSegment`) para mantener el contrato estrecho y fÃ¡cilmente testeable.

## Regla de visibilidad

Los conceptos tÃ©cnicos base tienen `payslipOrderCode = null`. No se persisten como lÃ­neas de recibo.
Son calculados y disponibles en el estado de ejecuciÃ³n para ser operandos de otros conceptos.

## Regla arquitectÃ³nica

Las clases `TechnicalConceptCalculator` no deben convertirse en un motor paralelo.

Solo pueden calcular conceptos tÃ©cnicos fundamentales derivados de:

- perÃ­odo de nÃ³mina;
- calendario mensual;
- presencia del empleado;
- segmentos temporales efectivos.

No deben calcular conceptos econÃ³micos como salario base, antigÃ¼edad, nocturnidad, IRPF, totales o neto.

## SegmentaciÃ³n futura

Cuando exista cÃ¡lculo por subperÃ­odos, D01 ya estÃ¡ preparado: opera sobre `daysInSegment`
(el segmento activo), no sobre `daysInPeriod`. D02 y D03 son invariantes del perÃ­odo y
devuelven el mismo valor en todos los segmentos de un mismo mes.

## Consecuencias positivas

- Mejora la trazabilidad del cÃ¡lculo de SALARIO_BASE.
- Elimina el hardcode de `D01_FIXED_30`.
- Prepara el motor para altas, bajas y cambios de situaciÃ³n dentro del mes.
- Permite explicar el salario base con meses parciales.
- Mantiene limpio el recibo.
- Encaja con el grafo de conceptos sin forzar que todo sea configurable.

## Riesgos

**1. Abusar de JAVA_PROVIDED**

No todo helper interno debe convertirse en concepto tÃ©cnico. Solo deben modelarse
valores relevantes para explicar el cÃ¡lculo.

**2. Crear un segundo motor en Java**

Las clases tÃ©cnicas no deben calcular conceptos econÃ³micos.

**3. Naming ambiguo**

Se adopta la nomenclatura D01/D02/D03 por ser mÃ¡s cÃ³moda para negocio y no generar
confusiÃ³n entre DIAS_MES_REALES (D03) y DIAS_MES_NOMINA (D02).


<!-- END FILE: ADR-046-Conceptos-técnicos-base-de-período-y-presencia-en-nómina.md -->


---

# FILE: ADR-047-lifecycle-workflow-participant-pattern.md
<a name="file-adr-047-lifecycle-workflow-participant-pattern-md"></a>

<!-- BEGIN FILE: ADR-047-lifecycle-workflow-participant-pattern.md -->

# ADR-047 â€” Lifecycle Workflow Participant Pattern (Hire / Terminate)

## Estado

Propuesto

---

## Contexto

ADR-007 y ADR-018 establecieron que los workflows de ciclo de vida (Hire, Terminate, Rehire) se implementan como servicios de orquestaciÃ³n en la capa de aplicaciÃ³n: un Ãºnico `@Transactional` que coordina mÃºltiples verticales y garantiza coherencia temporal.

Esa decisiÃ³n sigue siendo correcta. El problema es la **implementaciÃ³n concreta** que ha emergido de ella.

`HireEmployeeService` actualmente:

- Tiene **10 dependencias inyectadas** en el constructor
- Ocupa **358 lÃ­neas**
- Ejecuta **8 sub-operaciones** secuenciales dentro del mÃ©todo `hire()`
- `HireEmployeeExceptionHandler` importa excepciones de **6 verticales distintos**

El patrÃ³n real que ha emergido es este:

```
HireEmployeeService â†’ CreatePresenceUseCase
                    â†’ CreateContractUseCase
                    â†’ CreateWorkCenterUseCase
                    â†’ CreateCostCenterDistributionUseCase
                    â†’ CreateWorkingTimeUseCase
                    â†’ CreateLaborClassificationUseCase
                    â†’ NextEmployeeNumberPort
                    â†’ WorkCenterCompanyValidator
                    â†’ EmployeeTypeCatalogValidator
```

Cada nuevo vertical que necesita participar en el hire obliga a:

1. AÃ±adir una dependencia al constructor de `HireEmployeeService`
2. AÃ±adir la llamada dentro del mÃ©todo `hire()`
3. AÃ±adir un `@ExceptionHandler` en `HireEmployeeExceptionHandler` para las excepciones de ese vertical

Esto viola Open/Closed: el servicio mÃ¡s crÃ­tico del sistema cambia con cada feature nueva.

El patrÃ³n se reproduce en `TerminateEmployeeService` y `RehireEmployeeService`.

---

## Problema

El diseÃ±o actual acopla el servicio orquestador con cada vertical que participa en el workflow. AÃ±adir un nuevo vertical al hire no es una operaciÃ³n local â€” requiere modificar el servicio central.

Esto no es un problema de complejidad (el hire ES complejo por dominio), sino un problema de **direcciÃ³n de las dependencias**.

---

## DecisiÃ³n

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

`order()` garantiza la secuencia de ejecuciÃ³n. Los valores de orden son:

| Orden | Participante | RazÃ³n |
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

**AÃ±adir un nuevo vertical al hire = crear un fichero `@Component` que implementa `HireParticipant`. `HireEmployeeService` nunca vuelve a cambiar.**

### 4. ExtracciÃ³n de `HireEmployeePreConditionValidator`

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

Con el patrÃ³n participant, cada vertical puede manejar sus propias excepciones mediante un `@RestControllerAdvice` scoped a `HireEmployeeController`:

```java
// workcenter/infrastructure/web/WorkCenterHireExceptionHandler.java
@RestControllerAdvice(assignableTypes = HireEmployeeController.class)
@Order(10)
public class WorkCenterHireExceptionHandler {
    @ExceptionHandler(WorkCenterCatalogValueInvalidException.class)
    public ResponseEntity<HireEmployeeErrorResponse> handle(...) { ... }
}
```

`HireEmployeeExceptionHandler` queda Ãºnicamente con las excepciones propias del lifecycle (autonumeraciÃ³n, validaciones transversales).

### 6. El mismo patrÃ³n para TERMINATION

```java
// employee/lifecycle/application/port/TerminationParticipant.java
public interface TerminationParticipant {
    int order();
    void participate(TerminationContext ctx);
}
```

`TerminationContext` lleva el `Employee` existente, la fecha de terminaciÃ³n, el motivo, y acumula los resultados del cierre de cada vertical (presence.endDate, contract.endDate, etc.).

El orden en termination es inverso a hire en semÃ¡ntica (cerrar desde fuera hacia dentro):

| Orden | Participante |
|-------|-------------|
| 10 | WorkingTimeTerminationParticipant |
| 20 | WorkCenterTerminationParticipant |
| 30 | CostCenterTerminationParticipant |
| 40 | ContractTerminationParticipant |
| 50 | LaborClassificationTerminationParticipant |
| 60 | PresenceTerminationParticipant | â† cierra presence al final |

Presence es la Ãºltima en cerrarse porque es el eje del lifecycle (ADR-018): cerrarla primero implicarÃ­a que el empleado estÃ¡ "fuera" mientras aÃºn tiene contratos abiertos.

---

## Invariantes que NO cambian

- **La transacciÃ³n sigue siendo Ãºnica.** Todos los participantes se ejecutan dentro del `@Transactional` del servicio orquestador. Si cualquier participante lanza, Spring hace rollback de todo. El comportamiento transaccional es idÃ©ntico al actual.
- **No se introducen domain events asÃ­ncronos.** Los eventos sÃ­ncronos de Spring (`@EventListener`) son una alternativa vÃ¡lida al participant port, pero aÃ±aden indirecciÃ³n sin ventaja real en este contexto. Se descarta.
- **No se introducen sagas ni consistencia eventual.** El hire necesita devolver el nÃºmero de matrÃ­cula generado en la misma peticiÃ³n HTTP. La atomicidad no es negociable.
- **El orden sigue siendo explÃ­cito y auditable.** `order()` como entero en la interfaz es deliberadamente simple â€” no hay grafos de dependencias ni resoluciÃ³n dinÃ¡mica. Si el orden cambia, se ve en el diff.

---

## Consecuencias positivas

- `HireEmployeeService` y `TerminateEmployeeService` se vuelven estables â€” no cambian al aÃ±adir nuevos verticales
- Cada vertical es dueÃ±o de su lÃ³gica de participaciÃ³n en el workflow (cohesiÃ³n)
- Los tests de cada participante son unitarios y pequeÃ±os
- `HireEmployeeExceptionHandler` pierde el conocimiento de verticales externos
- La misma base sirve para Rehire sin duplicar la orquestaciÃ³n

## Consecuencias negativas / riesgos

- El orden de participaciÃ³n es implÃ­cito al leer el cÃ³digo del servicio â€” hay que consultar las implementaciones para ver la secuencia completa. **MitigaciÃ³n:** documentar el orden en `HireContext` con un comment de referencia.
- Spring inyecta `List<HireParticipant>` en el orden que descubre los beans, por lo que el `order()` explÃ­cito es crÃ­tico â€” un test de contexto debe verificar el orden en cada release.
- La migraciÃ³n de `HireEmployeeService` al patrÃ³n debe hacerse gradualmente (un participante cada vez) para no introducir regresiones.

---

## Alternativas descartadas

### Mantener el orquestador actual y aceptar el crecimiento

Descartado:

- 10 dependencias hoy, 15 en 12 meses
- Cada feature de lifecycle toca el servicio mÃ¡s crÃ­tico del sistema
- No hay punto de estabilizaciÃ³n natural

### Domain events asÃ­ncronos (ApplicationEventPublisher + @TransactionalEventListener)

Descartado:

- AÃ±ade indirecciÃ³n sin ventaja en un contexto single-service
- El orden de los handlers es menos explÃ­cito
- El debugging es mÃ¡s complejo
- Para lograr el mismo orden de operaciones se necesita `@Order` igualmente

### Saga pattern (compensating transactions)

Descartado:

- Consistencia eventual no es apropiada para hire
- El nÃºmero de matrÃ­cula debe estar disponible sincrÃ³nicamente
- AÃ±ade infraestructura de saga que no existe ni se necesita

### Employee como agregado monolÃ­tico

Descartado desde ADR-002 y ADR-007. La arquitectura vertical se mantiene.

---

## RelaciÃ³n con ADRs anteriores

| ADR | RelaciÃ³n |
|-----|----------|
| ADR-007 | Este ADR evoluciona la implementaciÃ³n de los workflows definidos allÃ­. Los workflows siguen siendo orquestados y transaccionales. |
| ADR-018 | La secciÃ³n "OrquestaciÃ³n interna" de ADR-018 queda reemplazada por este patrÃ³n. El modelo de datos, la API y las validaciones no cambian. |

---

## Resumen

El Lifecycle Workflow Participant Pattern invierte la dependencia entre el orquestador y los verticales:

- Antes: el servicio conoce cada vertical
- DespuÃ©s: cada vertical se registra en el servicio

La transacciÃ³n, el orden de ejecuciÃ³n y el contrato de API permanecen inalterados. Lo que cambia es que aÃ±adir un nuevo vertical al hire o al terminate se convierte en una operaciÃ³n local (crear un fichero), no en una modificaciÃ³n del servicio central.


<!-- END FILE: ADR-047-lifecycle-workflow-participant-pattern.md -->


---

# FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md
<a name="file-adr-048-modelo-de-cotizacion-ss-e-irpf-md"></a>

<!-- BEGIN FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md -->

# ADR-048 â€” Modelo de cotizaciÃ³n de Seguridad Social e IRPF

## Estado

Aceptado. **Documenta una implementaciÃ³n ya existente**, no propone una nueva.

El grafo de cotizaciÃ³n se construyÃ³ entre V77 y V91 sin ADR que lo respaldara. Este
documento reconstruye la decisiÃ³n tal como estÃ¡ en el cÃ³digo a 25 de agosto de 2026 y separa
explÃ­citamente lo que es decisiÃ³n firme de lo que es provisional. Escribirlo tarde tiene un
coste: partes de este ADR describen elecciones que se tomaron sobre la marcha y que quizÃ¡ no
se habrÃ­an tomado igual con la discusiÃ³n delante. Se seÃ±alan como tales.

---

## Contexto

Los ADR-033 a ADR-046 fijan el metamodelo del motor: `PayrollObject` como raÃ­z,
`PayrollConcept` con tipologÃ­a de cÃ¡lculo, operandos resueltos por source, agregaciÃ³n por
feed, grafo acÃ­clico, plan topolÃ³gico y segmentaciÃ³n temporal.

Sobre esa base habÃ­a que calcular lo que convierte un devengo en una nÃ³mina de verdad: la
cotizaciÃ³n a la Seguridad Social y la retenciÃ³n de IRPF. Eso obligÃ³ a resolver cuatro cosas
que el metamodelo no cubrÃ­a:

1. **Topes.** La base de cotizaciÃ³n no es el devengo: es el devengo recortado entre un tope
   mÃ¡ximo y un tope mÃ­nimo que dependen del grupo de cotizaciÃ³n del empleado.
2. **Coste de empresa.** Hay conceptos que se calculan y se muestran pero no descuentan del
   lÃ­quido.
3. **Valores que no salen de otro concepto.** Un tipo de cotizaciÃ³n o un tope no se calcula:
   se consulta.
4. **Prorrateo de los topes cuando el mes estÃ¡ partido en segmentos.**

---

## DecisiÃ³n

**La cotizaciÃ³n se modela dentro del mismo grafo de conceptos que el resto de la nÃ³mina.**
No hay motor paralelo, ni servicio de cotizaciÃ³n, ni lÃ³gica de negocio fuera del grafo. Un
tope es un nodo; un tipo es un nodo; la base de cotizaciÃ³n es un concepto con su cÃ³digo.

De ahÃ­ se derivan las cuatro decisiones concretas de este ADR.

### 1. Los topes se aplican con dos tipologÃ­as nuevas: `LEAST` y `GREATEST`

ADR-036 declarÃ³ cuatro tipologÃ­as canÃ³nicas y avisÃ³ de que no se crearÃ­an mÃ¡s por cada caso
de negocio. AquÃ­ se aÃ±aden dos, y la justificaciÃ³n es la que el propio ADR-036 exige: no
describen un caso de negocio, describen **operadores** que faltaban.

```
B_CC_MAX = LEAST(B01, P_TOPE_MAX)          â† el devengo, recortado por arriba
B_CC     = GREATEST(B_CC_MAX, P_TOPE_MIN)  â† y despuÃ©s levantado por abajo
```

Cada una toma dos operandos con roles nuevos, `LEFT` y `RIGHT`, aÃ±adidos a `OperandRole`.

**El orden importa y es deliberado**: primero el techo, despuÃ©s el suelo. La consecuencia
funcional es que un empleado cuya base queda por debajo del mÃ­nimo de su grupo cotiza por el
mÃ­nimo aunque haya devengado menos.

### 2. La base de cotizaciÃ³n es un concepto, no un campo calculado

```
B01   BASE_COTIZABLE   AGGREGATE   alimentado hoy Ãºnicamente por 101
B_CC  BASE_COTIZACION_COTIZ        la base ya recortada, que usan todos los tipos
```

Todos los conceptos de cotizaciÃ³n â€”de trabajador y de empresaâ€” cuelgan de `B_CC`, nunca de
`B01`. V88 reescribiÃ³ el operando `BASE` del concepto 700 justamente para eso.

Que la base sea un nodo del grafo y no un valor interno es lo que permite explicar una
nÃ³mina: se puede preguntar cuÃ¡nto valÃ­a la base y por quÃ©.

### 3. El coste de empresa se calcula, se muestra y no descuenta

Los conceptos 720â€“724 tienen `functionalNature = INFORMATIONAL` y **no tienen relaciÃ³n de
feed hacia 980**. Se calculan sobre la misma `B_CC`, aparecen en el recibo con su
`payslipOrderCode`, y no tocan el lÃ­quido.

Es la aplicaciÃ³n directa de la regla de ADR-036: la naturaleza funcional es presentaciÃ³n y
semÃ¡ntica; quien decide si un importe resta es la relaciÃ³n de feed, no el tipo de cÃ¡lculo.

### 4. Los valores que se consultan entran como nodos `ENGINE_PROVIDED`

`ENGINE_PROVIDED` (renombrado desde `JAVA_PROVIDED` en V89) identifica conceptos cuyo valor
lo produce una clase Java que implementa `TechnicalConceptCalculator`, registrada por Spring
en `TechnicalConceptCalculatorRegistry` bajo su cÃ³digo de concepto.

Se mantiene la regla de ADR-046: **estas clases no pueden calcular conceptos econÃ³micos.**
Solo resuelven valores que se consultan o se derivan del contexto de ejecuciÃ³n â€” tipos,
topes, dÃ­as, coeficientes. La frontera es la que separa Â«consultar un datoÂ» de Â«calcular una
nÃ³minaÂ».

---

## El grafo implementado

```
                      101 SALARIO_BASE
                        â”‚        â”‚
                        â”‚        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º 970 TOTAL_DEVENGOS â”€â”€â”
                        â–¼                                              â”‚
                  B01 BASE_COTIZABLE                                   â”‚
                        â”‚                                              â”‚
        P_TOPE_MAX â”€â”€â–º  LEAST  â”€â”€â–º B_CC_MAX                            â”‚
                                      â”‚                                â”‚
        P_TOPE_MIN â”€â”€â–º GREATEST â”€â”€â–º  B_CC                              â”‚
                                      â”‚                                â–¼
      â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”      990 LIQUIDO
      â”‚            trabajador         â”‚      empresa       â”‚           â–²
      â–¼                               â–¼                    â–¼           â”‚
  700 CC 4,70 %                720 CC 23,60 %                          â”‚
  703 DESEMPLEO 1,55 %         721 DESEMPLEO 7,05 %                    â”‚
  701 FP 0,10 %                722 FP 0,60 %          (INFORMATIONAL,  â”‚
  702 MEI 0,11 %               723 FOGASA 0,20 %       sin feed a 980) â”‚
  800 IRPF 15,00 %             724 MEI 0,58 %                          â”‚
      â”‚                                                                â”‚
      â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â–º 980 TOTAL_DEDUCCIONES â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€(invert_sign)â”€â”€â”€â”˜
```

### Conceptos

| CÃ³digo | MnemÃ³nico | Tipo | Naturaleza | FÃ³rmula | Recibo |
|---|---|---|---|---|---|
| `B01` | BASE_COTIZABLE | AGGREGATE | BASE | â† 101 | no |
| `B_CC_MAX` | BASE_COTIZACION_MAX | LEAST | BASE | min(B01, P_TOPE_MAX) | no |
| `B_CC` | BASE_COTIZACION_COTIZ | GREATEST | BASE | max(B_CC_MAX, P_TOPE_MIN) | no |
| `700` | CC_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC Ã— 4,70 % | 700 |
| `703` | DESEMPLEO_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC Ã— 1,55 % | 703 |
| `701` | FP_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC Ã— 0,10 % | 701 |
| `702` | MEI_TRABAJADOR | PERCENTAGE | DEDUCTION | B_CC Ã— 0,11 % | 702 |
| `800` | RETENCION_IRPF | PERCENTAGE | DEDUCTION | B01 Ã— 15,00 % | 800 |
| `720` | SS_CC_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC Ã— 23,60 % | 720 |
| `721` | SS_DESEMPLEO_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC Ã— 7,05 % | 721 |
| `722` | SS_FP_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC Ã— 0,60 % | 722 |
| `723` | SS_FOGASA_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC Ã— 0,20 % | 723 |
| `724` | SS_MEI_EMPRESARIO | PERCENTAGE | INFORMATIONAL | B_CC Ã— 0,58 % | 724 |

Los nodos `P_*` (`P_TOPE_MAX`, `P_TOPE_MIN`, `P_SS_CC`, `P_SS_DESEMPLEO`, `P_FP_TRAB`,
`P_MEI_TRAB`, `P_IRPF`, `P_SS_CC_EMP`, `P_SS_DESEMPLEO_EMP`, `P_SS_FP_EMP`,
`P_SS_FOGASA_EMP`, `P_SS_MEI_EMP`) son todos `ENGINE_PROVIDED` con naturaleza `TECHNICAL` y
sin lÃ­nea de recibo.

> **Nota**: el concepto 800 (IRPF) toma como base `B01`, no `B_CC`. Es correcto â€”la base de
> retenciÃ³n no es la de cotizaciÃ³nâ€” pero conviene tenerlo presente: hoy coinciden porque
> ambas se alimentan solo del salario base, y dejarÃ¡n de coincidir en cuanto B01 crezca.

---

## Topes: origen, vigencia y prorrateo

Los topes viven en `payroll_engine.ss_cotizacion_topes`, con vigencia por fecha:

```
(rule_system_code, grupo_code, period_type, base_min, base_max, valid_from, valid_to)
```

Sembrada con los valores TGSS de 2025: grupos 01â€“07 en `MENSUAL`, grupos 08â€“11 en `DIARIO`,
tope mÃ¡ximo Ãºnico de 4.909,50 â‚¬/mes y 163,65 â‚¬/dÃ­a.

**El grupo de cotizaciÃ³n y el tipo de nÃ³mina del empleado salen de
`rulesystem.agreement_category_profile`** (V85, sembrada en V87), que asocia cada categorÃ­a
del convenio a su grupo y a `MENSUAL` o `DIARIO`. La cadena completa es:

```
empleado â†’ labor_classification â†’ categorÃ­a de convenio â†’ agreement_category_profile
        â†’ (grupo de cotizaciÃ³n, tipo de nÃ³mina) â†’ ss_cotizacion_topes
```

### Prorrateo por segmento

`TopeMaxCotizacionCalculator` y `TopeMinCotizacionCalculator` no devuelven el tope del mes:
devuelven la parte que corresponde al segmento.

```
MENSUAL:  tope Ã— dÃ­asDelSegmento / dÃ­asDelPerÃ­odo
DIARIO:   tope Ã— dÃ­asDelSegmento
```

Y por eso V90 les puso `result_composition_mode = ACCUMULATE`: los trozos de todos los
segmentos se suman y el resultado consolidado es el que recortan `B_CC_MAX` y `B_CC`.

Es la decisiÃ³n menos evidente de todo el modelo y merece quedar escrita: **los topes son lo
Ãºnico que se calcula por segmento y se acumula, mientras que todos los conceptos de
cotizaciÃ³n tienen `executionScope = PERIOD`.** Si alguien cambia el Ã¡mbito de un concepto de
cotizaciÃ³n a `SEGMENT` sin entender esto, el recorte deja de cuadrar.

---

## Lo que es firme y lo que es provisional

### Firme

- La cotizaciÃ³n vive en el grafo. No hay motor paralelo.
- Los topes se aplican con operadores del grafo (`LEAST`/`GREATEST`), no con condicionales en
  Java.
- El coste de empresa es informativo y no alimenta 980.
- La base de cotizaciÃ³n es un concepto con identidad propia.
- El grupo de cotizaciÃ³n se deriva de la categorÃ­a del convenio, no se informa a mano.

### Provisional â€” y conscientemente

1. **Los tipos estÃ¡n escritos en Java.** `payroll_engine.ss_cotizacion_tipos` existe, estÃ¡
   creada y sembrada con los tipos de 2025 por contingenciaâ€¦ y **no la lee nadie**. Los diez
   tipos son constantes en sus respectivas clases `*RateCalculator`. Es la deuda mÃ¡s grave de
   este modelo: cuando la TGSS cambie los tipos habrÃ¡ que tocar cÃ³digo y desplegar, teniendo
   la tabla al lado.
2. **El IRPF es un 15 % fijo.** `IrpfWithholdingRateCalculator` lo dice en su javadoc: es un
   marcador de posiciÃ³n. El tipo real depende de los datos fiscales del empleado y se
   regulariza. La vertical `employee.tax_information` ya existe (V95); no estÃ¡ conectada.
3. **`P_SS` (6,35 % todo en uno) quedÃ³ muerto** al partirse el 700 en V91. El concepto y su
   `SsContributionRateCalculator` siguen en el Ã¡rbol sin que nadie los referencie.
4. **No hay accidentes de trabajo (725).** Requiere la tarifa por CNAE de cada empresa; queda
   aplazado explÃ­citamente desde V88.
5. **`B01` solo se alimenta del salario base.** Falta la prorrata de pagas extra y cualquier
   otro devengo cotizable. Mientras eso siga asÃ­, la base de cotizaciÃ³n de esta nÃ³mina no es
   la real.
6. **Todo estÃ¡ sembrado para un Ãºnico convenio** (99002405011982) y un Ãºnico rule system.

---

## Invariantes

- NingÃºn concepto de cotizaciÃ³n toma su base de `B01`: todos cuelgan de `B_CC`. La excepciÃ³n
  documentada es 800 (IRPF), cuya base es otra por definiciÃ³n.
- Los conceptos de empresa nunca alimentan 980.
- `P_TOPE_MAX` y `P_TOPE_MIN` deben mantener `ACCUMULATE`; el resto de la cotizaciÃ³n, `PERIOD`.
- Un tipo nuevo de cotizaciÃ³n se aÃ±ade como concepto `ENGINE_PROVIDED` + concepto
  `PERCENTAGE` + relaciÃ³n de feed, nunca como rama dentro de un calculador existente.
- Las clases `TechnicalConceptCalculator` no calculan conceptos econÃ³micos.

---

## Deuda que este ADR deja abierta

| # | Deuda | DÃ³nde |
|---|---|---|
| 1 | `ss_cotizacion_tipos` no la lee nadie; los tipos son constantes Java | los 10 `*RateCalculator` |
| 2 | IRPF fijo al 15 %, con `employee.tax_information` ya disponible | `IrpfWithholdingRateCalculator` |
| 3 | `P_SS` y `SsContributionRateCalculator` muertos desde V91 | `payroll_engine` |
| 4 | Sin AT/EP (725): falta tarifa CNAE por empresa | V88, aplazado |
| 5 | `B01` incompleta: sin prorrata de extras ni otros devengos cotizables | grafo de feeds |

---

## RelaciÃ³n con ADR anteriores

| ADR | RelaciÃ³n |
|---|---|
| ADR-036 | AmplÃ­a las tipologÃ­as canÃ³nicas de cuatro a seis con `LEAST` y `GREATEST`, por el criterio que el propio ADR-036 fija: son operadores, no casos de negocio |
| ADR-038 | La separaciÃ³n trabajador/empresa se resuelve con `FEED_BY_SOURCE`: quien alimenta 980 descuenta, quien no, informa |
| ADR-041 | El prorrateo de topes por segmento con consolidaciÃ³n `ACCUMULATE` es la aplicaciÃ³n concreta de la segmentaciÃ³n temporal |
| ADR-045 | La elegibilidad de todos estos conceptos se resuelve por `concept_assignment` |
| ADR-046 | `ENGINE_PROVIDED` es el `JAVA_PROVIDED` de ese ADR, renombrado en V89; la regla de que estas clases no calculan conceptos econÃ³micos se mantiene intacta |


<!-- END FILE: ADR-048-modelo-de-cotizacion-ss-e-irpf.md -->


---

# FILE: ADR-049-arquitectura-de-informacion-del-frontend.md
<a name="file-adr-049-arquitectura-de-informacion-del-frontend-md"></a>

<!-- BEGIN FILE: ADR-049-arquitectura-de-informacion-del-frontend.md -->

# ADR-049 â€” Arquitectura de informaciÃ³n del frontend

## Estado
Aceptado

## Contexto

La aplicaciÃ³n creciÃ³ por verticales, y la navegaciÃ³n creciÃ³ con ella. El resultado, medido sobre
el Ã¡rbol actual:

- `shared/ui` contiene un sistema de maestro-detalle completo y documentado, y lo usan solo
  `company` (791 lÃ­neas de html/scss) y `work-center` (596). `employee` son **7.530 lÃ­neas en 256
  ficheros** y no usa casi nada de Ã©l. El sistema se validÃ³ en la periferia y nunca llegÃ³ al centro.
- El menÃº calca los bounded contexts del backend, no la jornada de nadie.
- Â«CatÃ¡logosÂ» es una entrada de menÃº que nombra un **mecanismo de almacenamiento**, no un concepto
  del negocio. ADR-012 ya observÃ³ que exponer el metamodelo por una pantalla de catÃ¡logos Â«ha hecho
  visible una tensiÃ³n de diseÃ±oÂ» y que hay `rule_entity_type` nombrados desde la vertical donde el
  concepto apareciÃ³ primero. No es casualidad: una pantalla organizada por mecanismo obliga a
  nombrar cada concepto por su mecanismo.
- `rule_system`, que ADR-003 define como el contexto regulatorio, aparece como la Ãºltima entrada del
  menÃº bajo Â«ConfiguraciÃ³nÂ», en inglÃ©s.

## DecisiÃ³n

### 1. Cuatro grupos

| Grupo | Contenido | Criterio |
|---|---|---|
| Empleados | El directorio y la ficha | El core |
| OrganizaciÃ³n | Empresas, centros de trabajo, centros de coste | Lo decide la empresa |
| Sociedad | Convenios, reglamentaciÃ³n | Viene impuesto de fuera |
| NÃ³minas | Ejecuciones y sus recibos | El trabajo batch |

La separaciÃ³n OrganizaciÃ³n / Sociedad no es cosmÃ©tica: distingue lo que la empresa decide de lo que
le imponen, y eso cambia lo que la interfaz debe permitir. En OrganizaciÃ³n se edita; en Sociedad se
consultan datos con vigencia que no se pueden cambiar.

**Test de colocaciÃ³n de cualquier entidad: Â¿quiÃ©n manda sobre este dato, nosotros o el BOE?**

### 2. El sistema de reglas es Ã¡mbito, no destino

`rule_system` sale de la lista de navegaciÃ³n y pasa al cromo, junto a la marca, visible siempre.
Casi todo lo que se muestra estÃ¡ dentro de uno â€”catÃ¡logos, convenios, la ficha, la nÃ³minaâ€”, igual
que el ejercicio en un programa de contabilidad. Con un solo sistema activo se muestra igualmente.

Administrar sistemas de reglas sigue existiendo como tarea, pero es otra cosa y va en otro sitio.

### 3. Los maestros se colocan por significado, no por mecanismo

Que algo se guarde como `rule_entity` y otra cosa tenga agregado propio es un hecho del esquema,
invisible para quien usa la aplicaciÃ³n. Quien busca *tipos de contrato* y quien busca *centros de
trabajo* estÃ¡n haciendo lo mismo: decidir quÃ© se le puede poner a un empleado.

- **Se mantiene** la pantalla genÃ©rica dirigida por metadatos. Es la ganancia del metamodelo.
- **Desaparece** Â«CatÃ¡logosÂ» como grupo de menÃº. Cada maestro se lista bajo OrganizaciÃ³n o bajo
  Sociedad segÃºn el test anterior, servido por el mismo componente genÃ©rico.
- Como un maestro nuevo aparece **declarando**, sin tocar cÃ³digo, **el menÃº de esa secciÃ³n debe
  generarse** desde los `rule_entity_type`. Si aÃ±adir un maestro obliga a editar el menÃº a mano, se
  pierde la propiedad que hacÃ­a valioso el metamodelo. Esto exige metadatos de agrupaciÃ³n y
  visibilidad en `rule_entity_type` (b4rrhh/backend#15) y es el mismo campo que ADR-012 necesita
  para pagar su deuda semÃ¡ntica.

### 4. El modelo de pÃ¡gina es directorio + pÃ¡gina completa

Se descarta el maestro-detalle permanente como patrÃ³n general. La ficha del empleado no cabe en un
panel de detalle: el Ã¡rea laboral ya necesita todo el ancho para sus tablas de periodos, y un panel
fijo cuesta unos 350 px en la pantalla que mÃ¡s ancho necesita.

`company` y `work-center` se mudan al modelo de la ficha, no al revÃ©s. Las piezas de maestro-detalle
de `shared/ui` quedan superadas y **no deben usarse en pantallas nuevas**.

### 5. La selecciÃ³n mÃºltiple es una cola de trabajo

Lo que hace falta no es ver la lista todo el rato: es no perder la selecciÃ³n. Se resuelve en el raÃ­l
de la ficha, que muestra la cola con los nombres cuando se llega desde una selecciÃ³n, y solo la
identidad cuando se llega a uno solo. Coste de ancho: cero cuando no hay cola.

**La cola no escala a cientos, y no debe fingir que sÃ­.** Revisar cien fichas de una en una es la
herramienta equivocada: eso se responde aÃ±adiendo la columna al directorio y ordenando por ella.

## Consecuencias

- El directorio necesita un modelo de lectura mÃ¡s rico del que hay
  (`EmployeeDirectoryItemResponse` hoy solo devuelve claves, `displayName`, `status` y
  `workCenterCode`). La fase 4 tiene una mitad de backend.
- El reparto fino de maestros entre OrganizaciÃ³n y Sociedad **no sale limpio**: hay tipos con
  opciones de las dos clases. Cuando un tipo no caiga claramente de un lado, es seÃ±al de que ese
  tipo son dos.
- ADR-004 sigue abierto. Esta decisiÃ³n no depende de la forma de la business key.

## Fuera de alcance

La pantalla de inicio (bandeja de trabajo frente a panel de mÃ©tricas) sigue sin decidir.


<!-- END FILE: ADR-049-arquitectura-de-informacion-del-frontend.md -->


---

# FILE: ADR-050-esqueleto-de-pagina.md
<a name="file-adr-050-esqueleto-de-pagina-md"></a>

<!-- BEGIN FILE: ADR-050-esqueleto-de-pagina.md -->

# ADR-050 â€” Esqueleto de pÃ¡gina

## Estado
Aceptado

## Contexto

Cada pantalla se inventa su disposiciÃ³n. La prueba mÃ¡s limpia es el panel Â«HistorialÂ» de la ficha
del empleado: aparece en las tres Ã¡reas â€”resumen, personales, laboralesâ€” en **tres posiciones
distintas, con tres anchos distintos**, sin alinearse con nada de lo que tiene debajo. Si un
componente no sabe dÃ³nde va, es que no hay ningÃºn sitio donde deba ir.

De ahÃ­ sale todo lo demÃ¡s:

- El Ã¡rea laboral es una pÃ¡gina de dos columnas donde la izquierda tiene una caja y **600 px de nada**.
- El Ã¡rea personal deja el 85 % de la pantalla en blanco.
- El directorio ocupa poco mÃ¡s de la mitad del ancho disponible y desperdicia el resto.
- Las acciones de pÃ¡gina (Â«Calcular nÃ³minaÂ», Â«AccionesÂ») viven **dentro de una card**, en el sitio de
  un dato.
- El raÃ­l de identidad se colapsa a iconos en un Ã¡rea y va con etiquetas en las otras dos, de forma
  que en la pantalla de resumen **desaparece el nombre del empleado**.

Ninguno de estos es un fallo de estilo. Son sÃ­ntomas de que no hay un plano.

## DecisiÃ³n

Existe **un Ãºnico esqueleto de pÃ¡gina** al que las pantallas se acogen, con cuatro huecos nombrados:

| Hueco | QuÃ© lleva | Notas |
|---|---|---|
| `identidad` | QuiÃ©n o quÃ© se estÃ¡ mirando, y las acciones de pÃ¡gina | Franja superior, ancho completo |
| `raÃ­l` | Ãndice de la pÃ¡gina y, si la hay, la cola de trabajo | Izquierda, plegable como una unidad |
| `principal` | El contenido | Gobierna las columnas |
| `contextual` | Paneles secundarios: historial, ayuda, auditorÃ­a | Derecha, **plegado por defecto** |

### Reglas

1. **Las acciones de pÃ¡gina van en `identidad`**, nunca dentro de una card del contenido.
2. **`contextual` estÃ¡ plegado por defecto** y se despliega a peticiÃ³n. Su estado se recuerda.
3. **El raÃ­l se pliega entero**, no por partes. Ãndice y cola viven o desaparecen juntos.
4. **El menÃº principal se pliega a iconos**, y su estado se recuerda. Es la Ãºnica navegaciÃ³n que
   admite plegarse a iconos, porque son pocos destinos usados a diario y se reconocen por la forma.
   **El Ã­ndice del raÃ­l no se pliega a iconos**: es un sumario que se lee de reojo, sus conceptos son
   vecinos entre sÃ­ â€”centro de trabajo y centro de coste, convenio y reglamentaciÃ³nâ€” y obligar a
   pasar el ratÃ³n por cada uno lo convierte en una adivinanza. Se aprieta con tipografÃ­a, no con
   iconos.
5. **El Ã­ndice informa, no solo navega**: lleva el recuento de cada secciÃ³n y marca en gris las
   vacÃ­as. Lo excepcional se ve porque lo normal calla.
6. **La identidad no cambia entre secciones de la misma entidad.** Nunca puede desaparecer el nombre
   de lo que se estÃ¡ mirando.

### El ancho

Ancho completo no es la respuesta automÃ¡tica: una tabla de 2.500 px es ilegible porque el ojo pierde
la fila. **La medida de lectura manda sobre el ancho disponible**, y donde sobre espacio se usa para
poner cosas al lado, no para estirar. El esqueleto decide esto una vez, no cada pantalla.

## Consecuencias

- Migrar una pantalla al esqueleto **no debe obligar a reescribir su contenido**. Si obliga, el
  esqueleto estÃ¡ mal y hay que revisar este ADR. Esa restricciÃ³n permite desplegar el esqueleto antes
  de rediseÃ±ar nada.
- Las clases de contenedor propias de cada feature dejan de tener sentido. Ver ADR-051.
- Los huecos se dimensionan una vez, en el esqueleto. Ninguna pantalla ajusta anchos por su cuenta.

## Fuera de alcance

QuÃ© aspecto tiene cada bloque dentro de `principal`. Eso es ADR-051.


<!-- END FILE: ADR-050-esqueleto-de-pagina.md -->


---

# FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md
<a name="file-adr-051-el-contenedor-deriva-del-modo-de-mantenimiento-md"></a>

<!-- BEGIN FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md -->

# ADR-051 â€” El contenedor deriva del modo de mantenimiento

## Estado
Aceptado

## Contexto

Hoy el contenedor de una secciÃ³n lo elige quien escribe la pantalla. El resultado, en una sola
pantalla â€”el Ã¡rea laboral de la fichaâ€” es que **la presencia es una card y el contrato, la jornada y
el convenio no lo son**, siendo los cuatro del mismo rango y las cuatro secciones temporales. Ninguna
de las dos elecciones codifica nada: no se puede deducir por quÃ© una tiene caja y las otras no.

Por debajo hay **19 ficheros `.scss`** que declaran cada uno su propia caja: `.presence-card`,
`.labor-section`, `.work-center-section`, `.work-center-detail-panel`, `.cost-center-section`,
`.dnf-card`, `.journey-presence-card`, `.header-box`, `.identity-panel`, `.list-panel`,
`.employee-timeline-panel`, `.tax-information-section`, `.working-time-section`,
`.labor-classification-section`, `.rehire-section`, `.panel`, `.section-card` y algunas mÃ¡s.

Ninguna se escribiÃ³ de mala fe: cada una es alguien resolviendo el mismo problema otra vez.

AdemÃ¡s, la presencia â€”que en el dominio gobierna sobre las demÃ¡s verticales, hasta el punto de que
el flujo de cese la cierra la primera (ADR-047)â€” es visualmente lo menos importante de su pantalla.

## DecisiÃ³n

**El modo de mantenimiento de una secciÃ³n determina su contenedor, su cabecera y su acciÃ³n.** El
vocabulario ya existe en ADR-010 y ADR-016: `SLOT`, `TEMPORAL_APPEND_CLOSE`, `WORKFLOW`, `READONLY`.

Consecuencia directa: **dos secciones con el mismo modo se ven iguales**. Al verla, ya se sabe quÃ© se
puede hacer con ella, antes de leer su contenido. Hoy eso no es posible.

### Reglas

1. **Un modo, un tratamiento.** Presencia, contrato, jornada y convenio son todas
   `TEMPORAL_APPEND_CLOSE` y comparten caja, cabecera y acciÃ³n. Lo Ãºnico que distingue a la presencia
   es una marca de que **gobierna** sobre las demÃ¡s.
2. **Los contenedores viven en `shared/ui`.** Una feature no declara cajas. Se pondrÃ¡ un candado en
   el pipeline cuando la primera secciÃ³n estÃ© migrada.
3. **Nada de contenedor dentro de contenedor.** El caso actual â€”tres cards, cada una con una caja
   gris dentro para decir que no hay datosâ€” es el ejemplo a no repetir. El estado vacÃ­o es contenido
   de la secciÃ³n, no otra secciÃ³n.
4. **El cÃ³digo nunca va solo.** `420` se muestra como Â«Indefinido a tiempo parcialÂ» con el cÃ³digo
   debajo, en gris y en monoespaciada. Quien conoce el catÃ¡logo sigue leyendo el nÃºmero; quien no,
   entiende la fila. Requiere el binding de literales de ADR-015.
5. **Las fechas en formato local**, con `formatDisplayDate`. Las tablas de periodos las muestran hoy
   en ISO pese a que ese trabajo se hizo.

### Corolario: la ficha del empleado

La ficha tiene **dos naturalezas, no cinco Ã¡reas hermanas**:

- **La relaciÃ³n laboral**: presencia, contrato, jornada, convenio, centro de trabajo, centro de
  coste. Todo son vigencias y **se solapan entre sÃ­**. Se presenta como un eje temporal con sus
  carriles debajo, en una sola pÃ¡gina.
- **La persona**: contactos, identificadores, foto. Sin eje temporal, o con otro.

Partir la primera en pestaÃ±as hermanas esconde justo lo que hay que ver, porque **los solapes ocurren
entre las pestaÃ±as**. Desaparece el Ã¡rea Â«ResumenÂ»: el resumen *es* la lÃ­nea de tiempo.

### Nota (frontend#25): de dÃ³nde viene la jerarquÃ­a

Â«Un modo, un tratamientoÂ» produce pÃ¡ginas uniformes cuando la entrada es uniforme: las seis
secciones de la relaciÃ³n laboral comparten modo, luego se ven iguales, y una pÃ¡gina donde todo se
ve igual no tiene jerarquÃ­a. La regla se mantiene. La jerarquÃ­a viene de otros dos sitios:

- **De dentro de la secciÃ³n**: lo vigente manda sobre lo cerrado. La fila en vigor es la Ãºnica en
  tinta plena; las cerradas, apagadas. La historia no se pliega: con el estado de hoy arriba ya es
  secundaria por posiciÃ³n, y esconderla aÃ±adirÃ­a un clic a algo que se abre a menudo. La jerarquÃ­a
  se consigue bajando su peso, no ocultÃ¡ndola.
- **De la pÃ¡gina**: primero el estado actual, despuÃ©s la historia. La relaciÃ³n lleva un bloque
  Â«HoyÂ» entre la lÃ­nea de vida y las secciones â€”una lÃ­nea por vigencia, en el mismo orden que los
  carriles del eje, el Ã­ndice del raÃ­l y las secciones, con el valor, su cÃ³digo y desde cuÃ¡ndo
  rigeâ€”, y las tablas van debajo como historia. Es el borde derecho de la lÃ­nea de vida, escrito.
  Y es donde aparece lo anÃ³malo: sin centro de coste se lee Â«sin asignarÂ» en tono de aviso.

Corolarios: una marca de modo que llevan todas las secciones de una pÃ¡gina no informa de nada y se
quita (queda la de la que **gobierna**, que solo lleva una); el Ã©nfasis de la caja se invierte: la
lleva el bloque Â«HoyÂ» y la pierden las secciones de historia, que quedan en tÃ­tulo, regla y tabla.
La caja dejÃ³ de significar algo en cuanto la tuvieron todas; dÃ¡rsela solo a lo que importa es lo
que crea la jerarquÃ­a.

## Consecuencias

- AÃ±adir una vertical temporal deja de ser una decisiÃ³n de diseÃ±o: hereda el tratamiento de su modo.
- Las 19 clases propias se van borrando a medida que se migra cada secciÃ³n. Nada de big bang: cada
  secciÃ³n migrada borra la suya.
- Si una secciÃ³n necesita un tratamiento que su modo no da, o el modo estÃ¡ mal asignado o falta un
  modo. En ningÃºn caso se resuelve con una clase nueva en la feature.

## Fuera de alcance

La escala del eje temporal â€”cÃ³mo se representa una relaciÃ³n de veinte aÃ±os con un cambio de dos
semanasâ€” sigue sin resolver, y es el riesgo conocido de la implementaciÃ³n.


<!-- END FILE: ADR-051-el-contenedor-deriva-del-modo-de-mantenimiento.md -->


---

# FILE: ADR-28-payroll-calculation-launch-semantics.md
<a name="file-adr-28-payroll-calculation-launch-semantics-md"></a>

<!-- BEGIN FILE: ADR-28-payroll-calculation-launch-semantics.md -->

# ADR â€” Payroll Calculation Launch Semantics

## Estado
Propuesto

## Contexto

B4RRHH organiza el cÃ³digo por vertical/subdominio y exige APIs pÃºblicas basadas en business keys, nunca en IDs tÃ©cnicos. AdemÃ¡s, cuando una operaciÃ³n no encaja como CRUD plano, el proyecto favorece modelarla como una acciÃ³n de negocio o workflow explÃ­cito. îˆ€fileciteîˆ‚turn4file10îˆ îˆ€fileciteîˆ‚turn4file12îˆ îˆ€fileciteîˆ‚turn4file8îˆ

En el bounded context `payroll` ya se ha decidido que:

- la raÃ­z funcional es `payroll.payroll`;
- su identidad funcional es:
  - `ruleSystemCode`
  - `employeeTypeCode`
  - `employeeNumber`
  - `payrollPeriodCode`
  - `payrollTypeCode`
  - `presenceNumber`;
- la nÃ³mina es un resultado materializado, no editable, regenerable por cÃ¡lculo;
- las hijas cuelgan con `ON DELETE CASCADE`;
- solo `NOT_VALID` es estado recalcable entre las nÃ³minas ya existentes. îˆ€fileciteîˆ‚turn4file1îˆ îˆ€fileciteîˆ‚turn4file6îˆ

TambiÃ©n se ha fijado que `employee.presence` es un recurso funcional identificado por business key ampliada `ruleSystemCode + employeeTypeCode + employeeNumber + presenceNumber`, y que las acciones de negocio compuestas deben vivir como workflows por encima de los recursos canÃ³nicos. îˆ€fileciteîˆ‚turn4file14îˆ îˆ€fileciteîˆ‚turn4file16îˆ îˆ€fileciteîˆ‚turn4file8îˆ

Al empezar a hablar de cÃ¡lculo de nÃ³mina aparece una tensiÃ³n natural:

- una cosa es el **modelo de datos del resultado** (`payroll.payroll`);
- otra cosa distinta es el **lanzamiento del cÃ¡lculo**.

Si ambas cosas se mezclan demasiado pronto, el diseÃ±o queda borroso y se dificulta la evoluciÃ³n futura del motor de reglas.

## Problema

Se necesita definir quÃ© significa tÃ©cnicamente â€œlanzar nÃ³minaâ€ sin entrar todavÃ­a en el motor real de reglas de cÃ¡lculo.

El sistema debe poder:

- recibir un perÃ­odo y un tipo de nÃ³mina;
- resolver una poblaciÃ³n objetivo;
- expandir esa poblaciÃ³n a unidades reales de cÃ¡lculo;
- decidir cuÃ¡les son elegibles;
- delegar el cÃ¡lculo efectivo a otro caso de uso especializado;
- devolver un resumen de ejecuciÃ³n.

AdemÃ¡s, el launch no debe recalcular indiscriminadamente:

- una nÃ³mina existente en `CALCULATED` no debe tocarse;
- una nÃ³mina existente en `EXPLICIT_VALIDATED` no debe tocarse;
- una nÃ³mina `DEFINITIVE` jamÃ¡s debe tocarse;
- una unidad sin nÃ³mina previa sÃ­ debe calcularse;
- una unidad con nÃ³mina previa en `NOT_VALID` sÃ­ debe recalcularse.

## DecisiÃ³n

Se introduce la semÃ¡ntica de **Payroll Calculation Launch** como workflow de aplicaciÃ³n dentro del bounded context `payroll`.

El launch:

- **no es** la raÃ­z funcional del dominio;
- **no es** un CRUD;
- **no es** todavÃ­a un recurso persistente canÃ³nico tipo `payroll_run`;
- **no implementa** por sÃ­ mismo el motor de cÃ¡lculo;
- **resuelve y orquesta** quÃ© unidades deben intentarse calcular.

### Regla principal

`launch` resuelve la lista de unidades de cÃ¡lculo elegibles y delega el cÃ¡lculo efectivo a un caso de uso/endpoint especializado de cÃ¡lculo.

## DefiniciÃ³n funcional

Lanzar nÃ³mina significa:

> ejecutar un workflow que, para un `ruleSystemCode`, `payrollPeriodCode`, `payrollTypeCode` y una poblaciÃ³n objetivo determinada, resuelve las unidades de cÃ¡lculo candidato, considera elegibles las que no tienen nÃ³mina previa o la tienen en `NOT_VALID`, delega el cÃ¡lculo efectivo a un componente especializado y devuelve un resumen de ejecuciÃ³n.

## Unidad funcional de cÃ¡lculo

La unidad mÃ­nima de cÃ¡lculo es:

- `ruleSystemCode`
- `employeeTypeCode`
- `employeeNumber`
- `payrollPeriodCode`
- `payrollTypeCode`
- `presenceNumber`

JustificaciÃ³n:

- `payroll.payroll` ya estÃ¡ anclada a una presencia concreta; îˆ€fileciteîˆ‚turn4file1îˆ
- `presence` tiene identidad pÃºblica propia dentro del empleado; îˆ€fileciteîˆ‚turn4file14îˆ îˆ€fileciteîˆ‚turn4file16îˆ
- dos presencias distintas en el mismo mes representan nÃ³minas independientes.

El launch trabaja con una colecciÃ³n de estas unidades, no con â€œempleados enterosâ€ de forma opaca.

## PoblaciÃ³n objetivo vs poblaciÃ³n elegible

Se distinguen dos conceptos:

### 1. PoblaciÃ³n objetivo

Es el conjunto de empleados o Ã¡mbitos sobre los que el usuario desea lanzar el cÃ¡lculo.

Ejemplos posibles:

- un empleado;
- una lista explÃ­cita de empleados;
- todos los empleados de un `ruleSystemCode`;
- futuros filtros mÃ¡s ricos.

### 2. PoblaciÃ³n elegible

Es el conjunto de unidades de cÃ¡lculo que realmente pueden entrar al cÃ¡lculo efectivo.

Una unidad es elegible si:

- **no existe** `payroll.payroll` para su business key funcional; o
- **existe** y su `status = NOT_VALID`.

Una unidad no es elegible si existe y su estado es:

- `CALCULATED`
- `EXPLICIT_VALIDATED`
- `DEFINITIVE`

## Responsabilidades del launch

El launch debe:

1. recibir el contexto de ejecuciÃ³n;
2. resolver la poblaciÃ³n objetivo;
3. expandirla a unidades de cÃ¡lculo candidatas;
4. comprobar existencia y estado de `payroll.payroll`;
5. construir la lista final de unidades elegibles;
6. delegar el cÃ¡lculo efectivo;
7. consolidar un resumen de ejecuciÃ³n.

El launch no debe:

- generar directamente conceptos de nÃ³mina;
- decidir reglas salariales;
- prorratear;
- aplicar retroactividad real;
- convertirse en el motor de cÃ¡lculo.

## Contexto mÃ­nimo de ejecuciÃ³n

El launch debe trabajar al menos con:

- `ruleSystemCode`
- `payrollPeriodCode`
- `payrollTypeCode`
- `calculationEngineCode`
- `calculationEngineVersion`
- `targetSelection`

Los dos campos de engine son obligatorios por coherencia con el modelo raÃ­z ya adoptado para `payroll.payroll`. îˆ€fileciteîˆ‚turn4file1îˆ

## targetSelection

`targetSelection` representa la poblaciÃ³n objetivo.

No se fija todavÃ­a un Ãºnico shape contractual cerrado, pero el modelo debe permitir al menos:

- cÃ¡lculo de un empleado concreto;
- cÃ¡lculo de una lista explÃ­cita;
- cÃ¡lculo masivo por Ã¡mbito.

El diseÃ±o exacto del payload se cerrarÃ¡ en OpenAPI posterior.

## DelegaciÃ³n al cÃ¡lculo efectivo

El launch no implementa el cÃ¡lculo. DelegarÃ¡ en un caso de uso/endpoint especializado, en adelante `calculate`.

Esta separaciÃ³n permite:

- probar el flujo completo antes de tener motor real;
- evolucionar el componente de cÃ¡lculo sin rediseÃ±ar el launch;
- distinguir claramente entre orquestaciÃ³n y cÃ¡lculo.

## Resultado del launch

El launch debe devolver un resumen explÃ­cito de ejecuciÃ³n.

Campos esperables del resumen:

- total de candidatos detectados;
- total de unidades elegibles;
- total de unidades no elegibles por estado;
- total de unidades calculadas con resultado `CALCULATED`;
- total de unidades calculadas con resultado `NOT_VALID`;
- total de errores tÃ©cnicos;
- detalle opcional por unidad.

No se decide todavÃ­a persistir este resumen como recurso canÃ³nico.

## QuÃ© se rechaza explÃ­citamente

Se rechaza en esta fase:

- modelar `launch` como CRUD;
- mezclar launch y cÃ¡lculo efectivo en la misma semÃ¡ntica;
- recalcular cualquier nÃ³mina encontrada dentro de la poblaciÃ³n objetivo;
- introducir ya un `payroll_run` como centro del dominio;
- abrir todavÃ­a un repositorio/microservicio separado sÃ³lo para el cÃ¡lculo.

## RelaciÃ³n con el workflow de estados

Este ADR no sustituye al ADR de estados de nÃ³mina.

Se complementa con Ã©l:

- `NOT_VALID` sigue siendo el estado que autoriza la sustituciÃ³n de una nÃ³mina existente; îˆ€fileciteîˆ‚turn4file0îˆ
- ademÃ¡s, una unidad sin nÃ³mina previa es tambiÃ©n elegible para cÃ¡lculo.

## API conceptual inicial

A falta de OpenAPI definitivo, se recomienda un endpoint de negocio del estilo:

- `POST /payroll/calculations/launch`

El nombre debe seguir semÃ¡ntica de negocio, no nomenclatura tÃ©cnica vaga. El proyecto prioriza nombres orientados a negocio y paths por business keys cuando aplica. îˆ€fileciteîˆ‚turn4file12îˆ

## Consecuencias

### Positivas

- separa claramente modelo y proceso;
- permite probar el flujo completo sin motor real;
- protege de recÃ¡lculos accidentales;
- deja abierta evoluciÃ³n futura del motor;
- encaja con el patrÃ³n del proyecto de workflows explÃ­citos. îˆ€fileciteîˆ‚turn4file8îˆ

### Costes

- introduce un caso de uso adicional;
- exige resolver correctamente la expansiÃ³n de poblaciÃ³n a presencias;
- obliga a diseÃ±ar un resumen de ejecuciÃ³n Ãºtil.

## Resumen

En B4RRHH, `launch` no calcula la nÃ³mina por sÃ­ mismo.

`launch` es el workflow que:

- resuelve la poblaciÃ³n objetivo;
- expande a unidades reales de cÃ¡lculo;
- considera elegibles las unidades sin nÃ³mina previa o con nÃ³mina `NOT_VALID`;
- delega el cÃ¡lculo efectivo;
- devuelve un resumen explÃ­cito del proceso.


<!-- END FILE: ADR-28-payroll-calculation-launch-semantics.md -->

