package com.b4rrhh.payroll_engine.concept.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataConceptLabelRepository extends JpaRepository<ConceptLabelEntity, Long> {

    /**
     * Los nombres del sistema de reglas en un idioma, ya emparejados con su codigo de concepto.
     *
     * <p>Un {@code join} por identificador de objeto y no una relacion mapeada: lo que sale de aqui
     * son pares (codigo, literal), no entidades, y asi no se arrastra el {@code payroll_object}
     * entero por cada literal.
     */
    @Query("""
        select o.objectCode, l.label
        from PayrollEngineConceptLabelEntity l, PayrollEngineObjectEntity o
        where o.id = l.objectId
          and o.ruleSystemCode = :ruleSystemCode
          and o.objectTypeCode = 'CONCEPT'
          and l.languageCode = :languageCode
        """)
    List<Object[]> findCodeAndLabel(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("languageCode") String languageCode
    );

    @Query("""
        select l
        from PayrollEngineConceptLabelEntity l, PayrollEngineObjectEntity o
        where o.id = l.objectId
          and o.ruleSystemCode = :ruleSystemCode
          and o.objectTypeCode = 'CONCEPT'
          and o.objectCode = :conceptCode
          and l.languageCode = :languageCode
        """)
    Optional<ConceptLabelEntity> findOne(
            @Param("ruleSystemCode") String ruleSystemCode,
            @Param("conceptCode") String conceptCode,
            @Param("languageCode") String languageCode
    );
}
