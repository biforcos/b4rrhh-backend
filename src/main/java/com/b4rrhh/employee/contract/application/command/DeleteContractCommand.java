package com.b4rrhh.employee.contract.application.command;

import java.time.LocalDate;

/** Removes the contract that starts on {@code startDate}: a contract is identified by the day it starts. */
public record DeleteContractCommand(
        String ruleSystemCode,
        String employeeTypeCode,
        String employeeNumber,
        LocalDate startDate
) {
}
