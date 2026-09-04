package com.b4rrhh.employee.lifecycle.application.participant;

import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.application.port.HireParticipant;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeBusinessValidationException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeCommand;
import com.b4rrhh.employee.working_time.application.usecase.CreateWorkingTimeUseCase;
import com.b4rrhh.employee.working_time.domain.exception.InvalidWorkingTimePercentageException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeCoverageGapException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeEmployeeNotFoundException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeNumberConflictException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOutsidePresencePeriodException;
import com.b4rrhh.employee.working_time.domain.exception.WorkingTimeOverlapException;
import org.springframework.stereotype.Component;

@Component
public class WorkingTimeParticipant implements HireParticipant {

    private final CreateWorkingTimeUseCase createWorkingTimeUseCase;

    public WorkingTimeParticipant(CreateWorkingTimeUseCase createWorkingTimeUseCase) {
        this.createWorkingTimeUseCase = createWorkingTimeUseCase;
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public void participate(HireContext ctx) {
        try {
            ctx.setWorkingTimeResult(createWorkingTimeUseCase.create(new CreateWorkingTimeCommand(
                    ctx.ruleSystemCode(), ctx.employeeTypeCode(), ctx.employeeNumber(),
                    ctx.hireDate(), null, ctx.workingTime().workingTimePercentage()
            )));
        } catch (InvalidWorkingTimePercentageException
                 | WorkingTimeOutsidePresencePeriodException
                 | WorkingTimeOverlapException
                 | WorkingTimeCoverageGapException ex) {
            throw new HireEmployeeBusinessValidationException(ex.getMessage(), ex);
        } catch (WorkingTimeNumberConflictException ex) {
            throw new HireEmployeeConflictException(ex.getMessage());
        } catch (WorkingTimeEmployeeNotFoundException ex) {
            throw new HireEmployeeConflictException("Created employee not available for initial working time creation");
        }
    }
}
