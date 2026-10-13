package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressCatalogValidator;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.exception.InvalidAddressDateRangeException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateAddressServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmployeeAddressLookupPort employeeAddressLookupPort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private AddressPresenceLookupPort presencePort;
    @Mock
    private AddressTypeCoverageLookupPort coveragePort;

    private UpdateAddressService service;

    @BeforeEach
    void setUp() {
        service = new UpdateAddressService(
                addressRepository,
                employeeAddressLookupPort,
                ruleSystemRepository,
                DireccionesDePrueba.validacionQueNoMiraElCatalogo(),
                new AddressTimelineService(addressRepository, presencePort, coveragePort)
        );
    }

    @Test
    void correctsTheDatesWhenTheyAreGiven() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input("Calle Mayor 10", "Madrid", "ESP", "28013"),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 6, 30)
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(existingAddress()));
        // The employee left on 2026-06-30: closing the domicile that day leaves no gap.
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME"))
                .thenReturn(List.of(existingAddress()));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 6, 30))));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, "HOME")).thenReturn(Optional.of(TimelineCoverage.MANDATORY));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address updated = service.update(command);

        assertEquals(LocalDate.of(2026, 1, 10), updated.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 30), updated.getEndDate());
        assertEquals(1, updated.getAddressNumber());
    }

    @Test
    void rejectsCorrectedDatesThatLeaveTheDomicileUncovered() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input("Calle Mayor 10", "Madrid", "ESP", "28013"),
                LocalDate.of(2026, 2, 1),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(existingAddress()));
        givenTheHomeSeriesOf(existingAddress());

        AddressCoverageGapException ex = assertThrows(AddressCoverageGapException.class, () -> service.update(command));

        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 31))), ex.gaps());
        verify(addressRepository, never()).save(any(Address.class));
    }

    /** The employee is present from the day the existing domicile starts, so leaving its dates alone is always accepted. */
    private void givenTheHomeSeriesOf(Address... occurrences) {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME"))
                .thenReturn(List.of(occurrences));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(LocalDate.of(2026, 1, 10), null)));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, "HOME")).thenReturn(Optional.of(TimelineCoverage.MANDATORY));
    }

    @Test
    void updatesAddressByBusinessKeyWithoutMutatingIdentity() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input("Calle de Alcala 100", "Madrid", "esp", "28009"),
                LocalDate.of(2026, 1, 10),
                null
        );

        Address existing = existingAddress();

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(existing));
        givenTheHomeSeriesOf(existingAddress());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address updated = service.update(command);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address savedAddress = captor.getValue();

        assertEquals(existing.getEmployeeId(), savedAddress.getEmployeeId());
        assertEquals(existing.getAddressNumber(), savedAddress.getAddressNumber());
        assertEquals(existing.getAddressTypeCode(), savedAddress.getAddressTypeCode());
        assertEquals(existing.getStartDate(), savedAddress.getStartDate());

        assertEquals("Calle de Alcala 100", savedAddress.getAddress().streetName());
        assertEquals("Madrid", savedAddress.getAddress().locality());
        assertEquals("ESP", savedAddress.getAddress().countryCode());
        assertEquals("28009", savedAddress.getAddress().postalCode());

        assertEquals(existing.getAddressNumber(), updated.getAddressNumber());
        assertEquals(existing.getAddressTypeCode(), updated.getAddressTypeCode());
        assertEquals(existing.getStartDate(), updated.getStartDate());
    }

    // The correction is the whole address, not a patch (backend#155): with thirteen fields, merging
    // what arrives empty with what was there left no way to clear one. What is not sent is empty.
    @Test
    void theCorrectionReplacesTheWholeAddress() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input(null, null, "ESP", null),
                LocalDate.of(2026, 1, 10),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(existingAddress()));
        givenTheHomeSeriesOf(existingAddress());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address updated = service.update(command);

        assertEquals("ESP", updated.getAddress().countryCode());
        assertNull(updated.getAddress().streetName());
        assertNull(updated.getAddress().locality());
        assertNull(updated.getAddress().postalCode());
    }

    // A body without a start date used to mean "leave both dates as they
    // are", which is what a client that forgot to send it looks like too. The
    // two arrived identical and both got a 200, so a screen could stop moving
    // the start and nobody would hear about it — three of them did
    // (backend#69). Now the silence is a rejection, and nothing is written.
    @Test
    void rejectsACorrectionThatDoesNotSayWhereTheAddressStarts() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input("Calle de Alcala 100", "Madrid", "ESP", "28009"),
                null,
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(existingAddress()));

        InvalidAddressDateRangeException rejected = assertThrows(
                InvalidAddressDateRangeException.class,
                () -> service.update(command)
        );

        assertTrue(rejected.getMessage().contains("startDate"));
        verify(addressRepository, never()).save(any(Address.class));
    }

    // The body is not even looked at: an address that is not there is a 404,
    // whatever the body says or leaves out.
    @Test
    void throwsNotFoundWhenEmployeeDoesNotExist() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                1,
                DireccionesDePrueba.input("Calle de Alcala 100", "Madrid", "ESP", "28009"),
                null,
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(AddressEmployeeNotFoundException.class, () -> service.update(command));
    }

    @Test
    void throwsNotFoundWhenAddressNumberDoesNotExistForEmployee() {
        UpdateAddressCommand command = new UpdateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                9,
                DireccionesDePrueba.input("Calle de Alcala 100", "Madrid", "ESP", "28009"),
                null,
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem()));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 9)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.update(command));
    }

    private Address existingAddress() {
        return new Address(
                20L,
                10L,
                1,
                "HOME",
                DireccionesDePrueba.address("Calle Mayor 10", "Madrid", "ESP", "28013"),
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private com.b4rrhh.rulesystem.domain.model.RuleEntity activeCountryRuleEntity() {
        return new com.b4rrhh.rulesystem.domain.model.RuleEntity(
                1L,
                RULE_SYSTEM_CODE,
                AddressRuleEntityTypeCodes.COUNTRY,
                "ESP",
                "Spain",
                null,
                true,
                LocalDate.of(1900, 1, 1),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private EmployeeAddressContext employeeContext(Long employeeId) {
        return new EmployeeAddressContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
    }

    private RuleSystem ruleSystem() {
        return new RuleSystem(
                1L,
                RULE_SYSTEM_CODE,
                "Spain",
                "ESP",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
