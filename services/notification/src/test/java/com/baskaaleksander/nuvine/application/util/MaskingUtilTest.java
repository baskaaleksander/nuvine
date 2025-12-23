package com.baskaaleksander.nuvine.application.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaskingUtilTest {

    @Test
    void maskEmail_validEmail_partiallyConceals() {
        assertEquals("u***r@example.com", MaskingUtil.maskEmail("user@example.com"));
    }

    @Test
    void maskEmail_shortLocalPart_returnsStarMask() {
        assertEquals("*@example.com", MaskingUtil.maskEmail("ab@example.com"));
    }

    @Test
    void maskEmail_invalidEmail_returnsInvalid() {
        assertEquals("invalid", MaskingUtil.maskEmail("not-an-email"));
    }

    @Test
    void maskEmail_null_returnsInvalid() {
        assertEquals("invalid", MaskingUtil.maskEmail(null));
    }
}
