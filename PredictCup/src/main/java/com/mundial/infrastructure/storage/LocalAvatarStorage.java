package com.mundial.infrastructure.storage;

import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Guarda los avatares en un directorio local (un volumen en Docker) y los sirve por
 * GET /api/usuarios/avatars/{clave}. Activa por defecto (app.storage.avatar=local o sin
 * definir). Pensada para desarrollo/demo sin cuenta de Cloudflare.
 */
@ApplicationScoped
@LookupIfProperty(name = "app.storage.avatar", stringValue = "local", lookupIfMissing = true)
public class LocalAvatarStorage implements AvatarStorage {

    @ConfigProperty(name = "app.storage.local.dir", defaultValue = "avatars")
    String dir;

    @ConfigProperty(name = "app.public-base-url", defaultValue = "http://localhost:8080")
    String baseUrl;

    @Override
    public String subir(String clave, byte[] datos, String contentType) {
        try {
            Path destino = Path.of(dir);
            Files.createDirectories(destino);
            Files.write(destino.resolve(clave), datos);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el avatar", e);
        }
        return baseUrl.replaceAll("/+$", "") + "/api/usuarios/avatars/" + clave;
    }

    /** Lee los bytes de un avatar guardado (para servirlo). */
    public Optional<byte[]> leer(String clave) {
        try {
            Path base = Path.of(dir).toAbsolutePath().normalize();
            Path p = base.resolve(clave).normalize();
            // Contención: la ruta resuelta no puede salir del directorio base.
            if (!p.startsWith(base)) {
                return Optional.empty();
            }
            return Files.exists(p) ? Optional.of(Files.readAllBytes(p)) : Optional.empty();
        } catch (RuntimeException | IOException e) {
            return Optional.empty();
        }
    }
}
