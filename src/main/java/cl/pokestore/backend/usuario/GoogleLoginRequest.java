package cl.pokestore.backend.usuario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
        @NotNull @Size(min = 1) String credential
) {
}
