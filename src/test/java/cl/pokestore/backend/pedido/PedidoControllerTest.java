package cl.pokestore.backend.pedido;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.LocalDateTime;
import java.util.List;

import cl.pokestore.backend.config.PokeStoreProperties;
import cl.pokestore.backend.security.SecurityConfiguration;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PedidoController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
@Import(SecurityConfiguration.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;

    @Test
    void createsOrderAndReturnsItsTotal() throws Exception {
        when(service.crear(any(PedidoRequest.class))).thenReturn(new PedidoResponse(21, 3000));

        mockMvc.perform(post("/api/pedidos")
                        .with(user("7").roles("CLIENTE"))
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
                        .with(user("7").roles("CLIENTE"))
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

        mockMvc.perform(get("/api/pedidos").with(user("7")).param("usuarioId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].total").value(3000))
                .andExpect(jsonPath("$[0].items[0].nombre").value("Pikachu"))
                .andExpect(jsonPath("$[0].items[0].cantidad").value(2));

        verify(service).historial(7);
    }

    @Test
    void requiresUserIdForOrderHistory() throws Exception {
        mockMvc.perform(get("/api/pedidos").with(user("7")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Datos inválidos: usuarioId"));

        verifyNoInteractions(service);
    }

    @Test
    void sellersCanListAllOrdersWithoutAUserId() throws Exception {
        when(service.historialTodos()).thenReturn(List.of(new PedidoHistoryResponse(
                21,
                3000,
                LocalDateTime.of(2026, 1, 2, 10, 30),
                List.of(new ItemHistoryResponse("Pikachu", 2, 1500))
        )));

        mockMvc.perform(get("/api/pedidos").with(user("9").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].items[0].nombre").value("Pikachu"));

        verify(service).historialTodos();
    }

    @Test
    void sellersCanReadAnOrderDetail() throws Exception {
        when(service.detalle(21, null)).thenReturn(new PedidoHistoryResponse(
                21,
                3000,
                LocalDateTime.of(2026, 1, 2, 10, 30),
                List.of(new ItemHistoryResponse("Pikachu", 2, 1500))
        ));

        mockMvc.perform(get("/api/pedidos/21").with(user("9").roles("VENDEDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.items[0].cantidad").value(2));

        verify(service).detalle(21, null);
    }

    @Test
    void sellersCannotCreateOrders() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user("9").roles("VENDEDOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":9,"items":[{"productoId":3,"cantidad":1}]}
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void usersCannotPlaceOrdersForAnotherAccount() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .with(user("7").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"usuarioId":8,"items":[{"productoId":3,"cantidad":1}]}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No puedes acceder a pedidos de otra cuenta"));

        verifyNoInteractions(service);
    }

    @Test
    void usersCannotReadAnotherAccountsOrderHistory() throws Exception {
        mockMvc.perform(get("/api/pedidos").with(user("8")).param("usuarioId", "7"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No puedes acceder a pedidos de otra cuenta"));

        verifyNoInteractions(service);
    }
}
