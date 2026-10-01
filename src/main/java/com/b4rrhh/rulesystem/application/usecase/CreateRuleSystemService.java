package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleSystemLayerPort;
import com.b4rrhh.rulesystem.domain.model.DefaultRuleSystemLayers;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateRuleSystemService implements CreateRuleSystemUseCase {

    private final RuleSystemRepository ruleSystemRepository;
    private final RuleSystemLayerPort ruleSystemLayerPort;

    public CreateRuleSystemService(RuleSystemRepository ruleSystemRepository,
                                   RuleSystemLayerPort ruleSystemLayerPort) {
        this.ruleSystemRepository = ruleSystemRepository;
        this.ruleSystemLayerPort = ruleSystemLayerPort;
    }

    // Transaccional porque el esquema comprueba al confirmar que la reglamentacion monta sus
    // cinco capas (backend#156): la reglamentacion y sus capas entran juntas o no entra ninguna.
    @Override
    @Transactional
    public RuleSystem create(CreateRuleSystemCommand command) {
        String normalizedCode = command.code().trim().toUpperCase();
        String normalizedCountryCode = command.countryCode().trim().toUpperCase();

        ruleSystemRepository.findByCode(normalizedCode).ifPresent(existing -> {
            throw new IllegalArgumentException("Rule system already exists with code: " + normalizedCode);
        });
        for (String layerCode : DefaultRuleSystemLayers.ownLayerCodes(normalizedCode)) {
            if (ruleSystemLayerPort.layerExists(layerCode)) {
                throw new IllegalArgumentException("Layer already exists with code: " + layerCode);
            }
        }

        RuleSystem ruleSystem = new RuleSystem(
                null,
                normalizedCode,
                command.name().trim(),
                normalizedCountryCode,
                true,
                null,
                null
        );

        RuleSystem saved = ruleSystemRepository.save(ruleSystem);
        ruleSystemLayerPort.assembleDefaultLayers(saved.getCode(), saved.getName());
        return saved;
    }
}