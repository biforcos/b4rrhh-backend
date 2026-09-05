package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The employee is present from 2026-01-01 onwards. HOME is the domicile
 * (mandatory); FISCAL is optional. Removing judges the series of the type
 * the removed address belongs to, and nothing else.
 */
@ExtendWith(MockitoExtension.class)
class DeleteAddressServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 1);

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmployeeAddressLookupPort employeeAddressLookupPort;
    @Mock
    private AddressPresenceLookupPort presencePort;
    @Mock
    private AddressTypeCoverageLookupPort coveragePort;

    private DeleteAddressService service;

    @BeforeEach
    void setUp() {
        service = new DeleteAddressService(
                addressRepository,
                employeeAddressLookupPort,
                new AddressTimelineService(addressRepository, presencePort, coveragePort)
        );
    }

    @Test
    void deletingTheLastOneReopensThePreviousOne() {
        Address first = address(1, "HOME", PRESENCE_START, LocalDate.of(2026, 1, 15));
        Address last = address(2, "HOME", LocalDate.of(2026, 1, 16), null);
        whenEmployeeExists();
        givenSeries("HOME", TimelineCoverage.MANDATORY, first, last);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 2)).thenReturn(Optional.of(last));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(first));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(command(2));

        ArgumentCaptor<Address> reopened = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(reopened.capture());
        assertEquals(1, reopened.getValue().getAddressNumber());
        assertNull(reopened.getValue().getEndDate());
        verify(addressRepository).delete(last);
    }

    @Test
    void deletingTheOnlyDomicileIsRejectedBecauseThePresenceWouldBeUncovered() {
        Address only = address(1, "HOME", PRESENCE_START, null);
        whenEmployeeExists();
        givenSeries("HOME", TimelineCoverage.MANDATORY, only);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(only));

        AddressCoverageGapException ex = assertThrows(AddressCoverageGapException.class, () -> service.delete(command(1)));

        assertEquals(List.of(new AddressPeriod(PRESENCE_START, null)), ex.gaps());
        verify(addressRepository, never()).delete(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void deletingTheOnlyFiscalAddressIsFineBecauseItsCoverageIsOptional() {
        Address only = address(3, "FISCAL", PRESENCE_START, null);
        whenEmployeeExists();
        givenSeries("FISCAL", TimelineCoverage.OPTIONAL, only);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 3)).thenReturn(Optional.of(only));

        service.delete(command(3));

        verify(addressRepository).delete(only);
        verify(addressRepository, never()).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME");
    }

    @Test
    void deletingOneInTheMiddleOfTheDomicileIsRejectedNamingTheNeighboursToStretch() {
        Address first = address(1, "HOME", PRESENCE_START, LocalDate.of(2026, 1, 15));
        Address middle = address(2, "HOME", LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31));
        Address last = address(3, "HOME", LocalDate.of(2026, 2, 1), null);
        whenEmployeeExists();
        givenSeries("HOME", TimelineCoverage.MANDATORY, first, middle, last);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 2)).thenReturn(Optional.of(middle));

        AddressCoverageGapException ex = assertThrows(AddressCoverageGapException.class, () -> service.delete(command(2)));

        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 31))), ex.gaps());
        assertEquals(
                List.of(
                        new AddressOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 15)),
                        new AddressOccurrence(3, LocalDate.of(2026, 2, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(addressRepository, never()).delete(any());
        verify(addressRepository, never()).save(any());
    }

    @Test
    void rejectsWhenTheAddressDoesNotExist() {
        whenEmployeeExists();
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 7)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.delete(command(7)));
        verify(addressRepository, never()).delete(any());
    }

    private void whenEmployeeExists() {
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeAddressContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private void givenSeries(String addressTypeCode, TimelineCoverage coverage, Address... existing) {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, addressTypeCode))
                .thenReturn(List.of(existing));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, addressTypeCode)).thenReturn(Optional.of(coverage));
    }

    private static DeleteAddressCommand command(int addressNumber) {
        return new DeleteAddressCommand(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, addressNumber);
    }

    private static Address address(int number, String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new Address(
                (long) number, 10L, number, addressTypeCode, "Calle Mayor " + number, "Madrid", "ESP", "28013", "MD",
                startDate, endDate, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
