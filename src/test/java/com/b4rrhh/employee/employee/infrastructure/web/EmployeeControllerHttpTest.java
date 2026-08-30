package com.b4rrhh.employee.employee.infrastructure.web;

import com.b4rrhh.employee.employee.application.DisplayNameComputationService;
import com.b4rrhh.employee.employee.application.usecase.CreateEmployeeUseCase;
import com.b4rrhh.employee.employee.application.usecase.ListEmployeesQuery;
import com.b4rrhh.employee.employee.application.usecase.ListEmployeesUseCase;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryItem;
import com.b4rrhh.employee.employee.domain.model.EmployeeDirectoryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerHttpTest {

    @Mock
    private CreateEmployeeUseCase createEmployeeUseCase;
    @Mock
    private ListEmployeesUseCase listEmployeesUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DisplayNameComputationService displayNameComputationService =
                new DisplayNameComputationService(ruleSystemCode -> java.util.Optional.empty());
        EmployeeController controller = new EmployeeController(createEmployeeUseCase, listEmployeesUseCase, displayNameComputationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new EmployeeExceptionHandler())
                .build();
    }

    @Test
    void listMapsQueryParamsToUseCaseAndReturnsOnePageWithTotal() throws Exception {
        List<EmployeeDirectoryItem> items = List.of(
                new EmployeeDirectoryItem(
                        "ESP",
                        "INTERNAL",
                        "EMP001",
                        "Lidia Morales",
                        "ACTIVE",
                        "MADRID_HQ"
                )
        );
        when(listEmployeesUseCase.list(any(ListEmployeesQuery.class)))
                .thenReturn(new EmployeeDirectoryPage(items, 1, 2, 7));

        mockMvc.perform(get("/employees")
                        .queryParam("q", "lidia")
                        .queryParam("ruleSystemCode", "esp")
                        .queryParam("employeeTypeCode", "internal")
                        .queryParam("status", "active")
                        .queryParam("size", "2")
                        .queryParam("page", "1"))
                .andExpect(status().isOk())
                // La página se envuelve: items + page + size + total (backend#18, opción A).
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.total").value(7))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].ruleSystemCode").value("ESP"))
                .andExpect(jsonPath("$.items[0].employeeTypeCode").value("INTERNAL"))
                .andExpect(jsonPath("$.items[0].employeeNumber").value("EMP001"))
                .andExpect(jsonPath("$.items[0].displayName").value("Lidia Morales"))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].workCenterCode").value("MADRID_HQ"))
                .andExpect(jsonPath("$.items[0].id").doesNotExist());

        ArgumentCaptor<ListEmployeesQuery> captor = ArgumentCaptor.forClass(ListEmployeesQuery.class);
        verify(listEmployeesUseCase).list(captor.capture());
        assertEquals("lidia", captor.getValue().q());
        assertEquals("esp", captor.getValue().ruleSystemCode());
        assertEquals("internal", captor.getValue().employeeTypeCode());
        assertEquals("active", captor.getValue().status());
        assertEquals(2, captor.getValue().size());
        assertEquals(1, captor.getValue().page());
    }

    @Test
    void listReturnsBadRequestWhenUseCaseRejectsParams() throws Exception {
        when(listEmployeesUseCase.list(any(ListEmployeesQuery.class)))
                .thenThrow(new IllegalArgumentException("size must be between 1 and 200"));

        mockMvc.perform(get("/employees").queryParam("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must be between 1 and 200"));
    }
}