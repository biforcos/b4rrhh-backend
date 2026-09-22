# ADR-071 — El modelo oficial tiene tres bases, y cada cuota lee la suya

## Estado
Aceptado

## Contexto

El motor tenía **una** base de cotización. La montó la `V88` para poder jugar con los topes —`B01`
agregada, `B_CC_MAX = LEAST(B01, tope máximo)`, `B_CC = GREATEST(B_CC_MAX, tope mínimo)`— y las
nueve cuotas, las cuatro del trabajador y las cinco de la empresa, colgaban todas de `B_CC`.

Mientras el único devengo fue el salario base eso no se distinguía de lo correcto. Dejó de serlo el
día que el `backend#104` declaró las horas extraordinarias: la `V133` las enchufó a `B01` con el
comentario «las horas extra cotizan», que es cierto, pero **no ahí**. En la demo eso eran 138,78 €
de horas extra de `EMP001000` cotizando por contingencias comunes, y 245 de los 863 recibos con el
mismo defecto.

El síntoma por el que se abrió el issue era más pequeño: el recuadro de bases del recibo no se leía
como una suma. Los dos son la misma cosa. Un recuadro que no suma es lo que se ve cuando debajo hay
una base donde deberían estar tres.

## Decisión 1 — Tres bases, y cada cuota lee la suya

| base | qué es | topes | la leen |
|---|---|---|---|
| **contingencias comunes** (`B_CC`) | remuneración mensual **sin horas extra** + prorrata de pagas | mín. y máx. **del grupo de cotización** | `700`, `702`, `720`, `724` |
| **contingencias profesionales y recaudación conjunta** (`B_CP`) | la de comunes **ya topada** + horas extra | máximo el mismo; mínimo el **tope mínimo de cotización**, que no es el del grupo | `701`, `703`, `721`, `722`, `723`, y el AT/EP del `backend#122` |
| **horas extraordinarias** (`B08`) | el importe de las horas extra | **ninguno** | `704` (4,70 % trabajador) y `726` (23,60 % empresa) |

El MEI (`702`, `724`) cotiza sobre la base de **comunes**, y por eso se queda donde estaba. El resto
de la recaudación conjunta —desempleo, formación profesional, FOGASA— cambia de base.

**En un recibo sin horas extra no se mueve ni un céntimo**: `B_CP = B_CC + 0`. Ésa es la propiedad
que hace que el cambio sea seguro y, a la vez, la razón por la que un test cómodo no prueba nada:
las tres bases sólo se distinguen cuando hay horas extra.

### Los topes se declaran por contingencia

`ss_cotizacion_topes` sabía decir el mínimo y el máximo de un grupo, y hacían falta dos respuestas
distintas para el mismo empleado. Se le añade `contingency_code` (`COMUNES` | `PROFESIONALES`) y
cuatro nodos `ENGINE_PROVIDED` en vez de dos, emparejados con su contingencia en
`SsCotizacionTopeCalculators` —el mismo patrón con el que el `backend#105` emparejó concepto y tipo.

Escribir «el mínimo es el mínimo» habría funcionado para los grupos bajos, donde las dos cifras
coinciden, y habría fallado en silencio en los altos. Es exactamente la clase de error que este
metamodelo existe para no tener: **lo que interviene en un cálculo se ve en el grafo**.

## Decisión 2 — La remuneración mensual se define por exclusión, no por lista

«Remuneración mensual» es los devengos cotizables **sin** horas extra y **sin** la prorrata. La
forma natural de escribirlo en un grafo de alimentaciones es una lista: hoy, el `101`. Y esa lista
envejece sola —el día que alguien añada un plus cotizable tendría que acordarse de alimentar dos
sitios, y olvidarlo no rompe nada: sólo descuadra el recuadro.

Así que se escribe al revés, que es como lo dice el modelo:

```
B03 (remuneración mensual) = B01 (base de comunes) − B04 (prorrata)
```

`B01` sigue siendo la puerta por la que un devengo cotizable entra en la base, igual que hasta hoy,
y `B03` es lo que queda al quitarle la prorrata. **Un devengo nuevo aparece en la remuneración
mensual sin tocar el catálogo.**

Lo que el papel lee de arriba abajo —remuneración + prorrata = base— y lo que el motor calcula
—base − prorrata = remuneración— son la misma identidad escrita en las dos direcciones. La del
motor es la que no envejece.

La prorrata del recuadro (`B04`) es la suma de las dos puertas del ADR-070: `103 + B02`. Como los
dos coeficientes suman uno siempre, eso es la prorrata del mes exactamente una vez, sea cual sea el
régimen. **El `B02` deja de imprimirse por su cuenta**, y con eso el recuadro enseña la misma línea
de prorrata a los dos regímenes; antes se la enseñaba sólo a quien no la cobra.

## Decisión 3 — El recuadro se declara en cuatro apartados, no se dibuja

El recuadro del modelo oficial es **un** bloque con cuatro partes numeradas dentro. La `V138`
declaró los cinco bloques del recibo colgando de la naturaleza; aquí hace falta un nivel más, y se
declara igual: `payroll_engine.payslip_subsection`, y la subsección viaja congelada con la línea
como viajan la sección, el nombre y el orden.

Partirlo en cuatro secciones hermanas era más barato —cero cambios en las dos salidas— y se
descartó: haría desaparecer del papel el título del bloque, y el recibo pasaría de cinco bloques a
ocho sin que ninguno se llame ya como el modelo.

**La subsección cuelga del concepto y la sección de la naturaleza, y eso no es una incoherencia:**

- La **sección** dice de qué *clase* es la línea —un devengo, una deducción, una base— y eso ya lo
  dice `functional_nature`. Atarla al concepto obligaría a repetir la misma decisión 65 veces.
- La **subsección** dice *a qué base* pertenece, y eso **no se deduce de la naturaleza**: las diez
  líneas del recuadro son todas `BASE` y viven en cuatro apartados distintos.

## Consecuencias

- Se mueven **exactamente** los 245 recibos con horas extra de la semilla, y en ellos sólo el `700`,
  el `702`, el `720`, el `724` (bajan), el `980`, el `990` y el `725`. Los 618 sin horas no mueven
  ningún importe.
- Las cuotas de recaudación conjunta (`701`, `703`, `721`, `722`, `723`) **no se mueven** salvo
  cuando un tope muerde: sin topes, `B_CP = B_CC + horas` es la `B01` de antes.
- El catálogo `ESP` pasa de 50 conceptos a 65, y de 20 líneas posibles de recibo a 29. El recibo de
  un empleado sin horas extra pasa de 18 líneas a 23.
- **El recibo más largo de la semilla ya no cabe en una hoja A4**: 31 líneas. Es una decisión de
  maqueta pendiente y está anotada donde se ve, en el test que lo vigila.
- El motor sabe declarar **un nivel de agrupación dentro de un bloque**, que es lo que el modelo
  oficial pide y no tenía.

### Tres invariantes nuevas, y las dos anteriores siguen a cero

```
B01 = B03 + B04      el bloque 1 del recuadro suma a la vista
B07 = B05 + B06      el bloque 2 también
B08 = 102            el bloque 3 es el importe de las horas extra
```

y siguen ciertas `B01 = 101 + 103 + B02` —la del paso 4, ahora **sin** el `102`, que es el cambio—
y `990 = 970 − 980`.

## Lo que este ADR no decide

- **Qué ejercicio manda en `ss_cotizacion_topes`.** Traer el tope mínimo de profesionales con su
  cita dejó a la vista que las filas de la `V88` mezclan las bases mínimas de 2024 (Orden
  PJC/51/2024, en la redacción de la PJC/281/2024) con el tope máximo de 2025 (Orden PJC/178/2025).
  El mínimo de profesionales se siembra con la cifra del mismo ejercicio que los mínimos que ya
  están, para no inventar una discrepancia que no existe bajo ninguna orden. Poner la tabla entera
  al ejercicio corriente mueve todos los recibos de base baja: es otra decisión y otro issue.
- **Las horas extraordinarias de fuerza mayor**, que cotizan al 14,00 % (12,00 empresa + 2,00
  trabajador) en vez de al 28,30 %. El `H01` es una sola cantidad y el motor no distingue todavía
  unas de otras.
- **La cuota de accidentes de trabajo y enfermedad profesional**, que cotiza sobre la base de
  profesionales y necesita el CNAE de la empresa y la tarifa de primas: es el `backend#122`.

## Relación con ADR anteriores

| ADR | Relación |
|---|---|
| ADR-048 | Cierra su deuda 5 —«`B01` incompleta»— por el otro extremo del que parecía: no le faltaban devengos, le sobraba uno. Y sustituye su invariante «ningún concepto de cotización toma su base de `B01`, todos cuelgan de `B_CC`» por la de este ADR: cada cuota cuelga de la base de su contingencia |
| ADR-058 | Las quince bases nuevas son `PERIOD`, como las que ya había. Los topes siguen siendo lo único que se calcula por tramo y se acumula |
| ADR-070 | La prorrata entra en la base por una de dos puertas y la base no sabe cuál. El `B04` suma las dos, así que el recuadro tampoco lo sabe, que es la misma invariante vista en el papel |
