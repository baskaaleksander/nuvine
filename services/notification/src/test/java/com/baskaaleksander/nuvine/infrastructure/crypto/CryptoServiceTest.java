package com.baskaaleksander.nuvine.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {

    private static String base64OfBytes(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static CryptoService newCryptoService() {
        byte[] aesKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] hmacKey = "hmac-key-32-bytes-minimum-ish".getBytes(StandardCharsets.UTF_8);

        return new CryptoService(base64OfBytes(aesKey), base64OfBytes(hmacKey));
    }

    @Test
    void encrypt_validData_encryptsSuccessfully() {
        CryptoService cryptoService = newCryptoService();

        String cipherText = cryptoService.encrypt("hello");

        assertNotNull(cipherText);
        assertNotEquals("hello", cipherText);
    }

    @Test
    void decrypt_validData_decryptsCorrectly() {
        CryptoService cryptoService = newCryptoService();
        String plainText = "hello";

        String cipherText = cryptoService.encrypt(plainText);
        String decrypted = cryptoService.decrypt(cipherText);

        assertEquals(plainText, decrypted);
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginal() {
        CryptoService cryptoService = newCryptoService();
        String plainText = "Some longer text with UTF-8: żółw";

        String decrypted = cryptoService.decrypt(cryptoService.encrypt(plainText));

        assertEquals(plainText, decrypted);
    }

    @Test
    void hash_sameInput_returnsSameHash() {
        CryptoService cryptoService = newCryptoService();

        String hash1 = cryptoService.hash("hello");
        String hash2 = cryptoService.hash("hello");

        assertEquals(hash1, hash2);
    }

    @Test
    void hash_differentInput_returnsDifferentHash() {
        CryptoService cryptoService = newCryptoService();

        String hash1 = cryptoService.hash("hello");
        String hash2 = cryptoService.hash("hello2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void constructor_invalidAesKeyLength_throwsIllegalArgumentException() {
        byte[] invalidAesKey = "short".getBytes(StandardCharsets.UTF_8);
        byte[] hmacKey = "hmac-key".getBytes(StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> new CryptoService(base64OfBytes(invalidAesKey), base64OfBytes(hmacKey))
        );
    }
}
