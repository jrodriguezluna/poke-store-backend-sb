package cl.pokestore.backend.blog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
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

@WebMvcTest(BlogController.class)
@EnableConfigurationProperties(PokeStoreProperties.class)
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlogService service;

    @Test
    void listsBlogContentAndOrderedSections() throws Exception {
        when(service.listar()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Guía de cartas"))
                .andExpect(jsonPath("$[0].textoAlternativo").value("Cartas"))
                .andExpect(jsonPath("$[0].secciones[0].orden").value(0))
                .andExpect(jsonPath("$[0].secciones[0].titulo").value("Verificación"));
    }

    @Test
    void invalidBlogRequestReturns422AndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "etiqueta": "",
                                  "titulo": "",
                                  "resumen": "",
                                  "autor": "",
                                  "introduccion": "",
                                  "secciones": []
                                }
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.error").value(allOf(
                        containsString("etiqueta"),
                        containsString("titulo"),
                        containsString("resumen"),
                        containsString("autor"),
                        containsString("introduccion"),
                        containsString("secciones")
                )));

        verifyNoInteractions(service);
    }

    @Test
    void missingBlogUsesTheSharedErrorContract() throws Exception {
        when(service.obtener(999)).thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Blog no encontrado"));

        mockMvc.perform(get("/api/blogs/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Blog no encontrado"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/blogs/7"))
                .andExpect(status().isNoContent());

        verify(service).eliminar(7);
    }

    @Test
    void createsBlogWithItsSections() throws Exception {
        when(service.crear(any(BlogRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.secciones[0].texto").value("Revisa el código."));
    }

    @Test
    void updatesBlogWithItsSections() throws Exception {
        when(service.actualizar(org.mockito.ArgumentMatchers.eq(4), any(BlogRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/blogs/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Guía de cartas"))
                .andExpect(jsonPath("$.secciones[0].titulo").value("Verificación"));

        verify(service).actualizar(org.mockito.ArgumentMatchers.eq(4), any(BlogRequest.class));
    }

    private BlogResponse response() {
        return new BlogResponse(
                4,
                "Guía",
                "Guía de cartas",
                "Resumen",
                "Entrenador",
                "/assets/cartas.jpg",
                "Cartas",
                "Introducción",
                List.of(new BlogSeccionResponse(8, 0, "Verificación", "Revisa el código.")),
                "Consejo",
                null,
                null
        );
    }

    private String validRequest() {
        return """
                {
                  "etiqueta": "Guía",
                  "titulo": "Guía de cartas",
                  "resumen": "Resumen",
                  "autor": "Entrenador",
                  "imagen": "/assets/cartas.jpg",
                  "textoAlternativo": "Cartas",
                  "introduccion": "Introducción",
                  "consejo": "Consejo",
                  "secciones": [
                    { "titulo": "Verificación", "texto": "Revisa el código." }
                  ]
                }
                """;
    }
}
