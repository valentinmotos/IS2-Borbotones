package com.example.jwtpartea.service;

import com.example.jwtpartea.dto.AuthRequest;
import com.example.jwtpartea.dto.AuthResponse;
import com.example.jwtpartea.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userService.loadUserByUsername(request.getUsername());
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return new AuthResponse(accessToken, "Bearer", refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token requerido");
        }

        try {
            String username = jwtService.extractUsername(request.getRefreshToken());
            UserDetails userDetails = userService.loadUserByUsername(username);
            if (!jwtService.isRefreshTokenValid(request.getRefreshToken(), userDetails)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido");
            }
            String accessToken = jwtService.generateToken(userDetails);
            return new AuthResponse(accessToken, "Bearer", request.getRefreshToken());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido", ex);
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado", ex);
        }
    }
}
