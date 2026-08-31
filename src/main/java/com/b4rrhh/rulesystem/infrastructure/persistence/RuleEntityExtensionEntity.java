package com.b4rrhh.rulesystem.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "rule_entity_extension", schema = "rulesystem")
@IdClass(RuleEntityExtensionId.class)
public class RuleEntityExtensionEntity {

    @Id
    @Column(name = "rule_entity_type_code", length = 30)
    private String ruleEntityTypeCode;

    @Id
    @Column(name = "extension_code", length = 30)
    private String extensionCode;

    @Column(name = "table_name", nullable = false, length = 63)
    private String tableName;

    @Column(nullable = false, length = 3)
    private String cardinality;

    @Column(nullable = false)
    private boolean required;

    public String getRuleEntityTypeCode() { return ruleEntityTypeCode; }
    public void setRuleEntityTypeCode(String ruleEntityTypeCode) { this.ruleEntityTypeCode = ruleEntityTypeCode; }

    public String getExtensionCode() { return extensionCode; }
    public void setExtensionCode(String extensionCode) { this.extensionCode = extensionCode; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getCardinality() { return cardinality; }
    public void setCardinality(String cardinality) { this.cardinality = cardinality; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
}
