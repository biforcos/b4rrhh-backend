package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.object.infrastructure.persistence.PayrollObjectEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity(name = "PayrollEngineConceptOperandEntity")
@Table(name = "payroll_concept_operand", schema = "payroll_engine")
public class PayrollConceptOperandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "target_object_id", nullable = false)
    private PayrollObjectEntity targetObject;

    @Column(name = "operand_role", nullable = false, length = 30)
    private String operandRole;

    @ManyToOne
    @JoinColumn(name = "source_object_id", nullable = false)
    private PayrollObjectEntity sourceObject;

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

    public PayrollObjectEntity getTargetObject() { return targetObject; }
    public void setTargetObject(PayrollObjectEntity targetObject) { this.targetObject = targetObject; }

    public String getOperandRole() { return operandRole; }
    public void setOperandRole(String operandRole) { this.operandRole = operandRole; }

    public PayrollObjectEntity getSourceObject() { return sourceObject; }
    public void setSourceObject(PayrollObjectEntity sourceObject) { this.sourceObject = sourceObject; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
