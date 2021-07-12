// Created: 28.10.2016
package de.freese.jsync;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Enthält die Optionen für die Synchronisierung.
 *
 * @author Thomas Freese
 */
public final class Options
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
         *
         * @return {@link Builder}
         */
        public Builder checksum(final boolean checksum)
        {
            this.options.checksum = checksum;
            return this;
        }

        /**
         * @param delete boolean
         *
         * @return {@link Builder}
         */
        public Builder delete(final boolean delete)
        {
            this.options.delete = delete;
            return this;
        }

        /**
         * @param dryRun boolean
         *
         * @return {@link Builder}
         */
        public Builder dryRun(final boolean dryRun)
        {
            this.options.dryRun = dryRun;
            return this;
        }

        /**
         * @param followSymLinks boolean
         *
         * @return {@link Builder}
         */
        public Builder followSymLinks(final boolean followSymLinks)
        {
            this.options.followSymLinks = followSymLinks;
            return this;
        }

        /**
         * @param parallel boolean
         *
         * @return {@link Builder}
         */
        public Builder parallel(final boolean parallel)
        {
            this.options.parallel = parallel;
            return this;
        }
    }

    /**
     * Default: 4 MB
     */
    public static final int BUFFER_SIZE = 1024 * 1024 * 4;

    /**
     * Default: UTF-8
     */
    public static final Charset CHARSET = StandardCharsets.UTF_8;

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
    private boolean checksum;

    /**
     *
     */
    private boolean delete;

    /**
     *
     */
    private boolean dryRun = true;

    /**
     *
     */
    private boolean followSymLinks = true;

    /**
    *
    */
    private boolean parallel;

    /**
     * Erzeugt eine neue Instanz von {@link Options}
     */
    private Options()
    {
        super();
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
     * @return boolean
     */
    public boolean isParallel()
    {
        return this.parallel;
    }
}
