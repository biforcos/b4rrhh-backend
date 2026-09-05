package com.b4rrhh.employee.address.infrastructure.web;

import com.b4rrhh.employee.address.application.usecase.CloseAddressCommand;
import com.b4rrhh.employee.address.application.usecase.CloseAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.CreateAddressCommand;
import com.b4rrhh.employee.address.application.usecase.CreateAddressUseCase;
import com.b4rrhh.employee.address.application.usecase.GetAddressByBusinessKeyUseCase;
import com.b4rrhh.employee.address.application.usecase.ListEmployeeAddressesUseCase;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressCommand;
import com.b4rrhh.employee.address.application.usecase.UpdateAddressUseCase;
import com.b4rrhh.employee.address.domain.model.Address;
import com.b4rrhh.employee.address.infrastructure.web.assembler.AddressResponseAssembler;
import com.b4rrhh.employee.address.infrastructure.web.dto.AddressResponse;
import com.b4rrhh.employee.address.infrastructure.web.dto.CloseAddressRequest;
import com.b4rrhh.employee.address.infrastructure.web.dto.CreateAddressRequest;
import com.b4rrhh.employee.address.infrastructure.web.dto.UpdateAddressRequest;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/addresses")
public class AddressBusinessKeyController {

    private final CreateAddressUseCase createAddressUseCase;
    private final CloseAddressUseCase closeAddressUseCase;
    private final GetAddressByBusinessKeyUseCase getAddressByBusinessKeyUseCase;
    private final ListEmployeeAddressesUseCase listEmployeeAddressesUseCase;
        private final UpdateAddressUseCase updateAddressUseCase;
        private final AddressResponseAssembler addressResponseAssembler;

    public AddressBusinessKeyController(
            CreateAddressUseCase createAddressUseCase,
            CloseAddressUseCase closeAddressUseCase,
            GetAddressByBusinessKeyUseCase getAddressByBusinessKeyUseCase,
                        ListEmployeeAddressesUseCase listEmployeeAddressesUseCase,
                        UpdateAddressUseCase updateAddressUseCase,
                        AddressResponseAssembler addressResponseAssembler
    ) {
        this.createAddressUseCase = createAddressUseCase;
        this.closeAddressUseCase = closeAddressUseCase;
        this.getAddressByBusinessKeyUseCase = getAddressByBusinessKeyUseCase;
        this.listEmployeeAddressesUseCase = listEmployeeAddressesUseCase;
                this.updateAddressUseCase = updateAddressUseCase;
                this.addressResponseAssembler = addressResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateAddressRequest request,
            ResponseLanguage language
    ) {
        Address created = createAddressUseCase.create(
                new CreateAddressCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.addressTypeCode(),
                        request.street(),
                        request.city(),
                        request.countryCode(),
                        request.postalCode(),
                        request.regionCode(),
                        request.startDate(),
                        request.endDate()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(addressResponseAssembler.toResponse(ruleSystemCode, created, language));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        List<AddressResponse> response = listEmployeeAddressesUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .stream()
                .map(address -> addressResponseAssembler.toResponse(ruleSystemCode, address, language))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{addressNumber}")
    public ResponseEntity<AddressResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer addressNumber,
            ResponseLanguage language
    ) {
        return getAddressByBusinessKeyUseCase.getByBusinessKey(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        addressNumber
                )
                .map(address -> ResponseEntity.ok(addressResponseAssembler.toResponse(ruleSystemCode, address, language)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{addressNumber}")
    public ResponseEntity<AddressResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer addressNumber,
            @RequestBody UpdateAddressRequest request,
            ResponseLanguage language
    ) {
        Address updated = updateAddressUseCase.update(
                new UpdateAddressCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        addressNumber,
                        request.street(),
                        request.city(),
                        request.countryCode(),
                        request.postalCode(),
                        request.regionCode(),
                        request.startDate(),
                        request.endDate()
                )
        );

        return ResponseEntity.ok(addressResponseAssembler.toResponse(ruleSystemCode, updated, language));
    }

    @PostMapping("/{addressNumber}/close")
    public ResponseEntity<AddressResponse> close(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable Integer addressNumber,
            @RequestBody CloseAddressRequest request,
            ResponseLanguage language
    ) {
        Address closed = closeAddressUseCase.close(
                new CloseAddressCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        addressNumber,
                        request.endDate()
                )
        );

        return ResponseEntity.ok(addressResponseAssembler.toResponse(ruleSystemCode, closed, language));
    }
}
