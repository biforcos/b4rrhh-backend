package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.extra_payment_regime.application.usecase.CloseExtraPaymentRegimeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.CloseExtraPaymentRegimeUseCase;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.ListEmployeeExtraPaymentRegimesUseCase;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeAlreadyClosedException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeOutsidePresencePeriodException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.InvalidExtraPaymentRegimeDateRangeException;
import com.b4rrhh.employee.extra_payment_regime.domain.model.ExtraPaymentRegime;
import com.b4rrhh.employee.lifecycle.application.model.TerminationContext;
import com.b4rrhh.employee.lifecycle.application.port.TerminationParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.TerminateEmployeeConflictException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El cese cierra el regimen de pagas extras, como cierra todo lo que deriva de la presencia.
 *
 * <p>Detras de la presencia, que se cierra la primera (orden 5): la invariante de cobertura de
 * esta serie se comprueba contra el periodo de presencia que va a quedar, no contra el que habia.
 */
@Component
public class ExtraPaymentRegimeTerminationParticipant implements TerminationParticipant {

    private final ListEmployeeExtraPaymentRegimesUseCase listExtraPaymentRegimes;
    private final CloseExtraPaymentRegimeUseCase closeExtraPaymentRegime;

    public ExtraPaymentRegimeTerminationParticipant(
            ListEmployeeExtraPaymentRegimesUseCase listExtraPaymentRegimes,
            CloseExtraPaymentRegimeUseCase closeExtraPaymentRegime) {
        this.listExtraPaymentRegimes = listExtraPaymentRegimes;
        this.closeExtraPaymentRegime = closeExtraPaymentRegime;
    }

    @Override
    public int order() { return 25; }

    @Override
    public void participate(TerminationContext ctx) {
        List<ExtraPaymentRegime> all = listExtraPaymentRegimes.listByEmployeeBusinessKey(
                new ListEmployeeExtraPaymentRegimesCommand(
                        ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber()));

        List<ExtraPaymentRegime> active = all.stream()
                .filter(regime -> regime.getEndDate() == null)
                .toList();

        if (active.size() > 1) {
            throw new TerminateEmployeeConflictException(
                    "Multiple active extra payment regimes found for employee " + ctx.employeeNumber());
        }
        if (active.isEmpty()) return;

        ExtraPaymentRegime activeRegime = active.get(0);
        if (activeRegime.getStartDate().isAfter(ctx.terminationDate())) return;

        try {
            closeExtraPaymentRegime.close(new CloseExtraPaymentRegimeCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    activeRegime.getExtraPaymentRegimeNumber(), ctx.terminationDate()));
        } catch (ExtraPaymentRegimeAlreadyClosedException | ExtraPaymentRegimeNotFoundException |
                 InvalidExtraPaymentRegimeDateRangeException | ExtraPaymentRegimeOutsidePresencePeriodException e) {
            throw new TerminateEmployeeConflictException(e.getMessage(), e);
        }
    }
}
