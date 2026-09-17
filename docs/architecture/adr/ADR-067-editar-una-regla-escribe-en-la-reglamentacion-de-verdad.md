# ADR-067 — Editar una regla escribe en la reglamentación de verdad, para todos y al instante

## Estado
Aceptado

## Contexto

El paso 7 del camino pone delante del visitante el gesto que cierra el producto: **tocar la regla que
produjo un número y ver cambiar el recibo**. Desde el recibo se salta a la fila de tabla salarial que
puso el precio, se edita, se vuelve, y el recibo dice que las reglas han cambiado.

Ese gesto no lleva ni una línea de código de demo. **Editar una fila de tabla salarial es lo que hace
un administrador de nóminas cada vez que se actualiza el convenio.** La pantalla que lo hace es la
misma que haría si detrás hubiera una empresa de verdad, y el endpoint es el que ya existía.

Y ahí aparece la tentación, que no es hipotética: ya se cometió una vez en este mismo paso y la
corrigió Juan. **Poner un aislamiento «por si el visitante rompe algo»** — un borrador, una rama de
reglamentación, una copia de la tabla que sólo ve quien la edita, un modo de edición que no escribe
de verdad.

## Decisión

**Editar una regla escribe en la reglamentación de verdad, para todos, al instante. No hay
borradores, ni ramas, ni sandbox, y no debe haberlos.**

Lo que protege la demo es el **reinicio nocturno** del `deploy`, que restaura la semilla. No un
mecanismo dentro del producto.

### Por qué no un sandbox

Porque el sandbox no sería una protección: sería **una segunda semántica de edición** que el producto
no tiene y no quiere.

1. **Capa el producto para proteger la demo.** Un administrador de nóminas que actualiza el convenio
   no está haciendo una prueba: está cambiando el precio del día, para todo el mundo, desde ya. Un
   modo que no escriba no enseña el producto, enseña una maqueta del producto.
2. **Es una segunda forma de editar.** Dos caminos de escritura divergen; es el mismo argumento del
   ADR-062 §1, y allí ya costó. La pantalla de tablas es una y se entra a ella igual desde el menú
   que desde el salto del recibo, **porque es la misma**.
3. **Quita lo único que hace que el paso signifique algo.** Si el cambio no alcanza a los demás
   recibos, la marca de «las reglas han cambiado» del `backend#107` no tiene qué decir, y el paso 7
   se queda en una animación.

### Lo que sí se hace, que es decirlo

Que la escritura sea real es exactamente por lo que el recibo tiene que **avisar**. Las dos decisiones
son la misma:

- El recibo no cambia solo — es lo que el motor calculó (ADR-062).
- Pero **dice** si la reglamentación se tocó después, y ofrece recalcular (`backend#107`).

Sobre-avisar es deliberado: la comparación es contra el último cambio del sistema de reglas entero,
así que un cambio en un concepto que este empleado no usa levanta la marca igual. **Nunca dice fresco
cuando está rancio**, y por eso se redacta como *«puede que ya no refleje las reglas actuales»* y
nunca como una afirmación.

## Consecuencias

- **El visitante de la demo puede dejar la reglamentación distinta de como la encontró.** Es
  correcto, y es lo que se quiere enseñar. El reinicio nocturno la devuelve.
- **Nadie tiene que preguntar si una edición "cuenta".** Cuenta. No hay un estado intermedio en el
  que un cambio exista para unos y no para otros.
- **Un cambio pequeño levanta la marca en 873 recibos.** Es el coste de sobre-avisar y se paga a
  sabiendas: afinarlo exigiría saber qué conceptos alcanzan a qué recibo, que es otro problema
  —reconstruir la reglamentación de una ejecución— y no hace falta para éste.
- **Si alguna vez hacen falta borradores de convenio**, serán una capacidad de producto con su propio
  modelo —vigencias futuras, aprobación, publicación— y su propio ADR. No un aislamiento puesto de
  lado para que la demo no se estropee. Las vigencias, de hecho, ya son la forma que el modelo tiene
  de decir «esto entra en vigor el mes que viene»: una fila nueva con su `start_date`.
