# ADR-064 — Un error de la API lleva código, mensaje y detalle, y los dos últimos son opcionales

## Estado
Aceptado

## Contexto

El `backend#78` encontró que `/payroll-engine` sirve **dos formas de error incompatibles** y que
ninguna está en el contrato: las tablas contestan `{"error": …}` y el resto `{"message": …}`. Un
cliente que reciba un error de ese contexto no puede saber qué campo leer sin haber probado antes
el endpoint concreto.

El barrido del `backend#100` puso números a toda la aplicación, sobre los 32 `*ErrorResponse` de
`src/main`:

| forma | cuántos |
|---|---|
| `{message}` | 18 |
| `{code, message, details}` | 11 (casi todo `employee`) |
| `{code, message}` | 2 |
| `{error, message}` | 1 |
| `{"error": …}` por `Map.of`, sin DTO | 2 manejadores de `payroll_engine` |
| sin cuerpo | Bean Validation, en todo `payroll_engine` |

El `backend#78` proponía unificar en `{message}` «porque es la que ya usa el resto de la
aplicación». **El resto usa las dos**, y 18 a 11 no es una mayoría que mande: es un empate incómodo
entre dos criterios distintos de cómo se cuenta un fallo.

Las formas están capturadas con una sonda contra respuestas reales, no leídas del código
(`PayrollEngineErrorShapeProbeTest`), porque los dos sitios donde la forma la pone otra cosa —un
`Map.of` sin DTO, y Spring cuando no hay manejador— no se ven leyendo `@ExceptionHandler`:

```
conceptos 404  -> {"message":"PayrollConcept not found: ruleSystemCode=ESP, conceptCode=NO_EXISTE_ZZ"}
tablas    404  -> {"error":"Table row not found: id=999999999"}
validación 400 -> cuerpo vacío, sin Content-Type
```

## Decisión

**La forma única de error de la API es `{code, message, details}`, con `code` y `details`
opcionales y omitidos cuando están vacíos.** Es la que ya existe como `PayrollErrorResponse` desde
el `backend#100`.

No se decide por recuento. Se decide por lo que cada forma **puede contar**, y son cosas distintas:

### 1. La rica no es una segunda forma: contiene a la pobre

Con `@JsonInclude(NON_NULL)`, un error sin código sale byte a byte como salía antes. Está
comprobado contra una respuesta real, no razonado:

```
payrolls 404 -> {"message":"Payroll not found with business key: ESP/INTERNAL/NO_EXISTE/202501/NORMAL/1"}
```

Eso cambia la naturaleza de la elección. No es «`{message}` contra `{code, message, details}`» con
migración en un lado y no en el otro: es **el mismo contrato con sitio para más**. Elegir la rica no
obliga a tocar ninguno de los 18 sitios que hoy sólo llevan `message`, y elegir la pobre sí obligaría
a vaciar los 11 que llevan código —o sea, a tirar información que alguien puso a propósito—.

### 2. Un mensaje no es un código, y quien lee al otro lado necesita el código

`message` es una frase en inglés escrita para una persona. Con sólo eso, un cliente que quiera
**hacer algo distinto** según el fallo —reintentar, ofrecer dar de alta el código que falta, llevar
al usuario a otra pantalla— no tiene más remedio que comparar cadenas. Y el designer, que es quien
consume `/payroll-engine`, es exactamente ese cliente.

Hay además un precedente que ya lo resolvió así y que conviene no contradecir: los códigos de
mensaje de ejecución (`UNIT_CALCULATION_ERROR`, `UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT`) viven en
`rulesystem.rule_entity` como `PAYROLL_RUN_MESSAGE`, sembrados por la `V125`, **traducidos**, y con
una guardia que cruza fuente y catálogo (`EveryRunMessageCodeIsInTheCatalogTest`). La aplicación ya
tiene un vocabulario de fallos; `{message}` no tiene dónde ponerlo.

### 3. `details` es donde cabe lo que hoy se tira

El caso que lo demuestra es el `400` sin cuerpo de Bean Validation. La información existe —Spring la
tiene entera y la registra en el log— y **no llega al cliente**:

```
Validation failed ... with 6 errors: [Field error on field 'dailyValue': rejected value [null];
 default message [must not be null]] [Field error on field 'searchCode': rejected value [null] ...
```

Seis campos, con su valor rechazado y su motivo. En `{message}` eso sólo cabe concatenado en una
frase, que es un formato que ningún cliente puede recorrer. En `details` cabe tal cual. El
`backend#78` dice, y con razón, que ese `400` es un defecto y no un hueco de documentación; la forma
elegida es la que permite arreglarlo sin inventarse nada.

### 4. El `#100` sirve de precedente, y de aviso

El `#100` eligió `422` con `UNIT_CALCULATION_ERROR` reutilizando el vocabulario que ya existía, y
añadió `code` y `details` a `PayrollErrorResponse` **sin cambiar ni un byte de los errores que ya
había**. Es el mismo movimiento que este ADR generaliza, y la prueba de que se puede hacer sin
romper clientes.

Donde **no** sirve de precedente es en el alcance: aquel era un endpoint y éste son diecisiete más
dos contextos. Que la forma sea compatible hacia atrás no hace compatible lo demás, y hay un sitio
donde sí se rompe: las tablas y los binding roles pasan de `error` a `message`, que es un cambio de
campo y lo ve el designer.

## Consecuencias

**Lo que no cambia**: los 18 sitios con `{message}` y los 11 con `{code, message, details}` siguen
sirviendo exactamente lo mismo. Ésa es la mitad del valor de esta decisión.

**Lo que cambia**, y es el trabajo que el `backend#78` describe y este ADR no ejecuta:

1. Los dos manejadores de `payroll_engine` que devuelven `Map.of("error", …)` —tablas y binding
   roles— pasan a la forma común. **Esto rompe al designer**, que lee `error`, y va en su commit del
   repo `designer`.
2. `AbsenceErrorResponse` (`{error, message}`) y los dos `{code, message}` se alinean.
3. Bean Validation produce cuerpo en `payroll_engine`, con `details` llevando los campos.
4. Los 17 endpoints declaran sus errores con `$ref` al esquema común en
   `personnel-administration-api.yaml`.
5. La guarda de `OpenApiContractsAreValidTest`: ninguna respuesta de error declarada sin esquema de
   cuerpo.

**El orden importa y es el del issue**: primero unificar, después documentar. Un contrato que
declarase hoy la forma común estaría mintiendo en los dos sitios que sirven `error` y en todos los
`400` sin cuerpo, y esta vez con la firma de un contrato detrás.

**No se tocan los códigos de estado.** Esto va de la forma del cuerpo. Si alguno está mal, se dice y
se decide aparte.

**`code` no se inventa endpoint a endpoint.** Cuando se añada, sale del catálogo de
`rulesystem.rule_entity` como los de ejecución, o el vocabulario se fragmenta y habremos cambiado
dos formas incompatibles por dos diccionarios incompatibles.

## Fuera de alcance

El `/error` de Spring (`{timestamp, status, error, path}`), que sale por cualquier excepción sin
manejador y no es una forma que nadie haya elegido: en `com.b4rrhh.payroll` no hay advice de
paquete, así que lo que no recoge `PayrollExceptionHandler` sale por ahí. Es un agujero distinto y
más grande, y se mira aparte.

Y el `404` sin cuerpo de `GET /payrolls/…`, que no pasa por el manejador porque el controlador
construye `ResponseEntity.notFound().build()`. Misma familia que el `400` sin cuerpo, otro contexto.
