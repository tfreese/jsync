/// **
// * Created: 18.11.2018
// */
//
// package de.freese.jsync.filesystem.sender;
//
// import java.io.IOException;
// import java.net.InetSocketAddress;
// import java.net.URI;
// import java.nio.ByteBuffer;
// import java.nio.channels.AsynchronousChannelGroup;
// import java.nio.channels.AsynchronousSocketChannel;
// import java.nio.channels.ReadableByteChannel;
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
// * {@link Sender} für Remote-Filesysteme.
// *
// * @author Thomas Freese
// */
// @SuppressWarnings("resource")
// public class RemoteSenderAsync extends AbstractSender
// {
// /**
// * @author Thomas Freese
// */
// private class NoCloseReadableByteChannel implements ReadableByteChannel
// {
// /**
// *
// */
// private final AsynchronousSocketChannel delegate;
//
// /**
// * Erstellt ein neues {@link NoCloseReadableByteChannel} Object.
// *
// * @param delegate {@link AsynchronousSocketChannel}
// */
// public NoCloseReadableByteChannel(final AsynchronousSocketChannel delegate)
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
// // Empty
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
// * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
// */
// @Override
// public int read(final ByteBuffer dst) throws IOException
// {
// Future<Integer> futureRead = this.delegate.read(dst);
//
// try
// {
// return futureRead.get();
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
// * Erstellt ein neues {@link RemoteSenderAsync} Object.
// *
// * @param serverUri {@link URI}
// */
// public RemoteSenderAsync(final URI serverUri)
// {
// super(serverUri);
//
// this.executorService = Executors.newCachedThreadPool();
// this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
// }
//
// /**
// * @see de.freese.jsync.filesystem.sender.Sender#connect()
// */
// @Override
// public void connect() throws Exception
// {
// InetSocketAddress serverAddress = new InetSocketAddress(getBaseUri().getHost(), getBaseUri().getPort());
//
// // this.client = SocketChannel.open(serverAddress);
// // this.client.configureBlocking(true);
//
// // int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
// // this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(poolSize));
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
// * @see de.freese.jsync.filesystem.sender.Sender#disconnect()
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
// * @see de.freese.jsync.filesystem.sender.Sender#getChannel(de.freese.jsync.model.SyncItem)
// */
// @Override
// public ReadableByteChannel getChannel(final SyncItem syncItem) throws Exception
// {
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
//
// this.buffer.putLong(syncItem.getSize());
//
// byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
// this.buffer.putInt(bytes.length);
// this.buffer.put(bytes);
//
// this.buffer.flip();
// write(this.buffer);
//
// return new NoCloseReadableByteChannel(this.asyncSocketChannel);
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
// public String getChecksum(final String relativePath, final LongConsumer consumerBytesRead) throws Exception
// {
// String checksum = null;
//
// this.buffer.clear();
//
// // JSyncCommand senden.
// Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_CHECKSUM);
//
// byte[] bytes = relativePath.getBytes(getCharset());
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
// this.buffer.flip();
//
// bytes = new byte[this.buffer.getInt()];
// this.buffer.get(bytes);
// checksum = new String(bytes, getCharset());
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
// Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
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
// // while (getClient().read(bufferfer) > 0)
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
// // Future<Integer> futureResponse = asyncSocketChannel.read(this.buffer);
// //
// // futureResponse.get();
// //
// // this.buffer.flip();
// //
// // byte b = this.buffer.get();
// //
// // if (b != Byte.MIN_VALUE)
// // {
// // // TODO Fehlerhandling
// // }
// }
//
// /**
// * @param buffer {@link ByteBuffer}
// * @throws Exception Falls was schief geht.
// */
// protected void write(final ByteBuffer buffer) throws Exception
// {
// Future<Integer> futureRequest = this.asyncSocketChannel.write(buffer);
//
// while (futureRequest.get() > 0)
// {
// futureRequest = this.asyncSocketChannel.write(buffer);
// }
// }
// }
