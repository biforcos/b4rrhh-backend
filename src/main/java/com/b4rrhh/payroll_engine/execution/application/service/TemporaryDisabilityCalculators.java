package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.port.ItPrestacionTramoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Que concepto del motor lee que tramo de la prestacion por incapacidad temporal
 * ({@code backend#129}).
 *
 * <p>La correspondencia esta aqui y en un solo sitio, como la de las cuotas de cotizacion
 * ({@code SsCotizacionRateCalculators}). Y hace falta declararla porque los nombres no coinciden: el
 * concepto del motor se llama {@code D_IT_E60} y el tramo del catalogo {@code EMPRESA_60}. Adivinarlo
 * con una regla de transformacion de cadenas funcionaria y fallaria en silencio el dia que aparezca
 * un tramo que no encaje.
 *
 * <p>Seis conceptos y tres tramos: por cada tramo, <b>cuantos dias</b> caen en el —que depende del
 * tramo del periodo— y <b>a que porcentaje</b> se paga —que no depende de nada del empleado—.
 */
@Configuration
public class TemporaryDisabilityCalculators {

    /** El tipo de ausencia de la v1: enfermedad comun (ADR-073, ADR-075). */
    static final String IT_COMMON = "IT_COMMON";

    /** Dias 4 a 15 de la baja, al 60 % y a cargo de la empresa (art. 173.1 LGSS). */
    static final String EMPRESA_60 = "EMPRESA_60";

    /** Dias 16 a 20, al 60 % y en pago delegado (art. unico RD 53/1980). */
    static final String DELEGADO_60 = "DELEGADO_60";

    /** Dia 21 y siguientes, al 75 % y en pago delegado (art. 2.1 Decreto 3158/1966). */
    static final String DELEGADO_75 = "DELEGADO_75";

    // ── Los dias de cada tramo ───────────────────────────────────────────────

    @Bean
    TemporaryDisabilityDaysConceptCalculator diasItEmpresa(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityDaysConceptCalculator("D_IT_E60", EMPRESA_60, tramos);
    }

    @Bean
    TemporaryDisabilityDaysConceptCalculator diasItDelegado60(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityDaysConceptCalculator("D_IT_D60", DELEGADO_60, tramos);
    }

    @Bean
    TemporaryDisabilityDaysConceptCalculator diasItDelegado75(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityDaysConceptCalculator("D_IT_D75", DELEGADO_75, tramos);
    }

    // ── Los porcentajes de cada tramo ────────────────────────────────────────

    @Bean
    TemporaryDisabilityRateConceptCalculator tipoItEmpresa(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityRateConceptCalculator("P_IT_E60", IT_COMMON, EMPRESA_60, tramos);
    }

    @Bean
    TemporaryDisabilityRateConceptCalculator tipoItDelegado60(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityRateConceptCalculator("P_IT_D60", IT_COMMON, DELEGADO_60, tramos);
    }

    @Bean
    TemporaryDisabilityRateConceptCalculator tipoItDelegado75(ItPrestacionTramoRepository tramos) {
        return new TemporaryDisabilityRateConceptCalculator("P_IT_D75", IT_COMMON, DELEGADO_75, tramos);
    }
}
