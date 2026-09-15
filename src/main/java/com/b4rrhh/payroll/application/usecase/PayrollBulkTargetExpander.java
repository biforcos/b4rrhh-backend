package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchPresenceLookupPort;
import com.b4rrhh.payroll.domain.exception.InvalidPayrollArgumentException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * El selector de objetivo de los verbos masivos: de a quien apunta el encargo a que unidades de
 * calculo son.
 *
 * <p>Existe porque el {@code backend#102} pedia que el cierre masivo tuviera <b>el mismo</b>
 * selector que el invalidador masivo, y «el mismo» escrito dos veces deja de serlo en cuanto
 * alguien arregla uno. Las dos partes que comparten son la validacion de la seleccion y la
 * expansion a unidades: un empleado del periodo puede tener mas de una presencia relevante, y
 * entonces tiene mas de un recibo.
 *
 * <p>No lo usa {@code LaunchPayrollCalculationService}, que tiene su propia copia: el lanzamiento
 * necesita anotar {@code NO_RELEVANT_PRESENCE} en la ejecucion por cada objetivo que se queda sin
 * presencias, y eso pide una ejecucion abierta que los verbos masivos no tienen. Es la misma forma
 * con un trabajo mas, no la misma funcion.
 */
@Component
class PayrollBulkTargetExpander {

    private final PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort;

    PayrollBulkTargetExpander(PayrollLaunchPresenceLookupPort payrollLaunchPresenceLookupPort) {
        this.payrollLaunchPresenceLookupPort = payrollLaunchPresenceLookupPort;
    }

    /** La seleccion, validada, con los campos que no son de su tipo puestos a nulo. */
    PayrollLaunchTargetSelection normalize(PayrollLaunchTargetSelection targetSelection) {
        if (targetSelection == null || targetSelection.selectionType() == null) {
            throw new InvalidPayrollArgumentException("targetSelection.selectionType is required");
        }

        return switch (targetSelection.selectionType()) {
            case SINGLE_EMPLOYEE -> {
                if (targetSelection.employee() == null) {
                    throw new InvalidPayrollArgumentException("targetSelection.employee is required for SINGLE_EMPLOYEE");
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.SINGLE_EMPLOYEE,
                        targetSelection.employee(),
                        null
                );
            }
            case EMPLOYEE_LIST -> {
                if (targetSelection.employees() == null || targetSelection.employees().isEmpty()) {
                    throw new InvalidPayrollArgumentException("targetSelection.employees is required for EMPLOYEE_LIST");
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.EMPLOYEE_LIST,
                        null,
                        List.copyOf(targetSelection.employees())
                );
            }
            case ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD -> {
                if (targetSelection.employee() != null || targetSelection.employees() != null) {
                    throw new InvalidPayrollArgumentException(
                            "targetSelection.employee and targetSelection.employees must be null for ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD"
                    );
                }
                yield new PayrollLaunchTargetSelection(
                        PayrollLaunchTargetSelectionType.ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD,
                        null,
                        null
                );
            }
        };
    }

    /**
     * Las unidades de calculo del encargo: una por presencia relevante de cada empleado objetivo.
     *
     * <p>Por eso el total de candidatas no es el numero de empleados, y el contrato lo dice desde el
     * {@code backend#85}: un mes partido deja dos presencias y dos recibos para la misma persona.
     */
    List<PayrollCalculationUnit> expand(
            PayrollLaunchTargetSelection targetSelection,
            String ruleSystemCode,
            String payrollPeriodCode,
            String payrollTypeCode,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        List<PayrollCalculationUnit> units = new ArrayList<>();
        for (PayrollLaunchEmployeeTarget employeeTarget : resolveEmployees(
                targetSelection, ruleSystemCode, periodStart, periodEnd)) {
            List<PayrollLaunchPresenceContext> presences = payrollLaunchPresenceLookupPort.findRelevantPresences(
                    ruleSystemCode,
                    employeeTarget.employeeTypeCode(),
                    employeeTarget.employeeNumber(),
                    periodStart,
                    periodEnd
            );
            for (PayrollLaunchPresenceContext presence : presences) {
                units.add(new PayrollCalculationUnit(
                        ruleSystemCode,
                        presence.employeeTypeCode(),
                        presence.employeeNumber(),
                        payrollPeriodCode,
                        payrollTypeCode,
                        presence.presenceNumber()
                ));
            }
        }
        return units;
    }

    private List<PayrollLaunchEmployeeTarget> resolveEmployees(
            PayrollLaunchTargetSelection targetSelection,
            String ruleSystemCode,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        List<PayrollLaunchEmployeeTarget> rawTargets = switch (targetSelection.selectionType()) {
            case SINGLE_EMPLOYEE -> List.of(targetSelection.employee());
            case EMPLOYEE_LIST -> targetSelection.employees();
            case ALL_EMPLOYEES_WITH_PRESENCE_IN_PERIOD -> payrollLaunchPresenceLookupPort
                    .findEmployeesWithPresenceInPeriod(ruleSystemCode, periodStart, periodEnd)
                    .stream()
                    .map(emp -> new PayrollLaunchEmployeeTarget(emp.employeeTypeCode(), emp.employeeNumber()))
                    .toList();
        };

        // Un empleado repetido en la lista es un encargo, no dos: sin esto, la misma unidad se
        // visitaria dos veces y los contadores dirian que habia dos.
        LinkedHashMap<String, PayrollLaunchEmployeeTarget> uniqueTargets = new LinkedHashMap<>();
        for (PayrollLaunchEmployeeTarget rawTarget : rawTargets) {
            String employeeTypeCode = PayrollFieldNormalizer.code(
                    rawTarget.employeeTypeCode(), "targetSelection.employeeTypeCode", 30);
            String employeeNumber = PayrollFieldNormalizer.text(
                    rawTarget.employeeNumber(), "targetSelection.employeeNumber", 15);
            uniqueTargets.put(employeeTypeCode + "|" + employeeNumber,
                    new PayrollLaunchEmployeeTarget(employeeTypeCode, employeeNumber));
        }
        return List.copyOf(uniqueTargets.values());
    }
}
