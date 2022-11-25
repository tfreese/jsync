// Created: 22.10.2016
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.nio.file.Paths;

import de.freese.jsync.utils.DigestUtils;
import de.freese.jsync.utils.JSyncUtils;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncUtils
{
    @Test
    void testBytesToHex() throws Exception
    {
        String hex = "0123456789ABCDEF";
        byte[] bytes = JSyncUtils.hexToBytes(hex);

        assertEquals(hex, JSyncUtils.bytesToHex(bytes));
    }

    @Test
    void testChecksum() throws Exception
    {
        Path path = Paths.get("pom.xml");

        String checksum1 = DigestUtils.sha256DigestAsHex(path);
        String checksum2 = DigestUtils.sha256DigestAsHex(path);

        assertEquals(checksum1, checksum2);
    }
}
