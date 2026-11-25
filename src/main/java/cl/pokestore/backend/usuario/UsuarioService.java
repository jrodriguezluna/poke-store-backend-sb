package cl.pokestore.backend.usuario;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.config.PokeStoreProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private static final Set<String> DOMINIOS_PERMITIDOS =
            Set.of("gmail.com", "outlook.com", "duoc.cl", "duocuc.cl");

    private final UsuarioRepository repository;
    private final PokeStoreProperties properties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();

    public UsuarioService(UsuarioRepository repository, PokeStoreProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public UsuarioResponse registrar(RegistroRequest request) {
        String email = normalizeEmail(request.email());
        validarDominioRegistro(email);
        if (repository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }

        UsuarioEntity usuario = new UsuarioEntity(
                request.nombre().strip(),
                email,
                passwordEncoder.encode(request.password()),
                normalizarTelefono(request.telefono()),
                request.direccion().strip(),
                request.region(),
                request.comuna(),
                "cliente"
        );
        try {
            return UsuarioResponse.from(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponse login(LoginRequest request) {
        UsuarioEntity usuario = repository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (usuario == null || !passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        return UsuarioResponse.from(usuario);
    }

    public UsuarioResponse loginGoogle(GoogleLoginRequest request) {
        String clientId = properties.googleClientId();
        if (clientId == null || clientId.isBlank()) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Login con Google no configurado: falta GOOGLE_CLIENT_ID en el .env del backend"
            );
        }

        GoogleIdToken token = parseGoogleToken(request.credential());
        GoogleIdToken.Payload payload = verificarGoogleToken(token, clientId);
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "El correo de Google no está verificado");
        }

        String email = normalizeEmail(payload.getEmail());
        if (!dominioPermitido(email)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Solo se permiten correos @gmail.com, @outlook.com o @duoc.cl"
            );
        }

        UsuarioEntity existente = repository.findByEmail(email).orElse(null);
        if (existente != null) {
            return UsuarioResponse.from(existente);
        }

        Object nameClaim = payload.get("name");
        String nombre = nameClaim instanceof String value ? value.strip() : "";
        if (nombre.isEmpty()) {
            nombre = email.substring(0, email.indexOf('@'));
        }
        if (nombre.length() > 100) {
            nombre = nombre.substring(0, 100);
        }

        UsuarioEntity nuevo = new UsuarioEntity(
                nombre,
                email,
                passwordEncoder.encode(randomPassword()),
                null,
                "Sin dirección",
                "Sin región",
                "Sin comuna",
                "cliente"
        );
        try {
            return UsuarioResponse.from(repository.saveAndFlush(nuevo));
        } catch (DataIntegrityViolationException exception) {
            return repository.findByEmail(email)
                    .map(UsuarioResponse::from)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.CONFLICT,
                            "No se pudo crear la cuenta con Google"
                    ));
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, UsuarioEntity::getId)).stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional
    public UsuarioResponse modificar(Integer id, UsuarioUpdateRequest request, boolean puedeCambiarRol) {
        UsuarioEntity usuario = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        usuario.setNombre(request.nombre().strip());
        usuario.setEmail(normalizeEmail(request.email()));
        usuario.setTelefono(normalizarTelefono(request.telefono()));
        usuario.setDireccion(request.direccion().strip());
        usuario.setRegion(request.region());
        usuario.setComuna(request.comuna());
        if (puedeCambiarRol) {
            usuario.setRol(request.rol() == null ? "cliente" : request.rol());
        }
        if (request.password() != null) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        try {
            return UsuarioResponse.from(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
    }

    @Transactional
    public void eliminar(Integer id) {
        UsuarioEntity usuario = repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        try {
            repository.delete(usuario);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: el usuario tiene pedidos");
        }
    }

    private GoogleIdToken parseGoogleToken(String credential) {
        try {
            return GoogleIdToken.parse(GsonFactory.getDefaultInstance(), credential);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }
    }

    private GoogleIdToken.Payload verificarGoogleToken(GoogleIdToken token, String clientId) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(List.of(clientId)).build();
            if (!token.verify(verifier)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
            }
            return token.getPayload();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo contactar a Google para validar el token");
        } catch (GeneralSecurityException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }
    }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private void validarDominioRegistro(String email) {
        if (!dominioPermitido(email)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Datos inválidos: email");
        }
    }

    private boolean dominioPermitido(String email) {
        int separator = email.lastIndexOf('@');
        return separator >= 0 && DOMINIOS_PERMITIDOS.contains(email.substring(separator + 1));
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null) {
            return null;
        }
        String valor = telefono.strip();
        return valor.isEmpty() ? null : valor;
    }

    private String randomPassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
