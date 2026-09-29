package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.exception.EmployeeRuleSystemNotFoundException;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeByBusinessKeyServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RuleSystemRepository ruleSystemRepository;

    private GetEmployeeByBusinessKeyService service;

    @BeforeEach
    void setUp() {
        service = new GetEmployeeByBusinessKeyService(employeeRepository, ruleSystemRepository);
    }

    @Test
    void findsEmployeeByBusinessKey() {
        Employee employee = new Employee(
                10L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "Juan",
                "Perez",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(employee));

        Optional<Employee> result = service.getByBusinessKey(" esp ", " internal ", " EMP001 ");

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
        assertEquals("INTERNAL", result.get().getEmployeeTypeCode());
        assertEquals("EMP001", result.get().getEmployeeNumber());
    }

    @Test
    void throwsWhenRuleSystemDoesNotExist() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.empty());

        assertThrows(
                EmployeeRuleSystemNotFoundException.class,
                () -> service.getByBusinessKey("ESP", "INTERNAL", "EMP001")
        );
    }

    private RuleSystem ruleSystem(String code) {
        return new RuleSystem(
                1L,
                code,
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
