// Created: 31.07.2021
package de.freese.jsync.swing;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;

import de.freese.jsync.swing.messages.Messages;
import reactor.core.publisher.Hooks;

/**
 * @author Thomas Freese
 */
public final class JsyncSwingLauncher
{
    /**
     * @param args String[]
     */
    public static void main(final String[] args)
    {
        if (args == null)
        {
            JSyncSwing.getLogger().info("init");
        }
        else
        {
            JSyncSwing.getLogger().info("init: {}", Arrays.toString(args));
        }

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            JSyncSwing.getLogger().error("Default exception handler", ex);
        });

        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        JsyncContext.setExecutorService(Executors.newFixedThreadPool(8));
        JsyncContext.setScheduledExecutorService(Executors.newScheduledThreadPool(8));
        JsyncContext.setMessages(new Messages(Locale.getDefault()));

        // Um Comparator Fehler zu vermeiden.
        // System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        Hooks.onErrorDropped(th -> JSyncSwing.getLogger().warn(th.getMessage()));

        JsyncContext.startLocalRsocketServer();

        SwingUtilities.invokeLater(() -> {
            try
            {
                new JSyncSwing().initGui(frame -> {
                    // frame.setSize(800, 600);
                    // frame.setSize(1024, 768);
                    // frame.setSize(1280, 768);
                    // frame.setSize(1280, 1024);
                    frame.setSize(1680, 1050);
                    // frame.setSize(1920, 1080);
                    // frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);

                    JsyncContext.setMainFrame(frame);
                });
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        });
    }

    /**
     * Erstellt ein neues {@link JsyncSwingLauncher} Object.
     */
    private JsyncSwingLauncher()
    {
        super();
    }
}
