package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.employee.contract.infrastructure.persistence.ContractEntity;
import com.b4rrhh.employee.contract.infrastructure.persistence.SpringDataContractRepository;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.LaborClassificationEntity;
import com.b4rrhh.employee.labor_classification.infrastructure.persistence.SpringDataLaborClassificationRepository;
import com.b4rrhh.employee.presence.infrastructure.persistence.PresenceEntity;
import com.b4rrhh.employee.presence.infrastructure.persistence.SpringDataPresenceRepository;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContext;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextRepository;
import com.b4rrhh.employee.working_time.infrastructure.persistence.SpringDataWorkingTimeRepository;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimeEntity;
import com.b4rrhh.employee.workcenter.infrastructure.persistence.SpringDataWorkCenterRepository;
import com.b4rrhh.payroll.application.port.PayrollLaunchAgreementWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchContractWindowContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkingTimeWindowContext;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository.EmployeeAgreementCategoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class PayrollLaunchEligibleInputLookupAdapter implements PayrollLaunchEligibleInputLookupPort {

    private final EmployeeBusinessKeyLookupSupport employeeLookupSupport;
    private final SpringDataPresenceRepository presenceRepository;
    private final EmployeeAgreementContextRepository agreementContextRepository;
    private final EmployeeAgreementCategoryRepository agreementCategoryRepository;
    private final SpringDataWorkingTimeRepository workingTimeRepository;
    private final SpringDataWorkCenterRepository workCenterRepository;
    private final SpringDataLaborClassificationRepository laborClassificationRepository;
    private final SpringDataContractRepository contractRepository;

    public PayrollLaunchEligibleInputLookupAdapter(
            EmployeeBusinessKeyLookupSupport employeeLookupSupport,
            SpringDataPresenceRepository presenceRepository,
            EmployeeAgreementContextRepository agreementContextRepository,
            EmployeeAgreementCategoryRepository agreementCategoryRepository,
            SpringDataWorkingTimeRepository workingTimeRepository,
            SpringDataWorkCenterRepository workCenterRepository,
            SpringDataLaborClassificationRepository laborClassificationRepository,
            SpringDataContractRepository contractRepository
    ) {
        this.employeeLookupSupport = employeeLookupSupport;
        this.presenceRepository = presenceRepository;
        this.agreementContextRepository = agreementContextRepository;
        this.agreementCategoryRepository = agreementCategoryRepository;
        this.workingTimeRepository = workingTimeRepository;
        this.workCenterRepository = workCenterRepository;
        this.laborClassificationRepository = laborClassificationRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    public Optional<PayrollLaunchEligibleInputContext> findByUnitAndPeriod(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            Integer presenceNumber,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        Optional<Long> employeeIdOpt = employeeLookupSupport.findByBusinessKey(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber
                )
                .map(employee -> employee.getId());

        if (employeeIdOpt.isEmpty()) {
            return Optional.empty();
        }

        Long employeeId = employeeIdOpt.get();
        Optional<PresenceEntity> presenceOpt = presenceRepository.findByEmployeeIdAndPresenceNumber(employeeId, presenceNumber)
                .filter(presence -> isOverlapping(presence.getStartDate(), presence.getEndDate(), periodStart, periodEnd));

        if (presenceOpt.isEmpty()) {
            return Optional.empty();
        }

        PresenceEntity presence = presenceOpt.get();
        LocalDate referenceDate = lastDayInsidePresence(presence, periodEnd);

        List<EmployeeAgreementContext> agreementContexts = agreementContextRepository.findLatestValidByEmployeeIdAndEffectiveDate(
                employeeId,
                referenceDate,
                PageRequest.of(0, 1)
        );
        String agreementCode = agreementContexts.isEmpty() ? null : agreementContexts.getFirst().agreementCode();

        List<String> categories = agreementCategoryRepository.findLatestValidByEmployeeIdAndEffectiveDate(
                employeeId,
                referenceDate,
                PageRequest.of(0, 1)
        );
        String agreementCategoryCode = categories.isEmpty() ? null : categories.getFirst();

        List<PayrollLaunchWorkingTimeWindowContext> windows = workingTimeRepository
                .findOverlappingByEmployeeIdAndPeriodOrdered(employeeId, periodStart, periodEnd)
                .stream()
                .map(this::toWorkingTimeWindow)
                .toList();

        // Los TRAMOS, y no solo el vigente a una fecha: son los que parten el periodo (backend#47).
        // Se piden contra el periodo entero y no contra la presencia porque el recorte contra la
        // presencia lo hace la particion, que es donde estan las dos fechas a la vez.
        List<PayrollLaunchAgreementWindowContext> agreementWindows = laborClassificationRepository
                .findOverlappingByEmployeeIdAndPeriodOrdered(employeeId, periodStart, periodEnd)
                .stream()
                .map(PayrollLaunchEligibleInputLookupAdapter::toAgreementWindow)
                .toList();

        List<PayrollLaunchContractWindowContext> contractWindows = contractRepository
                .findOverlappingByEmployeeIdAndPeriodOrdered(employeeId, periodStart, periodEnd)
                .stream()
                .map(PayrollLaunchEligibleInputLookupAdapter::toContractWindow)
                .toList();

        String workCenterCode = workCenterRepository
                .findActiveByEmployeeIdAndReferenceDate(employeeId, referenceDate, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(wc -> wc.getWorkCenterCode())
                .orElse(null);

        // La antiguedad sale de la presencia MAS ANTIGUA del empleado y no de la de esta
        // unidad: quien se readmite la conserva del primer alta (backend#91). Para EMP000001
        // —alta 25/11/2023, cese por jubilacion, readmision 08/03/2024— son dos fechas
        // distintas, y es el unico caso en el que se nota.
        LocalDate seniorityDate = presenceRepository.findEarliestStartDateByEmployeeId(employeeId);

        return Optional.of(new PayrollLaunchEligibleInputContext(
                presence.getCompanyCode(),
                agreementCode,
                agreementCategoryCode,
                windows,
                agreementWindows,
                contractWindows,
                presence.getStartDate(),
                presence.getEndDate(),
                workCenterCode,
                seniorityDate
        ));
    }

    /**
     * La fecha a la que se resuelve el contexto vigente de la unidad: el ultimo dia que el
     * empleado estuvo presente dentro del periodo.
     *
     * Esto se preguntaba a fin de periodo, y quien cesa el 15 tiene la clasificacion cerrada con el
     * cese: a fin de mes no habia ninguna vigente, el lanzador lo contaba como entrada que falta y
     * no le hacia recibo (backend#73).
     *
     * LO QUE ESTA FECHA DECIDE, DESDE EL backend#47, YA NO ES EL PRECIO DE CADA DIA. El precio sale
     * de los tramos, que parten el periodo y resuelven su categoria cada uno. Esta fecha resuelve
     * lo que es del PERIODO y no del tramo: el convenio con el que se arma el plan de conceptos
     * —que conceptos entran— y la foto del contexto que se guarda con el recibo. Que un empleado
     * cambie de categoria a mitad de mes no cambia que conceptos se le calculan; cambia a que
     * precio, y eso ya no se pregunta aqui.
     */
    private LocalDate lastDayInsidePresence(PresenceEntity presence, LocalDate periodEnd) {
        LocalDate presenceEnd = presence.getEndDate();
        return presenceEnd != null && presenceEnd.isBefore(periodEnd) ? presenceEnd : periodEnd;
    }

    private boolean isOverlapping(
            LocalDate sourceStart,
            LocalDate sourceEnd,
            LocalDate queryStart,
            LocalDate queryEnd
    ) {
        return !sourceStart.isAfter(queryEnd)
                && (sourceEnd == null || !sourceEnd.isBefore(queryStart));
    }

    private static PayrollLaunchAgreementWindowContext toAgreementWindow(LaborClassificationEntity e) {
        return new PayrollLaunchAgreementWindowContext(
                e.getStartDate(), e.getEndDate(), e.getAgreementCode(), e.getAgreementCategoryCode());
    }

    private static PayrollLaunchContractWindowContext toContractWindow(ContractEntity e) {
        return new PayrollLaunchContractWindowContext(
                e.getStartDate(), e.getEndDate(), e.getContractCode(), e.getContractSubtypeCode());
    }

    private PayrollLaunchWorkingTimeWindowContext toWorkingTimeWindow(WorkingTimeEntity workingTime) {
        return new PayrollLaunchWorkingTimeWindowContext(
                workingTime.getStartDate(),
                workingTime.getEndDate(),
                workingTime.getWorkingTimePercentage()
        );
    }
}
