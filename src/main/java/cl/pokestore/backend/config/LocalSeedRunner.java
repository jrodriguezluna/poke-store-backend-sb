package cl.pokestore.backend.config;

import java.util.List;

import cl.pokestore.backend.producto.ProductoEntity;
import cl.pokestore.backend.producto.ProductoRepository;
import cl.pokestore.backend.usuario.UsuarioEntity;
import cl.pokestore.backend.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSeedRunner.class);
    private static final String ADMIN_EMAIL = "admin@pokestore.cl";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String IMAGE_URL =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/%d.png";
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder(12);

    private static final List<ProductoEntity> PRODUCTS = List.of(
            new ProductoEntity("Peluche Pikachu", "electrico", "Peluches", 14990,
                    IMAGE_URL.formatted(25), "Peluche suave de Pikachu, ideal para coleccionistas.", 25),
            new ProductoEntity("Peluche Charmander", "fuego", "Peluches", 13990,
                    IMAGE_URL.formatted(4), "Charmander de felpa con cola brillante.", 18),
            new ProductoEntity("Peluche Squirtle", "agua", "Peluches", 13990,
                    IMAGE_URL.formatted(7), "Squirtle de felpa, perfecto para regalar.", 20),
            new ProductoEntity("Peluche Bulbasaur", "planta", "Peluches", 13990,
                    IMAGE_URL.formatted(1), "Bulbasaur de felpa con bulbo removible.", 15),
            new ProductoEntity("Figura Eevee", "normal", "Coleccionable", 9990,
                    IMAGE_URL.formatted(133), "Figura articulada de Eevee de 12 cm.", 30),
            new ProductoEntity("Figura Charizard", "fuego", "Coleccionable", 24990,
                    IMAGE_URL.formatted(6), "Figura de colección de Charizard de 20 cm.", 10),
            new ProductoEntity("Almohada Snorlax", "normal", "Hogar", 19990,
                    IMAGE_URL.formatted(143), "Almohada gigante de Snorlax para dormir como campeón.", 12),
            new ProductoEntity("Peluche Jigglypuff", "hada", "Peluches", 11990,
                    IMAGE_URL.formatted(39), "Jigglypuff de felpa, versión cantante.", 22)
    );

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public LocalSeedRunner(ProductoRepository productoRepository, UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productoRepository.count() == 0) {
            productoRepository.saveAll(PRODUCTS);
            log.info("Seed local: se insertaron {} productos de demostración", PRODUCTS.size());
        }

        if (!usuarioRepository.existsByEmail(ADMIN_EMAIL)) {
            usuarioRepository.save(new UsuarioEntity(
                    "Administrador",
                    ADMIN_EMAIL,
                    PASSWORD_ENCODER.encode(ADMIN_PASSWORD),
                    null,
                    "Sin dirección",
                    "Metropolitana",
                    "Santiago",
                    "admin"
            ));
            log.info("Seed local: se creó la cuenta de demostración {}", ADMIN_EMAIL);
        }
    }
}
