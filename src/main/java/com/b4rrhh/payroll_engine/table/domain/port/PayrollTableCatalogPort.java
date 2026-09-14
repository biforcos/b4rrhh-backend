package com.b4rrhh.payroll_engine.table.domain.port;

import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRowCount;

import java.util.List;
import java.util.Map;

/**
 * Las dos mitades de "que tablas hay", cada una tal cual la tiene la base.
 *
 * No hay un metodo que devuelva la lista ya hecha a proposito: juntarlas es
 * una decision -que una tabla existe si tiene filas O si algo la ata- y una
 * decision no vive en el adaptador (backend#95).
 */
public interface PayrollTableCatalogPort {

    /** Un recuento por cada codigo de tabla que tenga al menos una fila. */
    List<PayrollTableRowCount> countRowsByTableCode(String ruleSystemCode);

    /** Las vinculaciones que apuntan a una tabla, agrupadas por el codigo al que apuntan. */
    Map<String, List<PayrollTableBinding>> findTableBindingsByTableCode(String ruleSystemCode);
}
