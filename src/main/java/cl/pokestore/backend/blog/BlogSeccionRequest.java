package cl.pokestore.backend.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogSeccionRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 10000) String texto
) {
}
