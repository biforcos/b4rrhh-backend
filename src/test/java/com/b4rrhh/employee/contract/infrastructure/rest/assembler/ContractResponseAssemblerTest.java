package com.b4rrhh.employee.contract.infrastructure.rest.assembler;

import com.b4rrhh.employee.contract.domain.model.Contract;
import com.b4rrhh.employee.contract.infrastructure.rest.dto.ContractResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @Test
    void toResponseEnrichesContractTypeAndSubtypeNamesInTheLanguageOfTheResponse() {
        ContractResponseAssembler assembler = new ContractResponseAssembler(ruleEntityLabelResolver);
        Contract contract = contract("IND", "FT1");
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTRACT", "IND", "es-ES"))
                .thenReturn(Optional.of("Indefinido"));
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTRACT_SUBTYPE", "FT1", "es-ES"))
                .thenReturn(Optional.of("Tiempo completo"));

        ContractResponse response = assembler.toResponse("ESP", contract, new ResponseLanguage("es-ES"));

        assertEquals("IND", response.contractCode());
        assertEquals("Indefinido", response.contractTypeName());
        assertEquals("FT1", response.contractSubtypeCode());
        assertEquals("Tiempo completo", response.contractSubtypeName());
    }

    @Test
    void toResponseKeepsCodesAndUsesNullWhenLabelsAreMissing() {
        ContractResponseAssembler assembler = new ContractResponseAssembler(ruleEntityLabelResolver);
        Contract contract = contract("TMP", "PT1");
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTRACT", "TMP", null))
                .thenReturn(Optional.empty());
        when(ruleEntityLabelResolver.resolveName("ESP", "CONTRACT_SUBTYPE", "PT1", null))
                .thenReturn(Optional.empty());

        ContractResponse response = assembler.toResponse("ESP", contract, ResponseLanguage.base());

        assertEquals("TMP", response.contractCode());
        assertNull(response.contractTypeName());
        assertEquals("PT1", response.contractSubtypeCode());
        assertNull(response.contractSubtypeName());
    }

    private Contract contract(String contractCode, String contractSubtypeCode) {
        return new Contract(
                10L,
                contractCode,
                contractSubtypeCode,
                LocalDate.of(2026, 1, 10),
                null
        );
    }
}
