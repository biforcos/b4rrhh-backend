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

@Service
public class CloseLayerRuleEntityService implements CloseLayerRuleEntityUseCase {

    private final LayerRepository layerRepository;
    private final RuleEntityRepository ruleEntityRepository;

    public CloseLayerRuleEntityService(LayerRepository layerRepository, RuleEntityRepository ruleEntityRepository) {
        this.layerRepository = layerRepository;
        this.ruleEntityRepository = ruleEntityRepository;
    }

    @Override
    @Transactional
    public RuleEntity close(CloseLayerRuleEntityCommand command) {
        String layerCode = LayerRuleEntityInput.requiredCode("layerCode", command.layerCode());
        String ruleEntityTypeCode = LayerRuleEntityInput.requiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = LayerRuleEntityInput.requiredCode("code", command.code());
        LocalDate startDate = LayerRuleEntityInput.requiredDate("startDate", command.startDate());
        LocalDate endDate = LayerRuleEntityInput.requiredDate("endDate", command.endDate());

        layerRepository.findByCode(layerCode).orElseThrow(() -> new LayerNotFoundException(layerCode));
        RuleEntity existing = ruleEntityRepository
                .findInLayerByBusinessKeyAndStartDate(layerCode, ruleEntityTypeCode, code, startDate)
                .orElseThrow(() -> new RuleEntityNotFoundException(layerCode, ruleEntityTypeCode, code, startDate));

        existing.close(endDate);

        if (ruleEntityRepository.existsOverlapInLayerExcludingStartDate(
                layerCode, ruleEntityTypeCode, code, existing.getStartDate(), existing.getEndDate(), existing.getStartDate())) {
            throw new RuleEntityOverlapException(layerCode, ruleEntityTypeCode, code);
        }

        return ruleEntityRepository.save(existing);
    }
}
