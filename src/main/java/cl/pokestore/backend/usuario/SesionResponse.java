package cl.pokestore.backend.usuario;

public record SesionResponse(
        Integer id,
        String nombre,
        String email,
        String telefono,
        String direccion,
        String region,
        String comuna,
        String rol,
        String token
) {

    public static SesionResponse from(UsuarioResponse usuario, String token) {
        return new SesionResponse(
                usuario.id(),
                usuario.nombre(),
                usuario.email(),
                usuario.telefono(),
                usuario.direccion(),
                usuario.region(),
                usuario.comuna(),
                usuario.rol(),
                token
        );
    }
}
