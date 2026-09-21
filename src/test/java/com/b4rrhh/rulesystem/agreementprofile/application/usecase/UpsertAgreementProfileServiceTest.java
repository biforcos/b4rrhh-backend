package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.rulesystem.agreementprofile.domain.port.AgreementProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpsertAgreementProfileServiceTest {

    @Mock
    private AgreementCatalogLookupPort agreementCatalogLookupPort;

    @Mock
    private AgreementProfileRepository agreementProfileRepository;

    @Test
    void upsertSavesProfileWhenAgreementFound() {
        UpsertAgreementProfileService service = new UpsertAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "99002405011982"))
                .thenReturn(Optional.of(1L));

        AgreementProfile expectedProfile = new AgreementProfile(
                "99002405011982",
                "Convenio colectivo general centros atencion personas con discapacidad",
                "Discapacidad 99002405",
                new BigDecimal("1736.00"),
                false,
                true
        );
        when(agreementProfileRepository.save(any(Long.class), any(AgreementProfile.class)))
                .thenReturn(expectedProfile);

        var result = service.upsert(new UpsertAgreementProfileCommand(
                "ESP",
                "99002405011982",
                "99002405011982",
                "Convenio colectivo general centros atencion personas con discapacidad",
                "Discapacidad 99002405",
                new BigDecimal("1736.00"),
                false,
                true
        ));

        assertNotNull(result);
        assertEquals("99002405011982", result.officialAgreementNumber());
        assertEquals("Convenio colectivo general centros atencion personas con discapacidad", result.displayName());

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AgreementProfile> profileCaptor = ArgumentCaptor.forClass(AgreementProfile.class);
        verify(agreementProfileRepository).save(idCaptor.capture(), profileCaptor.capture());

        assertEquals(1L, idCaptor.getValue());
        assertEquals("99002405011982", profileCaptor.getValue().getOfficialAgreementNumber());
    }

    @Test
    void upsertThrowsWhenAgreementNotFound() {
        UpsertAgreementProfileService service = new UpsertAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "INVALID"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.upsert(new UpsertAgreementProfileCommand(
                "ESP",
                "INVALID",
                "99002405011982",
                "Convenio",
                "Disc",
                new BigDecimal("1736.00"),
                false,
                true
        )));
    }

    @Test
    void upsertValidatesRequiredFields() {
        UpsertAgreementProfileService service = new UpsertAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "99002405011982"))
                .thenReturn(Optional.of(1L));

        // null display name should fail
        assertThrows(IllegalArgumentException.class, () -> service.upsert(new UpsertAgreementProfileCommand(
                "ESP",
                "99002405011982",
                "99002405011982",
                null,
                "Disc",
                new BigDecimal("1736.00"),
                false,
                true
        )));
    }
}
