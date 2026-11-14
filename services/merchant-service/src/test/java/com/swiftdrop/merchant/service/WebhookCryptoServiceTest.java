package com.swiftdrop.merchant.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.*;

class WebhookCryptoServiceTest {

    private final WebhookCryptoService cryptoService =
            new WebhookCryptoService("test-only-webhook-encryption-key");

    @Test
    void encryptedSecretCanBeDecryptedAndSignedWithHmacSha256() throws Exception {
        String secret = cryptoService.generateSecret();
        String encrypted = cryptoService.encrypt(secret);
        String payload = "{\"eventType\":\"parcel.status.changed\"}";

        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(cryptoService.decrypt(encrypted)).isEqualTo(secret);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertThat(cryptoService.sign(secret, payload))
                .isEqualTo(HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void productionEncryptionRequiresConfiguredKey() {
        WebhookCryptoService unconfigured = new WebhookCryptoService("");
        assertThatThrownBy(() -> unconfigured.encrypt("secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("WEBHOOK_ENCRYPTION_KEY");
    }
}
