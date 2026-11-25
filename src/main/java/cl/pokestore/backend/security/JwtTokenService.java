package cl.pokestore.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import cl.pokestore.backend.config.PokeStoreProperties;
import cl.pokestore.backend.usuario.UsuarioResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

public class JwtTokenService {

    private static final String ISSUER = "poke-store";
    private static final Duration TOKEN_TTL = Duration.ofHours(12);

    private final PokeStoreProperties properties;

    public JwtTokenService(PokeStoreProperties properties) {
        this.properties = properties;
    }

    public String generar(UsuarioResponse usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(usuario.id()))
                .claim("role", usuario.rol())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(TOKEN_TTL)))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = properties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
