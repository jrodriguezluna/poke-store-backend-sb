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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.config.PokeStoreProperties;
import cl.pokestore.backend.security.JwtTokenService;
import cl.pokestore.backend.security.SecurityConfiguration;
import cl.pokestore.backend.usuario.UsuarioResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductoController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = "pokestore.jwt-secret=test-only-jwt-secret-with-at-least-32-bytes")
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokenService;

    @MockitoBean
    private ProductoService service;

    @Test
    void invalidProductReturns422WithFieldName() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(user("admin").roles("ADMIN"))
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
        String token = tokenService.generar(usuario("admin"));

        mockMvc.perform(post("/api/productos")
                        .header("Authorization", "Bearer " + token)
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
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pikachu"));

        verify(service).actualizar(org.mockito.ArgumentMatchers.eq(7), any(ProductoRequest.class));
    }

    @Test
    void deleteProductReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/productos/7").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(service).eliminar(7);
    }

    @Test
    void productWritesRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Autenticación requerida"));

        verifyNoInteractions(service);
    }

    @Test
    void productWritesRequireAdminRole() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(user("7").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permisos para esta operación"));

        verifyNoInteractions(service);
    }

    private ProductoResponse producto() {
        return new ProductoResponse(7, "Pikachu", "Electrico", "Cartas",
                1500, null, "Carta de Pikachu", 10);
    }

    private UsuarioResponse usuario(String rol) {
        return new UsuarioResponse(7, "Ana", "ana@gmail.com", null,
                "Calle 123", "Metropolitana", "Santiago", rol);
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
