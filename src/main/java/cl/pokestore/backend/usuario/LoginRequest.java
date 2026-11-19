package cl.pokestore.backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotNull @Email String email,
        @NotNull @Size(max = 64) String password
) {
}
