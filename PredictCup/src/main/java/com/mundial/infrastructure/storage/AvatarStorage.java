package com.mundial.infrastructure.storage;

/**
 * Almacenamiento de avatares. La implementacion activa se elige por config
 * (app.storage.avatar = local | r2). La BD solo guarda la URL que devuelve subir().
 */
public interface AvatarStorage {

    /**
     * Sube el avatar y devuelve su URL publica (absoluta).
     *
     * @param clave       nombre del objeto (p.ej. "{idUsuario}.png")
     * @param datos       bytes de la imagen
     * @param contentType tipo MIME (image/png, image/jpeg, image/webp)
     */
    String subir(String clave, byte[] datos, String contentType);
}
