package cl.pokestore.backend.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.producto.ProductoEntity;
import cl.pokestore.backend.producto.ProductoRepository;
import cl.pokestore.backend.usuario.UsuarioEntity;
import cl.pokestore.backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PedidoService service;

    private UsuarioEntity usuario;
    private ProductoEntity producto;

    @BeforeEach
    void setUp() {
        usuario = org.mockito.Mockito.mock(UsuarioEntity.class);
        producto = org.mockito.Mockito.mock(ProductoEntity.class);
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario));
        when(producto.getId()).thenReturn(10);
        when(productoRepository.findActiveForUpdate(any())).thenReturn(List.of(producto));
    }

    @Test
    void repeatedProductLinesAreCombinedIntoOneOrderItem() {
        when(producto.getStock()).thenReturn(5);
        when(producto.getPrecio()).thenReturn(1500);
        when(pedidoRepository.saveAndFlush(any(PedidoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PedidoRequest request = new PedidoRequest(7, List.of(
                new ItemRequest(10, 1),
                new ItemRequest(10, 2)
        ));

        PedidoResponse response = service.crear(request);

        assertThat(response.total()).isEqualTo(4500);
        ArgumentCaptor<PedidoEntity> pedidoCaptor = ArgumentCaptor.forClass(PedidoEntity.class);
        verify(pedidoRepository).saveAndFlush(pedidoCaptor.capture());
        assertThat(pedidoCaptor.getValue().getItems()).hasSize(1);
        assertThat(pedidoCaptor.getValue().getItems().getFirst().getCantidad()).isEqualTo(3);
        verify(producto).setStock(2);
    }

    @Test
    void insufficientStockDoesNotChangeStockOrSaveAnOrder() {
        when(producto.getStock()).thenReturn(1);
        when(producto.getNombre()).thenReturn("Figura Eevee");

        assertThatThrownBy(() -> service.crear(
                new PedidoRequest(7, List.of(new ItemRequest(10, 2)))
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(producto, never()).setStock(any());
        verify(pedidoRepository, never()).saveAndFlush(any());
    }
}
