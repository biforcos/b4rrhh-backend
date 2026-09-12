package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.employee.presence.infrastructure.persistence.PresenceEntity;
import com.b4rrhh.employee.presence.infrastructure.persistence.SpringDataPresenceRepository;
import com.b4rrhh.employee.shared.infrastructure.persistence.EmployeeBusinessKeyLookupSupport;
import com.b4rrhh.employee.working_time.application.port.EmployeeAgreementContext;
import com.b4rrhh.employee.working_time.infrastructure.persistence.EmployeeAgreementContextRepository;
import com.b4rrhh.employee.working_time.infrastructure.persistence.SpringDataWorkingTimeRepository;
import com.b4rrhh.employee.working_time.infrastructure.persistence.WorkingTimeEntity;
import com.b4rrhh.employee.workcenter.infrastructure.persistence.SpringDataWorkCenterRepository;
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

    public PayrollLaunchEligibleInputLookupAdapter(
            EmployeeBusinessKeyLookupSupport employeeLookupSupport,
            SpringDataPresenceRepository presenceRepository,
            EmployeeAgreementContextRepository agreementContextRepository,
            EmployeeAgreementCategoryRepository agreementCategoryRepository,
            SpringDataWorkingTimeRepository workingTimeRepository,
            SpringDataWorkCenterRepository workCenterRepository
    ) {
        this.employeeLookupSupport = employeeLookupSupport;
        this.presenceRepository = presenceRepository;
        this.agreementContextRepository = agreementContextRepository;
        this.agreementCategoryRepository = agreementCategoryRepository;
        this.workingTimeRepository = workingTimeRepository;
        this.workCenterRepository = workCenterRepository;
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

        String workCenterCode = workCenterRepository
                .findActiveByEmployeeIdAndReferenceDate(employeeId, referenceDate, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(wc -> wc.getWorkCenterCode())
                .orElse(null);

        return Optional.of(new PayrollLaunchEligibleInputContext(
                presence.getCompanyCode(),
                agreementCode,
                agreementCategoryCode,
                windows,
                presence.getStartDate(),
                presence.getEndDate(),
                workCenterCode
        ));
    }

    /**
     * La fecha a la que se resuelve el contexto vigente de la unidad: el ultimo dia que el
     * empleado estuvo presente dentro del periodo.
     *
     * APANO ACOTADO, y lo sustituye el backend#47. Esto se preguntaba a fin de periodo, y quien
     * cesa el 15 tiene la clasificacion cerrada con el cese: a fin de mes no hay ninguna vigente,
     * el lanzador lo contaba como entrada que falta y no le hacia recibo (backend#73).
     *
     * Arregla el cese, y no arregla el cambio de categoria a mitad de mes: ahi sigue ganando el
     * ultimo tramo para todo el periodo, que da un numero equivocado en vez de una ausencia. La
     * salida buena es que las fechas de corte salgan de la union de los puntos de cambio de las
     * verticales (backend#47), y entonces nadie pregunta por el convenio de un tramo que no
     * existe. Ese issue tiene dos decisiones de negocio abiertas —que verticales rompen y que
     * pasa con SegmentSpec.workingTimePercentage—, y el cese no podia esperarlas.
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

    private PayrollLaunchWorkingTimeWindowContext toWorkingTimeWindow(WorkingTimeEntity workingTime) {
        return new PayrollLaunchWorkingTimeWindowContext(
                workingTime.getStartDate(),
                workingTime.getEndDate(),
                workingTime.getWorkingTimePercentage()
        );
    }
}
