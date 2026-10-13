package com.b4rrhh.rulesystem.application.usecase;

import com.b4rrhh.rulesystem.application.port.RuleEntityUsageCheckPort;
import com.b4rrhh.rulesystem.domain.exception.RuleEntityInUseException;
import com.b4rrhh.rulesystem.domain.model.Layer;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.model.RuleEntityReference;
import com.b4rrhh.rulesystem.domain.port.LayerRepository;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * frontend#131: una fila de INT la ven todas las reglamentaciones que montan INT, y las referencias
 * se cuentan por reglamentación. Borrarla desde la capa pregunta a cada una.
 */
@ExtendWith(MockitoExtension.class)
class DeleteLayerRuleEntityServiceTest {

    private static final LocalDate START = LocalDate.of(1900, 1, 1);
    private static final Layer INT = new Layer("INT", "Internacional", 2, true, List.of("ESP", "FRA", "PRT"));

    @Mock
    private LayerRepository layerRepository;
    @Mock
    private RuleEntityRepository ruleEntityRepository;
    @Mock
    private RuleEntityUsageCheckPort ruleEntityUsageCheckPort;

    private DeleteLayerRuleEntityService service;

    @BeforeEach
    void setUp() {
        service = new DeleteLayerRuleEntityService(layerRepository, ruleEntityRepository, ruleEntityUsageCheckPort);
        when(layerRepository.findByCode("INT")).thenReturn(Optional.of(INT));
        when(ruleEntityRepository.findInLayerByBusinessKeyAndStartDate("INT", "COUNTRY", "AND", START))
                .thenReturn(Optional.of(new RuleEntity(1L, null, "INT", 2, "COUNTRY", "AND", "Andorra", null,
                        true, START, null, null, null)));
    }

    @Test
    void aCountryUsedOnlyFromFraIsInUseAlthoughEspDoesNotUseIt() {
        when(ruleEntityUsageCheckPort.findReferences("ESP", "COUNTRY", "AND")).thenReturn(List.of());
        when(ruleEntityUsageCheckPort.findReferences("FRA", "COUNTRY", "AND"))
                .thenReturn(List.of(new RuleEntityReference("addresses", 2)));
        when(ruleEntityUsageCheckPort.findReferences("PRT", "COUNTRY", "AND"))
                .thenReturn(List.of(new RuleEntityReference("addresses", 1)));

        RuleEntityInUseException ex = assertThrows(RuleEntityInUseException.class,
                () -> service.delete(new DeleteLayerRuleEntityCommand("int", "country", "and", START)));

        assertEquals(List.of(new RuleEntityReference("addresses", 3)), ex.getReferences());
        verify(ruleEntityRepository, never()).deleteInLayerByBusinessKeyAndStartDate(any(), any(), any(), any());
    }

    @Test
    void aCountryNobodyUsesIsDeletedFromItsLayer() {
        when(ruleEntityUsageCheckPort.findReferences(any(), any(), any())).thenReturn(List.of());

        service.delete(new DeleteLayerRuleEntityCommand("INT", "COUNTRY", "AND", START));

        verify(ruleEntityRepository).deleteInLayerByBusinessKeyAndStartDate("INT", "COUNTRY", "AND", START);
    }
}
