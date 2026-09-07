# Consultas de invariantes sobre datos vivos

Seis consultas de diagnóstico, una por vertical temporal del contexto `employee`.
Cada una comprueba sobre datos reales los invariantes que el ADR-057 le atribuye a
su serie: **que no se solape consigo misma** y, cuando su cobertura es obligatoria,
**que no deje huecos dentro de la presencia del empleado**.

| Fichero | Vertical | Serie de | Cobertura |
|---|---|---|---|
| `contrato-cobertura-y-solape.sql` | `contract` | empleado | obligatoria |
| `jornada-cobertura-y-solape.sql` | `working_time` | empleado | obligatoria |
| `clasificacion-laboral-cobertura-y-solape.sql` | `labor_classification` | empleado | obligatoria |
| `direccion-cobertura-y-solape-por-tipo.sql` | `address` | empleado **y tipo** | la declara el catálogo |
| `centro-de-trabajo-cobertura-y-solape.sql` | `work_center` | empleado | obligatoria |
| `centro-de-coste-solape-y-reparto.sql` | `cost_center` | empleado, ocurrencia compuesta | opcional |

**El resultado correcto de todas es cero filas.** Cada fila que sale es un defecto en
los datos, y la columna `detalle` dice qué fechas lo provocan. Ninguna avisa de nada
que sea legal: los huecos que el ADR-057 declara legales —una dirección opcional, un
reparto de centros de coste que no existe— no se cuentan, a propósito.

## Cómo se lanzan

Contra el Postgres local, por stdin. El fichero está en tu máquina y no dentro del
contenedor, así que `-f -` y la redirección son lo que hace falta; `docker cp` no:

```bash
docker exec -i b4rrhh-postgres psql -U b4rrhh -d b4rrhh -v ON_ERROR_STOP=1 -f - \
  < b4rrhh_backend/docs/consultas/contrato-cobertura-y-solape.sql
```

Las seis de una vez, desde la raíz del workspace:

```bash
for f in b4rrhh_backend/docs/consultas/*.sql; do
  echo "== $f"
  docker exec -i b4rrhh-postgres psql -U b4rrhh -d b4rrhh -v ON_ERROR_STOP=1 -f - < "$f"
done
```

Necesitan PostgreSQL 14 o superior: usan `range_agg` y la resta de `datemultirange`.
El contenedor de desarrollo es `postgres:16`.

## Para qué sirven

Son el **chequeo previo** de un issue que va a tocar una de estas verticales: la
comprobación sobre datos vivos de que la premisa del issue es cierta antes de escribir
código. Son también la pareja natural de los recuentos de la resiembra completa
(`deploy#3`): los recuentos dicen que **hay** datos, éstas dicen que los datos son
**válidos**.

## Por qué están en el repositorio

Estuvieron un tiempo en un directorio suelto de una máquina, y se perdieron. La pérdida
no importaba —se reescriben— pero una de ellas estaba mal, y eso sí: la de cobertura de
dirección agregaba todos los tipos con `range_agg` y sólo contaba huecos, así que era
ciega a los solapes y la agregación tapaba que la serie no es por empleado sino por
tipo. Devolvió cero, y ese cero entró en el ADR-057 como hecho decidido.

Una consulta que puede equivocarse así y cuyo resultado acaba en un ADR es código. Vive
aquí, se lee y se corrige (`backend#68`).

## Lo que no son

**No son tests.** El invariante ya está en el código de cada vertical y en sus tests;
esto es diagnóstico sobre datos reales o sembrados. Si una de estas consultas devuelve
filas, el sitio donde se arregla es la vertical o los datos, no esta carpeta.

Tampoco comprueban el tercer eje del ADR-057 —si una ocurrencia puede sobrevivir a la
presencia—, que es un invariante distinto del de cobertura y no entra aquí.
