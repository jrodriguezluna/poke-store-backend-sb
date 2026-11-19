package cl.pokestore.backend.usuario;

public record UsuarioResponse(
        Integer id,
        String nombre,
        String email,
        String telefono,
        String direccion,
        String region,
        String comuna,
        String rol
) {

    public static UsuarioResponse from(UsuarioEntity usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getRegion(),
                usuario.getComuna(),
                usuario.getRol()
        );
    }
}
