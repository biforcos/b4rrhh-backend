package com.b4rrhh.rulesystem.companyprofile.infrastructure.web;

import com.b4rrhh.support.DireccionesDePrueba;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.GetCompanyProfileQuery;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.GetCompanyProfileUseCase;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.UpsertCompanyProfileCommand;
import com.b4rrhh.rulesystem.companyprofile.application.usecase.UpsertCompanyProfileUseCase;
import com.b4rrhh.rulesystem.companyprofile.domain.exception.CompanyProfileCompanyNotFoundException;
import com.b4rrhh.rulesystem.companyprofile.domain.model.CompanyProfile;
import com.b4rrhh.rulesystem.companyprofile.infrastructure.web.assembler.CompanyProfileResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyProfileControllerHttpTest {

    @Mock
    private GetCompanyProfileUseCase getCompanyProfileUseCase;
    @Mock
    private UpsertCompanyProfileUseCase upsertCompanyProfileUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CompanyProfileController controller = new CompanyProfileController(
                getCompanyProfileUseCase,
                upsertCompanyProfileUseCase,
                new CompanyProfileResponseAssembler()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CompanyProfileExceptionHandler())
                .build();
    }

    @Test
    void getReturnsExistingProfile() throws Exception {
        when(getCompanyProfileUseCase.get(any(GetCompanyProfileQuery.class)))
                .thenReturn(companyProfile());

        mockMvc.perform(get("/companies/ESP/ACME/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCode").value("ACME"))
                .andExpect(jsonPath("$.legalName").value("Acme Spain SA"))
                .andExpect(jsonPath("$.address.countryCode").value("ESP"));
    }

    @Test
    void getAlwaysReturnsNonNullAddressObject() throws Exception {
        when(getCompanyProfileUseCase.get(any(GetCompanyProfileQuery.class)))
                .thenReturn(new CompanyProfile("Acme Spain SA", null, null, null));

        mockMvc.perform(get("/companies/esp/acme/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCode").value("ACME"))
                .andExpect(jsonPath("$.address").exists())
                .andExpect(jsonPath("$.address.streetName").isEmpty());
    }

    @Test
    void putMapsPathAndBodyToCommand() throws Exception {
        when(upsertCompanyProfileUseCase.upsert(any(UpsertCompanyProfileCommand.class)))
                .thenReturn(companyProfile());

        mockMvc.perform(put("/companies/ESP/ACME/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "legalName": "Acme Spain SA",
                                  "taxIdentifier": "A12345678",
                                  "address": {
                                    "streetName": "Gran Via 1",
                                    "locality": "Madrid",
                                    "postalCode": "28013",
                                    "countryCode": "ESP"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCode").value("ACME"))
                .andExpect(jsonPath("$.legalName").value("Acme Spain SA"));

        ArgumentCaptor<UpsertCompanyProfileCommand> captor = ArgumentCaptor.forClass(UpsertCompanyProfileCommand.class);
        verify(upsertCompanyProfileUseCase).upsert(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("ACME", captor.getValue().companyCode());
        assertEquals("Acme Spain SA", captor.getValue().legalName());
        assertEquals("ESP", captor.getValue().address().countryCode());
    }

    @Test
    void getMapsMissingCompanyToHttp404() throws Exception {
        when(getCompanyProfileUseCase.get(any(GetCompanyProfileQuery.class)))
                .thenThrow(new CompanyProfileCompanyNotFoundException("ESP", "ACME"));

        mockMvc.perform(get("/companies/ESP/ACME/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Company not found")));
    }

    private CompanyProfile companyProfile() {
        return new CompanyProfile(
                "Acme Spain SA",
                "A12345678",
                DireccionesDePrueba.address("Gran Via 1", "Madrid", "ESP", "28013"),
                null
        );
    }
}