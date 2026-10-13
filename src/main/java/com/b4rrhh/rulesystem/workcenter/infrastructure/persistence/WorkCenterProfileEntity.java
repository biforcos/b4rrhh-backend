package com.b4rrhh.rulesystem.workcenter.infrastructure.persistence;

import com.b4rrhh.geo.address.infrastructure.persistence.PostalAddressColumns;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "work_center_profile",
        schema = "rulesystem",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_work_center_profile_rule_entity",
                columnNames = "work_center_rule_entity_id"
        )
)
public class WorkCenterProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_center_rule_entity_id", nullable = false)
    private Long workCenterRuleEntityId;

    @Column(name = "company_code", nullable = false, length = 30)
    private String companyCode;

    // Las columnas de la dirección, las mismas que en el empleado y el otro perfil (backend#155).
    @Embedded
    private PostalAddressColumns address = new PostalAddressColumns();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkCenterRuleEntityId() {
        return workCenterRuleEntityId;
    }

    public void setWorkCenterRuleEntityId(Long workCenterRuleEntityId) {
        this.workCenterRuleEntityId = workCenterRuleEntityId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PostalAddressColumns getAddress() {
        return address;
    }

    public void setAddress(PostalAddressColumns address) {
        this.address = address;
    }
}
