// Created: 12.07.2020
package de.freese.jsync.swing.view;

import java.awt.Component;

import javax.swing.JFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.swing.JSyncContext;
import de.freese.jsync.swing.util.SwingUtils;

/**
 * @author Thomas Freese
 */
public abstract class AbstractView {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    abstract Component getComponent();

    protected Logger getLogger() {
        return this.logger;
    }

    protected JFrame getMainFrame() {
        return JSyncContext.getMainFrame();
    }

    protected String getMessage(final String key) {
        return JSyncContext.getMessages().getString(key);
    }

    protected void runInEdt(final Runnable runnable) {
        SwingUtils.runInEdt(runnable);
    }
}
