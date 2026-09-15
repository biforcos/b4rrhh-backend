package com.b4rrhh.rulesystem.application.port;

import java.util.List;

/**
 * Lee el estado de cada columna de catálogo declarada por los participantes (backend#44).
 *
 * <p>La lista de columnas <b>no se escribe en ninguna parte</b>: sale de {@code declaredUsages()}
 * de cada participante, el mismo {@code Map} que usa {@code countReferences}. Un vertical nuevo
 * entra en la comprobación con declararse, y una segunda lista que alguien tuviera que mantener
 * sería justo el registro central que el patrón del ADR-047 existe para evitar. Ése es el motivo
 * de que esto sea un puerto de la aplicación y no un {@code .sql} en el repo de despliegue.
 */
public interface CatalogCodeIntegrityReadPort {

    /** Una entrada por columna declarada, tenga filas o no. */
    List<CatalogColumnIntegrity> readAll();
}
