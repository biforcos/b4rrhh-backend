package com.b4rrhh.support;

import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El almacén de fotos de los tests de extremo a extremo (backend#177).
 *
 * <p>Por lo mismo que {@link AlmacenDeDocumentosEnMemoria}: el pipeline no tiene MinIO, y un
 * {@code mvn test} no deja fotos en el almacén de nadie. Lo que esto no prueba —que el adaptador de
 * MinIO guarda y sirve de verdad— se comprueba contra el backend arrancado, mirando el cubo.
 *
 * <p>Va en el {@code classes} de {@link TestWebSobreEsquemaReal}, y no en un {@code @Import}, por la
 * misma razón que el de los documentos: una anotación propia saca al test del contexto compartido.
 */
@Configuration
public class AlmacenDeFotosEnMemoria {

    @Bean
    @Primary
    public FotosEnMemoria employeePhotoStorageEnMemoria() {
        return new FotosEnMemoria();
    }

    public static final class FotosEnMemoria implements EmployeePhotoStoragePort {

        private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();

        public Set<String> claves() {
            return Set.copyOf(objetos.keySet());
        }

        @Override
        public void store(String objectKey, byte[] jpeg) {
            objetos.put(objectKey, jpeg);
        }

        @Override
        public Optional<byte[]> retrieve(String objectKey) {
            return Optional.ofNullable(objetos.get(objectKey));
        }

        @Override
        public void delete(String objectKey) {
            objetos.remove(objectKey);
        }
    }
}
