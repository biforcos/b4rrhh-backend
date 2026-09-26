package com.b4rrhh.payroll_engine.segment.domain.model;

import java.math.BigDecimal;

/**
 * Los tres totales de los atrasos que este recibo paga ({@code backend#133}).
 *
 * <p>Vienen <b>hechos</b>: los resuelve la unidad antes de ejecutar el grafo, por el mismo camino que la
 * base reguladora del {@code backend#128}. Lo que los lee son tres conceptos tecnicos {@code A_DEV},
 * {@code A_DED} y {@code A_EMP} que alimentan al {@code 970}, al {@code 980} y al {@code 725} (V162), y
 * asi las lineas de atraso son dinero de este mes <b>dentro del grafo</b> y no una suma que alguien hace
 * despues.
 *
 * <p>Tres y no uno porque van a tres sitios distintos: un solo concepto con el neto dentro daria el mismo
 * liquido y un recibo que no distingue lo que se cobra de lo que se retiene, que es justo lo que el modelo
 * oficial separa.
 *
 * <p>Del periodo y no del tramo, aunque viaje en el contexto del tramo: un atraso no pertenece a ningun
 * dia del mes abierto. Los tres conceptos son de ambito {@code PERIOD} y se evaluan una vez.
 */
public record SegmentRetroArrears(
        BigDecimal earnings,
        BigDecimal employeeDeductions,
        BigDecimal employerContributions
) {

    private static final SegmentRetroArrears NINGUNO =
            new SegmentRetroArrears(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public SegmentRetroArrears {
        earnings = earnings == null ? BigDecimal.ZERO : earnings;
        employeeDeductions = employeeDeductions == null ? BigDecimal.ZERO : employeeDeductions;
        employerContributions = employerContributions == null ? BigDecimal.ZERO : employerContributions;
    }

    /**
     * Sin atrasos, que es el caso normal.
     *
     * <p>Ceros y no nulos a proposito: los tres conceptos tecnicos valen cero donde no hay atraso, y un
     * cero no se imprime (backend#104). Lo que se gana es que «quien lleva atrasos» se contesta mirando
     * el numero.
     */
    public static SegmentRetroArrears none() {
        return NINGUNO;
    }
}
