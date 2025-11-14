package com.baskaaleksander.nuvine.infrastructure.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoService {

    private static final String AES_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(
            @Value("${crypto.notifications.secret}") String aesSecret, @Value("${crypto.notifications.fingerprint-secret}") String hmacSecret
    ) {
        byte[] aesKeyBytes = Base64.getDecoder().decode(aesSecret);
        if (!(aesKeyBytes.length == 16 || aesKeyBytes.length == 24 || aesKeyBytes.length == 32)) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes, got: " + aesKeyBytes.length);
        }
        this.aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        byte[] hmacKeyBytes = Base64.getDecoder().decode(hmacSecret);
        this.hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ct.length);
            buffer.put(iv);
            buffer.put(ct);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new RuntimeException("AES-GCM encryption failed", ex);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] ct = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(AES_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String hash(String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);

            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new RuntimeException("HMAC-SHA256 failed", ex);
        }
    }
}
