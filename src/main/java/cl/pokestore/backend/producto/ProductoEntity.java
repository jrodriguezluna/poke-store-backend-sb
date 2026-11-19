package cl.pokestore.backend.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "productos")
public class ProductoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 30)
    private String tipo;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    private Integer precio;

    @Column(length = 255)
    private String imagen;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(nullable = false)
    private boolean activo = true;

    protected ProductoEntity() {
    }

    public ProductoEntity(
            String nombre,
            String categoria,
            Integer precio,
            String imagen,
            String descripcion,
            Integer stock
    ) {
        this(nombre, null, categoria, precio, imagen, descripcion, stock);
    }

    public ProductoEntity(
            String nombre,
            String tipo,
            String categoria,
            Integer precio,
            String imagen,
            String descripcion,
            Integer stock
    ) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.precio = precio;
        this.imagen = imagen;
        this.descripcion = descripcion;
        this.stock = stock;
        this.activo = true;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Integer getPrecio() {
        return precio;
    }

    public void setPrecio(Integer precio) {
        this.precio = precio;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
