package cl.pokestore.backend.blog;

public record BlogSeccionResponse(
        Integer id,
        Integer orden,
        String titulo,
        String texto
) {

    static BlogSeccionResponse from(BlogSeccionEntity seccion) {
        return new BlogSeccionResponse(
                seccion.getId(),
                seccion.getOrden(),
                seccion.getTitulo(),
                seccion.getTexto()
        );
    }
}
