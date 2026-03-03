package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.AuthResponse;
import com.javation.coloringbook.DTO.LoginRequest;
import com.javation.coloringbook.DTO.RegisterRequest;
import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Security.JwtUtil;
import com.javation.coloringbook.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login - Retorna JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Autenticar usuário
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Carregar detalhes do usuário
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());

            // Gerar token JWT
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // Buscar dados do usuário
            Users user = userService.findUserByEmail(loginRequest.getEmail());

            // Retornar resposta
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .message("Login realizado com sucesso")
                    .build();

            log.info("Login bem-sucedido para usuário: {}", loginRequest.getEmail());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Tentativa de login falhou para: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou senha incorretos"));
        } catch (Exception e) {
            log.error("Erro no login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao processar login"));
        }
    }

    /**
     * Registro - Cria novo usuário e retorna JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email já cadastrado"));
            }

            // Criar novo usuário
            Users newUser = new Users();
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));

            Users savedUser = userService.createUser(newUser);

            // Gerar token JWT
            String token = jwtUtil.generateToken(savedUser.getEmail());

            // Retornar resposta
            AuthResponse response = AuthResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .message("Usuário criado com sucesso")
                    .build();

            log.info("Novo usuário registrado: {}", savedUser.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Erro no registro: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao criar usuário"));
        }
    }

    /**
     * Validar token - Endpoint para frontend verificar se token é válido
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractUsername(token);
                    Users user = userService.findUserByEmail(email);

                    return ResponseEntity.ok(Map.of(
                            "valid", true,
                            "userId", user.getId(),
                            "email", user.getEmail()
                    ));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token inválido"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token inválido"));
        }
    }

    /**
     * Refresh token - Gera novo token antes de expirar
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String oldToken = authHeader.substring(7);

                if (jwtUtil.validateToken(oldToken)) {
                    String email = jwtUtil.extractUsername(oldToken);
                    String newToken = jwtUtil.generateToken(email);

                    return ResponseEntity.ok(Map.of(
                            "token", newToken,
                            "type", "Bearer"
                    ));
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token inválido"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Erro ao renovar token"));
        }
    }
}