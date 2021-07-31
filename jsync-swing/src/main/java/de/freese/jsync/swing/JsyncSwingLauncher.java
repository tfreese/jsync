// Created: 31.07.2021
package de.freese.jsync.swing;

import javax.swing.SwingUtilities;

/**
 * @author Thomas Freese
 */
public class JsyncSwingLauncher
{
    /**
     * @param args String[]
     */
    public static void main(final String[] args)
    {
        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
            JSyncSwingApplication.getLogger().error("***Default exception handler***");
            JSyncSwingApplication.getLogger().error("Thread-Name = " + t.getName(), ex);

            // new ErrorDialog().forThrowable(ex).showAndWait();
        });

        // Um Comparator Fehler zu vermeiden.
        // System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        SwingUtilities.invokeLater(() -> {
            try
            {
                JSyncSwingApplication.getInstance().init(args);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
        });

        // Runnable task = () -> {
        // launch(args);
        // // LauncherImpl.launchApplication(JSyncJavaFxApplication.class, JSyncJavaFxApplicationPreloader.class, args);
        // };
        // task.run();

        // // Eigene ThreadGroup für Handling von Runtime-Exceptions.
        // ThreadGroup threadGroup = new ThreadGroup("jsync");
        //
        // // Kein Thread des gesamten Clients kann eine höhere Prio haben.
        // threadGroup.setMaxPriority(Thread.NORM_PRIORITY + 1);
        //
        // Thread thread = new Thread(threadGroup, task, "JSyncJavaFx-Startup");
        // // thread.setDaemon(false);
        // thread.start();
    }
}
