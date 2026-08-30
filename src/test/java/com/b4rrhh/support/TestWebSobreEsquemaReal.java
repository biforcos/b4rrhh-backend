package com.b4rrhh.support;

import com.b4rrhh.B4rrhhBackendApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Un test de extremo a extremo: peticion HTTP real contra el esquema real (backend#31).
 *
 * Es el hermano de {@link TestSobreEsquemaReal}, y existe por el hueco que aquel no cubre:
 * los {@code *ControllerHttpTest} prueban la capa web con la persistencia mockeada, y los
 * {@code @TestSobreEsquemaReal} prueban el esquema real sin capa web. Nada cruzaba las dos,
 * y por ahi se colaron tres bugs en un dia: un JPQL invalido en Postgres que los mocks
 * daban por bueno (#30), una excepcion que solo existia en el test (#28) y un parametro
 * que nadie pasaba (#24). Esto es una franja fina —el camino feliz de lectura, uno por
 * vertical—, no una copia de la suite con mocks, que sigue valiendo para rutas, validacion
 * y codigos de error.
 *
 * El contexto es la aplicacion entera ({@code @SpringBootTest} + MockMvc) sobre un clon
 * del esquema de produccion (ver {@link EsquemaRealInitializer}): migraciones y semillas
 * reales, Postgres de verdad. La seguridad esta activa: cada test se autentica con
 * {@code @WithMockUser(roles = ...)} (o el post-processor {@code jwt()} de
 * spring-security-test si necesita claims).
 *
 * Los tests van en transaccion y se deshacen al terminar: MockMvc despacha en el mismo
 * hilo, asi que el controller se une a la transaccion del test y el clon compartido queda
 * limpio entre tests. Un test que necesite commit de verdad no es de esta anotacion.
 *
 * La misma disciplina de contexto unico que {@code TestSobreEsquemaReal}: Spring cachea
 * los contextos por configuracion, y cualquier anotacion extra en la clase de test —un
 * {@code @Import}, un {@code @TestPropertySource}— es otro contexto, o sea, otro clon de
 * la base y otros segundos de arranque. Todos los tests de esta anotacion declaran
 * EXACTAMENTE esto y comparten un unico contexto.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
        classes = B4rrhhBackendApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.flyway.enabled=false"
        }
)
@AutoConfigureMockMvc
@Transactional
@ContextConfiguration(initializers = EsquemaRealInitializer.class)
public @interface TestWebSobreEsquemaReal {
}
