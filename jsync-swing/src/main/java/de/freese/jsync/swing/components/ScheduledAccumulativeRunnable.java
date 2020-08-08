package de.freese.jsync.swing.components;

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
     * MilliSeconds
     */
    private final int delay;

    /**
     *
     */
    private final ScheduledExecutorService scheduledExecutor;

    /**
     *
     */
    private Consumer<List<T>> submitConsumer = null;

    /**
     * Erstellt ein neues {@link ScheduledAccumulativeRunnable} Object.
     *
     * @param scheduledExecutor {@link ScheduledExecutorService}; optional
     */
    public ScheduledAccumulativeRunnable(final ScheduledExecutorService scheduledExecutor)
    {
        super();

        this.scheduledExecutor = Objects.requireNonNull(scheduledExecutor, "scheduledExecutor required");
        this.delay = 100;
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
        if (this.submitConsumer == null)
        {
            return;
        }

        this.submitConsumer.accept(args);
    }

    /**
     *
     */
    @Override
    protected final void submit()
    {
        this.scheduledExecutor.schedule(() -> SwingUtilities.invokeLater(this), this.delay, TimeUnit.MILLISECONDS);

        // LoggerFactory.getLogger(getClass()).info("ActiveCount: {}", ((ScheduledThreadPoolExecutor) this.scheduledExecutor).getActiveCount());
        // LoggerFactory.getLogger(getClass()).info("PoolSize: {}", ((ScheduledThreadPoolExecutor) this.scheduledExecutor).getPoolSize());

        // Timer timer = new Timer(this.delay, event -> SwingUtilities.invokeLater(this));
        // timer.setRepeats(false);
        // timer.start();
    }
}
