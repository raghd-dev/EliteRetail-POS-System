package com.eliteretail.pos.service;

import com.eliteretail.pos.utils.PasswordUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {
    @Test
    void pbkdf2HashCanBeVerified() {
        String hash = PasswordUtils.hash("manager123");
        assertTrue(PasswordUtils.verify("manager123", hash));
        assertFalse(PasswordUtils.verify("wrong-password", hash));
    }

    @Test
    void legacyPlaintextPasswordsAreRejected() {
        assertFalse(PasswordUtils.verify("legacy", "legacy"));
        assertFalse(PasswordUtils.verify("wrong", "legacy"));
    }

    @Test
    void malformedPbkdf2ValueIsRejected() {
        assertFalse(PasswordUtils.verify("password", "pbkdf2$bad$value"));
    }
}
