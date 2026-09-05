package com.b4rrhh.employee.contract.domain.exception;

import com.b4rrhh.employee.contract.domain.model.ContractPeriod;

import java.util.List;

/**
 * The resulting series would leave a stretch of the employee's presence
 * without a contract (ADR-057): the coverage of this series is mandatory. It
 * names the gaps and the neighbouring contracts the user could stretch to
 * cover them.
 */
public class ContractCoverageIncompleteException extends RuntimeException {

    private final List<ContractPeriod> gaps;
    private final List<ContractPeriod> stretchCandidates;

    public ContractCoverageIncompleteException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber
    ) {
        this(ruleSystemCode, employeeTypeCode, employeeNumber, List.of(), List.of());
    }

    public ContractCoverageIncompleteException(
            String ruleSystemCode,
            String employeeTypeCode,
            String employeeNumber,
            List<ContractPeriod> gaps,
            List<ContractPeriod> stretchCandidates
    ) {
        super("Contract coverage is incomplete for ruleSystemCode="
                + ruleSystemCode
                + ", employeeTypeCode="
                + employeeTypeCode
                + ", employeeNumber="
                + employeeNumber
                + ", gaps="
                + gaps
                + ", stretchCandidates="
                + stretchCandidates);
        this.gaps = List.copyOf(gaps);
        this.stretchCandidates = List.copyOf(stretchCandidates);
    }

    public List<ContractPeriod> gaps() {
        return gaps;
    }

    public List<ContractPeriod> stretchCandidates() {
        return stretchCandidates;
    }
}
