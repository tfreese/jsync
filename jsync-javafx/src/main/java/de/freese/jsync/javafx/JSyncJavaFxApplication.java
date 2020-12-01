package de.freese.jsync.javafx;

import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Geht momentan nicht aus der IDE, sondern nur per Console: mvn compile exec:java<br>
 * <br>
 * In Eclipse:<br>
 * <ol>
 * <li>Konstruktor muss public empty-arg sein oder nicht vorhanden sein.
 * <li>VM-Parameter: --add-modules javafx.controls
 * <li>Module-Classpath: OpenJFX die jeweils 2 Jars für javafx-base, javafx-controls und javafx-graphics hinzufügen
 * </ol>
 *
 * @author Thomas Freese
 */
public class JSyncJavaFxApplication extends Application
{
    /**
     *
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(JSyncJavaFxApplication.class);

    /**
     * @return int; Beispiel: 14.0.1 = 014.000.001 = 14000001
     */
    private static int getJavaVersion()
    {
        // String javaVersion = SystemUtils.JAVA_VERSION;
        String javaVersion = System.getProperty("java.version");
        // String javaVersionDate = System.getProperty("java.version.date");
        // String vmVersion = System.getProperty("java.vm.version");
        String[] splits = javaVersion.toLowerCase().split("[._]");

        // Major
        String versionString = String.format("%03d", Integer.parseInt(splits[0]));

        // Minor
        versionString += "." + String.format("%03d", Integer.parseInt(splits[1]));

        if (splits.length > 2)
        {
            // Micro
            versionString += "." + String.format("%03d", Integer.parseInt(splits[2]));
        }

        if ((splits.length > 3) && !splits[3].startsWith("ea"))
        {
            // Update
            try
            {
                versionString += "." + String.format("%03d", Integer.parseInt(splits[3]));
            }
            catch (Exception ex)
            {
                LOGGER.error(null, ex);
            }
        }

        int version = Integer.parseInt(versionString.replace(".", ""));

        getLogger().info("JavaVersion = {} = {} = {}", javaVersion, versionString, version);

        return version;
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

        Runnable task = () ->
        {
            launch(args);
            // LauncherImpl.launchApplication(JSyncJavaFxApplication.class, JSyncJavaFxApplicationPreloader.class, args);
        };
        task.run();

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
     * Erstellt ein neues {@link JSyncJavaFxApplication} Object.
     */
    public JSyncJavaFxApplication()
    {
        super();
    }

    /**
     * @see javafx.application.Application#init()
     */
    @Override
    public void init() throws Exception
    {
        getLogger().info("init");
    }

    /**
     * @see javafx.application.Application#start(javafx.stage.Stage)
     */
    @Override
    public void start(final Stage primaryStage) throws Exception
    {
        getLogger().info("start");

        // Scene
        Scene scene = null;

        BorderPane pane = new BorderPane();
        pane.setCenter(new Label("jSync JavaFX GUI"));

        // Momentan kein Antialising wegen JavaFX-Bug.
        int javaVersion = getJavaVersion();

        if (Platform.isSupported(ConditionalFeature.SCENE3D) && (javaVersion >= 1_800_072))
        {

            scene = new Scene(pane, 1280, 768, true, SceneAntialiasing.BALANCED);
        }
        else
        {
            scene = new Scene(pane, 1280, 768);
        }

        getLogger().info("Antialising: {}", scene.getAntiAliasing());

        // scene.getStylesheets().add("styles/styles.css");

        primaryStage.setTitle("jSync JavaFX GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @see javafx.application.Application#stop()
     */
    @Override
    public void stop() throws Exception
    {
        getLogger().info("stop");
        System.exit(0);
    }
}
