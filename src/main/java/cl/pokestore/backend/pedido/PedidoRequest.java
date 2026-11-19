package cl.pokestore.backend.pedido;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedidoRequest(
        @NotNull Integer usuarioId,
        @NotNull @Size(min = 1) List<@Valid ItemRequest> items
) {
}
