# OpenAPI Source

Place OpenAPI source files in this folder.
The backend uses contract-first development and OpenAPI is the source of truth.

**Un solo contrato: `personnel-administration-api.yaml`.**

Hubo dos. `payroll-api.yaml` nacio en abril de 2026 como banco de diseno del ADR-029,
para probar el flujo launch -> calculate antes de que existiera el motor, y acabo
describiendo endpoints servidos sin que nadie lo decidiera. Los dos ficheros derivaron en
silencio durante cinco meses: un endpoint servido y documentado solo en el pequeno era
invisible para el cliente que el frontend genera del grande (`frontend#61`), y ninguno de
los dos llego a describir las once rutas que el backend sirve de verdad.

Se fusiono en `backend#80`. La regla que queda es la que evita repetirlo:

> **Lo que se sirve se declara, y se declara en un solo sitio.**

`TheTwoContractsNeverDivergeInSilenceTest` la sostiene: si algun dia vuelve a haber dos
contratos con la misma operacion, la suite se cae.
