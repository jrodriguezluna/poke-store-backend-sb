package cl.pokestore.backend.config;

import java.util.List;

import cl.pokestore.backend.blog.BlogEntity;
import cl.pokestore.backend.blog.BlogRepository;
import cl.pokestore.backend.blog.BlogSeccionEntity;
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

    private static final List<BlogEntity> BLOGS = List.of(
            crearBlog(
                    "Guía de Autenticidad",
                    "Cómo identificar cartas Pokémon graduadas falsas (PSA, Beckett y CGC)",
                    "Aprende a revisar certificaciones, hologramas y estuches para reconocer señales de una carta graduada falsa.",
                    "Entrenador Daniel",
                    "/assets/img/blogs/CartasGradeadas.jpg",
                    "Cartas Pokémon graduadas",
                    "El coleccionismo de cartas del juego de cartas coleccionables Pokémon (TCG) ha alcanzado cotas históricas. Debido a su valor, también han proliferado réplicas que imitan las cartas y los estuches sellados de empresas como PSA, BGS (Beckett) y CGC.",
                    List.of(
                            new BlogSeccionEntity(
                                    "1. Verificación del código de certificación y código QR",
                                    "Cada carta graduada legítima cuenta con un número de serie único emitido por la empresa certificadora. Ingresa el código en el sitio oficial y comprueba que coincidan el año, la edición, el nombre del Pokémon y la calificación. En slabs modernos, revisa también que el QR lleve a la base de datos oficial."
                            ),
                            new BlogSeccionEntity(
                                    "2. Holograma de seguridad y microimpresión",
                                    "Los estuches genuinos incorporan elementos holográficos y microtexto nítido. Las imitaciones pueden usar etiquetas planas o estampados sin efecto tridimensional ni detalles legibles con lupa."
                            ),
                            new BlogSeccionEntity(
                                    "3. Calidad del plástico y sellado",
                                    "Un estuche auténtico debe tener un sellado uniforme. Restos de pegamento, holgura en los bordes o grietas blanquecinas pueden ser señales de manipulación."
                            )
                    ),
                    "Compra a vendedores confiables y verifica siempre el número de certificación en la fuente oficial antes de pagar."
            ),
            crearBlog(
                    "Accesorios y cuidados",
                    "Los mejores accesorios y protectores para conservar tus cartas Pokémon",
                    "Micas, toploaders y carpetas apropiadas ayudan a proteger las cartas del polvo, la humedad y los dobleces.",
                    "Entrenador Jeff",
                    "/assets/img/blogs/AlbumCartas.webp",
                    "Álbum y cartas Pokémon",
                    "El valor de una carta no depende únicamente de su rareza: su estado también importa. Una doblez, un rasguño o bordes desgastados pueden afectar su conservación. Estos accesorios ayudan a proteger la colección en el uso y almacenamiento diario.",
                    List.of(
                            new BlogSeccionEntity(
                                    "1. Doble enfundado: perfect fit y funda estándar",
                                    "Una funda perfect fit ajustada, combinada con una funda estándar, añade una barrera contra polvo, humedad y derrames. Manipula las cartas con las manos limpias y evita forzar las esquinas al enfundarlas."
                            ),
                            new BlogSeccionEntity(
                                    "2. Toploaders y protectores rígidos",
                                    "Para cartas valiosas o que transportas con frecuencia, un protector rígido aporta estructura y reduce el riesgo de dobleces e impactos. Guarda la carta enfundada antes de introducirla."
                            ),
                            new BlogSeccionEntity(
                                    "3. Carpetas con carga lateral",
                                    "Prefiere carpetas con bolsillos cosidos y carga lateral. Evita archivadores cuyos anillos metálicos puedan presionar las cartas y guárdalas lejos de la humedad y la luz solar directa."
                            )
                    ),
                    "Elige materiales de archivo libres de ácido y PVC, y mantén la colección en un lugar seco, fresco y sin exposición solar directa."
            )
    );

    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final BlogRepository blogRepository;

    public LocalSeedRunner(
            ProductoRepository productoRepository,
            UsuarioRepository usuarioRepository,
            BlogRepository blogRepository
    ) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.blogRepository = blogRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (productoRepository.count() == 0) {
            productoRepository.saveAll(PRODUCTS);
            log.info("Seed local: se insertaron {} productos de demostración", PRODUCTS.size());
        }

        if (blogRepository.count() == 0) {
            blogRepository.saveAll(BLOGS);
            log.info("Seed local: se insertaron {} artículos de demostración", BLOGS.size());
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

    private static BlogEntity crearBlog(
            String etiqueta,
            String titulo,
            String resumen,
            String autor,
            String imagen,
            String textoAlternativo,
            String introduccion,
            List<BlogSeccionEntity> secciones,
            String consejo
    ) {
        BlogEntity blog = new BlogEntity(
                etiqueta,
                titulo,
                resumen,
                autor,
                imagen,
                textoAlternativo,
                introduccion,
                consejo
        );
        blog.reemplazarSecciones(secciones);
        return blog;
    }
}
