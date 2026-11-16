package com.utn.atenea.seguridad.controller;

import com.utn.atenea.seguridad.UsuarioPrincipal;
import com.utn.atenea.seguridad.dto.CambioPasswordRequest;
import com.utn.atenea.seguridad.dto.LinkPasswordRequest;
import com.utn.atenea.seguridad.dto.TokenResponse;
import com.utn.atenea.seguridad.service.AuthService;
import com.utn.atenea.seguridad.service.RecuperacionPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** La cuenta del usuario logueado, con cualquier rol. */
@RestController
@RequestMapping("/api/cuenta")
@RequiredArgsConstructor
public class CuentaController {

    private final AuthService authService;
    private final RecuperacionPasswordService recuperacionPasswordService;

    // Devuelve tokens nuevos: el cambio cierra todas las sesiones, también la de este pedido.
    @PutMapping("/password")
    public TokenResponse cambiarPassword(@AuthenticationPrincipal UsuarioPrincipal principal,
                                         @Valid @RequestBody CambioPasswordRequest request) {
        return authService.cambiarPassword(principal.usuarioId(), request);
    }

    /** Para quien no se acuerda de la contraseña actual: le llega el mismo link que desde el login. */
    @PostMapping("/password/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pedirLinkPassword(@AuthenticationPrincipal UsuarioPrincipal principal,
                                  @Valid @RequestBody LinkPasswordRequest request) {
        recuperacionPasswordService.solicitarParaUsuario(principal.usuarioId(), request.rol());
    }
}
