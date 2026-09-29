package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;

    private CreateEmployeeService service;

    @BeforeEach
    void setUp() {
        service = new CreateEmployeeService(employeeRepository);
    }

    @Test
    void createsEmployeeSuccessfully() {
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "ORD", "00001"))
                .thenReturn(Optional.empty());
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.create(new CreateEmployeeCommand("ESP", "ORD", "00001", "Juan", "García", null, null));

        assertEquals("ESP", result.getRuleSystemCode());
        assertEquals("ORD", result.getEmployeeTypeCode());
        assertEquals("00001", result.getEmployeeNumber());
        assertEquals("Juan", result.getFirstName());
        assertEquals("García", result.getLastName1());
        verify(employeeRepository).save(any());
    }

    @Test
    void failsWhenEmployeeWithSameBusinessKeyAlreadyExists() {
        Employee existing = new Employee(1L, "ESP", "ORD", "00001", "Juan", "García", null, null, java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), null);
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "ORD", "00001"))
                .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () ->
                service.create(new CreateEmployeeCommand("ESP", "ORD", "00001", "Juan", "García", null, null)));

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void persistsOptionalFields() {
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.create(
                new CreateEmployeeCommand("ESP", "ORD", "00001", "Juan", "García", "López", "Juanito"));

        assertEquals("López", result.getLastName2());
        assertEquals("Juanito", result.getPreferredName());
    }
}
