package com.b4rrhh.payroll.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lo que se sirve se declara, y se declara en un solo sitio.
 *
 * <p>Es la regla que dejo el {@code backend#80}, y la dejo porque durante cinco meses hubo un
 * endpoint servido por el backend y ausente del contrato del que el cliente se genera: existia y
 * era invisible. Aquello se arreglo fusionando los dos contratos, pero fusionarlos no impide que
 * manana alguien anada un {@code @PostMapping} y se olvide del {@code .yaml} — que es exactamente
 * como empezo.
 *
 * <p>Las rutas se sacan de las anotaciones, no de una lista escrita a mano: una lista a mano tiene
 * el mismo defecto que quiere evitar, porque tambien hay que acordarse de ampliarla. Se comparan
 * contra las claves de {@code paths:} del contrato, no contra el texto suelto, para que una
 * mencion en una descripcion no valga por una declaracion.
 *
 * <p>Lo que este test <b>no</b> mira: si lo declarado describe bien lo servido —verbos, cuerpos,
 * codigos de estado—. Eso no lo puede saber una ruta. Aqui solo se afirma que la ruta esta, que es
 * la parte que se olvida.
 */
class EveryPayrollEndpointIsDeclaredInTheContractTest {

    private static final List<Class<?>> CONTROLADORES = List.of(
            PayrollController.class,
            PayrollCalculationRunController.class
    );

    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    @Test
    void everyServedPathIsAPathOfTheContract() {
        Set<String> servidas = rutasServidas();
        Set<String> declaradas = rutasDeclaradas();

        Set<String> sinDeclarar = new TreeSet<>(servidas);
        sinDeclarar.removeAll(declaradas);

        assertThat(sinDeclarar)
                .withFailMessage("""
                        Estas rutas las sirve el backend y no estan declaradas en el contrato: %s

                        El cliente del frontend y los tipos del designer se generan de ese fichero,
                        asi que lo que no este ahi existe y es invisible — que es exactamente lo que
                        paso durante cinco meses con /payroll/calculation-runs/{runId}/messages
                        (backend#80).

                        Servidas:   %s
                        Declaradas: %s
                        """.formatted(sinDeclarar, servidas, declaradas))
                .isEmpty();
    }

    private Set<String> rutasServidas() {
        Set<String> rutas = new TreeSet<>();
        for (Class<?> controlador : CONTROLADORES) {
            RequestMapping base = controlador.getAnnotation(RequestMapping.class);
            String prefijo = base == null || base.value().length == 0 ? "" : base.value()[0];
            for (Method metodo : controlador.getDeclaredMethods()) {
                for (String sufijo : sufijosDe(metodo)) {
                    String ruta = prefijo + sufijo;
                    rutas.add(ruta.endsWith("/") && ruta.length() > 1
                            ? ruta.substring(0, ruta.length() - 1)
                            : ruta);
                }
            }
        }
        assertTrue(!rutas.isEmpty(),
                "No he encontrado ninguna ruta en " + CONTROLADORES
                        + ". Si han cambiado de forma, este test hay que actualizarlo, no borrarlo.");
        return rutas;
    }

    /** Lo que cada anotacion de mapeo aporta detras del prefijo de la clase. */
    private List<String> sufijosDe(Method metodo) {
        GetMapping get = metodo.getAnnotation(GetMapping.class);
        if (get != null) {
            return get.value().length == 0 ? List.of("") : List.of(get.value());
        }
        PostMapping post = metodo.getAnnotation(PostMapping.class);
        if (post != null) {
            return post.value().length == 0 ? List.of("") : List.of(post.value());
        }
        PutMapping put = metodo.getAnnotation(PutMapping.class);
        if (put != null) {
            return put.value().length == 0 ? List.of("") : List.of(put.value());
        }
        DeleteMapping delete = metodo.getAnnotation(DeleteMapping.class);
        if (delete != null) {
            return delete.value().length == 0 ? List.of("") : List.of(delete.value());
        }
        return List.of();
    }

    /**
     * Las claves de {@code paths:}: una linea con dos espacios de sangrado, una barra y dos puntos
     * al final. Lo que hay dentro de una descripcion va mas sangrado y no cuela.
     */
    private Set<String> rutasDeclaradas() {
        Set<String> rutas = new TreeSet<>();
        for (String linea : leerContrato().lines().toList()) {
            if (linea.startsWith("  /") && linea.endsWith(":") && !linea.startsWith("   ")) {
                rutas.add(linea.trim().substring(0, linea.trim().length() - 1));
            }
        }
        assertTrue(!rutas.isEmpty(),
                "No he encontrado ninguna clave de paths: en " + CONTRATO.toAbsolutePath()
                        + ". Si el fichero ha cambiado de forma, este test hay que actualizarlo.");
        return rutas;
    }

    private String leerContrato() {
        assertTrue(Files.isRegularFile(CONTRATO), "No encuentro " + CONTRATO.toAbsolutePath());
        try {
            return Files.readString(CONTRATO);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
