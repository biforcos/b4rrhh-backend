package com.b4rrhh.rulesystem.agreementprofile.application.usecase;

import com.b4rrhh.rulesystem.agreementprofile.application.port.AgreementCatalogLookupPort;
import com.b4rrhh.rulesystem.agreementprofile.domain.model.AgreementProfile;
import com.b4rrhh.rulesystem.agreementprofile.domain.port.AgreementProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAgreementProfileServiceTest {

    @Mock
    private AgreementCatalogLookupPort agreementCatalogLookupPort;

    @Mock
    private AgreementProfileRepository agreementProfileRepository;

    @Test
    void getReturnsProfileWhenFound() {
        GetAgreementProfileService service = new GetAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "99002405011982"))
                .thenReturn(Optional.of(1L));

        AgreementProfile profile = new AgreementProfile(
                "99002405011982",
                "Convenio colectivo general centros atencion personas con discapacidad",
                "Discapacidad 99002405",
                new BigDecimal("1736.00"),
                false,
                true
        );
        when(agreementProfileRepository.findByAgreementRuleEntityId(1L))
                .thenReturn(Optional.of(profile));

        var result = service.get(new GetAgreementProfileQuery("ESP", "99002405011982"));

        assertTrue(result.isPresent());
        assertEquals("99002405011982", result.get().officialAgreementNumber());
        assertEquals("Convenio colectivo general centros atencion personas con discapacidad", result.get().displayName());
        assertEquals(0, new BigDecimal("1736.00").compareTo(result.get().annualHours()));
    }

    @Test
    void getReturnsEmptyWhenAgreementNotFound() {
        GetAgreementProfileService service = new GetAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "INVALID"))
                .thenReturn(Optional.empty());

        var result = service.get(new GetAgreementProfileQuery("ESP", "INVALID"));

        assertTrue(result.isEmpty());
    }

    @Test
    void getReturnsEmptyWhenProfileNotFound() {
        GetAgreementProfileService service = new GetAgreementProfileService(
                agreementCatalogLookupPort,
                agreementProfileRepository
        );

        when(agreementCatalogLookupPort.findAgreementRuleEntityId("ESP", "99002405011982"))
                .thenReturn(Optional.of(1L));
        when(agreementProfileRepository.findByAgreementRuleEntityId(1L))
                .thenReturn(Optional.empty());

        var result = service.get(new GetAgreementProfileQuery("ESP", "99002405011982"));

        assertTrue(result.isEmpty());
    }
}
