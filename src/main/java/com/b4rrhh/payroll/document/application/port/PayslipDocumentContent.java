package com.b4rrhh.payroll.document.application.port;

import java.util.List;

/**
 * El recibo, ya resuelto y listo para dibujar ({@code backend#112}).
 *
 * <p>Aqui no queda nada por decidir: los numeros vienen escritos, los bloques vienen ordenados y
 * los nombres vienen elegidos. Eso es a proposito. El renderizador sabe de A4, de milimetros y de
 * tipografias, y no tiene que saber que una linea de naturaleza {@code NET_PAY} cierra el recibo
 * ni con cuantos decimales se enseña una tarifa. <b>Si esas reglas viviesen en el dibujo, la unica
 * forma de comprobarlas seria leer un PDF</b>, y entonces el criterio 5 del issue —que el PDF diga
 * lo mismo que el folio— dejaria de poder escribirse.
 *
 * <p>Todos los campos de texto pueden ser nulos menos los codigos: una foto de contexto vieja a la
 * que le falta un dato ensena un hueco, que es la verdad.
 */
public record PayslipDocumentContent(
        /**
         * Si este documento es un borrador ({@code backend#112}).
         *
         * <p>Verdadero en todo lo que no sea {@code DEFINITIVE}. Lo que cambia no es el contenido
         * —las cifras son las mismas— sino que el papel dice que todavia no se ha entregado nada.
         */
        boolean draft,
        Party company,
        Party employee,
        LaborData labor,
        List<Block> blocks,
        Provenance provenance
) {

    /**
     * Una de las dos cajas de la cabecera: la empresa que paga o quien cobra.
     *
     * @param details las lineas de debajo del nombre, ya escritas y sin huecos. Son una lista y no
     *                campos con nombre porque las dos cajas no llevan lo mismo: la del trabajador
     *                lleva NIF y matricula, la de la empresa lleva CIF. Un record con los campos de
     *                las dos obligaria a la mitad a ir siempre en nulo.
     */
    public record Party(
            String name,
            List<String> details
    ) {}

    /** El recuadro de datos laborales, con los rotulos ya puestos. */
    public record LaborData(
            String agreement,
            String category,
            String period,
            String workCenter,
            String seniority
    ) {}

    /** Una linea del recibo. Las cifras vienen escritas, no en crudo. */
    public record Line(
            String period,
            String code,
            String label,
            String quantity,
            String rate,
            String amount
    ) {}

    /**
     * Un bloque del modelo oficial.
     *
     * <p>No lleva subtotal, y esa ausencia es la afirmacion del {@code backend#114}: <b>ninguna
     * cifra del papel se calcula aqui</b>. Los cuatro bloques que el modelo oficial totaliza
     * traen su total como una linea mas —{@code 970}, {@code 980}, {@code 990} y, desde la
     * {@code V141}, el {@code 725} de la aportacion empresarial—, y el recuadro de bases no se
     * totaliza porque su suma no significa nada. El motor calcula; el papel lee.
     *
     * @param closing si se pinta como la linea de cierre del recibo en vez de como una tabla.
     */
    public record Block(
            String label,
            List<Line> lines,
            boolean closing
    ) {}

    /**
     * De donde sale este papel ({@code backend#112}).
     *
     * <p>Es lo que convierte el PDF en algo mas que una hoja con numeros: con la clave impresa,
     * quien tenga el papel encuentra el recibo en el sistema y ve de donde sale cada linea.
     * <b>El puente a la explicacion es la clave, no el contenido</b>: por eso aqui hay una
     * direccion y no un solo paso de calculo.
     */
    public record Provenance(
            String businessKey,
            String presence,
            String runId,
            String calculatedAt,
            String engine
    ) {}
}
