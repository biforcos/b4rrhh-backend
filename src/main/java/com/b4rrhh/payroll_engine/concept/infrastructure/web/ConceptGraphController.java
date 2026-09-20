package com.b4rrhh.payroll_engine.concept.infrastructure.web;

import com.b4rrhh.payroll_engine.concept.application.usecase.ConceptGraph;
import com.b4rrhh.payroll_engine.concept.application.usecase.GetConceptGraphUseCase;
import com.b4rrhh.payroll_engine.concept.application.usecase.GetConceptLabelsUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * El grafo de un sistema de reglas, en una llamada ({@code designer#15}).
 *
 * <p>Va en su propio controlador y no colgando de {@code /concepts} porque no es una operacion
 * sobre los conceptos: es otra vista de lo mismo, con sus aristas. Cuelga del sistema de reglas,
 * que es de lo que el grafo es.
 */
@RestController
@RequestMapping("/payroll-engine/{ruleSystemCode}/graph")
public class ConceptGraphController {

    private final GetConceptGraphUseCase getConceptGraphUseCase;
    private final GetConceptLabelsUseCase getConceptLabelsUseCase;
    private final PayrollConceptManagementAssembler assembler;

    public ConceptGraphController(
            GetConceptGraphUseCase getConceptGraphUseCase,
            GetConceptLabelsUseCase getConceptLabelsUseCase,
            PayrollConceptManagementAssembler assembler
    ) {
        this.getConceptGraphUseCase = getConceptGraphUseCase;
        this.getConceptLabelsUseCase = getConceptLabelsUseCase;
        this.assembler = assembler;
    }

    @GetMapping
    public PayrollConceptGraphResponse graph(@PathVariable String ruleSystemCode) {
        ConceptGraph graph = getConceptGraphUseCase.graphOf(ruleSystemCode);

        // Los nombres, una vez para todo el grafo. Es el mismo gesto que la lista de conceptos y
        // por la misma razon: el catalogo es pequeno, pero «una lectura por fila» no lo es
        // (backend#109). Seria absurdo quitar 2N peticiones de red y dejar N consultas dentro.
        Map<String, String> labels = getConceptLabelsUseCase.byRuleSystemCode(ruleSystemCode);

        return new PayrollConceptGraphResponse(
                graph.ruleSystemCode(),
                graph.concepts().stream()
                        .map(concept -> assembler.toResponse(
                                concept, labels.get(concept.getConceptCode())))
                        .toList(),
                graph.operands().stream()
                        .map(operand -> new ConceptGraphOperandResponse(
                                operand.getTargetObject().getObjectCode(),
                                operand.getOperandRole().name(),
                                operand.getSourceObject().getObjectCode()))
                        .toList(),
                graph.feeds().stream()
                        .map(feed -> new ConceptGraphFeedResponse(
                                feed.getTargetObject().getObjectCode(),
                                feed.getSourceObject().getObjectCode(),
                                feed.isInvertSign(),
                                feed.getEffectiveFrom(),
                                feed.getEffectiveTo()))
                        .toList()
        );
    }
}
