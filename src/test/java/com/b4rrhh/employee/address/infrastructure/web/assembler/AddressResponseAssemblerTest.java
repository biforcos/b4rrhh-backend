package com.b4rrhh.employee.address.infrastructure.web.assembler;

import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressResponse;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressResponseAssemblerTest {

    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    @Test
    void toResponseEnrichesLabelInTheLanguageOfTheResponse() {
        AddressResponseAssembler assembler = new AddressResponseAssembler(ruleEntityLabelResolver);
        Address address = address(1, "HOME");
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", "es-ES"))
                .thenReturn(Optional.of("Domicilio"));

        AddressResponse response = assembler.toResponse("ESP", address, new ResponseLanguage("es-ES"));

        assertEquals(1, response.addressNumber());
        assertEquals("HOME", response.addressTypeCode());
        assertEquals("Domicilio", response.addressTypeName());
    }

    @Test
    void toResponseKeepsCodeAndUsesNullWhenLabelMissing() {
        AddressResponseAssembler assembler = new AddressResponseAssembler(ruleEntityLabelResolver);
        Address address = address(1, "HOME");
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", null))
                .thenReturn(Optional.empty());

        AddressResponse response = assembler.toResponse("ESP", address, ResponseLanguage.base());

        assertEquals("HOME", response.addressTypeCode());
        assertNull(response.addressTypeName());
    }

    private Address address(int addressNumber, String addressTypeCode) {
        return new Address(
                10L,
                20L,
                addressNumber,
                addressTypeCode,
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                "28013",
                "M",
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}
