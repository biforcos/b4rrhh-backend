package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.domain.exception.RuleEntityIsMaintainedFromItsLayerException;
import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityTypeRepository;
import org.springframework.stereotype.Component;

/**
 * Lo que se escribe por reglamentación se escribe en su capa nacional, y sólo ahí (backend#166).
 *
 * <p>El puerto resuelve una entidad desde una reglamentación por el nivel de su tipo, así que desde
 * ESP un país sale de {@code INT}: verlo es lo que se quiere, escribirlo no. Todo caso de uso que
 * escribe {@code rule_entity} recibiendo una reglamentación pasa por aquí antes de escribir, y
 * {@code EveryRuleSystemWriteOfARuleEntityChecksItsLayerTest} lo vigila. Los de {@code /layers/…}
 * no, porque escriben por capa.</p>
 */
@Component
public class NationalLayerGuard {

    private final RuleEntityTypeRepository ruleEntityTypeRepository;
    private final LayerRepository layerRepository;

    public NationalLayerGuard(RuleEntityTypeRepository ruleEntityTypeRepository, LayerRepository layerRepository) {
        this.ruleEntityTypeRepository = ruleEntityTypeRepository;
        this.layerRepository = layerRepository;
    }

    /** Para corregir, cerrar o borrar: la entidad que se encontró desde la reglamentación es de su capa nacional. */
    public void requireDefinedInNationalLayer(String ruleSystemCode, RuleEntity existing) {
        if (existing.getLevel() == null || existing.getLevel() != RuleEntityType.NATIONAL_LEVEL) {
            throw new RuleEntityIsMaintainedFromItsLayerException(
                    ruleSystemCode, existing.getLayerCode(), existing.getRuleEntityTypeCode(), existing.getCode());
        }
    }

    /** Para dar de alta: el tipo es nacional, que es donde la reglamentación escribe. Un tipo que no existe lo dice quien llama. */
    public void requireNationalType(String ruleSystemCode, String ruleEntityTypeCode) {
        ruleEntityTypeRepository.findByCode(ruleEntityTypeCode)
                .filter(type -> type.getLevel() != RuleEntityType.NATIONAL_LEVEL)
                .ifPresent(type -> {
                    throw new RuleEntityIsMaintainedFromItsLayerException(
                            ruleSystemCode, layerAt(ruleSystemCode, type.getLevel()), ruleEntityTypeCode, null);
                });
    }

    private String layerAt(String ruleSystemCode, int level) {
        return layerRepository.findAll().stream()
                .filter(layer -> layer.level() == level && layer.mountedBy().contains(ruleSystemCode))
                .map(Layer::code)
                .findFirst()
                .orElse("level " + level);
    }
}
