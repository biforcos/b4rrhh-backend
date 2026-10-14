package com.b4rrhh.geo.territory.domain.model;

/**
 * Si un país tiene territorio cargado: una reglamentación que lo tiene como suyo, y con ella una capa
 * nacional de la que leer provincias y tipos de vía (backend#169). Cuando no, por qué.
 */
public record CountryTerritory(String countryCode, boolean hasTerritory, String message) {
}
