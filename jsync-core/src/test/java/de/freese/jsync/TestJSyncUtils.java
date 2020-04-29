/**
 * Created: 22.10.2016
 */

package de.freese.jsync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import de.freese.jsync.util.JSyncUtils;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
public class TestJSyncUtils
{
    /**
     * Erstellt ein neues {@link TestJSyncUtils} Object.
     */
    public TestJSyncUtils()
    {
        super();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    @Test
    public void test010BytesToHex() throws Exception
    {
        String hex = "0123456789ABCDEF";
        byte[] bytes = JSyncUtils.hexToBytes(hex);

        assertEquals(hex, JSyncUtils.bytesToHex(bytes));
    }
}
