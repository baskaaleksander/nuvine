package com.baskaaleksander.nuvine.integration.support;

import io.jsonwebtoken.Jwts;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class JwtTestUtils {

    private static final String KEY_ID = "test-key";

    private static final KeyPair KEY_PAIR;
    private static final PrivateKey PRIVATE_KEY;
    private static final RSAPublicKey PUBLIC_KEY;

    static {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KEY_PAIR = keyGen.generateKeyPair();
            PRIVATE_KEY = KEY_PAIR.getPrivate();
            PUBLIC_KEY = (RSAPublicKey) KEY_PAIR.getPublic();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test RSA keypair", e);
        }
    }

    private final String issuer;

    public JwtTestUtils(String issuer) {
        this.issuer = issuer;
    }

    public String generateJwt(UUID userId, String email) {
        return generateJwt(userId, email, List.of("ROLE_USER"));
    }

    public String generateJwt(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();

        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("email", email)
                .claim("preferred_username", email)
                .claim("realm_access", Map.of("roles", roles))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .signWith(PRIVATE_KEY, Jwts.SIG.RS256)
                .compact();
    }

    public String generateUserJwt(UUID userId, String email) {
        return generateJwt(userId, email, List.of("ROLE_USER"));
    }

    public String generateInternalServiceJwt() {
        return generateJwt(
                UUID.randomUUID(),
                "internal-service@nuvine.com",
                List.of("ROLE_INTERNAL_SERVICE")
        );
    }

    public String generateExpiredJwt(UUID userId, String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .header().keyId(KEY_ID).and()
                .subject(userId.toString())
                .issuer(issuer)
                .claim("email", email)
                .claim("preferred_username", email)
                .claim("realm_access", Map.of("roles", List.of("ROLE_USER")))
                .issuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .signWith(PRIVATE_KEY, Jwts.SIG.RS256)
                .compact();
    }

    public static String getKeyId() {
        return KEY_ID;
    }

    public static RSAPublicKey getPublicKey() {
        return PUBLIC_KEY;
    }

    public static String getModulusBase64Url() {
        byte[] modBytes = PUBLIC_KEY.getModulus().toByteArray();
        if (modBytes[0] == 0) {
            byte[] tmp = new byte[modBytes.length - 1];
            System.arraycopy(modBytes, 1, tmp, 0, tmp.length);
            modBytes = tmp;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(modBytes);
    }

    public static String getExponentBase64Url() {
        byte[] expBytes = PUBLIC_KEY.getPublicExponent().toByteArray();
        if (expBytes[0] == 0) {
            byte[] tmp = new byte[expBytes.length - 1];
            System.arraycopy(expBytes, 1, tmp, 0, tmp.length);
            expBytes = tmp;
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(expBytes);
    }
}
