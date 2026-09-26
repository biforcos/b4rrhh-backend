package com.b4rrhh.employee.cost_center.application.usecase;

import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterContext;
import com.b4rrhh.employee.cost_center.application.port.EmployeeCostCenterLookupPort;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.port.CostCenterRepository;
import com.b4rrhh.employee.shared.application.port.DatedWrite;
import com.b4rrhh.employee.shared.application.port.DatedWriteNoticePort;
import com.b4rrhh.employee.shared.application.port.DatedWriteSources;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Closes all active cost center lines for an employee at the termination date.
 * Identifies the active window by its startDate and closes all its lines together.
 * Does not fail if no active distribution exists — termination may precede any cost center setup.
 */
@Service
public class CloseActiveCostCenterDistributionAtTerminationService
        implements CloseActiveCostCenterDistributionAtTerminationUseCase {

    private static final String TABLA = "employee.cost_center";

    private final CostCenterRepository costCenterRepository;
    private final EmployeeCostCenterLookupPort employeeCostCenterLookupPort;
    private final DatedWriteNoticePort datedWrites;

    public CloseActiveCostCenterDistributionAtTerminationService(
            CostCenterRepository costCenterRepository,
            EmployeeCostCenterLookupPort employeeCostCenterLookupPort,
            DatedWriteNoticePort datedWrites
    ) {
        this.costCenterRepository = costCenterRepository;
        this.employeeCostCenterLookupPort = employeeCostCenterLookupPort;
        this.datedWrites = datedWrites;
    }

    @Override
    @Transactional
    public void closeIfPresent(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            LocalDate terminationDate
    ) {
        Optional<EmployeeCostCenterContext> employeeOpt = employeeCostCenterLookupPort
                .findByBusinessKeyForUpdate(ruleSystemCode, employeeTypeCode, employeeNumber);

        if (employeeOpt.isEmpty()) {
            return;
        }

        Long employeeId = employeeOpt.get().employeeId();

        List<CostCenterAllocation> activeLines = costCenterRepository.findActiveAtDate(
                employeeId, terminationDate
        );

        if (activeLines.isEmpty()) {
            return;
        }

        // Group active lines by their startDate to identify the window
        LocalDate windowStartDate = activeLines.get(0).getStartDate();
        costCenterRepository.closeAllForWindow(employeeId, windowStartDate, terminationDate);

        // El cese tambien es una escritura con fecha, y este participante avisa por su parte. El dia
        // SIGUIENTE al cierre, que es cuando la ventana deja de cubrir (backend#130).
        datedWrites.notice(DatedWrite.on(terminationDate.plusDays(1),
                ruleSystemCode, employeeTypeCode, employeeNumber,
                DatedWriteSources.COST_CENTER, TABLA, windowStartDate.toString()));
    }
}
