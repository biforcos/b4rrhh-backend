package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSubsection;
import com.b4rrhh.payroll_engine.concept.domain.port.PayrollConceptRepository;
import com.b4rrhh.payroll_engine.concept.domain.port.PayslipSectionRepository;
import com.b4rrhh.support.TestSobreEsquemaReal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Las agrupaciones del modelo oficial estan declaradas ({@code backend#109}).
 *
 * <p>Estaban implicitas en que los devengos van por el 1xx, las deducciones por el 7xx y los
 * totales por el 9xx. Eso funciona hasta que alguien numera un devengo en el 750 porque le tocaba
 * ahi por orden de folio, y entonces se rompe en silencio y dos anos despues. El PDF del paso 2
 * necesita los bloques, y deducirlos de un rango es como se rompen.
 */
@TestSobreEsquemaReal
class ThePayslipGroupingsAreDeclaredAndNotDeducedFromCodeRangesTest {

    @Autowired
    private PayslipSectionRepository payslipSectionRepository;

    @Autowired
    private PayrollConceptRepository conceptRepository;

    @Test
    void theFiveBlocksOfTheOfficialModelAreDeclaredInPrintingOrder() {
        List<PayslipSection> sections = payslipSectionRepository.findByRuleSystemCode("ESP");

        assertEquals(
                List.of("DEVENGOS", "DEDUCCIONES", "LIQUIDO", "BASES", "APORTACION_EMPRESARIAL"),
                sections.stream().map(PayslipSection::sectionCode).toList());
        assertEquals("Liquido total a percibir",
                sections.stream()
                        .filter(s -> s.sectionCode().equals("LIQUIDO"))
                        .findFirst().orElseThrow().label());
    }

    /**
     * Y el recuadro de bases, en sus cuatro apartados ({@code backend#121}).
     *
     * <p>Es el unico bloque del modelo oficial que tiene partes dentro, y son las que el modelo
     * numera. Que este declarado aqui y no dibujado en el PDF es lo mismo que la {@code V138}
     * decidio un nivel mas arriba: un renderizador que supiera que hay cuatro bloques de bases
     * volveria a tener las agrupaciones escritas dentro.
     */
    @Test
    void theContributionBasesBoxIsDeclaredInFourNumberedParts() {
        List<PayslipSection> sections = payslipSectionRepository.findByRuleSystemCode("ESP");

        PayslipSection bases = sections.stream()
                .filter(s -> s.sectionCode().equals("BASES"))
                .findFirst().orElseThrow();
        assertEquals(List.of("BASE_CC", "BASE_CP", "BASE_HE", "BASE_IRPF"),
                bases.subsections().stream().map(PayslipSubsection::subsectionCode).toList(),
                "los cuatro apartados del recuadro, en el orden del modelo oficial");
        assertEquals("1. Contingencias comunes", bases.subsections().get(0).label());

        // Y los otros cuatro bloques no tienen ninguno, que es el caso normal: sus lineas se
        // imprimen seguidas. Un apartado que apareciera ahi seria un rotulo que nadie ha pedido.
        assertEquals(List.of(),
                sections.stream()
                        .filter(s -> !s.sectionCode().equals("BASES"))
                        .filter(s -> !s.subsections().isEmpty())
                        .map(PayslipSection::sectionCode)
                        .toList());
    }

    /**
     * A que apartado va cada linea lo dice el concepto, y <b>no su naturaleza</b>
     * ({@code backend#121}).
     *
     * <p>Es la diferencia con la seccion, y la razon de que la subseccion cuelgue del concepto:
     * las diez lineas del recuadro son todas {@code BASE} y viven en cuatro apartados distintos.
     * Una regla que dedujera el apartado de la naturaleza las metaria a las diez en el mismo.
     */
    @Test
    void whichPartALineGoesInIsDeclaredByTheConceptAndNotByItsNature() {
        Map<String, String> byConcept = payslipSectionRepository.findSubsectionCodeByConcept("ESP");

        assertEquals(Map.of(
                        "B03", "BASE_CC", "B04", "BASE_CC", "B01", "BASE_CC", "B_CC", "BASE_CC",
                        "B05", "BASE_CP", "B06", "BASE_CP", "B07", "BASE_CP", "B_CP", "BASE_CP",
                        "B08", "BASE_HE", "B09", "BASE_IRPF"),
                byConcept,
                "los diez del recuadro, y solo ellos: un concepto de mas aqui es una linea que va"
                        + " a salir bajo un rotulo que no le toca");
    }

    /** Vienen ordenadas, y el orden es el declarado y no el de insercion. */
    @Test
    void theSectionsComeInTheOrderTheyArePrintedIn() {
        List<PayslipSection> sections = payslipSectionRepository.findByRuleSystemCode("ESP");

        assertEquals(
                sections.stream().sorted(Comparator.comparingInt(PayslipSection::displayOrder)).toList(),
                sections,
                "el repositorio tiene que devolverlas ya ordenadas");
    }

    /**
     * La afirmacion que hace que esto no sea decorativo: <b>toda naturaleza que llega al folio
     * tiene bloque</b>, y lo tiene por estar declarada, no por el rango de su codigo.
     *
     * <p>Un concepto llega al folio si lleva {@code payslipOrderCode}. Si manana alguien anade uno
     * con una naturaleza nueva y no declara donde va, este test se pone rojo — que es justo cuando
     * hay que enterarse, y no cuando el PDF lo coloque en cualquier parte.
     */
    @Test
    void everyNatureThatReachesThePayslipHasADeclaredSection() {
        Map<String, String> sectionByNature = payslipSectionRepository.findSectionCodeByNature("ESP");

        List<String> sinSeccion = conceptRepository.findAllByRuleSystemCode("ESP").stream()
                .filter(concept -> concept.getPayslipOrderCode() != null)
                .map(concept -> concept.getFunctionalNature().name())
                .distinct()
                .filter(nature -> !sectionByNature.containsKey(nature))
                .toList();

        assertTrue(sinSeccion.isEmpty(),
                "naturalezas que salen en el folio sin bloque declarado: " + sinSeccion);
    }

    /**
     * El total de un bloque vive en su bloque.
     *
     * <p>{@code TOTAL_EARNING} es la ultima linea de devengos, no una seccion aparte. Si
     * estuvieran separados, el folio tendria un bloque «Total devengado» con una sola linea y la
     * suma dejaria de leerse como el cierre de lo que tiene encima.
     */
    @Test
    void aBlockTotalLivesInsideItsOwnBlock() {
        Map<String, String> sectionByNature = payslipSectionRepository.findSectionCodeByNature("ESP");

        assertEquals(sectionByNature.get(FunctionalNature.EARNING.name()),
                sectionByNature.get(FunctionalNature.TOTAL_EARNING.name()));
        assertEquals(sectionByNature.get(FunctionalNature.DEDUCTION.name()),
                sectionByNature.get(FunctionalNature.TOTAL_DEDUCTION.name()));
        assertEquals("LIQUIDO", sectionByNature.get(FunctionalNature.NET_PAY.name()));
        // El cuarto total, que hasta el backend#114 no existia: lo sumaba la plantilla del PDF.
        assertEquals(sectionByNature.get(FunctionalNature.INFORMATIONAL.name()),
                sectionByNature.get(FunctionalNature.TOTAL_EMPLOYER_CONTRIBUTION.name()));
    }

    /**
     * Y la comprobacion de que el bloque no sale del rango del codigo.
     *
     * <p>{@code 970} y {@code 800} empiezan los dos por un digito alto y van en bloques distintos;
     * {@code 101} y {@code 970} empiezan por digitos muy distintos y van en el mismo. Si alguien
     * sustituyera la declaracion por un {@code if (code < 700)}, esto se caeria.
     */
    @Test
    void theBlockDoesNotFollowTheNumericRangeOfTheConceptCode() {
        Map<String, String> sectionByNature = payslipSectionRepository.findSectionCodeByNature("ESP");

        assertEquals(seccionDe("101", sectionByNature), seccionDe("970", sectionByNature),
                "101 y 970 son los dos devengos aunque sus codigos no se parezcan");
        assertTrue(!seccionDe("800", sectionByNature).equals(seccionDe("970", sectionByNature)),
                "800 y 970 estan en la misma decena alta y en bloques distintos");
        // Y el 720, que esta entre las deducciones por numeracion, no es una deduccion.
        assertEquals("APORTACION_EMPRESARIAL", seccionDe("720", sectionByNature));
        assertEquals("DEDUCCIONES", seccionDe("703", sectionByNature));
    }

    private String seccionDe(String conceptCode, Map<String, String> sectionByNature) {
        PayrollConcept concept = conceptRepository.findByBusinessKey("ESP", conceptCode).orElseThrow();
        String section = sectionByNature.get(concept.getFunctionalNature().name());
        assertNotNull(section, "el concepto " + conceptCode + " tendria que tener bloque");
        return section;
    }
}
