package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateRuleEntityTypeService implements CreateRuleEntityTypeUseCase {

    private final RuleEntityTypeRepository ruleEntityTypeRepository;

    public CreateRuleEntityTypeService(RuleEntityTypeRepository ruleEntityTypeRepository) {
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
    }

    @Override
    public RuleEntityType create(CreateRuleEntityTypeCommand command) {
        String normalizedCode = command.code().trim().toUpperCase();

        // Añadir un tipo cuesta tres decisiones y ninguna tiene defecto (ADR-054 §6):
        // aquí se rechazan pronto y con nombre, en vez de dejar que reviente el not null.
        if (command.literalClass() == null || command.maintenanceMode() == null
                || command.groupCode() == null || command.groupCode().isBlank()) {
            throw new IllegalArgumentException(
                    "A rule entity type requires literalClass, maintenanceMode and groupCode (ADR-054)");
        }

        ruleEntityTypeRepository.findByCode(normalizedCode).ifPresent(existing -> {
            throw new IllegalArgumentException("Rule entity type already exists with code: " + normalizedCode);
        });

        RuleEntityType ruleEntityType = new RuleEntityType(
                null,
                normalizedCode,
                command.name().trim(),
                command.literalClass(),
                command.maintenanceMode(),
                command.groupCode().trim().toUpperCase(),
                true,
                null,
                null
        );

        return ruleEntityTypeRepository.save(ruleEntityType);
    }
}
