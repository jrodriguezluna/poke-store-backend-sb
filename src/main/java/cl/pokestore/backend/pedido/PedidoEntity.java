package cl.pokestore.backend.pedido;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import cl.pokestore.backend.usuario.UsuarioEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(nullable = false)
    private Integer total;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoItemEntity> items = new ArrayList<>();

    protected PedidoEntity() {
    }

    public PedidoEntity(UsuarioEntity usuario, Integer total) {
        this.usuario = usuario;
        this.total = total;
    }

    public void addItem(PedidoItemEntity item) {
        items.add(item);
        item.setPedido(this);
    }

    public Integer getId() {
        return id;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public Integer getTotal() {
        return total;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<PedidoItemEntity> getItems() {
        return items;
    }
}
