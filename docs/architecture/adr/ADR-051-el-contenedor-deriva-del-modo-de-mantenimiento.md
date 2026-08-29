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
