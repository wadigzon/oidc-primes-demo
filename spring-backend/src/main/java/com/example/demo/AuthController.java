package com.example.demo;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    @Value("${app.github.client-id}")
    private String githubClientId;

    @Value("${app.github.client-secret}")
    private String githubClientSecret;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @PostMapping("/github")
    public Map<String, Object> github(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Missing GitHub authorization code.");
        }

        // Step 1: Exchange code for access token
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", githubClientId);
        params.add("client_secret", githubClientSecret);
        params.add("code", code);

        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "https://github.com/login/oauth/access_token",
                new HttpEntity<>(params, headers),
                Map.class
        );

        String githubToken = (String) tokenResponse.getBody().get("access_token");

        // Step 2: Get user info
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(githubToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                userRequest,
                Map.class
        );

        Map<String, Object> githubUser = userResponse.getBody();
        String username = (String) githubUser.get("login");
        Object email = githubUser.get("email");
        if (email == null) {
            // You can use /user/emails API or default to "unknown"
            email = "unknown@github.com";
        }

        // Step 3: Issue JWT
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        String jwt = Jwts.builder()
                .setSubject(username)
                .claim("provider", "github")
                .setIssuer("http://prime-list-app-demo-backend") // 👈 This is the line you asked about
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        return Map.of(
            "name", githubUser.getOrDefault("name", "unknown"),
            "email", email,
            "token", jwt
        );
    }
    // Add this in your AuthController.java
    @PostMapping("/github/revoke")
    public ResponseEntity<String> revokeGitHubToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(githubClientId, githubClientSecret); // GitHub requires client credentials
        headers.setContentType(MediaType.APPLICATION_JSON);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "https://api.github.com/applications/" + githubClientId + "/token",
            HttpMethod.DELETE,
            entity,
            String.class
        );
        return ResponseEntity.ok("Token revoked");
    }
}
