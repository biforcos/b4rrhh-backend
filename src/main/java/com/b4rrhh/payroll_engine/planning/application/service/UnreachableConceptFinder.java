package com.b4rrhh.payroll_engine.planning.application.service;

import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.metamodel.domain.model.RuleSystemMetamodel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Los conceptos de una reglamentación a los que no llega ninguna asignación ({@code backend#110}).
 *
 * <h2>El caso, tal y como salió</h2>
 *
 * <p>Un catálogo hondo, encadenado con {@code AGGREGATE}. Se siembra, se lanza el cálculo, la
 * corrida <b>sale bien</b>, los recibos salen <b>idénticos al día anterior</b> y no hay error, ni
 * aviso, ni una línea de diferencia. Lo que había pasado es que los conceptos nuevos existían
 * sólo como fuente de un agregado y no estaban asignados a nadie, y las fuentes de un agregado no
 * se expanden — a propósito, ver {@link DefaultEligibleConceptExpansionService}.
 *
 * <p>Lo que está mal ahí no es que no se expandan: es <b>el silencio</b>. Un recibo idéntico es
 * indistinguible de «este cambio no tenía que mover nada», que es un caso legítimo y frecuente, y
 * lo primero que se mira cuando no cuadra es la fórmula, no la asignación.
 *
 * <h2>Qué cuenta como alcanzable, y por qué se pregunta expandiendo</h2>
 *
 * <p>Se parte de <b>todas</b> las asignaciones del sistema de reglas —sin mirar empresa, convenio
 * ni tipo de empleado— y se expande con la misma expansión que usa el motor. Lo que queda fuera no
 * lo alcanza nadie: un empleado concreto parte siempre de un subconjunto de ese punto de partida,
 * así que lo que no se alcanza desde el todo no se alcanza desde una parte. Sin falsos positivos.
 *
 * <p>Se expande y no se consulta «¿lo nombra alguien?» porque son dos preguntas distintas y ésta
 * es la que no tenía respuesta. Un concepto que alimenta a un agregado <b>sí</b> lo nombra alguien
 * —es fuente de una alimentación—, así que la consulta de huérfanos del {@code backend#96} lo
 * daría por sano. Ese es el mismo animal por el otro extremo: allí, un concepto que no alimenta a
 * nadie; aquí, uno al que no llega nadie.
 *
 * <p>Y se reutiliza {@link EligibleConceptExpansionService} en vez de repetir el recorrido para
 * que las dos respuestas no puedan divergir: el día que la expansión cambie de reglas, esta
 * comprobación cambia con ella. Un recorrido propio sería una segunda definición de «alcanzable»,
 * y la que mandaría sería la otra.
 */
@Service
public class UnreachableConceptFinder {

    private final EligibleConceptExpansionService expansion;

    public UnreachableConceptFinder(EligibleConceptExpansionService expansion) {
        this.expansion = expansion;
    }

    /**
     * Los códigos declarados que ninguna asignación alcanza, ordenados y sin repetir.
     *
     * <p>Lista vacía es lo normal y es lo que tiene que salir de un catálogo sano: un aviso que
     * sale siempre no avisa de nada.
     *
     * <p><b>No atrapa nada.</b> Si el catálogo tiene una referencia rota, la expansión revienta
     * igual que revienta en el motor, y quien llame decide qué hacer con eso; esta clase no está
     * para tapar esa otra avería.
     */
    public List<String> unreachableConceptsIn(RuleSystemMetamodel metamodel) {
        Set<String> alcanzables = expansion.expand(metamodel.assignedConcepts(), metamodel).stream()
                .map(PayrollConcept::getConceptCode)
                .collect(Collectors.toSet());

        return metamodel.concepts().stream()
                .map(PayrollConcept::getConceptCode)
                .filter(codigo -> !alcanzables.contains(codigo))
                .collect(Collectors.toCollection(TreeSet::new))
                .stream()
                .toList();
    }
}
