package com.b4rrhh.rulesystem.translation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Sólo lectura: las traducciones de los tipos entran por migración (backend#152), y los
 * sellos de tiempo los pone la base.
 */
@Entity
@Table(name = "rule_entity_type_translation", schema = "rulesystem")
@IdClass(RuleEntityTypeTranslationId.class)
public class RuleEntityTypeTranslationEntity {

    @Id
    @Column(name = "rule_entity_type_code", nullable = false, length = 30)
    private String ruleEntityTypeCode;

    @Id
    @Column(name = "language_code", nullable = false, length = 5)
    private String languageCode;

    @Column(nullable = false, length = 100)
    private String name;

    public String getRuleEntityTypeCode() { return ruleEntityTypeCode; }
    public String getLanguageCode() { return languageCode; }
    public String getName() { return name; }
}
