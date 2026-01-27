package org.project.infrastructure.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.project.domain.shared.containers.Result;
import org.project.domain.user.entities.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

@Component
public class JWTUtility {

    private static final Logger log = LoggerFactory.getLogger(JWTUtility.class);

    private static final RSAPublicKey KEYCLOAK_PUBLIC_KEY = readX509PublicKey();

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JWTUtility() {
        this.jwtDecoder = NimbusJwtDecoder.withPublicKey(KEYCLOAK_PUBLIC_KEY).build();

        // Encoder without private key → used only if you later add one
        this.jwtEncoder = null;
    }

    /* ================= TOKEN GENERATION ================= */

    public String generateToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("UYol")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.DAYS))
                .subject(retrieveUPN(user))
                .claim("groups", user.role().name())
                .claim("firstname", user.personalData().firstname())
                .claim("surname", user.personalData().surname())
                .claim("isVerified", user.isVerified())
                .build();

        // If you later add private key signing, this will work
        return encode(claims);
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("UYol")
                .issuedAt(now)
                .expiresAt(now.plus(365, ChronoUnit.DAYS))
                .subject(retrieveUPN(user))
                .claim("groups", user.role().name())
                .build();

        return encode(claims);
    }

    private String encode(JwtClaimsSet claims) {
        if (jwtEncoder == null) {
            throw new IllegalStateException(
                    "JWTEncoder is not configured. Add private key if you want to generate tokens."
            );
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /* ================= PARSING / VERIFICATION ================= */

    public Result<Jwt, Throwable> parse(String token) {
        try {
            return Result.success(jwtDecoder.decode(token));
        } catch (JwtException e) {
            log.error("Cannot parse jwt.", e);
            return Result.failure(e);
        }
    }

    public Result<Jwt, Throwable> verifyAndParse(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Result.success(jwt);
        } catch (JwtException e) {
            return Result.failure(e);
        }
    }

    /* ================= HELPERS ================= */

    private static String retrieveUPN(User user) {
        return user.personalData().email().isPresent()
                ? user.personalData().email().get()
                : user.personalData().phone().orElseThrow();
    }

    private static RSAPublicKey readX509PublicKey() {
        try (InputStream is = JWTUtility.class
                .getClassLoader()
                .getResourceAsStream("keycloackPublicKey.pem")) {

            if (is == null) {
                throw new IllegalStateException("Public key file not found in resources");
            }

            String key = new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.decodeBase64(key);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key", e);
        }
    }
}
