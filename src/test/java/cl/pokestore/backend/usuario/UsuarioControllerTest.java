package cl.pokestore.backend.usuario;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(UsuarioController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService service;

    @Test
    void registrationReturnsCreatedUser() throws Exception {
        when(service.registrar(any(RegistroRequest.class))).thenReturn(usuario());

        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ana@gmail.com"))
                .andExpect(jsonPath("$.rol").value("cliente"));
    }

    @Test
    void loginReturnsUserAndInvalidCredentialsKeepTheApiErrorContract() throws Exception {
        when(service.login(any(LoginRequest.class))).thenReturn(usuario());

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@gmail.com","password":"secreto1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));

        when(service.login(any(LoginRequest.class)))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@gmail.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    void invalidRegistrationReturns422WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"no-es-email","direccion":"","region":"","comuna":"","password":"123"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(service);
    }

    @Test
    void googleLoginReturnsUser() throws Exception {
        when(service.loginGoogle(any(GoogleLoginRequest.class))).thenReturn(usuario());

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"credential":"token-de-prueba"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@gmail.com"));
    }

    @Test
    void listsAndUpdatesUsers() throws Exception {
        when(service.listar()).thenReturn(List.of(usuario()));
        when(service.modificar(eq(7), any(UsuarioUpdateRequest.class)))
                .thenReturn(usuario());

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "email":"ana@gmail.com",
                                  "direccion":"Calle 123",
                                  "region":"Metropolitana",
                                  "comuna":"Santiago"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"));

        verify(service).modificar(eq(7), any(UsuarioUpdateRequest.class));
    }

    @Test
    void deleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(7);
    }

    private UsuarioResponse usuario() {
        return new UsuarioResponse(7, "Ana", "ana@gmail.com", null,
                "Calle 123", "Metropolitana", "Santiago", "cliente");
    }

    private String registrationRequest() {
        return """
                {
                  "nombre":"Ana",
                  "email":"ana@gmail.com",
                  "direccion":"Calle 123",
                  "region":"Metropolitana",
                  "comuna":"Santiago",
                  "password":"secreto1"
                }
                """;
    }
}
