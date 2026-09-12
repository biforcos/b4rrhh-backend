package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionItemResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionWindowResponse;
import com.b4rrhh.employee.lifecycle.application.command.RehireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.RehireEmployeeResult;
import com.b4rrhh.employee.lifecycle.domain.exception.RehireEmployeeRequestInvalidException;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehireEmployeeRequest;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehireEmployeeResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehireEmployeeWorkingTimeRequest;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehiredContractResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehiredLaborClassificationResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehiredPresenceResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehiredWorkingTimeResponse;
import com.b4rrhh.employee.lifecycle.infrastructure.rest.dto.RehiredWorkCenterResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class RehireEmployeeWebMapper {

    public RehireEmployeeCommand toCommand(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            RehireEmployeeRequest request
    ) {
        if (request == null) {
            throw new RehireEmployeeRequestInvalidException("request body is required");
        }
        if (request.laborClassification() == null) {
            throw new RehireEmployeeRequestInvalidException("laborClassification is required");
        }
        if (request.contract() == null) {
            throw new RehireEmployeeRequestInvalidException("contract is required");
        }
        if (request.workCenter() == null) {
            throw new RehireEmployeeRequestInvalidException("workCenter is required");
        }

        return new RehireEmployeeCommand(
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                request.rehireDate(),
                request.entryReasonCode(),
                request.companyCode(),
                request.laborClassification().agreementCode(),
                request.laborClassification().agreementCategoryCode(),
                request.contract().contractTypeCode(),
                request.contract().contractSubtypeCode(),
                request.workCenter().workCenterCode(),
                request.costCenterDistribution() != null
                        ? new RehireEmployeeCommand.RehireEmployeeCostCenterDistributionCommand(
                                request.costCenterDistribution().items().stream()
                                        .map(item -> new RehireEmployeeCommand.RehireEmployeeCostCenterItemCommand(
                                                item.costCenterCode(),
                                                item.allocationPercentage()
                                        ))
                                        .collect(Collectors.toList())
                          )
                                                : null,
                                toWorkingTimeCommand(request.workingTime())
        );
    }

        private RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand toWorkingTimeCommand(RehireEmployeeWorkingTimeRequest request) {
                if (request == null) {
                        throw new RehireEmployeeRequestInvalidException("workingTime is required");
                }
                rejectDerivedHours(request);
                if (request.workingTimePercentage() == null) {
                        throw new RehireEmployeeRequestInvalidException("workingTime.workingTimePercentage is required");
                }

                return new RehireEmployeeCommand.RehireEmployeeWorkingTimeCommand(request.workingTimePercentage());
        }

        private void rejectDerivedHours(RehireEmployeeWorkingTimeRequest request) {
                if (request.weeklyHours() != null) {
                        throw new RehireEmployeeRequestInvalidException("workingTime.weeklyHours is not accepted");
                }
                if (request.dailyHours() != null) {
                        throw new RehireEmployeeRequestInvalidException("workingTime.dailyHours is not accepted");
                }
                if (request.monthlyHours() != null) {
                        throw new RehireEmployeeRequestInvalidException("workingTime.monthlyHours is not accepted");
                }
        }

    public RehireEmployeeResponse toResponse(RehireEmployeeResult result) {
        return new RehireEmployeeResponse(
                result.ruleSystemCode(),
                result.employeeTypeCode(),
                result.employeeNumber(),
                result.rehireDate(),
                result.status(),
                new RehiredPresenceResponse(
                        result.newPresenceNumber(),
                        result.newPresenceCompanyCode(),
                        result.newPresenceEntryReasonCode(),
                        result.newPresenceStartDate()
                ),
                new RehiredContractResponse(
                        result.newContractTypeCode(),
                        result.newContractSubtypeCode(),
                        result.newContractStartDate()
                ),
                new RehiredLaborClassificationResponse(
                        result.newAgreementCode(),
                        result.newAgreementCategoryCode(),
                        result.newLaborClassificationStartDate()
                ),
                new RehiredWorkCenterResponse(
                        result.newWorkCenterAssignmentNumber(),
                        result.newWorkCenterCode(),
                        result.newWorkCenterStartDate()
                ),
                result.newCostCenter() != null
                        ? new CostCenterDistributionWindowResponse(
                                result.newCostCenter().startDate(),
                                null,
                                BigDecimal.valueOf(result.newCostCenter().totalAllocationPercentage()),
                                result.newCostCenter().items().stream()
                                        .map(item -> new CostCenterDistributionItemResponse(
                                                item.costCenterCode(),
                                                null, // no enrichment at command response level
                                                BigDecimal.valueOf(item.allocationPercentage())
                                        ))
                                        .collect(Collectors.toList())
                          )
                        : null,
                new RehiredWorkingTimeResponse(
                        result.newWorkingTime().workingTimeNumber(),
                        result.newWorkingTime().workingTimePercentage(),
                        result.newWorkingTime().weeklyHours(),
                        result.newWorkingTime().dailyHours(),
                        result.newWorkingTime().monthlyHours(),
                        result.newWorkingTime().startDate(),
                        result.newWorkingTime().endDate()
                )
        );
    }
}
