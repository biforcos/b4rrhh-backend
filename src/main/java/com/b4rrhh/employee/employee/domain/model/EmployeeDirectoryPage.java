package com.b4rrhh.employee.employee.domain.model;

import java.util.List;

/**
 * Una página del directorio con el total de los que cumplen el filtro (backend#18).
 *
 * {@code total} cuenta con los mismos filtros que la página, no las filas de la página: es lo
 * que permite distinguir «no hay nadie que se llame así» de «no hay nadie más en esta página».
 */
public record EmployeeDirectoryPage(
        List<EmployeeDirectoryItem> items,
        int page,
        int size,
        long total
) {

    public EmployeeDirectoryPage {
        items = List.copyOf(items);
    }
}
