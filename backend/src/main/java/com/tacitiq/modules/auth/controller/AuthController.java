package com.tacitiq.modules.auth.controller;
 
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.tacitiq.core.security.JwtTokenUtil;
import com.tacitiq.modules.auth.dto.AuthRequest;
import com.tacitiq.modules.auth.dto.AuthResponse;
import com.tacitiq.modules.auth.dto.CustomUserDetails;
import com.tacitiq.modules.auth.dto.UserDto;
import com.tacitiq.modules.auth.entity.User;
import com.tacitiq.modules.auth.repository.UserRepository;
import com.tacitiq.modules.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
 
@RestController
@RequestMapping("/api/auth")
public class AuthController {
 
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;
 
    @Value("${google.client-id}")
    private String googleClientId;
 
    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenUtil jwtTokenUtil,
            UserDetailsServiceImpl userDetailsService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }
 
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Validated @RequestBody AuthRequest request,
            HttpServletResponse response) {
        
        // Explicitly check and reject logins for Google/OAuth-only accounts that lack a password hash
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null && (existingUser.getPasswordHash() == null || existingUser.getPasswordHash().isEmpty())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password login disabled for this account");
        }
 
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bad credentials");
        }
 
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();
 
        String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
 
        // Set refresh token in HTTP-only cookie
        Cookie cookie = new Cookie("refresh_token", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
 
        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getPlantId(),
                user.getExpertiseAreas(),
                user.getRetirementDate(),
                user.getYearsExperience(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getProvider()
        );
 
        return ResponseEntity.ok(new AuthResponse(accessToken, userDto));
    }
 
    @PostMapping("/google")
    public ResponseEntity<?> loginWithGoogle(
            @RequestBody Map<String, String> requestBody,
            HttpServletResponse response) {
        
        String idTokenString = requestBody.get("idToken");
        if (idTokenString == null || idTokenString.isEmpty()) {
            return ResponseEntity.badRequest().body("Missing idToken");
        }
 
        try {
            JWTClaimsSet claims = verifyGoogleToken(idTokenString);
            String email = (String) claims.getClaim("email");
            String name = (String) claims.getClaim("name");
            String picture = (String) claims.getClaim("picture");
            String sub = claims.getSubject();
 
            if (email == null) {
                return ResponseEntity.badRequest().body("Token claims do not include email");
            }
 
            // Find user by email
            User user = userRepository.findByEmail(email).orElse(null);
 
            if (user == null) {
                // Register a new user
                user = User.builder()
                        .email(email)
                        .displayName(name)
                        .profilePicture(picture)
                        .googleSubjectId(sub)
                        .provider("GOOGLE")
                        .role("USER") // Default role
                        .passwordHash(null) // No password hash for Google-only users
                        .createdAt(java.time.OffsetDateTime.now())
                        .build();
                user = userRepository.save(user);
            } else {
                // User exists. Update sub and profile data from Google OAuth claims
                boolean modified = false;
                if (user.getGoogleSubjectId() == null) {
                    user.setGoogleSubjectId(sub);
                    modified = true;
                }
                if (name != null && !name.equals(user.getDisplayName())) {
                    user.setDisplayName(name);
                    modified = true;
                }
                if (picture != null && !picture.equals(user.getProfilePicture())) {
                    user.setProfilePicture(picture);
                    modified = true;
                }
                if (modified) {
                    user = userRepository.save(user);
                }
            }
 
            // Create CustomUserDetails
            CustomUserDetails userDetails = new CustomUserDetails(user);
 
            // Generate tokens
            String accessToken = jwtTokenUtil.generateAccessToken(userDetails);
            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);
 
            // Set refresh token in HTTP-only cookie
            Cookie cookie = new Cookie("refresh_token", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            cookie.setAttribute("SameSite", "Lax");
            response.addCookie(cookie);
 
            UserDto userDto = new UserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPlantId(),
                    user.getExpertiseAreas(),
                    user.getRetirementDate(),
                    user.getYearsExperience(),
                    user.getDisplayName(),
                    user.getProfilePicture(),
                    user.getProvider()
            );
 
            return ResponseEntity.ok(new AuthResponse(accessToken, userDto));
 
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Google authentication failed: " + e.getMessage());
        }
    }
 
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh token");
        }
 
        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
 
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh token");
        }
 
        try {
            String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
 
            if (jwtTokenUtil.validateToken(refreshToken, userDetails)) {
                String newAccessToken = jwtTokenUtil.generateAccessToken(userDetails);
                User user = userDetails.getUser();
                UserDto userDto = new UserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getRole(),
                        user.getPlantId(),
                        user.getExpertiseAreas(),
                        user.getRetirementDate(),
                        user.getYearsExperience(),
                        user.getDisplayName(),
                        user.getProfilePicture(),
                        user.getProvider()
                );
                return ResponseEntity.ok(new AuthResponse(newAccessToken, userDto));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
 
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
    }
 
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userDetails.getUser();
        UserDto userDto = new UserDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getPlantId(),
                user.getExpertiseAreas(),
                user.getRetirementDate(),
                user.getYearsExperience(),
                user.getDisplayName(),
                user.getProfilePicture(),
                user.getProvider()
        );
        return ResponseEntity.ok(userDto);
    }
 
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }
 
    private JWTClaimsSet verifyGoogleToken(String idToken) throws Exception {
        if (googleClientId == null || 
            googleClientId.isEmpty() || 
            "mock_client_id".equals(googleClientId) || 
            "YOUR_GOOGLE_CLIENT_ID".equals(googleClientId)) {
            throw new IllegalStateException("Google OAuth Client ID is not configured on this host.");
        }
 
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL("https://www.googleapis.com/oauth2/v3/certs"));
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
 
        JWTClaimsSet claimsSet = jwtProcessor.process(idToken, null);
 
        // Validate Issuer
        String iss = claimsSet.getIssuer();
        if (iss == null || (!iss.equals("accounts.google.com") && !iss.equals("https://accounts.google.com"))) {
            throw new IllegalArgumentException("Invalid issuer: " + iss);
        }
 
        // Validate Audience
        java.util.List<String> aud = claimsSet.getAudience();
        if (aud == null || !aud.contains(googleClientId)) {
            throw new IllegalArgumentException("Invalid audience");
        }
 
        // Validate Expiration
        java.util.Date exp = claimsSet.getExpirationTime();
        if (exp == null || exp.before(new java.util.Date())) {
            throw new IllegalArgumentException("Token has expired");
        }
 
        return claimsSet;
    }
}
