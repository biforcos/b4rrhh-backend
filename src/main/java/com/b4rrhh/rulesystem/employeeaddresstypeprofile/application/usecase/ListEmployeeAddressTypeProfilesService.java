package com.b4rrhh.rulesystem.employeeaddresstypeprofile.application.usecase;

import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.model.EmployeeAddressTypeCoverage;
import com.b4rrhh.rulesystem.employeeaddresstypeprofile.domain.port.EmployeeAddressTypeProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/**
 * La cobertura de cada tipo de dirección de un sistema de reglas (b4rrhh/backend#145).
 *
 * <p>El catálogo la sabía desde la V117 y la cronología de direcciones la aplicaba al validar, pero
 * nadie la publicaba: la pantalla no podía saber qué dirección se puede cerrar —una opcional sí; la
 * obligatoria, mientras haya presencia, no— y por eso direcciones era la única sección sin
 * «Cerrar».
 */
@Service
public class ListEmployeeAddressTypeProfilesService implements ListEmployeeAddressTypeProfilesUseCase {

    private final EmployeeAddressTypeProfileRepository repository;

    public ListEmployeeAddressTypeProfilesService(EmployeeAddressTypeProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, EmployeeAddressTypeCoverage> list(String ruleSystemCode) {
        if (ruleSystemCode == null || ruleSystemCode.isBlank()) {
            throw new IllegalArgumentException("ruleSystemCode is required");
        }
        return repository.findAllCoverages(ruleSystemCode.trim().toUpperCase(Locale.ROOT));
    }
}
