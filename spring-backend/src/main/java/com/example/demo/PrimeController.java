
package com.example.demo;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PrimeController {

    @GetMapping("/primes")
    public List<Integer> getPrimes(@AuthenticationPrincipal Jwt jwt, @RequestParam int max) {
        // 🔍 Log token claims for debugging
        System.out.println("Google user email: " + jwt.getClaim("email"));
        System.out.println("Subject (sub): " + jwt.getSubject());
        System.out.println("Issued at: " + jwt.getIssuedAt());
        System.out.println("Expires at: " + jwt.getExpiresAt());
        System.out.println("Issuer: " + jwt.getClaimAsString("iss") /*jwt.getIssuer()*/);
        System.out.println("Provider: " + jwt.getClaimAsString("provider") /*jwt.getClaim("provider")*/);
        System.out.println("User: " + jwt.getSubject());
        
        List<Integer> primes = new ArrayList<>();
        for (int num = 2; num <= max; num++) {
            boolean isPrime = true;
            for (int i = 2; i <= Math.sqrt(num); i++) {
                if (num % i == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) primes.add(num);
        }
        return primes;
    }
}
