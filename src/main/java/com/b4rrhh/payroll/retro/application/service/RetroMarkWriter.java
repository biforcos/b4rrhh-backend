package com.b4rrhh.payroll.retro.application.service;

import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.payroll.domain.model.PayrollTypeCodes;
import com.b4rrhh.payroll.retro.application.port.ClosedPeriodPresenceLookupPort;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkSource;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * El otro lado del puerto: convierte un aviso de escritura con fecha en marcas de retroactividad
 * ({@code backend#130}).
 *
 * <p>La regla entera, que es lo que este servicio existe para tener escrito una sola vez:
 *
 * <blockquote>
 * Si el periodo de la escritura ya tiene un recibo <b>entregado</b> de ese empleado, queda una marca
 * por cada presencia que lo tenga. Si no, no queda nada.
 * </blockquote>
 *
 * <p>Vive en {@code payroll} y no en {@code employee} porque la marca es de payroll —su unidad es un
 * periodo de nomina, la consume un {@code calculation_run} y la cierra un recibo— y porque
 * {@code employee} no importa {@code payroll} en este arbol. La interfaz esta alli; la regla, aqui.
 */
@Service
public class RetroMarkWriter implements DatedWriteNoticePort {

    private final ClosedPeriodPresenceLookupPort closedPeriods;
    private final RetroMarkRepository marks;

    public RetroMarkWriter(
            ClosedPeriodPresenceLookupPort closedPeriods,
            RetroMarkRepository marks
    ) {
        this.closedPeriods = closedPeriods;
        this.marks = marks;
    }

    /**
     * {@code REQUIRED} y no {@code REQUIRES_NEW}: la marca y la escritura que la provoca <b>van o no
     * van juntas</b>.
     *
     * <p>Si la escritura se deshace y la marca quedara, el motor recalcularia un mes por un cambio que
     * no existe y el recibo llevaria una linea de atraso de cero —o peor, de lo que otro cambio
     * hubiera dejado a medias—. Y al contrario es peor todavia: una escritura que sobrevive sin su
     * marca es dinero que nadie va a pagar.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void notice(DatedWrite write) {
        List<Integer> presencias = closedPeriods.findPresencesWithDefinitivePayroll(
                write.ruleSystemCode(),
                write.employeeTypeCode(),
                write.employeeNumber(),
                write.periodCode(),
                PayrollTypeCodes.NORMAL);

        if (presencias.isEmpty()) {
            // El periodo esta abierto, o no tiene recibo todavia. No es pasado: no hay nada que
            // marcar y tampoco nada que avisar.
            return;
        }

        RetroMarkSource origen = new RetroMarkSource(
                write.verticalCode(), write.table(), write.rowId(), write.rowKey());

        for (Integer presencia : presencias) {
            marks.save(RetroMark.create(
                    write.ruleSystemCode(),
                    write.employeeTypeCode(),
                    write.employeeNumber(),
                    presencia,
                    write.periodCode(),
                    origen,
                    Instant.now()));
        }
    }
}
