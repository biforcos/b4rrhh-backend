package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.exception.EmployeeNotFoundException;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteEmployeeByBusinessKeyServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private DeleteEmployeeByBusinessKeyService service;

    @BeforeEach
    void setUp() {
        service = new DeleteEmployeeByBusinessKeyService(employeeRepository);
    }

    @Test
    void deletesResolvedEmployeeById() {
        Employee existing = employee(10L, "ESP", "INTERNAL", "EMP001");

        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(existing));

        service.delete(new DeleteEmployeeByBusinessKeyCommand(" esp ", " internal ", " EMP001 "));

        verify(employeeRepository).deleteById(10L);
    }

    @Test
    void throwsNotFoundWhenEmployeeDoesNotExist() {
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP404"))
                .thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> service.delete(new DeleteEmployeeByBusinessKeyCommand("ESP", "INTERNAL", "EMP404"))
        );

        verify(employeeRepository, never()).deleteById(anyLong());
    }

    private Employee employee(Long id, String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        return new Employee(
                id,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                "Ana",
                "Lopez",
                null,
                null,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
        );
    }
}
