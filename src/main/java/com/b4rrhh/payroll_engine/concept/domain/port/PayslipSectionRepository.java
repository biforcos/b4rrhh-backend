package com.b4rrhh.payroll_engine.concept.domain.port;

import com.b4rrhh.payroll_engine.concept.domain.model.PayslipSection;

import java.util.List;
import java.util.Map;

/**
 * Las agrupaciones del recibo de un sistema de reglas ({@code backend#109}).
 */
public interface PayslipSectionRepository {

    /** Los bloques declarados, en el orden en el que se imprimen. */
    List<PayslipSection> findByRuleSystemCode(String ruleSystemCode);

    /**
     * En que seccion va cada naturaleza, indexado por el nombre de la naturaleza.
     *
     * <p>Una naturaleza <b>sin seccion declarada no esta en el mapa</b>. No se coloca por defecto
     * en ningun bloque: una linea sin seccion se ve, y colocarla donde no pinta nada, no.
     */
    Map<String, String> findSectionCodeByNature(String ruleSystemCode);

    /**
     * En que apartado de su bloque va cada concepto, indexado por codigo de concepto
     * ({@code backend#121}).
     *
     * <p>Un concepto <b>sin apartado no esta en el mapa</b>, y ese es el caso de casi todos: su
     * linea se imprime en el bloque, sin nada por encima. A diferencia de la seccion, esto no se
     * deduce de la naturaleza —los diez conceptos del recuadro de bases son todos {@code BASE} y
     * van en cuatro apartados— asi que la clave es el concepto.
     */
    Map<String, String> findSubsectionCodeByConcept(String ruleSystemCode);
}
