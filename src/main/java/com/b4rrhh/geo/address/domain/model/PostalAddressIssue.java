package com.b4rrhh.geo.address.domain.model;

/**
 * Por qué una dirección está «sin normalizar» (backend#155): la ficha dice por qué, no sólo que no.
 *
 * <p>Se guarda el primero que falla, en el orden en que los mira la validación: el municipio, la
 * provincia, el CP y el tipo de vía. Una dirección normalizada no tiene ninguno. Los nombres son los
 * códigos que se guardan en {@code normalization_issue} y viajan por el API: renombrar uno es una
 * migración.</p>
 */
public enum PostalAddressIssue {

    /** Una empresa o un centro sin dirección ninguna: sólo puede pasar en un perfil, que la tiene opcional. */
    SIN_DIRECCION,
    /**
     * Escrita en la base sin pasar por la validación: una inserción a mano. Es lo que pone la base
     * por omisión (V178); la aplicación nunca lo escribe, porque siempre valida.
     */
    SIN_VALIDAR,

    /** En España, sin municipio o con uno que no está en el maestro {@code geo}. */
    MUNICIPIO_DESCONOCIDO,
    /** El municipio terminó antes de la fecha de la dirección (el INE lo fusionó o lo suprimió). */
    MUNICIPIO_NO_VIGENTE,
    /** Sin provincia, o con una que no está en el catálogo de la capa ESP. */
    PROVINCIA_DESCONOCIDA,
    /** La provincia no es la del municipio. */
    PROVINCIA_DE_OTRO_MUNICIPIO,
    /** Sin CP, con uno que no tiene cinco cifras, o con uno que no está en el maestro {@code geo}. */
    CP_DESCONOCIDO,
    /** El CP existe, pero sus dos primeras cifras son otra provincia. */
    CP_DE_OTRA_PROVINCIA,
    /** Sin tipo de vía, o con uno que no está en el catálogo de la capa ESP. */
    TIPO_DE_VIA_DESCONOCIDO,
    /** Fuera de España, con municipio, provincia o tipo de vía, que son españoles. */
    DATOS_DE_ESPANA_FUERA_DE_ESPANA
}
