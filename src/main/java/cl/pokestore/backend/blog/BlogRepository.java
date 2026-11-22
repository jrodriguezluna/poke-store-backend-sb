package cl.pokestore.backend.blog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<BlogEntity, Integer> {

    List<BlogEntity> findAllByOrderByIdAsc();
}
