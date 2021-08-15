// Created: 15.08.2021
package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

/**
 * Exclude-Filter, der nichts excludiert.
 *
 * @author Thomas Freese
 */
public class PathFilterTrue implements PathFilter
{
    /**
     * @see de.freese.jsync.filter.PathFilter#getDirectoryFilter()
     */
    @Override
    public Set<String> getDirectoryFilter()
    {
        return Collections.emptySet();
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#getFileFilter()
     */
    @Override
    public Set<String> getFileFilter()
    {
        return Collections.emptySet();
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#isExcludedDirectory(java.nio.file.Path)
     */
    @Override
    public boolean isExcludedDirectory(final Path dir)
    {
        return false;
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#isExcludedFile(java.nio.file.Path)
     */
    @Override
    public boolean isExcludedFile(final Path file)
    {
        return false;
    }
}
