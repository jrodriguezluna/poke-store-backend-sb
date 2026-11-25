package cl.pokestore.backend.usuario;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import cl.pokestore.backend.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService service;
    private final JwtTokenService tokenService;

    public UsuarioController(UsuarioService service, JwtTokenService tokenService) {
        this.service = service;
        this.tokenService = tokenService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest request) {
        UsuarioResponse usuario = service.login(request);
        return SesionResponse.from(usuario, tokenService.generar(usuario));
    }

    @PostMapping("/auth/google")
    public SesionResponse loginGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        UsuarioResponse usuario = service.loginGoogle(request);
        return SesionResponse.from(usuario, tokenService.generar(usuario));
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioResponse modificar(
            @PathVariable Integer id,
            @Valid @RequestBody UsuarioUpdateRequest request,
            Authentication authentication
    ) {
        boolean esAdmin = esAdmin(authentication);
        if (!esAdmin && !authentication.getName().equals(String.valueOf(id))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes modificar otra cuenta");
        }
        return service.modificar(id, request, esAdmin);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
