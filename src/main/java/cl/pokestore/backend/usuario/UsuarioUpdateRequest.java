package cl.pokestore.backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateRequest(
        @NotNull @Size(min = 1, max = 100) String nombre,
        @NotNull @Email @Size(max = 120) String email,
        @Size(max = 20) String telefono,
        @NotNull @Size(min = 5, max = 200) String direccion,
        @NotNull @Size(min = 1, max = 100) String region,
        @NotNull @Size(min = 1, max = 100) String comuna,
        @Pattern(regexp = "cliente|vendedor|admin") String rol,
        @Size(min = 6, max = 64) String password
) {
}
