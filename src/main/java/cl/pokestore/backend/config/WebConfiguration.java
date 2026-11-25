package cl.pokestore.backend.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final PokeStoreProperties properties;

    public WebConfiguration(PokeStoreProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(properties.corsOrigins().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (origins.length == 0) {
            throw new IllegalStateException("CORS_ORIGINS debe contener al menos un origen");
        }

        registry.addMapping("/**")
                .allowedOrigins(origins)
                .allowedMethods("*")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDirectory = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
        String location = uploadDirectory.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }

    @Bean
    ApplicationRunner createUploadDirectory() {
        return args -> {
            Path uploadDirectory = Path.of(properties.uploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDirectory);
        };
    }
}
