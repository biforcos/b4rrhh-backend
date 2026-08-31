package com.b4rrhh.employee.cost_center.infrastructure.web;

import com.b4rrhh.employee.cost_center.application.usecase.CloseCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.CostCenterDistributionReadModel;
import com.b4rrhh.employee.cost_center.application.usecase.CreateCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionQuery;
import com.b4rrhh.employee.cost_center.application.usecase.GetCurrentCostCenterDistributionUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryQuery;
import com.b4rrhh.employee.cost_center.application.usecase.ListCostCenterDistributionHistoryUseCase;
import com.b4rrhh.employee.cost_center.application.usecase.ReplaceCostCenterDistributionFromDateUseCase;
import com.b4rrhh.employee.cost_center.infrastructure.web.assembler.CostCenterResponseAssembler;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fija la forma de la respuesta de las lecturas de centro de coste tras backend#27: los
 * mismos campos con los mismos nombres que cuando el literal lo ponía el caso de uso; lo
 * único que cambia es que ahora lo rellena el assembler en la capa web, con el idioma de
 * la respuesta.
 */
@ExtendWith(MockitoExtension.class)
class CostCenterBusinessKeyControllerHttpTest {

    @Mock
    private CreateCostCenterDistributionUseCase createCostCenterDistributionUseCase;
    @Mock
    private GetCurrentCostCenterDistributionUseCase getCurrentCostCenterDistributionUseCase;
    @Mock
    private ListCostCenterDistributionHistoryUseCase listCostCenterDistributionHistoryUseCase;
    @Mock
    private ReplaceCostCenterDistributionFromDateUseCase replaceCostCenterDistributionFromDateUseCase;
    @Mock
    private CloseCostCenterDistributionUseCase closeCostCenterDistributionUseCase;
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
                                replaceCostCenterDistributionFromDateUseCase,
                                closeCostCenterDistributionUseCase,
                                new CostCenterResponseAssembler(ruleEntityLabelResolver)
                        )
                )
                .setControllerAdvice(new CostCenterExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
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
