/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

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
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestJSyncUtils
{
    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test010BytesToHex() throws Exception
    {
        String hex = "0123456789ABCDEF";
        byte[] bytes = JSyncUtils.hexToBytes(hex);

        assertEquals(hex, JSyncUtils.bytesToHex(bytes));
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    void test020Checksum() throws Exception
    {
        Path path = Paths.get("pom.xml");

        String checksum1 = DigestUtils.sha256DigestAsHex(path, 16, null);
        String checksum2 = DigestUtils.sha256DigestAsHex(path, 32, null);

        assertEquals(checksum1, checksum2);
    }
}
