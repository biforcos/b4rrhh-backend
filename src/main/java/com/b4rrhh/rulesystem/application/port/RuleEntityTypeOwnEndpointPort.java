package com.b4rrhh.rulesystem.application.port;

import java.util.Optional;

/**
 * La colección del API por la que se da de alta una raíz de un tipo, tal como la declara
 * {@code rule_entity_type.api_collection_path} (backend#88).
 *
 * <p>Es un puerto de aplicación y no del dominio a propósito: la ruta de un endpoint no es
 * un hecho del dominio, es un hecho del API. El dominio decide <b>que</b> este tipo no se da
 * de alta por la puerta genérica; esto sólo sirve para poder decir por cuál sí.</p>
 */
public interface RuleEntityTypeOwnEndpointPort {

    /** Vacío si el tipo no declara ninguna, que es el caso de todo maestro simple. */
    Optional<String> findApiCollectionPath(String ruleEntityTypeCode);
}
