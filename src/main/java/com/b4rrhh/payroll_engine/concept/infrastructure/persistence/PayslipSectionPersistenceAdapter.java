package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSubsection;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayslipSectionPersistenceAdapter implements PayslipSectionRepository {

    private final SpringDataPayslipSectionRepository sectionRepository;
    private final SpringDataPayslipSectionNatureRepository natureRepository;
    private final SpringDataPayslipSubsectionRepository subsectionRepository;
    private final JdbcTemplate jdbc;

    public PayslipSectionPersistenceAdapter(
            SpringDataPayslipSectionRepository sectionRepository,
            SpringDataPayslipSectionNatureRepository natureRepository,
            SpringDataPayslipSubsectionRepository subsectionRepository,
            JdbcTemplate jdbc
    ) {
        this.sectionRepository = sectionRepository;
        this.natureRepository = natureRepository;
        this.subsectionRepository = subsectionRepository;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PayslipSection> findByRuleSystemCode(String ruleSystemCode) {
        Map<String, List<PayslipSubsection>> porSeccion = new LinkedHashMap<>();
        for (PayslipSubsectionEntity e : subsectionRepository.findOrdered(ruleSystemCode)) {
            porSeccion.computeIfAbsent(e.getSectionCode(), key -> new ArrayList<>())
                    .add(new PayslipSubsection(
                            e.getSubsectionCode(), e.getSubsectionLabel(), e.getDisplayOrder()));
        }
        return sectionRepository.findOrdered(ruleSystemCode).stream()
                .map(e -> new PayslipSection(
                        e.getSectionCode(),
                        e.getSectionLabel(),
                        e.getDisplayOrder(),
                        porSeccion.getOrDefault(e.getSectionCode(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findSectionCodeByNature(String ruleSystemCode) {
        Map<String, String> byNature = new LinkedHashMap<>();
        for (PayslipSectionNatureEntity e : natureRepository.findByRuleSystemCode(ruleSystemCode)) {
            byNature.put(e.getFunctionalNature(), e.getSectionCode());
        }
        return byNature;
    }

    /**
     * El apartado de cada concepto, leido en una sola consulta.
     *
     * <p>Por JDBC y no por JPA: el dato vive en {@code payroll_engine.payroll_concept}, cuya
     * entidad es del metamodelo del motor, y lo unico que hace falta aqui es un par de cadenas.
     * Montar una proyeccion del agregado del concepto para eso seria arrastrar el metamodelo
     * entero a una pregunta de dos columnas.
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> findSubsectionCodeByConcept(String ruleSystemCode) {
        Map<String, String> byConcept = new LinkedHashMap<>();
        jdbc.query("""
                select o.object_code, c.payslip_subsection_code
                  from payroll_engine.payroll_concept c
                  join payroll_engine.payroll_object o on o.id = c.object_id
                 where o.rule_system_code = ?
                   and c.payslip_subsection_code is not null
                """,
                rs -> { byConcept.put(rs.getString(1), rs.getString(2)); },
                ruleSystemCode);
        return byConcept;
    }
}
