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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAddressByBusinessKeyServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmployeeAddressLookupPort employeeAddressLookupPort;

    private GetAddressByBusinessKeyService service;

    @BeforeEach
    void setUp() {
        service = new GetAddressByBusinessKeyService(addressRepository, employeeAddressLookupPort);
    }

    @Test
    void getsAddressByBusinessKeyAndAddressNumber() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1))
                .thenReturn(Optional.of(address(20L, 10L, 1)));

        Optional<Address> result = service.getByBusinessKey(" esp ", " internal ", " EMP001 ", 1);

        assertTrue(result.isPresent());
        assertEquals(20L, result.get().getId());
        assertEquals(1, result.get().getAddressNumber());
    }

    @Test
    void returnsEmptyWhenAddressNumberDoesNotExist() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 9)).thenReturn(Optional.empty());

        Optional<Address> result = service.getByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, 9);

        assertTrue(result.isEmpty());
    }

    @Test
    void throwsWhenEmployeeBusinessKeyDoesNotExist() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                AddressEmployeeNotFoundException.class,
                () -> service.getByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, 1)
        );
    }

    private EmployeeAddressContext employeeContext(Long employeeId) {
        return new EmployeeAddressContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
    }

    private Address address(Long id, Long employeeId, Integer addressNumber) {
        return new Address(
                id,
                employeeId,
                addressNumber,
                "HOME",
                DireccionesDePrueba.address("Calle Mayor 10", "Madrid", "ESP", "28013"),
                LocalDate.of(2026, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
