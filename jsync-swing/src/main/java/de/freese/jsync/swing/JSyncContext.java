// Created: 06.08.2021
package de.freese.jsync.swing;

import java.util.concurrent.ExecutorService;

import javax.swing.JFrame;

import de.freese.jsync.rsocket.JSyncRSocketHandlerByteBuf;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.swing.messages.Messages;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;

/**
 * @author Thomas Freese
 */
public final class JSyncContext
{
    private static ExecutorService executorService;

    private static JFrame mainFrame;

    private static Messages messages;

    private static Closeable rSocketServerLocal;

    public static ExecutorService getExecutorService()
    {
        return executorService;
    }

    public static JFrame getMainFrame()
    {
        return mainFrame;
    }

    public static Messages getMessages()
    {
        return messages;
    }

    public static void shutdown()
    {
        if (rSocketServerLocal != null)
        {
            rSocketServerLocal.dispose();
            rSocketServerLocal = null;
        }

        JSyncUtils.shutdown(executorService, JSyncSwing.getLogger());

        ByteBufferPool.DEFAULT.clear();

        executorService = null;
        messages = null;
        mainFrame = null;
    }

    public static void startLocalRSocketServer()
    {
        if (rSocketServerLocal == null)
        {
            // @formatter:off
            rSocketServerLocal = RSocketBuilders.serverLocal()
              .name("jSync")
              .socketAcceptor(SocketAcceptor.with(new JSyncRSocketHandlerByteBuf()))
              .logger(JSyncSwing.getLogger())
              .build()
              .block()
              ;
          // @formatter:on
        }
    }

    static void setExecutorService(final ExecutorService executorService)
    {
        JSyncContext.executorService = executorService;
    }

    static void setMainFrame(final JFrame mainFrame)
    {
        JSyncContext.mainFrame = mainFrame;
    }

    static void setMessages(final Messages messages)
    {
        JSyncContext.messages = messages;
    }

    private JSyncContext()
    {
        super();
    }
}
