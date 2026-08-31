ADR — Racionalización de naming y alcance semántico de rule_entity_type en B4RRHH
Estado

Aceptado — con la clasificación de la decisión 2 sustituida por el ADR-054 (backend#15, backend#37).

## Cierre (2026-08-31)

Este ADR llevaba en «Propuesto» desde que se escribió. Se cierra ahora porque sus dos mitades ya
tienen respuesta, y no la misma.

**Lo que se adoptó tal cual.** Las reglas de nombrado —1, y 4 a 6— están vigentes y ya se han
aplicado: la regla 1 («un tipo nombra el concepto funcional real, no el primer sitio donde se usó»)
es literalmente lo que ejecutaron la V35, que migró `EMPLOYEE_PRESENCE_COMPANY` a `COMPANY` y borró
el tipo viejo, y la V37, que hizo lo propio con `EMPLOYEE_CONTACT_TYPE` → `CONTACT_TYPE`. La
estrategia incremental que este ADR pedía —nada de *big bang*— se respetó: dos renombrados, cada uno
con su migración y su motivo.

Esa misma regla es la que hizo que el ADR-054 §1 **rechazara** añadir un nombre de interfaz al tipo:
si un tipo está mal nombrado se renombra, no se le pone una etiqueta encima que tape el problema.

**Lo que quedó sustituido.** La decisión 2 proponía clasificar los tipos por alcance semántico en tres
familias —*domain reusable*, *employee-specific*, *lifecycle-specific*—. Esa clasificación **nunca se
implementó y no se va a implementar**: describe de dónde viene un concepto, que es una guía para quien
elige un nombre, no un dato del que nadie deriva nada.

El ADR-054 clasifica los tipos por **tres ejes distintos, cada uno con su consumidor**: la clase del
literal (traducibilidad, universalidad, forma del alta), el modo de mantenimiento (qué acción ofrece
la pantalla) y el grupo (dónde vive en el menú). El criterio que decidió la diferencia es el del
ADR-053 §5: entra lo que alguien consume y una guardia puede vigilar. `A / B / C` no lo consumía
nadie.

Lo que sí sobrevive de la decisión 2 es su *regla práctica* —no poner prefijo de vertical porque el
primer consumidor pertenezca a esa vertical—, que es parte de las reglas de nombrado y sigue viva en
el checklist del final.


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