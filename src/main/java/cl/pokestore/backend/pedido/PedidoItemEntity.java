package cl.pokestore.backend.pedido;

import cl.pokestore.backend.producto.ProductoEntity;
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
@Table(name = "pedido_items")
public class PedidoItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private ProductoEntity producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unit", nullable = false)
    private Integer precioUnit;

    protected PedidoItemEntity() {
    }

    public PedidoItemEntity(ProductoEntity producto, Integer cantidad, Integer precioUnit) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnit = precioUnit;
    }

    void setPedido(PedidoEntity pedido) {
        this.pedido = pedido;
    }

    public Integer getId() {
        return id;
    }

    public PedidoEntity getPedido() {
        return pedido;
    }

    public ProductoEntity getProducto() {
        return producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public Integer getPrecioUnit() {
        return precioUnit;
    }
}
