package cl.pokestore.backend.blog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "blog_secciones")
public class BlogSeccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blog_id", nullable = false)
    private BlogEntity blog;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    protected BlogSeccionEntity() {
    }

    public BlogSeccionEntity(String titulo, String texto) {
        this.titulo = titulo;
        this.texto = texto;
    }

    void asociar(BlogEntity blog, int orden) {
        this.blog = blog;
        this.orden = orden;
    }

    public Integer getId() {
        return id;
    }

    public Integer getOrden() {
        return orden;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getTexto() {
        return texto;
    }
}
