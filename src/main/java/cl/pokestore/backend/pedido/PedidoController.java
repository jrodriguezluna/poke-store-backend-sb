package cl.pokestore.backend.pedido;

import java.util.List;

import cl.pokestore.backend.common.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @Valid @RequestBody PedidoRequest request,
            Authentication authentication
    ) {
        if (esVendedor(authentication)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El vendedor solo puede consultar pedidos");
        }
        validarPropietario(request.usuarioId(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @GetMapping
    public List<PedidoHistoryResponse> historial(
            @RequestParam(name = "usuarioId", required = false) Integer usuarioId,
            Authentication authentication
    ) {
        if (puedeVerTodasLasOrdenes(authentication)) {
            return usuarioId == null ? service.historialTodos() : service.historial(usuarioId);
        }
        if (usuarioId == null) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Datos inválidos: usuarioId");
        }
        validarPropietario(usuarioId, authentication);
        return service.historial(usuarioId);
    }

    @GetMapping("/{id}")
    public PedidoHistoryResponse detalle(@PathVariable Integer id, Authentication authentication) {
        Integer usuarioId = puedeVerTodasLasOrdenes(authentication)
                ? null
                : Integer.valueOf(authentication.getName());
        return service.detalle(id, usuarioId);
    }

    private void validarPropietario(Integer usuarioId, Authentication authentication) {
        boolean esAdmin = tieneRol(authentication, "ADMIN");
        if (!esAdmin && !authentication.getName().equals(String.valueOf(usuarioId))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes acceder a pedidos de otra cuenta");
        }
    }

    private boolean puedeVerTodasLasOrdenes(Authentication authentication) {
        return tieneRol(authentication, "ADMIN") || esVendedor(authentication);
    }

    private boolean esVendedor(Authentication authentication) {
        return tieneRol(authentication, "VENDEDOR");
    }

    private boolean tieneRol(Authentication authentication, String rol) {
        return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
