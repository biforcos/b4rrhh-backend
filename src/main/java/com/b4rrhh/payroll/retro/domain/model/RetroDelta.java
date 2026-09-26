package com.b4rrhh.payroll.retro.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Los atrasos que bajan al recibo de este mes, y sus tres totales ({@code backend#133}).
 *
 * <p>Los tres totales van aqui y no se calculan al pintar porque <b>los necesita el grafo</b>: los tres
 * conceptos tecnicos {@code A_DEV}, {@code A_DED} y {@code A_EMP} los reciben hechos y alimentan al
 * {@code 970}, al {@code 980} y al {@code 725} (V162). Asi las lineas de atraso son dinero de este mes
 * <b>dentro del grafo</b>, y no una suma que alguien hace despues y que el recibo no puede explicar.
 */
public record RetroDelta(
        List<RetroDeltaLine> lines,
        BigDecimal totalEarnings,
        BigDecimal totalEmployeeDeductions,
        BigDecimal totalEmployerContributions
) {

    public RetroDelta {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public static RetroDelta none() {
        return new RetroDelta(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Suma cada linea a su total, preguntandole al grafo a cual pertenece. */
    public static RetroDelta of(List<RetroDeltaLine> lines, RetroBucketing bucketing) {
        BigDecimal devengos = BigDecimal.ZERO;
        BigDecimal deducciones = BigDecimal.ZERO;
        BigDecimal empresa = BigDecimal.ZERO;
        for (RetroDeltaLine linea : lines) {
            switch (bucketing.bucketOf(linea.conceptCode())) {
                case EARNING -> devengos = devengos.add(linea.amount());
                case EMPLOYEE_DEDUCTION -> deducciones = deducciones.add(linea.amount());
                case EMPLOYER_CONTRIBUTION -> empresa = empresa.add(linea.amount());
                case NEITHER -> { }
            }
        }
        return new RetroDelta(lines, devengos, deducciones, empresa);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
