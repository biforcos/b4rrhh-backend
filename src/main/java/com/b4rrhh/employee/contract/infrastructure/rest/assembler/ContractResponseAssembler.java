package com.b4rrhh.employee.contract.infrastructure.rest.assembler;

import com.b4rrhh.employee.contract.application.model.ContractPlan;
import com.b4rrhh.employee.contract.application.model.ContractPlanAdjustment;
import com.b4rrhh.employee.contract.application.usecase.ContractRuleEntityTypeCodes;
import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.domain.model.ContractPeriod;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractPeriodResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractPlanAdjustmentResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractPlanResponse;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContractResponseAssembler {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public ContractResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    public ContractResponse toResponse(String ruleSystemCode, Contract contract, ResponseLanguage language) {
        String contractTypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, ContractRuleEntityTypeCodes.CONTRACT,
                        contract.getContractCode(), language.code())
                .orElse(null);
        String contractSubtypeName = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, ContractRuleEntityTypeCodes.CONTRACT_SUBTYPE,
                        contract.getContractSubtypeCode(), language.code())
                .orElse(null);

        return new ContractResponse(
                contract.getContractCode(),
                contractTypeName,
                contract.getContractSubtypeCode(),
                contractSubtypeName,
                contract.getStartDate(),
                contract.getEndDate()
        );
    }

    public List<ContractResponse> toResponseList(String ruleSystemCode, List<Contract> contracts, ResponseLanguage language) {
        return contracts.stream()
                .map(contract -> toResponse(ruleSystemCode, contract, language))
                .toList();
    }

    public ContractPlanResponse toPlanResponse(ContractPlan plan) {
        return new ContractPlanResponse(
                plan.operation().name(),
                plan.isAccepted(),
                plan.rejection() == null ? null : plan.rejection().name(),
                toPeriod(plan.occurrence()),
                plan.correctedOccurrence() == null ? null : toPeriod(plan.correctedOccurrence()),
                toAdjustment(plan.adjustedOccurrence()),
                plan.overlaps().stream().map(this::toPeriod).toList(),
                plan.gaps().stream().map(this::toPeriod).toList(),
                plan.stretchCandidates().stream().map(this::toPeriod).toList(),
                plan.projected().stream().map(this::toPeriod).toList()
        );
    }

    private ContractPlanAdjustmentResponse toAdjustment(ContractPlanAdjustment adjustment) {
        if (adjustment == null) {
            return null;
        }

        return new ContractPlanAdjustmentResponse(toPeriod(adjustment.before()), toPeriod(adjustment.after()));
    }

    private ContractPeriodResponse toPeriod(ContractPeriod period) {
        return new ContractPeriodResponse(period.startDate(), period.endDate());
    }
}
