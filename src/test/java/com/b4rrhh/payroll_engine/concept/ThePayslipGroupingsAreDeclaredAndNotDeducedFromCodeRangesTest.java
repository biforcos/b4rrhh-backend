package com.b4rrhh.payroll_engine.concept;

import com.b4rrhh.payroll_engine.concept.domain.model.FunctionalNature;
import com.b4rrhh.payroll_engine.concept.domain.model.PayrollConcept;
import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;
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
