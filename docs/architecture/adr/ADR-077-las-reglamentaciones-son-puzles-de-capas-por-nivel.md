# ADR-077 — Las reglamentaciones son puzles de capas por nivel

## Estado
Propuesto (30/09/2026), pendiente de aceptar al empujar el `backend#156`.

## Contexto

Una reglamentación (`rule_system`) es hoy un código plano del que cuelga todo: catálogos (`rule_entity`), tablas del motor, empleados, empresas, convenios. Consecuencias medidas:

- Un país es el mismo en ESP, FRA y PRT y está sembrado tres veces (V16, diez países en inglés por reglamentación). **Quince migraciones** siembran con `cross join` sobre `rule_system` lo que es igual en cualquier país (tipos de dirección, de contacto, de identificador, países…). Tres copias de una verdad divergen en cuanto alguien toca una.
- Una organización con dos esquemas de nómina (misma ley, distintos conceptos) no tiene forma de compartir la ley y separar lo suyo: hoy sería duplicar la reglamentación entera.
- Nada dice qué parte del grafo de cálculo es ley y qué parte es empresa.

Salió al diseñar el territorio (`workspace#19`): antes de sembrar 249 países había que decidir dónde viven.

## Decisión

1. **Cinco niveles fijos**: 1 Común · 2 Internacional · 3 Nacional · 4 Nómina nacional (ley) · 5 Nómina de empresa (esquema).
2. **Capa (`layer`)**: la unidad donde se definen cosas. Tiene código, nombre y **un nivel**. `COM`, `INT`, `ESP`, `NOM_ESP`, `NOM_ESP_EMP`.
3. **Una reglamentación es un puzle de exactamente una capa por nivel** (`rule_system_layer`). `ESP = (COM, INT, ESP, NOM_ESP, NOM_ESP_EMP)`; `ESP_2 = (COM, INT, ESP, NOM_ESP, NOM_EMP2)`.
4. **El tipo de entidad declara su nivel.** `COUNTRY` es de nivel 2 y sólo puede vivir en capas de nivel 2. Resolver `(reglamentación, tipo, código)` es: nivel del tipo → capa de esa reglamentación en ese nivel → entidad. **No se sube por ninguna cadena**; no hay sombras ni prioridades.
5. Dos códigos iguales en capas distintas **no chocan**: ninguna reglamentación monta dos capas del mismo nivel.
6. **El convenio no es capa.** Se elige por presencia; dos empleados de la misma empresa pueden tener convenios distintos.
7. **La capa 4 es cerrada**: un nodo de ley no nombra un nodo de empresa. Agrega por atributo (*cotiza, tributa, se prorratea*), que es lo que ya exige ADR-070. Los conceptos de la empresa (capa 5) declaran sus atributos.
8. Las tablas del motor que son ley (topes, tipos, tarifa AT, CNAE, IRPF) cuelgan de la capa 4, no de la reglamentación.
9. Todo lo demás (empleados, empresas, convenios, operaciones, marcas) **sigue colgando de la reglamentación**, que es el ensamblaje.

## Alternativas descartadas

- **Un `global` booleano en el tipo.** Resuelve los países y nada más; no da sitio a lo nacional compartido por dos esquemas ni a la ley separada de la empresa.
- **Un árbol de reglamentaciones con padre** (ESP → INT → COM) y resolución subiendo por la cadena. Funciona para catálogos, pero obliga a decidir si abajo se pisa lo de arriba, y `ESP_2` sería «hija de ESP» de casualidad. La composición lo hace por construcción.
- **Mantener el `cross join` con un test de que las copias son iguales.** Es sostener con un test lo que el modelo debería impedir.
- **El convenio como capa.** Rompe en cuanto dos empleados de la misma empresa tienen convenios distintos.
- **Partir el grafo ahora.** No hay un caso que lo pida; se deja la puerta y el inventario (`backend#160`), no el corte.

## Precedente

HR Access: la reglamentación no es plana; tiene niveles (COM, INT, nacional, nómina, y de usuario en adelante) y «modelos» que se montan por nivel. Copiamos la idea de niveles y modelos (aquí, capas). **No copiamos** que un nivel inferior pueda redefinir lo del superior: aquí un tipo vive en un solo nivel.

## Consecuencias

- Migración en pasos, cada uno con **md5 idéntico** de las once tablas de negocio tras recalcular la semilla de nueve meses cerrados (`backend#156`–`#160`). «Cero recibos se mueven» es el criterio de todos.
- Un solo puerto resuelve entidades, con candado (`backend#157`).
- Catálogos enseña de qué capa viene cada entidad y sólo edita en la suya (`frontend#127`); el Ámbito ofrece reglamentaciones, nunca capas.
- **Límite conocido**: `ESP_2` como puzle vale para catálogos y para las tablas de ley; la partición real del grafo (conceptos de empresa en la capa 5) es la puerta que no se cruza hasta que exista la segunda empresa con esquema propio o lo pida la retribución en especie (paso 6b del camino 2).
- Lo que sea mixto entre niveles (tipos de identificador: pasaporte internacional, DNI/NIE nacional) se queda en 3 con nota hasta que se parta.
