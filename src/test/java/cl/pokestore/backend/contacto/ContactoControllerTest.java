package cl.pokestore.backend.contacto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.pokestore.backend.config.PokeStoreProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import cl.pokestore.backend.security.SecurityConfiguration;

@WebMvcTest(ContactoController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
@Import(SecurityConfiguration.class)
class ContactoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactoService service;

    @Test
    void acceptsValidContactMessage() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","email":"ana@gmail.com","mensaje":"Quiero consultar por un producto."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true));

        verify(service).enviar(any(ContactoRequest.class));
    }

    @Test
    void rejectsInvalidContactEmailWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/contacto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","email":"no-es-email","mensaje":"Consulta"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.error").value("Datos inválidos: email"));

        verifyNoInteractions(service);
    }
}
