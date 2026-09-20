package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.payroll_engine.concept.domain.model.ConceptLabelLanguage;
import com.b4rrhh.payroll_engine.concept.domain.port.ConceptLabelRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Todos los conceptos sembrados de ESP tienen nombre, y el nombre no es el mnemonico
 * ({@code backend#109}).
 *
 * <p>El defecto que esto vigila es el de partida: el recibo ensenaba {@code SALARIO_BASE} porque
 * lo que se guardaba como literal era el {@code concept_mnemonic} copiado tal cual. Un mnemonico
 * es un identificador —es lo que las reglas referencian para encontrar un concepto— y estaba
 * puesto en el sitio donde va un nombre.
 *
 * <p>Por eso la afirmacion no es «hay 38 filas en la tabla de literales»: es que <b>ningun
 * concepto se llama igual que su mnemonico</b>. Una siembra que copiara los mnemonicos a la tabla
 * nueva pasaria el recuento y dejaria el defecto intacto.
 */
@TestSobreEsquemaReal
class EveryEngineConceptHasASpanishNameTest {

    @Autowired
    private PayrollConceptRepository conceptRepository;

    @Autowired
    private ConceptLabelRepository conceptLabelRepository;

    @Test
    void everySeededEspConceptIsNamed() {
        List<PayrollConcept> concepts = conceptRepository.findAllByRuleSystemCode("ESP");
        Map<String, String> labels = conceptLabelRepository
                .findLabelsByRuleSystemCode("ESP", ConceptLabelLanguage.DEFAULT);

        List<String> sinNombre = concepts.stream()
                .map(PayrollConcept::getConceptCode)
                .filter(code -> !labels.containsKey(code))
                .toList();

        assertTrue(sinNombre.isEmpty(),
                "Conceptos de ESP sin nombre en espanol: " + sinNombre);
        assertEquals(concepts.size(), labels.size(),
                "Hay literales que no cuelgan de ningun concepto de ESP");
    }

    @Test
    void noConceptIsNamedAfterItsOwnMnemonic() {
        Map<String, String> labels = conceptLabelRepository
                .findLabelsByRuleSystemCode("ESP", ConceptLabelLanguage.DEFAULT);

        List<String> seLlamanComoSuClave = conceptRepository.findAllByRuleSystemCode("ESP").stream()
                .filter(concept -> concept.getConceptMnemonic()
                        .equalsIgnoreCase(labels.get(concept.getConceptCode())))
                .map(PayrollConcept::getConceptCode)
                .toList();

        assertTrue(seLlamanComoSuClave.isEmpty(),
                "Estos conceptos tienen por nombre su propio mnemonico, que es el defecto que "
                        + "este paso arregla: " + seLlamanComoSuClave);
    }

    /** El que el recibo ensena, y el motivo por el que esto se abrio. */
    @Test
    void salaryIsCalledSalarioBaseAndKeepsItsMnemonic() {
        Map<String, String> labels = conceptLabelRepository
                .findLabelsByRuleSystemCode("ESP", ConceptLabelLanguage.DEFAULT);

        assertEquals("Salario base", labels.get("101"));
        assertEquals("SALARIO_BASE",
                conceptRepository.findByBusinessKey("ESP", "101").orElseThrow().getConceptMnemonic());
    }

    /**
     * El caso que va a existir el dia que alguien anada un concepto y se olvide del nombre.
     *
     * <p>El concepto sin literal <b>no esta en el mapa</b>. No hay entrada vacia ni cadena en
     * blanco: quien lo lea ve que falta y ensena el mnemonico. Una ausencia visible es mejor que
     * una invisible.
     */
    @Test
    void aConceptWithNoNameIsAVisibleAbsenceAndNotAnEmptyString() {
        Map<String, String> labels = conceptLabelRepository
                .findLabelsByRuleSystemCode("ESP", ConceptLabelLanguage.DEFAULT);

        assertFalse(labels.containsKey("NO_EXISTE_ESTE_CONCEPTO"));
        assertTrue(labels.values().stream().noneMatch(String::isBlank),
                "Un literal en blanco es un hueco disfrazado de nombre");
    }
}
