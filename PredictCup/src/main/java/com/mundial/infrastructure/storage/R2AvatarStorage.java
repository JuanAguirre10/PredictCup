package com.mundial.infrastructure.storage;

import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.Optional;

/**
 * Sube los avatares a Cloudflare R2 (compatible con S3) y devuelve la URL publica del
 * bucket. Activa con app.storage.avatar=r2. El cliente S3 se construye perezosamente,
 * asi que en modo local esta clase ni siquiera se instancia (no exige credenciales).
 */
@ApplicationScoped
@LookupIfProperty(name = "app.storage.avatar", stringValue = "r2")
public class R2AvatarStorage implements AvatarStorage {

    // Optional: en modo local estas props van vacias y Quarkus no debe fallar al validarlas.
    @ConfigProperty(name = "app.storage.r2.endpoint")
    Optional<String> endpoint;

    @ConfigProperty(name = "app.storage.r2.access-key")
    Optional<String> accessKey;

    @ConfigProperty(name = "app.storage.r2.secret-key")
    Optional<String> secretKey;

    @ConfigProperty(name = "app.storage.r2.bucket")
    Optional<String> bucket;

    @ConfigProperty(name = "app.storage.r2.public-url")
    Optional<String> publicUrl;

    private volatile S3Client s3;

    private static String requerido(Optional<String> valor, String prop) {
        return valor.filter(v -> !v.isBlank())
                .orElseThrow(() -> new IllegalStateException("Falta la config " + prop));
    }

    private S3Client s3() {
        S3Client local = s3;
        if (local == null) {
            synchronized (this) {
                if (s3 == null) {
                    s3 = S3Client.builder()
                            .endpointOverride(URI.create(requerido(endpoint, "app.storage.r2.endpoint")))
                            .region(Region.of("auto")) // R2 ignora la region
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                    requerido(accessKey, "app.storage.r2.access-key"),
                                    requerido(secretKey, "app.storage.r2.secret-key"))))
                            .httpClient(UrlConnectionHttpClient.create())
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(true).build())
                            .build();
                }
                local = s3;
            }
        }
        return local;
    }

    @Override
    public String subir(String clave, byte[] datos, String contentType) {
        s3().putObject(
                PutObjectRequest.builder()
                        .bucket(requerido(bucket, "app.storage.r2.bucket"))
                        .key(clave).contentType(contentType).build(),
                RequestBody.fromBytes(datos));
        return requerido(publicUrl, "app.storage.r2.public-url").replaceAll("/+$", "") + "/" + clave;
    }
}
