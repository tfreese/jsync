// Created: 06.04.2018
package de.freese.jsync.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.model.serializer.adapter.impl.InputOutputStreamAdapter;
import de.freese.jsync.rsocket.model.adapter.ByteBufAdapter;

/**
 * @author Thomas Freese
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
class TestJSyncSerializers {
    private static final int BUFFER_SIZE = 1024 * 16;

    private static final DataHolder DATA_HOLDER_BYTE_BUF = new DataHolder() {
        private static final ByteBuf BYTE_BUF = UnpooledByteBufAllocator.DEFAULT.buffer(BUFFER_SIZE);

        @Override
        public Object getSink() {
            BYTE_BUF.clear();

            return BYTE_BUF;
        }

        @Override
        public Object getSource() {
            return BYTE_BUF;
        }
    };
    private static final DataHolder DATA_HOLDER_BYTE_BUFFER = new DataHolder() {
        private static final ByteBuffer BYTE_BUFFER = ByteBuffer.allocate(BUFFER_SIZE);

        @Override
        public Object getSink() {
            BYTE_BUFFER.clear();

            return BYTE_BUFFER;
        }

        @Override
        public Object getSource() {
            BYTE_BUFFER.flip();

            return BYTE_BUFFER;
        }
    };
    private static final DataHolder DATA_HOLDER_OUTPUT_INPUT_STREAM = new DataHolder() {
        private ByteArrayOutputStream baos;

        @Override
        public Object getSink() {
            baos = new ByteArrayOutputStream(BUFFER_SIZE);

            return baos;
        }

        @Override
        public Object getSource() {
            return new ByteArrayInputStream(baos.toByteArray());
        }
    };

    private interface DataHolder {
        Object getSink();

        Object getSource();
    }

    static Stream<Arguments> createArguments() {
        // @formatter:off
        return Stream.of(
                Arguments.of("ByteBufferAdapter", DefaultSerializer.of(new ByteBufferAdapter()), DATA_HOLDER_BYTE_BUFFER)
                , Arguments.of("ByteBufAdapter", DefaultSerializer.of(new ByteBufAdapter()), DATA_HOLDER_BYTE_BUF)
                , Arguments.of("InputOutputStreamAdapter", DefaultSerializer.of(new InputOutputStreamAdapter()), DATA_HOLDER_OUTPUT_INPUT_STREAM)
        );
        // @formatter:on
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testBoolean(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, Boolean.class);
        serializer.writeTo(sink, true);
        serializer.writeTo(sink, false);

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, Boolean.class));
        assertEquals(true, serializer.readFrom(source, boolean.class));
        assertEquals(false, serializer.readFrom(source, boolean.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testDouble(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, Double.class);
        serializer.writeTo(sink, 123.123D);

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, Double.class));
        assertEquals(123.123D, serializer.readFrom(source, double.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testException(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        Exception exception1 = new UnsupportedOperationException("Test Fail");
        serializer.writeTo(sink, exception1, Exception.class);

        Object source = dataHolder.getSource();

        Exception exception2 = serializer.readFrom(source, Exception.class);

        assertEquals(UnsupportedOperationException.class, exception2.getClass());
        assertEquals("Test Fail", exception2.getMessage());
        assertEquals(exception1.getStackTrace().length, exception2.getStackTrace().length);
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testFloat(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, Float.class);
        serializer.writeTo(sink, 123.123F);

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, Float.class));
        assertEquals(123.123F, serializer.readFrom(source, float.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testGroup(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        Group group1 = new Group("TestGroupA", 41);
        Group group2 = new Group("TestGroupB", 42);

        serializer.writeTo(sink, group1);
        serializer.writeTo(sink, group2);

        Object source = dataHolder.getSource();

        group1 = serializer.readFrom(source, Group.class);
        assertEquals("TestGroupA", group1.getName());
        assertEquals(41, group1.getGid());

        group2 = serializer.readFrom(source, Group.class);
        assertEquals("TestGroupB", group2.getName());
        assertEquals(42, group2.getGid());
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testInteger(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, Integer.class);
        serializer.writeTo(sink, 123);

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, Integer.class));
        assertEquals(123, serializer.readFrom(source, int.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testLong(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, Long.class);
        serializer.writeTo(sink, 123L);

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, Long.class));
        assertEquals(123L, serializer.readFrom(source, long.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testPathFilter(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        Set<String> directoryFiltersOrigin = Set.of("a", "b");
        Set<String> fileFiltersOrigin = Set.of("c", "d");

        serializer.writeTo(sink, new PathFilterEndsWith(directoryFiltersOrigin, fileFiltersOrigin), PathFilterEndsWith.class);

        Object source = dataHolder.getSource();

        PathFilter pathFilter = serializer.readFrom(source, PathFilter.class);

        assertEquals(PathFilterEndsWith.class, pathFilter.getClass());
        assertEquals(directoryFiltersOrigin, pathFilter.getDirectoryFilter());
        assertEquals(fileFiltersOrigin, pathFilter.getFileFilter());
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testString(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

        serializer.writeTo(sink, null, String.class);
        serializer.writeTo(sink, "-");
        serializer.writeTo(sink, "###");

        Object source = dataHolder.getSource();

        assertNull(serializer.readFrom(source, String.class));
        assertEquals("-", serializer.readFrom(source, String.class));
        assertEquals("###", serializer.readFrom(source, String.class));
    }

    @ParameterizedTest(name = "{index} -> {0}")
    @MethodSource("createArguments")
    void testSyncItem(final String name, final Serializer<Object, Object> serializer, final DataHolder dataHolder) {
        Object sink = dataHolder.getSink();

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

        serializer.writeTo(sink, syncItem1);
        serializer.writeTo(sink, syncItem2);

        Object source = dataHolder.getSource();

        syncItem1 = serializer.readFrom(source, SyncItem.class);
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

        syncItem2 = serializer.readFrom(source, SyncItem.class);
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
        Object sink = dataHolder.getSink();

        User user1 = new User("TestUserA", 41);
        User user2 = new User("TestUserB", 42);

        serializer.writeTo(sink, user1);
        serializer.writeTo(sink, user2);

        Object source = dataHolder.getSource();

        user1 = serializer.readFrom(source, User.class);
        assertEquals("TestUserA", user1.getName());
        assertEquals(41, user1.getUid());

        user2 = serializer.readFrom(source, User.class);
        assertEquals("TestUserB", user2.getName());
        assertEquals(42, user2.getUid());
    }
}
