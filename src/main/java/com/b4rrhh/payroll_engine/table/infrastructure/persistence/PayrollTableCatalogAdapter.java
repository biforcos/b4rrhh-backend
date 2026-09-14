package com.b4rrhh.payroll_engine.table.infrastructure.persistence;

import com.b4rrhh.payroll.basesalary.infrastructure.persistence.entity.PayrollObjectBindingEntity;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository.PayrollObjectBindingRepository;
import com.b4rrhh.payroll.basesalary.infrastructure.persistence.repository.PayrollTableRowRepository;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableBinding;
import com.b4rrhh.payroll_engine.table.domain.model.PayrollTableRowCount;
import com.b4rrhh.payroll_engine.table.domain.port.PayrollTableCatalogPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las dos consultas que contestan "que tablas hay", y nada mas: juntarlas es
 * cosa del servicio (backend#95).
 */
@Component
public class PayrollTableCatalogAdapter implements PayrollTableCatalogPort {

    private static final String BOUND_OBJECT_TYPE_TABLE = "TABLE";

    private final PayrollTableRowRepository rowRepository;
    private final PayrollObjectBindingRepository bindingRepository;

    public PayrollTableCatalogAdapter(
            PayrollTableRowRepository rowRepository,
            PayrollObjectBindingRepository bindingRepository
    ) {
        this.rowRepository = rowRepository;
        this.bindingRepository = bindingRepository;
    }

    @Override
    public List<PayrollTableRowCount> countRowsByTableCode(String ruleSystemCode) {
        List<PayrollTableRowCount> recuentos = new ArrayList<>();
        for (Object[] fila : rowRepository.countRowsByTableCode(ruleSystemCode)) {
            recuentos.add(new PayrollTableRowCount(
                    (String) fila[0],
                    ((Number) fila[1]).longValue(),
                    fila[2] == null ? 0L : ((Number) fila[2]).longValue()
            ));
        }
        return recuentos;
    }

    @Override
    public Map<String, List<PayrollTableBinding>> findTableBindingsByTableCode(String ruleSystemCode) {
        Map<String, List<PayrollTableBinding>> porTabla = new LinkedHashMap<>();
        List<PayrollObjectBindingEntity> vinculaciones =
                bindingRepository.findByRuleSystemCodeAndBoundObjectTypeCode(
                        ruleSystemCode, BOUND_OBJECT_TYPE_TABLE);
        for (PayrollObjectBindingEntity vinculacion : vinculaciones) {
            porTabla.computeIfAbsent(vinculacion.getBoundObjectCode(), codigo -> new ArrayList<>())
                    .add(new PayrollTableBinding(
                            vinculacion.getOwnerTypeCode(),
                            vinculacion.getOwnerCode(),
                            vinculacion.getBindingRoleCode(),
                            Boolean.TRUE.equals(vinculacion.getActive())
                    ));
        }
        return porTabla;
    }
}
