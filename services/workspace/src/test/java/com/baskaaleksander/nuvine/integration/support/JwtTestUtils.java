package com.baskaaleksander.nuvine.integration.support;

import io.jsonwebtoken.Jwts;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JwtTestUtils {

    private static final String KEY_ID = "test-key";

    // Fixed 2048-bit RSA private key for deterministic test tokens
    private static final String PRIVATE_KEY_BASE64 =
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC7VJTUt9Us8cKj" +
        "MzEtBJvnaS9RKQZ2Qv9bKJYjS4QENBiPNmYwj7gB0mnT1sBNnOBRraxLdbwzVSZ6" +
        "k8cc3laO/bP7H0QswsYyuFRr8jtB0AQ8Dg0mMoGMrmwXJgZlH9Ea0W3K5yt8XBPG" +
        "5i+3lqQjF1r7W8LRZGuOXbKlZxMk2B9m+7ahdPR9tLaT0qqqKnMoIKqR5rM/F4y0" +
        "FJlErdOYDqVfbS6KjMIJzLGqZ1ildDVaI1qzFqHvK4s7wRqVlq3qL6K6F5OqH9S7" +
        "TxZKV2IhE1dBTqV0sdP3TO8R1DRVjQ3eXhA/WGP0bMoiPwYq5kAS2BVKUHR4J0Ej" +
        "T7+BFMZ1AgMBAAECggEAM0O6vKpPym/5CpWBp9W1A7kDH8KL8W1AGqPmMHcJ8w7M" +
        "0fIq8yqAq0uZj0p0N2xOsrJ5KWSoJDLy1xP6k1b8MBHK1p6yGwGy8+4sD5h7xBOl" +
        "oXvCgKpn1KqZlIN5tLi3ofvWsepk1ACR2WzqwvKCPG2+9Xc+aJiV6cQ4QKBHD4nE" +
        "oFZp8sHkwdmKXJWWSXlkXJqNKJirGJhOKqC2sl5qN5bPIB1R1G8osHw6nDoLUVFE" +
        "r8K6N5m0fqKCrGJkW3mPChMdJh5BqV4jrGhqJC8nU5yOz1Q2bXJEzQ9r5K6f4V6M" +
        "hRl8nAJ8D9JKNI7xLqPzBYOi1lXLpJmBNBNVfNQkQQKBgQDwAJU2oQP7P4D1jMar" +
        "qxuGsyfABVAnKJNV4H0F5VQ0xURTPQ3u0u1YMaY7xQf1F5DGTmLBHx4gT5j0xH4n" +
        "PqJg0dl1EV1VQHqwqXovA2liF3IDh7RkQcH0R1KnVJYYvDG3GGVmGshKJGEuGkLr" +
        "eIYPmKQ8oG4Eq7UyDfkAhVtVNQKBgQDH8Tqq/zq5rLbsaHK2PG2QKP0lMWZ7suVp" +
        "GXluHLqC7rJtGn0xI2D8cFBZVAuiqxT9psrIFVDHFLqPJkLT8WkNNL2x8C7LqQ8w" +
        "q8ZbWKJmYmYNLgJ7Xq0gI8m0wKJ6YIH6ygLpDJmN0RBOzUKnYCFHL0gZxaHhH9CL" +
        "B9QQvKnGoQKBgEVWU5/OxCu3JGqdxKjFl3KeKpOCUWHjGtQN0lmGqLBxvKfXl0Z0" +
        "OiCl4K0mPjQp8qT3zmUqLh1MQXD2B4qZVuFqdwKXi9UJCVE7fiYHHHKLsD7GBHND" +
        "xRKhUGM4d7MQlqM0cc2K9Q0hRD8dEJUmKOuLlKYsLxKBqxTVtrKlZmGFAoGBALjq" +
        "V2MQMa7FudepQJfhclNwPuJZLi5BNEDEVHBGQP3Gd5apDyFLqLy8H4QHxBqOJl/H" +
        "i5m8K4P1PoVKgT5N2ZI4eu1OeH2YEqSJ2cRQ0LXGL5i1fBK0jo7N0qeuOAHNN4lG" +
        "PyBJKjPbPmdZNP8w1ojSr7CYqHODugVhDHGZr0ChAoGBANrc+k0Z77dTa7G0gMNx" +
        "rk7V2OlPbWmINQHeTPR/P6r8dGPJKneaMJrKNDmGP+RcM9vycs1RviP2PpHP7s1j" +
        "rBdnT2WZi3RQHZ4UDynDB6lxksFP1k4i0xTQI9m/JCLZfF8sQE8bMjNDoqDqPHCX" +
        "lFkRBZxG8IXkPexOaOad2e8l";

    private static final PrivateKey PRIVATE_KEY;
    private static final RSAPublicKey PUBLIC_KEY;

    static {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(PRIVATE_KEY_BASE64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PRIVATE_KEY = kf.generatePrivate(spec);

            RSAPrivateCrtKey privCrt = (RSAPrivateCrtKey) PRIVATE_KEY;
            RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(
                privCrt.getModulus(), privCrt.getPublicExponent());
            PUBLIC_KEY = (RSAPublicKey) kf.generatePublic(pubSpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test RSA keypair", e);
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
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(PUBLIC_KEY.getModulus().toByteArray());
    }

    public static String getExponentBase64Url() {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(PUBLIC_KEY.getPublicExponent().toByteArray());
    }
}
