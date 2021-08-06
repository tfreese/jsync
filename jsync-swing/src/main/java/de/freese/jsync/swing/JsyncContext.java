// Created: 06.08.2021
package de.freese.jsync.swing;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;

import de.freese.jsync.swing.messages.Messages;

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
    private static ScheduledExecutorService scheduledExecutorService;

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
     * @return {@link ScheduledExecutorService}
     */
    public static ScheduledExecutorService getScheduledExecutorService()
    {
        return scheduledExecutorService;
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
     * @param scheduledExecutorService {@link ScheduledExecutorService}
     */
    static void setScheduledExecutorService(final ScheduledExecutorService scheduledExecutorService)
    {
        JsyncContext.scheduledExecutorService = scheduledExecutorService;
    }

    /**
     * Erstellt ein neues {@link JsyncContext} Object.
     */
    private JsyncContext()
    {
        super();
    }
}
