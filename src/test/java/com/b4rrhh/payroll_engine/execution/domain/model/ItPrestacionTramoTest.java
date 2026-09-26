package com.b4rrhh.payroll_engine.execution.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cuantos dias de un intervalo caen en un tramo de la prestacion ({@code backend#129}).
 *
 * <p>Es aritmetica de dias y por eso tiene un test propio, rapido y sin base de datos: los escenarios
 * la prueban de extremo a extremo, pero un fallo aqui sale alli como un importe raro y no como lo que
 * es. Los tramos reales son los de la enfermedad comun -4 a 15, 16 a 20, y 21 en adelante-.
 */
class ItPrestacionTramoTest {

    private static final ItPrestacionTramo EMPRESA_60 =
            new ItPrestacionTramo(4, 15, new BigDecimal("60.00"));
    private static final ItPrestacionTramo DELEGADO_60 =
            new ItPrestacionTramo(16, 20, new BigDecimal("60.00"));
    private static final ItPrestacionTramo DELEGADO_75 =
            new ItPrestacionTramo(21, null, new BigDecimal("75.00"));

    /** Una baja de diez dias que empieza en el mes: siete dias pagados y tres que no. */
    @Test
    void unaBajaDeDiezDiasQueEmpiezaEnElMesDejaSieteDiasEnElTramoDeLaEmpresa() {
        assertEquals(7, EMPRESA_60.daysWithin(1, 10));
        assertEquals(0, DELEGADO_60.daysWithin(1, 10));
        assertEquals(0, DELEGADO_75.daysWithin(1, 10));
    }

    /**
     * Una baja que viene de otro mes: los dias del 8 al 37, y los tres tramos a la vez.
     *
     * <p>Este es el caso que el {@code backend#127} dejo preparado llevando en el tramo los dias ya
     * transcurridos: aqui no hay ninguna fecha, solo el numero de dia de la baja.
     */
    @Test
    void unaBajaQueVieneDeOtroMesReparteElMesEntreLosTresTramos() {
        assertEquals(8, EMPRESA_60.daysWithin(8, 37));
        assertEquals(5, DELEGADO_60.daysWithin(8, 37));
        assertEquals(17, DELEGADO_75.daysWithin(8, 37));
        assertEquals(30, 8 + 5 + 17, "y los tres suman el mes entero, sin dias de mas ni de menos");
    }

    /**
     * Un tramo sin fin cuenta hasta donde llegue el intervalo.
     *
     * <p>Y esto no es teoria: el tramo del 75 % lleva {@code day_to} nulo, y una lectura que lo trajera
     * como cero dejaria de pagar el tramo mas largo de todos <b>sin dar ningun error</b>. Paso, y este
     * es el caso que lo habria visto antes.
     */
    @Test
    void unTramoSinFinLlegaHastaDondeLleguelaBaja() {
        assertEquals(1, DELEGADO_75.daysWithin(21, 21));
        assertEquals(525, DELEGADO_75.daysWithin(21, 545), "una baja agotando los 545 dias");
    }

    /** Un tramo que no toca el intervalo aporta cero, y no un numero al reves. */
    @Test
    void unTramoQueNoTocaElIntervaloAportaCero() {
        assertEquals(0, EMPRESA_60.daysWithin(1, 3), "los tres primeros dias no son de ningun tramo");
        assertEquals(0, EMPRESA_60.daysWithin(16, 30), "ni los de despues del dia 15");
        assertEquals(0, DELEGADO_75.daysWithin(1, 3));
    }

    /** Un dia de baja: ningun tramo lo paga, porque los tres primeros no se pagan. */
    @Test
    void unDiaDeBajaNoLoPagaNingunTramo() {
        assertEquals(0, EMPRESA_60.daysWithin(1, 1));
        assertEquals(0, DELEGADO_60.daysWithin(1, 1));
        assertEquals(0, DELEGADO_75.daysWithin(1, 1));
    }
}
