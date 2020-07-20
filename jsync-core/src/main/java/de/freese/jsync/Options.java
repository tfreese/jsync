// Created: 28.10.2016
package de.freese.jsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Enthält die Optionen für die Synchronisierung.
 *
 * @author Thomas Freese
 */
public class Options
{
    /**
     * @author Thomas Freese
     */
    public static class Builder
    {
        /**
         *
         */
        private final Options options;

        /**
         * Erstellt ein neues {@link Builder} Object.
         */
        public Builder()
        {
            super();

            this.options = new Options();
        }

        /**
         * @return {@link Options}
         */
        public Options build()
        {
            return this.options;
        }

        /**
         * @param checksum boolean
         * @return {@link Builder}
         */
        public Builder checksum(final boolean checksum)
        {
            this.options.checksum = checksum;
            return this;
        }

        /**
         * @param delete boolean
         * @return {@link Builder}
         */
        public Builder delete(final boolean delete)
        {
            this.options.delete = delete;
            return this;
        }

        /**
         * @param dryRun boolean
         * @return {@link Builder}
         */
        public Builder dryRun(final boolean dryRun)
        {
            this.options.dryRun = dryRun;
            return this;
        }

        /**
         * @param executorService {@link ExecutorService}
         * @return {@link Builder}
         */
        public Builder executorService(final ExecutorService executorService)
        {
            this.options.executorService = executorService;
            return this;
        }

        /**
         * @param followSymLinks boolean
         * @return {@link Builder}
         */
        public Builder followSymLinks(final boolean followSymLinks)
        {
            this.options.followSymLinks = followSymLinks;
            return this;
        }
    }

    /**
     * Default: 4 MB
     */
    public static final int BUFFER_SIZE = 4 * 1024 * 1024;

    /**
    *
    */
    public static final String EMPTY_STRING = "";

    /**
     *
     */
    public static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().startsWith("linux");

    /**
     *
     */
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     *
     */
    private boolean checksum = false;

    /**
     *
     */
    private boolean delete = false;

    /**
     *
     */
    private boolean dryRun = true;

    /**
     *
     */
    private ExecutorService executorService = ForkJoinPool.commonPool();

    /**
     *
     */
    private boolean followSymLinks = true;

    /**
     * Erzeugt eine neue Instanz von {@link Options}
     */
    private Options()
    {
        super();
    }

    /**
     * @return {@link ExecutorService}
     */
    public ExecutorService getExecutorService()
    {
        return this.executorService;
    }

    /**
     * @return boolean
     */
    public boolean isChecksum()
    {
        return this.checksum;
    }

    /**
     * @return boolean
     */
    public boolean isDelete()
    {
        return this.delete;
    }

    /**
     * @return boolean
     */
    public boolean isDryRun()
    {
        return this.dryRun;
    }

    /**
     * @return boolean
     */
    public boolean isFollowSymLinks()
    {
        return this.followSymLinks;
    }
}
