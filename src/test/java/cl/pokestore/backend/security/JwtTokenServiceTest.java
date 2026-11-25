package cl.pokestore.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.pokestore.backend.config.PokeStoreProperties;
import cl.pokestore.backend.usuario.UsuarioResponse;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private static final String SECRET = "test-only-jwt-secret-with-at-least-32-bytes";

    private final JwtTokenService service = new JwtTokenService(
            new PokeStoreProperties("uploads", "http://localhost:5173", "", SECRET)
    );

    @Test
    void generatedTokenContainsUserIdRoleAndTwelveHourExpiry() {
        String token = service.generar(usuario("admin"));

        var claims = service.validar(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("role", String.class)).isEqualTo("admin");
        assertThat(claims.getIssuer()).isEqualTo("poke-store");
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
        assertThat(claims.getExpiration().getTime())
                .isBetween(
                        System.currentTimeMillis() + 11 * 60 * 60 * 1000,
                        System.currentTimeMillis() + 12 * 60 * 60 * 1000 + 1000
                );
    }

    @Test
    void rejectsTokenWhoseSignatureWasChanged() {
        String token = service.generar(usuario("cliente"));
        String[] parts = token.split("\\.");
        String changedSignature = (parts[2].startsWith("a") ? "b" : "a") + parts[2].substring(1);

        assertThatThrownBy(() -> service.validar(parts[0] + "." + parts[1] + "." + changedSignature))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void requiresASecretOfAtLeastThirtyTwoBytes() {
        JwtTokenService sinSecreto = new JwtTokenService(
                new PokeStoreProperties("uploads", "http://localhost:5173", "", "short")
        );

        assertThatThrownBy(() -> sinSecreto.generar(usuario("cliente")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    private UsuarioResponse usuario(String rol) {
        return new UsuarioResponse(7, "Ana", "ana@gmail.com", null,
                "Calle 123", "Metropolitana", "Santiago", rol);
    }
}
