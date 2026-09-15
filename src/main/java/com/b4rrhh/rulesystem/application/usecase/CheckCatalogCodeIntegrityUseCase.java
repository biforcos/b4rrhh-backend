package com.b4rrhh.rulesystem.application.usecase;

/**
 * Comprueba que ninguna fila del esquema {@code employee} apunte a un codigo de catalogo que no
 * exista en {@code rulesystem.rule_entity} (backend#43, backend#44).
 *
 * <p>Una sola definicion, dos sitios donde se usa: la guardia de la suite y la comprobacion del
 * despliegue contra una base poblada. El issue pedia justo eso —que quien comprueba la demo sea
 * <b>la aplicacion</b> y no un {@code .sql} con la lista de columnas copiada— porque una segunda
 * lista se queda atras el dia que entra un vertical nuevo y nadie se entera.
 */
public interface CheckCatalogCodeIntegrityUseCase {

    CatalogCodeIntegrityReport check();
}
