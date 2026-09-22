package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.port.SsCotizacionTiposRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Que concepto del motor lee que contingencia ({@code backend#105}).
 *
 * <p>La correspondencia esta aqui y en un solo sitio. Antes estaba repartida en nueve clases
 * iguales de dieciocho lineas, cada una con su numero dentro; lo unico que las distinguia era
 * el codigo de concepto y la constante, y la constante se ha ido a la tabla.
 *
 * <p>Los nombres no coinciden y por eso hace falta declararlo: el concepto del motor se llama
 * {@code P_SS_CC} y la contingencia del catalogo {@code CC_TRAB}. Adivinarlo con una regla de
 * transformacion de cadenas funcionaria para ocho de los nueve y fallaria en silencio en el
 * noveno.
 */
@Configuration
public class SsCotizacionRateCalculators {

    // ── Trabajador ───────────────────────────────────────────────────────────

    @Bean
    SsCotizacionRateCalculator ccTrabajadorRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_CC", "CC_TRAB", tipos);
    }

    @Bean
    SsCotizacionRateCalculator desempleoTrabajadorRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_DESEMPLEO", "DESEMPLEO_TRAB", tipos);
    }

    @Bean
    SsCotizacionRateCalculator fpTrabajadorRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_FP_TRAB", "FP_TRAB", tipos);
    }

    @Bean
    SsCotizacionRateCalculator meiTrabajadorRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_MEI_TRAB", "MEI_TRAB", tipos);
    }

    // ── Empresa ──────────────────────────────────────────────────────────────

    @Bean
    SsCotizacionRateCalculator ccEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_CC_EMP", "CC_EMP", tipos);
    }

    @Bean
    SsCotizacionRateCalculator desempleoEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_DESEMPLEO_EMP", "DESEMPLEO_EMP", tipos);
    }

    @Bean
    SsCotizacionRateCalculator fpEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_FP_EMP", "FP_EMP", tipos);
    }

    @Bean
    SsCotizacionRateCalculator fogasaEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_FOGASA_EMP", "FOGASA_EMP", tipos);
    }

    @Bean
    SsCotizacionRateCalculator meiEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_SS_MEI_EMP", "MEI_EMP", tipos);
    }

    // ── Cotizacion adicional por horas extraordinarias (backend#121) ─────────

    @Bean
    SsCotizacionRateCalculator horasExtraTrabajadorRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_HE_TRAB", "HORAS_EXTRA_TRAB", tipos);
    }

    @Bean
    SsCotizacionRateCalculator horasExtraEmpresarioRate(SsCotizacionTiposRepository tipos) {
        return new SsCotizacionRateCalculator("P_HE_EMP", "HORAS_EXTRA_EMP", tipos);
    }
}
