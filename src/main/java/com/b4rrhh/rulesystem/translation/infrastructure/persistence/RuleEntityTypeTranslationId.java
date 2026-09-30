package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

public class RuleEntityTypeTranslationId implements Serializable {

    private String ruleEntityTypeCode;
    private String languageCode;

    public RuleEntityTypeTranslationId() {
    }

    public RuleEntityTypeTranslationId(String ruleEntityTypeCode, String languageCode) {
        this.ruleEntityTypeCode = ruleEntityTypeCode;
        this.languageCode = languageCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuleEntityTypeTranslationId that)) {
            return false;
        }
        return Objects.equals(ruleEntityTypeCode, that.ruleEntityTypeCode)
                && Objects.equals(languageCode, that.languageCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleEntityTypeCode, languageCode);
    }
}
