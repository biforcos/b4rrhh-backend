package com.b4rrhh.rulesystem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataRuleEntityRepository extends JpaRepository<RuleEntityEntity, Long>, JpaSpecificationExecutor<RuleEntityEntity> {

    LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    /**
     * El catalogo entero de un tipo, sin filtrar por fecha (backend#32).
     *
     * La vigencia no esconde: la fecha de referencia dice respecto a que dia se
     * calcula la marca de vigencia de cada opcion, y eso lo hace el caso de uso.
     * Elegir un codigo no vigente es frecuente en este dominio —la correccion
     * administrativa: «esto se grabo mal, ponle el codigo antiguo»—, y si la
     * excepcion es frecuente no es una excepcion: esconder esos codigos obliga a
     * un modo especial que el usuario tiene que saber que existe.
     *
     * {@code re.active = true} se queda: dado de baja y no vigente son cosas
     * distintas, y lo dado de baja no se ofrece nunca.
     */
    @Query("""
        select re
        from RuleEntityEntity re
        where re.ruleSystemCode = :ruleSystemCode
          and re.ruleEntityTypeCode = :ruleEntityTypeCode
          and re.active = true
          and (
              :qLike is null
              or lower(re.code) like :qLike
              or lower(re.name) like :qLike
          )
        order by lower(coalesce(re.name, '')), re.code
        """)
    List<RuleEntityEntity> findDirectCatalogOptions(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
            @Param("qLike") String qLike
    );

        @Query("""
      select re
      from RuleEntityEntity re
      where re.ruleSystemCode = :ruleSystemCode
        and re.ruleEntityTypeCode = :ruleEntityTypeCode
        and re.code = :code
        and re.startDate <= :referenceDate
        and :referenceDate <= coalesce(re.endDate, :maxDate)
      order by re.startDate desc, re.id desc
      """)
        List<RuleEntityEntity> findApplicableByBusinessKey(
          @Param("ruleSystemCode") String ruleSystemCode,
          @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
          @Param("code") String code,
          @Param("referenceDate") LocalDate referenceDate,
          @Param("maxDate") LocalDate maxDate
        );

    Optional<RuleEntityEntity> findByRuleSystemCodeAndRuleEntityTypeCodeAndCode(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code
    );

    Optional<RuleEntityEntity> findByRuleSystemCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    );

    @Query("""
        select (count(re) > 0)
        from RuleEntityEntity re
        where re.ruleSystemCode = :ruleSystemCode
          and re.ruleEntityTypeCode = :ruleEntityTypeCode
          and re.code = :code
          and re.startDate <> :excludedStartDate
          and re.startDate <= :effectiveProjectedEndDate
          and :projectedStartDate <= coalesce(re.endDate, :maxDate)
        """)
    boolean existsOverlapExcludingStartDate(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
            @Param("code") String code,
            @Param("projectedStartDate") LocalDate projectedStartDate,
            @Param("effectiveProjectedEndDate") LocalDate effectiveProjectedEndDate,
            @Param("excludedStartDate") LocalDate excludedStartDate,
            @Param("maxDate") LocalDate maxDate
    );

    long deleteByRuleSystemCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
            String ruleSystemCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    );
}
