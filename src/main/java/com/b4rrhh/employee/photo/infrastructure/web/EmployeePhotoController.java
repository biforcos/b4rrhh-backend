package com.b4rrhh.employee.photo.infrastructure.web;

import com.b4rrhh.employee.employee.application.usecase.GetEmployeeByBusinessKeyUseCase;
import com.b4rrhh.employee.employee.domain.model.Employee;
import com.b4rrhh.employee.employee.infrastructure.web.EmployeeResponseAssembler;
import com.b4rrhh.employee.employee.infrastructure.web.dto.EmployeeResponse;
import com.b4rrhh.employee.photo.application.usecase.DeleteEmployeePhotoCommand;
import com.b4rrhh.employee.photo.application.usecase.DeleteEmployeePhotoUseCase;
import com.b4rrhh.employee.photo.application.usecase.EmployeePhotoContent;
import com.b4rrhh.employee.photo.application.usecase.GetEmployeePhotoQuery;
import com.b4rrhh.employee.photo.application.usecase.GetEmployeePhotoUseCase;
import com.b4rrhh.employee.photo.application.usecase.UploadEmployeePhotoCommand;
import com.b4rrhh.employee.photo.application.usecase.UploadEmployeePhotoUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * La foto del empleado viaja siempre por la API (backend#177): se lee con {@code GET}, se sube con
 * {@code PUT} y se borra con {@code DELETE}. El almacén no se ve desde fuera.
 */
@RestController
@RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/photo")
public class EmployeePhotoController {

    private final UploadEmployeePhotoUseCase uploadEmployeePhotoUseCase;
    private final GetEmployeePhotoUseCase getEmployeePhotoUseCase;
    private final DeleteEmployeePhotoUseCase deleteEmployeePhotoUseCase;
    private final GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase;
    private final EmployeeResponseAssembler employeeResponseAssembler;

    public EmployeePhotoController(
            UploadEmployeePhotoUseCase uploadEmployeePhotoUseCase,
            GetEmployeePhotoUseCase getEmployeePhotoUseCase,
            DeleteEmployeePhotoUseCase deleteEmployeePhotoUseCase,
            GetEmployeeByBusinessKeyUseCase getEmployeeByBusinessKeyUseCase,
            EmployeeResponseAssembler employeeResponseAssembler) {
        this.uploadEmployeePhotoUseCase = uploadEmployeePhotoUseCase;
        this.getEmployeePhotoUseCase = getEmployeePhotoUseCase;
        this.deleteEmployeePhotoUseCase = deleteEmployeePhotoUseCase;
        this.getEmployeeByBusinessKeyUseCase = getEmployeeByBusinessKeyUseCase;
        this.employeeResponseAssembler = employeeResponseAssembler;
    }

    /**
     * Sin token, a propósito: la pide un {@code <img>}, que no manda la cabecera {@code Authorization}.
     * La llave es {@code v}, el UUID de la clave vigente, que sólo conoce quien ha leído el empleado con
     * token. Es lo mismo que exponía el cubo público —una URL con UUID—, menos lo que ya no está: una
     * foto cambiada o borrada deja de servirse. Ver {@code SecurityConfig}.
     *
     * <p>Se cachea un año: la versión va en el enlace, así que una foto nueva es un enlace nuevo.
     */
    @GetMapping(produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestParam(name = "v", required = false) String version
    ) {
        Optional<EmployeePhotoContent> foto = getEmployeePhotoUseCase.get(
                new GetEmployeePhotoQuery(ruleSystemCode, employeeTypeCode, employeeNumber, version));
        return foto
                .map(contenido -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .eTag(contenido.objectKey())
                        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                        .body(contenido.jpeg()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping(consumes = MediaType.IMAGE_JPEG_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmployeeResponse> uploadPhoto(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber,
            @RequestBody byte[] jpeg
    ) {
        uploadEmployeePhotoUseCase.upload(
                new UploadEmployeePhotoCommand(ruleSystemCode, employeeTypeCode, employeeNumber, jpeg));

        Employee employee = getEmployeeByBusinessKeyUseCase
                .getByBusinessKey(ruleSystemCode, employeeTypeCode, employeeNumber)
                .orElseThrow();

        return ResponseEntity.ok(employeeResponseAssembler.toResponse(employee));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable String ruleSystemCode,
            @PathVariable String employeeTypeCode,
            @PathVariable String employeeNumber
    ) {
        deleteEmployeePhotoUseCase.delete(
                new DeleteEmployeePhotoCommand(ruleSystemCode, employeeTypeCode, employeeNumber));

        return ResponseEntity.noContent().build();
    }
}
