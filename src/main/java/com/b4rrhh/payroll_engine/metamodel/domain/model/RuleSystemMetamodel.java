package com.b4rrhh.payroll_engine.metamodel.domain.model;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptFeedRelation;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConceptOperand;
import com.b4rrhh.payroll_engine.eligibility.domain.model.ConceptAssignment;
import com.b4rrhh.payroll_engine.eligibility.domain.model.EmployeeAssignmentContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * La reglamentación de una ejecución: los conceptos de un sistema de reglas con sus
 * operandos, sus alimentaciones y sus asignaciones, tal y como estaban en el instante
 * en que la ejecución empezó.
 *
 * <h3>Es inmutable dentro de una ejecución, y eso es una regla</h3>
 *
 * <p>Todas las unidades de una misma ejecución se calculan contra este mismo objeto.
 * No es una optimización con un efecto secundario agradable: es lo que hace que dos
 * nóminas de la misma corrida con los mismos datos de entrada den lo mismo.
 *
 * <p>Antes de esto, cada unidad releía el grafo de conceptos de la base. Una corrida de
 * la plantilla dura minutos; si alguien cambiaba un operando a mitad, las primeras
 * unidades y las últimas se calculaban con reglas distintas y <strong>nada lo decía</strong>.
 * El resultado dependía del reloj. Cargando la reglamentación una vez, esa clase de
 * divergencia deja de ser posible: o entra entera en la ejecución o no entra.
 *
 * <p>El borde de «todo» es el que la ejecución ya declara: un {@code ruleSystemCode} y
 * una {@code referenceDate}. No es la base entera, y no crece cuando haya cinco convenios
 * con sus tablas por año.
 *
 * <p>La vida de lo cargado es la ejecución, no el proceso. Dos ejecuciones seguidas con
 * un cambio en el grafo en medio ven cada una el suyo, porque cada una carga el suyo.
 * Por eso esto no es —ni debe convertirse en— una caché de segundo nivel.
 *
 * <p>Lo que este objeto <strong>no</strong> guarda: los datos del empleado, su presencia,
 * sus entradas del periodo ni las tablas salariales. Eso es el caso, no la regla, y
 * cambia de una unidad a otra.
 */
public final class RuleSystemMetamodel {

    private final String ruleSystemCode;
    private final LocalDate referenceDate;
    private final Map<String, PayrollConcept> conceptsByCode;
    private final Map<String, List<PayrollConceptOperand>> operandsByConceptCode;
    private final Map<Long, List<PayrollConceptFeedRelation>> activeFeedsByTargetObjectId;
    private final List<ConceptAssignment> assignments;

    public RuleSystemMetamodel(
            String ruleSystemCode,
            LocalDate referenceDate,
            List<PayrollConcept> concepts,
            List<PayrollConceptOperand> operands,
            List<PayrollConceptFeedRelation> activeFeedRelations,
            List<ConceptAssignment> validAssignments
    ) {
        if (ruleSystemCode == null || ruleSystemCode.isBlank()) {
            throw new IllegalArgumentException("ruleSystemCode must not be blank");
        }
        if (referenceDate == null) {
            throw new IllegalArgumentException("referenceDate must not be null");
        }
        this.ruleSystemCode = ruleSystemCode;
        this.referenceDate = referenceDate;

        Map<String, PayrollConcept> byCode = new LinkedHashMap<>();
        for (PayrollConcept concept : concepts) {
            byCode.put(concept.getConceptCode(), concept);
        }
        this.conceptsByCode = Map.copyOf(byCode);

        Map<String, List<PayrollConceptOperand>> byConcept = new LinkedHashMap<>();
        for (PayrollConceptOperand operand : operands) {
            byConcept.computeIfAbsent(operand.getTargetObject().getObjectCode(), k -> new ArrayList<>())
                    .add(operand);
        }
        this.operandsByConceptCode = unmodifiableLists(byConcept);

        Map<Long, List<PayrollConceptFeedRelation>> byTarget = new LinkedHashMap<>();
        for (PayrollConceptFeedRelation relation : activeFeedRelations) {
            byTarget.computeIfAbsent(relation.getTargetObject().getId(), k -> new ArrayList<>())
                    .add(relation);
        }
        this.activeFeedsByTargetObjectId = unmodifiableLists(byTarget);

        this.assignments = List.copyOf(validAssignments);
    }

    public String ruleSystemCode() {
        return ruleSystemCode;
    }

    /**
     * La fecha contra la que se resolvieron vigencias al cargar. Quien construye un plan
     * con este metamodelo no elige fecha: la fecha ya está elegida, y es esta.
     */
    public LocalDate referenceDate() {
        return referenceDate;
    }

    public Optional<PayrollConcept> findConcept(String conceptCode) {
        return Optional.ofNullable(conceptsByCode.get(conceptCode));
    }

    /**
     * Devuelve las definiciones de los códigos pedidos, en el orden en que se piden.
     * Los códigos sin definición se omiten: detectarlos es cosa de quien llama.
     */
    public List<PayrollConcept> findConcepts(Collection<String> conceptCodes) {
        if (conceptCodes == null || conceptCodes.isEmpty()) {
            return List.of();
        }
        List<PayrollConcept> found = new ArrayList<>(conceptCodes.size());
        for (String code : conceptCodes) {
            PayrollConcept concept = conceptsByCode.get(code);
            if (concept != null) {
                found.add(concept);
            }
        }
        return List.copyOf(found);
    }

    /** Los operandos declarados por el concepto indicado, ordenados por rol. */
    public List<PayrollConceptOperand> operandsOf(String conceptCode) {
        return operandsByConceptCode.getOrDefault(conceptCode, List.of());
    }

    /**
     * Las alimentaciones vigentes en {@link #referenceDate()} cuyo destino es el objeto
     * indicado. No hace falta volver a filtrar por fecha: al cargar ya se filtró.
     */
    public List<PayrollConceptFeedRelation> activeFeedsOf(Long targetObjectId) {
        if (targetObjectId == null) {
            return List.of();
        }
        return activeFeedsByTargetObjectId.getOrDefault(targetObjectId, List.of());
    }

    /**
     * Las asignaciones candidatas para un contexto de empleado, con la semántica de
     * comodín de {@code concept_assignment}: una dimensión nula <em>en la asignación</em>
     * vale para cualquier valor; una dimensión nula <em>en el contexto</em> significa
     * desconocida y solo casa con el comodín.
     *
     * <p>La vigencia no se comprueba aquí porque ya se comprobó al cargar.
     */
    public List<ConceptAssignment> applicableAssignments(EmployeeAssignmentContext context) {
        requireSameRuleSystem(context.getRuleSystemCode());
        List<ConceptAssignment> candidates = new ArrayList<>();
        for (ConceptAssignment assignment : assignments) {
            if (matches(assignment.getCompanyCode(), context.getCompanyCode())
                    && matches(assignment.getAgreementCode(), context.getAgreementCode())
                    && matches(assignment.getEmployeeTypeCode(), context.getEmployeeTypeCode())) {
                candidates.add(assignment);
            }
        }
        return List.copyOf(candidates);
    }

    public int conceptCount() {
        return conceptsByCode.size();
    }

    public int operandCount() {
        return operandsByConceptCode.values().stream().mapToInt(List::size).sum();
    }

    public int activeFeedCount() {
        return activeFeedsByTargetObjectId.values().stream().mapToInt(List::size).sum();
    }

    public int assignmentCount() {
        return assignments.size();
    }

    /**
     * Falla si se intenta calcular con la reglamentación de otro sistema de reglas.
     * Silenciar esto daría una nómina vacía en vez de un error, que es peor.
     */
    public void requireSameRuleSystem(String otherRuleSystemCode) {
        if (!ruleSystemCode.equals(otherRuleSystemCode)) {
            throw new IllegalArgumentException(
                    "Metamodel was loaded for rule system " + ruleSystemCode
                            + " and cannot answer for " + otherRuleSystemCode);
        }
    }

    @Override
    public String toString() {
        return "RuleSystemMetamodel{" + ruleSystemCode + "@" + referenceDate
                + ", conceptos=" + conceptCount()
                + ", operandos=" + operandCount()
                + ", alimentaciones=" + activeFeedCount()
                + ", asignaciones=" + assignmentCount() + "}";
    }

    private static boolean matches(String assignmentDimension, String contextDimension) {
        return assignmentDimension == null || assignmentDimension.equals(contextDimension);
    }

    private static <K, V> Map<K, List<V>> unmodifiableLists(Map<K, List<V>> source) {
        Map<K, List<V>> copy = new LinkedHashMap<>(source.size() * 2);
        source.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        return Map.copyOf(copy);
    }
}
