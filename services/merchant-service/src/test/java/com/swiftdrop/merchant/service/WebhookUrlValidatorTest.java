package com.swiftdrop.merchant.service;

import com.swiftdrop.merchant.exception.MerchantConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class WebhookUrlValidatorTest {

    @Test
    void rejectsLoopbackAndPlainHttpByDefault() {
        WebhookUrlValidator validator = new WebhookUrlValidator(false);
        assertThatThrownBy(() -> validator.validate("http://127.0.0.1/hook"))
                .isInstanceOf(MerchantConflictException.class);
        assertThatThrownBy(() -> validator.validate("https://127.0.0.1/hook"))
                .isInstanceOf(MerchantConflictException.class);
    }

    @Test
    void rejectsCredentialedAndQueryUrls() {
        WebhookUrlValidator validator = new WebhookUrlValidator(true);
        assertThatThrownBy(() -> validator.validate("http://user:pass@localhost/hook"))
                .isInstanceOf(MerchantConflictException.class);
        assertThatThrownBy(() -> validator.validate("http://localhost/hook?target=127.0.0.1"))
                .isInstanceOf(MerchantConflictException.class);
    }
}
