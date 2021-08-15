// Created: 15.08.2021
package de.freese.jsync.filter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * Exclude-Filter, prüft mit {@link String#endsWith(String)} ob der Pfad den Filter enthält.
 *
 * @author Thomas Freese
 */
public class PathFilterEndsWith implements PathFilter
{
    /**
     *
     */
    private final Set<String> directoryFilter;
    /**
     *
     */
    private final Set<String> fileFilter;

    /**
     * Erstellt ein neues {@link PathFilterEndsWith} Object.
     *
     * @param directoryFilter {@link Set}
     * @param fileFilter {@link Set}
     */
    public PathFilterEndsWith(final Set<String> directoryFilter, final Set<String> fileFilter)
    {
        super();

        this.directoryFilter = Objects.requireNonNull(directoryFilter, "directoryFilter required");
        this.fileFilter = Objects.requireNonNull(fileFilter, "fileFilter required");
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#getDirectoryFilter()
     */
    @Override
    public Set<String> getDirectoryFilter()
    {
        return this.directoryFilter;
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#getFileFilter()
     */
    @Override
    public Set<String> getFileFilter()
    {
        return this.fileFilter;
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#isExcludedDirectory(java.nio.file.Path)
     */
    @Override
    public boolean isExcludedDirectory(final Path dir)
    {
        return this.directoryFilter.stream().anyMatch(filter -> dir.toString().endsWith(filter));
    }

    /**
     * @see de.freese.jsync.filter.PathFilter#isExcludedFile(java.nio.file.Path)
     */
    @Override
    public boolean isExcludedFile(final Path file)
    {
        return this.fileFilter.stream().anyMatch(filter -> file.toString().endsWith(filter));
    }
}
