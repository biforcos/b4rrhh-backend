package com.b4rrhh.support;

import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.Optional;
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
    public PayslipDocumentStorage payslipDocumentStorageEnMemoria() {
        Map<String, byte[]> objetos = new ConcurrentHashMap<>();
        return new PayslipDocumentStorage() {
            @Override
            public void store(String objectKey, byte[] content, String contentType) {
                objetos.put(objectKey, content);
            }

            @Override
            public Optional<byte[]> retrieve(String objectKey) {
                return Optional.ofNullable(objetos.get(objectKey));
            }
        };
    }
}
