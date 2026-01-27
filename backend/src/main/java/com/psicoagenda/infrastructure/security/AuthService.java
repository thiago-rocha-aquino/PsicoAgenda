package com.psicoagenda.infrastructure.security;

import com.psicoagenda.application.dto.request.LoginRequest;
import com.psicoagenda.application.dto.request.RefreshTokenRequest;
import com.psicoagenda.application.dto.response.AuthResponse;
import com.psicoagenda.domain.entity.AdminUser;
import com.psicoagenda.domain.repository.AdminUserRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AdminUserRepository adminUserRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    public AuthService(AdminUserRepository adminUserRepository,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserDetailsService userDetailsService,
                       AuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AdminUser admin = adminUserRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Update last login
        admin.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(admin);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("User logged in: {}", request.email());
        auditService.logAccess("AdminUser", admin.getId(), "LOGIN");

        return new AuthResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtService.getAccessTokenExpiration() / 1000,
            new AuthResponse.UserInfo(
                admin.getId().toString(),
                admin.getEmail(),
                admin.getName()
            )
        );
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        AdminUser admin = adminUserRepository.findByEmail(username)
            .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("Token refreshed for user: {}", username);

        return new AuthResponse(
            newAccessToken,
            newRefreshToken,
            "Bearer",
            jwtService.getAccessTokenExpiration() / 1000,
            new AuthResponse.UserInfo(
                admin.getId().toString(),
                admin.getEmail(),
                admin.getName()
            )
        );
    }
}
