package com.b4rrhh.employee.address.application.model;

import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
import com.b4rrhh.employee.temporal.support.TimelineOperation;
import com.b4rrhh.employee.temporal.support.TimelineRejection;

import java.util.List;

/**
 * What would happen to one address series of the employee if an operation
 * were applied (ADR-057), with every occurrence named by its number. The
 * series is the one of {@link #addressTypeCode()}: an address of another type
 * is never in it (ADR-057, decision 0). It is what the screen shows before
 * confirming and what the write use cases apply; it is never persisted.
 *
 * <ul>
 *   <li>{@link #occurrence()} is the one added, removed or corrected; on an
 *       add it has no number yet.</li>
 *   <li>{@link #correctedOccurrence()} is, on a correction, the address as it
 *       stands today, the one {@link #occurrence()} replaces. It is also how
 *       the plan tells the screen that an add on an existing start date is the
 *       correction of that address (backend#58). {@code null} on an add and
 *       on a removal.</li>
 *   <li>{@link #adjustedOccurrence()} is the only automatic consequence, or
 *       {@code null} when nothing else moves.</li>
 *   <li>{@link #stretchCandidates()} are the neighbours of the gaps: the
 *       addresses the user could stretch. They are named, never moved.</li>
 *   <li>{@link #projected()} is the series as it would be, accepted or not.</li>
 * </ul>
 */
public record AddressPlan(
        TimelineOperation operation,
        TimelineRejection rejection,
        String addressTypeCode,
        AddressOccurrence occurrence,
        AddressOccurrence correctedOccurrence,
        AddressPlanAdjustment adjustedOccurrence,
        List<AddressPeriod> overlaps,
        List<AddressPeriod> gaps,
        List<AddressOccurrence> stretchCandidates,
        List<AddressOccurrence> projected
) {

    public AddressPlan {
        if (operation == null) {
            throw new IllegalArgumentException("operation is required");
        }
        if (addressTypeCode == null || addressTypeCode.isBlank()) {
            throw new IllegalArgumentException("addressTypeCode is required");
        }
        if (occurrence == null) {
            throw new IllegalArgumentException("occurrence is required");
        }
        overlaps = List.copyOf(overlaps);
        gaps = List.copyOf(gaps);
        stretchCandidates = List.copyOf(stretchCandidates);
        projected = List.copyOf(projected);
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public boolean adjustsAnOccurrence() {
        return adjustedOccurrence != null;
    }
}
