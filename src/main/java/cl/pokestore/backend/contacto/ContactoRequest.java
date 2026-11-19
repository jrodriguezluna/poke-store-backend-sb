package cl.pokestore.backend.contacto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
        @NotNull @Size(min = 1, max = 100) String nombre,
        @NotNull @Email String email,
        @NotNull @Size(min = 1, max = 2000) String mensaje
) {
}
