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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.config.PokeStoreProperties;
import cl.pokestore.backend.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

@WebMvcTest(UsuarioController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = "pokestore.jwt-secret=test-only-jwt-secret-with-at-least-32-bytes")
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
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.token").isNotEmpty());

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
                .andExpect(jsonPath("$.email").value("ana@gmail.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void listsAndUpdatesUsers() throws Exception {
        when(service.listar()).thenReturn(List.of(usuario()));
        when(service.modificar(eq(7), any(UsuarioUpdateRequest.class), eq(true)))
                .thenReturn(vendedor());

        mockMvc.perform(get("/api/usuarios").with(user("7").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));

        mockMvc.perform(put("/api/usuarios/7")
                        .with(user("7").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "email":"ana@gmail.com",
                                  "direccion":"Calle 123",
                                  "region":"Metropolitana",
                                  "comuna":"Santiago",
                                  "rol":"vendedor"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.rol").value("vendedor"));

        verify(service).modificar(eq(7), any(UsuarioUpdateRequest.class), eq(true));
    }

    @Test
    void usersCanUpdateTheirOwnProfileWithoutChangingTheirRole() throws Exception {
        when(service.modificar(eq(7), any(UsuarioUpdateRequest.class), eq(false)))
                .thenReturn(usuario());

        mockMvc.perform(put("/api/usuarios/7")
                        .with(user("7").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "email":"ana@gmail.com",
                                  "direccion":"Calle 123",
                                  "region":"Metropolitana",
                                  "comuna":"Santiago",
                                  "rol":"admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("cliente"));

        verify(service).modificar(eq(7), any(UsuarioUpdateRequest.class), eq(false));
    }

    @Test
    void usersCannotUpdateAnotherProfile() throws Exception {
        mockMvc.perform(put("/api/usuarios/7")
                        .with(user("8").roles("CLIENTE"))
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No puedes modificar otra cuenta"));

        verifyNoInteractions(service);
    }

    @Test
    void onlySupportedRolesCanBeAssigned() throws Exception {
        mockMvc.perform(put("/api/usuarios/7")
                        .with(user("7").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Ana",
                                  "email":"ana@gmail.com",
                                  "direccion":"Calle 123",
                                  "region":"Metropolitana",
                                  "comuna":"Santiago",
                                  "rol":"superadmin"
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value("Datos inválidos: rol"));

        verifyNoInteractions(service);
    }

    @Test
    void deleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7").with(user("7").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(service).eliminar(7);
    }

    private UsuarioResponse usuario() {
        return new UsuarioResponse(7, "Ana", "ana@gmail.com", null,
                "Calle 123", "Metropolitana", "Santiago", "cliente");
    }

    private UsuarioResponse vendedor() {
        return new UsuarioResponse(7, "Ana", "ana@gmail.com", null,
                "Calle 123", "Metropolitana", "Santiago", "vendedor");
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
