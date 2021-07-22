package de.freese.jsync.swing.components;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

/**
 * Zeitgesteuerter {@link AccumulativeRunnable}, der nach einer Zeitspanne die gesammelten Daten ausführt.
 *
 * @param <T> Type
 *
 * @author Thomas Freese
 */
public class ScheduledAccumulativeRunnable<T> extends AccumulativeRunnable<T>
{
    /**
     *
     */
    private final Duration delay;

    /**
     *
     */
    private final ScheduledExecutorService scheduledExecutor;

    /**
     *
     */
    private Consumer<List<T>> submitConsumer = chunks -> {
    };

    /**
     * Erstellt ein neues {@link ScheduledAccumulativeRunnable} Object.<br>
     * Default delay = 250 ms
     *
     * @param scheduledExecutor {@link ScheduledExecutorService}; optional
     */
    public ScheduledAccumulativeRunnable(final ScheduledExecutorService scheduledExecutor)
    {
        this(scheduledExecutor, Duration.ofMillis(250));
    }

    /**
     * Erstellt ein neues {@link ScheduledAccumulativeRunnable} Object.
     *
     * @param scheduledExecutor {@link ScheduledExecutorService}; optional
     * @param delay {@link Duration}
     */
    public ScheduledAccumulativeRunnable(final ScheduledExecutorService scheduledExecutor, final Duration delay)
    {
        super();

        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutor, "scheduledExecutor required");
        this.delay = Objects.requireNonNull(delay, "delay required");
    }

    /**
     * @param submitConsumer {@link Consumer}
     */
    public void doOnSubmit(final Consumer<List<T>> submitConsumer)
    {
        this.submitConsumer = Objects.requireNonNull(submitConsumer, "submitConsumer required");
    }

    /**
     * @see de.freese.jsync.swing.components.AccumulativeRunnable#run(java.util.List)
     */
    @Override
    protected void run(final List<T> args)
    {
        this.submitConsumer.accept(args);
    }

    /**
     *
     */
    @Override
    protected final void submit()
    {
        this.scheduledExecutor.schedule(() -> SwingUtilities.invokeLater(this), this.delay.toMillis(), TimeUnit.MILLISECONDS);

        // LoggerFactory.getLogger(getClass()).info("ActiveCount: {}", ((ScheduledThreadPoolExecutor) this.scheduledExecutor).getActiveCount());
        // LoggerFactory.getLogger(getClass()).info("PoolSize: {}", ((ScheduledThreadPoolExecutor) this.scheduledExecutor).getPoolSize());

        // Timer timer = new Timer((int) this.delay.toMillis(), event -> SwingUtilities.invokeLater(this));
        // timer.setRepeats(false);
        // timer.start();
    }
}
