package com.b4rrhh.rulesystem.domain.port;

import com.b4rrhh.rulesystem.domain.model.RuleEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * El único puerto que resuelve entidades de catálogo (ADR-077 §4, backend#157).
 *
 * <p>Se pide por reglamentación y se resuelve por nivel: el tipo declara su nivel, la reglamentación
 * monta una capa en ese nivel, y la entidad se busca en esa capa. No sube por ninguna cadena. Lo que
 * devuelve dice desde qué reglamentación se pidió ({@code ruleSystemCode}) y de dónde viene
 * ({@code layerCode}, {@code level}). Sin reglamentación —{@link #findByFilters} con
 * {@code ruleSystemCode} nulo—, cada entidad sale una vez por cada reglamentación que monta su capa.
 *
 * <p>Nadie más lee {@code rulesystem.rule_entity}: {@code NobodyReadsRuleEntityOutsideItsPortTest}.
 */
public interface RuleEntityRepository {
    List<RuleEntity> findAll();
    List<RuleEntity> findByFilters(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            Boolean active,
            LocalDate referenceDate
    );
    Optional<RuleEntity> findApplicableByBusinessKey(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate referenceDate
    );
    Optional<RuleEntity> findByBusinessKey(String ruleSystemCode, String ruleEntityTypeCode, String code);
    Optional<RuleEntity> findByBusinessKeyAndStartDate(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate startDate);
    boolean existsOverlapExcludingStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate projectedStartDate,
            LocalDate projectedEndDate,
            LocalDate excludedStartDate
    );
    /**
     * Las opciones de un desplegable: activas, cuyo código o nombre case con {@code qLike} (nulo, todas),
     * vigentes en {@code referenceDate} si llega, y ordenadas por nombre <b>en la base</b>. El orden
     * no se rehace en Java: la intercalación de la base no ordena los {@code _} como Java, y quien
     * elige por posición —el loader— cambiaría de elección (backend#32, backend#157).
     */
    List<RuleEntity> findActiveOptions(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String qLike,
            LocalDate referenceDate
    );
    void deleteByBusinessKeyAndStartDate(String ruleSystemCode, String ruleEntityTypeCode, String code, LocalDate startDate);
    RuleEntity save(RuleEntity ruleEntity);
}
