# Los ficheros de datos del maestro geo

Los carga la `V176__The_geo_master_is_loaded_from_its_data_files` (backend#154, ADR-078).
**Están congelados**: el checksum de esa migración es el de su contenido, así que tocarlos
después de aplicarla hace que el backend no arranque. Lo de cada año siguiente es una
migración nueva, nunca una edición de éstos.

Los dos se generan con `python tools/geo/datos_geo.py inicial`, desde la raíz de
`b4rrhh_backend`.

## `municipios-ine-2021-2025.tsv`

Los 8.132 municipios de España: código INE de cinco cifras (provincia + municipio), nombre,
provincia y desde cuándo.

- **Fuente**: INE, «Relación de municipios y códigos por comunidades autónomas y provincias»
  a 1 de enero de 2021, 2022, 2023, 2024 y 2025
  (`https://www.ine.es/daco/daco42/codmun/diccionarioAA.xlsx`). Son las que el INE sirve en
  fichero; las anteriores dan 404. Bajadas el 02/10/2026. Reutilización libre citando la
  fuente (aviso legal del INE).
- **Vigencia «desde al menos»**: lo que está en la relación de 2021 entra con `2021-01-01`,
  y esa fecha significa *vigente desde al menos*, no *creado en*. Lo que aparece después,
  con el 1 de enero de la primera relación que lo trae: Usansolo (48916), `2024-01-01`.
  Entre 2021 y 2025 no hay bajas.
- **Un renombre no es un municipio nuevo**: mismo código, nombre de la relación más
  reciente, sin historial de nombres. Entre 2021 y 2025 hay 49 (Candín → Valle de Ancares).
- Si algún día hace falta la historia de antes de 2021, la fuente es «Alteraciones de los
  municipios en los Censos de Población desde 1842» del INE, y la columna ya existe.

## `codigos-postales-geonames.tsv`

Los 11.150 códigos postales de GeoNames para España y, en 6.068, el municipio que sugieren.

- **Fuente**: GeoNames, `https://download.geonames.org/export/zip/ES.zip`, bajado el
  02/10/2026. **CC BY 4.0**: hay que atribuirlo a GeoNames (`https://www.geonames.org`).
- **Es una sugerencia**, y la columna se llama así. La provincia de un código postal no
  sale de aquí: son sus dos primeras cifras, siempre.
- **El municipio sale del nombre del lugar, no de la columna de municipio de GeoNames.**
  Esa columna se equivoca en 663 de los 6.068 códigos que se pueden comprobar, un 11 %: el
  46250 es L'Alcúdia y dice Sagunto, y el 04640 es Pulpí y dice Oria. Por eso se sugiere un
  municipio sólo cuando un lugar del código postal se llama como un municipio de su misma
  provincia. Si un código postal sugiere dos, no sugiere ninguno (915), y si ningún lugar
  se llama como un municipio, tampoco (4.167: pedanías, barrios, polígonos).

## El procedimiento anual

Cada enero el INE publica la relación a 1 de enero. Para cargarla:

1. `python tools/geo/datos_geo.py anual 2026` (el año de la relación nueva). Compara la
   relación nueva con la del año anterior y escribe en la salida el SQL:
   - **altas**: fila nueva con `start_date` el 1 de enero;
   - **bajas**: `end_date` el 31 de diciembre anterior. La fila no se borra: una dirección
     antigua sigue apuntando a ella;
   - **renombres**: el nombre nuevo, mismo código.
2. Ese SQL va en una migración nueva (`V<siguiente>__the_municipalities_of_2026.sql`), con la
   fuente y la fecha de la relación en la cabecera. Se revisa antes: una baja y un alta del
   mismo año suelen ser una fusión, y el ADR-078 quiere saberlo.
3. Este fichero y el de códigos postales no se tocan.

El guion compara un año con el anterior, así que los años van de uno en uno y en orden. Si
falta uno, se carga primero ése.
