// Created: 06.04.2018
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJsyncSerializers
{
    /**
     * 8 kb
     */
    private static final ByteBuffer BUFFER = ByteBuffer.allocate(1024 * 8);

    /**
    *
    */
    private static final Serializer<ByteBuffer> SERIALIZER = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     *
     */
    @BeforeEach
    void beforeEach()
    {
        BUFFER.clear();
    }

    /**
     *
     *
     */
    @Test
    void test010String()
    {
        ByteBuffer buffer = BUFFER;

        SERIALIZER.writeTo(buffer, "A");
        SERIALIZER.writeTo(buffer, "BB");
        SERIALIZER.writeTo(buffer, "CCC");

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        assertEquals("A", SERIALIZER.readFrom(buffer, String.class));
        assertEquals("BB", SERIALIZER.readFrom(buffer, String.class));
        assertEquals("CCC", SERIALIZER.readFrom(buffer, String.class));
    }

    /**
     *
     */
    @Test
    void test020Group()
    {
        ByteBuffer buffer = BUFFER;

        Group group1 = new Group("TestGroupA", 41);
        Group group2 = new Group("TestGroupB", 42);

        SERIALIZER.writeTo(buffer, group1);
        SERIALIZER.writeTo(buffer, group2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        group1 = SERIALIZER.readFrom(buffer, Group.class);
        assertEquals("TestGroupA", group1.getName());
        assertEquals(41, group1.getGid());

        group2 = SERIALIZER.readFrom(buffer, Group.class);
        assertEquals("TestGroupB", group2.getName());
        assertEquals(42, group2.getGid());
    }

    /**
     *
     */
    @Test
    void test030User()
    {
        ByteBuffer buffer = BUFFER;

        User user1 = new User("TestUserA", 41);
        User user2 = new User("TestUserB", 42);

        SERIALIZER.writeTo(buffer, user1);
        SERIALIZER.writeTo(buffer, user2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        user1 = SERIALIZER.readFrom(buffer, User.class);
        assertEquals("TestUserA", user1.getName());
        assertEquals(41, user1.getUid());

        user2 = SERIALIZER.readFrom(buffer, User.class);
        assertEquals("TestUserB", user2.getName());
        assertEquals(42, user2.getUid());
    }

    /**
     *
     */
    @Test
    void test040SyncItem()
    {
        ByteBuffer buffer = BUFFER;

        SyncItem syncItem1 = new DefaultSyncItem("/");
        syncItem1.setChecksum("ABC");
        syncItem1.setFile(false);
        syncItem1.setGroup(new Group("TestGroupA", 41));
        syncItem1.setLastModifiedTime(123456789);
        syncItem1.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        syncItem1.setSize(123456789);
        syncItem1.setUser(new User("TestUserA", 41));

        SyncItem syncItem2 = new DefaultSyncItem("/script.sh");
        syncItem2.setChecksum("XYZ");
        syncItem2.setFile(true);
        syncItem2.setGroup(new Group("TestGroupB", 42));
        syncItem2.setLastModifiedTime(987654321);
        syncItem2.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        syncItem2.setSize(987654321);
        syncItem2.setUser(new User("TestUserB", 42));

        SERIALIZER.writeTo(buffer, syncItem1);
        SERIALIZER.writeTo(buffer, syncItem2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        syncItem1 = SERIALIZER.readFrom(buffer, SyncItem.class);
        assertEquals("/", syncItem1.getRelativePath());
        assertEquals("ABC", syncItem1.getChecksum());
        assertEquals(false, syncItem1.isFile());
        assertEquals(true, syncItem1.isDirectory());
        assertEquals("TestGroupA", syncItem1.getGroup().getName());
        assertEquals(41, syncItem1.getGroup().getGid());
        assertEquals(123456789, syncItem1.getLastModifiedTime());
        assertEquals("rw-------", syncItem1.getPermissionsToString());
        assertEquals(123456789, syncItem1.getSize());
        assertEquals("TestUserA", syncItem1.getUser().getName());
        assertEquals(41, syncItem1.getUser().getUid());

        syncItem2 = SERIALIZER.readFrom(buffer, SyncItem.class);
        assertEquals("/script.sh", syncItem2.getRelativePath());
        assertEquals("XYZ", syncItem2.getChecksum());
        assertEquals(true, syncItem2.isFile());
        assertEquals(false, syncItem2.isDirectory());
        assertEquals("TestGroupB", syncItem2.getGroup().getName());
        assertEquals(42, syncItem2.getGroup().getGid());
        assertEquals(987654321, syncItem2.getLastModifiedTime());
        assertEquals("rwx------", syncItem2.getPermissionsToString());
        assertEquals(987654321, syncItem2.getSize());
        assertEquals("TestUserB", syncItem2.getUser().getName());
        assertEquals(42, syncItem2.getUser().getUid());
    }

    /**
     *
     */
    @Test
    void test050Exception()
    {
        ByteBuffer buffer = BUFFER;

        Exception exception1 = new UnsupportedOperationException("Test Fail");
        SERIALIZER.writeTo(buffer, exception1, Exception.class);

        buffer.flip();
        Exception exception2 = SERIALIZER.readFrom(buffer, Exception.class);

        assertEquals(UnsupportedOperationException.class, exception2.getClass());
        assertEquals("Test Fail", exception2.getMessage());
        assertEquals(exception1.getStackTrace().length, exception2.getStackTrace().length);

        // LoggerFactory.getLogger(getClass()).error(null, exception1);
        // LoggerFactory.getLogger(getClass()).error(null, exception2);
    }
}
