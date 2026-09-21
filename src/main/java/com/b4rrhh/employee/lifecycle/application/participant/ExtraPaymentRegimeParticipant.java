package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.extra_payment_regime.application.usecase.CreateExtraPaymentRegimeCommand;
import com.b4rrhh.employee.extra_payment_regime.application.usecase.CreateExtraPaymentRegimeUseCase;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeEmployeeNotFoundException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeNumberConflictException;
import com.b4rrhh.employee.extra_payment_regime.domain.exception.ExtraPaymentRegimeSeriesInvariantException;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import org.springframework.stereotype.Component;

/**
 * El alta crea el primer tramo de regimen de pagas extras ({@code backend#118}).
 *
 * <h2>Nadie elige el regimen al contratar</h2>
 *
 * <p>El mandato no lo lleva: se copia el del convenio que le aplica al empleado
 * ({@code backend#117}). Contratar no es el sitio donde se decide si a alguien se le prorratean
 * las pagas — lo decide el convenio, y quien quiera otra cosa lo pide, y eso es una fila mas en
 * su vertical.
 *
 * <p>Va detras de la clasificacion profesional (orden 60), que es la que dice a que convenio
 * pertenece el empleado: sin ella no hay testigo que copiar.
 */
@Component
public class ExtraPaymentRegimeParticipant implements HireParticipant {

    private final CreateExtraPaymentRegimeUseCase createExtraPaymentRegimeUseCase;

    public ExtraPaymentRegimeParticipant(CreateExtraPaymentRegimeUseCase createExtraPaymentRegimeUseCase) {
        this.createExtraPaymentRegimeUseCase = createExtraPaymentRegimeUseCase;
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public void participate(HireContext ctx) {
        try {
            createExtraPaymentRegimeUseCase.create(new CreateExtraPaymentRegimeCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    // Sin regimen: el que diga el convenio.
                    ctx.hireDate(), null, null
            ));
        } catch (ExtraPaymentRegimeSeriesInvariantException ex) {
            throw new HireEmployeeBusinessValidationException(ex.getMessage(), ex);
        } catch (ExtraPaymentRegimeNumberConflictException ex) {
            throw new HireEmployeeConflictException(ex.getMessage());
        } catch (ExtraPaymentRegimeEmployeeNotFoundException ex) {
            throw new HireEmployeeConflictException(
                    "Created employee not available for initial extra payment regime creation");
        }
    }
}
