// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncUtils {
    @Test
    void testBytesToHex() {
        final String hex = "0123456789ABCDEF";
        final byte[] bytes = JSyncUtils.hexToBytes(hex);

        assertEquals(hex, JSyncUtils.bytesToHex(bytes));
    }

    @Test
    void testChecksum() {
        final String checksum1 = JSyncUtils.bytesToHex(DigestUtils.sha256Digest("Hello World!".getBytes(StandardCharsets.UTF_8)));
        final String checksum2 = JSyncUtils.bytesToHex(DigestUtils.sha256Digest("Hello World!".getBytes(StandardCharsets.UTF_8)));

        assertEquals(checksum1, checksum2);
    }
}
