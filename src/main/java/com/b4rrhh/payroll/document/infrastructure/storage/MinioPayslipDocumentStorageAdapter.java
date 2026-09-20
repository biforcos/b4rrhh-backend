package com.b4rrhh.payroll.document.infrastructure.storage;

import com.b4rrhh.payroll.document.domain.exception.PayslipDocumentStorageUnavailableException;
import com.b4rrhh.payroll.document.domain.port.PayslipDocumentStorage;
import com.b4rrhh.payroll.document.infrastructure.config.PayslipDocumentStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

/**
 * Los documentos de los recibos, en MinIO ({@code backend#112}).
 *
 * <p>Reutiliza el {@code MinioClient} que ya estaba cableado para las fotos de empleado: el issue
 * dice que el PDF tiene donde vivir y un adaptador que copiar, y que no se invente almacenamiento.
 * Lo que no comparte es el cubo, porque no comparte la politica: el de las fotos se sirve publico
 * y un recibo de nomina no.
 *
 * <p><b>Aqui no se tapa ningun fallo.</b> Es lo unico importante de este fichero. Un
 * {@code catch} que devolviera {@code empty} convertiria un almacen apagado en «ese recibo no
 * tiene documento», y entonces el cierre pasaria y el {@code DEFINITIVE} se quedaria sin papel
 * sin que nadie se entere. Vacio y caido son dos cosas y se distinguen por el codigo de error que
 * devuelve el servidor, no por la ausencia de bytes.
 */
@Component
@EnableConfigurationProperties(PayslipDocumentStorageProperties.class)
public class MinioPayslipDocumentStorageAdapter implements PayslipDocumentStorage {

    /** Lo que MinIO contesta cuando lo que falta es el objeto, no el servidor. */
    private static final Set<String> NO_ESTA = Set.of("NoSuchKey", "NoSuchObject", "NoSuchBucket");

    private final MinioClient minioClient;
    private final PayslipDocumentStorageProperties properties;

    public MinioPayslipDocumentStorageAdapter(
            MinioClient minioClient,
            PayslipDocumentStorageProperties properties
    ) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        try {
            asegurarCubo();
            try (InputStream datos = new ByteArrayInputStream(content)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.bucketName())
                        .object(objectKey)
                        .stream(datos, content.length, -1)
                        .contentType(contentType)
                        .build());
            }
        } catch (Exception e) {
            throw new PayslipDocumentStorageUnavailableException(objectKey, e);
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
            throw new PayslipDocumentStorageUnavailableException(objectKey, e);
        } catch (Exception e) {
            throw new PayslipDocumentStorageUnavailableException(objectKey, e);
        }
    }

    /**
     * El cubo se crea la primera vez que hace falta, y no al arrancar.
     *
     * <p>Al arrancar tendria que hablar con MinIO para levantar la aplicacion, y entonces un MinIO
     * caido impediria <b>consultar</b> un recibo, que no necesita el almacen para nada. Aqui solo
     * bloquea lo que de verdad depende de el: cerrar.
     *
     * <p>Y por eso no esta en el {@code docker-compose}, al lado del cubo de las fotos: el de las
     * fotos se crea alli porque ademas se le pone politica publica. Este no lleva ninguna.
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
