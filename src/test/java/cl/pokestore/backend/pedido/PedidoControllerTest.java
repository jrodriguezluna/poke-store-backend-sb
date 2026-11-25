package cl.pokestore.backend.pedido;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import cl.pokestore.backend.config.PokeStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PedidoController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;

    @Test
    void createsOrderAndReturnsItsTotal() throws Exception {
        when(service.crear(any(PedidoRequest.class))).thenReturn(new PedidoResponse(21, 3000));

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":7,"items":[{"productoId":3,"cantidad":2}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.total").value(3000));

        verify(service).crear(any(PedidoRequest.class));
    }

    @Test
    void rejectsOrderWithNonPositiveItemQuantity() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":7,"items":[{"productoId":3,"cantidad":0}]}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Datos inválidos: items.0.cantidad"));

        verifyNoInteractions(service);
    }

    @Test
    void returnsOrderHistoryForUser() throws Exception {
        when(service.historial(7)).thenReturn(List.of(new PedidoHistoryResponse(
                21,
                3000,
                LocalDateTime.of(2026, 1, 2, 10, 30),
                List.of(new ItemHistoryResponse("Pikachu", 2, 1500))
        )));

        mockMvc.perform(get("/api/pedidos").param("usuarioId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].total").value(3000))
                .andExpect(jsonPath("$[0].items[0].nombre").value("Pikachu"))
                .andExpect(jsonPath("$[0].items[0].cantidad").value(2));

        verify(service).historial(7);
    }

    @Test
    void requiresUserIdForOrderHistory() throws Exception {
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Datos inválidos: usuarioId"));

        verifyNoInteractions(service);
    }
}
