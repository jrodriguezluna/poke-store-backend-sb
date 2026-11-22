package cl.pokestore.backend.blog;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogService {

    private final BlogRepository repository;

    public BlogService(BlogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BlogResponse> listar() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(BlogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlogResponse obtener(Integer id) {
        return BlogResponse.from(buscar(id));
    }

    @Transactional
    public BlogResponse crear(BlogRequest request) {
        BlogEntity blog = new BlogEntity(
                limpiar(request.etiqueta()),
                limpiar(request.titulo()),
                limpiar(request.resumen()),
                limpiar(request.autor()),
                normalizarOpcional(request.imagen()),
                normalizarOpcional(request.textoAlternativo()),
                limpiar(request.introduccion()),
                normalizarOpcional(request.consejo())
        );
        blog.reemplazarSecciones(mapearSecciones(request.secciones()));
        return BlogResponse.from(repository.saveAndFlush(blog));
    }

    @Transactional
    public BlogResponse actualizar(Integer id, BlogRequest request) {
        BlogEntity blog = buscar(id);
        blog.actualizar(
                limpiar(request.etiqueta()),
                limpiar(request.titulo()),
                limpiar(request.resumen()),
                limpiar(request.autor()),
                normalizarOpcional(request.imagen()),
                normalizarOpcional(request.textoAlternativo()),
                limpiar(request.introduccion()),
                normalizarOpcional(request.consejo())
        );
        blog.reemplazarSecciones(mapearSecciones(request.secciones()));
        return BlogResponse.from(repository.saveAndFlush(blog));
    }

    @Transactional
    public void eliminar(Integer id) {
        repository.delete(buscar(id));
    }

    private BlogEntity buscar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Blog no encontrado"));
    }

    private List<BlogSeccionEntity> mapearSecciones(List<BlogSeccionRequest> secciones) {
        return secciones.stream()
                .map(seccion -> new BlogSeccionEntity(limpiar(seccion.titulo()), limpiar(seccion.texto())))
                .toList();
    }

    private String limpiar(String valor) {
        return valor.strip();
    }

    private String normalizarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.strip();
        return limpio.isEmpty() ? null : limpio;
    }
}
