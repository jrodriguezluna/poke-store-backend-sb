package cl.pokestore.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import cl.pokestore.backend.producto.ProductoEntity;
import cl.pokestore.backend.producto.ProductoRepository;
import cl.pokestore.backend.usuario.UsuarioEntity;
import cl.pokestore.backend.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalSeedRunnerTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationArguments arguments;

    @Test
    void seedsProductsAndDemoAdminOnlyWhenMissing() {
        when(productoRepository.count()).thenReturn(0L);
        when(usuarioRepository.existsByEmail("admin@pokestore.cl")).thenReturn(false);
        LocalSeedRunner runner = new LocalSeedRunner(productoRepository, usuarioRepository);

        runner.run(arguments);

        ArgumentCaptor<UsuarioEntity> userCaptor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(productoRepository).saveAll(anyList());
        verify(usuarioRepository).save(userCaptor.capture());
        UsuarioEntity admin = userCaptor.getValue();
        assertThat(admin.getEmail()).isEqualTo("admin@pokestore.cl");
        assertThat(new BCryptPasswordEncoder(12).matches("admin123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void doesNotOverwriteExistingLocalData() {
        when(productoRepository.count()).thenReturn(1L);
        when(usuarioRepository.existsByEmail("admin@pokestore.cl")).thenReturn(true);
        LocalSeedRunner runner = new LocalSeedRunner(productoRepository, usuarioRepository);

        runner.run(arguments);

        verify(productoRepository, never()).saveAll(anyList());
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }
}
