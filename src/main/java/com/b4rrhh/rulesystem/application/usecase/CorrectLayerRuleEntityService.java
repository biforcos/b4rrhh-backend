package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.LayerNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Corrige la fila de la capa: la ven todas las reglamentaciones que la montan (frontend#131). */
@Service
public class CorrectLayerRuleEntityService implements CorrectLayerRuleEntityUseCase {

    private final LayerRepository layerRepository;
    private final RuleEntityRepository ruleEntityRepository;

    public CorrectLayerRuleEntityService(LayerRepository layerRepository, RuleEntityRepository ruleEntityRepository) {
        this.layerRepository = layerRepository;
        this.ruleEntityRepository = ruleEntityRepository;
    }

    @Override
    @Transactional
    public RuleEntity correct(CorrectLayerRuleEntityCommand command) {
        String layerCode = LayerRuleEntityInput.requiredCode("layerCode", command.layerCode());
        String ruleEntityTypeCode = LayerRuleEntityInput.requiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = LayerRuleEntityInput.requiredCode("code", command.code());
        LocalDate startDate = LayerRuleEntityInput.requiredDate("startDate", command.startDate());
        String name = LayerRuleEntityInput.requiredName(command.name());
        String description = LayerRuleEntityInput.description(command.description());

        layerRepository.findByCode(layerCode).orElseThrow(() -> new LayerNotFoundException(layerCode));
        RuleEntity existing = ruleEntityRepository
                .findInLayerByBusinessKeyAndStartDate(layerCode, ruleEntityTypeCode, code, startDate)
                .orElseThrow(() -> new RuleEntityNotFoundException(layerCode, ruleEntityTypeCode, code, startDate));

        existing.correct(name, description, command.endDate());

        if (ruleEntityRepository.existsOverlapInLayerExcludingStartDate(
                layerCode, ruleEntityTypeCode, code, existing.getStartDate(), existing.getEndDate(), existing.getStartDate())) {
            throw new RuleEntityOverlapException(layerCode, ruleEntityTypeCode, code);
        }

        return ruleEntityRepository.save(existing);
    }
}
