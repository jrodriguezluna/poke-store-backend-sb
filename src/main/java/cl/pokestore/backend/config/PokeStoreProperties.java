package cl.pokestore.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pokestore")
public record PokeStoreProperties(
        String uploadDir,
        String corsOrigins,
        String googleClientId
) {
}
