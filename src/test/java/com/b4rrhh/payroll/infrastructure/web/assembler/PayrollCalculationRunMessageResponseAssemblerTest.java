package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunMessagesResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollCalculationRunMessageResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @InjectMocks
    private PayrollCalculationRunMessageResponseAssembler assembler;

    @Test
    void resolvesTheLiteralOfTheCodeFromTheCatalog() {
        when(ruleEntityLabelResolver.resolveName(
                "ESP", "PAYROLL_RUN_MESSAGE", "UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT", "es-ES"))
                .thenReturn(Optional.of("Sin calcular: faltaban datos"));

        PayrollCalculationRunMessagesResponse response = assembler.toResponse(
                7L,
                "ESP",
                List.of(message("UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT", "WARNING", "ESP")),
                new ResponseLanguage("es-ES")
        );

        assertThat(response.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.messageCode()).isEqualTo("UNIT_ELIGIBLE_REAL_SKIPPED_MISSING_INPUT");
                    assertThat(item.messageCodeName()).isEqualTo("Sin calcular: faltaban datos");
                });
    }

    // Sin catalogo detras la pantalla pinta el codigo desnudo, que es feo y honesto
    // (backend#81). Lo que no puede hacer el backend es fabricarse un literal.
    @Test
    void leavesTheLiteralEmptyWhenTheCodeIsNotSeeded() {
        when(ruleEntityLabelResolver.resolveName("ESP", "PAYROLL_RUN_MESSAGE", "UNIT_INVENTADO", null))
                .thenReturn(Optional.empty());

        PayrollCalculationRunMessagesResponse response = assembler.toResponse(
                7L,
                "ESP",
                List.of(message("UNIT_INVENTADO", "WARNING", "ESP")),
                ResponseLanguage.base()
        );

        assertThat(response.items()).singleElement()
                .satisfies(item -> assertThat(item.messageCodeName()).isNull());
    }

    // Un mensaje de la ejecucion entera —la cola llena, el reinicio— no nombra unidad
    // y por eso no lleva reglamentacion. Su codigo se llama igual que los demas, asi
    // que se busca con la de la ejecucion.
    @Test
    void fallsBackToTheRunRuleSystemForAMessageThatNamesNoUnit() {
        when(ruleEntityLabelResolver.resolveName("ESP", "PAYROLL_RUN_MESSAGE", "LAUNCH_REJECTED", null))
                .thenReturn(Optional.of("Launch rejected: the queue was full"));

        PayrollCalculationRunMessagesResponse response = assembler.toResponse(
                7L,
                "ESP",
                List.of(message("LAUNCH_REJECTED", "ERROR", null)),
                ResponseLanguage.base()
        );

        assertThat(response.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.ruleSystemCode()).isNull();
                    assertThat(item.messageCodeName()).isEqualTo("Launch rejected: the queue was full");
                });
    }

    private CalculationRunMessage message(String code, String severity, String ruleSystemCode) {
        return new CalculationRunMessage(
                1L,
                7L,
                code,
                severity,
                "lo que diga el motor del caso concreto",
                null,
                ruleSystemCode,
                ruleSystemCode == null ? null : "INTERNAL",
                ruleSystemCode == null ? null : "EMP000298",
                ruleSystemCode == null ? null : "202601",
                ruleSystemCode == null ? null : "NORMAL",
                ruleSystemCode == null ? null : 1,
                LocalDateTime.of(2026, 9, 12, 10, 0)
        );
    }
}
