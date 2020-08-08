/**
 * Created: 25.11.2018
 */

package de.freese.jsync.swing;

import de.freese.jsync.swing.controller.JsyncController;
import de.freese.jsync.swing.messages.Messages;
import de.freese.jsync.swing.view.SyncView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.plaf.FontUIResource;

/**
 * @author Thomas Freese
 */
public final class JSyncSwingApplication
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class InstanceHolder
    {
        /**
         *
         */
        private static final JSyncSwingApplication INSTANCE = new JSyncSwingApplication();

        /**
         * Erstellt ein neues {@link InstanceHolder} Object.
         */
        private InstanceHolder()
        {
            super();
        }
    }

    /**
     * WindowListener zum Beenden.
     *
     * @author Thomas Freese
     */
    private class MainFrameListener extends WindowAdapter
    {
        /**
         * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
         */
        @Override
        public void windowClosing(final WindowEvent e)
        {
            getLogger().info("stop");
            JSyncSwingApplication.this.controller.shutdown();

            System.exit(0);
        }
    }

    /**
     *
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(JSyncSwingApplication.class);

    /**
     * @return {@link JSyncSwingApplication}
     */
    public static JSyncSwingApplication getInstance()
    {
        return InstanceHolder.INSTANCE;
    }

    /**
     * @return {@link Logger}
     */
    public static Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param args final String[]
     */
    public static void main(final String[] args)
    {
        Thread.setDefaultUncaughtExceptionHandler((t, ex) ->
        {
            getLogger().error("***Default exception handler***");
            getLogger().error(null, ex);

            // new ErrorDialog().forThrowable(ex).showAndWait();
        });

        // Um Comparator Fehler zu vermeiden.
        // System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        SwingUtilities.invokeLater(() ->
        {
            try
            {
                getInstance().init(args);
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

    /**
     *
     */
    private JsyncController controller;

    /**
     *
     */
    private ExecutorService executorService = null;

    /**
     *
     */
    private JFrame mainFrame = null;

    /**
     *
     */
    private Messages messages = null;

    /**
     *
     */
    private ScheduledExecutorService scheduledExecutorService = null;

    /**
     * Erstellt ein neues {@link JSyncSwingApplication} Object.
     */
    private JSyncSwingApplication()
    {
        super();
        this.controller = null;
    }

    /**
     * @return {@link ExecutorService}
     */
    public ExecutorService getExecutorService()
    {
        return this.executorService;
    }

    /**
     * @return JFrame
     */
    public JFrame getMainFrame()
    {
        return this.mainFrame;
    }

    /**
     * @return {@link Messages}
     */
    public Messages getMessages()
    {
        return this.messages;
    }

    /**
     * @return {@link ScheduledExecutorService}
     */
    public ScheduledExecutorService getScheduledExecutorService()
    {
        return this.scheduledExecutorService;
    }

    /**
     * Initialisierung der GUI.
     *
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    private void init(final String[] args) throws Exception
    {
        if (args == null)
        {
            getLogger().info("init");
        }
        else
        {
            getLogger().info("init: {}", Arrays.toString(args));
        }

        this.executorService = Executors.newFixedThreadPool(4);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(4);

        this.messages = new Messages(Locale.getDefault());

        initUIDefaults();

        JFrame frame = new JFrame();
        frame.setTitle(this.messages.getString("jsync.title"));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new MainFrameListener());
        frame.setLayout(new BorderLayout());

        // JLabel label = new JLabel("jSync Swing GUI", SwingConstants.CENTER);
        // frame.add(label, BorderLayout.CENTER);
        SyncView syncView = new SyncView();
        syncView.initGUI();

        this.controller = new JsyncController();
        this.controller.init(syncView);

        frame.add(syncView.getPanel(), BorderLayout.CENTER);

        // frame.setSize(800, 600);
        // frame.setSize(1024, 768);
        // frame.setSize(1280, 768);
        // frame.setSize(1280, 1024);
        frame.setSize(1680, 1050);
        // frame.setSize(1920, 1080);
        // frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        this.mainFrame = frame;
    }

    /**
     *
     */
    private void initUIDefaults()
    {
        getLogger().info("initUIDefaults");

        try
        {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // com.sun.java.swing.plaf.gtk.GTKLookAndFeel
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            // UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex)
        {
            LOGGER.error(null, ex);
        }

        UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

        // Farben
        Color color = new Color(215, 215, 215);
        // UIManager.put("Table.alternatingBackground", color);
        UIManager.put("Table.alternateRowColor", color);
        // UIManager.put("List.alternatingBackground", color);
        // defaults.put("Tree.alternatingBackground", color);

        // Fonts: Dialog, Monospaced, Arial, DejaVu Sans
        Font font = new Font("DejaVu Sans", Font.PLAIN, 16);

        UIManager.getLookAndFeelDefaults().entrySet().forEach(entry ->
        {
            Object key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof FontUIResource)
            {
                UIManager.put(key, new FontUIResource(font));
            }

            // String keyString = key.toString();
            //
            // if (keyString.endsWith(".font") || keyString.endsWith(".acceleratorFont"))
            // {
            // UIManager.put(key, new FontUIResource(font));
            // }
        });

        // Ausnahmen
        // Font fontBold = font.deriveFont(Font.BOLD);
        // UIManager.put("TitledBorder.font", fontBold);

        // UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        //
        // SortedSet<Object> uiKeys = new TreeSet<>(new Comparator<Object>()
        // {
        // /**
        // * @param o1 Object
        // * @param o2 Object
        // * @return int
        // */
        // @Override
        // public int compare(final Object o1, final Object o2)
        // {
        // return o1.toString().compareTo(o2.toString());
        // }
        //
        // });
        // uiKeys.addAll(defaults.keySet());
        //
        // String format = "%1$s \t %2$s %n";
        //
        // for (Object key : uiKeys)
        // {
        // Object value = defaults.get(key);
        //
        // System.out.printf(format, key.toString(), (value != null) ? value.toString() : "NULL");
        // }
    }
}
