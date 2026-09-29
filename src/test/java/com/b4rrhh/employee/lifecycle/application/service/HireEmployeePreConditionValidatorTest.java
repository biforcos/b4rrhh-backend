package com.b4rrhh.employee.lifecycle.application.service;

import com.b4rrhh.employee.employee.application.service.EmployeeTypeCatalogValidator;
import com.b4rrhh.employee.employee.domain.exception.EmployeeTypeInvalidException;
import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireContext;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeRequestInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;
import com.b4rrhh.employee.workcenter.domain.service.WorkCenterCompanyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HireEmployeePreConditionValidatorTest {

    @Mock
    private WorkCenterCompanyValidator workCenterCompanyValidator;
    @Mock
    private EmployeeTypeCatalogValidator employeeTypeCatalogValidator;

    private HireEmployeePreConditionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new HireEmployeePreConditionValidator(workCenterCompanyValidator, employeeTypeCatalogValidator);
    }

    @Test
    void returnsNormalizedContextForValidCommand() {
        HireContext ctx = validator.validateAndNormalize(validCommand());

        assertThat(ctx.ruleSystemCode()).isEqualTo("ESP");
        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(ctx.firstName()).isEqualTo("Ana");
        assertThat(ctx.lastName1()).isEqualTo("Lopez");
        assertThat(ctx.lastName2()).isNull();
        assertThat(ctx.preferredName()).isEqualTo("Ani");
        assertThat(ctx.companyCode()).isEqualTo("COMP");
        assertThat(ctx.entryReasonCode()).isEqualTo("HIRING");
        assertThat(ctx.workCenterCode()).isEqualTo("WC1");
        assertThat(ctx.hireDate()).isEqualTo(LocalDate.of(2026, 3, 23));
        assertThat(ctx.contract().contractTypeCode()).isEqualTo("CON");
        assertThat(ctx.laborClassification().agreementCode()).isEqualTo("AGR");
        assertThat(ctx.workingTime().workingTimePercentage()).isEqualByComparingTo("75");
        assertThat(ctx.employeeNumber()).isNull();
    }

    @Test
    void normalizesCodeFieldsToUpperCase() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "esp", "internal", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "hiring", "comp", "wc1",
                new HireEmployeeCommand.HireEmployeeContractCommand("con", "sub"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("agr", "cat"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        HireContext ctx = validator.validateAndNormalize(command);

        assertThat(ctx.ruleSystemCode()).isEqualTo("ESP");
        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL");
        assertThat(ctx.companyCode()).isEqualTo("COMP");
        assertThat(ctx.workCenterCode()).isEqualTo("WC1");
        assertThat(ctx.contract().contractTypeCode()).isEqualTo("CON");
        assertThat(ctx.laborClassification().agreementCode()).isEqualTo("AGR");
    }

    @Test
    void defaultsEmployeeTypeCodeWhenBlank() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP", null, "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRING", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        HireContext ctx = validator.validateAndNormalize(command);

        assertThat(ctx.employeeTypeCode()).isEqualTo("INTERNAL"); // HireEmployeeDefaultValues.DEFAULT_EMPLOYEE_TYPE_CODE
    }

    @Test
    void throwsWhenCommandIsNull() {
        assertThatThrownBy(() -> validator.validateAndNormalize(null))
                .isInstanceOf(HireEmployeeRequestInvalidException.class);
    }

    @Test
    void throwsWhenRuleSystemCodeIsBlank() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "  ", "INTERNAL", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRING", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(command))
                .isInstanceOf(HireEmployeeRequestInvalidException.class)
                .hasMessageContaining("ruleSystemCode");
    }

    @Test
    void throwsWhenContractIsNull() {
        HireEmployeeCommand command = new HireEmployeeCommand(
                "ESP", "INTERNAL", "Ana", "Lopez", null, null,
                LocalDate.of(2026, 3, 23), "HIRING", "COMP", "WC1",
                null,
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(command))
                .isInstanceOf(HireEmployeeRequestInvalidException.class)
                .hasMessageContaining("contract");
    }

    @Test
    void throwsWhenWorkCenterDoesNotBelongToCompany() {
        doThrow(new WorkCenterCompanyMismatchException("WC1", "COMP"))
                .when(workCenterCompanyValidator)
                .validateBelongsToCompany(eq("ESP"), eq("WC1"), eq("COMP"), any(LocalDate.class));

        assertThatThrownBy(() -> validator.validateAndNormalize(validCommand()))
                .isInstanceOf(WorkCenterCompanyMismatchException.class);
    }

    @Test
    void throwsWhenEmployeeTypeIsInvalid() {
        doThrow(new EmployeeTypeInvalidException("INTERNAL"))
                .when(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode(eq("ESP"), eq("INTERNAL"), any(LocalDate.class));

        assertThatThrownBy(() -> validator.validateAndNormalize(validCommand()))
                .isInstanceOf(HireEmployeeCatalogValueInvalidException.class);
    }

    @Test
    void callsValidatorsWithNormalizedValues() {
        validator.validateAndNormalize(validCommand());

        verify(workCenterCompanyValidator)
                .validateBelongsToCompany("ESP", "WC1", "COMP", LocalDate.of(2026, 3, 23));
        verify(employeeTypeCatalogValidator)
                .validateEmployeeTypeCode("ESP", "INTERNAL", LocalDate.of(2026, 3, 23));
    }

    private HireEmployeeCommand validCommand() {
        return new HireEmployeeCommand(
                "ESP", "INTERNAL", "Ana", "Lopez", null, "Ani",
                LocalDate.of(2026, 3, 23), "HIRING", "COMP", "WC1",
                new HireEmployeeCommand.HireEmployeeContractCommand("CON", "SUB"),
                new HireEmployeeCommand.HireEmployeeLaborClassificationCommand("AGR", "CAT"),
                null,
                new HireEmployeeCommand.HireEmployeeWorkingTimeCommand(new BigDecimal("75"))
        );
    }
}
