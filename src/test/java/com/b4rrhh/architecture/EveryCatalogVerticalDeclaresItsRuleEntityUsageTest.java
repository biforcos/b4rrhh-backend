package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Un vertical que guarda códigos de catálogo por texto tiene que declarar dónde, o borrar
 * uno de esos códigos pasará sin que nadie se entere (backend#28).
 *
 * La comprobación de uso la suma Spring de la lista de RuleEntityUsageParticipant, así que
 * no hay registro central; pero tampoco nada obliga a escribir el participante. Este test
 * convierte el olvido en un fallo de la suite, con la misma convención que
 * TerminationCoversEveryPresenceVerticalTest: se mira el código fuente, no el bytecode.
 *
 * «Guarda códigos de catálogo» se detecta por la clase {@code *RuleEntityTypeCodes} del
 * vertical. Si un vertical resuelve catálogos con literales sueltos en vez de esa clase, este
 * test no lo ve: es la limitación conocida, y absence hoy es ese caso.
 */
class EveryCatalogVerticalDeclaresItsRuleEntityUsageTest {

    private static final Path VERTICALES = Path.of("src/main/java/com/b4rrhh/employee");

    @Test
    void everyVerticalWithCatalogCodesHasAUsageParticipant() {
        assertTrue(Files.isDirectory(VERTICALES), "No encuentro " + VERTICALES.toAbsolutePath());

        Set<String> olvidados = new TreeSet<>();
        try (Stream<Path> verticales = Files.list(VERTICALES)) {
            verticales.filter(Files::isDirectory)
                    .filter(vertical -> tiene(vertical, "RuleEntityTypeCodes.java"))
                    .filter(vertical -> !tiene(vertical, "RuleEntityUsageParticipant.java"))
                    .map(vertical -> vertical.getFileName().toString())
                    .forEach(olvidados::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertTrue(olvidados.isEmpty(), """
                Estos verticales guardan códigos de catálogo (tienen *RuleEntityTypeCodes) pero no
                declaran dónde, así que borrar uno de sus códigos pasaría en silencio: %s

                Cada uno necesita un *RuleEntityUsageParticipant en su infrastructure/persistence,
                normalmente extendiendo EmployeeOwnedRuleEntityUsageParticipant con su tabla y la
                columna de cada tipo. Con implementarlo basta: Spring lo recoge solo.
                """.formatted(olvidados));
    }

    private boolean tiene(Path vertical, String sufijo) {
        try (Stream<Path> ficheros = Files.walk(vertical)) {
            return ficheros.anyMatch(f -> f.getFileName().toString().endsWith(sufijo));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
