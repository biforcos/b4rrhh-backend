package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.employee.workcenter.application.usecase.WorkCenterRuleEntityTypeCodes;
import com.b4rrhh.payroll.application.port.WorkCenterProfileContext;
import com.b4rrhh.payroll.application.port.WorkCenterProfileLookupPort;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WorkCenterProfileLookupAdapter implements WorkCenterProfileLookupPort {

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public WorkCenterProfileLookupAdapter(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    @Override
    public Optional<WorkCenterProfileContext> findByRuleSystemAndCode(String ruleSystemCode, String workCenterCode) {
        if (workCenterCode == null || workCenterCode.isBlank()) return Optional.empty();
        // La nomina no tiene idioma de respuesta: literal base.
        String name = ruleEntityLabelResolver
                .resolveName(ruleSystemCode, WorkCenterRuleEntityTypeCodes.WORK_CENTER, workCenterCode, null)
                .orElse(null);
        return Optional.of(new WorkCenterProfileContext(workCenterCode, name));
    }
}
