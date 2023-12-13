// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    void testBytesToHex() throws Exception {
        final String hex = "0123456789ABCDEF";
        final byte[] bytes = JSyncUtils.hexToBytes(hex);

        assertEquals(hex, JSyncUtils.bytesToHex(bytes));
    }

    @Test
    void testChecksum() throws Exception {
        final Path path = Paths.get("pom.xml");

        final String checksum1 = DigestUtils.sha256DigestAsHex(path);
        final String checksum2 = DigestUtils.sha256DigestAsHex(path);

        assertEquals(checksum1, checksum2);
    }
}
