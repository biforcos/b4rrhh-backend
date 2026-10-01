package com.b4rrhh.payroll.infrastructure.persistence;

import com.b4rrhh.payroll.application.port.PayrollLaunchTargetLookupPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Los tipos de empleado del catalogo y la existencia de un empleado, para el lanzamiento
 * ({@code frontend#88}). Lee de las tablas de los otros contextos por consulta nativa, como
 * {@link PayrollLaunchPresenceLookupAdapter}.
 */
@Component
public class PayrollLaunchTargetLookupAdapter implements PayrollLaunchTargetLookupPort {

    private final EntityManager entityManager;

    public PayrollLaunchTargetLookupAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<String> findEmployeeTypeCodes(String ruleSystemCode) {
        List<?> rows = entityManager.createNativeQuery("""
            select distinct upper(trim(e.code))
              from rulesystem.rule_entity e
             where upper(trim(e.layer_code)) = :ruleSystemCode
               and e.rule_entity_type_code = 'EMPLOYEE_TYPE'
               and e.active
             order by 1
            """)
                .setParameter("ruleSystemCode", ruleSystemCode)
                .getResultList();
        return rows.stream().map(String::valueOf).toList();
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
