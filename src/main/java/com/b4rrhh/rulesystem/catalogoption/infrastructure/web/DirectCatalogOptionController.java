package com.b4rrhh.rulesystem.catalogoption.infrastructure.web;

import com.b4rrhh.rulesystem.catalogoption.application.query.GetDirectCatalogOptionsQuery;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.DirectCatalogOptionsResult;
import com.b4rrhh.rulesystem.catalogoption.application.usecase.GetDirectCatalogOptionsUseCase;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.catalogoption.infrastructure.web.dto.DirectCatalogOptionResponse;
import com.b4rrhh.rulesystem.catalogoption.infrastructure.web.dto.DirectCatalogOptionsResponse;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/catalog-options")
public class DirectCatalogOptionController {

    private final GetDirectCatalogOptionsUseCase getDirectCatalogOptionsUseCase;

    public DirectCatalogOptionController(GetDirectCatalogOptionsUseCase getDirectCatalogOptionsUseCase) {
        this.getDirectCatalogOptionsUseCase = getDirectCatalogOptionsUseCase;
    }

    @GetMapping("/direct")
    public ResponseEntity<DirectCatalogOptionsResponse> getDirectOptions(
            @RequestParam String ruleSystemCode,
            @RequestParam String ruleEntityTypeCode,
            @RequestParam(required = false) LocalDate referenceDate,
            @RequestParam(required = false) String q,
            ResponseLanguage language
    ) {
        DirectCatalogOptionsResult result = getDirectCatalogOptionsUseCase.get(
                new GetDirectCatalogOptionsQuery(ruleSystemCode, ruleEntityTypeCode, referenceDate, q, language.code())
        );

        List<DirectCatalogOptionResponse> items = result.items()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new DirectCatalogOptionsResponse(
                result.ruleSystemCode(),
                result.ruleEntityTypeCode(),
                result.referenceDate(),
                items
        ));
    }

    private DirectCatalogOptionResponse toResponse(DirectCatalogOption item) {
        return new DirectCatalogOptionResponse(
                item.code(),
                item.name(),
                item.active(),
                item.startDate(),
                item.endDate()
        );
    }
}
