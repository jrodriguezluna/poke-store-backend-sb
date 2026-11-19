package cl.pokestore.backend.producto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotNull @Size(min = 1, max = 100) String nombre,
        @Size(max = 50) String categoria,
        @NotNull @Min(0) Integer precio,
        @Min(0) Integer stock,
        @Size(max = 255) String imagen,
        String descripcion
) {
}
