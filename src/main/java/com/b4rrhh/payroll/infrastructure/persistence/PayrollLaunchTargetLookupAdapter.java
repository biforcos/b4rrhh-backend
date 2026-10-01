package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollLaunchTargetLookupPort;
import com.b4rrhh.rulesystem.domain.model.RuleEntity;
import com.b4rrhh.rulesystem.domain.port.RuleEntityRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Los tipos de empleado del catalogo y la existencia de un empleado, para el lanzamiento
 * ({@code frontend#88}). La existencia del empleado la lee de su tabla por consulta nativa, como
 * {@link PayrollLaunchPresenceLookupAdapter}; los tipos, del puerto que resuelve el catálogo por
 * el nivel del tipo (backend#157).
 */
@Component
public class PayrollLaunchTargetLookupAdapter implements PayrollLaunchTargetLookupPort {

    private static final String EMPLOYEE_TYPE = "EMPLOYEE_TYPE";

    private final EntityManager entityManager;
    private final RuleEntityRepository ruleEntityRepository;

    public PayrollLaunchTargetLookupAdapter(EntityManager entityManager, RuleEntityRepository ruleEntityRepository) {
        this.entityManager = entityManager;
        this.ruleEntityRepository = ruleEntityRepository;
    }

    @Override
    public List<String> findEmployeeTypeCodes(String ruleSystemCode) {
        // Ordenados por código en la base, que es el orden del puerto.
        return ruleEntityRepository.findByFilters(ruleSystemCode, EMPLOYEE_TYPE, null, true, null).stream()
                .map(RuleEntity::getCode)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    @Override
    public boolean employeeExists(String ruleSystemCode, String employeeTypeCode, String employeeNumber) {
        Number cuantos = (Number) entityManager.createNativeQuery("""
            select count(*)
              from employee.employee e
             where upper(trim(e.rule_system_code)) = :ruleSystemCode
               and upper(trim(e.employee_type_code)) = :employeeTypeCode
               and trim(e.employee_number) = :employeeNumber
            """)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .setParameter("employeeTypeCode", employeeTypeCode)
                .setParameter("employeeNumber", employeeNumber)
                .getSingleResult();
        return cuantos.longValue() > 0;
    }
}
