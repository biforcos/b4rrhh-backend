package com.b4rrhh.employee.photo.infrastructure.storage;

import com.b4rrhh.employee.photo.domain.EmployeePhotoStorageUnavailableException;
import com.b4rrhh.employee.photo.domain.port.EmployeePhotoStoragePort;
import com.b4rrhh.employee.photo.infrastructure.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

/**
 * Las fotos de los empleados, en MinIO (backend#177).
 *
 * <p>Bytes por clave y nada más. Aquí había {@code buildPublicUrl} y {@code extractObjectKey}: el
 * almacén se veía desde fuera, y la URL que se componía era la que veía <b>este</b> backend, que no
 * le servía a ningún navegador fuera de su máquina. Ahora la foto la sirve la API y el cubo puede ser
 * privado.
 *
 * <p>Como en el de los recibos ({@code MinioPayslipDocumentStorageAdapter}), vacío y caído no se
 * confunden: lo que falta da vacío, una avería del almacén da excepción.
 */
@Component
public class MinioEmployeePhotoStorageAdapter implements EmployeePhotoStoragePort {

    private static final Set<String> NO_ESTA = Set.of("NoSuchKey", "NoSuchObject", "NoSuchBucket");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioEmployeePhotoStorageAdapter(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void store(String objectKey, byte[] jpeg) {
        try {
            asegurarCubo();
            try (InputStream datos = new ByteArrayInputStream(jpeg)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.bucketName())
                        .object(objectKey)
                        .stream(datos, jpeg.length, -1)
                        .contentType("image/jpeg")
                        .build());
            }
        } catch (Exception e) {
            throw new EmployeePhotoStorageUnavailableException(objectKey, e);
        }
    }

    @Override
    public Optional<byte[]> retrieve(String objectKey) {
        try (InputStream datos = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucketName())
                .object(objectKey)
                .build())) {
            return Optional.of(datos.readAllBytes());
        } catch (ErrorResponseException e) {
            if (NO_ESTA.contains(e.errorResponse().code())) {
                return Optional.empty();
            }
            throw new EmployeePhotoStorageUnavailableException(objectKey, e);
        } catch (Exception e) {
            throw new EmployeePhotoStorageUnavailableException(objectKey, e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucketName())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new EmployeePhotoStorageUnavailableException(objectKey, e);
        }
    }

    /**
     * Si el cubo no está, se crea, y sin política: privado. Hasta backend#177 lo creaba el
     * {@code minio-init} de la demo con descarga anónima, porque el navegador iba a buscar la foto
     * allí directamente. Ya no va.
     */
    private void asegurarCubo() throws Exception {
        boolean existe = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucketName())
                .build());
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucketName())
                    .build());
        }
    }
}
