package com.b4rrhh.employee.contract.infrastructure.persistence;

import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractCatalogReadAdapterTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @Test
    void findContractTypeNameDelegatesToTheResolverWithTheContractType() {
        ContractCatalogReadAdapter adapter = new ContractCatalogReadAdapter(ruleEntityLabelResolver);
        when(ruleEntityLabelResolver.resolveName(" esp ", "CONTRACT", " ind ", null))
                .thenReturn(Optional.of("Indefinido"));

        Optional<String> result = adapter.findContractTypeName(" esp ", " ind ");

        assertEquals(Optional.of("Indefinido"), result);
        verify(ruleEntityLabelResolver).resolveName(" esp ", "CONTRACT", " ind ", null);
    }

    @Test
    void findContractSubtypeNamePassesTheLanguageThrough() {
        ContractCatalogReadAdapter adapter = new ContractCatalogReadAdapter(ruleEntityLabelResolver);
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTRACT_SUBTYPE", "FT1", "es-ES"))
                .thenReturn(Optional.empty());

        Optional<String> result = adapter.findContractSubtypeName("ESP", "FT1", "es-ES");

        assertTrue(result.isEmpty());
    }
}
