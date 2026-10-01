package com.b4rrhh.rulesystem.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * La tabla, por capa. Sólo la usa {@link RuleEntityPersistenceAdapter}, que es quien sabe qué capa
 * monta cada reglamentación en el nivel de cada tipo (ADR-077, backend#157); el candado
 * {@code NobodyReadsRuleEntityOutsideItsPortTest} lo vigila.
 */
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
        where re.layerCode = :layerCode
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
            @Param("layerCode") String layerCode,
            @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
            @Param("qLike") String qLike
    );

    /** {@link #findDirectCatalogOptions}, y además vigentes en la fecha: los centros de una empresa. */
    @Query("""
        select re
        from RuleEntityEntity re
        where re.layerCode = :layerCode
          and re.ruleEntityTypeCode = :ruleEntityTypeCode
          and re.active = true
          and re.startDate <= :referenceDate
          and :referenceDate <= coalesce(re.endDate, :maxDate)
          and (
              :qLike is null
              or lower(re.code) like :qLike
              or lower(re.name) like :qLike
          )
        order by lower(coalesce(re.name, '')), re.code
        """)
    List<RuleEntityEntity> findDirectCatalogOptionsOnDate(
            @Param("layerCode") String layerCode,
            @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
            @Param("qLike") String qLike,
            @Param("referenceDate") LocalDate referenceDate,
            @Param("maxDate") LocalDate maxDate
    );

        @Query("""
      select re
      from RuleEntityEntity re
      where re.layerCode = :layerCode
        and re.ruleEntityTypeCode = :ruleEntityTypeCode
        and re.code = :code
        and re.startDate <= :referenceDate
        and :referenceDate <= coalesce(re.endDate, :maxDate)
      order by re.startDate desc, re.id desc
      """)
        List<RuleEntityEntity> findApplicableByBusinessKey(
          @Param("layerCode") String layerCode,
          @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
          @Param("code") String code,
          @Param("referenceDate") LocalDate referenceDate,
          @Param("maxDate") LocalDate maxDate
        );

    Optional<RuleEntityEntity> findByLayerCodeAndRuleEntityTypeCodeAndCode(
            String layerCode,
            String ruleEntityTypeCode,
            String code
    );

    Optional<RuleEntityEntity> findByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
            String layerCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    );

    @Query("""
        select (count(re) > 0)
        from RuleEntityEntity re
        where re.layerCode = :layerCode
          and re.ruleEntityTypeCode = :ruleEntityTypeCode
          and re.code = :code
          and re.startDate <> :excludedStartDate
          and re.startDate <= :effectiveProjectedEndDate
          and :projectedStartDate <= coalesce(re.endDate, :maxDate)
        """)
    boolean existsOverlapExcludingStartDate(
            @Param("layerCode") String layerCode,
            @Param("ruleEntityTypeCode") String ruleEntityTypeCode,
            @Param("code") String code,
            @Param("projectedStartDate") LocalDate projectedStartDate,
            @Param("effectiveProjectedEndDate") LocalDate effectiveProjectedEndDate,
            @Param("excludedStartDate") LocalDate excludedStartDate,
            @Param("maxDate") LocalDate maxDate
    );

    long deleteByLayerCodeAndRuleEntityTypeCodeAndCodeAndStartDate(
            String layerCode,
            String ruleEntityTypeCode,
            String code,
            LocalDate startDate
    );
}
