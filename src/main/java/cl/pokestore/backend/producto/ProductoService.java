package cl.pokestore.backend.producto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.config.PokeStoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository repository;
    private final PokeStoreProperties properties;

    public ProductoService(ProductoRepository repository, PokeStoreProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return repository.findByActivoTrueOrderByIdAsc().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Integer id) {
        return ProductoResponse.from(obtenerActivo(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        ProductoEntity producto = new ProductoEntity(
                request.nombre().strip(),
                request.categoria(),
                request.precio(),
                validarImagen(request.imagen()),
                request.descripcion(),
                request.stock() == null ? 0 : request.stock()
        );
        return ProductoResponse.from(repository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Integer id, ProductoRequest request) {
        ProductoEntity producto = obtenerActivo(id);
        String imagen = validarImagen(request.imagen());
        String imagenAnterior = producto.getImagen();

        producto.setNombre(request.nombre().strip());
        producto.setCategoria(request.categoria());
        producto.setPrecio(request.precio());
        producto.setDescripcion(request.descripcion());
        producto.setStock(request.stock() == null ? 0 : request.stock());
        producto.setImagen(imagen);

        ProductoEntity actualizado = repository.save(producto);
        if (!Objects.equals(imagenAnterior, imagen)) {
            borrarImagenLocal(imagenAnterior);
        }
        return ProductoResponse.from(actualizado);
    }

    @Transactional
    public void eliminar(Integer id) {
        ProductoEntity producto = obtenerActivo(id);
        producto.setActivo(false);
    }

    private ProductoEntity obtenerActivo(Integer id) {
        return repository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private String validarImagen(String value) {
        String imagen = value == null ? null : value.strip();
        if (imagen == null || imagen.isEmpty()) {
            return null;
        }
        if (!imagen.startsWith("http://") && !imagen.startsWith("https://")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La URL de la imagen debe comenzar con http:// o https://"
            );
        }
        return imagen;
    }

    private void borrarImagenLocal(String ruta) {
        if (ruta == null || !ruta.startsWith("/uploads/")) {
            return;
        }

        Path directorio = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        Path nombre = Path.of(ruta).getFileName();
        if (nombre == null) {
            return;
        }
        Path archivo = directorio.resolve(nombre).normalize();
        if (!archivo.startsWith(directorio)) {
            return;
        }

        try {
            Files.deleteIfExists(archivo);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo eliminar la imagen local");
        }
    }
}
