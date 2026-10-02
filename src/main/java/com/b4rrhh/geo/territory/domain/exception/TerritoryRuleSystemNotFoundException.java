package com.b4rrhh.geo.territory.domain.exception;

public class TerritoryRuleSystemNotFoundException extends RuntimeException {

    public TerritoryRuleSystemNotFoundException(String ruleSystemCode) {
        super("No existe la reglamentación " + ruleSystemCode + ".");
    }
}
