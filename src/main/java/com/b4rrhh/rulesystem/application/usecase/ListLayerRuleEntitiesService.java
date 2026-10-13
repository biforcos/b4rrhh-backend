package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.LayerNotFoundException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListLayerRuleEntitiesService implements ListLayerRuleEntitiesUseCase {

    private final LayerRepository layerRepository;
    private final RuleEntityRepository ruleEntityRepository;

    public ListLayerRuleEntitiesService(LayerRepository layerRepository, RuleEntityRepository ruleEntityRepository) {
        this.layerRepository = layerRepository;
        this.ruleEntityRepository = ruleEntityRepository;
    }

    @Override
    public List<RuleEntity> list(ListLayerRuleEntitiesQuery query) {
        String layerCode = LayerRuleEntityInput.requiredCode("layerCode", query.layerCode());
        layerRepository.findByCode(layerCode).orElseThrow(() -> new LayerNotFoundException(layerCode));

        return ruleEntityRepository.findInLayer(
                layerCode,
                LayerRuleEntityInput.optionalCode(query.ruleEntityTypeCode()),
                LayerRuleEntityInput.optionalCode(query.code()),
                query.active(),
                query.referenceDate()
        );
    }
}
