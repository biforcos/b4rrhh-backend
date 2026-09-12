package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HireEmployeeDefaultingTest {

    private static final String REQUIRED_HIRE_BLOCKS = """
                            "hireDate": "2026-03-23",
                            "companyCode": "COMP",
                            "entryReasonCode": "HIRE",
                            "workCenterCode": "WC1",
                            "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                            },
                            "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                            },
                            "workingTime": {
                                    "workingTimePercentage": 75
                            }
                        """;

    @Mock
    private HireEmployeeUseCase hireEmployeeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HireEmployeeController controller = new HireEmployeeController(hireEmployeeUseCase, new HireEmployeeWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private HireEmployeeResult createMockResult(String rs, String type, String num) {
        LocalDate hireDate = LocalDate.now();
        return new HireEmployeeResult(
                new HireEmployeeResult.EmployeeSummary(rs, type, num, "Ana", "Lopez", null, null, "Ana Lopez", "ACTIVE", hireDate),
                new HireEmployeeResult.PresenceSummary(1, hireDate, "COMP", "HIRE"),
                new HireEmployeeResult.WorkCenterSummary(hireDate, "WC1"),
                null,
                new HireEmployeeResult.ContractSummary(hireDate, "CON", "SUB"),
                new HireEmployeeResult.LaborClassificationSummary(hireDate, "AGR", "CAT"),
                new HireEmployeeResult.WorkingTimeSummary(
                        1,
                        new BigDecimal("75"),
                        new BigDecimal("30.00"),
                        new BigDecimal("6.00"),
                        new BigDecimal("125.00"),
                        hireDate,
                        null
                )
        );
    }

    @Test
    void hireWithExplicitInternalUsesProvidedValue() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E001"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "E001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithoutEmployeeTypeCodeDefaultsToInternal() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E002"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeNumber": "E002",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithBlankEmployeeTypeCodeDefaultsToInternal() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E003"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "   ",
                                  "employeeNumber": "E003",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithExplicitExternalValuePreservesIt() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "EXTERNAL", "E004"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "EXTERNAL",
                                  "employeeNumber": "E004",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("EXTERNAL", captor.getValue().employeeTypeCode());
    }

    @Test
    void hireWithLowercaseValueNormalizesToUppercase() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(createMockResult("ESP", "INTERNAL", "E005"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "esp",
                                  "employeeTypeCode": "internal",
                                  "employeeNumber": "E005",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                """ + REQUIRED_HIRE_BLOCKS + """
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
    }
}
