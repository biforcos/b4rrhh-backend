package com.b4rrhh.payroll_engine.execution.application.service;

import com.b4rrhh.payroll_engine.execution.domain.model.TechnicalConceptSegmentData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@code ENGINE_PROVIDED} calculator for concept P_IRPF — tipo de retencion IRPF.
 *
 * <p>Devuelve un 15 % fijo. Es un valor de arranque, no un calculo.
 *
 * <h2>Por que sigue en codigo cuando los tipos de cotizacion se han ido a la tabla</h2>
 *
 * <p>Esta es la respuesta al criterio 4 del {@code backend#105}, y esta escrita aqui porque es
 * aqui donde llega quien se pregunte de donde sale el 15: <b>el IRPF no entra en
 * {@code ss_cotizacion_tipos} y no debe entrar.</b>
 *
 * <p>Un tipo de cotizacion es un numero por contingencia y por fecha, igual para todo el mundo:
 * cabe en una fila con vigencia y es exactamente lo que esa tabla sabe guardar. Una retencion de
 * IRPF no es eso. Se calcula por tramos sobre la retribucion anual prevista y depende de la
 * situacion personal y familiar de cada persona, asi que <b>ni es un tipo, ni es unico, ni se
 * puede buscar por contingencia</b>. Meterlo ahi seria llamarle tipo de cotizacion a algo que no
 * lo es, y la tabla dejaria de significar lo que significa.
 *
 * <p>Donde va cuando se haga: en {@code employee.employee_tax_information} —que existe y hoy
 * esta vacia— mas el calculo por tramos que la lea. Eso es otro issue y no este.
 *
 * <p>Mientras tanto el 15 % se queda, y se queda <b>visible</b>: sale en el recibo como
 * {@code TIPO_IRPF 15,00}, que es mejor que esconderlo en una tabla donde pareceria un dato
 * gobernado.
 */
@Component
public class IrpfWithholdingRateCalculator implements TechnicalConceptCalculator {

    private static final BigDecimal IRPF_RATE = new BigDecimal("15.00");

    @Override
    public String conceptCode() {
        return "P_IRPF";
    }

    @Override
    public BigDecimal resolve(TechnicalConceptSegmentData context) {
        return IRPF_RATE;
    }
}
