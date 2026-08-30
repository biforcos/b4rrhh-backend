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
   deja por debajo, se pliega. Medido en la ficha del empleado en una pantalla de 1920 px: con el
   contextual abierto `principal` mide 1617 px frente a una medida de 1400 px —217 px de holgura— y
   con el contextual cerrado quedan unos 800 px de papel muerto a la derecha. «Plegado siempre» era
   una regla escrita antes de medir; el ancho es un dato, no una preferencia. El estado que elija el
   usuario se recuerda y manda sobre el inicial.
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
