package com.b4rrhh.support;

import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.ConnectException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El almacen de documentos de los tests de extremo a extremo ({@code backend#112}).
 *
 * <h2>Por que aqui si hay un doble, habiendo Postgres de verdad</h2>
 *
 * <p>La suite corre contra Postgres real a proposito: el esquema <b>es</b> lo que se esta
 * probando. Con MinIO no pasa lo mismo. Desde que cerrar un recibo emite su documento, tests que
 * no van de almacenamiento —el cierre en masa y sus contadores— pasaron a necesitar un servidor de
 * objetos levantado, y eso es acoplamiento accidental: no prueban nada de MinIO y sin embargo no
 * corren sin el.
 *
 * <p>Dos cosas lo decidieron, y las dos se vieron, no se supusieron:
 *
 * <ul>
 *   <li><b>El pipeline no tiene MinIO.</b> Levanta Postgres y nada mas, asi que la suite habria
 *       empezado a fallar alli mientras seguia verde en el portatil, que es la peor forma de
 *       romperse.</li>
 *   <li><b>La suite escribia en el MinIO de verdad del portatil.</b> Se vio mirando el bucket:
 *       ocho PDFs de sistemas de reglas de test —{@code CLB}, {@code CLC}— con la hora exacta de
 *       las dos corridas. Un {@code mvn test} no deja basura en el almacen de nadie.</li>
 * </ul>
 *
 * <p>Lo que este doble <b>no</b> sustituye: que el adaptador de MinIO guarda y sirve de verdad.
 * Eso no se deduce de aqui y no se prueba con un mapa — se comprueba cerrando un recibo contra el
 * backend arrancado y <b>mirando el bucket</b>, que es lo que el issue pide y como se hizo.
 *
 * <p>Va en el {@code classes} de {@link TestWebSobreEsquemaReal} y no en un {@code @Import} por
 * test: cualquier anotacion propia en una clase de test la saca del contexto compartido y clona la
 * base otra vez.
 */
@Configuration
public class AlmacenDeDocumentosEnMemoria {

    // El nombre no puede ser el de la clase: Spring registra la propia @Configuration como un
    // bean llamado «almacenDeDocumentosEnMemoria», y el @Bean del mismo nombre revienta el
    // contexto entero con un BeanDefinitionOverrideException.
    @Bean
    @Primary
    public AlmacenEnMemoria payslipDocumentStorageEnMemoria() {
        return new AlmacenEnMemoria();
    }

    /**
     * El mapa, con un interruptor para caerse ({@code backend#113}).
     *
     * <p>Sale a clase con nombre —era una anonima— porque un doble que no sabe fallar deja sin
     * probar la unica rama que de verdad importa del cierre: la de no cerrar. El {@code #112}
     * decidio que cerrar con el almacen inalcanzable falla y que el recibo se queda
     * {@code CALCULATED}, y eso solo lo habia visto una persona, una vez, con MinIO parado a mano.
     * Un test que existe solo a mano se deja de correr el dia que se toca el cierre, que es el
     * unico dia en que hace falta.
     *
     * <p>El interruptor <b>no</b> es una segunda implementacion del almacen: caido tiene que
     * fallar como falla el de verdad, con la excepcion del dominio, o el verde no diria nada del
     * cierre real. Ver {@link #caida(String)}.
     */
    public static final class AlmacenEnMemoria implements PayslipDocumentStorage {

        private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

        /** {@code volatile} porque quien lo mueve es el test y quien lo lee es el servicio. */
        private volatile boolean caido;

        /** Deja de contestar, como el MinIO apagado contra el que se probo a mano el {@code #112}. */
        public void caer() {
            this.caido = true;
        }

        /**
         * Vuelve.
         *
         * <p>Quien lo tumba lo levanta antes de terminar, y no por cortesia: el contexto de
         * {@link TestWebSobreEsquemaReal} es uno solo y se comparte con toda la suite. Un almacen
         * que se queda caido no rompe el test que lo tumbo — rompe el siguiente.
         */
        public void levantar() {
            this.caido = false;
        }

        /** Lo que hay guardado, para afirmar que algo se archivo o que no se archivo nada. */
        public Set<String> claves() {
            return Set.copyOf(objetos.keySet());
        }

        @Override
        public void store(String objectKey, byte[] content, String contentType) {
            if (caido) {
                throw caida(objectKey);
            }
            objetos.put(objectKey, content);
        }

        @Override
        public Optional<byte[]> retrieve(String objectKey) {
            if (caido) {
                throw caida(objectKey);
            }
            return Optional.ofNullable(objetos.get(objectKey));
        }

        /**
         * Exactamente lo que sale del adaptador de MinIO cuando el servidor no esta: la excepcion
         * del dominio envolviendo el fallo de red.
         *
         * <p>Vacio y caido son dos cosas distintas y se distinguen —caido revienta, vacio devuelve
         * {@link Optional#empty()}—, que es lo que dice el puerto y lo que hace
         * {@code MinioPayslipDocumentStorageAdapter}. Un doble que devolviera vacio al estar caido
         * probaria la mentira mas cara: «ese recibo no tiene documento».
         */
        private PayslipDocumentStorageUnavailableException caida(String objectKey) {
            return new PayslipDocumentStorageUnavailableException(
                    objectKey, new ConnectException("Connection refused: el almacen esta apagado"));
        }
    }
}
