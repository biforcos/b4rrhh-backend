package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.CreatePayrollConceptCommand;
import com.b4rrhh.payroll_engine.concept.domain.exception.PayrollConceptAlreadyExistsException;
import com.b4rrhh.payroll_engine.concept.domain.model.CalculationType;
import com.b4rrhh.payroll_engine.concept.domain.model.ExecutionScope;
import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObject;
import com.b4rrhh.payroll_engine.object.domain.model.PayrollObjectTypeCode;
import com.b4rrhh.payroll_engine.object.domain.port.PayrollObjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePayrollConceptServiceTest {

    @Mock
    private PayrollConceptRepository conceptRepository;

    @Mock
    private PayrollObjectRepository objectRepository;

    @InjectMocks
    private CreatePayrollConceptService service;

    @Test
    void createsConceptWhenCodeIsNew() {
        CreatePayrollConceptCommand command = new CreatePayrollConceptCommand(
                "ES", "201", "PLUS_TRANSPORTE",
                CalculationType.RATE_BY_QUANTITY, FunctionalNature.EARNING,
                ExecutionScope.SEGMENT, "20", null
        );
        when(conceptRepository.existsByBusinessKey("ES", "201")).thenReturn(false);
        when(objectRepository.save(any(PayrollObject.class))).thenAnswer(invocation -> {
            PayrollObject input = invocation.getArgument(0);
            return new PayrollObject(
                    42L,
                    input.getRuleSystemCode(),
                    input.getObjectTypeCode(),
                    input.getObjectCode(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        });
        when(conceptRepository.save(any(PayrollConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PayrollConcept result = service.create(command);

        ArgumentCaptor<PayrollObject> objectCaptor = ArgumentCaptor.forClass(PayrollObject.class);
        verify(objectRepository).save(objectCaptor.capture());
        PayrollObject savedObject = objectCaptor.getValue();
        assertThat(savedObject.getRuleSystemCode()).isEqualTo("ES");
        assertThat(savedObject.getObjectTypeCode()).isEqualTo(PayrollObjectTypeCode.CONCEPT);
        assertThat(savedObject.getObjectCode()).isEqualTo("201");

        ArgumentCaptor<PayrollConcept> conceptCaptor = ArgumentCaptor.forClass(PayrollConcept.class);
        verify(conceptRepository).save(conceptCaptor.capture());
        PayrollConcept savedConcept = conceptCaptor.getValue();
        assertThat(savedConcept.getRuleSystemCode()).isEqualTo("ES");
        assertThat(savedConcept.getConceptCode()).isEqualTo("201");
        assertThat(savedConcept.getConceptMnemonic()).isEqualTo("PLUS_TRANSPORTE");
        assertThat(savedConcept.getCalculationType()).isEqualTo(CalculationType.RATE_BY_QUANTITY);
        assertThat(savedConcept.getFunctionalNature()).isEqualTo(FunctionalNature.EARNING);
        assertThat(savedConcept.getExecutionScope()).isEqualTo(ExecutionScope.SEGMENT);
        assertThat(savedConcept.getPayslipOrderCode()).isEqualTo("20");
        assertThat(savedConcept.getObject().getId()).isEqualTo(42L);

        assertThat(result).isSameAs(savedConcept);
    }

    @Test
    void rejectsCreationWhenCodeAlreadyExists() {
        CreatePayrollConceptCommand command = new CreatePayrollConceptCommand(
                "ES", "101", "SALARIO_BASE",
                CalculationType.DIRECT_AMOUNT, FunctionalNature.EARNING,
                ExecutionScope.PERIOD, "10", null
        );
        when(conceptRepository.existsByBusinessKey("ES", "101")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(PayrollConceptAlreadyExistsException.class);

        verify(objectRepository, never()).save(any());
        verify(conceptRepository, never()).save(any());
    }
}
