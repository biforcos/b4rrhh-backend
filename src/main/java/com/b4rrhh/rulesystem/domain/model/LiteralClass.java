package com.b4rrhh.rulesystem.domain.model;

/**
 * La clase del literal de un tipo de entidad (ADR-054 §3): qué es el texto que nombra
 * cada código del catálogo. De ella derivan la traducibilidad (ADR-052 §1) y la guardia
 * de universalidad (backend#25).
 */
public enum LiteralClass {

    /**
     * Vocabulario nuestro, o uno neutro compartido. Inglés como forma base, traducible.
     * Universal: el mismo literal en todas las reglamentaciones.
     */
    DOMAIN_VOCABULARY,

    /**
     * El nombre oficial de una figura de la norma, en su idioma. Traducirlo sería
     * falsificarlo.
     */
    REGULATORY_CITATION,

    /** El nombre de una cosa concreta. No tiene idioma. */
    PROPER_NOUN
}
