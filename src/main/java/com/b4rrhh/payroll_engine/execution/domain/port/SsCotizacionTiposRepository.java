package com.b4rrhh.payroll_engine.execution.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Los tipos de cotizacion vigentes, leidos del catalogo ({@code backend#105}).
 *
 * <p>Hermano de {@link SsCotizacionTopesRepository} y con la misma forma, porque resuelven el
 * mismo tipo de pregunta: <b>que numero manda en esta fecha</b>. Los topes se leian desde el
 * primer dia; los tipos estaban sembrados con sus vigencias y no los leia nadie, porque los
 * mismos nueve numeros vivian duplicados como constantes de Java.
 */
public interface SsCotizacionTiposRepository {

    /**
     * El tipo vigente de una contingencia en una fecha, en tanto por ciento ({@code 4.70} es
     * el 4,70 %).
     */
    Optional<BigDecimal> findRate(String ruleSystemCode, String contingencyCode, LocalDate referenceDate);
}
