//// Created: 05.04.2018
// package de.freese.jsync.filesystem.receiver;
//
// import java.io.IOException;
// import java.net.InetSocketAddress;
// import java.net.URI;
// import java.nio.ByteBuffer;
// import java.nio.channels.AsynchronousChannelGroup;
// import java.nio.channels.AsynchronousSocketChannel;
// import java.nio.channels.WritableByteChannel;
// import java.nio.charset.Charset;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Objects;
// import java.util.concurrent.ExecutionException;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.Future;
// import java.util.function.LongConsumer;
// import de.freese.jsync.Options;
// import de.freese.jsync.model.JSyncCommand;
// import de.freese.jsync.model.SyncItem;
// import de.freese.jsync.model.serializer.Serializers;
// import de.freese.jsync.utils.JSyncUtils;
//
/// **
// * {@link Receiver} für Remote-Filesysteme.
// *
// * @author Thomas Freese
// */
// @SuppressWarnings("resource")
// public class RemoteReceiverAsync extends AbstractReceiver
// {
// /**
// * @author Thomas Freese
// */
// private class NoCloseWritableByteChannel implements WritableByteChannel
// {
// /**
// *
// */
// private final AsynchronousSocketChannel delegate;
//
// /**
// * Erstellt ein neues {@link NoCloseWritableByteChannel} Object.
// *
// * @param delegate {@link AsynchronousSocketChannel}
// */
// public NoCloseWritableByteChannel(final AsynchronousSocketChannel delegate)
// {
// super();
//
// this.delegate = Objects.requireNonNull(delegate, "delegate required");
// }
//
// /**
// * @see java.nio.channels.Channel#close()
// */
// @Override
// public void close() throws IOException
// {
// // Diese Methode wird unmittelbar nach dem Kopieren aufgerufen.
// // Daher muss erst mal auf das Finish vom Server gewartet werden.
// // try
// // {
// // handleResponse();
// // }
// // catch (IOException iex)
// // {
// // throw iex;
// // }
// // catch (Exception ex)
// // {
// // throw new IOException(ex);
// // }
// }
//
// /**
// * @see java.nio.channels.Channel#isOpen()
// */
// @Override
// public boolean isOpen()
// {
// return this.delegate.isOpen();
// }
//
// /**
// * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
// */
// @Override
// public int write(final ByteBuffer src) throws IOException
// {
// Future<Integer> futureWrite = this.delegate.write(src);
//
// try
// {
// return futureWrite.get();
// }
// catch (InterruptedException | ExecutionException ex)
// {
// if (ex.getCause() instanceof IOException)
// {
// throw (IOException) ex.getCause();
// }
//
// throw new IOException(ex.getCause());
// }
// }
// }
//
// /**
// *
// */
// private AsynchronousSocketChannel asyncSocketChannel = null;
//
// /**
// *
// */
// private final ByteBuffer buffer;
//
// /**
// *
// */
// private AsynchronousChannelGroup channelGroup = null;
//
// /**
// *
// */
// private final ExecutorService executorService;
//
// /**
// * Erzeugt eine neue Instanz von {@link RemoteReceiverAsync}.
// *
// * @param serverUri {@link URI}
// */
// public RemoteReceiverAsync(final URI serverUri)
// {
// super(serverUri);
//
// this.executorService = Executors.newCachedThreadPool();
// this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#connect()
// */
// @Override
// public void connect() throws Exception
// {
// InetSocketAddress serverAddress = new InetSocketAddress(getBaseUri().getHost(), getBaseUri().getPort());
//
// this.channelGroup = AsynchronousChannelGroup.withThreadPool(this.executorService);
//
// this.asyncSocketChannel = AsynchronousSocketChannel.open(this.channelGroup);
// // this.client = AsynchronousSocketChannel.open();
//
// Future<Void> futureConnect = this.asyncSocketChannel.connect(serverAddress);
// futureConnect.get();
//
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.CONNECT);
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String)
// */
// @Override
// public void deleteDirectory(final String relativeDir) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_DIRECTORY);
//
// byte[] bytes = relativeDir.getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String)
// */
// @Override
// public void deleteFile(final String relativeFile) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_FILE);
//
// byte[] bytes = relativeFile.getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
// */
// @Override
// public void disconnect() throws Exception
// {
// this.asyncSocketChannel.shutdownInput();
// this.asyncSocketChannel.shutdownOutput();
// this.asyncSocketChannel.close();
//
// if (this.channelGroup != null)
// {
// JSyncUtils.shutdown(this.channelGroup, getLogger());
// }
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(de.freese.jsync.model.SyncItem)
// */
// @Override
// public WritableByteChannel getChannel(final SyncItem syncItem) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
//
// this.buffer.putLong(syncItem.getSize());
//
// byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// // if (syncItem.getChecksum() == null)
// // {
// // buf.put((byte) 0);
// // }
// // else
// // {
// // buf.put((byte) 1);
// // bytes = syncItem.getChecksum().getBytes(getCharset());
// // buf.putInt(bytes.length);
// // buf.put(bytes);
// // }
//
// this.buffer.flip();
// write(this.buffer);
//
// return new NoCloseWritableByteChannel(this.asyncSocketChannel);
// }
//
// /**
// * @return {@link Charset}
// */
// protected Charset getCharset()
// {
// return Options.CHARSET;
// }
//
// /**
// * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.util.function.LongConsumer)
// */
// @Override
// public String getChecksum(final String relativeFile, final LongConsumer consumerBytesRead) throws Exception
// {
// String checksum = null;
//
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CHECKSUM);
//
// byte[] bytes = relativeFile.getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
// this.buffer.flip();
// write(this.buffer);
//
// // Response lesen.
// this.buffer.clear();
// Future<Integer> futureResponse = this.asyncSocketChannel.read(this.buffer);
// futureResponse.get();
//
// // while (futureResponse.get() > 0)
// // {
// this.buffer.flip();
//
// bytes = new byte[this.buffer.getInt()];
// this.buffer.get(bytes);
// checksum = new String(bytes, getCharset());
//
// // buf.clear();
// // futureResponse = asyncSocketChannel.read(buf);
// // }
//
// return checksum;
// }
//
// /**
// * @see de.freese.jsync.filesystem.FileSystem#getSyncItems(boolean)
// */
// @Override
// public List<SyncItem> getSyncItems(final boolean followSymLinks)
// {
// List<SyncItem> syncItems = new ArrayList<>();
//
// try
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
//
// byte[] bytes = getBasePath().toString().getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// this.buffer.put(followSymLinks ? (byte) 1 : (byte) 0);
//
// this.buffer.flip();
// write(this.buffer);
//
// // Response lesen.
// this.buffer.clear();
// Future<Integer> futureResponse = this.asyncSocketChannel.read(this.buffer);
//
// boolean sizeRead = false;
// boolean finished = false;
// int countSyncItems = -1;
// int counter = 0;
//
// // while (asyncSocketChannel.read(buffer) > 0)
// while (futureResponse.get() > 0)
// {
// this.buffer.flip();
//
// if (!sizeRead)
// {
// countSyncItems = this.buffer.getInt();
// sizeRead = true;
// }
//
// while (this.buffer.hasRemaining())
// {
// SyncItem syncItem = Serializers.readFrom(this.buffer, SyncItem.class);
// syncItems.add(syncItem);
// counter++;
//
// if (counter == countSyncItems)
// {
// // Finish-Flag.
// finished = true;
// break;
// }
// }
//
// if (finished)
// {
// break;
// }
//
// this.buffer.clear();
// futureResponse = this.asyncSocketChannel.read(this.buffer);
// }
// }
// catch (RuntimeException rex)
// {
// throw rex;
// }
// catch (Exception ex)
// {
// throw new RuntimeException(ex);
// }
//
// return syncItems;
// }
//
// /**
// * Finish-Flag (Byte.MIN_VALUE) abwarten oder Fehlerhandling.
// *
// * @throws Exception Falls was schief geht.
// */
// protected void handleResponse() throws Exception
// {
// // this.buffer.clear();
// //
// // Future<Integer> futureResponse = asyncSocketChannel.read(buf);
// //
// // futureResponse.get();
// //
// // buf.flip();
// //
// // byte b = buf.get();
// //
// // if (b != Byte.MIN_VALUE)
// // {
// // // TODO Fehlerhandling
// // }
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(de.freese.jsync.model.SyncItem)
// */
// @Override
// public void updateDirectory(final SyncItem syncItem) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_DIRECTORY);
//
// Serializers.writeTo(this.buffer, syncItem);
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(de.freese.jsync.model.SyncItem)
// */
// @Override
// public void updateFile(final SyncItem syncItem) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_FILE);
//
// Serializers.writeTo(this.buffer, syncItem);
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(de.freese.jsync.model.SyncItem, boolean)
// */
// @Override
// public void validateFile(final SyncItem syncItem, final boolean withChecksum) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.TARGET_VALIDATE_FILE);
//
// this.buffer.putLong(syncItem.getSize());
// this.buffer.put(withChecksum ? (byte) 1 : (byte) 0);
//
// byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// if (syncItem.getChecksum() == null)
// {
// this.buffer.put((byte) 0);
// }
// else
// {
// this.buffer.put((byte) 1);
// bytes = syncItem.getChecksum().getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
// }
//
// this.buffer.flip();
// write(this.buffer);
//
// handleResponse();
// }
//
// /**
// * @param buf {@link ByteBuffer}
// * @throws Exception Falls was schief geht.
// */
// protected void write(final ByteBuffer buf) throws Exception
// {
// Future<Integer> futureRequest = this.asyncSocketChannel.write(buf);
//
// while (futureRequest.get() > 0)
// {
// futureRequest = this.asyncSocketChannel.write(buf);
// }
// }
// }
