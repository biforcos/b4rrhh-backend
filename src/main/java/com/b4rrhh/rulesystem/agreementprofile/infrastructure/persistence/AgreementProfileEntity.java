package com.b4rrhh.rulesystem.agreementprofile.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_profile", schema = "rulesystem")
public class AgreementProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agreement_rule_entity_id", nullable = false, unique = true)
    private Long agreementRuleEntityId;

    @Column(name = "official_agreement_number", nullable = false, length = 50)
    private String officialAgreementNumber;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "annual_hours", nullable = false, precision = 7, scale = 2)
    private BigDecimal annualHours;

    @Column(name = "extra_payments_prorated", nullable = false)
    private Boolean extraPaymentsProrated;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgreementRuleEntityId() {
        return agreementRuleEntityId;
    }

    public void setAgreementRuleEntityId(Long agreementRuleEntityId) {
        this.agreementRuleEntityId = agreementRuleEntityId;
    }

    public String getOfficialAgreementNumber() {
        return officialAgreementNumber;
    }

    public void setOfficialAgreementNumber(String officialAgreementNumber) {
        this.officialAgreementNumber = officialAgreementNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public BigDecimal getAnnualHours() {
        return annualHours;
    }

    public void setAnnualHours(BigDecimal annualHours) {
        this.annualHours = annualHours;
    }

    public Boolean getExtraPaymentsProrated() {
        return extraPaymentsProrated;
    }

    public void setExtraPaymentsProrated(Boolean extraPaymentsProrated) {
        this.extraPaymentsProrated = extraPaymentsProrated;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
}
