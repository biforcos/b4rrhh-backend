package com.b4rrhh.employee.employee.application.usecase;

import com.b4rrhh.employee.employee.domain.exception.EmployeeNotFoundException;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.domain.port.EmployeeRepository;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RuleSystemRepository ruleSystemRepository;

    private UpdateEmployeeService service;

    @BeforeEach
    void setUp() {
        service = new UpdateEmployeeService(employeeRepository, ruleSystemRepository);
    }

    @Test
    void updatesEmployeeIdentityFieldsSuccessfully() {
        Employee existing = employee(
                10L,
                "ESP",
                "INTERNAL",
                "EMP001",
                "Ana",
                "Lopez",
                null,
                null
        );

        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Employee updated = service.update(new UpdateEmployeeCommand(
                " esp ",
                " internal ",
                " EMP001 ",
                "  Alicia  ",
                "  Garcia  ",
                "  Perez  ",
                "  Ali  "
        ));

        assertEquals("Alicia", updated.getFirstName());
        assertEquals("Garcia", updated.getLastName1());
        assertEquals("Perez", updated.getLastName2());
        assertEquals("Ali", updated.getPreferredName());
    }

    @Test
    void throwsWhenEmployeeDoesNotExist() {
        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP001"))
                .thenReturn(Optional.empty());

        assertThrows(
                EmployeeNotFoundException.class,
                () -> service.update(new UpdateEmployeeCommand(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "Alicia",
                        "Garcia",
                        null,
                        null
                ))
        );
    }

    @Test
    void keepsBusinessKeyImmutableByDesign() {
        Employee existing = employee(
                11L,
                "ESP",
                "INTERNAL",
                "EMP009",
                "Ana",
                "Lopez",
                null,
                null
        );

        when(ruleSystemRepository.findByCode("ESP")).thenReturn(Optional.of(ruleSystem("ESP")));
        when(employeeRepository.findByRuleSystemCodeAndEmployeeTypeCodeAndEmployeeNumber("ESP", "INTERNAL", "EMP009"))
                .thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(new UpdateEmployeeCommand(
                "ESP",
                "INTERNAL",
                "EMP009",
                "Bea",
                "Ruiz",
                null,
                null
        ));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());

        Employee saved = captor.getValue();
        assertEquals("ESP", saved.getRuleSystemCode());
        assertEquals("INTERNAL", saved.getEmployeeTypeCode());
        assertEquals("EMP009", saved.getEmployeeNumber());
                assertEquals(11L, saved.getId());
    }

    private Employee employee(
            Long id,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            String firstName,
            String lastName1,
            String lastName2,
            String preferredName
    ) {
        return new Employee(
                id,
                ruleSystemCode,
                employeeTypeCode,
                employeeNumber,
                firstName,
                lastName1,
                lastName2,
                preferredName,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1),
                null
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