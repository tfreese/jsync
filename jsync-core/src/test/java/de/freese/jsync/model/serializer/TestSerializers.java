package de.freese.jsync.model.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class TestSerializers
{
    /**
     *
     */
    @Test
    void test010String()
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(32);

        Serializers.writeTo(buffer, "A");
        Serializers.writeTo(buffer, "BB");
        Serializers.writeTo(buffer, "CCC");

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        assertEquals("A", Serializers.readFrom(buffer, String.class));
        assertEquals("BB", Serializers.readFrom(buffer, String.class));
        assertEquals("CCC", Serializers.readFrom(buffer, String.class));
    }

    /**
     *
     */
    @Test
    void test020Group()
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(48);

        Group group1 = new Group("TestGroupA", 41);
        Group group2 = new Group("TestGroupB", 42);

        Serializers.writeTo(buffer, group1);
        Serializers.writeTo(buffer, group2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        group1 = Serializers.readFrom(buffer, Group.class);
        assertEquals("TestGroupA", group1.getName());
        assertEquals(41, group1.getGid());

        group2 = Serializers.readFrom(buffer, Group.class);
        assertEquals("TestGroupB", group2.getName());
        assertEquals(42, group2.getGid());
    }

    /**
     *
     */
    @Test
    void test030User()
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(64);

        User user1 = new User("TestUserA", 41);
        User user2 = new User("TestUserB", 42);

        Serializers.writeTo(buffer, user1);
        Serializers.writeTo(buffer, user2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        user1 = Serializers.readFrom(buffer, User.class);
        assertEquals("TestUserA", user1.getName());
        assertEquals(41, user1.getUid());

        user2 = Serializers.readFrom(buffer, User.class);
        assertEquals("TestUserB", user2.getName());
        assertEquals(42, user2.getUid());
    }

    /**
     *
     */
    @Test
    void test040User()
    {
        ByteBuffer buffer = ByteBuffer.allocateDirect(256);

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

        Serializers.writeTo(buffer, syncItem1);
        Serializers.writeTo(buffer, syncItem2);

        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        buffer = ByteBuffer.wrap(bytes);

        syncItem1 = Serializers.readFrom(buffer, SyncItem.class);
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

        syncItem2 = Serializers.readFrom(buffer, SyncItem.class);
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
}
