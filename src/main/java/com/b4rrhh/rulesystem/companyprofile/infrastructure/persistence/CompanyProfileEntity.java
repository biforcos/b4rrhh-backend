package com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence;

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
        name = "company_profile",
        schema = "rulesystem",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_profile_company_rule_entity", columnNames = "company_rule_entity_id")
)
public class CompanyProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_rule_entity_id", nullable = false)
    private Long companyRuleEntityId;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "tax_identifier", length = 50)
    private String taxIdentifier;

    // Las columnas de la dirección, las mismas que en el empleado y el otro perfil (backend#155).
    @Embedded
    private PostalAddressColumns address = new PostalAddressColumns();

    @Column(name = "cnae_code", length = 10)
    private String cnaeCode;

    /** En que clasificacion esta escrito el codigo de arriba (backend#122). */
    @Column(name = "cnae_classification", length = 20)
    private String cnaeClassification;

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

    public Long getCompanyRuleEntityId() {
        return companyRuleEntityId;
    }

    public void setCompanyRuleEntityId(Long companyRuleEntityId) {
        this.companyRuleEntityId = companyRuleEntityId;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getTaxIdentifier() {
        return taxIdentifier;
    }

    public void setTaxIdentifier(String taxIdentifier) {
        this.taxIdentifier = taxIdentifier;
    }

    public String getCnaeCode() {
        return cnaeCode;
    }

    public void setCnaeCode(String v) {
        this.cnaeCode = v;
    }

    public String getCnaeClassification() {
        return cnaeClassification;
    }

    public void setCnaeClassification(String v) {
        this.cnaeClassification = v;
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
