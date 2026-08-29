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
