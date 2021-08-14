// Created: 06.08.2021
package de.freese.jsync.swing;

import java.util.concurrent.ExecutorService;

import javax.swing.JFrame;

import de.freese.jsync.rsocket.JsyncRSocketHandlerByteBuf;
import de.freese.jsync.rsocket.builder.RSocketBuilders;
import de.freese.jsync.swing.messages.Messages;
import de.freese.jsync.utils.JSyncUtils;
import io.rsocket.Closeable;
import io.rsocket.SocketAcceptor;

/**
 * @author Thomas Freese
 */
public final class JsyncContext
{
    /**
    *
    */
    private static ExecutorService executorService;

    /**
    *
    */
    private static JFrame mainFrame;

    /**
    *
    */
    private static Messages messages;

    /**
     *
     */
    private static Closeable rsocketServerLocal;

    /**
     * @return {@link ExecutorService}
     */
    public static ExecutorService getExecutorService()
    {
        return executorService;
    }

    /**
     * @return {@link JFrame}
     */
    public static JFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * @return {@link Messages}
     */
    public static Messages getMessages()
    {
        return messages;
    }

    /**
     * @param executorService {@link ExecutorService}
     */
    static void setExecutorService(final ExecutorService executorService)
    {
        JsyncContext.executorService = executorService;
    }

    /**
     * @param mainFrame {@link JFrame}
     */
    static void setMainFrame(final JFrame mainFrame)
    {
        JsyncContext.mainFrame = mainFrame;
    }

    /**
     * @param messages {@link Messages}
     */
    static void setMessages(final Messages messages)
    {
        JsyncContext.messages = messages;
    }

    /**
    *
    */
    public static void shutdown()
    {
        if (rsocketServerLocal != null)
        {
            rsocketServerLocal.dispose();
            rsocketServerLocal = null;
        }

        JSyncUtils.shutdown(executorService, JSyncSwing.getLogger());

        executorService = null;
        messages = null;
        mainFrame = null;
    }

    /**
     *
     */
    public static void startLocalRsocketServer()
    {
        if (rsocketServerLocal == null)
        {
          // @formatter:off
            rsocketServerLocal = RSocketBuilders.serverLocal()
              .name("jsync")
              .socketAcceptor(SocketAcceptor.with(new JsyncRSocketHandlerByteBuf()))
              .logger(JSyncSwing.getLogger())
              .build()
              .block()
              ;
          // @formatter:on
        }
    }

    /**
     * Erstellt ein neues {@link JsyncContext} Object.
     */
    private JsyncContext()
    {
        super();
    }
}
