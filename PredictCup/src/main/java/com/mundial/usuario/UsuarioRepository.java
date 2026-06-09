package com.mundial.usuario;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UsuarioRepository implements PanacheRepositoryBase<Usuario, UUID> {

    public Optional<Usuario> findByGoogleSub(String sub) {
        return find("googleSub", sub).firstResultOptional();
    }

    public Optional<Usuario> findByCorreo(String correo) {
        return find("correo", correo).firstResultOptional();
    }
}
