package com.b4rrhh.employee.lifecycle.infrastructure.rest;

import com.b4rrhh.employee.lifecycle.application.command.HireEmployeeCommand;
import com.b4rrhh.employee.lifecycle.application.model.HireEmployeeResult;
import com.b4rrhh.employee.lifecycle.application.usecase.HireEmployeeUseCase;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeCatalogValueInvalidException;
import com.b4rrhh.employee.lifecycle.domain.exception.HireEmployeeConflictException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCatalogValueInvalidException;
import com.b4rrhh.employee.workcenter.domain.exception.WorkCenterCompanyMismatchException;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HireEmployeeControllerHttpTest {

    @Mock
    private HireEmployeeUseCase hireEmployeeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
      HireEmployeeController controller = new HireEmployeeController(hireEmployeeUseCase, new HireEmployeeWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new HireEmployeeExceptionHandler())
                .build();
    }

    @Test
    void hireReturnsCreatedWithBusinessKeysAndInitialSections() throws Exception {
        LocalDate hireDate = LocalDate.of(2026, 3, 23);
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenReturn(new HireEmployeeResult(
                        new HireEmployeeResult.EmployeeSummary("ESP", "INTERNAL", "EMP001", "Ana", "Lopez", null, null, "Ana Lopez", "ACTIVE", hireDate),
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
                ));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "comp",
                                  "entryReasonCode": "hire",
                                  "workCenterCode": "wc1",
                                  "laborClassification": {
                                    "agreementCode": "agr",
                                    "agreementCategoryCode": "cat"
                                  },
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "contract": {
                                    "contractTypeCode": "con",
                                    "contractSubtypeCode": "sub"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.initialPresence.presenceNumber").value(1))
                .andExpect(jsonPath("$.initialContract.contractTypeCode").value("CON"))
                .andExpect(jsonPath("$.initialLaborClassification.agreementCode").value("AGR"))
                .andExpect(jsonPath("$.workingTime.workingTimeNumber").value(1))
                .andExpect(jsonPath("$.workingTime.workingTimePercentage").value(75))
                .andExpect(jsonPath("$.workingTime.weeklyHours").value(30.0))
                .andExpect(jsonPath("$.workingTime.dailyHours").value(6.0))
                .andExpect(jsonPath("$.workingTime.monthlyHours").value(125.0))
                .andExpect(jsonPath("$.workingTime.startDate[0]").value(2026))
                .andExpect(jsonPath("$.workingTime.startDate[1]").value(3))
                .andExpect(jsonPath("$.workingTime.startDate[2]").value(23))
                .andExpect(jsonPath("$.workingTime.endDate").isEmpty())
                .andExpect(jsonPath("$.id").doesNotExist());

        ArgumentCaptor<HireEmployeeCommand> captor = ArgumentCaptor.forClass(HireEmployeeCommand.class);
        verify(hireEmployeeUseCase).hire(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals(hireDate, captor.getValue().hireDate());
    }

    @Test
    void hireReturnsUnprocessableEntityWhenCatalogValueIsInvalid() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenThrow(new HireEmployeeCatalogValueInvalidException("companyCode is invalid", null));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "BAD",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CATALOG_VALUE"));
    }

    @Test
    void hireReturnsUnprocessableEntityWhenWorkCenterCatalogValueIsInvalid() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenThrow(new WorkCenterCatalogValueInvalidException("workCenterCode", "REMOTE"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2020-01-05",
                                  "companyCode": "ES01",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "REMOTE",
                                  "workingTime": {
                                    "workingTimePercentage": 100
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CATALOG_VALUE"));
    }

    @Test
    void hireReturnsConflictWhenWorkCenterBelongsToDifferentCompany() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenThrow(new WorkCenterCompanyMismatchException("WC1", "COMP"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "COMP",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORK_CENTER_COMPANY_MISMATCH"));
    }

    @Test
    void hireReturnsBadRequestWhenWorkingTimeIsMissing() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
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
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HIRE_REQUEST_INVALID"));

                          verify(hireEmployeeUseCase, never()).hire(any(HireEmployeeCommand.class));
    }

    @Test
    void hireReturnsBadRequestWhenWorkingTimeContainsDerivedHours() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "COMP",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75,
                                    "weeklyHours": 30
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HIRE_REQUEST_INVALID"));

        verify(hireEmployeeUseCase, never()).hire(any(HireEmployeeCommand.class));
    }

    @Test
    void hireReturnsBadRequestWhenContractIsMissing() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "COMP",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HIRE_REQUEST_INVALID"))
                .andExpect(jsonPath("$.message").value("contract is required"));

        verify(hireEmployeeUseCase, never()).hire(any(HireEmployeeCommand.class));
    }

    @Test
    void hireReturnsBadRequestWhenLaborClassificationIsMissing() throws Exception {
        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "COMP",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HIRE_REQUEST_INVALID"))
                .andExpect(jsonPath("$.message").value("laborClassification is required"));

        verify(hireEmployeeUseCase, never()).hire(any(HireEmployeeCommand.class));
    }

    @Test
    void hireReturnsConflictWhenLifecycleUseCaseMapsWorkingTimeConflict() throws Exception {
        when(hireEmployeeUseCase.hire(any(HireEmployeeCommand.class)))
                .thenThrow(new HireEmployeeConflictException("Working time number 1 already exists"));

        mockMvc.perform(post("/employees/hire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "employeeTypeCode": "INTERNAL",
                                  "employeeNumber": "EMP001",
                                  "firstName": "Ana",
                                  "lastName1": "Lopez",
                                  "hireDate": "2026-03-23",
                                  "companyCode": "COMP",
                                  "entryReasonCode": "HIRE",
                                  "workCenterCode": "WC1",
                                  "workingTime": {
                                    "workingTimePercentage": 75
                                  },
                                  "laborClassification": {
                                    "agreementCode": "AGR",
                                    "agreementCategoryCode": "CAT"
                                  },
                                  "contract": {
                                    "contractTypeCode": "CON",
                                    "contractSubtypeCode": "SUB"
                                  }
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HIRE_CONFLICT"));
    }
}
