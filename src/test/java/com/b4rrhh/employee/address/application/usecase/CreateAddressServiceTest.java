package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressCatalogValidator;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.exception.AddressOverlapException;
import com.b4rrhh.employee.address.domain.exception.AddressTypeCoverageNotDeclaredException;
import com.b4rrhh.employee.address.domain.exception.InvalidAddressDateRangeException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.rulesystem.domain.model.RuleSystem;
import com.b4rrhh.rulesystem.domain.port.RuleSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The employee is present from 2026-01-10 onwards. HOME is the domicile
 * (mandatory coverage); MAILING and FISCAL are optional. Each type is its own
 * series, so the use case only ever loads the addresses of the type it adds.
 */
@ExtendWith(MockitoExtension.class)
class CreateAddressServiceTest {

    private static final String RULE_SYSTEM_CODE = "ESP";
    private static final String EMPLOYEE_TYPE_CODE = "INTERNAL";
    private static final String EMPLOYEE_NUMBER = "EMP001";
    private static final LocalDate PRESENCE_START = LocalDate.of(2026, 1, 10);

    @Mock
    private AddressRepository addressRepository;
    @Mock
    private EmployeeAddressLookupPort employeeAddressLookupPort;
    @Mock
    private RuleSystemRepository ruleSystemRepository;
    @Mock
    private AddressPresenceLookupPort presencePort;
    @Mock
    private AddressTypeCoverageLookupPort coveragePort;
    private AddressCatalogValidator addressCatalogValidator;

    private CreateAddressService service;

    @BeforeEach
    void setUp() {
        addressCatalogValidator = new TestAddressCatalogValidator();
        service = new CreateAddressService(
                addressRepository,
                employeeAddressLookupPort,
                ruleSystemRepository,
                addressCatalogValidator,
                new AddressTimelineService(addressRepository, presencePort, coveragePort)
        );
    }

    @Test
    void createsAddressAndAssignsNextAddressNumber() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "home",
                "Calle Mayor 10",
                "Madrid",
                "esp",
                "28013",
                "md",
                LocalDate.of(2026, 1, 10),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(2));
        givenSeries("HOME", TimelineCoverage.MANDATORY);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address input = invocation.getArgument(0);
            return new Address(
                    99L,
                    input.getEmployeeId(),
                    input.getAddressNumber(),
                    input.getAddressTypeCode(),
                    input.getStreet(),
                    input.getCity(),
                    input.getCountryCode(),
                    input.getPostalCode(),
                    input.getRegionCode(),
                    input.getStartDate(),
                    input.getEndDate(),
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
        });

        Address created = service.create(command);

        assertEquals(99L, created.getId());
        assertEquals(3, created.getAddressNumber());
        assertEquals("HOME", created.getAddressTypeCode());
        assertEquals("ESP", created.getCountryCode());

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getAddressNumber());

        InOrder inOrder = inOrder(employeeAddressLookupPort, addressRepository);
        inOrder.verify(employeeAddressLookupPort)
                .findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
        inOrder.verify(addressRepository).findMaxAddressNumberByEmployeeId(10L);
        inOrder.verify(addressRepository).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME");
        verify(addressRepository, never()).existsOverlappingPeriodByAddressType(any(), any(), any(), any());
    }

    @Test
    void createsAddressWithSameStartAndEndDate() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "FISCAL",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                "28013",
                "MD",
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 10)
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.empty());
        givenSeries("FISCAL", TimelineCoverage.OPTIONAL);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = service.create(command);

        assertEquals(LocalDate.of(2026, 1, 10), created.getStartDate());
        assertEquals(LocalDate.of(2026, 1, 10), created.getEndDate());
    }

    // ADR-057, decision 2: adding from the 1st closes the one in force on the 31st instead of returning a conflict.
    @Test
    void addingAfterTheOpenOneOfTheSameTypeClosesItTheDayBefore() {
        CreateAddressCommand command = command("HOME", LocalDate.of(2026, 2, 1), null);
        Address open = address(1, "HOME", PRESENCE_START, null);

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        givenSeries("HOME", TimelineCoverage.MANDATORY, open);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(open));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = service.create(command);

        assertEquals(2, created.getAddressNumber());
        ArgumentCaptor<Address> saved = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository, times(2)).save(saved.capture());
        Address closed = saved.getAllValues().get(0);
        assertEquals(1, closed.getAddressNumber());
        assertEquals(LocalDate.of(2026, 1, 31), closed.getEndDate());
        assertNull(saved.getAllValues().get(1).getEndDate());
    }

    @Test
    void rejectsAnAddressThatReachesIntoAnotherOfTheSameTypeAsAnOverlap() {
        CreateAddressCommand command = command("HOME", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 10));

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(2));
        givenSeries(
                "HOME",
                TimelineCoverage.MANDATORY,
                address(1, "HOME", PRESENCE_START, LocalDate.of(2026, 1, 31)),
                address(2, "HOME", LocalDate.of(2026, 2, 1), null)
        );

        AddressOverlapException ex = assertThrows(AddressOverlapException.class, () -> service.create(command));

        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))), ex.overlaps());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void rejectsADomicileThatLeavesThePresenceUncoveredNamingTheGap() {
        CreateAddressCommand command = command("HOME", LocalDate.of(2026, 3, 1), null);

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        givenSeries("HOME", TimelineCoverage.MANDATORY, address(1, "HOME", PRESENCE_START, LocalDate.of(2026, 1, 31)));

        AddressCoverageGapException ex = assertThrows(AddressCoverageGapException.class, () -> service.create(command));

        assertEquals(List.of(new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28))), ex.gaps());
        assertEquals(
                List.of(
                        new AddressOccurrence(1, PRESENCE_START, LocalDate.of(2026, 1, 31)),
                        new AddressOccurrence(null, LocalDate.of(2026, 3, 1), null)
                ),
                ex.stretchCandidates()
        );
        verify(addressRepository, never()).save(any(Address.class));
    }

    // Each type is its own series (ADR-057, decision 0): a MAILING address never sees the HOME ones.
    @Test
    void allowsCreateForDifferentAddressTypeWithoutLookingAtTheOtherTypes() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "MAILING",
                "Apartado 123",
                "Madrid",
                "ESP",
                "28013",
                "MD",
                LocalDate.of(2026, 3, 1),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(3));
        givenSeries("MAILING", TimelineCoverage.OPTIONAL);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = service.create(command);

        assertEquals("MAILING", created.getAddressTypeCode());
        assertEquals(4, created.getAddressNumber());
        verify(addressRepository, never()).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME");
        verify(addressRepository, never()).findByEmployeeIdOrderByStartDate(10L);
    }

    @Test
    void allowsSameAddressTypeAfterPreviousPeriodClosedWithoutOverlap() {
        CreateAddressCommand command = command("HOME", LocalDate.of(2026, 2, 1), null);

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(4));
        givenSeries("HOME", TimelineCoverage.MANDATORY, address(4, "HOME", PRESENCE_START, LocalDate.of(2026, 1, 31)));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Address created = service.create(command);

        assertEquals("HOME", created.getAddressTypeCode());
        assertEquals(5, created.getAddressNumber());
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    // backend#58: an add on the start date of an existing one of the type is its correction, and persists nothing.
    @Test
    void rejectsAddingOnTheStartDateOfAnExistingOneOfTheTypeAsItsCorrection() {
        CreateAddressCommand command = command("HOME", PRESENCE_START, null);

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        givenSeries("HOME", TimelineCoverage.MANDATORY, address(1, "HOME", PRESENCE_START, null));

        AddressIsACorrectionException ex = assertThrows(AddressIsACorrectionException.class, () -> service.create(command));

        assertEquals(new AddressOccurrence(1, PRESENCE_START, null), ex.correctedOccurrence());
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void rejectsATypeWhoseCoverageTheCatalogDoesNotDeclare() {
        CreateAddressCommand command = command("TEMPORARY", LocalDate.of(2026, 3, 1), null);

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.of(1));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, "TEMPORARY")).thenReturn(Optional.empty());

        assertThrows(AddressTypeCoverageNotDeclaredException.class, () -> service.create(command));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void rejectsInvalidCatalogValue() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "bad",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                null,
                null,
                LocalDate.of(2026, 1, 10),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));

        assertThrows(AddressCatalogValueInvalidException.class, () -> service.create(command));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void rejectsInvalidCountryCatalogValue() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "HOME",
                "Calle Mayor 10",
                "Madrid",
                "bad_country",
                null,
                null,
                LocalDate.of(2026, 1, 10),
                null
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));

        assertThrows(AddressCatalogValueInvalidException.class, () -> service.create(command));
        verify(addressRepository, never()).save(any(Address.class));
    }

    @Test
    void rejectsInvalidDateRangeWhenEndDateIsBeforeStartDate() {
        CreateAddressCommand command = new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                "HOME",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                null,
                null,
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 9)
        );

        when(ruleSystemRepository.findByCode(RULE_SYSTEM_CODE)).thenReturn(Optional.of(ruleSystem(RULE_SYSTEM_CODE)));
        when(employeeAddressLookupPort.findByBusinessKeyForUpdate(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(employeeContext(10L)));
        when(addressRepository.findMaxAddressNumberByEmployeeId(10L)).thenReturn(Optional.empty());

        assertThrows(InvalidAddressDateRangeException.class, () -> service.create(command));
        verify(addressRepository, never()).save(any(Address.class));
    }

    private EmployeeAddressContext employeeContext(Long employeeId) {
        return new EmployeeAddressContext(employeeId, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER);
    }

    /** The series of that type as the component will see it: its addresses, the presence, its coverage. */
    private void givenSeries(String addressTypeCode, TimelineCoverage coverage, Address... existing) {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, addressTypeCode))
                .thenReturn(List.of(existing));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, addressTypeCode)).thenReturn(Optional.of(coverage));
    }

    private static CreateAddressCommand command(String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new CreateAddressCommand(
                RULE_SYSTEM_CODE,
                EMPLOYEE_TYPE_CODE,
                EMPLOYEE_NUMBER,
                addressTypeCode,
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                null,
                null,
                startDate,
                endDate
        );
    }

    private static Address address(int number, String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new Address(
                (long) number,
                10L,
                number,
                addressTypeCode,
                "Calle Mayor " + number,
                "Madrid",
                "ESP",
                "28013",
                "MD",
                startDate,
                endDate,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private static final class TestAddressCatalogValidator extends AddressCatalogValidator {

        private TestAddressCatalogValidator() {
            super(null);
        }

        @Override
        public void validateAddressTypeCode(String ruleSystemCode, String addressTypeCode, LocalDate referenceDate) {
            if ("BAD".equals(addressTypeCode)) {
                throw new AddressCatalogValueInvalidException("addressTypeCode", addressTypeCode);
            }
        }

        @Override
        public void validateCountryCode(String ruleSystemCode, String countryCode, LocalDate referenceDate) {
            if ("BAD_COUNTRY".equals(countryCode)) {
                throw new AddressCatalogValueInvalidException("countryCode", countryCode);
            }
        }

        @Override
        public String normalizeRequiredCode(String fieldName, String value) {
            if (value == null || value.trim().isEmpty()) {
                throw new AddressCatalogValueInvalidException(fieldName, String.valueOf(value));
            }

            return value.trim().toUpperCase();
        }
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
