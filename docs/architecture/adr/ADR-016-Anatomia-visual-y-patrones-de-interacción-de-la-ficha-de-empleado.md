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

### Nota (b4rrhh/frontend#117, 30/09/2026): la fase 1 nombra piezas que ya no existen

Este ADR se anota y no se reescribe: es historia. Las dos piezas de la **fase 1** del plan,
**`employee-page-header`** y **`employee-section-shell`**, se retiraron en el commit `d5f3948`
del frontend, porque nadie las montaba. La estructura de la ficha la rige el **ADR-051**: los
contenedores viven en `shared/ui` y los decide el modo de mantenimiento de la sección.
