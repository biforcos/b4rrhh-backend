package com.b4rrhh.rulesystem.infrastructure.persistence;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEntityPersistenceAdapterTest {

    @Mock
    private SpringDataRuleEntityRepository springDataRuleEntityRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query layerQuery;

    private RuleEntityPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        // ESP monta su capa nacional, ESP, en el nivel de todos los tipos de este test: el 3. Que
        // resuelva por nivel y no por nombre lo prueba TheRuleEntityPortResolvesByTheLevelOfTheTypeTest.
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(layerQuery);
        lenient().when(layerQuery.setParameter(anyString(), any())).thenReturn(layerQuery);
        lenient().when(layerQuery.getResultList()).thenReturn(List.<Object>of((Object) new Object[] {"ESP", (short) 3}));
        adapter = new RuleEntityPersistenceAdapter(springDataRuleEntityRepository, entityManager);
    }

        @Test
        void findByFiltersUsesSpecificationAndStableSortWithoutReferenceDate() {
                RuleEntityEntity entity = new RuleEntityEntity();
                entity.setId(10L);
                entity.setLayerCode("ESP");
                entity.setRuleEntityTypeCode("COMPANY");
                entity.setCode("ACME");
                entity.setName("Acme");
                entity.setActive(true);
                entity.setStartDate(LocalDate.of(2020, 1, 1));
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());

                when(springDataRuleEntityRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                                .thenReturn(List.of(entity));

                List<RuleEntity> result = adapter.findByFilters("ESP", "COMPANY", null, true, null);

                assertEquals(1, result.size());
                verify(springDataRuleEntityRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Sort.by("ruleEntityTypeCode", "code")));
        }

        @Test
        void findByFiltersUsesSpecificationAndStableSortWithReferenceDate() {
                when(springDataRuleEntityRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Sort.class)))
                                .thenReturn(List.of());

                List<RuleEntity> result = adapter.findByFilters("ESP", "COMPANY", null, true, LocalDate.of(2026, 4, 6));

                assertEquals(0, result.size());
                verify(springDataRuleEntityRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(Sort.by("ruleEntityTypeCode", "code")));
        }

    @Test
    void createFlowPersistsNewEntityWithoutLoadingExistingOccurrence() {
        RuleEntity newRuleEntity = new RuleEntity(
                null,
                "ESP",
                "WORK_CENTER",
                "MADRID",
                "Madrid",
                "Initial description",
                true,
                LocalDate.of(2026, 1, 1),
                null,
                null,
                null
        );

        when(springDataRuleEntityRepository.save(any(RuleEntityEntity.class)))
                .thenAnswer(invocation -> {
                    RuleEntityEntity entity = invocation.getArgument(0);
                    entity.setId(90L);
                    entity.setCreatedAt(LocalDateTime.of(2026, 1, 10, 8, 0, 0));
                    entity.setUpdatedAt(LocalDateTime.of(2026, 1, 10, 8, 0, 0));
                    return entity;
                });

        RuleEntity saved = adapter.save(newRuleEntity);

        verify(springDataRuleEntityRepository, never())
                .findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(any(), any(), any(), any());

        ArgumentCaptor<RuleEntityEntity> entityCaptor = ArgumentCaptor.forClass(RuleEntityEntity.class);
        verify(springDataRuleEntityRepository).save(entityCaptor.capture());

        RuleEntityEntity persisted = entityCaptor.getValue();
        assertEquals("ESP", persisted.getLayerCode());
        assertEquals("WORK_CENTER", persisted.getRuleEntityTypeCode());
        assertEquals("MADRID", persisted.getCode());
        assertEquals(LocalDate.of(2026, 1, 1), persisted.getStartDate());
        assertEquals("Madrid", persisted.getName());
        assertEquals("Initial description", persisted.getDescription());
        assertEquals(true, persisted.isActive());
        assertEquals(null, persisted.getEndDate());

        assertNotNull(saved.getId());
        assertEquals("ESP", saved.getRuleSystemCode());
        assertEquals("WORK_CENTER", saved.getRuleEntityTypeCode());
        assertEquals("MADRID", saved.getCode());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getStartDate());
    }

    @Test
    void updateFlowLoadsExistingEntityAndPreservesCreatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 10, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 20, 15, 30, 0);

        RuleEntity updatedDomain = new RuleEntity(
                10L,
                "ESP",
                "WORK_CENTER",
                "MADRID",
                "Madrid Updated",
                "Updated description",
                false,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                createdAt,
                updatedAt
        );

        RuleEntityEntity existing = new RuleEntityEntity();
        existing.setId(10L);
        existing.setLayerCode("ESP");
        existing.setRuleEntityTypeCode("WORK_CENTER");
        existing.setCode("MADRID");
        existing.setStartDate(LocalDate.of(2026, 1, 1));
        existing.setName("Madrid");
        existing.setDescription("Original description");
        existing.setActive(true);
        existing.setEndDate(null);
        existing.setCreatedAt(createdAt);
        existing.setUpdatedAt(updatedAt);

        when(springDataRuleEntityRepository.findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1)
        )).thenReturn(Optional.of(existing));

        when(springDataRuleEntityRepository.save(any(RuleEntityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RuleEntity saved = adapter.save(updatedDomain);

        ArgumentCaptor<RuleEntityEntity> entityCaptor = ArgumentCaptor.forClass(RuleEntityEntity.class);
        verify(springDataRuleEntityRepository).save(entityCaptor.capture());

        RuleEntityEntity persisted = entityCaptor.getValue();
        assertSame(existing, persisted);
        assertEquals(createdAt, persisted.getCreatedAt());
        assertEquals(updatedAt, persisted.getUpdatedAt());
        assertEquals("Madrid Updated", persisted.getName());
        assertEquals("Updated description", persisted.getDescription());
        assertEquals(false, persisted.isActive());
        assertEquals(LocalDate.of(2026, 12, 31), persisted.getEndDate());

        assertEquals(createdAt, saved.getCreatedAt());
        assertEquals(updatedAt, saved.getUpdatedAt());
    }

    @Test
    void updateFlowReturnsSavedCanonicalOccurrence() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 5, 10, 0, 0);

        RuleEntity updatedDomain = new RuleEntity(
                10L,
                "ESP",
                "WORK_CENTER",
                "MADRID",
                "Madrid Updated",
                "Updated description",
                false,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                createdAt,
                null
        );

        RuleEntityEntity existing = new RuleEntityEntity();
        existing.setId(10L);
        existing.setLayerCode("ESP");
        existing.setRuleEntityTypeCode("WORK_CENTER");
        existing.setCode("MADRID");
        existing.setStartDate(LocalDate.of(2026, 1, 1));
        existing.setName("Madrid");
        existing.setDescription("Original description");
        existing.setActive(true);
        existing.setEndDate(null);
        existing.setCreatedAt(createdAt);
        existing.setUpdatedAt(LocalDateTime.of(2026, 1, 20, 15, 30, 0));

        when(springDataRuleEntityRepository.findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1)
        )).thenReturn(Optional.of(existing));

        when(springDataRuleEntityRepository.save(any(RuleEntityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RuleEntity saved = adapter.save(updatedDomain);

        assertEquals(10L, saved.getId());
        assertEquals("ESP", saved.getRuleSystemCode());
        assertEquals("WORK_CENTER", saved.getRuleEntityTypeCode());
        assertEquals("MADRID", saved.getCode());
        assertEquals(LocalDate.of(2026, 1, 1), saved.getStartDate());
        assertEquals("Madrid Updated", saved.getName());
        assertEquals("Updated description", saved.getDescription());
        assertEquals(false, saved.isActive());
        assertEquals(LocalDate.of(2026, 12, 31), saved.getEndDate());
    }

    @Test
    void updateFlowFailsLoudlyWhenOccurrenceDoesNotExist() {
        RuleEntity updatedDomain = new RuleEntity(
                10L,
                "ESP",
                "WORK_CENTER",
                "MADRID",
                "Madrid Updated",
                "Updated description",
                false,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                null,
                null
        );

        when(springDataRuleEntityRepository.findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1)
        )).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> adapter.save(updatedDomain));

        assertEquals(
                "Rule entity not found for update with business key: ESP/WORK_CENTER/MADRID/2026-01-01",
                exception.getMessage()
        );
    }

    @Test
    void overlapCheckUsesMaxDateWhenProjectedEndDateIsNull() {
        when(springDataRuleEntityRepository.existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE,
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE
        )).thenReturn(false);

        boolean overlap = adapter.existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                null,
                LocalDate.of(2026, 1, 1)
        );

        assertEquals(false, overlap);
        verify(springDataRuleEntityRepository).existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE,
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE
        );
    }

    @Test
    void overlapCheckUsesProjectedEndDateWhenProvided() {
        LocalDate projectedEndDate = LocalDate.of(2026, 12, 31);
        when(springDataRuleEntityRepository.existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                projectedEndDate,
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE
        )).thenReturn(true);

        boolean overlap = adapter.existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                projectedEndDate,
                LocalDate.of(2026, 1, 1)
        );

        assertEquals(true, overlap);
        verify(springDataRuleEntityRepository).existsOverlapExcludingStartDate(
                "ESP",
                "WORK_CENTER",
                "MADRID",
                LocalDate.of(2026, 1, 1),
                projectedEndDate,
                LocalDate.of(2026, 1, 1),
                SpringDataRuleEntityRepository.MAX_DATE
        );
    }

    @Test
    void applicableLookupReturnsSingleApplicableOccurrence() {
        RuleEntityEntity applicable = new RuleEntityEntity();
        applicable.setId(50L);
        applicable.setLayerCode("ESP");
        applicable.setRuleEntityTypeCode("COMPANY");
        applicable.setCode("ACME");
        applicable.setName("Acme");
        applicable.setActive(true);
        applicable.setStartDate(LocalDate.of(2020, 1, 1));
        applicable.setEndDate(null);
        applicable.setCreatedAt(LocalDateTime.now());
        applicable.setUpdatedAt(LocalDateTime.now());

        when(springDataRuleEntityRepository.findApplicableByBusinessKey(
                "ESP",
                "COMPANY",
                "ACME",
                LocalDate.of(2026, 4, 6),
                SpringDataRuleEntityRepository.MAX_DATE
        )).thenReturn(java.util.List.of(applicable));

        Optional<RuleEntity> result = adapter.findApplicableByBusinessKey(
                "ESP",
                "COMPANY",
                "ACME",
                LocalDate.of(2026, 4, 6)
        );

        assertEquals(true, result.isPresent());
        assertEquals(50L, result.get().getId());
    }

    @Test
    void applicableLookupFailsWhenMoreThanOneOccurrenceIsApplicable() {
        RuleEntityEntity first = new RuleEntityEntity();
        first.setId(50L);
        first.setLayerCode("ESP");
        first.setRuleEntityTypeCode("COMPANY");
        first.setCode("ACME");
        first.setName("Acme 1");
        first.setActive(true);
        first.setStartDate(LocalDate.of(2020, 1, 1));
        first.setCreatedAt(LocalDateTime.now());
        first.setUpdatedAt(LocalDateTime.now());

        RuleEntityEntity second = new RuleEntityEntity();
        second.setId(51L);
        second.setLayerCode("ESP");
        second.setRuleEntityTypeCode("COMPANY");
        second.setCode("ACME");
        second.setName("Acme 2");
        second.setActive(true);
        second.setStartDate(LocalDate.of(2021, 1, 1));
        second.setCreatedAt(LocalDateTime.now());
        second.setUpdatedAt(LocalDateTime.now());

        when(springDataRuleEntityRepository.findApplicableByBusinessKey(
                "ESP",
                "COMPANY",
                "ACME",
                LocalDate.of(2026, 4, 6),
                SpringDataRuleEntityRepository.MAX_DATE
        )).thenReturn(java.util.List.of(first, second));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.findApplicableByBusinessKey("ESP", "COMPANY", "ACME", LocalDate.of(2026, 4, 6))
        );

        assertEquals(
                "Multiple applicable rule entities found for business key: ESP/COMPANY/ACME at 2026-04-06",
                exception.getMessage()
        );
    }
}
