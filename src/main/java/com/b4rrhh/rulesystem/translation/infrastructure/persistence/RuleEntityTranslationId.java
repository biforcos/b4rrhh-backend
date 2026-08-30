package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public class RuleEntityTranslationId implements Serializable {

    private Long ruleEntityId;
    private String languageCode;

    public RuleEntityTranslationId() {
    }

    public RuleEntityTranslationId(Long ruleEntityId, String languageCode) {
        this.ruleEntityId = ruleEntityId;
        this.languageCode = languageCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuleEntityTranslationId that)) {
            return false;
        }
        return Objects.equals(ruleEntityId, that.ruleEntityId)
                && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleEntityId, languageCode);
    }
}
