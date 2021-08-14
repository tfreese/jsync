// Created: 12.07.2020
package de.freese.jsync.swing.view;

import java.awt.Component;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.swing.JsyncContext;
import de.freese.jsync.swing.util.SwingUtils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractView
{
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @return {@link Component}
     */
    abstract Component getComponent();

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link JFrame}
     */
    protected JFrame getMainFrame()
    {
        return JsyncContext.getMainFrame();
    }

    /**
     * @param key String
     *
     * @return String
     */
    protected String getMessage(final String key)
    {
        return JsyncContext.getMessages().getString(key);
    }

    /**
     * @param runnable {@link Runnable}
     */
    protected void runInEdt(final Runnable runnable)
    {
        SwingUtils.runInEdt(runnable);
    }
}
