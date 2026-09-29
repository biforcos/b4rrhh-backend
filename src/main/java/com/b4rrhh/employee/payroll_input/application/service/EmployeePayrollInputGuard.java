package com.b4rrhh.employee.payroll_input.application.service;

import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.payroll_input.application.port.PayrollInputConceptLookupPort;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputConceptInvalidException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputEmployeeNotFoundException;
import com.b4rrhh.employee.payroll_input.domain.exception.EmployeePayrollInputOutsidePresenceException;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.model.Presence;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Una entrada de nómina sólo se acepta donde un recibo puede pagarla (b4rrhh/backend#142).
 *
 * <p>Tres cosas, en el orden en que se preguntan: que el empleado exista, que el concepto exista y
 * sea de entrada ({@code EMPLOYEE_INPUT}: los demás se calculan, no se introducen), y que el mes
 * tenga al menos un día de presencia. Un mes cubierto en parte se acepta: el motor prorratea por
 * días. Uno sin ni un día no tiene recibo que la consuma, y la entrada desaparecería sin que nadie
 * lo dijera: rechazarla cuesta un mensaje, aceptarla cuesta la entrada.
 *
 * <p>La tabla guarda la clave de negocio en texto y sin clave ajena, así que nada de esto lo impedía
 * la base.
 */
@Component
public class EmployeePayrollInputGuard {

    private static final String EMPLOYEE_INPUT = "EMPLOYEE_INPUT";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    private final ListEmployeePresencesUseCase listEmployeePresencesUseCase;
    private final PayrollInputConceptLookupPort conceptLookupPort;

    public EmployeePayrollInputGuard(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
            ListEmployeePresencesUseCase listEmployeePresencesUseCase,
            PayrollInputConceptLookupPort conceptLookupPort
    ) {
        this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
        this.listEmployeePresencesUseCase = listEmployeePresencesUseCase;
        this.conceptLookupPort = conceptLookupPort;
    }

    public void requireEmployee(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        if (getEmployeeByBusinessKeyUseCase.getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber).isEmpty()) {
            throw new EmployeePayrollInputEmployeeNotFoundException(ruleSystemCode, employeeTypeCode, employeeNumber);
        }
    }

    public void check(String ruleSystemCode, String employeeTypeCode, String employeeNumber,
                      String conceptCode, int period) {
        requireEmployee(ruleSystemCode, employeeTypeCode, employeeNumber);

        String calculationType = conceptLookupPort.calculationTypeOf(ruleSystemCode, conceptCode)
                .orElseThrow(() -> new EmployeePayrollInputConceptInvalidException(
                        "El concepto " + conceptCode + " no existe en " + ruleSystemCode + "."));
        if (!EMPLOYEE_INPUT.equals(calculationType)) {
            throw new EmployeePayrollInputConceptInvalidException(
                    "El concepto " + conceptCode + " no es de entrada: se calcula, no se introduce.");
        }

        YearMonth month = YearMonth.of(period / 100, period % 100);
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        List<Presence> presences = listEmployeePresencesUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        boolean covered = presences.stream().anyMatch(presence ->
                !presence.getStartDate().isAfter(last)
                        && (presence.getEndDate() == null || !presence.getEndDate().isBefore(first)));
        if (!covered) {
            throw new EmployeePayrollInputOutsidePresenceException(
                    describe(presences) + "; " + period + " no tiene ni un día.");
        }
    }

    /** «El empleado estuvo de alta del 08/12/2025 al 13/05/2026 y desde el 01/07/2026». */
    private static String describe(List<Presence> presences) {
        if (presences.isEmpty()) {
            return "El empleado no tiene ninguna presencia";
        }
        String spans = presences.stream()
                .sorted(Comparator.comparing(Presence::getStartDate))
                .map(presence -> presence.getEndDate() == null
                        ? "desde el " + presence.getStartDate().format(DAY)
                        : "del " + presence.getStartDate().format(DAY) + " al " + presence.getEndDate().format(DAY))
                .collect(Collectors.joining(", "));
        int comma = spans.lastIndexOf(", ");
        if (comma >= 0) {
            spans = spans.substring(0, comma) + " y " + spans.substring(comma + 2);
        }
        return "El empleado estuvo de alta " + spans;
    }
}
