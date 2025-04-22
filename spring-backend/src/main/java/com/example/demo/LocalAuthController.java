package com.example.demo;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;


@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class LocalAuthController {
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;
    private final String jwtSecret;

    public LocalAuthController(PasswordEncoder passwordEncoder, @Value("${app.jwt.secret}") String jwtSecret) {
        this.passwordEncoder = passwordEncoder;
        this.jwtSecret = jwtSecret;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> payload) {
        System.out.println("SIGNUP payload: " + payload);
        String email = payload.get("email");
        String password = payload.get("password");
    
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body("Username and password are required");
        }
    
        if (userStore.containsKey(email)) {
            return ResponseEntity.badRequest().body("User already exists");
        }
    
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(email, hashedPassword);
        userStore.put(email, user);
    // 🔐 Issue JWT
    Instant now = Instant.now();
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

    String jwt = Jwts.builder()
            .setSubject(email)
            .claim("provider", "local")
            .setIssuer("prime-list-app-demo-backend")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(3600))) // 1 hour
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

    return ResponseEntity.ok(Map.of(
        "name", email,
        "email", email,  // or null if you want to omit it
        "token", jwt
    ));
    }
    

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Map<String, String> payload) {
        System.out.println("SIGNIN payload: " + payload);
        String email = payload.get("email");
        String password = payload.get("password");

        User user = userStore.get(email);
        if (user == null || !passwordEncoder.matches(password, user.getHashedPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // Create JWT
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String jwt = Jwts.builder()
                .setSubject(email)
                .claim("provider", "local")
                .setIssuer("prime-list-app-demo-backend") // must be a valid URI if validated as URI
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600))) // 1 hour
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return ResponseEntity.ok(Map.of("name", email, "email", email + "@local.com", "token", jwt));    }
}
