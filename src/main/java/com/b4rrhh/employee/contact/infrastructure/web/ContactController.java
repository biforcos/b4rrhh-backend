package com.b4rrhh.employee.contact.infrastructure.web;

import com.b4rrhh.employee.contact.application.usecase.CreateContactCommand;
import com.b4rrhh.employee.contact.application.usecase.CreateContactUseCase;
import com.b4rrhh.employee.contact.application.usecase.DeleteContactCommand;
import com.b4rrhh.employee.contact.application.usecase.DeleteContactUseCase;
import com.b4rrhh.employee.contact.application.usecase.GetContactByBusinessKeyUseCase;
import com.b4rrhh.employee.contact.application.usecase.ListEmployeeContactsUseCase;
import com.b4rrhh.employee.contact.application.usecase.UpdateContactCommand;
import com.b4rrhh.employee.contact.application.usecase.UpdateContactUseCase;
import com.b4rrhh.employee.contact.domain.model.Contact;
import com.b4rrhh.employee.contact.infrastructure.web.assembler.ContactResponseAssembler;
import com.b4rrhh.employee.contact.infrastructure.web.dto.ContactResponse;
import com.b4rrhh.employee.contact.infrastructure.web.dto.CreateContactRequest;
import com.b4rrhh.employee.contact.infrastructure.web.dto.UpdateContactRequest;
import com.b4rrhh.shared.infrastructure.web.language.ResponseLanguage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/contacts")
public class ContactController {

    private final CreateContactUseCase createContactUseCase;
    private final UpdateContactUseCase updateContactUseCase;
    private final GetContactByBusinessKeyUseCase getContactByBusinessKeyUseCase;
    private final ListEmployeeContactsUseCase listEmployeeContactsUseCase;
    private final DeleteContactUseCase deleteContactUseCase;
        private final ContactResponseAssembler contactResponseAssembler;

    public ContactController(
            CreateContactUseCase createContactUseCase,
            UpdateContactUseCase updateContactUseCase,
            GetContactByBusinessKeyUseCase getContactByBusinessKeyUseCase,
            ListEmployeeContactsUseCase listEmployeeContactsUseCase,
                        DeleteContactUseCase deleteContactUseCase,
                        ContactResponseAssembler contactResponseAssembler
    ) {
        this.createContactUseCase = createContactUseCase;
        this.updateContactUseCase = updateContactUseCase;
        this.getContactByBusinessKeyUseCase = getContactByBusinessKeyUseCase;
        this.listEmployeeContactsUseCase = listEmployeeContactsUseCase;
        this.deleteContactUseCase = deleteContactUseCase;
                this.contactResponseAssembler = contactResponseAssembler;
    }

    @PostMapping
    public ResponseEntity<ContactResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody CreateContactRequest request,
            ResponseLanguage language
    ) {
        Contact created = createContactUseCase.create(
                new CreateContactCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        request.contactTypeCode(),
                        request.contactValue()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(contactResponseAssembler.toResponse(ruleSystemCode, created, language));
    }

    @GetMapping
    public ResponseEntity<List<ContactResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            ResponseLanguage language
    ) {
        List<ContactResponse> response = listEmployeeContactsUseCase
                .listByEmployeeBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .stream()
                .map(contact -> contactResponseAssembler.toResponse(ruleSystemCode, contact, language))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{contactTypeCode}")
    public ResponseEntity<ContactResponse> getByBusinessKey(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String contactTypeCode,
            ResponseLanguage language
    ) {
        return getContactByBusinessKeyUseCase.getByBusinessKey(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        contactTypeCode
                )
                .map(contact -> ResponseEntity.ok(contactResponseAssembler.toResponse(ruleSystemCode, contact, language)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{contactTypeCode}")
    public ResponseEntity<ContactResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String contactTypeCode,
            @RequestBody UpdateContactRequest request,
            ResponseLanguage language
    ) {
        Contact updated = updateContactUseCase.update(
                new UpdateContactCommand(
                        ruleSystemCode,
                        employeeTypeCode,
                        employeeNumber,
                        contactTypeCode,
                        request.getContactValue()
                )
        );

        return ResponseEntity.ok(contactResponseAssembler.toResponse(ruleSystemCode, updated, language));
    }

    @DeleteMapping("/{contactTypeCode}")
    public ResponseEntity<Void> delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @PathVariable String contactTypeCode
    ) {
        deleteContactUseCase.delete(
                new DeleteContactCommand(ruleSystemCode, employeeTypeCode, employeeNumber, contactTypeCode)
        );
        return ResponseEntity.noContent().build();
    }
}
