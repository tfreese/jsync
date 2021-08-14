// Created: 14.08.2021
package de.freese.jsync.swing.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * @author Thomas Freese
 */
public final class SwingUtils
{
    /**
     * Malt einen Rahmen um jede {@link JComponent}.
     *
     * @param component {@link Component}
     */
    public static void enableDebug(final Component component)
    {
        // System.out.printf("%s%n", component.getClass().getSimpleName());

        if (component instanceof JComponent)
        {
            try
            {
                ((JComponent) component).setBorder(BorderFactory.createLineBorder(Color.MAGENTA));
            }
            catch (Exception ex)
            {
                // Ignore
            }
        }
    }

    /**
     * Malt einen Rahmen um jede {@link JComponent}.
     *
     * @param container {@link Container}
     */
    public static void enableDebug(final Container container)
    {
        enableDebug((Component) container);

        for (Component child : container.getComponents())
        {
            if (child instanceof Container)
            {
                enableDebug((Container) child);
            }
            else
            {
                enableDebug(child);
            }
        }
    }

    /**
     * @param runnable {@link Runnable}
     */
    public static void runInEdt(final Runnable runnable)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            runnable.run();
        }
        else
        {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Erstellt ein neues {@link SwingUtils} Object.
     */
    private SwingUtils()
    {
        super();
    }
}
