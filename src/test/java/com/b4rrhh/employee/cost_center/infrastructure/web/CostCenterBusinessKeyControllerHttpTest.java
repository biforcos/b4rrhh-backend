package com.b4rrhh.employee.cost_center.infrastructure.web;

import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlan;
import com.b4rrhh.employee.cost_center.application.model.CostCenterDistributionPlanAdjustment;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionReadModel;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.DeleteCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.DeleteCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionQuery;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryQuery;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.PlanCostCenterDistributionChangeCommand;
import com.b4rrhh.employee.cost_center.application.usecase.PlanCostCenterDistributionChangeUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.UpdateCostCenterDistributionCommand;
import com.b4rrhh.employee.cost_center.application.usecase.UpdateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionCoverageGapException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionIsACorrectionException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionInvalidException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionNotFoundException;
import com.b4rrhh.employee.cost_center.domain.exception.CostCenterDistributionOverlapException;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterAllocation;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionPeriod;
import com.b4rrhh.employee.cost_center.domain.model.CostCenterDistributionWindow;
import com.b4rrhh.employee.cost_center.infrastructure.web.assembler.CostCenterResponseAssembler;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
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
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/**
 * Fija la forma de la respuesta de las lecturas de centro de coste tras backend#27: los
 * mismos campos con los mismos nombres que cuando el literal lo ponía el caso de uso; lo
 * único que cambia es que ahora lo rellena el assembler en la capa web, con el idioma de
 * la respuesta. Y, desde backend#54, la forma del PUT, el DELETE, el plan y los errores
 * con {@code code} y {@code details} (ADR-057).
 */
@ExtendWith(MockitoExtension.class)
class CostCenterBusinessKeyControllerHttpTest {

    private static final LocalDate JAN_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JAN_15 = LocalDate.of(2026, 1, 15);
    private static final LocalDate JAN_16 = LocalDate.of(2026, 1, 16);

    @Mock
    private CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;
    @Mock
    private GetCurrentCostCenterDistributionUseCase getCurrentCostCenterDistributionUseCase;
    @Mock
    private ListCostCenterDistributionHistoryUseCase listCostCenterDistributionHistoryUseCase;
    @Mock
    private UpdateCostCenterDistributionUseCase updateCostCenterDistributionUseCase;
    @Mock
    private DeleteCostCenterDistributionUseCase deleteCostCenterDistributionUseCase;
    @Mock
    private PlanCostCenterDistributionChangeUseCase planCostCenterDistributionChangeUseCase;
    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CostCenterBusinessKeyController(
                                createCostCenterDistributionUseCase,
                                getCurrentCostCenterDistributionUseCase,
                                listCostCenterDistributionHistoryUseCase,
                                updateCostCenterDistributionUseCase,
                                deleteCostCenterDistributionUseCase,
                                planCostCenterDistributionChangeUseCase,
                                new CostCenterResponseAssembler(ruleEntityLabelResolver)
                        )
                )
                .setControllerAdvice(new CostCenterExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void createMapsTheEndDateAndAnAddOnAnExistingStartDateToHttp409NamingTheWindowToCorrect() throws Exception {
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterDistributionIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001",
                        new CostCenterDistributionPeriod(JAN_1, null),
                        new CostCenterDistributionPeriod(JAN_1, JAN_15)
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-01-01","endDate":"2026-01-15",
                                 "items":[{"costCenterCode":"CC_HR","allocationPercentage":100}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COST_CENTER_IS_A_CORRECTION"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.correctedOccurrence.startDate").exists())
                .andExpect(jsonPath("$.details.correctedOccurrence.endDate").isEmpty());

        ArgumentCaptor<CreateCostCenterDistributionCommand> captor =
                ArgumentCaptor.forClass(CreateCostCenterDistributionCommand.class);
        verify(createCostCenterDistributionUseCase).create(captor.capture());
        assertEquals(JAN_1, captor.getValue().startDate());
        assertEquals(JAN_15, captor.getValue().endDate());
    }

    @Test
    void createMapsAnOverlapToHttp409WithTheSharedDates() throws Exception {
        when(createCostCenterDistributionUseCase.create(any(CreateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterDistributionOverlapException(
                        "ESP", "INTERNAL", "EMP001", JAN_1, JAN_15,
                        List.of(new CostCenterDistributionPeriod(JAN_1, JAN_15))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-01-01","endDate":"2026-01-15",
                                 "items":[{"costCenterCode":"CC_HR","allocationPercentage":100}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COST_CENTER_OVERLAP"))
                .andExpect(jsonPath("$.details.overlaps[0].startDate").exists());
    }

    // The PUT that fixes the typo in place: the window is given back the start
    // date it already had, which is how "do not move it" is said now
    // (backend#69); the path's start date identifies the window.
    @Test
    void updateMapsThePathAndTheItemsToTheCommandKeepingTheDatesItIsGivenAgain() throws Exception {
        when(updateCostCenterDistributionUseCase.update(any(UpdateCostCenterDistributionCommand.class)))
                .thenReturn(new CostCenterDistributionWindow(JAN_1, null, List.of(
                        new CostCenterAllocation(10L, "CC_ADMIN", new BigDecimal("70"), JAN_1, null),
                        new CostCenterAllocation(10L, "CC_HR", new BigDecimal("30"), JAN_1, null)
                )));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions/2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-01-01",
                                 "items":[{"costCenterCode":"CC_ADMIN","allocationPercentage":70},
                                          {"costCenterCode":"CC_HR","allocationPercentage":30}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAllocationPercentage").value(100))
                .andExpect(jsonPath("$.items[0].costCenterCode").value("CC_ADMIN"))
                .andExpect(jsonPath("$.items[0].allocationPercentage").value(70));

        ArgumentCaptor<UpdateCostCenterDistributionCommand> captor =
                ArgumentCaptor.forClass(UpdateCostCenterDistributionCommand.class);
        verify(updateCostCenterDistributionUseCase).update(captor.capture());
        assertEquals(JAN_1, captor.getValue().windowStartDate());
        assertEquals(JAN_1, captor.getValue().startDate());
        assertNull(captor.getValue().endDate());
        assertEquals(2, captor.getValue().items().size());
    }

    // The rejection has to be loud, and 400 is the loud one: a body that
    // forgot the start date is the client's mistake, not a clash with the
    // series. Before backend#69 that body was legal and got a 200 back with
    // the window left where it was.
    @Test
    void aCorrectionWithoutAStartDateIsAnHttp400NamingTheField() throws Exception {
        when(updateCostCenterDistributionUseCase.update(any(UpdateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterDistributionInvalidException("startDate is required"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions/2026-01-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"costCenterCode":"CC_ADMIN","allocationPercentage":100}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COST_CENTER_INVALID_WINDOW"))
                .andExpect(jsonPath("$.message").value(containsString("startDate")));
    }

    // The handler keeps the translation of the component's gap rejection (backend#58). This series
    // declares optional coverage (ADR-057), so no endpoint produces it today; the mapping is what is tested.
    @Test
    void updateMapsACoverageGapToHttp409NamingTheGapAndTheNeighboursToStretch() throws Exception {
        when(updateCostCenterDistributionUseCase.update(any(UpdateCostCenterDistributionCommand.class)))
                .thenThrow(new CostCenterDistributionCoverageGapException(
                        "ESP", "INTERNAL", "EMP001",
                        List.of(new CostCenterDistributionPeriod(JAN_16, LocalDate.of(2026, 1, 31))),
                        List.of(new CostCenterDistributionPeriod(JAN_1, JAN_15))
                ));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions/2026-01-16")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate":"2026-02-01",
                                 "items":[{"costCenterCode":"CC_HR","allocationPercentage":100}]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COST_CENTER_COVERAGE_GAP"))
                .andExpect(jsonPath("$.details.gaps[0].startDate").exists())
                .andExpect(jsonPath("$.details.stretchCandidates[0].startDate").exists());
    }

    @Test
    void deleteMapsThePathToTheCommandAndReturnsHttp204() throws Exception {
        doNothing().when(deleteCostCenterDistributionUseCase).delete(any(DeleteCostCenterDistributionCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions/2026-01-16"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<DeleteCostCenterDistributionCommand> captor =
                ArgumentCaptor.forClass(DeleteCostCenterDistributionCommand.class);
        verify(deleteCostCenterDistributionUseCase).delete(captor.capture());
        assertEquals(JAN_16, captor.getValue().windowStartDate());
        assertEquals("EMP001", captor.getValue().employeeNumber());
    }

    @Test
    void deleteMapsNotFoundToHttp404WithACode() throws Exception {
        doThrow(new CostCenterDistributionNotFoundException("ESP", "INTERNAL", "EMP001", JAN_16))
                .when(deleteCostCenterDistributionUseCase)
                .delete(any(DeleteCostCenterDistributionCommand.class));

        mockMvc.perform(delete("/employees/ESP/INTERNAL/EMP001/cost-centers/distributions/2026-01-16"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COST_CENTER_DISTRIBUTION_NOT_FOUND"));
    }

    // ADR-057 §6: the plan is asked for and shown; nothing is applied.
    @Test
    void planMapsTheRequestToTheCommandAndReturnsThePlanWithoutApplyingIt() throws Exception {
        when(planCostCenterDistributionChangeUseCase.plan(any(PlanCostCenterDistributionChangeCommand.class)))
                .thenReturn(new CostCenterDistributionPlan(
                        TimelineOperation.ADD,
                        null,
                        new CostCenterDistributionPeriod(JAN_16, null),
                        null,
                        new CostCenterDistributionPlanAdjustment(
                                new CostCenterDistributionPeriod(JAN_1, null),
                                new CostCenterDistributionPeriod(JAN_1, JAN_15)
                        ),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new CostCenterDistributionPeriod(JAN_1, JAN_15), new CostCenterDistributionPeriod(JAN_16, null))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/cost-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation":"ADD","startDate":"2026-01-16"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operation").value("ADD"))
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.rejection").isEmpty())
                .andExpect(jsonPath("$.adjustedOccurrence.after.startDate").exists())
                .andExpect(jsonPath("$.projected.length()").value(2));

        ArgumentCaptor<PlanCostCenterDistributionChangeCommand> captor =
                ArgumentCaptor.forClass(PlanCostCenterDistributionChangeCommand.class);
        verify(planCostCenterDistributionChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.ADD, captor.getValue().operation());
        assertEquals(JAN_16, captor.getValue().startDate());
        assertNull(captor.getValue().windowStartDate());
    }

    @Test
    void planIdentifiesTheWindowToRemoveByItsStartDateAndTellsTheScreenWhatAnAddWouldCorrect() throws Exception {
        when(planCostCenterDistributionChangeUseCase.plan(any(PlanCostCenterDistributionChangeCommand.class)))
                .thenReturn(new CostCenterDistributionPlan(
                        TimelineOperation.CORRECT,
                        TimelineRejection.IS_A_CORRECTION,
                        new CostCenterDistributionPeriod(JAN_1, JAN_15),
                        new CostCenterDistributionPeriod(JAN_1, null),
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new CostCenterDistributionPeriod(JAN_1, JAN_15))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/cost-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation":"REMOVE","windowStartDate":"2026-01-01"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.rejection").value("IS_A_CORRECTION"))
                .andExpect(jsonPath("$.operation").value("CORRECT"))
                .andExpect(jsonPath("$.correctedOccurrence.startDate").exists());

        ArgumentCaptor<PlanCostCenterDistributionChangeCommand> captor =
                ArgumentCaptor.forClass(PlanCostCenterDistributionChangeCommand.class);
        verify(planCostCenterDistributionChangeUseCase).plan(captor.capture());
        assertEquals(TimelineOperation.REMOVE, captor.getValue().operation());
        assertEquals(JAN_1, captor.getValue().windowStartDate());
    }

    @Test
    void planRejectsAnUnknownOperation() throws Exception {
        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/cost-centers/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation":"REPLACE","startDate":"2026-01-16"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentKeepsTheResponseShapeAndResolvesNamesInTheWebLayer() throws Exception {
        when(getCurrentCostCenterDistributionUseCase.getCurrent(any(GetCurrentCostCenterDistributionQuery.class)))
                .thenReturn(new CostCenterDistributionReadModel.CurrentDistribution(
                        "ESP", "INTERNAL", "EMP001",
                        new CostCenterDistributionReadModel.Window(
                                LocalDate.of(2026, 1, 1), null, new BigDecimal("100"),
                                List.of(
                                        new CostCenterDistributionReadModel.Item("CC_ADMIN", new BigDecimal("60")),
                                        new CostCenterDistributionReadModel.Item("CC_IT", new BigDecimal("40"))
                                )
                        )
                ));
        when(ruleEntityLabelResolver.resolveName("ESP", "COST_CENTER", "CC_ADMIN", "es-ES"))
                .thenReturn(Optional.of("Administración"));
        when(ruleEntityLabelResolver.resolveName("ESP", "COST_CENTER", "CC_IT", "es-ES"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/cost-centers/current")
                        .header("Accept-Language", "es-ES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.employee.employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.employee.employeeNumber").value("EMP001"))
                // La fecha se aserta como presencia: el MockMvc standalone no carga la
                // configuracion Jackson de Spring Boot y no serializa LocalDate en ISO.
                .andExpect(jsonPath("$.currentDistribution.startDate").exists())
                .andExpect(jsonPath("$.currentDistribution.totalAllocationPercentage").value(100))
                .andExpect(jsonPath("$.currentDistribution.items[0].costCenterCode").value("CC_ADMIN"))
                .andExpect(jsonPath("$.currentDistribution.items[0].costCenterName").value("Administración"))
                .andExpect(jsonPath("$.currentDistribution.items[0].allocationPercentage").value(60))
                .andExpect(jsonPath("$.currentDistribution.items[1].costCenterCode").value("CC_IT"))
                .andExpect(jsonPath("$.currentDistribution.items[1].costCenterName").isEmpty());
    }

    @Test
    void listHistoryKeepsTheResponseShape() throws Exception {
        when(listCostCenterDistributionHistoryUseCase.listHistory(any(ListCostCenterDistributionHistoryQuery.class)))
                .thenReturn(new CostCenterDistributionReadModel.History(
                        "ESP", "INTERNAL", "EMP001",
                        List.of(new CostCenterDistributionReadModel.Window(
                                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), new BigDecimal("100"),
                                List.of(new CostCenterDistributionReadModel.Item("CC_ADMIN", new BigDecimal("100")))
                        ))
                ));
        when(ruleEntityLabelResolver.resolveName("ESP", "COST_CENTER", "CC_ADMIN", null))
                .thenReturn(Optional.of("Administration"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/cost-centers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.windows[0].startDate").exists())
                .andExpect(jsonPath("$.windows[0].endDate").exists())
                .andExpect(jsonPath("$.windows[0].items[0].costCenterCode").value("CC_ADMIN"))
                .andExpect(jsonPath("$.windows[0].items[0].costCenterName").value("Administration"))
                .andExpect(jsonPath("$.windows[0].items[0].allocationPercentage").value(100));
    }
}
