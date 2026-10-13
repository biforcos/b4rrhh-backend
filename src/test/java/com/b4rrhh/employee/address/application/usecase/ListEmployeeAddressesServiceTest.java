package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeeAddressesServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmployeeAddressLookupPort employeeAddressLookupPort;

    private ListEmployeeAddressesService service;

    @BeforeEach
    void setUp() {
        service = new ListEmployeeAddressesService(addressRepository, employeeAddressLookupPort);
    }

    @Test
    void listsEmployeeAddressHistoryByBusinessKey() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));

        List<Address> expected = List.of(
                address(1L, 10L, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)),
                address(2L, 10L, 2, LocalDate.of(2026, 1, 16), null)
        );
        when(addressRepository.findByEmployeeIdOrderByStartDate(10L)).thenReturn(expected);

        List<Address> result = service.listByEmployeeBusinessKey(" esp ", " internal ", " EMP001 ");

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getAddressNumber());
        assertEquals(LocalDate.of(2026, 1, 1), result.get(0).getStartDate());
        assertEquals(2, result.get(1).getAddressNumber());
        assertEquals(LocalDate.of(2026, 1, 16), result.get(1).getStartDate());
    }

    @Test
    void rejectsListWhenEmployeeDoesNotExist() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                AddressEmployeeNotFoundException.class,
                () -> service.listByEmployeeBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)
        );
    }

    private EmployeeAddressContext employeeContext(Long employeeId) {
        return new EmployeeAddressContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
    }

    private Address address(Long id, Long employeeId, Integer number, LocalDate startDate, LocalDate endDate) {
        return new Address(
                id,
                employeeId,
                number,
                "HOME",
                DireccionesDePrueba.address("Calle Mayor 10", "Madrid", "ESP", "28013"),
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
