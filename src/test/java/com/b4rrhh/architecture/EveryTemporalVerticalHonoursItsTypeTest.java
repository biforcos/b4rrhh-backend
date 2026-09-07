package com.b4rrhh.architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El tipo de vertical del ADR-057 se cumple, no solo se documenta (backend#51).
 *
 * El ADR convierte el tipo de vertical en una regla de construccion: clasificas
 * una serie temporal nueva y eso te dice como se comporta y que expone. Una
 * regla de construccion que solo vive en un .md dura hasta el primer despiste
 * —asi llegamos a tener Replace...FromDate en cuatro verticales de siete—, y
 * este test es lo que la convierte en un fallo de la suite. Es para las series
 * temporales lo que EveryCatalogColumnIsDeclaredOrExemptedTest es para los
 * codigos de catalogo.
 *
 * Comprueba cinco cosas:
 *
 * <ol>
 *   <li>Toda vertical con fecha de fin en su modelo declara su cobertura, o
 *       esta en EXENTAS con el motivo escrito.</li>
 *   <li>En una vertical de tipo A, el solape y el hueco los juzga su
 *       TimelineService y nadie mas.</li>
 *   <li>No queda ningun Replace...FromDate en el arbol.</li>
 *   <li>No queda el planificador viejo: StrongTimelineReplacePlanner,
 *       StrongTimelineReplacePlan, ReplaceMode.</li>
 *   <li>Toda vertical de tipo A expone el mismo juego de verbos —crear,
 *       corregir, borrar, leer— y ni replace-from-date ni close.</li>
 * </ol>
 *
 * El tipo se declara, no se adivina: una vertical es de tipo A porque nombra
 * TimelineCoverage, no porque se llame de cierta forma. Lo unico que se mira
 * por nombre es donde vive cada cosa —la carpeta de la vertical, el sufijo
 * TimelineService, el sufijo Controller—, que es convencion del proyecto y no
 * clasificacion.
 *
 * Se lee el codigo fuente y no el bytecode a proposito, como en
 * {@link TerminationCoversEveryPresenceVerticalTest}: sin ArchUnit no hace
 * falta ninguna dependencia y la regla se comprueba de un vistazo. Los
 * comentarios se quitan antes de buscar, para que contar en un comentario por
 * que se retiro el patron viejo no cuente como tenerlo. Este fichero se excluye
 * de su propio barrido por lo mismo: una guarda tiene que nombrar lo que
 * prohibe.
 */
class EveryTemporalVerticalHonoursItsTypeTest {

    private static final Path VERTICALES = Path.of("src/main/java/com/b4rrhh/employee");
    private static final Path FUENTES = Path.of("src");
    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    private static final String ESTA_GUARDA = "EveryTemporalVerticalHonoursItsTypeTest.java";

    /** Carpetas bajo employee/ que no son verticales de datos. */
    private static final Set<String> NO_SON_VERTICALES = Set.of("lifecycle", "temporal", "shared");

    /**
     * Verticales con vigencia que NO son series de tipo A, con el motivo. Una
     * linea por vertical; una exencion sin motivo no vale, y una exencion de
     * algo que ya declara cobertura tampoco: seria mentira y ademas taparia el
     * dia en que dejara de serlo.
     */
    private static final Map<String, String> EXENTAS = Map.of(
            "presence",
            "es la raiz de la que derivan las demas y la escriben los flujos de ciclo de vida "
                    + "(HIRE, TERMINATE, REHIRE), no el usuario: su vigencia no es una serie que se planifique",
            "absence",
            "el ADR-057 no la clasifico en ninguno de sus cinco tipos y no encaja de momento: sus "
                    + "ocurrencias llevan hora (startTime, endTime) y el componente temporal razona por dias, "
                    + "asi que ponerla de tipo A pide antes decidir la granularidad",
            "journey",
            "es derivada: la componen los servicios de lectura a partir de las demas verticales y no "
                    + "tiene modelo de dominio propio que declarar",
            "tax_information",
            "es de tipo B: su vigencia acaba donde empieza la siguiente, no tiene fecha de fin, y por "
                    + "eso sustituir es insertar y no hay cobertura que declarar"
    );

    // ------------------------------------------------------------------
    // 1. El tipo y la cobertura se declaran
    // ------------------------------------------------------------------

    @Test
    void everyVerticalWithAnEndDateDeclaresItsCoverageOrIsExempted() {
        assertThat(VERTICALES).as("la carpeta de las verticales existe").isDirectory();

        Set<String> conVigencia = verticalesConFechaDeFin(VERTICALES);
        assertThat(conVigencia).as("alguna vertical tiene fecha de fin en su modelo").isNotEmpty();

        Set<String> tipoA = verticalesDeTipoA(VERTICALES);
        Set<String> sinDeclarar = new TreeSet<>(conVigencia);
        sinDeclarar.removeAll(tipoA);
        sinDeclarar.removeAll(EXENTAS.keySet());

        assertThat(sinDeclarar)
                .withFailMessage("""
                        Estas verticales tienen fecha de fin en su modelo y no dicen que clase de serie
                        son: %s

                        Una serie temporal declara tres cosas (ADR-057): de que es serie, si su
                        cobertura es obligatoria y si sus ocurrencias pueden sobrevivir a la presencia.
                        Lo primero se hace con un TimelineService que nombre TimelineCoverage. Si la
                        vertical no es una serie de tipo A, va a EXENTAS con el motivo escrito.

                        Con fecha de fin:   %s
                        Declaran cobertura: %s
                        Exentas:            %s
                        """, sinDeclarar, conVigencia, tipoA, new TreeSet<>(EXENTAS.keySet()))
                .isEmpty();
    }

    /**
     * Una exencion de una vertical que ya no existe, o que ya declara su
     * cobertura, es ruido que acaba tapando un hueco real.
     */
    @Test
    void everyExemptionPointsToARealVerticalThatIsNotOfTypeA() {
        Set<String> tipoA = verticalesDeTipoA(VERTICALES);

        for (Map.Entry<String, String> exenta : new TreeMap<>(EXENTAS).entrySet()) {
            assertThat(VERTICALES.resolve(exenta.getKey()))
                    .as("la vertical exenta %s existe", exenta.getKey())
                    .isDirectory();
            assertThat(exenta.getValue().length())
                    .as("la exencion de %s lleva un motivo escrito", exenta.getKey())
                    .isGreaterThan(20);
            assertThat(tipoA)
                    .withFailMessage(
                            "%s declara TimelineCoverage, o sea que ya es una serie de tipo A: quita su "
                                    + "linea de EXENTAS en vez de dejar escrito que no lo es.",
                            exenta.getKey())
                    .doesNotContain(exenta.getKey());
        }
    }

    // ------------------------------------------------------------------
    // 2. El solape y el hueco los juzga el componente temporal
    // ------------------------------------------------------------------

    @Test
    void onlyTheTimelineServiceOfATypeAVerticalJudgesOverlapsAndGaps() {
        List<String> porSuCuenta = quienJuzgaSolapesOHuecos(VERTICALES);

        assertThat(porSuCuenta)
                .withFailMessage("""
                        Estos ficheros de una vertical de tipo A deciden por su cuenta que hay un solape
                        o un hueco: %s

                        En el ADR-057 eso no es una precondicion de la operacion, es un invariante del
                        estado resultante: lo juzga el planificador del componente temporal y lo
                        traduce a excepcion el TimelineService de la vertical, en su requireAccepted.
                        Una regla escondida en una operacion solo protege a quien pasa por ella, que es
                        exactamente como llegamos aqui.
                        """, porSuCuenta)
                .isEmpty();
    }

    // ------------------------------------------------------------------
    // 3 y 4. Lo que se retiro no vuelve
    // ------------------------------------------------------------------

    @Test
    void noReplaceFromDateSurvivesAnywhere() {
        List<String> rastros = rastrosEnElCodigo(FUENTES, PATRON_REPLACE_FROM_DATE);
        rastros.addAll(rastrosEnElContrato(PATRON_REPLACE_FROM_DATE));

        assertThat(rastros)
                .withFailMessage("""
                        Vuelve a haber un Replace...FromDate: %s

                        Es el que reaparece solo, porque es el patron que la gente conoce. El ADR-057 lo
                        retiro como modelo (decision 5): meter una ocurrencia con inicio y fin ya cierra
                        la que estaba en vigor el dia anterior, y derivar la fecha de fin por dentro es
                        justo lo que no debe pasar solo.
                        """, rastros)
                .isEmpty();
    }

    @Test
    void theOldReplacePlannerIsGone() {
        List<String> rastros = rastrosEnElCodigo(FUENTES, PATRON_PLANIFICADOR_VIEJO);

        assertThat(rastros)
                .withFailMessage("""
                        Vuelve a haber planificador viejo: %s

                        StrongTimelineReplacePlanner, StrongTimelineReplacePlan y ReplaceMode los retiro
                        el backend#57. Dos formas de planificar una linea temporal es como empieza otra
                        vez todo esto: hay una, TimelinePlanner, y su plan lleva la intencion con la que
                        se pidio el cambio.

                        Nombrarlos en un comentario para contar por que se fueron no cuenta: aqui se
                        mira el codigo con los comentarios quitados.
                        """, rastros)
                .isEmpty();
    }

    // ------------------------------------------------------------------
    // 5. El mismo juego de verbos
    // ------------------------------------------------------------------

    @Test
    void everyTypeAVerticalExposesTheSameVerbs() {
        Map<String, Set<String>> verbos = verbosPorVertical(VERTICALES);
        Set<String> tipoA = verticalesDeTipoA(VERTICALES);
        assertThat(tipoA).as("hay verticales de tipo A que mirar").isNotEmpty();

        Map<String, String> faltan = new TreeMap<>();
        Map<String, String> sobran = new TreeMap<>();

        for (String vertical : tipoA) {
            Set<String> suyos = verbos.getOrDefault(vertical, Set.of());

            Set<String> sinExponer = new TreeSet<>(VERBOS_DEL_TIPO_A);
            sinExponer.removeAll(suyos);
            if (!sinExponer.isEmpty()) {
                faltan.put(vertical, sinExponer.toString());
            }

            Set<String> retirados = new TreeSet<>(suyos);
            retirados.retainAll(VERBOS_RETIRADOS);
            if (!retirados.isEmpty()) {
                sobran.put(vertical, retirados.toString());
            }
        }

        assertThat(faltan)
                .withFailMessage("""
                        A estas verticales de tipo A les falta algun verbo del juego comun: %s

                        Son cuatro y son los mismos en todas: crear (POST), corregir (PUT), borrar
                        (DELETE) y leer (GET). Una serie de tipo A que no ofrece corregir obliga a
                        inventar un cambio historico que nunca ocurrio, y una que no ofrece borrar deja
                        el "me equivoque" sin salida.

                        Verbos por vertical: %s
                        """, faltan, verbos)
                .isEmpty();

        assertThat(sobran)
                .withFailMessage("""
                        Estas verticales de tipo A exponen un verbo que el ADR-057 retiro: %s

                        Las dos operaciones desaparecen del API (decision 2): lo que hacian pasa a ser
                        efecto del invariante. Cerrar una ocurrencia es corregir su fecha de fin, y
                        sustituir desde una fecha es dar de alta la siguiente.
                        """, sobran)
                .isEmpty();
    }

    // ==================================================================
    // La prueba de la guarda: cada regla, en rojo, sobre un arbol de mentira
    // ==================================================================

    @Test
    void aRevivedReplaceFromDateShowsUpByItself(@TempDir Path arbol) throws IOException {
        escribir(arbol.resolve("main/java/com/b4rrhh/employee/contract/ReplaceContractFromDateService.java"), """
                package com.b4rrhh.employee.contract;
                class ReplaceContractFromDateService {
                    void replaceFromDate() {
                    }
                }
                """);
        escribir(arbol.resolve("main/java/com/b4rrhh/employee/contract/CreateContractService.java"), """
                package com.b4rrhh.employee.contract;
                class CreateContractService {
                    void create() {
                    }
                }
                """);

        assertThat(rastrosEnElCodigo(arbol, PATRON_REPLACE_FROM_DATE))
                .containsExactly("main/java/com/b4rrhh/employee/contract/ReplaceContractFromDateService.java");
    }

    @Test
    void aPatternOnlyNamedInACommentDoesNotCount(@TempDir Path arbol) throws IOException {
        escribir(arbol.resolve("main/java/com/b4rrhh/employee/contract/ContractTimelineService.java"), """
                package com.b4rrhh.employee.contract;
                /** Lo que el viejo replace-from-date hacia en silencio ahora se dice. */
                class ContractTimelineService {
                    // ReplaceContractFromDateService y su ReplaceMode se fueron en el backend#51
                    void planAdd() {
                    }
                }
                """);

        assertThat(rastrosEnElCodigo(arbol, PATRON_REPLACE_FROM_DATE)).isEmpty();
        assertThat(rastrosEnElCodigo(arbol, PATRON_PLANIFICADOR_VIEJO)).isEmpty();
    }

    @Test
    void aRevivedOldPlannerShowsUpByItself(@TempDir Path arbol) throws IOException {
        escribir(arbol.resolve("main/java/com/b4rrhh/employee/temporal/support/TimelinePlanner.java"), """
                package com.b4rrhh.employee.temporal.support;
                class TimelinePlanner {
                    TimelinePlan planAdd() {
                        return null;
                    }
                }
                """);
        escribir(arbol.resolve("main/java/com/b4rrhh/employee/contract/ContractTimelineService.java"), """
                package com.b4rrhh.employee.contract;
                class ContractTimelineService {
                    void planAdd() {
                        ReplaceMode modo = ReplaceMode.EXACT_START;
                    }
                }
                """);

        assertThat(rastrosEnElCodigo(arbol, PATRON_PLANIFICADOR_VIEJO))
                .containsExactly("main/java/com/b4rrhh/employee/contract/ContractTimelineService.java");
    }

    @Test
    void aTypeAVerticalThatJudgesOverlapsOnItsOwnShowsUpByItself(@TempDir Path arbol) throws IOException {
        Path vertical = arbol.resolve("working_time");
        escribir(vertical.resolve("application/service/WorkingTimeTimelineService.java"), """
                package x;
                class WorkingTimeTimelineService {
                    static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;
                    void requireAccepted() {
                        throw new WorkingTimeOverlapException("solapa");
                    }
                }
                """);
        escribir(vertical.resolve("domain/model/WorkingTime.java"), """
                package x;
                class WorkingTime {
                    java.time.LocalDate endDate;
                }
                """);
        escribir(vertical.resolve("domain/service/WorkingTimeDomainService.java"), """
                package x;
                class WorkingTimeDomainService {
                    void validateCreate() {
                        if (repository.existsOverlappingPeriod()) {
                            throw new WorkingTimeOverlapException("solapa");
                        }
                    }
                }
                """);

        assertThat(quienJuzgaSolapesOHuecos(arbol))
                .containsExactly("working_time/domain/service/WorkingTimeDomainService.java");
    }

    @Test
    void aTypeAVerticalThatStillExposesCloseShowsUpByItself(@TempDir Path arbol) throws IOException {
        Path vertical = arbol.resolve("working_time");
        escribir(vertical.resolve("application/service/WorkingTimeTimelineService.java"), """
                package x;
                class WorkingTimeTimelineService {
                    static final TimelineCoverage COVERAGE = TimelineCoverage.MANDATORY;
                }
                """);
        escribir(vertical.resolve("infrastructure/web/WorkingTimeController.java"), """
                package x;
                @RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/working-times")
                class WorkingTimeController {
                    @PostMapping
                    void create() {
                    }
                    @GetMapping
                    void list() {
                    }
                    @PutMapping("/{workingTimeNumber}")
                    void update() {
                    }
                    @DeleteMapping("/{workingTimeNumber}")
                    void delete() {
                    }
                    @PostMapping("/{workingTimeNumber}/close")
                    void close() {
                    }
                }
                """);

        assertThat(verbosPorVertical(arbol).get("working_time"))
                .contains("close")
                .contains("crear", "corregir", "borrar", "leer");
    }

    @Test
    void aTypeAVerticalMissingAVerbShowsUpByItself(@TempDir Path arbol) throws IOException {
        Path vertical = arbol.resolve("cost_center");
        escribir(vertical.resolve("application/service/CostCenterTimelineService.java"), """
                package x;
                class CostCenterTimelineService {
                    static final TimelineCoverage COVERAGE = TimelineCoverage.OPTIONAL;
                }
                """);
        escribir(vertical.resolve("infrastructure/web/CostCenterBusinessKeyController.java"), """
                package x;
                @RequestMapping("/employees/{ruleSystemCode}/{employeeTypeCode}/{employeeNumber}/cost-centers")
                class CostCenterBusinessKeyController {
                    @PostMapping("/distributions")
                    void create() {
                    }
                    @GetMapping
                    void list() {
                    }
                    @DeleteMapping("/distributions/{startDate}")
                    void delete() {
                    }
                }
                """);

        assertThat(verbosPorVertical(arbol).get("cost_center"))
                .containsExactly("borrar", "crear", "leer");
    }

    @Test
    void aVerticalWithAnEndDateAndNoCoverageShowsUpByItself(@TempDir Path arbol) throws IOException {
        escribir(arbol.resolve("seniority/domain/model/Seniority.java"), """
                package x;
                class Seniority {
                    java.time.LocalDate startDate;
                    java.time.LocalDate endDate;
                }
                """);

        assertThat(verticalesConFechaDeFin(arbol)).containsExactly("seniority");
        assertThat(verticalesDeTipoA(arbol)).isEmpty();
    }

    // ==================================================================
    // Lo que mira cada regla
    // ==================================================================

    private static final Pattern PATRON_REPLACE_FROM_DATE = Pattern.compile(
            "Replace\\w*FromDate|replaceFromDate|replace-from-date");

    private static final Pattern PATRON_PLANIFICADOR_VIEJO = Pattern.compile(
            "StrongTimelineReplacePlan\\w*|\\bReplaceMode\\b");

    /** Excepciones con las que se dice "esto solapa" o "esto deja un hueco". */
    private static final Pattern PATRON_VEREDICTO = Pattern.compile(
            "new\\s+\\w*(?:Overlap|Coverage|Gap)\\w*Exception\\s*\\(");

    private static final Set<String> VERBOS_DEL_TIPO_A = Set.of("crear", "corregir", "borrar", "leer");

    private static final Set<String> VERBOS_RETIRADOS = Set.of("close", "sustituir-desde-fecha");

    /** Una vertical tiene vigencia si algo de su modelo de dominio lleva fecha de fin. */
    private static Set<String> verticalesConFechaDeFin(Path raiz) {
        Set<String> conVigencia = new TreeSet<>();
        for (Path vertical : verticales(raiz)) {
            Path modelo = vertical.resolve("domain/model");
            if (Files.isDirectory(modelo) && algunFichero(modelo, f -> leer(f).contains("endDate"))) {
                conVigencia.add(vertical.getFileName().toString());
            }
        }
        return conVigencia;
    }

    /** Una vertical es de tipo A si declara su cobertura: nombra TimelineCoverage. */
    private static Set<String> verticalesDeTipoA(Path raiz) {
        Set<String> tipoA = new TreeSet<>();
        for (Path vertical : verticales(raiz)) {
            if (algunFichero(vertical, f -> sinComentarios(leer(f)).contains("TimelineCoverage"))) {
                tipoA.add(vertical.getFileName().toString());
            }
        }
        return tipoA;
    }

    /**
     * Ficheros de una vertical de tipo A, que no sean su TimelineService, en
     * los que se construye el veredicto de solape o de hueco. Consultar al
     * repositorio si hay solape no es juzgarlo: la linea se cruza al decidir.
     */
    private static List<String> quienJuzgaSolapesOHuecos(Path raiz) {
        List<String> porSuCuenta = new ArrayList<>();
        for (String vertical : verticalesDeTipoA(raiz)) {
            for (Path fichero : ficheros(raiz.resolve(vertical))) {
                if (fichero.getFileName().toString().endsWith("TimelineService.java")) {
                    continue;
                }
                if (PATRON_VEREDICTO.matcher(sinComentarios(leer(fichero))).find()) {
                    porSuCuenta.add(relativo(raiz, fichero));
                }
            }
        }
        porSuCuenta.sort(null);
        return porSuCuenta;
    }

    private static List<String> rastrosEnElCodigo(Path raiz, Pattern patron) {
        List<String> rastros = new ArrayList<>();
        for (Path fichero : ficheros(raiz)) {
            String nombre = fichero.getFileName().toString();
            if (nombre.equals(ESTA_GUARDA)) {
                continue;
            }
            if (patron.matcher(nombre).find() || patron.matcher(sinComentarios(leer(fichero))).find()) {
                rastros.add(relativo(raiz, fichero));
            }
        }
        rastros.sort(null);
        return rastros;
    }

    private static List<String> rastrosEnElContrato(Pattern patron) {
        if (Files.isRegularFile(CONTRATO) && patron.matcher(leer(CONTRATO)).find()) {
            return new ArrayList<>(List.of(CONTRATO.toString().replace('\\', '/')));
        }
        return new ArrayList<>();
    }

    /**
     * Verbos que expone cada vertical, leidos de los controladores que cuelgan
     * de /employees. El nombre del metodo no dice nada; lo que dice es la
     * anotacion y la ruta.
     */
    private static Map<String, Set<String>> verbosPorVertical(Path raiz) {
        Map<String, Set<String>> porVertical = new LinkedHashMap<>();
        for (Path vertical : verticales(raiz)) {
            Set<String> verbos = new TreeSet<>();
            for (Path fichero : ficheros(vertical)) {
                if (!fichero.getFileName().toString().endsWith("Controller.java")) {
                    continue;
                }
                String codigo = sinComentarios(leer(fichero));
                if (!codigo.contains("@RequestMapping(\"/employees/")) {
                    continue;
                }
                verbos.addAll(verbosDe(codigo));
            }
            if (!verbos.isEmpty()) {
                porVertical.put(vertical.getFileName().toString(), verbos);
            }
        }
        return porVertical;
    }

    private static final Pattern PATRON_MAPEO = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\")?");

    private static Set<String> verbosDe(String codigo) {
        Set<String> verbos = new TreeSet<>();
        Matcher mapeo = PATRON_MAPEO.matcher(codigo);
        while (mapeo.find()) {
            String metodo = mapeo.group(1);
            String ruta = mapeo.group(2) == null ? "" : mapeo.group(2);

            if (ruta.endsWith("/close")) {
                verbos.add("close");
            } else if (ruta.contains("replace") && ruta.contains("from-date")) {
                verbos.add("sustituir-desde-fecha");
            } else if (ruta.endsWith("/plan")) {
                verbos.add("planificar");
            } else {
                switch (metodo) {
                    case "Post" -> verbos.add("crear");
                    case "Put", "Patch" -> verbos.add("corregir");
                    case "Delete" -> verbos.add("borrar");
                    case "Get" -> verbos.add("leer");
                    default -> { }
                }
            }
        }
        return verbos;
    }

    // ------------------------------------------------------------------
    // Andamio
    // ------------------------------------------------------------------

    private static List<Path> verticales(Path raiz) {
        try (Stream<Path> hijos = Files.list(raiz)) {
            return hijos
                    .filter(Files::isDirectory)
                    .filter(p -> !NO_SON_VERTICALES.contains(p.getFileName().toString()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Path> ficheros(Path carpeta) {
        if (!Files.isDirectory(carpeta)) {
            return List.of();
        }
        try (Stream<Path> ficheros = Files.walk(carpeta)) {
            return ficheros
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().endsWith(".java"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean algunFichero(Path carpeta, java.util.function.Predicate<Path> criterio) {
        return ficheros(carpeta).stream().anyMatch(criterio);
    }

    private static String leer(Path fichero) {
        try {
            return Files.readString(fichero, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String relativo(Path raiz, Path fichero) {
        return raiz.relativize(fichero).toString().replace('\\', '/');
    }

    private static void escribir(Path fichero, String contenido) throws IOException {
        Files.createDirectories(fichero.getParent());
        Files.writeString(fichero, contenido, StandardCharsets.UTF_8);
    }

    /**
     * Quita los comentarios respetando las cadenas, los bloques de texto y los
     * escapes. Hace falta para las reglas 3 y 4: contar en un comentario por
     * que se retiro el patron viejo no puede ser tenerlo, y un "//" dentro de
     * una cadena no abre un comentario. El contenido de un bloque de texto se
     * tira entero, que es donde los tests escriben arboles de mentira.
     */
    static String sinComentarios(String fuente) {
        StringBuilder limpio = new StringBuilder(fuente.length());
        int i = 0;
        int n = fuente.length();

        while (i < n) {
            char c = fuente.charAt(i);

            if (c == '/' && i + 1 < n && fuente.charAt(i + 1) == '/') {
                while (i < n && fuente.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (c == '/' && i + 1 < n && fuente.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(fuente.charAt(i) == '*' && fuente.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(i + 2, n);
                continue;
            }
            if (fuente.startsWith("\"\"\"", i)) {
                int fin = fuente.indexOf("\"\"\"", i + 3);
                i = fin < 0 ? n : fin + 3;
                limpio.append("\"\"");
                continue;
            }
            if (c == '"' || c == '\'') {
                limpio.append(c);
                i++;
                while (i < n && fuente.charAt(i) != c) {
                    if (fuente.charAt(i) == '\\') {
                        i++;
                    }
                    if (i < n) {
                        limpio.append(fuente.charAt(i));
                    }
                    i++;
                }
                i++;
                limpio.append(c);
                continue;
            }

            limpio.append(c);
            i++;
        }

        return limpio.toString();
    }

}
