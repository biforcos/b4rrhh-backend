package com.b4rrhh.employee.address.application.usecase;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.application.port.EmployeeAddressContext;
import com.b4rrhh.employee.address.application.port.EmployeeAddressLookupPort;
import com.b4rrhh.employee.address.application.service.AddressTimelineService;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The plan is asked for and nothing is written. The employee is present from
 * 2026-01-01 onwards with one open HOME address from that day.
 */
@ExtendWith(MockitoExtension.class)
class PlanAddressChangeServiceTest {

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

    private PlanAddressChangeService service;

    @BeforeEach
    void setUp() {
        service = new PlanAddressChangeService(
                addressRepository,
                employeeAddressLookupPort,
                new AddressTimelineService(addressRepository, presencePort, coveragePort)
        );
    }

    @Test
    void plansAnAddWithoutWritingAnything() {
        whenEmployeeExists();
        givenSeries("HOME", TimelineCoverage.MANDATORY, address(1, "HOME", PRESENCE_START, null));

        AddressPlan plan = service.plan(command(TimelineOperation.ADD, "home", null, LocalDate.of(2026, 1, 16), null));

        assertTrue(plan.isAccepted());
        assertEquals(TimelineOperation.ADD, plan.operation());
        assertEquals("HOME", plan.addressTypeCode());
        assertEquals(1, plan.adjustedOccurrence().addressNumber());
        assertEquals(LocalDate.of(2026, 1, 15), plan.adjustedOccurrence().after().endDate());
        verify(addressRepository, never()).save(any());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    void aRejectedPlanComesBackAsAPlanNotAsAnError() {
        Address only = address(1, "HOME", PRESENCE_START, null);
        whenEmployeeExists();
        givenSeries("HOME", TimelineCoverage.MANDATORY, only);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 1)).thenReturn(Optional.of(only));

        AddressPlan plan = service.plan(command(TimelineOperation.REMOVE, null, 1, null, null));

        assertFalse(plan.isAccepted());
        assertEquals(TimelineRejection.GAP_NOT_ALLOWED, plan.rejection());
        assertEquals(List.of(new AddressPeriod(PRESENCE_START, null)), plan.gaps());
        assertEquals(new AddressOccurrence(1, PRESENCE_START, null), plan.occurrence());
    }

    @Test
    void aCorrectionTakesTheSeriesFromTheAddressItIsAbout() {
        Address fiscal = address(2, "FISCAL", PRESENCE_START, null);
        whenEmployeeExists();
        givenSeries("FISCAL", TimelineCoverage.OPTIONAL, fiscal);
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 2)).thenReturn(Optional.of(fiscal));

        AddressPlan plan = service.plan(command(TimelineOperation.CORRECT, null, 2, LocalDate.of(2026, 3, 1), null));

        assertTrue(plan.isAccepted());
        assertEquals("FISCAL", plan.addressTypeCode());
        assertEquals(new AddressOccurrence(2, LocalDate.of(2026, 3, 1), null), plan.occurrence());
        assertEquals(List.of(new AddressPeriod(PRESENCE_START, LocalDate.of(2026, 2, 28))), plan.gaps());
        verify(addressRepository, never()).findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, "HOME");
    }

    @Test
    void anAddNeedsTheTypeOfTheSeries() {
        whenEmployeeExists();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.ADD, null, null, LocalDate.of(2026, 1, 16), null))
        );
    }

    @Test
    void aTypeThatContradictsTheAddressBeingCorrectedIsRejected() {
        Address fiscal = address(2, "FISCAL", PRESENCE_START, null);
        whenEmployeeExists();
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 2)).thenReturn(Optional.of(fiscal));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.plan(command(TimelineOperation.CORRECT, "HOME", 2, LocalDate.of(2026, 3, 1), null))
        );
    }

    @Test
    void rejectsWhenTheAddressToRemoveDoesNotExist() {
        whenEmployeeExists();
        when(addressRepository.findByEmployeeIdAndAddressNumber(10L, 7)).thenReturn(Optional.empty());

        assertThrows(AddressNotFoundException.class, () -> service.plan(command(TimelineOperation.REMOVE, null, 7, null, null)));
    }

    @Test
    void rejectsWhenTheEmployeeDoesNotExist() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.empty());

        assertThrows(
                AddressEmployeeNotFoundException.class,
                () -> service.plan(command(TimelineOperation.ADD, "HOME", null, LocalDate.of(2026, 1, 16), null))
        );
    }

    private void whenEmployeeExists() {
        when(employeeAddressLookupPort.findByBusinessKey(RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER))
                .thenReturn(Optional.of(new EmployeeAddressContext(10L, RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER)));
    }

    private void givenSeries(String addressTypeCode, TimelineCoverage coverage, Address... existing) {
        when(addressRepository.findByEmployeeIdAndAddressTypeCodeOrderByStartDate(10L, addressTypeCode))
                .thenReturn(List.of(existing));
        when(presencePort.findPresencePeriodsByEmployeeIdOrderByStartDate(10L))
                .thenReturn(List.of(new DateRange(PRESENCE_START, null)));
        when(coveragePort.findCoverage(RULE_SYSTEM_CODE, addressTypeCode)).thenReturn(Optional.of(coverage));
    }

    private static PlanAddressChangeCommand command(
            TimelineOperation operation,
            String addressTypeCode,
            Integer addressNumber,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new PlanAddressChangeCommand(
                RULE_SYSTEM_CODE, EMPLOYEE_TYPE_CODE, EMPLOYEE_NUMBER, operation, addressTypeCode, addressNumber, startDate, endDate
        );
    }

    private static Address address(int number, String addressTypeCode, LocalDate startDate, LocalDate endDate) {
        return new Address(
                (long) number, 10L, number, addressTypeCode, "Calle Mayor " + number, "Madrid", "ESP", "28013", "MD",
                startDate, endDate, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
