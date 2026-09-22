package com.b4rrhh.rulesystem.company.infrastructure.web;

import com.b4rrhh.rulesystem.company.application.usecase.CreateCompanyCommand;
import com.b4rrhh.rulesystem.company.application.usecase.CreateCompanyUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.GetCompanyQuery;
import com.b4rrhh.rulesystem.company.application.usecase.GetCompanyUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.ListCompaniesQuery;
import com.b4rrhh.rulesystem.company.application.usecase.ListCompaniesUseCase;
import com.b4rrhh.rulesystem.company.application.usecase.UpdateCompanyCommand;
import com.b4rrhh.rulesystem.company.application.usecase.UpdateCompanyUseCase;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyAlreadyExistsException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotApplicableException;
import com.b4rrhh.rulesystem.company.domain.exception.CompanyNotFoundException;
import com.b4rrhh.rulesystem.company.domain.model.Company;
import com.b4rrhh.rulesystem.company.infrastructure.web.assembler.CompanyResponseAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompanyControllerHttpTest {

    @Mock
    private CreateCompanyUseCase createCompanyUseCase;
    @Mock
    private ListCompaniesUseCase listCompaniesUseCase;
    @Mock
    private GetCompanyUseCase getCompanyUseCase;
    @Mock
    private UpdateCompanyUseCase updateCompanyUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CompanyController controller = new CompanyController(
                createCompanyUseCase,
                listCompaniesUseCase,
                getCompanyUseCase,
                updateCompanyUseCase,
                new CompanyResponseAssembler()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new CompanyExceptionHandler())
                .build();
    }

    @Test
    void postCreatesAggregatedCompany() throws Exception {
        when(createCompanyUseCase.create(any(CreateCompanyCommand.class))).thenReturn(company());

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "companyCode": "ACME",
                                  "name": "Acme",
                                  "description": "Main company",
                                  "startDate": "2026-01-01",
                                  "legalName": "Acme Spain SA",
                                  "taxIdentifier": "A12345678",
                                  "address": {
                                    "street": "Gran Via 1",
                                    "city": "Madrid",
                                    "postalCode": "28013",
                                    "regionCode": "MD",
                                    "countryCode": "ESP"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.companyCode").value("ACME"))
                .andExpect(jsonPath("$.legalName").value("Acme Spain SA"));

        ArgumentCaptor<CreateCompanyCommand> captor = ArgumentCaptor.forClass(CreateCompanyCommand.class);
        verify(createCompanyUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("ACME", captor.getValue().companyCode());
    }

    @Test
    void getListReturnsAggregatedList() throws Exception {
        when(listCompaniesUseCase.list(any(ListCompaniesQuery.class))).thenReturn(List.of(company()));

        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].companyCode").value("ACME"))
                .andExpect(jsonPath("$[0].countryCode").value("ESP"));
    }

    @Test
    void getByBusinessKeyReturnsAggregatedCompany() throws Exception {
        when(getCompanyUseCase.get(any(GetCompanyQuery.class))).thenReturn(company());

        mockMvc.perform(get("/companies/ESP/ACME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme"));
    }

    @Test
    void putUpdatesAggregatedCompany() throws Exception {
        when(updateCompanyUseCase.update(any(UpdateCompanyCommand.class))).thenReturn(company());

        mockMvc.perform(put("/companies/ESP/ACME")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Acme",
                                  "description": "Main company",
                                  "legalName": "Acme Spain SA",
                                  "taxIdentifier": "A12345678",
                                  "address": {
                                    "street": "Gran Via 1",
                                    "city": "Madrid",
                                    "postalCode": "28013",
                                    "regionCode": "MD",
                                    "countryCode": "ESP"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyCode").value("ACME"));
    }

    @Test
    void postReturns409WhenCompanyAlreadyExists() throws Exception {
        when(createCompanyUseCase.create(any(CreateCompanyCommand.class)))
                .thenThrow(new CompanyAlreadyExistsException("ESP", "ACME"));

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "ruleSystemCode": "ESP",
                                  "companyCode": "ACME",
                                  "name": "Acme",
                                  "startDate": "2026-01-01",
                                  "legalName": "Acme Spain SA"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void getReturns409WhenCompanyNotApplicableToday() throws Exception {
        when(getCompanyUseCase.get(any(GetCompanyQuery.class)))
                .thenThrow(new CompanyNotApplicableException("ESP", "ACME", LocalDate.now()));

        mockMvc.perform(get("/companies/ESP/ACME"))
                .andExpect(status().isConflict());
    }

    @Test
    void getReturns404WhenCompanyNotFound() throws Exception {
        when(getCompanyUseCase.get(any(GetCompanyQuery.class)))
                .thenThrow(new CompanyNotFoundException("ESP", "ACME"));

        mockMvc.perform(get("/companies/ESP/ACME"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Company not found")));
    }

    private Company company() {
        return new Company(
                "ESP",
                "ACME",
                "Acme",
                "Main company",
                LocalDate.of(2026, 1, 1),
                null,
                true,
                "Acme Spain SA",
                "A12345678",
                "Gran Via 1",
                "Madrid",
                "28013",
                "MD",
                "ESP",
                "4719"
        );
    }
}
