package com.b4rrhh.employee.address.infrastructure.persistence;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.employee.address.domain.model.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressPersistenceAdapterTest {

    @Mock
    private SpringDataAddressRepository springDataAddressRepository;

    private AddressPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AddressPersistenceAdapter(springDataAddressRepository);
    }

    @Test
    void preservesCreatedAtAndUpdatedAtWhenSavingExistingAddress() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 10, 8, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 20, 9, 30, 0);

        Address existingAddress = new Address(
                20L,
                10L,
                1,
                "HOME",
                DireccionesDePrueba.address("Calle Mayor 10", "Madrid", "ESP", "28013"),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 1),
                createdAt,
                updatedAt
        );

        when(springDataAddressRepository.save(any(AddressEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = adapter.save(existingAddress);

        ArgumentCaptor<AddressEntity> entityCaptor = ArgumentCaptor.forClass(AddressEntity.class);
        verify(springDataAddressRepository).save(entityCaptor.capture());

        AddressEntity persistedEntity = entityCaptor.getValue();
        assertEquals(createdAt, persistedEntity.getCreatedAt());
        assertEquals(updatedAt, persistedEntity.getUpdatedAt());

        assertEquals(createdAt, saved.getCreatedAt());
        assertEquals(updatedAt, saved.getUpdatedAt());
    }
}
