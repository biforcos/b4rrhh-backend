package com.b4rrhh.rulesystem.infrastructure.web;

import com.b4rrhh.rulesystem.application.usecase.CloseRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.CloseRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.CorrectRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.CorrectRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.DeleteRuleEntityCommand;
import com.b4rrhh.rulesystem.application.usecase.DeleteRuleEntityUseCase;
import com.b4rrhh.rulesystem.application.usecase.GetRuleEntityByBusinessKeyQuery;
import com.b4rrhh.rulesystem.application.usecase.GetRuleEntityByBusinessKeyUseCase;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityAlreadyClosedException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInvalidDateRangeException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityNotFoundException;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityOverlapException;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RuleEntityBusinessKeyControllerHttpTest {

        @Mock
        private GetRuleEntityByBusinessKeyUseCase getRuleEntityByBusinessKeyUseCase;
        @Mock
        private CorrectRuleEntityUseCase correctRuleEntityUseCase;
        @Mock
        private CloseRuleEntityUseCase closeRuleEntityUseCase;
    @Mock
    private DeleteRuleEntityUseCase deleteRuleEntityUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RuleEntityBusinessKeyController controller = new RuleEntityBusinessKeyController(
                getRuleEntityByBusinessKeyUseCase,
                correctRuleEntityUseCase,
                closeRuleEntityUseCase,
                deleteRuleEntityUseCase
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RuleEntityExceptionHandler())
                .build();
    }

    @Test
    void getReturns200WhenOccurrenceExists() throws Exception {
        when(getRuleEntityByBusinessKeyUseCase.get(any(GetRuleEntityByBusinessKeyQuery.class)))
                .thenReturn(ruleEntity(LocalDate.of(1900, 1, 1), null));

        mockMvc.perform(get("/rule-entities/ESP/COMPANY/ES01/1900-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.ruleEntityTypeCode").value("COMPANY"))
                .andExpect(jsonPath("$.code").value("ES01"));
    }

    @Test
    void getReturns404WhenOccurrenceDoesNotExist() throws Exception {
        when(getRuleEntityByBusinessKeyUseCase.get(any(GetRuleEntityByBusinessKeyQuery.class)))
                .thenThrow(new RuleEntityNotFoundException("ESP", "COMPANY", "ES01", LocalDate.of(1900, 1, 1)));

        mockMvc.perform(get("/rule-entities/ESP/COMPANY/ES01/1900-01-01"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReturns200WhenCorrectionSucceeds() throws Exception {
        when(correctRuleEntityUseCase.correct(any(CorrectRuleEntityCommand.class)))
                .thenReturn(ruleEntity(LocalDate.of(1900, 1, 1), LocalDate.of(2026, 12, 31)));

        mockMvc.perform(put("/rule-entities/ESP/COMPANY/ES01/1900-01-01")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Company corrected",
                                  "description": "Corrected",
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Company"));
    }

    @Test
    void putReturns400WhenPayloadIsInvalid() throws Exception {
        when(correctRuleEntityUseCase.correct(any(CorrectRuleEntityCommand.class)))
                .thenThrow(new IllegalArgumentException("name is required"));

        mockMvc.perform(put("/rule-entities/ESP/COMPANY/ES01/1900-01-01")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putReturns404WhenOccurrenceDoesNotExist() throws Exception {
        when(correctRuleEntityUseCase.correct(any(CorrectRuleEntityCommand.class)))
                .thenThrow(new RuleEntityNotFoundException("ESP", "COMPANY", "ES01", LocalDate.of(1900, 1, 1)));

        mockMvc.perform(put("/rule-entities/ESP/COMPANY/ES01/1900-01-01")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Company corrected"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReturns409WhenDomainConflictOccurs() throws Exception {
        when(correctRuleEntityUseCase.correct(any(CorrectRuleEntityCommand.class)))
                .thenThrow(new RuleEntityOverlapException("ESP", "COMPANY", "ES01"));

        mockMvc.perform(put("/rule-entities/ESP/COMPANY/ES01/1900-01-01")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Company corrected",
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void closeReturns200WhenClosingSucceeds() throws Exception {
        when(closeRuleEntityUseCase.close(any(CloseRuleEntityCommand.class)))
                .thenReturn(ruleEntity(LocalDate.of(1900, 1, 1), LocalDate.of(2026, 12, 31)));

        mockMvc.perform(post("/rule-entities/ESP/COMPANY/ES01/1900-01-01/close")
                        .contentType("application/json")
                        .content("""
                                {
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk())
                                                                .andExpect(jsonPath("$.endDate[0]").value(2026))
                                                                .andExpect(jsonPath("$.endDate[1]").value(12))
                                                                .andExpect(jsonPath("$.endDate[2]").value(31));
    }

    @Test
    void closeReturns400WhenPayloadIsInvalid() throws Exception {
        when(closeRuleEntityUseCase.close(any(CloseRuleEntityCommand.class)))
                .thenThrow(new IllegalArgumentException("endDate is required"));

        mockMvc.perform(post("/rule-entities/ESP/COMPANY/ES01/1900-01-01/close")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closeReturns404WhenOccurrenceDoesNotExist() throws Exception {
        when(closeRuleEntityUseCase.close(any(CloseRuleEntityCommand.class)))
                .thenThrow(new RuleEntityNotFoundException("ESP", "COMPANY", "ES01", LocalDate.of(1900, 1, 1)));

        mockMvc.perform(post("/rule-entities/ESP/COMPANY/ES01/1900-01-01/close")
                        .contentType("application/json")
                        .content("""
                                {
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void closeReturns409WhenDomainConflictOccurs() throws Exception {
        when(closeRuleEntityUseCase.close(any(CloseRuleEntityCommand.class)))
                .thenThrow(new RuleEntityAlreadyClosedException("ESP", "COMPANY", "ES01"));

        mockMvc.perform(post("/rule-entities/ESP/COMPANY/ES01/1900-01-01/close")
                        .contentType("application/json")
                        .content("""
                                {
                                  "endDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteReturns204WhenRuleEntityCanBeDeleted() throws Exception {
        doNothing().when(deleteRuleEntityUseCase).delete(any(DeleteRuleEntityCommand.class));

        mockMvc.perform(delete("/rule-entities/ESP/COMPANY/ES01/1900-01-01"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteRuleEntityCommand> captor = ArgumentCaptor.forClass(DeleteRuleEntityCommand.class);
        verify(deleteRuleEntityUseCase).delete(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("COMPANY", captor.getValue().ruleEntityTypeCode());
        assertEquals("ES01", captor.getValue().code());
        assertEquals(LocalDate.of(1900, 1, 1), captor.getValue().startDate());
    }

    @Test
    void deleteReturns404WhenRuleEntityDoesNotExist() throws Exception {
        doThrow(new RuleEntityNotFoundException("ESP", "COMPANY", "ES01", LocalDate.of(1900, 1, 1)))
                .when(deleteRuleEntityUseCase)
                .delete(any(DeleteRuleEntityCommand.class));

        mockMvc.perform(delete("/rule-entities/ESP/COMPANY/ES01/1900-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteReturns409WhenRuleEntityIsUsed() throws Exception {
        doThrow(new RuleEntityInUseException("ESP", "COMPANY", "ES01",
                List.of(new RuleEntityReference("presences", 412))))
                .when(deleteRuleEntityUseCase)
                .delete(any(DeleteRuleEntityCommand.class));

        mockMvc.perform(delete("/rule-entities/ESP/COMPANY/ES01/1900-01-01"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Rule entity is in use: ESP/COMPANY/ES01 is referenced by 412 presences"));
    }

        @Test
        void closeReturns409WhenDateRangeIsInvalid() throws Exception {
                when(closeRuleEntityUseCase.close(any(CloseRuleEntityCommand.class)))
                                .thenThrow(new RuleEntityInvalidDateRangeException(LocalDate.of(1900, 1, 1), LocalDate.of(1899, 12, 31)));

                mockMvc.perform(post("/rule-entities/ESP/COMPANY/ES01/1900-01-01/close")
                                                .contentType("application/json")
                                                .content("""
                                                                {
                                                                  "endDate": "1899-12-31"
                                                                }
                                                                """))
                                .andExpect(status().isConflict());
        }

        private RuleEntity ruleEntity(LocalDate startDate, LocalDate endDate) {
                return new RuleEntity(
                                1L,
                                "ESP",
                                "COMPANY",
                                "ES01",
                                "Company",
                                null,
                                endDate == null,
                                startDate,
                                endDate,
                                LocalDateTime.now(),
                                LocalDateTime.now()
                );
        }
}
