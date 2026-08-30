package com.b4rrhh.rulesystem.domain.model;

/**
 * Cuántas filas de un recurso de negocio referencian un código de catálogo. «412 presences»
 * permite decidir; «está en uso» obliga a investigar a mano (backend#28).
 *
 * @param resource nombre del recurso en plural, como sale en la API: {@code presences}
 * @param count    todas las filas, históricas incluidas: un código usado en 2019 sigue en uso
 */
public record RuleEntityReference(String resource, long count) {
}
