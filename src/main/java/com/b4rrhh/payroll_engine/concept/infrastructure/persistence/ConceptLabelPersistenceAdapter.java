package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptNotFoundException;
import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabel;
import com.b4rrhh.payroll_engine.concept.domain.port.ConceptLabelRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import com.b4rrhh.payroll_engine.object.infrastructure.persistence.SpringDataPayrollObjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ConceptLabelPersistenceAdapter implements ConceptLabelRepository {

    private final SpringDataConceptLabelRepository labelRepository;
    private final SpringDataPayrollObjectRepository objectRepository;

    public ConceptLabelPersistenceAdapter(
            SpringDataConceptLabelRepository labelRepository,
            SpringDataPayrollObjectRepository objectRepository
    ) {
        this.labelRepository = labelRepository;
        this.objectRepository = objectRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findLabelsByRuleSystemCode(String ruleSystemCode, String languageCode) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (Object[] row : labelRepository.findCodeAndLabel(ruleSystemCode, languageCode)) {
            labels.put((String) row[0], (String) row[1]);
        }
        return labels;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConceptLabel> findByBusinessKey(
            String ruleSystemCode, String conceptCode, String languageCode) {
        return labelRepository.findOne(ruleSystemCode, conceptCode, languageCode)
                .map(entity -> new ConceptLabel(conceptCode, entity.getLanguageCode(), entity.getLabel()));
    }

    @Override
    @Transactional
    public ConceptLabel save(String ruleSystemCode, ConceptLabel label) {
        PayrollObjectEntity object = objectRepository
                .findByRuleSystemCodeAndObjectTypeCodeAndObjectCode(
                        ruleSystemCode,
                        PayrollObjectTypeCode.CONCEPT.name(),
                        label.conceptCode())
                .orElseThrow(() -> new PayrollConceptNotFoundException(ruleSystemCode, label.conceptCode()));

        ConceptLabelEntity entity = labelRepository
                .findOne(ruleSystemCode, label.conceptCode(), label.languageCode())
                .orElseGet(() -> {
                    ConceptLabelEntity nuevo = new ConceptLabelEntity();
                    nuevo.setObjectId(object.getId());
                    nuevo.setLanguageCode(label.languageCode());
                    return nuevo;
                });
        entity.setLabel(label.label());
        labelRepository.save(entity);
        return label;
    }
}
