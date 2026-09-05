package com.b4rrhh.employee.labor_classification.infrastructure.rest;

import com.b4rrhh.employee.labor_classification.application.command.CloseLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.CreateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.DeleteLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.application.command.PlanLaborClassificationChangeCommand;
import com.b4rrhh.employee.labor_classification.application.command.ReplaceLaborClassificationFromDateCommand;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlan;
import com.b4rrhh.employee.labor_classification.application.model.LaborClassificationPlanAdjustment;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.port.AgreementCategoryProfileRepository;
import com.b4rrhh.employee.labor_classification.application.usecase.CloseLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.CreateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.DeleteLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.GetLaborClassificationByBusinessKeyUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ListEmployeeLaborClassificationsUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.PlanLaborClassificationChangeUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.ReplaceLaborClassificationFromDateUseCase;
import com.b4rrhh.employee.labor_classification.application.usecase.UpdateLaborClassificationUseCase;
import com.b4rrhh.employee.labor_classification.application.command.UpdateLaborClassificationCommand;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationAgreementInvalidException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationCoverageIncompleteException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationIsACorrectionException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationNotFoundException;
import com.b4rrhh.employee.labor_classification.domain.exception.LaborClassificationOverlapException;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.domain.model.LaborClassificationPeriod;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.assembler.LaborClassificationResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LaborClassificationControllerHttpTest {

    @Mock
    private CreateLaborClassificationUseCase createLaborClassificationUseCase;
    @Mock
    private ListEmployeeLaborClassificationsUseCase listEmployeeLaborClassificationsUseCase;
    @Mock
    private GetLaborClassificationByBusinessKeyUseCase getLaborClassificationByBusinessKeyUseCase;
    @Mock
    private UpdateLaborClassificationUseCase updateLaborClassificationUseCase;
    @Mock
    private CloseLaborClassificationUseCase closeLaborClassificationUseCase;
    @Mock
    private ReplaceLaborClassificationFromDateUseCase replaceLaborClassificationFromDateUseCase;
    @Mock
    private DeleteLaborClassificationUseCase deleteLaborClassificationUseCase;
    @Mock
    private PlanLaborClassificationChangeUseCase planLaborClassificationChangeUseCase;
    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;
    @Mock
    private AgreementCategoryProfileRepository agreementCategoryProfileRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LaborClassificationResponseAssembler laborClassificationResponseAssembler =
                new LaborClassificationResponseAssembler(ruleEntityLabelResolver, agreementCategoryProfileRepository);

        LaborClassificationController controller = new LaborClassificationController(
                createLaborClassificationUseCase,
                listEmployeeLaborClassificationsUseCase,
                getLaborClassificationByBusinessKeyUseCase,
                updateLaborClassificationUseCase,
                closeLaborClassificationUseCase,
                replaceLaborClassificationFromDateUseCase,
                deleteLaborClassificationUseCase,
                planLaborClassificationChangeUseCase,
                laborClassificationResponseAssembler
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new LaborClassificationExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();

        lenient().when(ruleEntityLabelResolver.resolveName(anyString(), eq("AGREEMENT"), anyString(), any()))
                .thenReturn(Optional.empty());
        lenient().when(ruleEntityLabelResolver.resolveName(anyString(), eq("AGREEMENT_CATEGORY"), anyString(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void createMapsPathAndBodyToCommandAndHidesTechnicalIds() throws Exception {
        LaborClassification created = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class))).thenReturn(created);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.of("Administrative Category"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "AGR_OFFICE",
                                  "agreementCategoryCode": "CAT_ADMIN",
                                  "startDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agreementCode").value("AGR_OFFICE"))
                .andExpect(jsonPath("$.agreementName").value("Office Agreement"))
                .andExpect(jsonPath("$.agreementCategoryCode").value("CAT_ADMIN"))
                .andExpect(jsonPath("$.agreementCategoryName").value("Administrative Category"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.employeeId").doesNotExist());

        ArgumentCaptor<CreateLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(CreateLaborClassificationCommand.class);
        verify(createLaborClassificationUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().startDate());
    }

    @Test
    void closeEndpointUsesDomainActionPath() throws Exception {
        LaborClassification closed = laborClassification(
                "AGR_OFFICE",
                "CAT_ADMIN",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );
        when(closeLaborClassificationUseCase.close(any(CloseLaborClassificationCommand.class))).thenReturn(closed);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.of("Administrative Category"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-01/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endDate": "2026-01-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementCode").value("AGR_OFFICE"))
                .andExpect(jsonPath("$.agreementName").value("Office Agreement"))
                .andExpect(jsonPath("$.agreementCategoryCode").value("CAT_ADMIN"))
                .andExpect(jsonPath("$.agreementCategoryName").value("Administrative Category"));

        ArgumentCaptor<CloseLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(CloseLaborClassificationCommand.class);
        verify(closeLaborClassificationUseCase).close(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().startDate());
        assertEquals(LocalDate.of(2026, 1, 31), captor.getValue().endDate());
    }

    @Test
    void replaceFromDateMapsPathAndBodyToCommandAndReturns200() throws Exception {
        LaborClassification replaced = laborClassification("AGR_TECH", "CAT_TECH_1", LocalDate.of(2026, 3, 1), null);
        when(replaceLaborClassificationFromDateUseCase.replaceFromDate(any(ReplaceLaborClassificationFromDateCommand.class)))
                .thenReturn(replaced);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_TECH", null))
                .thenReturn(Optional.of("Technical Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_TECH_1", null))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/replace-from-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveDate": "2026-03-01",
                                  "agreementCode": "AGR_TECH",
                                  "agreementCategoryCode": "CAT_TECH_1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementCode").value("AGR_TECH"))
                .andExpect(jsonPath("$.agreementName").value("Technical Agreement"))
                .andExpect(jsonPath("$.agreementCategoryCode").value("CAT_TECH_1"))
                .andExpect(jsonPath("$.agreementCategoryName").isEmpty())
                .andExpect(jsonPath("$.startDate[0]").value(2026))
                .andExpect(jsonPath("$.startDate[1]").value(3))
                .andExpect(jsonPath("$.startDate[2]").value(1))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.employeeId").doesNotExist());

        ArgumentCaptor<ReplaceLaborClassificationFromDateCommand> captor =
                ArgumentCaptor.forClass(ReplaceLaborClassificationFromDateCommand.class);
        verify(replaceLaborClassificationFromDateUseCase).replaceFromDate(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(LocalDate.of(2026, 3, 1), captor.getValue().effectiveDate());
    }

    @Test
    void updateReturnsEnrichedLabels() throws Exception {
        LaborClassification updated = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(updateLaborClassificationUseCase.update(any())).thenReturn(updated);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.of("Administrative Category"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "AGR_OFFICE",
                                  "agreementCategoryCode": "CAT_ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementCode").value("AGR_OFFICE"))
                .andExpect(jsonPath("$.agreementName").value("Office Agreement"))
                .andExpect(jsonPath("$.agreementCategoryCode").value("CAT_ADMIN"))
                .andExpect(jsonPath("$.agreementCategoryName").value("Administrative Category"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    @Test
    void replaceFromDateMapsConflictToHttp409() throws Exception {
        when(replaceLaborClassificationFromDateUseCase.replaceFromDate(any(ReplaceLaborClassificationFromDateCommand.class)))
                .thenThrow(new LaborClassificationOverlapException(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        LocalDate.of(2026, 3, 1),
                        null
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/replace-from-date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "effectiveDate": "2026-03-01",
                                  "agreementCode": "AGR_TECH",
                                  "agreementCategoryCode": "CAT_TECH_1"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_OVERLAP"));
    }

    @Test
    void listEnrichesAgreementAndCategoryNamesWhenPresent() throws Exception {
        LaborClassification laborClassification = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any()))
                .thenReturn(List.of(laborClassification));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.of("Administrative Category"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/labor-classifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agreementCode").value("AGR_OFFICE"))
                .andExpect(jsonPath("$[0].agreementName").value("Office Agreement"))
                .andExpect(jsonPath("$[0].agreementCategoryCode").value("CAT_ADMIN"))
                .andExpect(jsonPath("$[0].agreementCategoryName").value("Administrative Category"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].employeeId").doesNotExist());
    }

    @Test
    void getByBusinessKeyKeepsCodesWhenCategoryLabelMissing() throws Exception {
        LaborClassification laborClassification = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(getLaborClassificationByBusinessKeyUseCase.getByBusinessKey(any()))
                .thenReturn(laborClassification);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementCode").value("AGR_OFFICE"))
                .andExpect(jsonPath("$.agreementName").value("Office Agreement"))
                .andExpect(jsonPath("$.agreementCategoryCode").value("CAT_ADMIN"))
                .andExpect(jsonPath("$.agreementCategoryName").isEmpty())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.employeeId").doesNotExist());
    }

    @Test
    void mapsConflictToHttp409() throws Exception {
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationOverlapException(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        LocalDate.of(2026, 1, 1),
                        null
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "AGR_OFFICE",
                                  "agreementCategoryCode": "CAT_ADMIN",
                                  "startDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_OVERLAP"));
    }

    @Test
    void updateCarriesBothDatesAndTheCodesToTheCommand() throws Exception {
        when(updateLaborClassificationUseCase.update(any()))
                .thenReturn(laborClassification("AGR_TECH", "CAT_TECH_1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endDate": "2026-06-30",
                                  "agreementCode": "AGR_TECH",
                                  "agreementCategoryCode": "CAT_TECH_1"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(UpdateLaborClassificationCommand.class);
        verify(updateLaborClassificationUseCase).update(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 1), captor.getValue().startDate());
        assertNull(captor.getValue().newStartDate());
        assertEquals(LocalDate.of(2026, 6, 30), captor.getValue().endDate());
        assertEquals("AGR_TECH", captor.getValue().agreementCode());
    }

    @Test
    void createMapsACoverageGapToHttp409SayingWhichGapAndWhatToStretch() throws Exception {
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationCoverageIncompleteException(
                        "ESP", "INTERNAL", "EMP001",
                        List.of(new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))),
                        List.of(
                                new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
                                new LaborClassificationPeriod(LocalDate.of(2026, 3, 1), null)
                        )
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "AGR_OFFICE",
                                  "agreementCategoryCode": "CAT_ADMIN",
                                  "startDate": "2026-03-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_INCOMPLETE_COVERAGE"))
                .andExpect(jsonPath("$.details.gaps[0].startDate[1]").value(2))
                .andExpect(jsonPath("$.details.gaps[0].endDate[2]").value(28))
                .andExpect(jsonPath("$.details.stretchCandidates[0].startDate[1]").value(1))
                .andExpect(jsonPath("$.details.stretchCandidates[1].endDate").doesNotExist());
    }

    @Test
    void createMapsACorrectionAskedForAsAnAddToHttp409NamingTheOccurrenceToCorrect() throws Exception {
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001",
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), null),
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), null)
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "AGR_TECH",
                                  "agreementCategoryCode": "CAT_TECH_1",
                                  "startDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_IS_A_CORRECTION"))
                .andExpect(jsonPath("$.message", containsString("corrige")))
                .andExpect(jsonPath("$.details.correctedOccurrence.startDate[0]").value(2026))
                .andExpect(jsonPath("$.details.correctedOccurrence.endDate").doesNotExist());
    }

    @Test
    void mapsBadRequestToHttp400() throws Exception {
        when(createLaborClassificationUseCase.create(any(CreateLaborClassificationCommand.class)))
                .thenThrow(new LaborClassificationAgreementInvalidException("BAD_AGR"));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreementCode": "BAD_AGR",
                                  "agreementCategoryCode": "CAT_ADMIN",
                                  "startDate": "2026-01-01"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGREEMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.details.field").value("agreementCode"));
    }

    @Test
    void mapsNotFoundToHttp404() throws Exception {
        when(getLaborClassificationByBusinessKeyUseCase.getByBusinessKey(any()))
                .thenThrow(new LaborClassificationNotFoundException(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        LocalDate.of(2026, 1, 1)
                ));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_NOT_FOUND"));
    }

    @Test
    void deleteMapsPathToCommandAndAnswersNoContent() throws Exception {
        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-16"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteLaborClassificationCommand> captor =
                ArgumentCaptor.forClass(DeleteLaborClassificationCommand.class);
        verify(deleteLaborClassificationUseCase).delete(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(LocalDate.of(2026, 1, 16), captor.getValue().startDate());
    }

    @Test
    void deleteMapsACoverageGapToHttp409NamingTheNeighboursToStretch() throws Exception {
        doThrow(new LaborClassificationCoverageIncompleteException(
                "ESP", "INTERNAL", "EMP001",
                List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                List.of(
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                        new LaborClassificationPeriod(LocalDate.of(2026, 2, 1), null)
                )
        )).when(deleteLaborClassificationUseCase).delete(any(DeleteLaborClassificationCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/labor-classifications/2026-01-16"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LABOR_CLASSIFICATION_INCOMPLETE_COVERAGE"))
                .andExpect(jsonPath("$.details.stretchCandidates[0].startDate[2]").value(1))
                .andExpect(jsonPath("$.details.stretchCandidates[1].startDate[1]").value(2));
    }

    @Test
    void planMapsTheRequestToTheCommandAndReturnsThePlanWithoutApplyingIt() throws Exception {
        when(planLaborClassificationChangeUseCase.plan(any(PlanLaborClassificationChangeCommand.class)))
                .thenReturn(new LaborClassificationPlan(
                        TimelineOperation.ADD,
                        null,
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), null),
                        null,
                        new LaborClassificationPlanAdjustment(
                                new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), null),
                                new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))
                        ),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(
                                new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                                new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), null)
                        )
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "ADD",
                                  "startDate": "2026-01-16"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.rejection").doesNotExist())
                .andExpect(jsonPath("$.adjustedOccurrence.before.endDate").doesNotExist())
                .andExpect(jsonPath("$.adjustedOccurrence.after.endDate[2]").value(15))
                .andExpect(jsonPath("$.projected[1].startDate[2]").value(16));

        ArgumentCaptor<PlanLaborClassificationChangeCommand> captor =
                ArgumentCaptor.forClass(PlanLaborClassificationChangeCommand.class);
        verify(planLaborClassificationChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.ADD, captor.getValue().operation());
        assertEquals(LocalDate.of(2026, 1, 16), captor.getValue().startDate());
        assertNull(captor.getValue().endDate());
        assertNull(captor.getValue().laborClassificationStartDate());
        verify(createLaborClassificationUseCase, org.mockito.Mockito.never()).create(any());
    }

    @Test
    void planTellsTheScreenAnAddOnAnExistingStartDateIsACorrectionOfThatOccurrence() throws Exception {
        when(planLaborClassificationChangeUseCase.plan(any(PlanLaborClassificationChangeCommand.class)))
                .thenReturn(new LaborClassificationPlan(
                        TimelineOperation.CORRECT,
                        TimelineRejection.IS_A_CORRECTION,
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), null),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "ADD",
                                  "startDate": "2026-01-01",
                                  "endDate": "2026-01-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("CORRECT"))
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.rejection").value("IS_A_CORRECTION"))
                .andExpect(jsonPath("$.correctedOccurrence.startDate[0]").value(2026))
                .andExpect(jsonPath("$.correctedOccurrence.endDate").doesNotExist())
                .andExpect(jsonPath("$.occurrence.endDate[2]").value(15));
    }

    @Test
    void planIdentifiesTheOccurrenceToRemoveByItsStartDate() throws Exception {
        when(planLaborClassificationChangeUseCase.plan(any(PlanLaborClassificationChangeCommand.class)))
                .thenReturn(new LaborClassificationPlan(
                        TimelineOperation.REMOVE,
                        TimelineRejection.GAP_NOT_ALLOWED,
                        new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31)),
                        null,
                        null,
                        List.of(),
                        List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))),
                        List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15))),
                        List.of(new LaborClassificationPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REMOVE",
                                  "laborClassificationStartDate": "2026-01-16"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.rejection").value("GAP_NOT_ALLOWED"))
                .andExpect(jsonPath("$.gaps[0].startDate[2]").value(16))
                .andExpect(jsonPath("$.stretchCandidates[0].startDate[2]").value(1));

        ArgumentCaptor<PlanLaborClassificationChangeCommand> captor =
                ArgumentCaptor.forClass(PlanLaborClassificationChangeCommand.class);
        verify(planLaborClassificationChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.REMOVE, captor.getValue().operation());
        assertEquals(LocalDate.of(2026, 1, 16), captor.getValue().laborClassificationStartDate());
    }

    @Test
    void planRejectsAnUnknownOperation() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/labor-classifications/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operation": "REPLACE",
                                  "startDate": "2026-01-16"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void doesNotExposeAlternateIdRoute() throws Exception {
        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/labor-classifications/AGR_OFFICE/2026-01-01"))
                .andExpect(status().isNotFound());
    }

    private LaborClassification laborClassification(
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new LaborClassification(
                10L,
                agreementCode,
                agreementCategoryCode,
                startDate,
                endDate
        );
    }

    // ADR-052 §4 (backend#24): el idioma entra por Accept-Language y llega al resolutor desde el ensamblador.
    @Test
    void listServesAgreementAndCategoryInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeeLaborClassificationsUseCase.listByEmployeeBusinessKey(any()))
                .thenReturn(List.of(laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null)));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", "es-ES"))
                .thenReturn(Optional.of("Convenio de oficinas"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", "es-ES"))
                .thenReturn(Optional.of("Categoría administrativa"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/labor-classifications")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].agreementName").value("Convenio de oficinas"))
                .andExpect(jsonPath("$[0].agreementCategoryName").value("Categoría administrativa"));
    }
}
