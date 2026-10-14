package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rectificar la fecha de alta o de cese de una presencia mueve todo lo que vive dentro de ella
 * (backend#172).
 *
 * <p>Es el mismo olvido que vigila {@code TerminationCoversEveryPresenceVerticalTest}, al revés: un
 * vertical nuevo que vive dentro de la presencia y no tiene participante de rectificación se quedaría
 * en la fecha vieja, y la rectificación saldría bien sin moverlo. Aquí no fallaría nada: quedaría un
 * hueco o una fila fuera de la presencia, y se vería meses después en un recibo.</p>
 *
 * <p>Se detectan igual que en el cese: se puede cerrar ({@code Close*Service}) y no puede salir de la
 * presencia (una excepción {@code *OutsidePresence*}). El participante es un
 * {@code *PresenceRectificationParticipant} dentro del propio vertical.</p>
 */
class PresenceRectificationCoversEveryPresenceVerticalTest {

    private static final Path VERTICALES = Path.of("src/main/java/com/b4rrhh/employee");

    /** No son verticales de datos: presence es la raíz y lifecycle la orquesta. */
    private static final Set<String> NO_SON_VERTICALES = Set.of("presence", "lifecycle", "temporal", "journey", "shared");

    @Test
    void everyVerticalConstrainedByPresenceMovesWithItsRectification() throws IOException {
        Set<String> acotados = new TreeSet<>();
        Set<String> sinParticipante = new TreeSet<>();
        try (Stream<Path> hijos = Files.list(VERTICALES)) {
            for (Path vertical : hijos.filter(Files::isDirectory)
                    .filter(p -> !NO_SON_VERTICALES.contains(p.getFileName().toString())).toList()) {
                boolean sePuedeCerrar = hay(vertical, f -> nombre(f).startsWith("Close") && nombre(f).endsWith("Service.java"));
                boolean dependeDeLaPresencia = hay(vertical, f -> nombre(f).contains("OutsidePresence"));
                if (sePuedeCerrar && dependeDeLaPresencia) {
                    acotados.add(vertical.getFileName().toString());
                    if (!hay(vertical, f -> nombre(f).endsWith("PresenceRectificationParticipant.java"))) {
                        sinParticipante.add(vertical.getFileName().toString());
                    }
                }
            }
        }

        assertTrue(acotados.size() >= 5, "Sólo encuentro " + acotados + ": ¿ha cambiado la estructura?");
        assertTrue(sinParticipante.isEmpty(), """
                Estos verticales viven dentro de la presencia, pero no se mueven cuando se rectifica su
                fecha de alta o de cese: %s

                Cada uno necesita un *PresenceRectificationParticipant en su application/service, que
                diga qué movería (PresenceRectificationPlan.of hace la regla común) y lo mueva. Con
                implementarlo basta: Spring lo recoge solo.
                """.formatted(sinParticipante));
    }

    private static String nombre(Path fichero) {
        return fichero.getFileName().toString();
    }

    private static boolean hay(Path vertical, Predicate<Path> criterio) {
        try (Stream<Path> ficheros = Files.walk(vertical)) {
            return ficheros.anyMatch(criterio);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
