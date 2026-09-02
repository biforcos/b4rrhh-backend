package com.b4rrhh.employee.journey.application.usecase;

import com.b4rrhh.employee.journey.application.port.EmployeeJourneyLookupPort;
import com.b4rrhh.employee.journey.application.port.JourneyContractReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyContractRecord;
import com.b4rrhh.employee.journey.application.port.JourneyEmployeeContext;
import com.b4rrhh.employee.journey.application.port.JourneyLaborClassificationReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyLaborClassificationRecord;
import com.b4rrhh.employee.journey.application.port.JourneyPresenceTimelineReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyPresenceTimelineRecord;
import com.b4rrhh.employee.journey.application.port.JourneyWorkCenterReadPort;
import com.b4rrhh.employee.journey.application.port.JourneyWorkCenterRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GetEmployeeJourneyV2Service implements GetEmployeeJourneyV2UseCase {

    private final EmployeeJourneyLookupPort employeeJourneyLookupPort;
    private final JourneyPresenceTimelineReadPort journeyPresenceTimelineReadPort;
    private final JourneyContractReadPort journeyContractReadPort;
    private final JourneyLaborClassificationReadPort journeyLaborClassificationReadPort;
    private final JourneyWorkCenterReadPort journeyWorkCenterReadPort;

    public GetEmployeeJourneyV2Service(
            EmployeeJourneyLookupPort employeeJourneyLookupPort,
            JourneyPresenceTimelineReadPort journeyPresenceTimelineReadPort,
            JourneyContractReadPort journeyContractReadPort,
            JourneyLaborClassificationReadPort journeyLaborClassificationReadPort,
            JourneyWorkCenterReadPort journeyWorkCenterReadPort
    ) {
        this.employeeJourneyLookupPort = employeeJourneyLookupPort;
        this.journeyPresenceTimelineReadPort = journeyPresenceTimelineReadPort;
        this.journeyContractReadPort = journeyContractReadPort;
        this.journeyLaborClassificationReadPort = journeyLaborClassificationReadPort;
        this.journeyWorkCenterReadPort = journeyWorkCenterReadPort;
    }

    @Override
    public EmployeeJourneyTimelineView get(GetEmployeeJourneyV2Command command) {
        String normalizedRuleSystemCode = normalizeRuleSystemCode(command.ruleSystemCode());
        String normalizedEmployeeTypeCode = normalizeEmployeeTypeCode(command.employeeTypeCode());
        String normalizedEmployeeNumber = normalizeEmployeeNumber(command.employeeNumber());

        JourneyEmployeeContext employee = employeeJourneyLookupPort
                .findByBusinessKey(normalizedRuleSystemCode, normalizedEmployeeTypeCode, normalizedEmployeeNumber)
                .orElseThrow(() -> new JourneyEmployeeNotFoundException(
                        normalizedRuleSystemCode,
                        normalizedEmployeeTypeCode,
                        normalizedEmployeeNumber
                ));

        List<EventCandidate> candidates = new ArrayList<>();
        long stableOrder = 0;
        LocalDate today = LocalDate.now();

        List<JourneyPresenceTimelineRecord> presences = journeyPresenceTimelineReadPort
                .findByEmployeeIdOrderByStartDate(employee.employeeId());
        stableOrder = appendPresenceEvents(candidates, stableOrder, presences, today);

        List<JourneyContractRecord> contracts = journeyContractReadPort
                .findByEmployeeIdOrderByStartDate(employee.employeeId());
        stableOrder = appendContractEvents(candidates, stableOrder, contracts, today);

        List<JourneyLaborClassificationRecord> laborClassifications = journeyLaborClassificationReadPort
                .findByEmployeeIdOrderByStartDate(employee.employeeId());
        stableOrder = appendLaborClassificationEvents(candidates, stableOrder, laborClassifications, today);

        List<JourneyWorkCenterRecord> workCenters = journeyWorkCenterReadPort
                .findByEmployeeIdOrderByStartDate(employee.employeeId());
        stableOrder = appendWorkCenterEvents(candidates, stableOrder, workCenters, today);

        List<JourneyEventView> orderedEvents = candidates.stream()
                .sorted(Comparator
                        .comparing((EventCandidate candidate) -> candidate.event().eventDate())
                        .thenComparingInt(EventCandidate::trackPriority)
                        .thenComparingLong(EventCandidate::stableOrder))
                .map(EventCandidate::event)
                .toList();

        JourneyEmployeeHeaderView header = new JourneyEmployeeHeaderView(
                employee.ruleSystemCode(),
                employee.employeeTypeCode(),
                employee.employeeNumber(),
                employee.displayName()
        );

        return new EmployeeJourneyTimelineView(header, orderedEvents);
    }

    private long appendPresenceEvents(
            List<EventCandidate> target,
            long stableOrder,
            List<JourneyPresenceTimelineRecord> presences,
            LocalDate today
    ) {
        for (int index = 0; index < presences.size(); index++) {
            JourneyPresenceTimelineRecord presence = presences.get(index);
            JourneyPresenceTimelineRecord previous = index == 0 ? null : presences.get(index - 1);
            int periodNumber = presence.presenceNumber() == null ? index + 1 : presence.presenceNumber();

            JourneyEventType startType;
            if (index == 0) {
                startType = JourneyEventType.HIRE;
            } else if (previous != null && previous.endDate() != null) {
                startType = JourneyEventType.REHIRE;
            } else {
                startType = JourneyEventType.PRESENCE_START;
            }

            boolean isCurrent = isPeriodCurrent(presence.startDate(), presence.endDate(), today);
            target.add(new EventCandidate(
                    new JourneyEventView(
                            presence.startDate(),
                            startType,
                            JourneyTrackCode.PRESENCE,
                            statusFor(presence.startDate(), isCurrent, today),
                            isCurrent,
                            presenceDetails(presence, periodNumber)
                    ),
                    trackPriority(JourneyTrackCode.PRESENCE),
                    stableOrder++
            ));

            if (presence.endDate() != null) {
                JourneyEventType endType = index == presences.size() - 1
                        ? JourneyEventType.TERMINATION
                        : JourneyEventType.PRESENCE_END;

                target.add(new EventCandidate(
                        new JourneyEventView(
                                presence.endDate(),
                                endType,
                                JourneyTrackCode.PRESENCE,
                                statusFor(presence.endDate(), false, today),
                                false,
                                presenceDetails(presence, periodNumber)
                        ),
                        trackPriority(JourneyTrackCode.PRESENCE),
                        stableOrder++
                ));
            }
        }

        return stableOrder;
    }

    private long appendContractEvents(
            List<EventCandidate> target,
            long stableOrder,
            List<JourneyContractRecord> contracts,
            LocalDate today
    ) {
        for (int index = 0; index < contracts.size(); index++) {
            JourneyContractRecord contract = contracts.get(index);
            JourneyEventType startType = index == 0
                    ? JourneyEventType.CONTRACT_START
                    : JourneyEventType.CONTRACT_CHANGE;
            boolean isCurrent = isPeriodCurrent(contract.startDate(), contract.endDate(), today);

            target.add(new EventCandidate(
                    new JourneyEventView(
                            contract.startDate(),
                            startType,
                            JourneyTrackCode.CONTRACT,
                            statusFor(contract.startDate(), isCurrent, today),
                            isCurrent,
                            contractDetails(contract)
                    ),
                    trackPriority(JourneyTrackCode.CONTRACT),
                    stableOrder++
            ));

            if (contract.endDate() != null) {
                target.add(new EventCandidate(
                        new JourneyEventView(
                                contract.endDate(),
                                JourneyEventType.CONTRACT_END,
                                JourneyTrackCode.CONTRACT,
                                statusFor(contract.endDate(), false, today),
                                false,
                                contractDetails(contract)
                        ),
                        trackPriority(JourneyTrackCode.CONTRACT),
                        stableOrder++
                ));
            }
        }

        return stableOrder;
    }

    private long appendLaborClassificationEvents(
            List<EventCandidate> target,
            long stableOrder,
            List<JourneyLaborClassificationRecord> laborClassifications,
            LocalDate today
    ) {
        for (int index = 0; index < laborClassifications.size(); index++) {
            JourneyLaborClassificationRecord laborClassification = laborClassifications.get(index);
            JourneyEventType startType = index == 0
                    ? JourneyEventType.LABOR_CLASSIFICATION_START
                    : JourneyEventType.LABOR_CLASSIFICATION_CHANGE;
            boolean isCurrent = isPeriodCurrent(
                    laborClassification.startDate(),
                    laborClassification.endDate(),
                    today
            );

            target.add(new EventCandidate(
                    new JourneyEventView(
                            laborClassification.startDate(),
                            startType,
                            JourneyTrackCode.LABOR_CLASSIFICATION,
                            statusFor(laborClassification.startDate(), isCurrent, today),
                            isCurrent,
                            laborClassificationDetails(laborClassification)
                    ),
                    trackPriority(JourneyTrackCode.LABOR_CLASSIFICATION),
                    stableOrder++
            ));

            if (laborClassification.endDate() != null) {
                target.add(new EventCandidate(
                        new JourneyEventView(
                                laborClassification.endDate(),
                                JourneyEventType.LABOR_CLASSIFICATION_END,
                                JourneyTrackCode.LABOR_CLASSIFICATION,
                                statusFor(laborClassification.endDate(), false, today),
                                false,
                                laborClassificationDetails(laborClassification)
                        ),
                        trackPriority(JourneyTrackCode.LABOR_CLASSIFICATION),
                        stableOrder++
                ));
            }
        }

        return stableOrder;
    }

    private long appendWorkCenterEvents(
            List<EventCandidate> target,
            long stableOrder,
            List<JourneyWorkCenterRecord> workCenters,
            LocalDate today
    ) {
        for (JourneyWorkCenterRecord workCenter : workCenters) {
            boolean isCurrent = isPeriodCurrent(workCenter.startDate(), workCenter.endDate(), today);

            target.add(new EventCandidate(
                    new JourneyEventView(
                            workCenter.startDate(),
                            JourneyEventType.WORK_CENTER_START,
                            JourneyTrackCode.WORK_CENTER,
                            statusFor(workCenter.startDate(), isCurrent, today),
                            isCurrent,
                            workCenterDetails(workCenter)
                    ),
                    trackPriority(JourneyTrackCode.WORK_CENTER),
                    stableOrder++
            ));

            if (workCenter.endDate() != null) {
                target.add(new EventCandidate(
                        new JourneyEventView(
                                workCenter.endDate(),
                                JourneyEventType.WORK_CENTER_END,
                                JourneyTrackCode.WORK_CENTER,
                                statusFor(workCenter.endDate(), false, today),
                                false,
                                workCenterDetails(workCenter)
                        ),
                        trackPriority(JourneyTrackCode.WORK_CENTER),
                        stableOrder++
                ));
            }
        }

        return stableOrder;
    }

    private Map<String, Object> presenceDetails(JourneyPresenceTimelineRecord presence, int periodNumber) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("companyCode", presence.companyCode());
        details.put("entryReasonCode", presence.entryReasonCode());
        details.put("exitReasonCode", presence.exitReasonCode());
        details.put("presenceNumber", periodNumber);
        return details;
    }

    private Map<String, Object> contractDetails(JourneyContractRecord contract) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("contractCode", contract.contractCode());
        details.put("contractSubtypeCode", contract.contractSubtypeCode());
        return details;
    }

    private Map<String, Object> laborClassificationDetails(JourneyLaborClassificationRecord laborClassification) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("agreementCode", laborClassification.agreementCode());
        details.put("agreementCategoryCode", laborClassification.agreementCategoryCode());
        return details;
    }

    private Map<String, Object> workCenterDetails(JourneyWorkCenterRecord workCenter) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("workCenterCode", workCenter.workCenterCode());
        details.put("workCenterAssignmentNumber", workCenter.workCenterAssignmentNumber());
        details.put("startDate", workCenter.startDate());
        if (workCenter.endDate() != null) {
            details.put("endDate", workCenter.endDate());
        }
        return details;
    }

    private JourneyEventStatus statusFor(LocalDate eventDate, boolean isCurrent, LocalDate today) {
        if (eventDate.isAfter(today)) {
            return JourneyEventStatus.FUTURE;
        }

        return isCurrent ? JourneyEventStatus.CURRENT : JourneyEventStatus.COMPLETED;
    }

    private boolean isPeriodCurrent(LocalDate startDate, LocalDate endDate, LocalDate today) {
        boolean started = !startDate.isAfter(today);
        boolean notEnded = endDate == null || !endDate.isBefore(today);
        return started && notEnded;
    }

    private int trackPriority(JourneyTrackCode trackCode) {
        return switch (trackCode) {
            case PRESENCE -> 0;
            case CONTRACT -> 1;
            case LABOR_CLASSIFICATION -> 2;
            case WORK_CENTER -> 3;
        };
    }

    private String normalizeRuleSystemCode(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }

        return ruleSystemCode.trim().toUpperCase();
    }

    private String normalizeEmployeeTypeCode(String employeeTypeCode) {
        if (employeeTypeCode == null || employeeTypeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeTypeCode is required");
        }

        return employeeTypeCode.trim().toUpperCase();
    }

    private String normalizeEmployeeNumber(String employeeNumber) {
        if (employeeNumber == null || employeeNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("employeeNumber is required");
        }

        return employeeNumber.trim();
    }

    private record EventCandidate(JourneyEventView event, int trackPriority, long stableOrder) {
    }
}