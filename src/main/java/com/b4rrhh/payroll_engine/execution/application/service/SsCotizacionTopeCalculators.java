package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTopesRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Que nodo de tope del motor recorta que base ({@code backend#121}).
 *
 * <p>El modelo oficial tiene dos bases con topes y cada una tiene los suyos:
 *
 * <ul>
 *   <li>{@code P_TOPE_MAX} / {@code P_TOPE_MIN} recortan la base de contingencias comunes.</li>
 *   <li>{@code P_TOPE_MAX_CP} / {@code P_TOPE_MIN_CP} recortan la de contingencias
 *       profesionales y recaudacion conjunta.</li>
 * </ul>
 *
 * <p>El maximo es el mismo numero en las dos; el <b>minimo no</b>, y ahi esta la razon de que
 * esto se declare en vez de deducirse. El de profesionales es el tope minimo de cotizacion que
 * fija la Orden de cotizacion, igual para los once grupos; el de comunes es la base minima del
 * grupo del empleado. Escribir «el minimo es el minimo» funcionaria para los grupos bajos y
 * fallaria en silencio en los altos.
 *
 * <p>Hermano de {@link SsCotizacionRateCalculators}, y por la misma razon: la correspondencia
 * entre el nodo del grafo y la fila del catalogo se declara en un sitio.
 */
@Configuration
public class SsCotizacionTopeCalculators {

    private static final String COMUNES = "COMUNES";
    private static final String PROFESIONALES = "PROFESIONALES";

    @Bean
    TopeMaxCotizacionCalculator topeMaxComunes(SsCotizacionTopesRepository topes) {
        return new TopeMaxCotizacionCalculator("P_TOPE_MAX", COMUNES, topes);
    }

    @Bean
    TopeMinCotizacionCalculator topeMinComunes(SsCotizacionTopesRepository topes) {
        return new TopeMinCotizacionCalculator("P_TOPE_MIN", COMUNES, topes);
    }

    @Bean
    TopeMaxCotizacionCalculator topeMaxProfesionales(SsCotizacionTopesRepository topes) {
        return new TopeMaxCotizacionCalculator("P_TOPE_MAX_CP", PROFESIONALES, topes);
    }

    @Bean
    TopeMinCotizacionCalculator topeMinProfesionales(SsCotizacionTopesRepository topes) {
        return new TopeMinCotizacionCalculator("P_TOPE_MIN_CP", PROFESIONALES, topes);
    }
}
