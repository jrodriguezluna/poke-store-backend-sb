package cl.pokestore.backend.pedido;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<PedidoEntity, Integer> {

    @Query("""
            select distinct p from PedidoEntity p
            left join fetch p.items item
            left join fetch item.producto
            where p.usuario.id = :usuarioId
            order by p.id desc
            """)
    List<PedidoEntity> findHistoryByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
