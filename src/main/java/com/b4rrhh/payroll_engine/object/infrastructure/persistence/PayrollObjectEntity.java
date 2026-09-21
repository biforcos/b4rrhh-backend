package com.b4rrhh.payroll_engine.object.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity(name = "PayrollEngineObjectEntity")
@Table(name = "payroll_object", schema = "payroll_engine")
public class PayrollObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_system_code", nullable = false, length = 10)
    private String ruleSystemCode;

    @Column(name = "object_type_code", nullable = false, length = 30)
    private String objectTypeCode;

    @Column(name = "object_code", nullable = false, length = 50)
    private String objectCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Instant y no LocalDateTime, y su hermana createdAt sigue siendo LocalDateTime: esta
    // columna entra en la comparacion del RuleSystemLastChangeLookupAdapter y la otra no.
    // La asimetria es el alcance del backend#116, no un descuido.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        Instant ahora = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = ahora;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRuleSystemCode() { return ruleSystemCode; }
    public void setRuleSystemCode(String ruleSystemCode) { this.ruleSystemCode = ruleSystemCode; }

    public String getObjectTypeCode() { return objectTypeCode; }
    public void setObjectTypeCode(String objectTypeCode) { this.objectTypeCode = objectTypeCode; }

    public String getObjectCode() { return objectCode; }
    public void setObjectCode(String objectCode) { this.objectCode = objectCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
