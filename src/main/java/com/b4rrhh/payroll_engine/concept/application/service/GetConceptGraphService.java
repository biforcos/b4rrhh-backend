package com.b4rrhh.payroll_engine.concept.application.service;

import com.b4rrhh.payroll_engine.concept.application.usecase.ConceptGraph;
import com.b4rrhh.payroll_engine.concept.application.usecase.GetConceptGraphUseCase;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptFeedRelationRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptOperandRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El grafo de un sistema de reglas, en tres consultas ({@code designer#15}).
 *
 * <h2>Por que existe</h2>
 *
 * <p>Dibujar el grafo costaba <b>1 + 2N</b> peticiones: la lista de conceptos, y luego los
 * operandos y las alimentaciones de cada uno, de uno en uno. Medido en el navegador: <b>78</b>
 * llamadas para los 38 conceptos de {@code ESP} y <b>200</b> para un catalogo de 99, y 198 de
 * esas 200 devolvian una lista de entre cero y dos elementos.
 *
 * <p>Y crece con los conceptos <b>del sistema de reglas</b>, no del recibo, asi que lo paga igual
 * el recibo mas simple. Que es lo que lo hace caro: el grafo es lo que el backoffice embebe para
 * explicar un numero, o sea la pantalla que se abre justo despues de que alguien pregunte «¿de
 * donde sale esto?».
 *
 * <p>El motor ya habia pagado esta factura por su lado —{@code RuleSystemMetamodel} se carga una
 * vez por ejecucion por la misma razon ({@code backend#87})—, y las lecturas masivas que aquello
 * dejo son las que esto usa. Es el mismo N+1 al otro lado del contrato.
 *
 * <h2>Una transaccion, y por que importa</h2>
 *
 * <p>Las tres lecturas van en la misma transaccion de solo lectura, asi que son consistentes
 * entre si: ningun operando nombra un concepto que falte de la lista. Con 1 + 2N peticiones esa
 * garantia no existia —entre la primera y la ultima cabe un borrado— y el cliente se comia una
 * arista colgando de un nodo que no estaba.
 *
 * <h2>Lo que esto no hace</h2>
 *
 * <p>No sustituye a los extremos por concepto ni los deja obsoletos: el panel de detalle y los
 * dos {@code PUT} trabajan contra ellos y siguen siendo lo correcto para una pregunta sobre un
 * concepto. Esto contesta otra pregunta.
 *
 * <p>Y no filtra las alimentaciones por fecha, igual que el extremo por concepto: el disenador
 * dibuja lo <b>declarado</b>, incluido lo que aun no esta vigente. Filtrar aqui por «hoy» es lo
 * que necesita el motor para calcular, y ya lo hace por su cuenta.
 */
@Service
public class GetConceptGraphService implements GetConceptGraphUseCase {

    private final PayrollConceptRepository conceptRepository;
    private final PayrollConceptOperandRepository operandRepository;
    private final PayrollConceptFeedRelationRepository feedRelationRepository;

    public GetConceptGraphService(
            PayrollConceptRepository conceptRepository,
            PayrollConceptOperandRepository operandRepository,
            PayrollConceptFeedRelationRepository feedRelationRepository
    ) {
        this.conceptRepository = conceptRepository;
        this.operandRepository = operandRepository;
        this.feedRelationRepository = feedRelationRepository;
    }

    /**
     * Un sistema de reglas sin conceptos contesta un grafo vacio y no un 404: preguntar por el
     * grafo de un sistema que todavia no tiene nada es lo que hace el disenador la primera vez
     * que alguien lo abre, y «no hay nada dibujado» es una respuesta, no un error.
     */
    @Override
    @Transactional(readOnly = true)
    public ConceptGraph graphOf(String ruleSystemCode) {
        return new ConceptGraph(
                ruleSystemCode,
                conceptRepository.findAllByRuleSystemCode(ruleSystemCode),
                operandRepository.findAllByRuleSystemCode(ruleSystemCode),
                feedRelationRepository.findAllByRuleSystemCode(ruleSystemCode)
        );
    }
}
