package com.b4rrhh.employee.cost_center.infrastructure.web;

import com.b4rrhh.employee.cost_center.application.usecase.CloseCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CloseCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionItem;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionReadModel;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionQuery;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryQuery;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.ReplaceCostCenterDistributionFromDateCommand;
import com.b4rrhh.employee.cost_center.application.usecase.ReplaceCostCenterDistributionFromDateUseCase;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.infrastructure.web.assembler.CostCenterResponseAssembler;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CloseCostCenterDistributionRequest;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterCurrentDistributionResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionHistoryResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionWindowResponse;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.CreateCostCenterDistributionRequest;
import com.b4rrhh.employee.cost_center.infrastructure.web.dto.ReplaceCostCenterDistributionFromDateRequest;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers")
public class CostCenterBusinessKeyController {

    private final CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;
    private final GetCurrentCostCenterDistributionUseCase getCurrentCostCenterDistributionUseCase;
    private final ListCostCenterDistributionHistoryUseCase listCostCenterDistributionHistoryUseCase;
    private final ReplaceCostCenterDistributionFromDateUseCase replaceCostCenterDistributionFromDateUseCase;
    private final CloseCostCenterDistributionUseCase closeCostCenterDistributionUseCase;
    private final CostCenterResponseAssembler costCenterResponseAssembler;

    public CostCenterBusinessKeyController(
            CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase,
            GetCurrentCostCenterDistributionUseCase getCurrentCostCenterDistributionUseCase,
            ListCostCenterDistributionHistoryUseCase listCostCenterDistributionHistoryUseCase,
            ReplaceCostCenterDistributionFromDateUseCase replaceCostCenterDistributionFromDateUseCase,
            CloseCostCenterDistributionUseCase closeCostCenterDistributionUseCase,
            CostCenterResponseAssembler costCenterResponseAssembler
    ) {
        this.createCostCenterDistributionUseCase = createCostCenterDistributionUseCase;
        this.getCurrentCostCenterDistributionUseCase = getCurrentCostCenterDistributionUseCase;
        this.listCostCenterDistributionHistoryUseCase = listCostCenterDistributionHistoryUseCase;
        this.replaceCostCenterDistributionFromDateUseCase = replaceCostCenterDistributionFromDateUseCase;
        this.closeCostCenterDistributionUseCase = closeCostCenterDistributionUseCase;
        this.costCenterResponseAssembler = costCenterResponseAssembler;
    }

    @PostMapping("/distributions")
    public ResponseEntity<CostCenterDistributionWindowResponse> createDistribution(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateCostCenterDistributionRequest request
    ) {
        List<CostCenterDistributionItem> items = toCommandItems(request.items());

        CostCenterDistributionWindow created = createCostCenterDistributionUseCase.create(
                new CreateCostCenterDistributionCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.startDate(),
                        items
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(costCenterResponseAssembler.toWindowResponse(created));
    }

    @GetMapping
    public ResponseEntity<CostCenterDistributionHistoryResponse> listHistory(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        CostCenterDistributionReadModel.History history = listCostCenterDistributionHistoryUseCase.listHistory(
                new ListCostCenterDistributionHistoryQuery(ruleSystemCode, employeeTypeCode, employeeNumber)
        );

        return ResponseEntity.ok(costCenterResponseAssembler.toHistoryResponse(history, language));
    }

    @GetMapping("/current")
    public ResponseEntity<CostCenterCurrentDistributionResponse> getCurrent(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        CostCenterDistributionReadModel.CurrentDistribution current = getCurrentCostCenterDistributionUseCase.getCurrent(
                new GetCurrentCostCenterDistributionQuery(ruleSystemCode, employeeTypeCode, employeeNumber)
        );

        return ResponseEntity.ok(costCenterResponseAssembler.toCurrentResponse(current, language));
    }

    @PostMapping("/replace-from-date")
    public ResponseEntity<CostCenterDistributionWindowResponse> replaceFromDate(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody ReplaceCostCenterDistributionFromDateRequest request
    ) {
        List<CostCenterDistributionItem> items = toCommandItems(request.items());

        CostCenterDistributionWindow replaced = replaceCostCenterDistributionFromDateUseCase.replaceFromDate(
                new ReplaceCostCenterDistributionFromDateCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.effectiveDate(),
                        items
                )
        );

        return ResponseEntity.ok(costCenterResponseAssembler.toWindowResponse(replaced));
    }

    @PostMapping("/distributions/{startDate}/close")
    public ResponseEntity<CostCenterDistributionWindowResponse> closeDistribution(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestBody CloseCostCenterDistributionRequest request
    ) {
        CostCenterDistributionWindow closed = closeCostCenterDistributionUseCase.close(
                new CloseCostCenterDistributionCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        startDate,
                        request.endDate()
                )
        );

        return ResponseEntity.ok(costCenterResponseAssembler.toWindowResponse(closed));
    }

    // ---- mapping helpers ----

    private List<CostCenterDistributionItem> toCommandItems(
            List<com.b4rrhh.employee.cost_center.infrastructure.web.dto.CostCenterDistributionItemRequest> requestItems
    ) {
        if (requestItems == null) {
            return List.of();
        }
        return requestItems.stream()
                .map(i -> new CostCenterDistributionItem(i.costCenterCode(), i.allocationPercentage()))
                .toList();
    }

}
