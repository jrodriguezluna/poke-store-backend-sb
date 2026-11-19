package cl.pokestore.backend.pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
        @NotNull Integer productoId,
        @NotNull @Min(1) Integer cantidad
) {
}
