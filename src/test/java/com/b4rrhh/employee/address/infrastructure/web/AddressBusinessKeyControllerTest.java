package com.b4rrhh.employee.address.infrastructure.web;

import com.b4rrhh.employee.address.application.usecase.CloseAddressCommand;
import com.b4rrhh.employee.address.application.usecase.CloseAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.CreateAddressCommand;
import com.b4rrhh.employee.address.application.usecase.CreateAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.DeleteAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.GetAddressByBusinessKeyUseCase;
import com.b4rrhh.employee.address.application.usecase.ListEmployeeAddressesUseCase;
import com.b4rrhh.employee.address.application.usecase.PlanAddressChangeUseCase;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressCommand;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressUseCase;
import com.b4rrhh.rulesystem.translation.application.service.RuleEntityLabelResolver;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.infrastructure.web.assembler.AddressResponseAssembler;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.CloseAddressRequest;
import com.b4rrhh.employee.address.infrastructure.web.dto.CreateAddressRequest;
import com.b4rrhh.employee.address.infrastructure.web.dto.UpdateAddressRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressBusinessKeyControllerTest {

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
    private DeleteAddressUseCase deleteAddressUseCase;
    @Mock
    private PlanAddressChangeUseCase planAddressChangeUseCase;
    @Mock
    private RuleEntityLabelResolver ruleEntityLabelResolver;

    private AddressBusinessKeyController controller;

    @BeforeEach
    void setUp() {
        controller = new AddressBusinessKeyController(
                createAddressUseCase,
                closeAddressUseCase,
                getAddressByBusinessKeyUseCase,
                listEmployeeAddressesUseCase,
                updateAddressUseCase,
                deleteAddressUseCase,
                planAddressChangeUseCase,
                new AddressResponseAssembler(ruleEntityLabelResolver)
        );
    }

    @Test
    void createsAddressUsingEmployeeBusinessKey() {
        CreateAddressRequest request = new CreateAddressRequest(
                "HOME",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                "28013",
                "MD",
                LocalDate.of(2026, 1, 10),
                null
        );

        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", null)).thenReturn(Optional.of("Domicilio"));
        when(createAddressUseCase.create(any(CreateAddressCommand.class))).thenReturn(activeAddress());

        ResponseEntity<AddressResponse> response = controller.create("ESP", "INTERNAL", "EMP001", request, ResponseLanguage.base());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().addressNumber());
        assertEquals("Domicilio", response.getBody().addressTypeName());

        ArgumentCaptor<CreateAddressCommand> captor = ArgumentCaptor.forClass(CreateAddressCommand.class);
        verify(createAddressUseCase).create(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals("HOME", captor.getValue().addressTypeCode());
    }

    @Test
    void listsAddressesUsingEmployeeBusinessKey() {
        when(listEmployeeAddressesUseCase.listByEmployeeBusinessKey("ESP", "INTERNAL", "EMP001"))
                .thenReturn(List.of(activeAddress()));

        ResponseEntity<List<AddressResponse>> response = controller.list("ESP", "INTERNAL", "EMP001", ResponseLanguage.base());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(1, response.getBody().get(0).addressNumber());
    }

    @Test
    void getsAddressByBusinessKeyAndAddressNumber() {
        when(getAddressByBusinessKeyUseCase.getByBusinessKey("ESP", "INTERNAL", "EMP001", 1))
                .thenReturn(Optional.of(activeAddress()));

        ResponseEntity<AddressResponse> response = controller.getByBusinessKey("ESP", "INTERNAL", "EMP001", 1, ResponseLanguage.base());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().addressNumber());
    }

    @Test
    void closesAddressUsingBusinessKeyAndAddressNumber() {
        CloseAddressRequest request = new CloseAddressRequest(LocalDate.of(2026, 2, 1));
        when(closeAddressUseCase.close(any(CloseAddressCommand.class))).thenReturn(closedAddress());

        ResponseEntity<AddressResponse> response = controller.close("ESP", "INTERNAL", "EMP001", 1, request, ResponseLanguage.base());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(LocalDate.of(2026, 2, 1), response.getBody().endDate());

        ArgumentCaptor<CloseAddressCommand> captor = ArgumentCaptor.forClass(CloseAddressCommand.class);
        verify(closeAddressUseCase).close(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().addressNumber());
    }

    @Test
    void updatesAddressUsingBusinessKeyAndAddressNumber() {
        UpdateAddressRequest request = new UpdateAddressRequest(
                "Calle de Alcala 100",
                "Madrid",
                "ESP",
                "28009",
                "MD",
                null,
                null
        );

        when(ruleEntityLabelResolver.resolveName("ESP", "EMPLOYEE_ADDRESS_TYPE", "HOME", null)).thenReturn(Optional.empty());
        when(updateAddressUseCase.update(any(UpdateAddressCommand.class))).thenReturn(updatedAddress());

        ResponseEntity<AddressResponse> response = controller.update("ESP", "INTERNAL", "EMP001", 1, request, ResponseLanguage.base());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Calle de Alcala 100", response.getBody().street());
        assertEquals("HOME", response.getBody().addressTypeCode());
        assertNull(response.getBody().addressTypeName());

        ArgumentCaptor<UpdateAddressCommand> captor = ArgumentCaptor.forClass(UpdateAddressCommand.class);
        verify(updateAddressUseCase).update(captor.capture());
        assertEquals("ESP", captor.getValue().ruleSystemCode());
        assertEquals("INTERNAL", captor.getValue().employeeTypeCode());
        assertEquals("EMP001", captor.getValue().employeeNumber());
        assertEquals(1, captor.getValue().addressNumber());
        assertEquals("Calle de Alcala 100", captor.getValue().street());
    }

    private Address activeAddress() {
        return new Address(
                20L,
                10L,
                1,
                "HOME",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                "28013",
                "MD",
                LocalDate.of(2026, 1, 10),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Address closedAddress() {
        return new Address(
                20L,
                10L,
                1,
                "HOME",
                "Calle Mayor 10",
                "Madrid",
                "ESP",
                "28013",
                "MD",
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 2, 1),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
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
}
