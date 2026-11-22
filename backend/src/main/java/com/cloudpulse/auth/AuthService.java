package com.cloudpulse.auth;

import com.cloudpulse.exception.BadRequestException;
import com.cloudpulse.exception.UnauthorizedException;
import com.cloudpulse.security.JwtTokenProvider;
import com.cloudpulse.security.SecurityUtils;
import com.cloudpulse.user.Role;
import com.cloudpulse.user.User;
import com.cloudpulse.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            @Value("${app.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                Role.ROLE_USER
        );
        user = userRepository.save(user);

        String accessToken = tokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String refreshToken = createAndSaveRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, UserSummary.from(user));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = createAndSaveRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, UserSummary.from(user));
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or has been revoked");
        }

        User user = token.getUser();
        // Refresh token rotation: revoke old token and create new token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String newAccessToken = tokenProvider.generateTokenFromUserId(user.getId(), user.getEmail());
        String newRefreshToken = createAndSaveRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken, UserSummary.from(user));
    }

    @Transactional
    public void logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return UserSummary.from(user);
    }

    private String createAndSaveRefreshToken(User user) {
        String tokenStr = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);
        RefreshToken refreshToken = new RefreshToken(user, tokenStr, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }
}
