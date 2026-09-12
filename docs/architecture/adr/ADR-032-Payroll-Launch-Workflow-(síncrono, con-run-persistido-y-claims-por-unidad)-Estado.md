ADR — Payroll Launch Workflow (síncrono, con run persistido y claims por unidad)
Estado

Propuesto

> **Ya no es sincrono.** El ADR-060 sustituye la «primera iteracion» de este documento en una sola
> cosa: la espera. El lanzamiento crea el run, contesta 202 y trabaja fuera de la peticion. Todo lo
> demas de aqui —launch coordina, calculate materializa, claim excluye, run resume; la unidad minima;
> los contadores; los mensajes— sigue vigente tal cual. Donde este documento dice «sin asincronia»,
> manda el ADR-060 (`backend#75`).

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