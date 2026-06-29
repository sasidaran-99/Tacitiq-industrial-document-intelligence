package com.tacitiq.modules.auth.controller;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserRepository userRepository;

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
    public ResponseEntity<AuthResponse> login(
            @Validated @RequestBody AuthRequest request,
            HttpServletResponse response) {
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

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
                user.getYearsExperience()
        );

        return ResponseEntity.ok(new AuthResponse(accessToken, userDto));
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
                        user.getYearsExperience()
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
                user.getYearsExperience()
        );
        return ResponseEntity.ok(userDto);
    }
}
