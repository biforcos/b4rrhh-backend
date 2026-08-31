package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.CreateRuleEntityTypeCommand;
import com.b4rrhh.rulesystem.application.usecase.CreateRuleEntityTypeUseCase;
import com.b4rrhh.rulesystem.application.usecase.GetRuleEntityTypeByCodeUseCase;
import com.b4rrhh.rulesystem.application.usecase.ListRuleEntityTypesUseCase;
import com.b4rrhh.rulesystem.domain.model.LiteralClass;
import com.b4rrhh.rulesystem.domain.model.MaintenanceMode;
import com.b4rrhh.rulesystem.domain.model.RuleEntityType;
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

    public RuleEntityTypeController(
            CreateRuleEntityTypeUseCase createRuleEntityTypeUseCase,
            GetRuleEntityTypeByCodeUseCase getRuleEntityTypeByCodeUseCase,
            ListRuleEntityTypesUseCase listRuleEntityTypesUseCase
    ) {
        this.createRuleEntityTypeUseCase = createRuleEntityTypeUseCase;
        this.getRuleEntityTypeByCodeUseCase = getRuleEntityTypeByCodeUseCase;
        this.listRuleEntityTypesUseCase = listRuleEntityTypesUseCase;
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
        List<RuleEntityTypeResponse> response = listRuleEntityTypesUseCase.listAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    /** Nulo se queda nulo: el caso de uso es quien rechaza la decisión ausente (ADR-054 §6). */
    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        return value == null || value.isBlank() ? null : Enum.valueOf(type, value.trim().toUpperCase());
    }

    private RuleEntityTypeResponse toResponse(RuleEntityType ruleEntityType) {
        return new RuleEntityTypeResponse(
                ruleEntityType.getCode(),
                ruleEntityType.getName(),
                ruleEntityType.isActive()
        );
    }
}
