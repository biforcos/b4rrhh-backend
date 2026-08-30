package com.b4rrhh.employee.address.infrastructure.web;

import com.b4rrhh.employee.address.application.usecase.CloseAddressCommand;
import com.b4rrhh.employee.address.application.usecase.CloseAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.CreateAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.GetAddressByBusinessKeyUseCase;
import com.b4rrhh.employee.address.application.usecase.ListEmployeeAddressesUseCase;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressCommand;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressUseCase;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguageArgumentResolver;
import com.b4rrhh.employee.address.domain.exception.AddressCatalogValueInvalidException;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.infrastructure.web.assembler.AddressResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AddressBusinessKeyControllerHttpTest {

    @Mock
    private CreateAddressUseCase createAddressUseCase;
    @Mock
    private CloseAddressUseCase closeAddressUseCase;
    @Mock
    private GetAddressByBusinessKeyUseCase getAddressByBusinessKeyUseCase;
    @Mock
    private ListEmployeeAddressesUseCase listEmployeeAddressesUseCase;
    @Mock
    private UpdateAddressUseCase updateAddressUseCase;
        @Mock
        private RuleEntityLabelResolver ruleEntityLabelResolver;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AddressBusinessKeyController controller = new AddressBusinessKeyController(
                createAddressUseCase,
                closeAddressUseCase,
                getAddressByBusinessKeyUseCase,
                listEmployeeAddressesUseCase,
                updateAddressUseCase,
                new AddressResponseAssembler(ruleEntityLabelResolver)
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new AddressExceptionHandler())
                .setCustomArgumentResolvers(new ResponseLanguageArgumentResolver())
                .build();
    }

    @Test
    void putReturns200WhenUpdateSucceeds() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", null))
                .thenReturn(Optional.of("Domicilio"));
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class))).thenReturn(updatedAddress());

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "postalCode": "28009",
                                  "regionCode": "MD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressNumber").value(1))
                .andExpect(jsonPath("$.addressTypeName").value("Domicilio"))
                .andExpect(jsonPath("$.street").value("Calle de Alcala 100"))
                .andExpect(jsonPath("$.employeeId").doesNotExist())
                .andExpect(jsonPath("$.id").doesNotExist());

        ArgumentCaptor<UpdateAddressCommand> captor = ArgumentCaptor.forClass(UpdateAddressCommand.class);
        verify(updateAddressUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().addressNumber());
    }

    @Test
    void putReturns404WhenEmployeeDoesNotExist() throws Exception {
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class)))
                .thenThrow(new AddressEmployeeNotFoundException("ESP", "INTERNAL", "EMP001"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "postalCode": "28009",
                                  "regionCode": "MD"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReturnsCodeWithNullLabelWhenCatalogNameIsMissing() throws Exception {
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", null))
                .thenReturn(Optional.empty());
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class))).thenReturn(updatedAddress());

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "postalCode": "28009",
                                  "regionCode": "MD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressTypeCode").value("HOME"))
                .andExpect(jsonPath("$.addressTypeName").isEmpty());
    }

    @Test
    void putReturns404WhenAddressDoesNotExist() throws Exception {
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class)))
                .thenThrow(new AddressNotFoundException("ESP", "INTERNAL", "EMP001", 9));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/9")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "postalCode": "28009",
                                  "regionCode": "MD"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReturns400WhenRequestIsInvalid() throws Exception {
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class)))
                .thenThrow(new IllegalArgumentException("street is required"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putReturns400WhenCountryCatalogValidationFails() throws Exception {
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class)))
                .thenThrow(new AddressCatalogValueInvalidException("countryCode", "BAD"));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "BAD",
                                  "postalCode": "28009",
                                  "regionCode": "MD"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private Address updatedAddress() {
        return new Address(
                20L,
                10L,
                1,
                "HOME",
                "Calle de Alcala 100",
                "Madrid",
                "ESP",
                "28009",
                "MD",
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // ADR-052 §4 (backend#24): el idioma entra por Accept-Language y llega al resolutor desde el ensamblador.
    @Test
    void listServesTheAddressTypeInTheLanguageOfTheAcceptLanguageHeader() throws Exception {
        when(listEmployeeAddressesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(updatedAddress()));
        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", "es-ES"))
                .thenReturn(Optional.of("Domicilio"));

        mockMvc.perform(get("/employees/ESP/INTERNAL/EMP001/addresses")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es-ES,es;q=0.9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].addressTypeName").value("Domicilio"));
    }
}
