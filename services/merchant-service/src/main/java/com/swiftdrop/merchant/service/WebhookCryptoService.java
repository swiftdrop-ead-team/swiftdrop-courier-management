package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.exception.WebhookConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class WebhookCryptoService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int NONCE_LENGTH = 12;

    private final String encryptionSecret;

    public WebhookCryptoService(@Value("${merchant.webhook.encryption-key:}") String encryptionSecret) {
        this.encryptionSecret = encryptionSecret;
    }

    public String generateSecret() {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] nonce = new byte[NONCE_LENGTH];
            RANDOM.nextBytes(nonce);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (WebhookConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebhookConfigurationException("Webhook secret encryption failed");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length <= NONCE_LENGTH) {
                throw new WebhookConfigurationException("Stored webhook secret is invalid");
            }
            byte[] nonce = java.util.Arrays.copyOfRange(combined, 0, NONCE_LENGTH);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, NONCE_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (WebhookConfigurationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebhookConfigurationException("Stored webhook secret could not be decrypted");
        }
    }

    public String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new WebhookConfigurationException("Webhook signature generation failed");
        }
    }

    private SecretKeySpec encryptionKey() throws Exception {
        if (encryptionSecret == null || encryptionSecret.length() < 32) {
            throw new WebhookConfigurationException("WEBHOOK_ENCRYPTION_KEY must be at least 32 characters");
        }
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(encryptionSecret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, "AES");
    }
}
