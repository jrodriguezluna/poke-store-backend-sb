package cl.pokestore.backend.blog;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlogRequest(
        @NotBlank @Size(max = 100) String etiqueta,
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank @Size(max = 1000) String resumen,
        @NotBlank @Size(max = 100) String autor,
        @Size(max = 500)
        @Pattern(regexp = "^(https?://\\S+|/\\S*)?$")
        String imagen,
        @Size(max = 255) String textoAlternativo,
        @NotBlank @Size(max = 10000) String introduccion,
        @Size(max = 5000) String consejo,
        @NotNull @Size(min = 1) List<@Valid BlogSeccionRequest> secciones
) {
}
