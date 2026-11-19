package cl.pokestore.backend.producto;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<ProductoEntity, Integer> {

    List<ProductoEntity> findByActivoTrueOrderByIdAsc();

    Optional<ProductoEntity> findByIdAndActivoTrue(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProductoEntity p where p.id in :ids and p.activo = true order by p.id")
    List<ProductoEntity> findActiveForUpdate(@Param("ids") Collection<Integer> ids);
}
