package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityTypeOwnEndpointPort;
import com.b4rrhh.rulesystem.domain.exception.LayerNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityTypeIsMaintainedByItsOwnEndpointException;
import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityExtensionRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * El alta en una capa (frontend#131): la de {@link CreateRuleEntityService}, pero la capa la dice
 * la ruta en vez de resolverla una reglamentación.
 *
 * <p>Por eso aquí sí hay que comprobar el nivel: por reglamentación, la capa sale del nivel del tipo
 * y no puede no coincidir; por capa, alguien puede pedir un país en ESP. El trigger diferido de la
 * V166 lo rechazaría igual, pero al confirmar y como un 500 (ADR-077 §4).</p>
 */
@Service
public class CreateLayerRuleEntityService implements CreateLayerRuleEntityUseCase {

    private final LayerRepository layerRepository;
    private final RuleEntityRepository ruleEntityRepository;
    private final RuleEntityTypeRepository ruleEntityTypeRepository;
    private final RuleEntityExtensionRepository ruleEntityExtensionRepository;
    private final RuleEntityTypeOwnEndpointPort ruleEntityTypeOwnEndpointPort;

    public CreateLayerRuleEntityService(
            LayerRepository layerRepository,
            RuleEntityRepository ruleEntityRepository,
            RuleEntityTypeRepository ruleEntityTypeRepository,
            RuleEntityExtensionRepository ruleEntityExtensionRepository,
            RuleEntityTypeOwnEndpointPort ruleEntityTypeOwnEndpointPort
    ) {
        this.layerRepository = layerRepository;
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
        this.ruleEntityExtensionRepository = ruleEntityExtensionRepository;
        this.ruleEntityTypeOwnEndpointPort = ruleEntityTypeOwnEndpointPort;
    }

    @Override
    @Transactional
    public RuleEntity create(CreateLayerRuleEntityCommand command) {
        String layerCode = LayerRuleEntityInput.requiredCode("layerCode", command.layerCode());
        String ruleEntityTypeCode = LayerRuleEntityInput.requiredCode("ruleEntityTypeCode", command.ruleEntityTypeCode());
        String code = LayerRuleEntityInput.requiredCode("code", command.code());
        String name = LayerRuleEntityInput.requiredName(command.name());
        String description = LayerRuleEntityInput.description(command.description());
        LocalDate startDate = LayerRuleEntityInput.requiredDate("startDate", command.startDate());
        LocalDate endDate = command.endDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }

        Layer layer = layerRepository.findByCode(layerCode).orElseThrow(() -> new LayerNotFoundException(layerCode));
        RuleEntityType type = ruleEntityTypeRepository.findByCode(ruleEntityTypeCode).orElseThrow(
                () -> new IllegalArgumentException("Rule entity type not found with code: " + ruleEntityTypeCode)
        );
        if (type.getLevel() != layer.level()) {
            throw new IllegalArgumentException(
                    "Rule entity type " + ruleEntityTypeCode + " lives at level " + type.getLevel()
                            + "; layer " + layerCode + " is of level " + layer.level()
            );
        }

        rejectIfTheTypeIsMaintainedByItsOwnEndpoint(ruleEntityTypeCode);

        ruleEntityRepository.findInLayerByBusinessKey(layerCode, ruleEntityTypeCode, code).ifPresent(existing -> {
            throw new IllegalArgumentException(
                    "Rule entity already exists with business key: " + layerCode + "/" + ruleEntityTypeCode + "/" + code
            );
        });

        return ruleEntityRepository.save(new RuleEntity(
                null,
                null,
                layerCode,
                layer.level(),
                ruleEntityTypeCode,
                code,
                name,
                description,
                true,
                startDate,
                endDate,
                null,
                null
        ));
    }

    /** La misma pregunta que {@link CreateRuleEntityService} le hace al metamodelo (backend#88). */
    private void rejectIfTheTypeIsMaintainedByItsOwnEndpoint(String ruleEntityTypeCode) {
        List<RuleEntityExtension> required =
                ruleEntityExtensionRepository.findRequiredByRuleEntityTypeCode(ruleEntityTypeCode);
        if (required.isEmpty()) {
            return;
        }

        throw new RuleEntityTypeIsMaintainedByItsOwnEndpointException(
                ruleEntityTypeCode,
                required.stream().map(RuleEntityExtension::tableName).toList(),
                ruleEntityTypeOwnEndpointPort.findApiCollectionPath(ruleEntityTypeCode).orElse(null)
        );
    }
}
