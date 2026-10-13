package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.CloseLayerRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.CloseLayerRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.CorrectLayerRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.CorrectLayerRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.DeleteLayerRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.DeleteLayerRuleEntityUseCase;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.infrastructure.web.assembler.RuleEntityResponseAssembler;
import com.b4rrhh.rulesystem.infrastructure.web.dto.CloseRuleEntityRequest;
import com.b4rrhh.rulesystem.infrastructure.web.dto.CorrectRuleEntityRequest;
import com.b4rrhh.rulesystem.infrastructure.web.dto.LayerRuleEntityResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/layers/{layerCode}/rule-entities/{ruleEntityTypeCode}/{code}/{startDate}")
public class LayerRuleEntityBusinessKeyController {

    private final CorrectLayerRuleEntityUseCase correctLayerRuleEntityUseCase;
    private final CloseLayerRuleEntityUseCase closeLayerRuleEntityUseCase;
    private final DeleteLayerRuleEntityUseCase deleteLayerRuleEntityUseCase;
    private final RuleEntityResponseAssembler assembler;

    public LayerRuleEntityBusinessKeyController(
            CorrectLayerRuleEntityUseCase correctLayerRuleEntityUseCase,
            CloseLayerRuleEntityUseCase closeLayerRuleEntityUseCase,
            DeleteLayerRuleEntityUseCase deleteLayerRuleEntityUseCase,
            RuleEntityResponseAssembler assembler
    ) {
        this.correctLayerRuleEntityUseCase = correctLayerRuleEntityUseCase;
        this.closeLayerRuleEntityUseCase = closeLayerRuleEntityUseCase;
        this.deleteLayerRuleEntityUseCase = deleteLayerRuleEntityUseCase;
        this.assembler = assembler;
    }

    @PutMapping
    public ResponseEntity<LayerRuleEntityResponse> correct(
            @PathVariable String layerCode,
            @PathVariable String ruleEntityTypeCode,
            @PathVariable String code,
            @PathVariable LocalDate startDate,
            @RequestBody CorrectRuleEntityRequest request,
            ResponseLanguage language
    ) {
        RuleEntity corrected = correctLayerRuleEntityUseCase.correct(new CorrectLayerRuleEntityCommand(
                layerCode,
                ruleEntityTypeCode,
                code,
                startDate,
                request.name(),
                request.description(),
                request.endDate()
        ));

        return ResponseEntity.ok(assembler.toLayerResponse(corrected, language));
    }

    @PostMapping("/close")
    public ResponseEntity<LayerRuleEntityResponse> close(
            @PathVariable String layerCode,
            @PathVariable String ruleEntityTypeCode,
            @PathVariable String code,
            @PathVariable LocalDate startDate,
            @RequestBody CloseRuleEntityRequest request,
            ResponseLanguage language
    ) {
        RuleEntity closed = closeLayerRuleEntityUseCase.close(new CloseLayerRuleEntityCommand(
                layerCode,
                ruleEntityTypeCode,
                code,
                startDate,
                request.endDate()
        ));

        return ResponseEntity.ok(assembler.toLayerResponse(closed, language));
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @PathVariable String layerCode,
            @PathVariable String ruleEntityTypeCode,
            @PathVariable String code,
            @PathVariable LocalDate startDate
    ) {
        deleteLayerRuleEntityUseCase.delete(new DeleteLayerRuleEntityCommand(layerCode, ruleEntityTypeCode, code, startDate));

        return ResponseEntity.noContent().build();
    }
}
