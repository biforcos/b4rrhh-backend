package com.b4rrhh.payroll_engine.concept.domain.model;

/**
 * El idioma con el que el motor resuelve el nombre de un concepto ({@code backend#109}).
 *
 * <p>Una constante y no un parametro, a proposito. La tabla de literales lleva idioma desde el
 * primer dia —anadirlo despues obligaria a reescribir la clave de una tabla ya poblada— pero
 * <b>elegirlo</b> es otra conversacion y otro issue. El sitio queda hecho; abrirlo, no.
 *
 * <p>Un recibo esta en un idioma: el idioma se decide al calcular y lo que viaja con la linea es
 * el texto ya resuelto, no la decision.
 */
public final class ConceptLabelLanguage {

    /** Espanol. El unico que hay hoy, y el unico que se siembra. */
    public static final String DEFAULT = "es";

    private ConceptLabelLanguage() {
    }
}
