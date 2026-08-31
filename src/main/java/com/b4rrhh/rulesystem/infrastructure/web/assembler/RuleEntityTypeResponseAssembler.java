package com.b4rrhh.rulesystem.infrastructure.web.assembler;

import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeGroupResponse;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El ensamblador de la capa web para los tipos y su clasificación (ADR-054 §8): de aquí
 * sale lo que el menú necesita — grupo, orden y modo de mantenimiento.
 *
 * Aquí no pasa nada por {@code RuleEntityLabelResolver} porque no hay literal de catálogo
 * que resolver: el nombre de un tipo no es una {@code rule_entity} y hoy no se traduce
 * (ADR-054 §1). El día que se traduzca, será en este ensamblador donde entre el resolutor
 * con el idioma de {@code Accept-Language}; ningún caso de uso sabrá de idiomas (ADR-052 §4).
 */
@Component
public class RuleEntityTypeResponseAssembler {

    public RuleEntityTypeResponse toResponse(RuleEntityType type, RuleEntityTypeGroup group) {
        return new RuleEntityTypeResponse(
                type.getCode(),
                type.getName(),
                type.isActive(),
                type.getLiteralClass().name(),
                type.getMaintenanceMode().name(),
                new RuleEntityTypeGroupResponse(group.code(), group.name(), group.displayOrder())
        );
    }

    /** En el orden del menú: grupo por {@code display_order} y, dentro, tipo por código. */
    public List<RuleEntityTypeResponse> toResponseList(
            List<RuleEntityType> types,
            List<RuleEntityTypeGroup> groups
    ) {
        Map<String, RuleEntityTypeGroup> groupsByCode = groups.stream()
                .collect(Collectors.toMap(RuleEntityTypeGroup::code, Function.identity()));

        return types.stream()
                .sorted(Comparator
                        .comparingInt((RuleEntityType type) -> groupsByCode.get(type.getGroupCode()).displayOrder())
                        .thenComparing(RuleEntityType::getCode))
                .map(type -> toResponse(type, groupsByCode.get(type.getGroupCode())))
                .toList();
    }
}
