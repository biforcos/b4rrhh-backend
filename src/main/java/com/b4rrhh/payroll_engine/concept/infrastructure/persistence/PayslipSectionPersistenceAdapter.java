package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayslipSectionPersistenceAdapter implements PayslipSectionRepository {

    private final SpringDataPayslipSectionRepository sectionRepository;
    private final SpringDataPayslipSectionNatureRepository natureRepository;

    public PayslipSectionPersistenceAdapter(
            SpringDataPayslipSectionRepository sectionRepository,
            SpringDataPayslipSectionNatureRepository natureRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.natureRepository = natureRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipSection> findByRuleSystemCode(String ruleSystemCode) {
        return sectionRepository.findOrdered(ruleSystemCode).stream()
                .map(e -> new PayslipSection(e.getSectionCode(), e.getSectionLabel(), e.getDisplayOrder()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findSectionCodeByNature(String ruleSystemCode) {
        Map<String, String> byNature = new LinkedHashMap<>();
        for (PayslipSectionNatureEntity e : natureRepository.findByRuleSystemCode(ruleSystemCode)) {
            byNature.put(e.getFunctionalNature(), e.getSectionCode());
        }
        return byNature;
    }
}
