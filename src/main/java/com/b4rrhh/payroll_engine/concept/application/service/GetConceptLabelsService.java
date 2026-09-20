package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.GetConceptLabelsUseCase;
import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabelLanguage;
import com.b4rrhh.payroll_engine.concept.domain.port.ConceptLabelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class GetConceptLabelsService implements GetConceptLabelsUseCase {

    private final ConceptLabelRepository conceptLabelRepository;

    public GetConceptLabelsService(ConceptLabelRepository conceptLabelRepository) {
        this.conceptLabelRepository = conceptLabelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> byRuleSystemCode(String ruleSystemCode) {
        return conceptLabelRepository.findLabelsByRuleSystemCode(
                ruleSystemCode, ConceptLabelLanguage.DEFAULT);
    }
}
