package cl.pokestore.backend.pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.producto.ProductoEntity;
import cl.pokestore.backend.producto.ProductoRepository;
import cl.pokestore.backend.usuario.UsuarioEntity;
import cl.pokestore.backend.usuario.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest request) {
        UsuarioEntity usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Usuario inexistente"));

        Map<Integer, Long> cantidades = new LinkedHashMap<>();
        for (ItemRequest item : request.items()) {
            cantidades.merge(item.productoId(), item.cantidad().longValue(), Long::sum);
        }

        List<ProductoEntity> encontrados = productoRepository.findActiveForUpdate(cantidades.keySet());
        Map<Integer, ProductoEntity> productos = encontrados.stream()
                .collect(Collectors.toMap(ProductoEntity::getId, producto -> producto));
        Set<Integer> faltantes = new TreeSet<>(cantidades.keySet());
        faltantes.removeAll(productos.keySet());
        if (!faltantes.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Productos no disponibles: " + faltantes);
        }

        List<String> sinStock = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : cantidades.entrySet()) {
            ProductoEntity producto = productos.get(entry.getKey());
            if (producto.getStock() < entry.getValue()) {
                sinStock.add(producto.getNombre());
            }
        }
        if (!sinStock.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Stock insuficiente para: " + String.join(", ", sinStock));
        }

        long total = 0;
        for (Map.Entry<Integer, Long> entry : cantidades.entrySet()) {
            ProductoEntity producto = productos.get(entry.getKey());
            try {
                total = Math.addExact(total, Math.multiplyExact(producto.getPrecio().longValue(), entry.getValue()));
            } catch (ArithmeticException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El total del pedido excede el límite permitido");
            }
            if (total > Integer.MAX_VALUE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El total del pedido excede el límite permitido");
            }
            producto.setStock((int) (producto.getStock() - entry.getValue()));
        }

        PedidoEntity pedido = new PedidoEntity(usuario, (int) total);
        for (Map.Entry<Integer, Long> entry : cantidades.entrySet()) {
            ProductoEntity producto = productos.get(entry.getKey());
            pedido.addItem(new PedidoItemEntity(producto, Math.toIntExact(entry.getValue()), producto.getPrecio()));
        }

        PedidoEntity guardado = pedidoRepository.saveAndFlush(pedido);
        return new PedidoResponse(guardado.getId(), guardado.getTotal());
    }

    @Transactional(readOnly = true)
    public List<PedidoHistoryResponse> historial(Integer usuarioId) {
        return pedidoRepository.findHistoryByUsuarioId(usuarioId).stream()
                .map(PedidoHistoryResponse::from)
                .toList();
    }
}
