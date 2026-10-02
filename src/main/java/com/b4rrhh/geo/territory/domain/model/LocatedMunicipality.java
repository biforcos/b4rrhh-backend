package com.b4rrhh.geo.territory.domain.model;

/**
 * Un municipio con su provincia, y por ella su comunidad. La provincia es nula si la
 * reglamentación no tiene una con ese código en su capa nacional.
 */
public record LocatedMunicipality(Municipality municipality, Province province) {
}
