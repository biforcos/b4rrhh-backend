package com.b4rrhh.geo.territory.domain.model;

/**
 * Una provincia: código INE (CPRO), nombre, ISO 3166-2 y su comunidad. Ceuta y Melilla no
 * tienen ISO de provincia y llevan el de la ciudad autónoma (V174).
 */
public record Province(String code, String name, String isoCode, Region region) {
}
