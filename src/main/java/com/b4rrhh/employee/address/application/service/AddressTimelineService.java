package com.b4rrhh.employee.address.application.service;

import com.b4rrhh.employee.address.application.model.AddressPlan;
import com.b4rrhh.employee.address.application.model.AddressPlanAdjustment;
import com.b4rrhh.employee.address.application.port.AddressPresenceLookupPort;
import com.b4rrhh.employee.address.application.port.AddressTypeCoverageLookupPort;
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.exception.AddressOverlapException;
import com.b4rrhh.employee.address.domain.exception.AddressTypeCoverageNotDeclaredException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.address.domain.port.AddressRepository;
import com.b4rrhh.employee.temporal.support.DateRange;
import com.b4rrhh.employee.temporal.support.Timeline;
import com.b4rrhh.employee.temporal.support.TimelineContainment;
import com.b4rrhh.employee.temporal.support.TimelineCoverage;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelinePlan;
import com.b4rrhh.employee.temporal.support.TimelinePlanner;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Where the address series meet the temporal component (ADR-057). There is
 * one series per employee <b>and type</b> (decision 0): the domicile, the
 * fiscal address and the mailing address live side by side and never judge
 * each other. The coverage of each series is what the catalog says about its
 * type (backend#53), and every series may outlive the presence: an address
 * does not expire because the employee leaves.
 *
 * <p>It plans and it judges; it never writes. The write use cases read the
 * plan and apply it, and the plan use case returns it as it is.
 */
@Component
public class AddressTimelineService {

    private static final TimelineContainment CONTAINMENT = TimelineContainment.MAY_OUTLIVE_PRESENCE;

    private final AddressRepository addressRepository;
    private final AddressPresenceLookupPort addressPresenceLookupPort;
    private final AddressTypeCoverageLookupPort addressTypeCoverageLookupPort;
    private final TimelinePlanner timelinePlanner = new TimelinePlanner();

    public AddressTimelineService(
            AddressRepository addressRepository,
            AddressPresenceLookupPort addressPresenceLookupPort,
            AddressTypeCoverageLookupPort addressTypeCoverageLookupPort
    ) {
        this.addressRepository = addressRepository;
        this.addressPresenceLookupPort = addressPresenceLookupPort;
        this.addressTypeCoverageLookupPort = addressTypeCoverageLookupPort;
    }

    public AddressPlan planAdd(Long employeeId, String ruleSystemCode, String addressTypeCode, DateRange occurrence) {
        Series series = load(employeeId, ruleSystemCode, addressTypeCode);
        TimelinePlan plan = timelinePlanner.planAdd(series.timeline(), occurrence);

        return toPlan(plan, series, null);
    }

    public AddressPlan planRemove(Long employeeId, String ruleSystemCode, Address occurrence) {
        Series series = load(employeeId, ruleSystemCode, occurrence.getAddressTypeCode());
        TimelinePlan plan = timelinePlanner.planRemove(series.timeline(), rangeOf(occurrence));

        return toPlan(plan, series, occurrence.getAddressNumber());
    }

    public AddressPlan planCorrect(Long employeeId, String ruleSystemCode, Address occurrence, DateRange corrected) {
        Series series = load(employeeId, ruleSystemCode, occurrence.getAddressTypeCode());
        TimelinePlan plan = timelinePlanner.planCorrect(series.timeline(), rangeOf(occurrence), corrected);

        return toPlan(plan, series, occurrence.getAddressNumber());
    }

    /**
     * Throws the business exception a rejected plan stands for. Accepted
     * plans pass through. The switch is an expression on purpose: a rejection
     * the component adds and this vertical does not translate stops compiling
     * instead of slipping through (backend#58).
     */
    public void requireAccepted(
            AddressPlan plan,
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        if (plan.isAccepted()) {
            return;
        }

        AddressOccurrence occurrence = plan.occurrence();
        throw switch (plan.rejection()) {
            case OUTSIDE_PRESENCE -> new IllegalStateException(
                    "An address series may outlive the presence and is never rejected as OUTSIDE_PRESENCE: "
                            + plan
            );
            case OVERLAP -> new AddressOverlapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.addressTypeCode(),
                    occurrence.startDate(),
                    occurrence.endDate(),
                    plan.overlaps()
            );
            case GAP_NOT_ALLOWED -> new AddressCoverageGapException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.addressTypeCode(),
                    plan.gaps(),
                    plan.stretchCandidates()
            );
            case IS_A_CORRECTION -> new AddressIsACorrectionException(
                    ruleSystemCode,
                    employeeTypeCode,
                    employeeNumber,
                    plan.addressTypeCode(),
                    plan.correctedOccurrence(),
                    new AddressPeriod(occurrence.startDate(), occurrence.endDate())
            );
        };
    }

    private Series load(Long employeeId, String ruleSystemCode, String addressTypeCode) {
        TimelineCoverage coverage = addressTypeCoverageLookupPort
                .findCoverage(ruleSystemCode, addressTypeCode)
                .orElseThrow(() -> new AddressTypeCoverageNotDeclaredException(ruleSystemCode, addressTypeCode));
        List<Address> occurrences = addressRepository
                .findByEmployeeIdAndAddressTypeCodeOrderByStartDate(employeeId, addressTypeCode);
        List<DateRange> presence = addressPresenceLookupPort
                .findPresencePeriodsByEmployeeIdOrderByStartDate(employeeId);

        Timeline timeline = new Timeline(
                coverage,
                CONTAINMENT,
                presence,
                occurrences.stream().map(AddressTimelineService::rangeOf).toList()
        );

        return new Series(addressTypeCode, occurrences, timeline);
    }

    /**
     * Gives the ranges of the plan their numbers. The numbers travel with the
     * ranges: an adjusted or corrected occurrence keeps its number under its
     * new dates, a removed one gives it up, and the one an add would create
     * is the only range left without one.
     *
     * <p>{@code subjectNumber} is the number of the occurrence the caller
     * asked about, when it asked about one. An add asks about none: if the
     * component answers that the add is a correction, the corrected
     * occurrence is the one whose start date it landed on, and its number is
     * found here (backend#58).
     */
    private AddressPlan toPlan(TimelinePlan plan, Series series, Integer subjectNumber) {
        Map<DateRange, Deque<Integer>> numbers = series.numbersByRange();

        AddressPlanAdjustment adjustment = null;
        if (plan.adjustsAnOccurrence()) {
            DateRange before = plan.adjustedOccurrence().before();
            DateRange after = plan.adjustedOccurrence().after();
            Integer adjustedNumber = take(numbers, before);
            give(numbers, after, adjustedNumber);
            adjustment = new AddressPlanAdjustment(adjustedNumber, toPeriod(before), toPeriod(after));
        }

        if (plan.operation() == TimelineOperation.REMOVE) {
            take(numbers, plan.occurrence());
        }

        AddressOccurrence correctedOccurrence = null;
        if (plan.operation() == TimelineOperation.CORRECT) {
            DateRange before = plan.correctedOccurrence();
            Integer numberUnderThoseDates = take(numbers, before);
            if (subjectNumber == null) {
                subjectNumber = numberUnderThoseDates;
            }
            give(numbers, plan.occurrence(), subjectNumber);
            correctedOccurrence = new AddressOccurrence(subjectNumber, before.startDate(), before.endDate());
        }

        List<AddressOccurrence> projected = new ArrayList<>();
        for (DateRange range : plan.projected()) {
            projected.add(new AddressOccurrence(take(numbers, range), range.startDate(), range.endDate()));
        }

        List<AddressOccurrence> stretchCandidates = plan.stretchCandidates().stream()
                .map(candidate -> projected.stream()
                        .filter(occurrence -> sameDates(occurrence, candidate))
                        .findFirst()
                        .orElseThrow())
                .toList();

        return new AddressPlan(
                plan.operation(),
                plan.rejection(),
                series.addressTypeCode(),
                new AddressOccurrence(subjectNumber, plan.occurrence().startDate(), plan.occurrence().endDate()),
                correctedOccurrence,
                adjustment,
                plan.overlaps().stream().map(AddressTimelineService::toPeriod).toList(),
                plan.gaps().stream().map(AddressTimelineService::toPeriod).toList(),
                stretchCandidates,
                projected
        );
    }

    private static boolean sameDates(AddressOccurrence occurrence, DateRange range) {
        return occurrence.startDate().equals(range.startDate())
                && Objects.equals(occurrence.endDate(), range.endDate());
    }

    private static Integer take(Map<DateRange, Deque<Integer>> numbers, DateRange range) {
        Deque<Integer> queue = numbers.get(range);
        return queue == null ? null : queue.pollFirst();
    }

    private static void give(Map<DateRange, Deque<Integer>> numbers, DateRange range, Integer number) {
        if (number != null) {
            numbers.computeIfAbsent(range, ignored -> new ArrayDeque<>()).addLast(number);
        }
    }

    private static DateRange rangeOf(Address address) {
        return new DateRange(address.getStartDate(), address.getEndDate());
    }

    private static AddressPeriod toPeriod(DateRange range) {
        return new AddressPeriod(range.startDate(), range.endDate());
    }

    private record Series(String addressTypeCode, List<Address> occurrences, Timeline timeline) {

        Map<DateRange, Deque<Integer>> numbersByRange() {
            Map<DateRange, Deque<Integer>> numbers = new HashMap<>();
            for (Address occurrence : occurrences) {
                give(numbers, rangeOf(occurrence), occurrence.getAddressNumber());
            }
            return numbers;
        }
    }
}
