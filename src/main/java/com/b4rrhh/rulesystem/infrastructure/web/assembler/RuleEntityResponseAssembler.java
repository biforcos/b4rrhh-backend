package com.b4rrhh.rulesystem.infrastructure.web.assembler;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * La respuesta de una entidad de catálogo, con su literal dos veces (backend#152):
 * {@code name}, el almacenado, que es el que se edita en la pantalla de mantenimiento, y
 * {@code label}, el del idioma de {@code Accept-Language}, resuelto por
 * {@link RuleEntityLabelResolver} como cualquier otra etiqueta de catálogo (ADR-052 §3).
 *
 * Antes cada controlador armaba la suya y ninguna pasaba por el resolutor: la lista de
 * Catálogos salía en inglés mientras los desplegables de la ficha, que sí pasan, salían en
 * castellano.
 */
@Component
public class RuleEntityResponseAssembler {

    private final RuleEntityLabelResolver labelResolver;

    public RuleEntityResponseAssembler(RuleEntityLabelResolver labelResolver) {
        this.labelResolver = labelResolver;
    }

    public RuleEntityResponse toResponse(RuleEntity entity, ResponseLanguage language) {
        return toResponseList(List.of(entity), language).get(0);
    }

    public List<RuleEntityResponse> toResponseList(List<RuleEntity> entities, ResponseLanguage language) {
        Map<Long, String> labels = labelResolver.resolveLabels(entities, language.code());
        return entities.stream()
                .map(entity -> toResponse(entity, labels.get(entity.getId())))
                .toList();
    }

    private static RuleEntityResponse toResponse(RuleEntity entity, String label) {
        return new RuleEntityResponse(
                entity.getRuleSystemCode(),
                entity.getLayerCode(),
                entity.getLevel(),
                entity.getRuleEntityTypeCode(),
                entity.getCode(),
                entity.getName(),
                label,
                entity.getDescription(),
                entity.isActive(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
