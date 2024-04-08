// Created: 05.04.2018
package de.freese.jsync.filesystem;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.Options;
import de.freese.jsync.generator.DefaultGenerator;
import de.freese.jsync.generator.Generator;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractFileSystem implements FileSystem {
    /**
     * -? – this part identifies if the given number is negative<br>
     * the dash “–” searches for dash literally<br>
     * the question mark “?” marks its presence as an optional one<br>
     * \d+ – this searches for one or more digits<br>
     * (\.\d+)? – this part of regex is to identify float numbers<br>
     * Here we're searching for one or more digits followed by a period.<br>
     * The question mark, in the end, signifies that this complete group is optional
     */
    protected static final Pattern PATTERN_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private final Generator generator = new DefaultGenerator();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected Charset getCharset() {
        return Options.CHARSET;
    }

    protected Generator getGenerator() {
        return this.generator;
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
