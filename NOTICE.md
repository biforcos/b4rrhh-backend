# NOTICE

## B4RRHH

B4RRHH is a personal project created and maintained by Juan Antonio Biforcos Amor.

This project explores advanced domain modeling and system design in the HR and payroll space, including:

- Employee lifecycle modeling
- Rule-based catalog systems
- Concept-driven payroll calculation engines
- Graph-based dependency modeling

---

## License Summary

This project is distributed under a Business Source License (BSL).

- Source code is visible and available for learning and evaluation
- Commercial use is NOT permitted without explicit authorization

See LICENSE.md for full terms.

---

## Datos de terceros

El maestro del territorio (backend#154, ADR-078) reproduce datos públicos de cuatro fuentes.
La licencia del código no es la suya: cada uno conserva la de su fuente, y es ésta.

| Fuente | Qué | Licencia | Versión o fecha | Dónde está |
|--------|-----|----------|-----------------|------------|
| **GeoNames** (`https://www.geonames.org`) | Códigos postales de España y el lugar que nombran | **CC BY 4.0**: se atribuye a GeoNames | `https://download.geonames.org/export/zip/ES.zip`, bajado el 02/10/2026 | `src/main/resources/db/geo/codigos-postales-geonames.tsv`, cargado por la `V176` |
| **INE** (Instituto Nacional de Estadística) | Municipios con su código; comunidades autónomas y provincias | Reutilización libre citando la fuente (aviso legal del INE) | Relación de municipios a 1 de enero de 2021, 2022, 2023, 2024 y 2025 (`diccionarioAA.xlsx`), bajadas el 02/10/2026; relación de comunidades y provincias consultada el 02/10/2026 | `src/main/resources/db/geo/municipios-ine-2021-2025.tsv`, cargado por la `V176`; comunidades y provincias en la `V174__the_territory_regions_provinces_and_street_types.sql` |
| **Dirección General del Catastro** | Las 93 siglas de tipo de vía | Licencia de uso de la información catastral: se cita a la Dirección General del Catastro como fuente y la fecha de acceso | Anexo II «Tipos de vía» de «Servicios web libres de la Sede Electrónica del Catastro», versión 2.6, consultado el 02/10/2026 | `V174__the_territory_regions_provinces_and_street_types.sql` |
| **iso-codes** (Debian, `https://salsa.debian.org/iso-codes-team/iso-codes`) | Los 249 países de ISO 3166-1 con su nombre en castellano; el código ISO 3166-2 de comunidades y provincias | LGPL-2.1-or-later | 4.18.0 | `V168__a_country_lives_in_int_and_they_are_the_249_of_iso_3166.sql` y `V174` |

Las migraciones están en `src/main/resources/db/migration/` y la `V176` en
`src/main/java/db/migration/`. Cómo se generan y se actualizan los ficheros de datos, en
`src/main/resources/db/geo/LEEME.md`. Un fichero de datos nuevo en `db/geo` necesita su fila
aquí: `EveryThirdPartyDataFileIsAttributedInNoticeTest` falla si no la tiene.

---

## Author

Bifor

This repository represents a personal approach to:

- Software architecture
- Domain-driven design
- Payroll engine and Human Resources application

---

## Disclaimer

This project is experimental and provided without any guarantees.

It is not intended for production use without proper validation and licensing.