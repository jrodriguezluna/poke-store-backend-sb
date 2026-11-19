package cl.pokestore.backend.pedido;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoHistoryResponse(
        Integer id,
        Integer total,
        LocalDateTime fecha,
        List<ItemHistoryResponse> items
) {

    public static PedidoHistoryResponse from(PedidoEntity pedido) {
        List<ItemHistoryResponse> items = pedido.getItems().stream()
                .map(item -> new ItemHistoryResponse(
                        item.getProducto().getNombre(),
                        item.getCantidad(),
                        item.getPrecioUnit()
                ))
                .toList();
        return new PedidoHistoryResponse(pedido.getId(), pedido.getTotal(), pedido.getCreadoEn(), items);
    }
}
