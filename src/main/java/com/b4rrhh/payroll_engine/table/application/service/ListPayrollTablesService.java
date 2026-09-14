package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.table.application.usecase.ListPayrollTablesUseCase;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRowCount;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableSummary;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableCatalogPort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Las tablas de verdad de un sistema de reglas (backend#95).
 *
 * La union de las dos fuentes es todo lo que hace este servicio, y es lo unico
 * que no puede hacer ninguna de las dos por su cuenta:
 *
 *   - Preguntandole solo a las filas, una tabla vinculada y vacia no existe.
 *   - Preguntandole solo a las vinculaciones, una tabla que ya no lee nadie
 *     desaparece, que es justo el caso que hay que poder ver.
 *
 * Y no se pregunta a payroll_object, que es donde estaba mirando el designer:
 * alli no hay tablas, hay ranuras, y ni siquiera todas -de los tres roles
 * atados en ESP hoy solo P02_DAILY_AMOUNT_TABLE es un objeto-. Un rol sin
 * tabla atada no sale en esta lista porque no hay tabla que listar; sigue
 * siendo un nodo del grafo y su sitio es el lienzo.
 */
@Service
public class ListPayrollTablesService implements ListPayrollTablesUseCase {

    private final PayrollTableCatalogPort catalog;

    public ListPayrollTablesService(PayrollTableCatalogPort catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<PayrollTableSummary> list(String ruleSystemCode) {
        List<PayrollTableRowCount> recuentos = catalog.countRowsByTableCode(ruleSystemCode);
        Map<String, List<PayrollTableBinding>> vinculaciones =
                catalog.findTableBindingsByTableCode(ruleSystemCode);

        Map<String, PayrollTableRowCount> porCodigo = new LinkedHashMap<>();
        for (PayrollTableRowCount recuento : recuentos) {
            porCodigo.put(recuento.tableCode(), recuento);
        }

        // Ordenado por codigo, y ordenado aqui y no en las consultas: la lista
        // sale de dos sitios, asi que el orden de cualquiera de los dos seria
        // el orden de media lista.
        Set<String> codigos = new TreeSet<>(porCodigo.keySet());
        codigos.addAll(vinculaciones.keySet());

        List<PayrollTableSummary> tablas = new ArrayList<>();
        for (String tableCode : codigos) {
            PayrollTableRowCount recuento = porCodigo.get(tableCode);
            tablas.add(new PayrollTableSummary(
                    ruleSystemCode,
                    tableCode,
                    recuento == null ? 0L : recuento.rowCount(),
                    recuento == null ? 0L : recuento.activeRowCount(),
                    ordenadas(vinculaciones.getOrDefault(tableCode, List.of()))
            ));
        }
        return tablas;
    }

    private static List<PayrollTableBinding> ordenadas(List<PayrollTableBinding> vinculaciones) {
        return vinculaciones.stream()
                .sorted(Comparator.comparing(PayrollTableBinding::bindingRoleCode)
                        .thenComparing(PayrollTableBinding::ownerTypeCode)
                        .thenComparing(PayrollTableBinding::ownerCode))
                .toList();
    }
}
