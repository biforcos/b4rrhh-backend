package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.domain.model.CalculationRunMessage;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunMessageResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCalculationRunMessagesResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayrollCalculationRunMessageResponseAssembler {

    /** El tipo de catalogo de los codigos de mensaje de ejecucion (V125, backend#81). */
    private static final String PAYROLL_RUN_MESSAGE = "PAYROLL_RUN_MESSAGE";

    private final RuleEntityLabelResolver ruleEntityLabelResolver;

    public PayrollCalculationRunMessageResponseAssembler(RuleEntityLabelResolver ruleEntityLabelResolver) {
        this.ruleEntityLabelResolver = ruleEntityLabelResolver;
    }

    /**
     * @param ruleSystemCode la reglamentacion de la ejecucion, no la del mensaje: un
     *                       mensaje de ejecucion entera —la cola llena, el reinicio— no
     *                       nombra ninguna unidad y por eso no lleva la suya, pero su
     *                       codigo se llama igual y hay que saber pintarlo.
     */
    public PayrollCalculationRunMessagesResponse toResponse(
            Long runId,
            String ruleSystemCode,
            List<CalculationRunMessage> messages,
            ResponseLanguage language
    ) {
        return new PayrollCalculationRunMessagesResponse(
                runId,
                messages.stream().map(message -> toItem(message, ruleSystemCode, language)).toList()
        );
    }

    private PayrollCalculationRunMessageResponse toItem(
            CalculationRunMessage message,
            String ruleSystemCode,
            ResponseLanguage language
    ) {
        String messageRuleSystemCode = message.ruleSystemCode() == null
                ? ruleSystemCode
                : message.ruleSystemCode();

        // Vacio si el codigo no esta en el catalogo. No se inventa nada aqui: la pantalla
        // ya sabe pintar el codigo solo, y un literal fabricado al vuelo seria el
        // diccionario en el cliente que el backend#16 existe para evitar.
        String messageCodeName = ruleEntityLabelResolver
                .resolveName(messageRuleSystemCode, PAYROLL_RUN_MESSAGE, message.messageCode(), language.code())
                .orElse(null);

        return new PayrollCalculationRunMessageResponse(
                message.messageCode(),
                messageCodeName,
                message.severityCode(),
                message.message(),
                message.detailsJson(),
                message.ruleSystemCode(),
                message.employeeTypeCode(),
                message.employeeNumber(),
                message.payrollPeriodCode(),
                message.payrollTypeCode(),
                message.presenceNumber(),
                message.createdAt()
        );
    }
}
