package cl.pokestore.backend.producto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.config.PokeStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductoController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService service;

    @Test
    void invalidProductReturns422WithFieldName() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Producto",
                                  "categoria": "Juguetes",
                                  "precio": -1,
                                  "stock": 0
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Datos inválidos: precio"));

        verifyNoInteractions(service);
    }

    @Test
    void missingProductUsesTheSharedErrorContract() throws Exception {
        when(service.obtener(999))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

        mockMvc.perform(get("/api/productos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Producto no encontrado"));

        verify(service).obtener(999);
    }

    @Test
    void listsProducts() throws Exception {
        when(service.listar()).thenReturn(List.of(producto()));

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nombre").value("Pikachu"));
    }

    @Test
    void createsProduct() throws Exception {
        when(service.crear(any(ProductoRequest.class))).thenReturn(producto());

        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.precio").value(1500));

        verify(service).crear(any(ProductoRequest.class));
    }

    @Test
    void updatesProduct() throws Exception {
        when(service.actualizar(org.mockito.ArgumentMatchers.eq(7), any(ProductoRequest.class)))
                .thenReturn(producto());

        mockMvc.perform(put("/api/productos/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pikachu"));

        verify(service).actualizar(org.mockito.ArgumentMatchers.eq(7), any(ProductoRequest.class));
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/productos/7"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(7);
    }

    private ProductoResponse producto() {
        return new ProductoResponse(7, "Pikachu", "Electrico", "Cartas",
                1500, null, "Carta de Pikachu", 10);
    }

    private String validRequest() {
        return """
                {
                  "nombre":"Pikachu",
                  "categoria":"Cartas",
                  "precio":1500,
                  "stock":10,
                  "descripcion":"Carta de Pikachu"
                }
                """;
    }
}
