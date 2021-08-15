// Created: 15.08.2021
package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Set;

/**
 * Exclude-Filter
 *
 * @author Thomas Freese
 */
public interface PathFilter
{
    /**
     * @return {@link Set}
     */
    Set<String> getDirectoryFilter();

    /**
     * @return {@link Set}
     */
    Set<String> getFileFilter();

    /**
     * @param dir {@link Path}
     *
     * @return boolean
     */
    boolean isExcludedDirectory(Path dir);

    /**
     * @param file {@link Path}
     *
     * @return boolean
     */
    boolean isExcludedFile(Path file);
}
