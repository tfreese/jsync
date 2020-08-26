// Created: 31.10.2016
package de.freese.jsync.server;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.server.handler.IoHandler;
import de.freese.jsync.server.handler.JSyncIoHandler;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.NamePreservingRunnable;

/**
 * Der Server nimmt nur die neuen Client-Verbindungen entgegen und übergibt sie einem {@link Processor}.<br>
 * Der {@link Processor} kümmert dann sich um das Connection-Handling.<br>
 * Der {@link IoHandler} übernimmt das Lesen und Schreiben von Request und Response.<br>
 * Zur Performance-Optimierung können mehrere Processoren gestartet werden,<br>
 * die im Wechsel (RoundRobin) mit neuen Verbindungen versorgt werden.
 *
 * @author Thomas Freese
 */
public class JSyncServer
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncServer.class);

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        ExecutorService executorService = Executors.newCachedThreadPool();

        JSyncServer server = new JSyncServer(8001, 2, executorService);
        // server.setIoHandler(new HttpIoHandler());
        // server.setIoHandler(new TestJSyncIoHandler());
        server.setIoHandler(new JSyncIoHandler());
        server.start();

        System.out.println();
        System.out.println();
        System.out.println("******************************************************************************************************************");
        System.out.println("You're using an IDE, click in this console and press ENTER to call System.exit() and trigger the shutdown routine.");
        System.out.println("******************************************************************************************************************");
        System.out.println();
        System.out.println();

        // Console für programmatische Eingabe simulieren.
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        System.setIn(pis);

        // Client Task starten
        // executorService.submit(() ->
        // {
        //
        // Thread.sleep(1000);
        //
        // InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8001);
        //
        // try (SocketChannel client = SocketChannel.open(hostAddress))
        // {
        // client.configureBlocking(false);
        //
        // // byte[] message = new String(companyName).getBytes();
        // // ByteBuffer buffer = ByteBuffer.wrap(message);
        //
        // Path pathSrc = null;
        // pathSrc = Paths.get(System.getProperty("user.dir"), "pom.xml");
        // // pathSrc = Paths.get(System.getProperty("user.home"), "dokumente/spiele/eve-online", "Haladas_Bergbauhandbuch_V.3.pdf");
        //
        // long fileSize = Files.size(pathSrc);
        // byte[] fileNameBytes = pathSrc.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        //
        // // TransferHeader mit allen benötigten Infos basteln.
        // ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
        // buffer.putLong(fileSize);
        // buffer.putInt(fileNameBytes.length);
        // buffer.put(fileNameBytes);
        //
        // // Header senden.
        // buffer.flip();
        // client.write(buffer);
        // buffer.clear();
        //
        // // Datei-Transfer
        // BiConsumer<Long, Long> monitor =
        // (written, gesamt) -> System.out.printf("Read: %d / %d = %.2f %%%n", written, gesamt, JSyncUtils.getPercent(written, gesamt));
        //
        // try (ReadableByteChannel fileChannel = new MonitoringReadableByteChannel(FileChannel.open(pathSrc, StandardOpenOption.READ), monitor, fileSize))
        // {
        // while (fileChannel.read(buffer) != -1)
        // {
        // buffer.flip();
        //
        // while (buffer.hasRemaining())
        // {
        // client.write(buffer);
        // }
        //
        // buffer.clear();
        // }
        // }
        // }
        //
        // // Console simulieren.
        // pos.write(0);
        //
        // return null;
        // });

        try
        {

            System.in.read();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        server.stop();
        System.exit(0);
    }

    /**
     *
     */
    private final ExecutorService executorService;

    /**
     *
     */
    private final boolean externalExecutor;
    /**
     *
     */
    private IoHandler ioHandler = null;
    /**
     *
     */
    private boolean isShutdown = false;
    /**
     *
     */
    private final int numOfProcessors;
    /**
     *
     */
    private final int port;
    /**
     * Queue für die {@link Processor}.
     */
    // private final Queue<Processor> processors = new ArrayBlockingQueue<>(NUM_OF_PROCESSORS);
    // private final Queue<Processor> processors = new ConcurrentLinkedQueue<>();
    private final LinkedList<Processor> processors = new LinkedList<>();
    /**
     *
     */
    private Selector selector = null;
    /**
     *
     */
    private ServerSocketChannel serverSocketChannel = null;
    /**
     *
     */
    private final Semaphore stopLock = new Semaphore(1, true);

    /**
     * Erstellt ein neues {@link JSyncServer} Object.
     *
     * @param port int
     */
    public JSyncServer(final int port)
    {
        this(port, 3, null);
    }

    /**
     * Erstellt ein neues {@link JSyncServer} Object.
     *
     * @param port int
     * @param numOfProcessors int
     */
    public JSyncServer(final int port, final int numOfProcessors)
    {
        this(port, numOfProcessors, null);
    }

    /**
     * Erstellt ein neues {@link JSyncServer} Object.
     *
     * @param port int
     * @param numOfProcessors int
     * @param executorService {@link ExecutorService}; optional
     */
    public JSyncServer(final int port, final int numOfProcessors, final ExecutorService executorService)
    {
        super();

        this.port = port;
        this.numOfProcessors = numOfProcessors;

        // this.executorService = Objects.requireNonNull(executorService, "executorService required");

        if (executorService == null)
        {
            int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            this.executorService = Executors.newFixedThreadPool(poolSize);
            this.externalExecutor = false;
        }
        else
        {
            this.executorService = executorService;
            this.externalExecutor = true;
        }
    }

    /**
     * @param ioHandler {@link IoHandler}
     */
    public void setIoHandler(final IoHandler ioHandler)
    {
        this.ioHandler = ioHandler;
    }

    /**
     * Starten des Servers.
     *
     * @throws IOException Falls was schief geht.
     */
    public void start() throws IOException
    {
        getLogger().info("starting server on port: {}", this.port);

        Objects.requireNonNull(this.ioHandler, "ioHandler requried");

        this.selector = Selector.open();

        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.configureBlocking(false);

        ServerSocket socket = this.serverSocketChannel.socket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(this.port), 50);

        SelectionKey selectionKey = this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        // selectionKey.attach(this);

        // Erzeugen der Prozessoren.
        while (this.processors.size() < getNumOfProcessors())
        {
            Processor processor = new Processor(getIoHandler());

            this.processors.add(processor);
            getExecutorService().execute(new NamePreservingRunnable(processor, "Processor-" + this.processors.size()));
        }

        getExecutorService().execute(new NamePreservingRunnable(this::listen, getClass().getSimpleName()));
    }

    /**
     * Stoppen des Servers.
     */
    public void stop()
    {
        getLogger().info("stopping server on port: {}", this.port);

        this.isShutdown = true;

        this.processors.forEach(Processor::stop);

        this.selector.wakeup();

        this.stopLock.acquireUninterruptibly();

        try
        {
            SelectionKey selectionKey = this.serverSocketChannel.keyFor(this.selector);

            if (selectionKey != null)
            {
                selectionKey.cancel();
            }

            this.serverSocketChannel.close();

            if (this.selector.isOpen())
            {
                this.selector.close();
            }
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            this.stopLock.release();
        }

        if (!this.externalExecutor)
        {
            JSyncUtils.shutdown(getExecutorService(), getLogger());
        }
    }

    /**
     * Reagieren auf Requests.
     */
    private void listen()
    {
        getLogger().info("server listening on port: {}", this.serverSocketChannel.socket().getLocalPort());

        this.stopLock.acquireUninterruptibly();

        try
        {
            while (!Thread.interrupted())
            {
                int readyChannels = this.selector.select();

                if (this.isShutdown || !this.selector.isOpen())
                {
                    break;
                }

                if (readyChannels > 0)
                {
                    Set<SelectionKey> selected = this.selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selected.iterator();

                    while (iterator.hasNext())
                    {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();

                        if (!selectionKey.isValid())
                        {
                            getLogger().debug("SelectionKey not valid: {}", selectionKey);
                        }

                        if (selectionKey.isAcceptable())
                        {
                            // Verbindung mit Client herstellen.
                            SocketChannel socketChannel = this.serverSocketChannel.accept();

                            getLogger().debug("Connection Accepted: {}", socketChannel.getRemoteAddress());
                            getLogger().debug("add new session: {}", socketChannel);

                            // Socket dem Processor übergeben.
                            nextProcessor().addSession(socketChannel);
                        }
                        else if (selectionKey.isConnectable())
                        {
                            getLogger().debug("Client Connected");
                        }
                    }

                    selected.clear();
                }
            }
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            this.stopLock.release();
        }
    }

    /**
     * Liefert den nächsten {@link Processor} im RoundRobin-Verfahren.<br>
     *
     * @return {@link Processor}
     */
    private synchronized Processor nextProcessor()
    {
        if (this.isShutdown)
        {
            return null;
        }

        // Ersten Processor entnehmen.
        Processor processor = this.processors.poll();

        // Processor wieder hinten dran hängen.
        this.processors.add(processor);

        return processor;
    }

    /**
     * @return {@link ExecutorService}
     */
    protected ExecutorService getExecutorService()
    {
        return this.executorService;
    }

    /**
     * @return {@link IoHandler}
     */
    protected IoHandler getIoHandler()
    {
        return this.ioHandler;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @return int
     */
    protected int getNumOfProcessors()
    {
        return this.numOfProcessors;
    }
}
