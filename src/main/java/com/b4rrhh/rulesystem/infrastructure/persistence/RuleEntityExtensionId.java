package com.b4rrhh.rulesystem.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de {@code rule_entity_extension}: (tipo, extensión). */
public class RuleEntityExtensionId implements Serializable {

    private String ruleEntityTypeCode;
    private String extensionCode;

    public RuleEntityExtensionId() {
    }

    public RuleEntityExtensionId(String ruleEntityTypeCode, String extensionCode) {
        this.ruleEntityTypeCode = ruleEntityTypeCode;
        this.extensionCode = extensionCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuleEntityExtensionId that)) {
            return false;
        }
        return Objects.equals(ruleEntityTypeCode, that.ruleEntityTypeCode)
                && Objects.equals(extensionCode, that.extensionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleEntityTypeCode, extensionCode);
    }
}
