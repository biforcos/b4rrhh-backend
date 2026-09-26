package com.b4rrhh.employee.shared.application.port;

/**
 * <b>El unico sitio por el que una escritura con fecha avisa de que ha tocado el pasado</b>
 * ({@code backend#130}, paso 6 de {@code b4rrhh/workspace#9}).
 *
 * <p>Toda alta, correccion o borrado de una fila con fecha —las verticales de la presencia y las
 * entradas de nomina— llama aqui. Si la fecha cae en un periodo que ya tiene recibo definitivo,
 * al otro lado queda una <i>marca de retroactividad</i>; si no, no pasa nada. <b>Quien escribe no
 * sabe cual de las dos cosas ocurre</b>, y eso es el punto: la regla de «que cuenta como pasado»
 * se escribe una vez.
 *
 * <p>Hay un candado que lo comprueba
 * ({@code EveryDatedWriteAnnouncesItselfThroughOnePortTest}): un caso de uso nuevo que escriba una
 * tabla con fecha y no pase por aqui sale rojo en el commit que lo anade. Hace falta porque ningun
 * test de comportamiento lo puede ver — una vertical que no avisa no rompe nada hoy, sencillamente
 * no se recalcula el dia que alguien le meta algo a un mes cerrado, y eso no se nota hasta que un
 * empleado cobra de menos.
 *
 * <h2>Por que la interfaz esta en {@code employee} y el adaptador en {@code payroll}</h2>
 *
 * <p>Porque {@code employee} no importa {@code payroll} en ninguna parte de este arbol, y la
 * dependencia va en un solo sentido: {@code payroll} lee {@code employee} por adaptadores en su
 * {@code infrastructure}. Declarar aqui la interfaz y dejar la implementacion alli respeta esa
 * direccion; hacerlo al reves la invertiria por una tabla.
 */
public interface DatedWriteNoticePort {

    /**
     * Avisa de una escritura con fecha.
     *
     * <p>No devuelve nada a proposito. Quien escribe no tiene que decidir nada con la respuesta —si
     * la decidiera, la regla estaria repartida— y una marca que no se escribe no es un error de la
     * escritura: es que el periodo estaba abierto.
     */
    void notice(DatedWrite write);
}
