package com.b4rrhh.payroll_engine.object.domain.exception;

public class BindingRoleAlreadyExistsException extends RuntimeException {

    public BindingRoleAlreadyExistsException(String ruleSystemCode, String bindingRoleCode) {
        super("Binding role " + bindingRoleCode + " already exists in rule system " + ruleSystemCode);
    }
}
