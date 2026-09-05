package com.b4rrhh.payroll.application.usecase;

import com.b4rrhh.payroll.application.port.AgreementProfileLookupPort;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoContext;
import com.b4rrhh.payroll.application.port.EmployeeTaxInfoPayrollLookupPort;
import com.b4rrhh.rulesystem.agreementcategoryprofile.application.usecase.GetAgreementCategoryProfileQuery;
import com.b4rrhh.rulesystem.agreementcategoryprofile.application.usecase.GetAgreementCategoryProfileUseCase;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.model.AgreementCategoryProfile;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.model.TipoNomina;
import com.b4rrhh.payroll.application.port.CompanyProfileLookupPort;
import com.b4rrhh.payroll.application.port.EmployeePersonalDataLookupPort;
import com.b4rrhh.payroll.application.port.EmployeePayrollInputLookupPort;
import com.b4rrhh.payroll.application.port.WorkCenterProfileLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputContext;
import com.b4rrhh.payroll.application.port.PayrollLaunchEligibleInputLookupPort;
import com.b4rrhh.payroll.application.port.PayrollLaunchWorkingTimeWindowContext;
import com.b4rrhh.payroll.application.service.PayrollConceptExecutionResult;
import com.b4rrhh.payroll.application.service.PayrollConceptGraphCalculator;
import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollStatus;
import com.b4rrhh.payroll.infrastructure.config.PayrollLaunchExecutionProperties;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.OperandRole;
import com.b4rrhh.payroll_engine.concept.domain.model.ResultCompositionMode;
import com.b4rrhh.payroll_engine.dependency.domain.model.ConceptNodeIdentity;
import com.b4rrhh.payroll_engine.execution.application.service.AccrualDaysConceptCalculator;
import com.b4rrhh.payroll_engine.execution.application.service.DefaultSegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.application.service.GreatestConceptResolver;
import com.b4rrhh.payroll_engine.execution.application.service.LeastConceptResolver;
import com.b4rrhh.payroll_engine.execution.application.service.PercentageConceptResolver;
import com.b4rrhh.payroll_engine.execution.application.service.RateByQuantityOperandResolver;
import com.b4rrhh.payroll_engine.execution.application.service.SegmentExecutionEngine;
import com.b4rrhh.payroll_engine.execution.application.service.SegmentTechnicalValueResolver;
import com.b4rrhh.payroll_engine.execution.application.service.TechnicalConceptCalculatorRegistry;
import com.b4rrhh.payroll_engine.execution.application.service.WorkingTimeConceptCalculator;
import com.b4rrhh.payroll_engine.execution.domain.model.ConceptExecutionPlanEntry;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.planning.application.service.BuildEligibleExecutionPlanUseCase;
import com.b4rrhh.payroll_engine.planning.domain.model.EligibleExecutionPlanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculatePayrollUnitServiceTest {

    @Mock
    private CalculatePayrollUseCase calculatePayrollUseCase;
    @Mock
    private PayrollLaunchEligibleInputLookupPort payrollLaunchEligibleInputLookupPort;
    @Mock
    private PayrollConceptGraphCalculator payrollConceptGraphCalculator;
    @Mock
    private BuildEligibleExecutionPlanUseCase buildEligibleExecutionPlanUseCase;
    @Mock
    private CompanyProfileLookupPort companyProfileLookupPort;
    @Mock
    private EmployeePersonalDataLookupPort employeePersonalDataLookupPort;
    @Mock
    private AgreementProfileLookupPort agreementProfileLookupPort;
    @Mock
    private WorkCenterProfileLookupPort workCenterProfileLookupPort;
    @Mock
    private EmployeePayrollInputLookupPort employeePayrollInputLookupPort;
    @Mock
    private GetAgreementCategoryProfileUseCase getAgreementCategoryProfileUseCase;
    @Mock
    private EmployeeTaxInfoPayrollLookupPort employeeTaxInfoLookupPort;
    // Motor real, no mock: lo que se prueba aqui es como el servicio reparte cada concepto
    // entre tramos y periodo segun su execution_scope, y eso solo se ve evaluando de verdad.
    private final SegmentExecutionEngine segmentExecutionEngine = new DefaultSegmentExecutionEngine(
            new SegmentTechnicalValueResolver(),
            new RateByQuantityOperandResolver(),
            new PercentageConceptResolver(),
            new GreatestConceptResolver(),
            new LeastConceptResolver(),
            new TechnicalConceptCalculatorRegistry(List.of(
                    new AccrualDaysConceptCalculator(),
                    new WorkingTimeConceptCalculator())));

    @Test
        void eligibleRealMode_persistsSingleConcept101FromMinimalExecutor() {
        PayrollLaunchExecutionProperties properties = new PayrollLaunchExecutionProperties();

        CalculatePayrollUnitService service = new CalculatePayrollUnitService(
            calculatePayrollUseCase,
            payrollLaunchEligibleInputLookupPort,
            properties,
            payrollConceptGraphCalculator,
            buildEligibleExecutionPlanUseCase,
            companyProfileLookupPort,
            employeePersonalDataLookupPort,
            agreementProfileLookupPort,
            workCenterProfileLookupPort,
            segmentExecutionEngine,
            employeePayrollInputLookupPort,
            getAgreementCategoryProfileUseCase,
            employeeTaxInfoLookupPort
        );

        when(employeeTaxInfoLookupPort.findLatestOnOrBefore(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(EmployeeTaxInfoContext.ofDefault());

        when(payrollLaunchEligibleInputLookupPort.findByUnitAndPeriod(
            "ESP", "INTERNAL", "EMP001", 2,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
        )).thenReturn(Optional.of(new PayrollLaunchEligibleInputContext(
            "ES01",
                        "99002405011982",
                        "99002405-G2",
            List.of(new PayrollLaunchWorkingTimeWindowContext(
                LocalDate.of(2025, 1, 1),
                null,
                new BigDecimal("100")
            )),
            LocalDate.of(2025, 1, 1),
            null,
            null
        )));

        // Pre-computation of DIRECT_AMOUNT during the pre-compute pass
        when(payrollConceptGraphCalculator.calculateConceptResult(org.mockito.ArgumentMatchers.eq("101"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PayrollConceptExecutionResult(
                        "101",
                        new BigDecimal("1425.00"),
                        null,
                        null
                ));
        when(calculatePayrollUseCase.calculate(org.mockito.ArgumentMatchers.any(CalculatePayrollCommand.class)))
            .thenReturn(payroll());

        PayrollObject obj101 = new PayrollObject(1L, "ESP", PayrollObjectTypeCode.CONCEPT, "101", null, null);
        com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept engineConcept101 =
                new com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept(
                        obj101, "SALARIO_BASE", CalculationType.DIRECT_AMOUNT, FunctionalNature.EARNING,
                        ResultCompositionMode.REPLACE, "101", ExecutionScope.PERIOD, true, null, null);
        ConceptExecutionPlanEntry entry101 = new ConceptExecutionPlanEntry(
                new ConceptNodeIdentity("ESP", "101"), CalculationType.DIRECT_AMOUNT);
        EligibleExecutionPlanResult planResult = new EligibleExecutionPlanResult(
                List.of(), List.of(), List.of(engineConcept101), null, List.of(entry101));
        when(buildEligibleExecutionPlanUseCase.build(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(planResult);
        when(getAgreementCategoryProfileUseCase.get(
                new GetAgreementCategoryProfileQuery("ESP", "99002405-G2")))
                .thenReturn(new AgreementCategoryProfile("05", TipoNomina.MENSUAL));

        when(employeePayrollInputLookupPort.findInputsByPeriod(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(Map.of());

        service.calculate(new CalculatePayrollUnitCommand(
            "ESP",
            "INTERNAL",
            "EMP001",
            "202501",
            "NORMAL",
            2,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31),
            "ENGINE",
            "1.0"
        ));

        ArgumentCaptor<CalculatePayrollCommand> captor = ArgumentCaptor.forClass(CalculatePayrollCommand.class);
        verify(calculatePayrollUseCase).calculate(captor.capture());

        CalculatePayrollCommand persisted = captor.getValue();
        assertEquals(1, persisted.concepts().size());
        assertEquals("101", persisted.concepts().getFirst().getConceptCode());
        assertEquals(0, new BigDecimal("1425.00").compareTo(persisted.concepts().getFirst().getAmount()));
        // quantity and rate are null for DIRECT_AMOUNT concepts
        verify(payrollConceptGraphCalculator, never()).calculateConceptResult(org.mockito.ArgumentMatchers.eq("D01"), org.mockito.ArgumentMatchers.any());
        verify(payrollConceptGraphCalculator, never()).calculateConceptResult(org.mockito.ArgumentMatchers.eq("P01"), org.mockito.ArgumentMatchers.any());
        }

        @Test
        void eligibleRealMode_missingAgreementCategory_throwsExplicitException() {
        PayrollLaunchExecutionProperties properties = new PayrollLaunchExecutionProperties();

        CalculatePayrollUnitService service = new CalculatePayrollUnitService(
            calculatePayrollUseCase,
            payrollLaunchEligibleInputLookupPort,
            properties,
            payrollConceptGraphCalculator,
            buildEligibleExecutionPlanUseCase,
            companyProfileLookupPort,
            employeePersonalDataLookupPort,
            agreementProfileLookupPort,
            workCenterProfileLookupPort,
            segmentExecutionEngine,
            employeePayrollInputLookupPort,
            getAgreementCategoryProfileUseCase,
            employeeTaxInfoLookupPort
        );

        lenient().when(employeeTaxInfoLookupPort.findLatestOnOrBefore(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(EmployeeTaxInfoContext.ofDefault());

        when(payrollLaunchEligibleInputLookupPort.findByUnitAndPeriod(
            "ESP", "INTERNAL", "EMP001", 2,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)
        )).thenReturn(Optional.of(new PayrollLaunchEligibleInputContext(
            "ES01",
                        "99002405011982",
            null,
            List.of(new PayrollLaunchWorkingTimeWindowContext(
                LocalDate.of(2025, 1, 1),
                null,
                new BigDecimal("100")
            )),
            LocalDate.of(2025, 1, 1),
            null,
            null
        )));

        PayrollLaunchInputMissingException ex = assertThrows(PayrollLaunchInputMissingException.class, () ->
            service.calculate(new CalculatePayrollUnitCommand(
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                2,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31),
                "ENGINE",
                "1.0"
            ))
        );

        assertEquals("AGREEMENT_CATEGORY_MISSING", ex.getReasonCode());
        verifyNoInteractions(payrollConceptGraphCalculator);
    }

    // Jornada partida a mitad de mes: 100 % del 1 al 15 y 50 % del 16 al 30, con precio
    // pleno 47,50. El mismo concepto, SALARIO_BASE = DIAS_DEVENGO x PRECIO_DIA, declarado
    // en SEGMENT o en PERIOD, tiene que dar recibos distintos (ADR-058, backend#64).

    @Test
    void splitWorkingTime_salarioBaseInSegmentScope_yieldsOneRowPerSegment() {
        CalculatePayrollCommand persisted = calculateSplitMonth(ExecutionScope.SEGMENT);

        List<com.b4rrhh.payroll.domain.model.PayrollConcept> rows = persisted.concepts();
        assertEquals(2, rows.size(), "un tramo por jornada, con precio distinto");

        assertEquals("101", rows.get(0).getConceptCode());
        assertEquals(0, new BigDecimal("712.50").compareTo(rows.get(0).getAmount()));
        assertEquals(0, new BigDecimal("15").compareTo(rows.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("47.50").compareTo(rows.get(0).getRate()));

        assertEquals("101", rows.get(1).getConceptCode());
        assertEquals(0, new BigDecimal("356.25").compareTo(rows.get(1).getAmount()));
        assertEquals(0, new BigDecimal("15").compareTo(rows.get(1).getQuantity()));
        assertEquals(0, new BigDecimal("23.75").compareTo(rows.get(1).getRate()));
    }

    @Test
    void splitWorkingTime_salarioBaseInPeriodScope_yieldsOneRowOverTheWholePeriod() {
        CalculatePayrollCommand persisted = calculateSplitMonth(ExecutionScope.PERIOD);

        List<com.b4rrhh.payroll.domain.model.PayrollConcept> rows = persisted.concepts();
        assertEquals(1, rows.size(), "la regla esta definida sobre el periodo entero: una linea");

        // 30 dias devengados sobre el periodo, y la jornada del periodo es la media
        // ponderada por dias (75 %): 47,50 x 0,75 = 35,625 -> 35,63; 30 x 35,63 = 1068,90.
        assertEquals("101", rows.get(0).getConceptCode());
        assertEquals(0, new BigDecimal("1068.90").compareTo(rows.get(0).getAmount()));
        assertEquals(0, new BigDecimal("30").compareTo(rows.get(0).getQuantity()));
        assertEquals(0, new BigDecimal("35.63").compareTo(rows.get(0).getRate()));
    }

    /**
     * Calcula septiembre de 2026 con la jornada partida y devuelve el comando persistido.
     * {@code salarioBaseChainScope} se aplica a los cuatro conceptos de la cadena del salario
     * base (101, D01, J01, P01): un PERIOD no puede leer un operando SEGMENT (ADR-058), asi
     * que el ambito se cambia en bloque. P02 (precio pleno) es PERIOD en los dos casos.
     */
    private CalculatePayrollCommand calculateSplitMonth(ExecutionScope salarioBaseChainScope) {
        PayrollLaunchExecutionProperties properties = new PayrollLaunchExecutionProperties();
        properties.setCollapseSegmentRows(false);

        CalculatePayrollUnitService service = new CalculatePayrollUnitService(
            calculatePayrollUseCase,
            payrollLaunchEligibleInputLookupPort,
            properties,
            payrollConceptGraphCalculator,
            buildEligibleExecutionPlanUseCase,
            companyProfileLookupPort,
            employeePersonalDataLookupPort,
            agreementProfileLookupPort,
            workCenterProfileLookupPort,
            segmentExecutionEngine,
            employeePayrollInputLookupPort,
            getAgreementCategoryProfileUseCase,
            employeeTaxInfoLookupPort
        );

        when(employeeTaxInfoLookupPort.findLatestOnOrBefore(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(EmployeeTaxInfoContext.ofDefault());

        when(payrollLaunchEligibleInputLookupPort.findByUnitAndPeriod(
            "ESP", "INTERNAL", "EMP001", 1,
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)
        )).thenReturn(Optional.of(new PayrollLaunchEligibleInputContext(
            "ES01",
            "99002405011982",
            "99002405-G2",
            List.of(
                new PayrollLaunchWorkingTimeWindowContext(
                    LocalDate.of(2023, 2, 6), LocalDate.of(2026, 9, 15), new BigDecimal("100")),
                new PayrollLaunchWorkingTimeWindowContext(
                    LocalDate.of(2026, 9, 16), null, new BigDecimal("50"))
            ),
            LocalDate.of(2023, 2, 6),
            null,
            null
        )));

        when(payrollConceptGraphCalculator.calculateConceptResult(
                org.mockito.ArgumentMatchers.eq("P02"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PayrollConceptExecutionResult("P02", new BigDecimal("47.50"), null, null));
        when(calculatePayrollUseCase.calculate(org.mockito.ArgumentMatchers.any(CalculatePayrollCommand.class)))
            .thenReturn(payroll());
        when(getAgreementCategoryProfileUseCase.get(
                new GetAgreementCategoryProfileQuery("ESP", "99002405-G2")))
                .thenReturn(new AgreementCategoryProfile("05", TipoNomina.MENSUAL));
        when(employeePayrollInputLookupPort.findInputsByPeriod(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(Map.of());

        ConceptNodeIdentity d01 = new ConceptNodeIdentity("ESP", "D01");
        ConceptNodeIdentity j01 = new ConceptNodeIdentity("ESP", "J01");
        ConceptNodeIdentity p02 = new ConceptNodeIdentity("ESP", "P02");
        ConceptNodeIdentity p01 = new ConceptNodeIdentity("ESP", "P01");
        ConceptNodeIdentity c101 = new ConceptNodeIdentity("ESP", "101");
        List<ConceptExecutionPlanEntry> plan = List.of(
                new ConceptExecutionPlanEntry(d01, CalculationType.ENGINE_PROVIDED),
                new ConceptExecutionPlanEntry(j01, CalculationType.ENGINE_PROVIDED),
                new ConceptExecutionPlanEntry(p02, CalculationType.DIRECT_AMOUNT),
                new ConceptExecutionPlanEntry(p01, CalculationType.RATE_BY_QUANTITY,
                        Map.of(OperandRole.QUANTITY, j01, OperandRole.RATE, p02)),
                new ConceptExecutionPlanEntry(c101, CalculationType.RATE_BY_QUANTITY,
                        Map.of(OperandRole.QUANTITY, d01, OperandRole.RATE, p01))
        );
        List<com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept> concepts = List.of(
                engineConcept(1L, "D01", "DIAS_DEVENGO", CalculationType.ENGINE_PROVIDED,
                        FunctionalNature.TECHNICAL, null, salarioBaseChainScope),
                engineConcept(2L, "J01", "COEFICIENTE_JORNADA", CalculationType.ENGINE_PROVIDED,
                        FunctionalNature.TECHNICAL, null, salarioBaseChainScope),
                engineConcept(3L, "P02", "PRECIO_DIA_PLENO", CalculationType.DIRECT_AMOUNT,
                        FunctionalNature.BASE, null, ExecutionScope.PERIOD),
                engineConcept(4L, "P01", "PRECIO_DIA", CalculationType.RATE_BY_QUANTITY,
                        FunctionalNature.BASE, null, salarioBaseChainScope),
                engineConcept(5L, "101", "SALARIO_BASE", CalculationType.RATE_BY_QUANTITY,
                        FunctionalNature.EARNING, "101", salarioBaseChainScope)
        );
        when(buildEligibleExecutionPlanUseCase.build(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new EligibleExecutionPlanResult(List.of(), List.of(), concepts, null, plan));

        service.calculate(new CalculatePayrollUnitCommand(
            "ESP",
            "INTERNAL",
            "EMP001",
            "202609",
            "NORMAL",
            1,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 30),
            "ENGINE",
            "1.0"
        ));

        ArgumentCaptor<CalculatePayrollCommand> captor = ArgumentCaptor.forClass(CalculatePayrollCommand.class);
        verify(calculatePayrollUseCase).calculate(captor.capture());
        return captor.getValue();
    }

    private static com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept engineConcept(
            long objectId,
            String code,
            String mnemonic,
            CalculationType calculationType,
            FunctionalNature nature,
            String payslipOrderCode,
            ExecutionScope scope
    ) {
        return new com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept(
                new PayrollObject(objectId, "ESP", PayrollObjectTypeCode.CONCEPT, code, null, null),
                mnemonic, calculationType, nature, ResultCompositionMode.REPLACE,
                payslipOrderCode, scope, true, null, null);
    }

    private Payroll payroll() {
        return Payroll.rehydrate(
                1L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "202501",
                "NORMAL",
                2,
                PayrollStatus.CALCULATED,
                null,
                LocalDateTime.of(2026, 4, 11, 10, 0),
                "ENGINE",
                "1.0",
                List.of(),
                List.of(),
                LocalDateTime.of(2026, 4, 11, 10, 0),
                LocalDateTime.of(2026, 4, 11, 10, 0)
        );
    }
}
