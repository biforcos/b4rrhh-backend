package com.b4rrhh.geo.territory.domain.model;

/** Una comunidad o ciudad autónoma: código INE (CODAUTO), nombre e ISO 3166-2. */
public record Region(String code, String name, String isoCode) {
}
