package com.b4rrhh.employee.lifecycle.application.usecase;

import com.b4rrhh.employee.contract.application.command.CreateContractCommand;
import com.b4rrhh.employee.contract.application.command.ListEmployeeContractsCommand;
import com.b4rrhh.employee.contract.application.usecase.CreateContractUseCase;
import com.b4rrhh.employee.contract.application.usecase.ListEmployeeContractsUseCase;
import com.b4rrhh.employee.contract.domain.exception.ContractCoverageIncompleteException;
import com.b4rrhh.employee.contract.domain.exception.ContractInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractOutsidePresencePeriodException;
import com.b4rrhh.employee.contract.domain.exception.ContractOverlapException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeInvalidException;
import com.b4rrhh.employee.contract.domain.exception.ContractSubtypeRelationInvalidException;
import com.b4rrhh.employee.contract.domain.exception.InvalidContractDateRangeException;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionItem;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterCatalogValueInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.model.EmployeeStatus;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.ListEmployeeLaborClassificationsCommand;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.domain.exception.InvalidLaborClassificationDateRangeException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementCategoryRelationInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCategoryInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOutsidePresencePeriodException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.lifecycle.application.command.RehireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.RehireEmployeeResult;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeConflictException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeDependentRelationInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeDistributionInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeEmployeeNotFoundException;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeRequestInvalidException;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceCommand;
import com.b4rrhh.employee.presence.application.usecase.CreatePresenceUseCase;
import com.b4rrhh.employee.presence.application.usecase.ListEmployeePresencesUseCase;
import com.b4rrhh.employee.presence.domain.exception.ActivePresenceAlreadyExistsException;
import com.b4rrhh.employee.presence.domain.exception.InvalidPresenceDateRangeException;
import com.b4rrhh.employee.presence.domain.exception.PresenceCatalogValueInvalidException;
import com.b4rrhh.employee.presence.domain.exception.PresenceOverlapException;
import com.b4rrhh.employee.presence.domain.model.Presence;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesCommand;
import com.b4rrhh.employee.working_time.application.usecase.ListEmployeeWorkingTimesUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import com.b4rrhh.employee.working_time.domain.model.WorkingTime;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterCommand;
import com.b4rrhh.employee.workcenter.application.usecase.CreateWorkCenterUseCase;
import com.b4rrhh.employee.workcenter.application.usecase.ListEmployeeWorkCentersUseCase;
import com.b4rrhh.employee.workcenter.domain.exception.InvalidWorkCenterDateRangeException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOutsidePresencePeriodException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterOverlapException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterPresenceCoverageGapException;
import com.b4rrhh.employee.workcenter.domain.model.WorkCenter;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class RehireEmployeeService implements RehireEmployeeUseCase {

    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    private final EmployeeRepository employeeRepository;
    private final ListEmployeePresencesUseCase listEmployeePresencesUseCase;
    private final ListEmployeeContractsUseCase listEmployeeContractsUseCase;
    private final ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase;
    private final ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase;
    private final ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase;
    private final CreatePresenceUseCase createPresenceUseCase;
    private final CreateLaborClassificationUseCase createLaborClassificationUseCase;
    private final CreateContractUseCase createContractUseCase;
    private final CreateWorkCenterUseCase createWorkCenterUseCase;
    private final CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;
    private final CreateWorkingTimeUseCase createWorkingTimeUseCase;
    private final WorkCenterCompanyValidator workCenterCompanyValidator;
    private final EmployeeTypeCatalogValidator employeeTypeCatalogValidator;

    public RehireEmployeeService(
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
            EmployeeRepository employeeRepository,
            ListEmployeePresencesUseCase listEmployeePresencesUseCase,
            ListEmployeeContractsUseCase listEmployeeContractsUseCase,
            ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase,
            ListEmployeeWorkCentersUseCase listEmployeeWorkCentersUseCase,
            ListEmployeeWorkingTimesUseCase listEmployeeWorkingTimesUseCase,
            CreatePresenceUseCase createPresenceUseCase,
            CreateLaborClassificationUseCase createLaborClassificationUseCase,
            CreateContractUseCase createContractUseCase,
            CreateWorkCenterUseCase createWorkCenterUseCase,
            CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase,
            CreateWorkingTimeUseCase createWorkingTimeUseCase,
            WorkCenterCompanyValidator workCenterCompanyValidator,
            EmployeeTypeCatalogValidator employeeTypeCatalogValidator
    ) {
        this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
        this.employeeRepository = employeeRepository;
        this.listEmployeePresencesUseCase = listEmployeePresencesUseCase;
        this.listEmployeeContractsUseCase = listEmployeeContractsUseCase;
        this.listEmployeeLaborClassificationsUseCase = listEmployeeLaborClassificationsUseCase;
        this.listEmployeeWorkCentersUseCase = listEmployeeWorkCentersUseCase;
        this.listEmployeeWorkingTimesUseCase = listEmployeeWorkingTimesUseCase;
        this.createPresenceUseCase = createPresenceUseCase;
        this.createLaborClassificationUseCase = createLaborClassificationUseCase;
        this.createContractUseCase = createContractUseCase;
        this.createWorkCenterUseCase = createWorkCenterUseCase;
        this.createCostCenterDistributionUseCase = createCostCenterDistributionUseCase;
        this.createWorkingTimeUseCase = createWorkingTimeUseCase;
        this.workCenterCompanyValidator = workCenterCompanyValidator;
        this.employeeTypeCatalogValidator = employeeTypeCatalogValidator;
    }

    @Override
    @Transactional
    public RehireEmployeeResult rehire(RehireEmployeeCommand command) {
        if (command == null) {
            throw new RehireEmployeeRequestInvalidException("request body is required");
        }

        String ruleSystemCode = normalizeRequiredCode("ruleSystemCode", command.ruleSystemCode(), false);
        String employeeTypeCode = normalizeRequiredCode("employeeTypeCode", command.employeeTypeCode(), false);
        String employeeNumber = normalizeRequiredText("employeeNumber", command.employeeNumber());
        LocalDate rehireDate = normalizeRequiredDate(command.rehireDate());
        String entryReasonCode = normalizeRequiredCode("entryReasonCode", command.entryReasonCode(), false);
        String companyCode = normalizeRequiredCode("companyCode", command.companyCode(), false);
        String agreementCode = normalizeRequiredCode("agreementCode", command.agreementCode(), false);
        String agreementCategoryCode = normalizeRequiredCode("agreementCategoryCode", command.agreementCategoryCode(), false);
        String contractTypeCode = normalizeRequiredCode("contractTypeCode", command.contractTypeCode(), false);
        String contractSubtypeCode = normalizeRequiredCode("contractSubtypeCode", command.contractSubtypeCode(), false);
        String workCenterCode = normalizeRequiredCode("workCenterCode", command.workCenterCode(), false);
        RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand workingTime = normalizeRequiredWorkingTime(command.workingTime());

        Employee employee = getEmployeeByBusinessKeyUseCase
                .getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow(() -> new RehireEmployeeEmployeeNotFoundException(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber
                ));

        workCenterCompanyValidator.validateBelongsToCompany(
            ruleSystemCode,
            workCenterCode,
            companyCode,
            rehireDate
        );

        List<Presence> presenceHistory = listEmployeePresencesUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        List<Contract> contractHistory = listEmployeeContractsUseCase.listByEmployeeBusinessKey(
                new ListEmployeeContractsCommand(ruleSystemCode, employeeTypeCode, employeeNumber)
        );
        List<LaborClassification> laborHistory = listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(
                new ListEmployeeLaborClassificationsCommand(ruleSystemCode, employeeTypeCode, employeeNumber)
        );
        List<WorkCenter> workCenterHistory = listEmployeeWorkCentersUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber);
        List<WorkingTime> workingTimeHistory = listEmployeeWorkingTimesUseCase.listByEmployeeBusinessKey(
            new ListEmployeeWorkingTimesCommand(ruleSystemCode, employeeTypeCode, employeeNumber)
        );

        List<Presence> activePresences = presenceHistory.stream().filter(Presence::isActive).toList();
        if (!activePresences.isEmpty()) {
            return resolveIdempotentOrConflict(
                    employee,
                    rehireDate,
                    entryReasonCode,
                    companyCode,
                    agreementCode,
                    agreementCategoryCode,
                    contractTypeCode,
                    contractSubtypeCode,
                    workCenterCode,
                    activePresences,
                    contractHistory,
                    laborHistory,
                    workCenterHistory,
                    workingTimeHistory,
                    workingTime.workingTimePercentage()
            );
        }

        if (!employee.isTerminated()) {
            throw new RehireEmployeeConflictException(
                    "Employee has no active presence but status is not TERMINATED"
            );
        }

        List<Presence> closedPresences = presenceHistory.stream()
                .filter(presence -> presence.getEndDate() != null)
                .toList();
        if (closedPresences.isEmpty()) {
            throw new RehireEmployeeConflictException("At least one previously closed presence is required for rehire");
        }

        Presence latestClosedPresence = closedPresences.stream()
                .max(Comparator.comparing(Presence::getEndDate)
                        .thenComparing(Presence::getPresenceNumber))
                .orElseThrow();

        if (!rehireDate.isAfter(latestClosedPresence.getEndDate())) {
            throw new RehireEmployeeConflictException(
                    "rehireDate must be strictly after the endDate of the latest closed presence"
            );
        }

        Presence createdPresence;
        LaborClassification createdLaborClassification;
        Contract createdContract;
        WorkCenter createdWorkCenter;
        CostCenterDistributionWindow createdCostCenter = null;
        WorkingTime createdWorkingTime;

        try {
            employeeTypeCatalogValidator.validateEmployeeTypeCode(ruleSystemCode, employeeTypeCode, rehireDate);

            createdPresence = createPresenceUseCase.create(new CreatePresenceCommand(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    companyCode,
                    entryReasonCode,
                    null,
                    rehireDate,
                    null
            ));

            createdLaborClassification = createLaborClassificationUseCase.create(new CreateLaborClassificationCommand(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    agreementCode,
                    agreementCategoryCode,
                    rehireDate,
                    null
            ));

            createdContract = createContractUseCase.create(new CreateContractCommand(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    contractTypeCode,
                    contractSubtypeCode,
                    rehireDate,
                    null
            ));

            createdWorkCenter = createWorkCenterUseCase.create(new CreateWorkCenterCommand(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    workCenterCode,
                    rehireDate,
                    null
            ));

            if (command.costCenterDistribution() != null) {
                validateCostCenterDistributionItems(command.costCenterDistribution());
                createdCostCenter = createCostCenterDistributionUseCase.create(new CreateCostCenterDistributionCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        rehireDate,
                        command.costCenterDistribution().items().stream()
                                .map(item -> new CostCenterDistributionItem(
                                        item.costCenterCode(),
                                        item.allocationPercentage()
                                ))
                                .collect(Collectors.toList())
                ));
            }

            createdWorkingTime = createWorkingTimeUseCase.create(new CreateWorkingTimeCommand(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    rehireDate,
                    null,
                    workingTime.workingTimePercentage()
            ));
        } catch (EmployeeTypeInvalidException
                 | PresenceCatalogValueInvalidException
                 | LaborClassificationAgreementInvalidException
                 | LaborClassificationCategoryInvalidException
                 | ContractInvalidException
                 | ContractSubtypeInvalidException
                 | WorkCenterCatalogValueInvalidException
                 | CostCenterCatalogValueInvalidException ex) {
            throw new RehireEmployeeCatalogValueInvalidException(ex.getMessage(), ex);
        } catch (CostCenterDistributionInvalidException ex) {
            throw new RehireEmployeeDistributionInvalidException(ex.getMessage(), ex);
        } catch (LaborClassificationAgreementCategoryRelationInvalidException
                 | ContractSubtypeRelationInvalidException ex) {
            throw new RehireEmployeeDependentRelationInvalidException(ex.getMessage(), ex);
        } catch (InvalidWorkingTimePercentageException
                 | WorkingTimeOutsidePresencePeriodException
                 | WorkingTimeOverlapException
                 | WorkingTimeCoverageGapException ex) {
            throw new RehireEmployeeBusinessValidationException(ex.getMessage(), ex);
        } catch (WorkingTimeNumberConflictException ex) {
            throw new RehireEmployeeConflictException(ex.getMessage(), ex);
        } catch (WorkingTimeEmployeeNotFoundException ex) {
            throw new RehireEmployeeConflictException("Employee is not available for rehire workingTime creation", ex);
        } catch (ActivePresenceAlreadyExistsException
                 | PresenceOverlapException
                 | InvalidPresenceDateRangeException
                 | ContractOverlapException
                 | InvalidContractDateRangeException
                 | ContractOutsidePresencePeriodException
                 | ContractCoverageIncompleteException
                 | LaborClassificationOverlapException
                 | InvalidLaborClassificationDateRangeException
                 | LaborClassificationOutsidePresencePeriodException
                 | LaborClassificationCoverageIncompleteException
                 | WorkCenterOverlapException
                 | InvalidWorkCenterDateRangeException
                 | WorkCenterOutsidePresencePeriodException
                 | WorkCenterPresenceCoverageGapException ex) {
            throw new RehireEmployeeConflictException(ex.getMessage(), ex);
        }

        Employee activeEmployee = employeeRepository.save(employee.activate());

        long activePresenceCountAfterRehire = listEmployeePresencesUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .stream()
                .filter(Presence::isActive)
                .count();

        if (activePresenceCountAfterRehire != 1) {
            throw new RehireEmployeeConflictException(
                    "Rehire post-condition failed: exactly one active presence is required"
            );
        }

        return new RehireEmployeeResult(
                activeEmployee.getRuleSystemCode(),
                activeEmployee.getEmployeeTypeCode(),
                activeEmployee.getEmployeeNumber(),
                rehireDate,
                activeEmployee.getStatus(),
                createdPresence.getPresenceNumber(),
                createdPresence.getCompanyCode(),
                createdPresence.getEntryReasonCode(),
                createdPresence.getStartDate(),
                createdContract.getContractCode(),
                createdContract.getContractSubtypeCode(),
                createdContract.getStartDate(),
                createdLaborClassification.getAgreementCode(),
                createdLaborClassification.getAgreementCategoryCode(),
                createdLaborClassification.getStartDate(),
                createdWorkCenter.getWorkCenterAssignmentNumber(),
                createdWorkCenter.getWorkCenterCode(),
                createdWorkCenter.getStartDate(),
                buildCostCenterSummary(createdCostCenter),
                toWorkingTimeSummary(createdWorkingTime),
                true
        );
    }

    private RehireEmployeeResult resolveIdempotentOrConflict(
            Employee employee,
            LocalDate rehireDate,
            String entryReasonCode,
            String companyCode,
            String agreementCode,
            String agreementCategoryCode,
            String contractTypeCode,
            String contractSubtypeCode,
            String workCenterCode,
            List<Presence> activePresences,
            List<Contract> contractHistory,
            List<LaborClassification> laborHistory,
                List<WorkCenter> workCenterHistory,
                List<WorkingTime> workingTimeHistory,
                java.math.BigDecimal workingTimePercentage
    ) {
        if (!employee.isActive()) {
            throw new RehireEmployeeConflictException("Active presence exists but employee status is not ACTIVE");
        }

        Presence activePresence = requireExactlyOneActive("presence", activePresences, presence -> true);
        if (!rehireDate.equals(activePresence.getStartDate())) {
            throw new RehireEmployeeConflictException("Existing active presence startDate is not equivalent to rehireDate");
        }
        if (!entryReasonCode.equals(activePresence.getEntryReasonCode())) {
            throw new RehireEmployeeConflictException("Existing active presence entryReasonCode is not equivalent");
        }
        if (!companyCode.equals(activePresence.getCompanyCode())) {
            throw new RehireEmployeeConflictException("Existing active presence companyCode is not equivalent");
        }

        Contract activeContract = requireExactlyOneActive("contract", contractHistory, Contract::isActive);
        if (!rehireDate.equals(activeContract.getStartDate())) {
            throw new RehireEmployeeConflictException("Existing active contract startDate is not equivalent to rehireDate");
        }
        if (!contractTypeCode.equals(activeContract.getContractCode())) {
            throw new RehireEmployeeConflictException("Existing active contract contractTypeCode is not equivalent");
        }
        if (!contractSubtypeCode.equals(activeContract.getContractSubtypeCode())) {
            throw new RehireEmployeeConflictException("Existing active contract contractSubtypeCode is not equivalent");
        }

        LaborClassification activeLaborClassification = requireExactlyOneActive(
                "labor classification",
                laborHistory,
                LaborClassification::isActive
        );
        if (!rehireDate.equals(activeLaborClassification.getStartDate())) {
            throw new RehireEmployeeConflictException(
                    "Existing active labor classification startDate is not equivalent to rehireDate"
            );
        }
        if (!agreementCode.equals(activeLaborClassification.getAgreementCode())) {
            throw new RehireEmployeeConflictException("Existing active labor classification agreementCode is not equivalent");
        }
        if (!agreementCategoryCode.equals(activeLaborClassification.getAgreementCategoryCode())) {
            throw new RehireEmployeeConflictException(
                    "Existing active labor classification agreementCategoryCode is not equivalent"
            );
        }

        WorkCenter activeWorkCenter = requireExactlyOneActive("work center", workCenterHistory, WorkCenter::isActive);
        WorkingTime activeWorkingTime = requireExactlyOneActive("working time", workingTimeHistory, WorkingTime::isActive);
        if (!isEquivalentActiveCycle(
                rehireDate,
                entryReasonCode,
                companyCode,
                agreementCode,
                agreementCategoryCode,
                contractTypeCode,
                contractSubtypeCode,
                workCenterCode,
            workingTimePercentage,
                activePresence,
                activeContract,
                activeLaborClassification,
            activeWorkCenter,
            activeWorkingTime
        )) {
            throw new RehireEmployeeConflictException("Active employee cycle is not equivalent to rehire request");
        }

        return new RehireEmployeeResult(
                employee.getRuleSystemCode(),
                employee.getEmployeeTypeCode(),
                employee.getEmployeeNumber(),
                rehireDate,
                EmployeeStatus.ACTIVE.name(),
                activePresence.getPresenceNumber(),
                activePresence.getCompanyCode(),
                activePresence.getEntryReasonCode(),
                activePresence.getStartDate(),
                activeContract.getContractCode(),
                activeContract.getContractSubtypeCode(),
                activeContract.getStartDate(),
                activeLaborClassification.getAgreementCode(),
                activeLaborClassification.getAgreementCategoryCode(),
                activeLaborClassification.getStartDate(),
                activeWorkCenter.getWorkCenterAssignmentNumber(),
                activeWorkCenter.getWorkCenterCode(),
                activeWorkCenter.getStartDate(),
                null,
                toWorkingTimeSummary(activeWorkingTime),
                false
        );
    }

    private boolean isEquivalentActiveCycle(
            LocalDate rehireDate,
            String entryReasonCode,
            String companyCode,
            String agreementCode,
            String agreementCategoryCode,
            String contractTypeCode,
            String contractSubtypeCode,
            String workCenterCode,
                java.math.BigDecimal workingTimePercentage,
            Presence activePresence,
            Contract activeContract,
            LaborClassification activeLaborClassification,
                WorkCenter activeWorkCenter,
                WorkingTime activeWorkingTime
    ) {
        return rehireDate.equals(activePresence.getStartDate())
                && entryReasonCode.equals(activePresence.getEntryReasonCode())
                && companyCode.equals(activePresence.getCompanyCode())
                && rehireDate.equals(activeContract.getStartDate())
                && contractTypeCode.equals(activeContract.getContractCode())
                && contractSubtypeCode.equals(activeContract.getContractSubtypeCode())
                && rehireDate.equals(activeLaborClassification.getStartDate())
                && agreementCode.equals(activeLaborClassification.getAgreementCode())
                && agreementCategoryCode.equals(activeLaborClassification.getAgreementCategoryCode())
                && rehireDate.equals(activeWorkCenter.getStartDate())
                && workCenterCode.equals(activeWorkCenter.getWorkCenterCode())
                && rehireDate.equals(activeWorkingTime.getStartDate())
                && activeWorkingTime.getEndDate() == null
                && activeWorkingTime.getWorkingTimePercentage().compareTo(workingTimePercentage) == 0;
    }

    private RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand normalizeRequiredWorkingTime(
            RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand workingTime
    ) {
        if (workingTime == null) {
            throw new RehireEmployeeRequestInvalidException("workingTime is required");
        }
        if (workingTime.workingTimePercentage() == null) {
            throw new RehireEmployeeRequestInvalidException("workingTime.workingTimePercentage is required");
        }

        return new RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand(
                workingTime.workingTimePercentage().stripTrailingZeros()
        );
    }

    private <T> T requireExactlyOneActive(String label, List<T> occurrences, Predicate<T> isActive) {
        List<T> active = occurrences.stream().filter(isActive).toList();

        if (active.size() != 1) {
            throw new RehireEmployeeConflictException(
                    "Expected exactly one active " + label + " but found " + active.size()
            );
        }

        return active.get(0);
    }

    private RehireEmployeeResult.CostCenterSummary buildCostCenterSummary(CostCenterDistributionWindow window) {
        if (window == null) {
            return null;
        }

        return new RehireEmployeeResult.CostCenterSummary(
                window.getStartDate(),
                window.getTotalAllocationPercentage().doubleValue(),
                window.getItems().stream()
                        .map(item -> new RehireEmployeeResult.CostCenterItemSummary(
                                item.getCostCenterCode(),
                                item.getCostCenterCode(),
                                item.getAllocationPercentage().doubleValue()
                        ))
                        .collect(Collectors.toList())
        );
    }

    private RehireEmployeeResult.WorkingTimeSummary toWorkingTimeSummary(WorkingTime workingTime) {
        return new RehireEmployeeResult.WorkingTimeSummary(
                workingTime.getWorkingTimeNumber(),
                workingTime.getWorkingTimePercentage(),
                workingTime.getWeeklyHours(),
                workingTime.getDailyHours(),
                workingTime.getMonthlyHours(),
                workingTime.getStartDate(),
                workingTime.getEndDate()
        );
    }

    private void validateCostCenterDistributionItems(
            RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand distribution
    ) {
        if (distribution.items() == null || distribution.items().isEmpty()) {
            throw new RehireEmployeeDistributionInvalidException(
                    "cost center distribution must contain at least one item"
            );
        }
        for (RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand item : distribution.items()) {
            if (item.allocationPercentage() == null) {
                throw new RehireEmployeeDistributionInvalidException(
                        "allocationPercentage is required for cost center item: " + item.costCenterCode()
                );
            }
        }
    }

    private String normalizeRequiredText(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new RehireEmployeeRequestInvalidException(fieldName + " is required");
        }

        return value.trim();
    }

    private String normalizeRequiredCode(String fieldName, String value, boolean allowTrimOnly) {
        if (value == null || value.trim().isEmpty()) {
            throw new RehireEmployeeRequestInvalidException(fieldName + " is required");
        }

        String normalized = value.trim().toUpperCase();
        if (!allowTrimOnly && normalized.isEmpty()) {
            throw new RehireEmployeeRequestInvalidException(fieldName + " is required");
        }

        return normalized;
    }

    private LocalDate normalizeRequiredDate(LocalDate value) {
        if (value == null) {
            throw new RehireEmployeeRequestInvalidException("rehireDate is required");
        }

        return value;
    }
}
