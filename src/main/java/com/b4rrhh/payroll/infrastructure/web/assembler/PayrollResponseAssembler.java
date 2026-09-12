package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.domain.model.Payroll;
import com.b4rrhh.payroll.domain.model.PayrollContextSnapshot;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollAgreementProfileResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollCompanyProfileResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollConceptResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollContextSnapshotResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollEmployeeProfileResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollSummaryResponse;
import com.b4rrhh.payroll.infrastructure.web.dto.PayrollWarningResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PayrollResponseAssembler {

    private static final String COMPANY_DATA = "COMPANY_DATA";
    private static final String EMPLOYEE_DATA = "EMPLOYEE_DATA";
    private static final String AGREEMENT_DATA = "AGREEMENT_DATA";
    private static final String EMPLOYEE_PAYROLL_CONTEXT = "EMPLOYEE_PAYROLL_CONTEXT";
    private static final String WORK_CENTER_DATA = "WORK_CENTER_DATA";

    private final ObjectMapper objectMapper;

    public PayrollResponseAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PayrollResponse toResponse(Payroll payroll) {
        List<PayrollContextSnapshot> snapshots = payroll.getContextSnapshots();
        return new PayrollResponse(
                payroll.getRuleSystemCode(),
                payroll.getEmployeeTypeCode(),
                payroll.getEmployeeNumber(),
                payroll.getPayrollPeriodCode(),
                payroll.getPayrollTypeCode(),
                payroll.getPresenceNumber(),
                payroll.getStatus(),
                payroll.getStatusReasonCode(),
                payroll.getCalculatedAt(),
                payroll.getCalculationEngineCode(),
                payroll.getCalculationEngineVersion(),
                payroll.getRunId(),
                payroll.getWarnings().stream()
                        .map(warning -> new PayrollWarningResponse(
                                warning.warningCode(),
                                warning.severityCode(),
                                warning.message(),
                                warning.detailsJson()
                        ))
                        .toList(),
                payroll.getConcepts().stream()
                        .map(concept -> new PayrollConceptResponse(
                                concept.getLineNumber(),
                                concept.getConceptCode(),
                                concept.getConceptLabel(),
                                concept.getAmount(),
                                concept.getQuantity(),
                                concept.getRate(),
                                concept.getConceptNatureCode(),
                                concept.getOriginPeriodCode(),
                                concept.getDisplayOrder()
                        ))
                        .toList(),
                snapshots.stream()
                        .map(snapshot -> new PayrollContextSnapshotResponse(
                                snapshot.getSnapshotTypeCode(),
                                snapshot.getSourceVerticalCode(),
                                snapshot.getSourceBusinessKeyJson(),
                                snapshot.getSnapshotPayloadJson()
                        ))
                        .toList(),
                extractCompanyProfile(snapshots),
                extractEmployeeProfile(snapshots),
                extractAgreementProfile(snapshots),
                extractPresenceStartDate(snapshots),
                extractPresenceEndDate(snapshots),
                extractWorkCenterCode(snapshots),
                extractWorkCenterName(snapshots)
        );
    }

    public PayrollSummaryResponse toSummaryResponse(Payroll payroll) {
        return new PayrollSummaryResponse(
                payroll.getRuleSystemCode(),
                payroll.getEmployeeTypeCode(),
                payroll.getEmployeeNumber(),
                payroll.getPayrollPeriodCode(),
                payroll.getPayrollTypeCode(),
                payroll.getPresenceNumber(),
                payroll.getStatus().name(),
                payroll.getCalculatedAt()
        );
    }

    private PayrollCompanyProfileResponse extractCompanyProfile(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> COMPANY_DATA.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> parseCompanyProfile(s.getSnapshotPayloadJson()))
                .orElse(null);
    }

    private PayrollEmployeeProfileResponse extractEmployeeProfile(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> EMPLOYEE_DATA.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> parseEmployeeProfile(s.getSnapshotPayloadJson()))
                .orElse(null);
    }

    private PayrollCompanyProfileResponse parseCompanyProfile(String json) {
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new PayrollCompanyProfileResponse(
                    map.get("legalName"),
                    map.get("taxIdentifier"),
                    map.get("street"),
                    map.get("city"),
                    map.get("postalCode")
            );
        } catch (Exception e) {
            return null;
        }
    }

    private PayrollEmployeeProfileResponse parseEmployeeProfile(String json) {
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new PayrollEmployeeProfileResponse(
                    map.get("fullName"),
                    map.get("nif"),
                    map.get("street"),
                    map.get("city"),
                    map.get("postalCode")
            );
        } catch (Exception e) {
            return null;
        }
    }

    private PayrollAgreementProfileResponse extractAgreementProfile(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> AGREEMENT_DATA.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> parseAgreementProfile(s.getSnapshotPayloadJson()))
                .orElse(null);
    }

    private PayrollAgreementProfileResponse parseAgreementProfile(String json) {
        try {
            Map<String, String> map = objectMapper.readValue(json, new TypeReference<>() {});
            return new PayrollAgreementProfileResponse(
                    map.get("officialAgreementNumber"),
                    map.get("displayName"),
                    map.get("shortName"),
                    map.get("annualHours"),
                    map.get("agreementCategoryCode")
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPresenceStartDate(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> EMPLOYEE_PAYROLL_CONTEXT.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> extractStringField(s.getSnapshotPayloadJson(), "presenceStartDate"))
                .orElse(null);
    }

    private String extractPresenceEndDate(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> EMPLOYEE_PAYROLL_CONTEXT.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> extractStringField(s.getSnapshotPayloadJson(), "presenceEndDate"))
                .orElse(null);
    }

    private String extractWorkCenterCode(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> WORK_CENTER_DATA.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> extractStringField(s.getSnapshotPayloadJson(), "workCenterCode"))
                .orElse(null);
    }

    private String extractWorkCenterName(List<PayrollContextSnapshot> snapshots) {
        return snapshots.stream()
                .filter(s -> WORK_CENTER_DATA.equals(s.getSnapshotTypeCode()))
                .findFirst()
                .map(s -> extractStringField(s.getSnapshotPayloadJson(), "workCenterName"))
                .orElse(null);
    }

    private String extractStringField(String json, String field) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object value = map.get(field);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
