// Created: 14.03.2020
package de.freese.jsync.arguments;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

/**
 * @author Thomas Freese
 */
public class ArgumentParserApacheCommonsCli implements ArgumentParser {
    private final CommandLine line;

    private final Options options;

    public ArgumentParserApacheCommonsCli(final String[] args) throws Exception {
        super();

        options = new Options();

        final OptionGroup groupParams = new OptionGroup();
        groupParams.addOption(Option.builder("d").longOpt("delete").hasArg(false).desc("Empfänger löscht Dateien vor dem Transfer").get());
        options.addOptionGroup(groupParams);

        options.addOption(Option.builder("f").longOpt("follow-symlinks").desc("Dateien von SymLinks kopieren").get());
        options.addOption(Option.builder("n").longOpt("dry-run").desc("Synchronisation nur Simulieren").get());
        options.addOption(Option.builder("c").longOpt("checksum").desc("Zusätzlich Prüfsumme für Vergleich berechnen").get());

        options.addOption(Option.builder("s").longOpt("sender").hasArg().argName("DIR").desc("Quell-Verzeichnis").required().get());
        options.addOption(Option.builder("r").longOpt("receiver").hasArg().argName("DIR").desc("Ziel-Verzeichnis").required().get());

        final CommandLineParser parser = new DefaultParser();

        try {
            line = parser.parse(options, args);
        }
        catch (ParseException pex) {
            printHelp(System.out);

            throw pex;
        }
    }

    @Override
    public boolean checksum() {
        return line.hasOption("checksum");
    }

    @Override
    public boolean delete() {
        return line.hasOption("delete");
    }

    @Override
    public boolean dryRun() {
        return line.hasOption("dry-run");
    }

    @Override
    public boolean followSymlinks() {
        return line.hasOption("follow-symlinks");
    }

    @Override
    public boolean hasArgs() {
        final Option[] opts = line.getOptions();

        return opts != null && opts.length > 0;
    }

    @Override
    public void printHelp(final PrintStream printStream) {
        final HelpFormatter formatter = HelpFormatter.builder()
                .setShowSince(false)
                .get();

        final StringBuilder footer = new StringBuilder();
        footer.append(System.lineSeparator()).append("@Thomas Freese");

        try {
            formatter.printHelp("jsync [OPTIONS]", "Parameter:", options, footer.toString(), true);
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public String receiver() {
        return line.getOptionValue("receiver");
    }

    @Override
    public String sender() {
        return line.getOptionValue("sender");
    }
}
