package com.b4rrhh.payroll.retro.application.service;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import com.b4rrhh.payroll.retro.infrastructure.persistence.SpringDataCurrentCalculationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Las marcas se consumen cuando el periodo <b>se cierra</b>, no cuando se calcula ({@code backend#133}).
 *
 * <h2>Por que al cerrar</h2>
 *
 * <p>Porque calcular no paga nada. Un recibo {@code CALCULATED} se vuelve a calcular entero y no ha
 * salido del sistema: si la marca se consumiera ahi, <b>una nomina de prueba se llevaria por delante el
 * atraso</b> y el recibo definitivo saldria sin el, sin que nada lo dijera.
 *
 * <p>Y por lo mismo, <b>invalidar el periodo no las devuelve</b>: no hace falta, porque nunca se
 * consumieron. Las marcas siguen activas y el siguiente calculo de ese periodo las vuelve a pagar. Eso
 * es lo que hace que invalidar sea seguro.
 *
 * <h2>Cuales consume</h2>
 *
 * <p>Las que apuntan a un mes que <b>esta corrida recalculo</b>. Y no las que tienen linea de atraso en
 * el recibo, que no es lo mismo: una marca puede apuntar a un mes cuyo delta salio cero —alguien cambio
 * algo y lo volvio a dejar como estaba— y esa marca esta atendida igual. Consumirla es lo correcto;
 * dejarla activa la haria aparecer en la checklist para siempre.
 */
@Service
public class RetroMarkConsumer {

    private static final Logger log = LoggerFactory.getLogger(RetroMarkConsumer.class);

    private final RetroMarkRepository marks;
    private final SpringDataCurrentCalculationRepository currentCalculations;

    public RetroMarkConsumer(
            RetroMarkRepository marks,
            SpringDataCurrentCalculationRepository currentCalculations
    ) {
        this.marks = marks;
        this.currentCalculations = currentCalculations;
    }

    /** Se llama con el recibo ya {@code DEFINITIVE}, dentro de su misma transaccion. */
    public void consumeFor(Payroll definitive) {
        if (definitive.getRunId() == null) {
            // Un recibo que no viene de una corrida -no hay ninguno hoy, pero el modelo lo admite- no
            // puede decir que meses se recalcularon con el, asi que no consume nada.
            return;
        }

        Set<String> recalculados = Set.copyOf(currentCalculations.findPeriodsRecalculatedByRun(
                definitive.getRuleSystemCode(),
                definitive.getEmployeeTypeCode(),
                definitive.getEmployeeNumber(),
                definitive.getPayrollTypeCode(),
                definitive.getPresenceNumber(),
                definitive.getRunId()));
        if (recalculados.isEmpty()) {
            return;
        }

        List<RetroMark> activas = marks.findActiveByEmployee(
                definitive.getRuleSystemCode(),
                definitive.getEmployeeTypeCode(),
                definitive.getEmployeeNumber());

        int consumidas = 0;
        for (RetroMark marca : activas) {
            boolean deEstaPresencia = marca.getPresenceNumber() != null
                    && marca.getPresenceNumber().equals(definitive.getPresenceNumber());
            if (deEstaPresencia && recalculados.contains(marca.getFromPeriodCode())) {
                marks.save(marca.consume(
                        definitive.getPayrollPeriodCode(), definitive.getRunId(), Instant.now()));
                consumidas++;
            }
        }

        if (consumidas > 0) {
            log.info("[RETRO] Marcas consumidas | empleado={} periodo={} presencia={} -> {}",
                    definitive.getEmployeeNumber(), definitive.getPayrollPeriodCode(),
                    definitive.getPresenceNumber(), consumidas);
        }
    }
}
