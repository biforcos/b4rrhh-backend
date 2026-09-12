package com.b4rrhh.rulesystem.catalogoption.application.usecase;

import com.b4rrhh.rulesystem.catalogoption.application.query.GetDirectCatalogOptionsQuery;
import com.b4rrhh.rulesystem.catalogoption.domain.model.DirectCatalogOption;
import com.b4rrhh.rulesystem.catalogoption.domain.port.DirectCatalogOptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDirectCatalogOptionsServiceTest {

    @Mock
    private DirectCatalogOptionRepository directCatalogOptionRepository;

    private GetDirectCatalogOptionsService service;

    @BeforeEach
    void setUp() {
        service = new GetDirectCatalogOptionsService(directCatalogOptionRepository);
    }

    // backend#32: la fecha no baja al repositorio, porque ya no filtra. Baja hasta la
    // marca, y solo hasta ahi.
    @Test
    void returnsItemsAndComputesActiveUsingReferenceDate() {
        when(directCatalogOptionRepository.findDirectOptions(
                eq("ES_DEFAULT"),
                eq("WORK_CENTER"),
                eq(null)
        )).thenReturn(List.of(
                new DirectCatalogOption("MAIN_OFFICE", "Oficina central", true, LocalDate.of(2020, 1, 1), null),
                new DirectCatalogOption("OLD_OFFICE", "Oficina antigua", true, LocalDate.of(2010, 1, 1), LocalDate.of(2020, 12, 31))
        ));

        DirectCatalogOptionsResult result = service.get(new GetDirectCatalogOptionsQuery(
                "ES_DEFAULT",
                "WORK_CENTER",
                LocalDate.of(2026, 3, 22),
                null
        ));

        assertEquals("ES_DEFAULT", result.ruleSystemCode());
        assertEquals("WORK_CENTER", result.ruleEntityTypeCode());
        assertEquals(LocalDate.of(2026, 3, 22), result.referenceDate());
        assertEquals(2, result.items().size());
        assertEquals(true, result.items().get(0).active());
        assertEquals(false, result.items().get(1).active());
    }

    @Test
    void normalizesQAsLikeFilter() {
        when(directCatalogOptionRepository.findDirectOptions(
                eq("ES_DEFAULT"),
                eq("WORK_CENTER"),
                eq("%office%")
        )).thenReturn(List.of());

        service.get(new GetDirectCatalogOptionsQuery("ES_DEFAULT", "WORK_CENTER", null, "  office  "));

        verify(directCatalogOptionRepository).findDirectOptions(
                "ES_DEFAULT",
                "WORK_CENTER",
                "%office%"
        );
    }

    @Test
    void usesTodayAsReferenceDateWhenNotProvided() {
        when(directCatalogOptionRepository.findDirectOptions(
                eq("ES_DEFAULT"),
                eq("WORK_CENTER"),
                eq(null)
        )).thenReturn(List.of());

        DirectCatalogOptionsResult result = service.get(new GetDirectCatalogOptionsQuery(
                "ES_DEFAULT",
                "WORK_CENTER",
                null,
                null
        ));

        assertNotNull(result.referenceDate());
    }

    // backend#32: la opcion cerrada en 2020 no desaparece de la lista al pedir 2026.
    // Sale, marcada como no vigente, y elegirla es una decision consciente.
    @Test
    void aCodeThatIsNotEffectiveOnTheDateStillComesBackMarked() {
        when(directCatalogOptionRepository.findDirectOptions(
                eq("ES_DEFAULT"),
                eq("WORK_CENTER"),
                eq(null)
        )).thenReturn(List.of(
                new DirectCatalogOption("OLD_OFFICE", "Oficina antigua", true,
                        LocalDate.of(2010, 1, 1), LocalDate.of(2020, 12, 31))
        ));

        DirectCatalogOptionsResult result = service.get(new GetDirectCatalogOptionsQuery(
                "ES_DEFAULT",
                "WORK_CENTER",
                LocalDate.of(2026, 3, 22),
                null
        ));

        assertEquals(1, result.items().size());
        assertEquals("OLD_OFFICE", result.items().get(0).code());
        assertEquals(false, result.items().get(0).active());
    }

    @Test
    void rejectsBlankRuleSystemCode() {
        assertThrows(IllegalArgumentException.class, () -> service.get(new GetDirectCatalogOptionsQuery(
                "   ",
                "WORK_CENTER",
                null,
                null
        )));
    }

    @Test
    void rejectsBlankRuleEntityTypeCode() {
        assertThrows(IllegalArgumentException.class, () -> service.get(new GetDirectCatalogOptionsQuery(
                "ES_DEFAULT",
                "   ",
                null,
                null
        )));
    }
}
