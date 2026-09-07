package com.b4rrhh.shared.infrastructure.system;

/**
 * A que base esta escribiendo este backend, en tres campos.
 *
 * No es "estoy vivo" ni "que version soy": es la identidad del sitio donde
 * acaban las escrituras. La diferencia importa. Un cliente que pregunta si el
 * backend responde, o si el token vale, obtiene un 200 igual de bueno del
 * backend equivocado (workforce-loader#8).
 *
 * - database: host:puerto/nombre. Lleva host y puerto a proposito, y no solo
 *   el nombre: la base de la demo y la de desarrollo se llaman las dos
 *   'b4rrhh' (deploy/docker-compose.yml y application.yml), asi que el nombre
 *   suelto no distingue el caso que mas dano hace.
 * - schemaVersion: la ultima migracion de Flyway aplicada con exito. Distingue
 *   un backend de otro arbol aunque apunte a la base correcta.
 * - employees: cuantos empleados hay ya. Es el estado, no la identidad: un
 *   cliente lo compara al empezar, nunca a mitad de corrida, porque crece.
 */
public record SystemTarget(
        String database,
        String schemaVersion,
        long employees
) {
}
