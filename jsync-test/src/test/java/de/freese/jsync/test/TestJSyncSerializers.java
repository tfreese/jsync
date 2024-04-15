// Created: 06.04.2018
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.rsocket.serialisation.ByteBufReader;
import de.freese.jsync.rsocket.serialisation.ByteBufWriter;
import de.freese.jsync.serialisation.DefaultSerializer;
import de.freese.jsync.serialisation.Serializer;
import de.freese.jsync.serialisation.io.ByteBufferReader;
import de.freese.jsync.serialisation.io.ByteBufferWriter;
import de.freese.jsync.serialisation.io.InputStreamReader;
import de.freese.jsync.serialisation.io.OutputStreamWriter;
import de.freese.jsync.serialisation.serializer.GroupSerializer;
import de.freese.jsync.serialisation.serializer.UserSerializer;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncSerializers {
    private static final int BUFFER_SIZE = 1024 * 16;

    private static final DataHolder DATA_HOLDER_BYTE_BUF = new DataHolder() {
        private final ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.buffer(BUFFER_SIZE);

        @Override
        public Object getInput() {
            return buffer;
        }

        @Override
        public Object getOutput() {
            buffer.clear();

            return buffer;
        }
    };
    private static final DataHolder DATA_HOLDER_BYTE_BUFFER = new DataHolder() {
        private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        @Override
        public Object getInput() {
            buffer.flip();

            return buffer;
        }

        @Override
        public Object getOutput() {
            buffer.clear();

            return buffer;
        }
    };
    private static final DataHolder DATA_HOLDER_OUTPUT_INPUT_STREAM = new DataHolder() {
        private ByteArrayOutputStream baos;

        @Override
        public Object getInput() {
            return new ByteArrayInputStream(baos.toByteArray());
        }

        @Override
        public Object getOutput() {
            baos = new ByteArrayOutputStream(BUFFER_SIZE);

            return baos;
        }
    };

    private interface DataHolder {
        Object getInput();

        Object getOutput();
    }

    static Stream<Arguments> createArguments() {
        return Stream.of(
                Arguments.of("ByteBuf", new DefaultSerializer<>(new ByteBufReader(), new ByteBufWriter()), DATA_HOLDER_BYTE_BUF),
                Arguments.of("ByteBuffer", new DefaultSerializer<>(new ByteBufferReader(), new ByteBufferWriter()), DATA_HOLDER_BYTE_BUFFER),
                Arguments.of("InputOutputStream", new DefaultSerializer<>(new InputStreamReader(), new OutputStreamWriter()), DATA_HOLDER_OUTPUT_INPUT_STREAM)
        );
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testBoolean(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        serializer.getWriter().writeBooleanOrNull(output, null);
        serializer.writeBoolean(output, true);
        serializer.writeBoolean(output, false);

        final Object input = dataHolder.getInput();
        assertNull(serializer.getReader().readBooleanOrNull(input));
        assertTrue(serializer.readBoolean(input));
        assertFalse(serializer.readBoolean(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testDouble(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        serializer.getWriter().writeDoubleOrNull(output, null);
        serializer.getWriter().writeDouble(output, 123.123D);

        final Object input = dataHolder.getInput();
        assertNull(serializer.getReader().readDoubleOrNull(input));
        assertEquals(123.123D, serializer.getReader().readDouble(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testException(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        final Exception exception1 = new UnsupportedOperationException("Test Fail");
        serializer.write(output, exception1);

        final Object input = dataHolder.getInput();
        final Exception exception2 = serializer.readException(input);

        assertEquals(UnsupportedOperationException.class, exception2.getClass());
        assertEquals("Test Fail", exception2.getMessage());
        assertEquals(exception1.getStackTrace().length, exception2.getStackTrace().length);
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testFloat(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        serializer.getWriter().writeFloatOrNull(output, null);
        serializer.getWriter().writeFloat(output, 123.123F);

        final Object input = dataHolder.getInput();
        assertNull(serializer.getReader().readFloatOrNull(input));
        assertEquals(123.123F, serializer.getReader().readFloat(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testGroup(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        Group group1 = new Group("TestGroupA", 41);
        Group group2 = new Group("TestGroupB", 42);

        GroupSerializer.write(serializer.getWriter(), output, group1);
        GroupSerializer.write(serializer.getWriter(), output, group2);

        final Object input = dataHolder.getInput();
        group1 = GroupSerializer.read(serializer.getReader(), input);
        assertNotNull(group1);
        assertEquals("TestGroupA", group1.getName());
        assertEquals(41, group1.getGid());

        group2 = GroupSerializer.read(serializer.getReader(), input);
        assertNotNull(group2);
        assertEquals("TestGroupB", group2.getName());
        assertEquals(42, group2.getGid());
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testInteger(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        serializer.getWriter().writeIntegerOrNull(output, null);
        serializer.getWriter().writeInteger(output, 123);

        final Object input = dataHolder.getInput();
        assertNull(serializer.getReader().readIntegerOrNull(input));
        assertEquals(123, serializer.getReader().readInteger(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testLong(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        serializer.getWriter().writeLongOrNull(output, null);
        serializer.writeLong(output, 123L);

        final Object input = dataHolder.getInput();
        assertNull(serializer.getReader().readLongOrNull(input));
        assertEquals(123L, serializer.readLong(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testPathFilter(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();

        final Set<String> directoryFiltersOrigin = Set.of("a", "b");
        final Set<String> fileFiltersOrigin = Set.of("c", "d");

        serializer.write(output, new PathFilterEndsWith(directoryFiltersOrigin, fileFiltersOrigin));

        final Object input = dataHolder.getInput();
        final PathFilter pathFilter = serializer.readPathFilter(input);

        assertEquals(PathFilterEndsWith.class, pathFilter.getClass());
        assertEquals(directoryFiltersOrigin, pathFilter.getDirectoryFilter());
        assertEquals(fileFiltersOrigin, pathFilter.getFileFilter());
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testString(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final String text = "abcABC123,.;:-_ÖÄÜöäü*'#+`?ß´987/()=?";

        final Object output = dataHolder.getOutput();
        serializer.writeString(output, null);
        serializer.writeString(output, "");
        serializer.writeString(output, text);

        final Object input = dataHolder.getInput();
        assertNull(serializer.readString(input));
        assertEquals("", serializer.readString(input));
        assertEquals(text, serializer.readString(input));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testSyncItem(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();

        SyncItem syncItem1 = new DefaultSyncItem("/");
        syncItem1.setChecksum("ABC");
        syncItem1.setFile(false);
        syncItem1.setLastModifiedTime(123456789);
        syncItem1.setSize(123456789);
        //        syncItem1.setGroup(new Group("TestGroupA", 41));
        //        syncItem1.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        //        syncItem1.setUser(new User("TestUserA", 41));

        SyncItem syncItem2 = new DefaultSyncItem("/script.sh");
        syncItem2.setChecksum("XYZ");
        syncItem2.setFile(true);
        syncItem2.setLastModifiedTime(987654321);
        syncItem2.setSize(987654321);
        //        syncItem2.setGroup(new Group("TestGroupB", 42));
        //        syncItem2.setPermissions(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE));
        //        syncItem2.setUser(new User("TestUserB", 42));

        serializer.write(output, syncItem1);
        serializer.write(output, syncItem2);

        final Object input = dataHolder.getInput();
        syncItem1 = serializer.readSyncItem(input);
        assertEquals("/", syncItem1.getRelativePath());
        assertEquals("ABC", syncItem1.getChecksum());
        assertFalse(syncItem1.isFile());
        assertTrue(syncItem1.isDirectory());
        assertEquals(123456789, syncItem1.getLastModifiedTime());
        assertEquals(123456789, syncItem1.getSize());
        //        assertEquals("TestGroupA", syncItem1.getGroup().getName());
        //        assertEquals(41, syncItem1.getGroup().getGid());
        //        assertEquals("rw-------", syncItem1.getPermissionsToString());
        //        assertEquals("TestUserA", syncItem1.getUser().getName());
        //        assertEquals(41, syncItem1.getUser().getUid());

        syncItem2 = serializer.readSyncItem(input);
        assertEquals("/script.sh", syncItem2.getRelativePath());
        assertEquals("XYZ", syncItem2.getChecksum());
        assertTrue(syncItem2.isFile());
        assertFalse(syncItem2.isDirectory());
        assertEquals(987654321, syncItem2.getLastModifiedTime());
        assertEquals(987654321, syncItem2.getSize());
        //        assertEquals("TestGroupB", syncItem2.getGroup().getName());
        //        assertEquals(42, syncItem2.getGroup().getGid());
        //        assertEquals("rwx------", syncItem2.getPermissionsToString());
        //        assertEquals("TestUserB", syncItem2.getUser().getName());
        //        assertEquals(42, syncItem2.getUser().getUid());
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testUser(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        final Object output = dataHolder.getOutput();
        User user1 = new User("TestUserA", 41);
        User user2 = new User("TestUserB", 42);

        UserSerializer.write(serializer.getWriter(), output, user1);
        UserSerializer.write(serializer.getWriter(), output, user2);

        final Object input = dataHolder.getInput();
        user1 = UserSerializer.read(serializer.getReader(), input);
        assertNotNull(user1);
        assertEquals("TestUserA", user1.getName());
        assertEquals(41, user1.getUid());

        user2 = UserSerializer.read(serializer.getReader(), input);
        assertNotNull(user2);
        assertEquals("TestUserB", user2.getName());
        assertEquals(42, user2.getUid());
    }
}
