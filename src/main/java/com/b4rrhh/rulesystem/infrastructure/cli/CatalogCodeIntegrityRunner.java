package com.b4rrhh.rulesystem.infrastructure.cli;

import com.b4rrhh.rulesystem.application.usecase.CatalogCodeIntegrityReport;
import com.b4rrhh.rulesystem.application.usecase.CheckCatalogCodeIntegrityUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * La comprobación de códigos de catálogo, ejecutable contra la base a la que apunte la
 * configuración (backend#44).
 *
 * <p>Es la salida 1 de las tres que planteaba el issue, y la que se eligió: un comando de la
 * propia aplicación. Las otras dos se descartaron por lo que el issue ya decía —un endpoint
 * obliga a decidir quién lo llama y qué pasa si tarda, y un test apuntado a una base externa mete
 * credenciales de producción en la suite y confunde «test» con «comprobación de operación»—. Ésta
 * no añade superficie: la imagen y la conexión ya están en el despliegue.
 *
 * <p>Se lanza con el perfil {@code comprobar-catalogos}:
 *
 * <pre>
 * java -jar b4rrhh-backend.jar \
 *      --spring.profiles.active=comprobar-catalogos \
 *      --spring.flyway.enabled=false \
 *      --server.port=0 \
 *      --spring.datasource.url=jdbc:postgresql://localhost:5432/b4rrhh_semilla
 * </pre>
 *
 * <p><b>Flyway apagado a propósito en esa orden</b>: comprobar una base no es migrarla, y una
 * comprobación que de paso le aplique migraciones a la base de la demo deja de ser una
 * comprobación.
 *
 * <p><b>Y {@code --server.port=0}, no {@code web-application-type=none}</b>: sin capa web no hay
 * {@code HttpSecurity} y {@code SecurityConfig} no arranca. Hacer condicional la configuración de
 * seguridad para que una comprobación pueda correr sin ella es justo el arreglo que un día deja la
 * aplicación sin seguridad por una propiedad; un puerto efímero no cuesta nada y no toca el
 * cableado de producción. Cero, además, y no un número fijo: en el portátil de desarrollo el 8080
 * suele tenerlo otro backend.
 *
 * <p>Sale con {@code 0} si no hay huérfanos y con {@code 1} si los hay, para que un workflow
 * pueda encadenarlo detrás de la restauración. Sale <b>siempre</b>, gane o pierda: la aplicación
 * levanta su capa web para arrancar, y un comando que se quedara sirviendo peticiones dejaría
 * colgado al workflow que lo llamó. Y publica siempre los recuentos, no sólo el veredicto: el
 * informe lleva columna, tipo, filas miradas y filas huérfanas, y debajo los códigos concretos de
 * las que estén rotas.
 */
@Component
@Profile("comprobar-catalogos")
public class CatalogCodeIntegrityRunner implements ApplicationRunner {

    /** Lo que devuelve el proceso cuando hay filas que apuntan a códigos inexistentes. */
    static final int HAY_HUERFANOS = 1;

    static final int SIN_HUERFANOS = 0;

    private static final Logger log = LoggerFactory.getLogger(CatalogCodeIntegrityRunner.class);

    private final CheckCatalogCodeIntegrityUseCase checkCatalogCodeIntegrityUseCase;
    private final ConfigurableApplicationContext applicationContext;

    public CatalogCodeIntegrityRunner(
            CheckCatalogCodeIntegrityUseCase checkCatalogCodeIntegrityUseCase,
            ConfigurableApplicationContext applicationContext
    ) {
        this.checkCatalogCodeIntegrityUseCase = checkCatalogCodeIntegrityUseCase;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        CatalogCodeIntegrityReport report = checkCatalogCodeIntegrityUseCase.check();

        log.info("[CATALOGOS] {}", report.summary());
        log.info("[CATALOGOS] Columna a columna:\n{}", report.describeAll());

        if (report.isClean()) {
            // Un verde sin denominador no es una noticia. Si no se ha mirado ni una fila, el
            // resultado no dice que los datos esten bien: dice que no habia datos.
            if (report.totalRows() == 0) {
                log.warn("[CATALOGOS] Ninguna fila que comprobar: esto NO es un aprobado, es una"
                        + " base sin datos. Apunta la comprobacion a una base poblada.");
            }
            log.info("[CATALOGOS] Sin huerfanos.");
        } else {
            log.error("[CATALOGOS] {} fila(s) apuntan a codigos que no existen en"
                    + " rulesystem.rule_entity:\n{}", report.totalOrphanRows(), report.describeOrphans());
            log.error("[CATALOGOS] Un codigo solo existe dentro de su reglamentacion. Si el codigo"
                    + " es correcto, lo que falta es darlo de alta en rule_entity; si es un residuo,"
                    + " hay que corregir la fila.");
        }

        int codigo = report.isClean() ? SIN_HUERFANOS : HAY_HUERFANOS;
        System.exit(SpringApplication.exit(applicationContext, () -> codigo));
    }
}
