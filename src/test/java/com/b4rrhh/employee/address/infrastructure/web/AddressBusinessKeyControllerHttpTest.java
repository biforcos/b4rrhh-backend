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
import com.b4rrhh.employee.address.domain.exception.AddressCoverageGapException;
import com.b4rrhh.employee.address.domain.exception.AddressEmployeeNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressIsACorrectionException;
import com.b4rrhh.employee.address.domain.exception.AddressNotFoundException;
import com.b4rrhh.employee.address.domain.exception.AddressOverlapException;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.domain.model.AddressOccurrence;
import com.b4rrhh.employee.address.domain.model.AddressPeriod;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void putCarriesTheCorrectedDatesToTheCommand() throws Exception {
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
                                  "startDate": "2026-01-10",
                                  "endDate": "2026-06-30"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateAddressCommand> captor = ArgumentCaptor.forClass(UpdateAddressCommand.class);
        verify(updateAddressUseCase).update(captor.capture());
        assertEquals(LocalDate.of(2026, 1, 10), captor.getValue().startDate());
        assertEquals(LocalDate.of(2026, 6, 30), captor.getValue().endDate());
    }

    // ADR-057: a rejected correction is a 409 that names the gap and what to stretch.
    @Test
    void putMapsACoverageGapToHttp409NamingTheGapAndTheNeighbours() throws Exception {
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class)))
                .thenThrow(new AddressCoverageGapException(
                        "ESP", "INTERNAL", "EMP001", "HOME",
                        List.of(new AddressPeriod(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 31))),
                        List.of(new AddressOccurrence(1, LocalDate.of(2026, 2, 1), null))
                ));

        mockMvc.perform(put("/employees/ESP/INTERNAL/EMP001/addresses/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "startDate": "2026-02-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_COVERAGE_GAP"))
                .andExpect(jsonPath("$.details.addressTypeCode").value("HOME"))
                .andExpect(jsonPath("$.details.gaps[0].endDate[2]").value(31))
                .andExpect(jsonPath("$.details.stretchCandidates[0].addressNumber").value(1));
    }

    @Test
    void postMapsAnOverlapToHttp409WithTheSharedDates() throws Exception {
        when(createAddressUseCase.create(any()))
                .thenThrow(new AddressOverlapException(
                        "ESP", "INTERNAL", "EMP001", "HOME",
                        LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 10),
                        List.of(new AddressPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10)))
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/addresses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "addressTypeCode": "HOME",
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "startDate": "2026-01-20",
                                  "endDate": "2026-02-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_OVERLAP"))
                .andExpect(jsonPath("$.details.overlaps[0].startDate[1]").value(2));
    }

    @Test
    void postMapsACorrectionAskedForAsAnAddToHttp409NamingTheAddressToCorrect() throws Exception {
        when(createAddressUseCase.create(any()))
                .thenThrow(new AddressIsACorrectionException(
                        "ESP", "INTERNAL", "EMP001", "HOME",
                        new AddressOccurrence(1, LocalDate.of(2026, 1, 10), null),
                        new AddressPeriod(LocalDate.of(2026, 1, 10), null)
                ));

        mockMvc.perform(post("/employees/ESP/INTERNAL/EMP001/addresses")
                        .contentType("application/json")
                        .content("""
                                {
                                  "addressTypeCode": "HOME",
                                  "street": "Calle de Alcala 100",
                                  "city": "Madrid",
                                  "countryCode": "ESP",
                                  "startDate": "2026-01-10"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_IS_A_CORRECTION"))
                .andExpect(jsonPath("$.details.correctedOccurrence.addressNumber").value(1));
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
