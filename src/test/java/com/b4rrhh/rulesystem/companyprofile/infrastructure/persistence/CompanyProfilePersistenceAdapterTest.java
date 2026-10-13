package com.b4rrhh.rulesystem.companyprofile.infrastructure.persistence;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProfilePersistenceAdapterTest {

    @Mock
    private SpringDataCompanyProfileRepository springDataCompanyProfileRepository;

    private CompanyProfilePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CompanyProfilePersistenceAdapter(springDataCompanyProfileRepository);
    }

    @Test
    void preservesCreatedAtAndUpdatedAtWhenSavingExistingProfile() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 8, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 20, 9, 30, 0);

        CompanyProfileEntity existing = new CompanyProfileEntity();
        existing.setId(20L);
        existing.setCompanyRuleEntityId(10L);
        existing.setLegalName("Old Legal Name");
        existing.setCreatedAt(createdAt);
        existing.setUpdatedAt(updatedAt);

        when(springDataCompanyProfileRepository.findByCompanyRuleEntityId(10L))
                .thenReturn(Optional.of(existing));
        when(springDataCompanyProfileRepository.save(any(CompanyProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(10L, new CompanyProfile(
                "Acme Spain SA",
                "A12345678",
                DireccionesDePrueba.address("Gran Via 1", "Madrid", "ESP", "28013"),
                null
        ));

        ArgumentCaptor<CompanyProfileEntity> captor = ArgumentCaptor.forClass(CompanyProfileEntity.class);
        verify(springDataCompanyProfileRepository).save(captor.capture());

        CompanyProfileEntity persistedEntity = captor.getValue();
        assertEquals(createdAt, persistedEntity.getCreatedAt());
        assertEquals(updatedAt, persistedEntity.getUpdatedAt());
        assertEquals("Acme Spain SA", persistedEntity.getLegalName());
        assertEquals("ESP", persistedEntity.getAddress().toDomain().countryCode());
    }

    @Test
    void updatesExistingRowInsteadOfCreatingDuplicateEntity() {
        CompanyProfileEntity existing = new CompanyProfileEntity();
        existing.setId(20L);
        existing.setCompanyRuleEntityId(10L);
        existing.setLegalName("Old Legal Name");

        when(springDataCompanyProfileRepository.findByCompanyRuleEntityId(10L))
                .thenReturn(Optional.of(existing));
        when(springDataCompanyProfileRepository.save(any(CompanyProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        adapter.save(10L, new CompanyProfile(
                "Acme Spain SA",
                null,
                null,
                null
        ));

        ArgumentCaptor<CompanyProfileEntity> captor = ArgumentCaptor.forClass(CompanyProfileEntity.class);
        verify(springDataCompanyProfileRepository).save(captor.capture());
        assertEquals(20L, captor.getValue().getId());
        assertEquals(10L, captor.getValue().getCompanyRuleEntityId());
    }
}