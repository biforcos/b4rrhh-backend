package com.b4rrhh.employee.labor_classification.infrastructure.rest.assembler;

import com.b4rrhh.employee.labor_classification.domain.model.LaborClassification;
import com.b4rrhh.employee.labor_classification.infrastructure.rest.dto.LaborClassificationResponse;
import com.b4rrhh.rulesystem.agreementcategoryprofile.domain.port.AgreementCategoryProfileRepository;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LaborClassificationResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;
    @Mock
    private AgreementCategoryProfileRepository agreementCategoryProfileRepository;
    @InjectMocks
    private LaborClassificationResponseAssembler assembler;

    @Test
    void toResponseEnrichesBothLabelsInTheLanguageOfTheResponse() {
        LaborClassification laborClassification = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", "es-ES"))
                .thenReturn(Optional.of("Office Agreement"));
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", "es-ES"))
                .thenReturn(Optional.of("Administrative Category"));
        when(agreementCategoryProfileRepository.findGrupoCotizacionCodeByCategoryCode("ESP", "CAT_ADMIN"))
                .thenReturn(Optional.of("05"));

        LaborClassificationResponse response = assembler.toResponse("ESP", laborClassification, new ResponseLanguage("es-ES"));

        assertEquals("AGR_OFFICE", response.agreementCode());
        assertEquals("Office Agreement", response.agreementName());
        assertEquals("CAT_ADMIN", response.agreementCategoryCode());
        assertEquals("Administrative Category", response.agreementCategoryName());
        assertEquals("05", response.grupoCotizacionCode());
    }

    @Test
    void toResponseKeepsCodesAndUsesNullWhenLabelsMissing() {
        LaborClassification laborClassification = laborClassification("AGR_OFFICE", "CAT_ADMIN", LocalDate.of(2026, 1, 1), null);
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT", "AGR_OFFICE", null))
                .thenReturn(Optional.empty());
        when(ruleEntityLabelResolver.resolveName("ESP", "AGREEMENT_CATEGORY", "CAT_ADMIN", null))
                .thenReturn(Optional.empty());
        when(agreementCategoryProfileRepository.findGrupoCotizacionCodeByCategoryCode("ESP", "CAT_ADMIN"))
                .thenReturn(Optional.empty());

        LaborClassificationResponse response = assembler.toResponse("ESP", laborClassification, ResponseLanguage.base());

        assertEquals("AGR_OFFICE", response.agreementCode());
        assertEquals("CAT_ADMIN", response.agreementCategoryCode());
        assertNull(response.agreementName());
        assertNull(response.agreementCategoryName());
        assertNull(response.grupoCotizacionCode());
    }

    // El grupo de cotizacion se busca con los codigos normalizados, como hacia el adaptador.
    @Test
    void grupoCotizacionIsLookedUpWithNormalizedCodes() {
        LaborClassification laborClassification = laborClassification("AGR_OFFICE", " cat_admin ", LocalDate.of(2026, 1, 1), null);
        when(agreementCategoryProfileRepository.findGrupoCotizacionCodeByCategoryCode("ESP", "CAT_ADMIN"))
                .thenReturn(Optional.of("05"));

        LaborClassificationResponse response = assembler.toResponse(" esp ", laborClassification, ResponseLanguage.base());

        assertEquals("05", response.grupoCotizacionCode());
    }

    private LaborClassification laborClassification(
            String agreementCode,
            String agreementCategoryCode,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return new LaborClassification(10L, agreementCode, agreementCategoryCode, startDate, endDate);
    }
}
