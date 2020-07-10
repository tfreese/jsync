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
    public Options()
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
    public void setBufferSize(final int bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    /**
     * @param checksum boolean
     */
    public void setChecksum(final boolean checksum)
    {
        this.checksum = checksum;
    }

    /**
     * @param delete boolean
     */
    public void setDelete(final boolean delete)
    {
        this.delete = delete;
    }

    /**
     * @param dryRun boolean
     */
    public void setDryRun(final boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    /**
     * @param executorService {@link ExecutorService}
     */
    public void setExecutorService(final ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    /**
     * @param followSymLinks boolean
     */
    public void setFollowSymLinks(final boolean followSymLinks)
    {
        this.followSymLinks = followSymLinks;
    }
}
