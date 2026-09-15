package com.b4rrhh.payroll.infrastructure.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code POST /payrolls/calculate} se retiro, y aqui esta escrito por que.
 *
 * <h2>Que era</h2>
 *
 * Un <b>stub</b>: recibia un recibo ya calculado en el cuerpo —sus conceptos, sus importes, sus
 * instantaneas de contexto— y lo materializaba en la base. <b>No llamaba al motor.</b> Su propio
 * comentario lo decia: «Temporary stub endpoint used to materialize payroll results before launch
 * and real engine flows exist».
 *
 * <h2>Por que se fue</h2>
 *
 * Las dos cosas que decia esperar ya existen. El lanzamiento llego con el {@code backend#75} y el
 * ADR-060; el motor calcula por grafo desde mucho antes; y el recalculo de una unidad tiene su
 * propia ruta, que si se usa. El barrido del {@code backend#90} —codigo de aplicacion del
 * frontend fuera de {@code core/api/generated}, mas el designer y el loader— no encontro
 * <b>ningun</b> llamante.
 *
 * <p>Y lo que no podia seguir es publicada sin que nadie supiera cual de las dos cosas era: una
 * operacion servida que unos leen como «el API de calculo» y otros como un resto es exactamente
 * el defecto que el {@code backend#80} acaba de cerrar por el otro lado.
 *
 * <h2>Que se fue con ella, y que no</h2>
 *
 * Se fueron la ruta, el metodo del controlador y sus tres DTO de peticion
 * —{@code CalculatePayrollRequest}, {@code PayrollConceptRequest},
 * {@code PayrollContextSnapshotRequest}—, que no los usaba nadie mas.
 *
 * <p><b>Se queda {@code CalculatePayrollUseCase}</b>, que no era suyo: lo usa
 * {@code CalculatePayrollUnitService}, o sea el lanzamiento y el recalculo. Borrarlo por arrastre
 * habria sido confundir «se retiro su endpoint» con «no hace falta».
 *
 * <p>Y se queda {@code finalizePayroll}, que el mismo issue mira y que <b>no</b> se retira: no
 * estaba muerta, estaba esperando a su mitad, y en este mismo issue se le construye.
 *
 * <h2>Por que este fichero</h2>
 *
 * Porque el criterio 4 del {@code backend#90} pide que lo que sale del contrato quede escrito
 * donde y por que, y no desaparezca sin rastro. Un mensaje de commit se lee una vez; esto se
 * ejecuta. Si alguien vuelve a servir esa ruta sin decidirlo, se pone rojo.
 */
class TheTemporaryCalculateStubIsGoneOnPurposeTest {

    private static final Path CONTRATO = Path.of("openapi/personnel-administration-api.yaml");

    @Test
    void theRouteIsNotInTheContract() throws IOException {
        String contrato = Files.readString(CONTRATO, StandardCharsets.UTF_8);

        assertThat(contrato)
                .withFailMessage("""
                        /payrolls/calculate ha vuelto al contrato.

                        Era un stub que materializaba un recibo recibido en el cuerpo, sin pasar por
                        el motor, y se retiro en el backend#90 porque las dos cosas que decia
                        esperar —el lanzamiento y el motor— ya existen y nadie lo llamaba.

                        Si hace falta otra vez, hara falta por un motivo nuevo: escribelo, y borra
                        este test con el motivo delante.
                        """)
                .doesNotContain("/payrolls/calculate:");

        assertThat(contrato)
                .withFailMessage("Han vuelto los esquemas de peticion del stub retirado.")
                .doesNotContain("CalculatePayrollRequest")
                .doesNotContain("PayrollContextSnapshotRequest");
    }

    @Test
    void noControllerServesItEither() {
        assertThat(Arrays.stream(PayrollController.class.getDeclaredMethods()).map(Method::getName))
                .withFailMessage("PayrollController vuelve a tener el metodo del stub retirado.")
                .noneMatch(nombre -> nombre.contains("calculateTemporaryStub"));
    }

    /**
     * Y lo que no se fue con el: el caso de uso, que no era suyo.
     *
     * <p>Lo usa {@code CalculatePayrollUnitService}, o sea el lanzamiento masivo y el recalculo.
     * Esta afirmacion existe para que quien lea «el endpoint de calculo se retiro» no vaya detras
     * a limpiar lo que le parezca del mismo lote: {@code muerto no quiere decir vacio}, y aqui
     * ni siquiera estaba muerto.
     */
    @Test
    void theUseCaseBehindItStaysBecauseItWasNeverItsOwn() throws Exception {
        Class<?> unitService = Class.forName("com.b4rrhh.payroll.application.usecase.CalculatePayrollUnitService");

        assertThat(Arrays.stream(unitService.getDeclaredFields()).map(f -> f.getType().getSimpleName()))
                .withFailMessage("""
                        CalculatePayrollUnitService ya no usa CalculatePayrollUseCase.

                        Si es a proposito, bien; pero entonces este test sobra y hay que quitarlo
                        diciendolo. Lo que este fichero impide es que se borre por arrastre al
                        retirar el endpoint que lo exponia (backend#90).
                        """)
                .contains("CalculatePayrollUseCase");
    }
}
