package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Cero ({@code backend#129}).
 *
 * <p>Parece un chiste y no lo es: es el <b>suelo</b> del complemento del convenio, y en este motor un
 * limite es un concepto. El {@code B_CC} se recorta con {@code GREATEST(B_CC_MAX, P_TOPE_MIN)} desde la
 * {@code V88}, y el complemento se recorta igual, con {@code GREATEST(diferencia, P_CERO)}.
 *
 * <p>La alternativa era un {@code Math.max(…, 0)} dentro de un calculador, y entonces «este complemento
 * no puede ser negativo» seria una regla que solo esta en el codigo. La pregunta «de donde sale este
 * numero» no puede tener una respuesta que no este en el grafo (V146).
 *
 * <p>Y el caso es real: el complemento rellena hasta el 100 % del salario base de grupo, y la prestacion
 * es un porcentaje de la <b>base reguladora</b>, que lleva la prorrata dentro y por tanto es mayor que
 * el salario del dia. Al 75 % la diferencia es cero justa; con una base reguladora que venga de un mes
 * con horas extra, es negativa.
 */
@Component
public class ZeroConceptCalculator implements TechnicalConceptCalculator {

    @Override
    public String conceptCode() {
        return "P_CERO";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return BigDecimal.ZERO;
    }
}
