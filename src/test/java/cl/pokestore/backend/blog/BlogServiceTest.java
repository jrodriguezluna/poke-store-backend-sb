package cl.pokestore.backend.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import cl.pokestore.backend.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository repository;

    @InjectMocks
    private BlogService service;

    @Test
    void createTrimsContentAndPreservesSectionOrder() {
        when(repository.saveAndFlush(any(BlogEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BlogRequest request = new BlogRequest(
                "  Guía  ",
                "  Cartas  ",
                "  Resumen  ",
                "  Autor  ",
                "  /assets/cartas.jpg  ",
                "  Cartas  ",
                "  Introducción  ",
                "  Consejo  ",
                List.of(
                        new BlogSeccionRequest(" Primero ", " Texto uno "),
                        new BlogSeccionRequest(" Segundo ", " Texto dos ")
                )
        );

        BlogResponse response = service.crear(request);

        assertThat(response.etiqueta()).isEqualTo("Guía");
        assertThat(response.imagen()).isEqualTo("/assets/cartas.jpg");
        assertThat(response.secciones()).extracting(BlogSeccionResponse::orden).containsExactly(0, 1);
        assertThat(response.secciones()).extracting(BlogSeccionResponse::titulo)
                .containsExactly("Primero", "Segundo");
        verify(repository).saveAndFlush(any(BlogEntity.class));
    }

    @Test
    void missingBlogReturnsNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
