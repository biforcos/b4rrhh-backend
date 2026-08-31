package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.CreateRuleEntityTypeCommand;
import com.b4rrhh.rulesystem.application.usecase.CreateRuleEntityTypeUseCase;
import com.b4rrhh.rulesystem.application.usecase.GetRuleEntityTypeByCodeUseCase;
import com.b4rrhh.rulesystem.application.usecase.ListRuleEntityExtensionsUseCase;
import com.b4rrhh.rulesystem.application.usecase.ListRuleEntityTypeGroupsUseCase;
import com.b4rrhh.rulesystem.application.usecase.ListRuleEntityTypesUseCase;
import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.RuleEntityExtension;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
import com.b4rrhh.rulesystem.domain.model.RuleEntityTypeGroup;
import com.b4rrhh.rulesystem.infrastructure.web.assembler.RuleEntityTypeResponseAssembler;
import com.b4rrhh.rulesystem.infrastructure.web.dto.CreateRuleEntityTypeRequest;
import com.b4rrhh.rulesystem.infrastructure.web.dto.RuleEntityTypeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rule-entity-types")
public class RuleEntityTypeController {

    private final CreateRuleEntityTypeUseCase createRuleEntityTypeUseCase;
    private final GetRuleEntityTypeByCodeUseCase getRuleEntityTypeByCodeUseCase;
    private final ListRuleEntityTypesUseCase listRuleEntityTypesUseCase;
    private final ListRuleEntityTypeGroupsUseCase listRuleEntityTypeGroupsUseCase;
    private final ListRuleEntityExtensionsUseCase listRuleEntityExtensionsUseCase;
    private final RuleEntityTypeResponseAssembler assembler;

    public RuleEntityTypeController(
            CreateRuleEntityTypeUseCase createRuleEntityTypeUseCase,
            GetRuleEntityTypeByCodeUseCase getRuleEntityTypeByCodeUseCase,
            ListRuleEntityTypesUseCase listRuleEntityTypesUseCase,
            ListRuleEntityTypeGroupsUseCase listRuleEntityTypeGroupsUseCase,
            ListRuleEntityExtensionsUseCase listRuleEntityExtensionsUseCase,
            RuleEntityTypeResponseAssembler assembler
    ) {
        this.createRuleEntityTypeUseCase = createRuleEntityTypeUseCase;
        this.getRuleEntityTypeByCodeUseCase = getRuleEntityTypeByCodeUseCase;
        this.listRuleEntityTypesUseCase = listRuleEntityTypesUseCase;
        this.listRuleEntityTypeGroupsUseCase = listRuleEntityTypeGroupsUseCase;
        this.listRuleEntityExtensionsUseCase = listRuleEntityExtensionsUseCase;
        this.assembler = assembler;
    }

    @PostMapping
    public ResponseEntity<RuleEntityTypeResponse> create(@RequestBody CreateRuleEntityTypeRequest request) {
        RuleEntityType created = createRuleEntityTypeUseCase.create(
                new CreateRuleEntityTypeCommand(
                        request.code(),
                        request.name(),
                        parse(LiteralClass.class, request.literalClass()),
                        parse(MaintenanceMode.class, request.maintenanceMode()),
                        request.groupCode()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping("/{ruleEntityTypeCode}")
    public ResponseEntity<RuleEntityTypeResponse> getByCode(@PathVariable String ruleEntityTypeCode) {
        return getRuleEntityTypeByCodeUseCase.getByCode(ruleEntityTypeCode)
                .map(ruleEntityType -> ResponseEntity.ok(toResponse(ruleEntityType)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RuleEntityTypeResponse>> list() {
        List<RuleEntityTypeResponse> response = assembler.toResponseList(
                listRuleEntityTypesUseCase.listAll(),
                listRuleEntityTypeGroupsUseCase.listAll(),
                listRuleEntityExtensionsUseCase.listAll()
        );

        return ResponseEntity.ok(response);
    }

    /** Nulo se queda nulo: el caso de uso es quien rechaza la decisión ausente (ADR-054 §6). */
    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value.trim().toUpperCase());
    }

    private RuleEntityTypeResponse toResponse(RuleEntityType ruleEntityType) {
        RuleEntityTypeGroup group = listRuleEntityTypeGroupsUseCase.listAll().stream()
                .filter(candidate -> candidate.code().equals(ruleEntityType.getGroupCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "El grupo " + ruleEntityType.getGroupCode() + " no está sembrado"));

        List<RuleEntityExtension> extensions = listRuleEntityExtensionsUseCase.listAll().stream()
                .filter(extension -> extension.ruleEntityTypeCode().equals(ruleEntityType.getCode()))
                .toList();

        return assembler.toResponse(ruleEntityType, group, extensions);
    }
}
