package cl.pokestore.backend.blog;

import java.time.LocalDateTime;
import java.util.List;

public record BlogResponse(
        Integer id,
        String etiqueta,
        String titulo,
        String resumen,
        String autor,
        String imagen,
        String textoAlternativo,
        String introduccion,
        List<BlogSeccionResponse> secciones,
        String consejo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {

    static BlogResponse from(BlogEntity blog) {
        return new BlogResponse(
                blog.getId(),
                blog.getEtiqueta(),
                blog.getTitulo(),
                blog.getResumen(),
                blog.getAutor(),
                blog.getImagen(),
                blog.getTextoAlternativo(),
                blog.getIntroduccion(),
                blog.getSecciones().stream().map(BlogSeccionResponse::from).toList(),
                blog.getConsejo(),
                blog.getCreadoEn(),
                blog.getActualizadoEn()
        );
    }
}
