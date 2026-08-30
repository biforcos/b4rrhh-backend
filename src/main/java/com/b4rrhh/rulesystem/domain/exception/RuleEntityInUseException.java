package com.b4rrhh.rulesystem.domain.exception;

import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;

import java.util.List;
import java.util.stream.Collectors;

public class RuleEntityInUseException extends RuntimeException {

    private final List<RuleEntityReference> references;

    public RuleEntityInUseException(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            List<RuleEntityReference> references
    ) {
        super("Rule entity is in use: " + ruleSystemCode + "/" + ruleEntityTypeCode + "/" + code
                + " is referenced by " + describe(references));
        this.references = List.copyOf(references);
    }

    public List<RuleEntityReference> getReferences() {
        return references;
    }

    private static String describe(List<RuleEntityReference> references) {
        return references.stream()
                .map(reference -> reference.count() + " " + reference.resource())
                .collect(Collectors.joining(", "));
    }
}
