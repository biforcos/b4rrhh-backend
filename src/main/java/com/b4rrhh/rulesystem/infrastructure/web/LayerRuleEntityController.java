package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.CreateLayerRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.CreateLayerRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.ListLayerRuleEntitiesQuery;
import com.b4rrhh.rulesystem.application.usecase.ListLayerRuleEntitiesUseCase;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.infrastructure.web.assembler.RuleEntityResponseAssembler;
import com.b4rrhh.rulesystem.infrastructure.web.dto.CreateLayerRuleEntityRequest;
import com.b4rrhh.rulesystem.infrastructure.web.dto.LayerRuleEntityResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Las entidades de una capa, mantenidas desde ella (frontend#131). Es donde se mantiene una de COM o
 * de INT: desde una reglamentación sólo se ve.
 */
@RestController
@RequestMapping("/layers/{layerCode}/rule-entities")
public class LayerRuleEntityController {

    private final CreateLayerRuleEntityUseCase createLayerRuleEntityUseCase;
    private final ListLayerRuleEntitiesUseCase listLayerRuleEntitiesUseCase;
    private final RuleEntityResponseAssembler assembler;

    public LayerRuleEntityController(
            CreateLayerRuleEntityUseCase createLayerRuleEntityUseCase,
            ListLayerRuleEntitiesUseCase listLayerRuleEntitiesUseCase,
            RuleEntityResponseAssembler assembler
    ) {
        this.createLayerRuleEntityUseCase = createLayerRuleEntityUseCase;
        this.listLayerRuleEntitiesUseCase = listLayerRuleEntitiesUseCase;
        this.assembler = assembler;
    }

    @PostMapping
    public ResponseEntity<LayerRuleEntityResponse> create(
            @PathVariable String layerCode,
            @RequestBody CreateLayerRuleEntityRequest request,
            ResponseLanguage language
    ) {
        RuleEntity created = createLayerRuleEntityUseCase.create(new CreateLayerRuleEntityCommand(
                layerCode,
                request.ruleEntityTypeCode(),
                request.code(),
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toLayerResponse(created, language));
    }

    @GetMapping
    public ResponseEntity<List<LayerRuleEntityResponse>> list(
            @PathVariable String layerCode,
            @RequestParam(required = false) String ruleEntityTypeCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) LocalDate referenceDate,
            ResponseLanguage language
    ) {
        List<RuleEntity> entities = listLayerRuleEntitiesUseCase.list(
                new ListLayerRuleEntitiesQuery(layerCode, ruleEntityTypeCode, code, active, referenceDate));

        return ResponseEntity.ok(assembler.toLayerResponseList(entities, language));
    }
}
