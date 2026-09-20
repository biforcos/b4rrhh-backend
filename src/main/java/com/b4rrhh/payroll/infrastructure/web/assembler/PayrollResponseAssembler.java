package com.b4rrhh.payroll.infrastructure.web.assembler;

import com.b4rrhh.payroll.application.service.PayrollSnapshotProfiles;
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
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PayrollResponseAssembler {

    /**
     * Quien lee las fotos del contexto, y lo hace una sola vez ({@code backend#112}).
     *
     * <p>Esta extraccion la pidio el PDF: es una segunda salida del mismo documento y tiene que
     * decir lo mismo que esta. Dos lectores del mismo JSON no lo garantizan.
     */
    private final PayrollSnapshotProfiles profiles;

    public PayrollResponseAssembler(PayrollSnapshotProfiles profiles) {
        this.profiles = profiles;
    }

    /**
     * @param rulesChangedSinceCalculation llega hecho de la capa de aplicación, porque no es un
     *                                     dato del recibo: es una comparación contra la
     *                                     reglamentación de hoy, y un ensamblador no consulta
     *                                     ({@code backend#107})
     */
    public PayrollResponse toResponse(Payroll payroll, boolean rulesChangedSinceCalculation) {
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
                                concept.getConceptMnemonic(),
                                concept.getConceptLabel(),
                                concept.getAmount(),
                                concept.getQuantity(),
                                concept.getRate(),
                                concept.getConceptNatureCode(),
                                concept.getOriginPeriodCode(),
                                concept.getDisplayOrder(),
                                concept.getMergedStepCount(),
                                concept.getPayslipSectionCode()
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
                extractSeniorityDate(snapshots),
                extractWorkCenterCode(snapshots),
                extractWorkCenterName(snapshots),
                rulesChangedSinceCalculation
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
        PayrollSnapshotProfiles.Company company = profiles.company(snapshots);
        return company == null ? null : new PayrollCompanyProfileResponse(
                company.legalName(),
                company.taxIdentifier(),
                company.street(),
                company.city(),
                company.postalCode()
        );
    }

    private PayrollEmployeeProfileResponse extractEmployeeProfile(List<PayrollContextSnapshot> snapshots) {
        PayrollSnapshotProfiles.Employee employee = profiles.employee(snapshots);
        return employee == null ? null : new PayrollEmployeeProfileResponse(
                employee.fullName(),
                employee.nif(),
                employee.street(),
                employee.city(),
                employee.postalCode()
        );
    }

    private PayrollAgreementProfileResponse extractAgreementProfile(List<PayrollContextSnapshot> snapshots) {
        PayrollSnapshotProfiles.Agreement agreement = profiles.agreement(snapshots);
        return agreement == null ? null : new PayrollAgreementProfileResponse(
                agreement.officialAgreementNumber(),
                agreement.displayName(),
                agreement.shortName(),
                agreement.annualHours(),
                agreement.agreementCategoryCode()
        );
    }

    private String extractPresenceStartDate(List<PayrollContextSnapshot> snapshots) {
        return profiles.presenceStartDate(snapshots);
    }

    private String extractPresenceEndDate(List<PayrollContextSnapshot> snapshots) {
        return profiles.presenceEndDate(snapshots);
    }

    private String extractSeniorityDate(List<PayrollContextSnapshot> snapshots) {
        return profiles.seniorityDate(snapshots);
    }

    private String extractWorkCenterCode(List<PayrollContextSnapshot> snapshots) {
        return profiles.workCenterCode(snapshots);
    }

    private String extractWorkCenterName(List<PayrollContextSnapshot> snapshots) {
        return profiles.workCenterName(snapshots);
    }
}
