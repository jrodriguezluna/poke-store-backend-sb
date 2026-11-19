package cl.pokestore.backend.producto;

public record ProductoResponse(
        Integer id,
        String nombre,
        String tipo,
        String categoria,
        Integer precio,
        String imagen,
        String descripcion,
        Integer stock
) {

    public static ProductoResponse from(ProductoEntity producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getTipo(),
                producto.getCategoria(),
                producto.getPrecio(),
                producto.getImagen(),
                producto.getDescripcion(),
                producto.getStock()
        );
    }
}
