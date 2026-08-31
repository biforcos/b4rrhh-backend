package com.b4rrhh.rulesystem.workcenter.infrastructure.web;

import com.b4rrhh.rulesystem.workcenter.application.usecase.CreateWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.DeleteWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.GetWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.ListWorkCenterContactsUseCase;
import com.b4rrhh.rulesystem.workcenter.application.usecase.UpdateWorkCenterContactUseCase;
import com.b4rrhh.rulesystem.workcenter.domain.model.WorkCenterContact;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.assembler.WorkCenterResponseAssembler;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.CreateWorkCenterContactRequest;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.UpdateWorkCenterContactRequest;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.dto.WorkCenterContactResponse;
import com.b4rrhh.rulesystem.workcenter.infrastructure.web.mapper.WorkCenterContactCommandMapper;
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
@RequestMapping("/work-centers/{ruleSystemCode}/{workCenterCode}/contacts")
public class WorkCenterContactController {

    private final CreateWorkCenterContactUseCase createWorkCenterContactUseCase;
    private final UpdateWorkCenterContactUseCase updateWorkCenterContactUseCase;
    private final DeleteWorkCenterContactUseCase deleteWorkCenterContactUseCase;
    private final GetWorkCenterContactUseCase getWorkCenterContactUseCase;
    private final ListWorkCenterContactsUseCase listWorkCenterContactsUseCase;
        private final WorkCenterContactCommandMapper workCenterContactCommandMapper;
    private final WorkCenterResponseAssembler workCenterResponseAssembler;

    public WorkCenterContactController(
            CreateWorkCenterContactUseCase createWorkCenterContactUseCase,
            UpdateWorkCenterContactUseCase updateWorkCenterContactUseCase,
            DeleteWorkCenterContactUseCase deleteWorkCenterContactUseCase,
            GetWorkCenterContactUseCase getWorkCenterContactUseCase,
            ListWorkCenterContactsUseCase listWorkCenterContactsUseCase,
            WorkCenterContactCommandMapper workCenterContactCommandMapper,
            WorkCenterResponseAssembler workCenterResponseAssembler
    ) {
        this.createWorkCenterContactUseCase = createWorkCenterContactUseCase;
        this.updateWorkCenterContactUseCase = updateWorkCenterContactUseCase;
        this.deleteWorkCenterContactUseCase = deleteWorkCenterContactUseCase;
        this.getWorkCenterContactUseCase = getWorkCenterContactUseCase;
        this.listWorkCenterContactsUseCase = listWorkCenterContactsUseCase;
        this.workCenterContactCommandMapper = workCenterContactCommandMapper;
        this.workCenterResponseAssembler = workCenterResponseAssembler;
    }

    @GetMapping
    public ResponseEntity<List<WorkCenterContactResponse>> list(
            @PathVariable String ruleSystemCode,
            @PathVariable String workCenterCode,
            ResponseLanguage language
    ) {
        return ResponseEntity.ok(
                listWorkCenterContactsUseCase.list(ruleSystemCode, workCenterCode)
                        .stream()
                        .map(contact -> workCenterResponseAssembler.toContactResponse(ruleSystemCode, contact, language))
                        .toList()
        );
    }

    @PostMapping
    public ResponseEntity<WorkCenterContactResponse> create(
            @PathVariable String ruleSystemCode,
            @PathVariable String workCenterCode,
            @RequestBody CreateWorkCenterContactRequest request,
            ResponseLanguage language
    ) {
        WorkCenterContact created = createWorkCenterContactUseCase.create(
                workCenterContactCommandMapper.toCreateCommand(ruleSystemCode, workCenterCode, request)
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workCenterResponseAssembler.toContactResponse(ruleSystemCode, created, language));
    }

    @GetMapping("/{contactNumber}")
    public ResponseEntity<WorkCenterContactResponse> get(
            @PathVariable String ruleSystemCode,
            @PathVariable String workCenterCode,
            @PathVariable Integer contactNumber,
            ResponseLanguage language
    ) {
        return ResponseEntity.ok(workCenterResponseAssembler.toContactResponse(
                ruleSystemCode,
                getWorkCenterContactUseCase.get(
                        workCenterContactCommandMapper.toGetQuery(ruleSystemCode, workCenterCode, contactNumber)
                ),
                language
        ));
    }

    @PutMapping("/{contactNumber}")
    public ResponseEntity<WorkCenterContactResponse> update(
            @PathVariable String ruleSystemCode,
            @PathVariable String workCenterCode,
            @PathVariable Integer contactNumber,
            @RequestBody UpdateWorkCenterContactRequest request,
            ResponseLanguage language
    ) {
        WorkCenterContact updated = updateWorkCenterContactUseCase.update(
                workCenterContactCommandMapper.toUpdateCommand(ruleSystemCode, workCenterCode, contactNumber, request)
        );

        return ResponseEntity.ok(workCenterResponseAssembler.toContactResponse(ruleSystemCode, updated, language));
    }

    @DeleteMapping("/{contactNumber}")
    public ResponseEntity<Void> delete(
            @PathVariable String ruleSystemCode,
            @PathVariable String workCenterCode,
            @PathVariable Integer contactNumber
    ) {
        deleteWorkCenterContactUseCase.delete(
                workCenterContactCommandMapper.toDeleteCommand(ruleSystemCode, workCenterCode, contactNumber)
        );

        return ResponseEntity.noContent().build();
    }
}