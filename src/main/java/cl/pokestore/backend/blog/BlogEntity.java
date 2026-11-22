package cl.pokestore.backend.blog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "blogs")
public class BlogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String etiqueta;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String resumen;

    @Column(nullable = false, length = 100)
    private String autor;

    @Column(length = 500)
    private String imagen;

    @Column(name = "texto_alternativo", length = 255)
    private String textoAlternativo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String introduccion;

    @Column(columnDefinition = "TEXT")
    private String consejo;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC, id ASC")
    private List<BlogSeccionEntity> secciones = new ArrayList<>();

    protected BlogEntity() {
    }

    public BlogEntity(
            String etiqueta,
            String titulo,
            String resumen,
            String autor,
            String imagen,
            String textoAlternativo,
            String introduccion,
            String consejo
    ) {
        this.etiqueta = etiqueta;
        this.titulo = titulo;
        this.resumen = resumen;
        this.autor = autor;
        this.imagen = imagen;
        this.textoAlternativo = textoAlternativo;
        this.introduccion = introduccion;
        this.consejo = consejo;
    }

    public void actualizar(
            String etiqueta,
            String titulo,
            String resumen,
            String autor,
            String imagen,
            String textoAlternativo,
            String introduccion,
            String consejo
    ) {
        this.etiqueta = etiqueta;
        this.titulo = titulo;
        this.resumen = resumen;
        this.autor = autor;
        this.imagen = imagen;
        this.textoAlternativo = textoAlternativo;
        this.introduccion = introduccion;
        this.consejo = consejo;
    }

    public void reemplazarSecciones(List<BlogSeccionEntity> nuevasSecciones) {
        secciones.clear();
        for (int indice = 0; indice < nuevasSecciones.size(); indice++) {
            BlogSeccionEntity seccion = nuevasSecciones.get(indice);
            seccion.asociar(this, indice);
            secciones.add(seccion);
        }
    }

    public Integer getId() {
        return id;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getResumen() {
        return resumen;
    }

    public String getAutor() {
        return autor;
    }

    public String getImagen() {
        return imagen;
    }

    public String getTextoAlternativo() {
        return textoAlternativo;
    }

    public String getIntroduccion() {
        return introduccion;
    }

    public String getConsejo() {
        return consejo;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }

    public List<BlogSeccionEntity> getSecciones() {
        return secciones;
    }
}
