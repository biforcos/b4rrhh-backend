package com.b4rrhh.payroll_engine.table.application.service;

import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRowCount;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableSummary;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que decide este servicio es de donde sale la lista, y por eso lo que se
 * afirma aqui son los dos casos que ninguna de las dos fuentes contesta sola
 * (backend#95).
 */
class ListPayrollTablesServiceTest {

    private static final String ESP = "ESP";

    private ListPayrollTablesService servicioCon(
            List<PayrollTableRowCount> recuentos,
            Map<String, List<PayrollTableBinding>> vinculaciones
    ) {
        return new ListPayrollTablesService(new PayrollTableCatalogPort() {
            @Override
            public List<PayrollTableRowCount> countRowsByTableCode(String ruleSystemCode) {
                return recuentos;
            }

            @Override
            public Map<String, List<PayrollTableBinding>> findTableBindingsByTableCode(String ruleSystemCode) {
                return vinculaciones;
            }
        });
    }

    @Test
    void unaTablaConFilasYSinVinculacionSeLista_conVinculacionesVacias() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(new PayrollTableRowCount("SB_HUERFANA", 3, 3)),
                Map.of()
        ).list(ESP);

        assertEquals(1, tablas.size());
        assertEquals("SB_HUERFANA", tablas.get(0).tableCode());
        assertEquals(3, tablas.get(0).rowCount());
        assertTrue(tablas.get(0).bindings().isEmpty(),
                "existe y no la lee nadie: eso es justo lo que hay que poder ver");
    }

    @Test
    void unaTablaVinculadaYSinFilasSeLista_conRecuentoCero() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(),
                Map.of("SB_VACIA", List.of(
                        new PayrollTableBinding("AGREEMENT", "99002405011982", "BASE_SALARY_TABLE", true)))
        ).list(ESP);

        assertEquals(1, tablas.size());
        assertEquals("SB_VACIA", tablas.get(0).tableCode());
        assertEquals(0, tablas.get(0).rowCount());
        assertEquals(0, tablas.get(0).activeRowCount());
        assertEquals(1, tablas.get(0).bindings().size(),
                "se lee y esta vacia, que no es lo mismo que no existir");
    }

    @Test
    void lasFilasDesactivadasNoSonUnaTablaVacia() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(new PayrollTableRowCount("SB_APAGADA", 3, 0)),
                Map.of()
        ).list(ESP);

        assertEquals(3, tablas.get(0).rowCount());
        assertEquals(0, tablas.get(0).activeRowCount());
    }

    @Test
    void unaTablaQueEstaEnLasDosFuentesSaleUnaVez() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(new PayrollTableRowCount("SB_99002405011982", 3, 3)),
                Map.of("SB_99002405011982", List.of(
                        new PayrollTableBinding("AGREEMENT", "99002405011982", "BASE_SALARY_TABLE", true)))
        ).list(ESP);

        assertEquals(1, tablas.size());
        assertEquals(3, tablas.get(0).rowCount());
        assertEquals(1, tablas.get(0).bindings().size());
    }

    @Test
    void unaVinculacionInactivaSeConserva_porqueEsLaExplicacion() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(new PayrollTableRowCount("PC_APAGADA", 3, 3)),
                Map.of("PC_APAGADA", List.of(
                        new PayrollTableBinding("AGREEMENT", "99002405011982", "AGREEMENT_PLUS_TABLE", false)))
        ).list(ESP);

        assertEquals(1, tablas.get(0).bindings().size());
        assertEquals(false, tablas.get(0).bindings().get(0).active());
    }

    @Test
    void laListaVaOrdenadaPorCodigo_aunqueCadaMitadLlegueEnOtroOrden() {
        List<PayrollTableSummary> tablas = servicioCon(
                List.of(new PayrollTableRowCount("SB_2", 1, 1),
                        new PayrollTableRowCount("PC_1", 1, 1)),
                Map.of("AA_0", List.of(
                        new PayrollTableBinding("AGREEMENT", "X", "OTRO_ROL", true)))
        ).list(ESP);

        assertEquals(List.of("AA_0", "PC_1", "SB_2"), tablas.stream().map(PayrollTableSummary::tableCode).toList());
    }
}
