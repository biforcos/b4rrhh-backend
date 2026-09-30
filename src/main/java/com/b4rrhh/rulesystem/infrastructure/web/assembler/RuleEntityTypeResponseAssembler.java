package com.b4rrhh.rulesystem.infrastructure.web.assembler;

import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeExtensionResponse;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeGroupResponse;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El ensamblador de la capa web para los tipos y su clasificación (ADR-054 §8): de aquí
 * sale lo que el menú necesita — grupo, orden, modo de mantenimiento y las extensiones
 * declaradas (ADR-053 §7, frontend#33), de las que el menú deriva quién tiene pantalla
 * propia y quién vive en Catálogos.
 *
 * El nombre del tipo sale dos veces (backend#152): {@code name}, el almacenado, que es el
 * que se edita, y {@code label}, el del idioma de {@code Accept-Language}, que resuelve
 * {@code RuleEntityLabelResolver} desde la tabla gemela de traducciones de los tipos. Aquí
 * entra el idioma y de aquí no baja: ningún caso de uso sabe de idiomas (ADR-052 §4).
 */
@Component
public class RuleEntityTypeResponseAssembler {

    private final RuleEntityLabelResolver labelResolver;

    public RuleEntityTypeResponseAssembler(RuleEntityLabelResolver labelResolver) {
        this.labelResolver = labelResolver;
    }

    public RuleEntityTypeResponse toResponse(
            RuleEntityType type,
            RuleEntityTypeGroup group,
            List<RuleEntityExtension> extensions,
            ResponseLanguage language
    ) {
        return toResponse(type, group, extensions,
                labelResolver.resolveTypeLabels(List.of(type), language.code()).get(type.getCode()));
    }

    private RuleEntityTypeResponse toResponse(
            RuleEntityType type,
            RuleEntityTypeGroup group,
            List<RuleEntityExtension> extensions,
            String label
    ) {
        return new RuleEntityTypeResponse(
                type.getCode(),
                type.getName(),
                label,
                type.isActive(),
                type.getLiteralClass().name(),
                type.getMaintenanceMode().name(),
                new RuleEntityTypeGroupResponse(group.code(), group.name(), group.displayOrder()),
                toExtensionResponses(extensions)
        );
    }

    /** En el orden del menú: grupo por {@code display_order} y, dentro, tipo por código. */
    public List<RuleEntityTypeResponse> toResponseList(
            List<RuleEntityType> types,
            List<RuleEntityTypeGroup> groups,
            List<RuleEntityExtension> extensions,
            ResponseLanguage language
    ) {
        Map<String, String> labels = labelResolver.resolveTypeLabels(types, language.code());
        Map<String, RuleEntityTypeGroup> groupsByCode = groups.stream()
                .collect(Collectors.toMap(RuleEntityTypeGroup::code, Function.identity()));
        Map<String, List<RuleEntityExtension>> extensionsByType = extensions.stream()
                .collect(Collectors.groupingBy(RuleEntityExtension::ruleEntityTypeCode));

        return types.stream()
                .sorted(Comparator
                        .comparingInt((RuleEntityType type) -> groupsByCode.get(type.getGroupCode()).displayOrder())
                        .thenComparing(RuleEntityType::getCode))
                .map(type -> toResponse(
                        type,
                        groupsByCode.get(type.getGroupCode()),
                        extensionsByType.getOrDefault(type.getCode(), List.of()),
                        labels.get(type.getCode())))
                .toList();
    }

    /** Por código de extensión: estable, y ninguna extensión es más importante que otra. */
    private static List<RuleEntityTypeExtensionResponse> toExtensionResponses(
            List<RuleEntityExtension> extensions
    ) {
        return extensions.stream()
                .sorted(Comparator.comparing(RuleEntityExtension::extensionCode))
                .map(extension -> new RuleEntityTypeExtensionResponse(
                        extension.extensionCode(),
                        extension.cardinality(),
                        extension.required()
                ))
                .toList();
    }
}
