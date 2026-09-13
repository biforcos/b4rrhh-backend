package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityTypeOwnEndpointPort;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityTypeIsMaintainedByItsOwnEndpointException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.port.RuleEntityExtensionRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * El alta generica de raices, que es la puerta de los maestros simples: una fila en
 * {@code rule_entity} y nada mas (ADR-053 §4).
 *
 * <p>No es la puerta de los tipos con extensiones obligatorias. Hasta el backend#88 lo era, y
 * dejaba raices sin su extension {@code required}: desde el #34 y el #35 eso las vuelve
 * ilegibles —{@code RequiredExtensionMissingException}— y tumba la lista de todos sus hermanos,
 * no solo su propia ficha. Lo que se retiro es la forma de crear la fila rota, no el aviso.</p>
 */
@Service
public class CreateRuleEntityService implements CreateRuleEntityUseCase {

    private final RuleEntityRepository ruleEntityRepository;
    private final RuleSystemRepository ruleSystemRepository;
    private final RuleEntityTypeRepository ruleEntityTypeRepository;
    private final RuleEntityExtensionRepository ruleEntityExtensionRepository;
    private final RuleEntityTypeOwnEndpointPort ruleEntityTypeOwnEndpointPort;

    public CreateRuleEntityService(
            RuleEntityRepository ruleEntityRepository,
            RuleSystemRepository ruleSystemRepository,
            RuleEntityTypeRepository ruleEntityTypeRepository,
            RuleEntityExtensionRepository ruleEntityExtensionRepository,
            RuleEntityTypeOwnEndpointPort ruleEntityTypeOwnEndpointPort
    ) {
        this.ruleEntityRepository = ruleEntityRepository;
        this.ruleSystemRepository = ruleSystemRepository;
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
        this.ruleEntityExtensionRepository = ruleEntityExtensionRepository;
        this.ruleEntityTypeOwnEndpointPort = ruleEntityTypeOwnEndpointPort;
    }

    @Override
    public RuleEntity create(CreateRuleEntityCommand command) {
        String normalizedRuleSystemCode = command.ruleSystemCode().trim().toUpperCase();
        String normalizedRuleEntityTypeCode = command.ruleEntityTypeCode().trim().toUpperCase();
        String normalizedCode = command.code().trim().toUpperCase();

        ruleSystemRepository.findByCode(normalizedRuleSystemCode).orElseThrow(
                () -> new IllegalArgumentException("Rule system not found with code: " + normalizedRuleSystemCode)
        );

        ruleEntityTypeRepository.findByCode(normalizedRuleEntityTypeCode).orElseThrow(
                () -> new IllegalArgumentException("Rule entity type not found with code: " + normalizedRuleEntityTypeCode)
        );

        rejectIfTheTypeIsMaintainedByItsOwnEndpoint(normalizedRuleEntityTypeCode);

        ruleEntityRepository.findByBusinessKey(normalizedRuleSystemCode, normalizedRuleEntityTypeCode, normalizedCode)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Rule entity already exists with business key: "
                                    + normalizedRuleSystemCode + "/" + normalizedRuleEntityTypeCode + "/" + normalizedCode
                    );
                });

        RuleEntity ruleEntity = new RuleEntity(
                null,
                normalizedRuleSystemCode,
                normalizedRuleEntityTypeCode,
                normalizedCode,
                command.name().trim(),
                normalizeDescription(command.description()),
                true,
                command.startDate(),
                command.endDate(),
                null,
                null
        );

        return ruleEntityRepository.save(ruleEntity);
    }

    /**
     * La pregunta se le hace al metamodelo, no a una lista de tipos: «¿este tipo declara
     * alguna extension {@code required}?». Un tipo nuevo queda cubierto el dia en que la
     * declara, sin tocar este codigo — que es justo lo que separa esta forma de la de escribir
     * aqui los tipos que tienen pantalla propia (backend#88).
     */
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

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String normalized = description.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }
}
