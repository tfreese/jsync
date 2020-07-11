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
         * @param size int
         * @return {@link Builder}
         */
        public Builder bufferSize(final int size)
        {
            this.options.setBufferSize(size);
            return this;
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
            this.options.setChecksum(checksum);
            return this;
        }

        /**
         * @param delete boolean
         * @return {@link Builder}
         */
        public Builder delete(final boolean delete)
        {
            this.options.setDelete(delete);
            return this;
        }

        /**
         * @param dryRun boolean
         * @return {@link Builder}
         */
        public Builder dryRun(final boolean dryRun)
        {
            this.options.setDryRun(dryRun);
            return this;
        }

        /**
         * @param executorService {@link ExecutorService}
         * @return {@link Builder}
         */
        public Builder executorService(final ExecutorService executorService)
        {
            this.options.setExecutorService(executorService);
            return this;
        }

        /**
         * @param followSymLinks boolean
         * @return {@link Builder}
         */
        public Builder followSymLinks(final boolean followSymLinks)
        {
            this.options.setFollowSymLinks(followSymLinks);
            return this;
        }
    }

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
     * Default: 4 MB
     */
    private int bufferSize = 4 * 1024 * 1024;

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
     * Default: 4 MB
     *
     * @return int
     */
    public int getBufferSize()
    {
        return this.bufferSize;
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

    /**
     * @param bufferSize int
     */
    private void setBufferSize(final int bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    /**
     * @param checksum boolean
     */
    private void setChecksum(final boolean checksum)
    {
        this.checksum = checksum;
    }

    /**
     * @param delete boolean
     */
    private void setDelete(final boolean delete)
    {
        this.delete = delete;
    }

    /**
     * @param dryRun boolean
     */
    private void setDryRun(final boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    /**
     * @param executorService {@link ExecutorService}
     */
    private void setExecutorService(final ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    /**
     * @param followSymLinks boolean
     */
    private void setFollowSymLinks(final boolean followSymLinks)
    {
        this.followSymLinks = followSymLinks;
    }
}
