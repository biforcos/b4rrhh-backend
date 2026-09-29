package com.b4rrhh.payroll.retro.application.usecase;

import java.util.Set;
import com.b4rrhh.payroll.retro.domain.model.RetroMarkStatus;
import com.b4rrhh.payroll.retro.application.port.CeasedPresenceWithoutReceiptsLookupPort;
import com.b4rrhh.payroll.retro.application.port.RetroMarkEmployeeLookupPort;
import com.b4rrhh.payroll.retro.domain.exception.RetroMarkEmployeeNotFoundException;
import com.b4rrhh.payroll.retro.domain.model.RetroMark;
import com.b4rrhh.payroll.retro.domain.port.RetroMarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Las marcas de un empleado, <b>todas y en su estado</b> ({@code backend#130}).
 *
 * <p>Todas y no solo las activas: la ficha tiene que ensenar las descartadas con su motivo —es la
 * razon de que descartar no borre— y las consumidas con el recibo que las pago.
 */
@Service
public class ListEmployeeRetroMarksService implements ListEmployeeRetroMarksUseCase {

    private final RetroMarkRepository marks;
    private final CeasedPresenceWithoutReceiptsLookupPort ceasedPresences;
    private final RetroMarkEmployeeLookupPort employees;

    public ListEmployeeRetroMarksService(
            RetroMarkRepository marks,
            CeasedPresenceWithoutReceiptsLookupPort ceasedPresences,
            RetroMarkEmployeeLookupPort employees
    ) {
        this.marks = marks;
        this.ceasedPresences = ceasedPresences;
        this.employees = employees;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetroMark> list(ListEmployeeRetroMarksCommand command) {
        return marks.findByEmployee(
                command.ruleSystemCode().trim().toUpperCase(),
                command.employeeTypeCode().trim().toUpperCase(),
                command.employeeNumber().trim());
    }

    /**
     * Cada marca con si le queda recibo que la pague ({@code backend#139}): activa, y de una presencia
     * que cesó y cuyo ultimo mes ya se cerro, no la paga nadie.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ListedRetroMark> listWithReceiptStatus(ListEmployeeRetroMarksCommand command) {
        String rs = command.ruleSystemCode().trim().toUpperCase();
        String tipo = command.employeeTypeCode().trim().toUpperCase();
        String numero = command.employeeNumber().trim();
        // «No tiene marcas» y «no existe» no son lo mismo, y la ficha los decía igual (b4rrhh/backend#144).
        if (!employees.exists(rs, tipo, numero)) {
            throw new RetroMarkEmployeeNotFoundException(rs, tipo, numero);
        }
        Set<Integer> sinRecibos = ceasedPresences.findPresenceNumbers(rs, tipo, numero);
        return marks.findByEmployee(rs, tipo, numero).stream()
                .map(m -> new ListedRetroMark(m,
                        m.getStatus() == RetroMarkStatus.ACTIVE && sinRecibos.contains(m.getPresenceNumber())))
                .toList();
    }
}
