// Created: 14.03.2020
package de.freese.jsync.arguments;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author Thomas Freese
 */
public class ArgumentParserApacheCommonsCli implements ArgumentParser
{
    /**
     *
     */
    private final CommandLine line;
    /**
     *
     */
    private final Options options;

    /**
     * Erstellt ein neues {@link ArgumentParserApacheCommonsCli} Object.
     *
     * @param args String[]
     *
     * @throws Exception Falls was schiefgeht.
     */
    public ArgumentParserApacheCommonsCli(final String[] args) throws Exception
    {
        super();

        this.options = new Options();

        OptionGroup groupParams = new OptionGroup();
        groupParams.addOption(Option.builder("d").longOpt("delete").hasArg(false).desc("Empfänger löscht Dateien vor dem Transfer").build());
        this.options.addOptionGroup(groupParams);

        this.options.addOption(Option.builder("f").longOpt("follow-symlinks").desc("Dateien von SymLinks kopieren").build());
        this.options.addOption(Option.builder("n").longOpt("dry-run").desc("Synchronisation nur Simulieren").build());
        this.options.addOption(Option.builder("c").longOpt("checksum").desc("Zusätzlich Prüfsumme für Vergleich berechnen").build());

        this.options.addOption(Option.builder("s").longOpt("sender").hasArg().argName("DIR").desc("Quell-Verzeichnis").required().build());
        this.options.addOption(Option.builder("r").longOpt("receiver").hasArg().argName("DIR").desc("Ziel-Verzeichnis").required().build());

        CommandLineParser parser = new DefaultParser();

        try
        {
            this.line = parser.parse(this.options, args);
        }
        catch (ParseException pex)
        {
            printHelp(System.out);

            throw pex;
        }
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#checksum()
     */
    @Override
    public boolean checksum()
    {
        return this.line.hasOption("checksum");
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#delete()
     */
    @Override
    public boolean delete()
    {
        return this.line.hasOption("delete");
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#dryRun()
     */
    @Override
    public boolean dryRun()
    {
        return this.line.hasOption("dry-run");
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#followSymlinks()
     */
    @Override
    public boolean followSymlinks()
    {
        return this.line.hasOption("follow-symlinks");
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#hasArgs()
     */
    @Override
    public boolean hasArgs()
    {
        Option[] opts = this.line.getOptions();

        return (opts != null) && (opts.length > 0);
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#printHelp(java.io.PrintStream)
     */
    @Override
    public void printHelp(final PrintStream printStream)
    {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(null);
        // formatter.setWidth(120);
        // formatter.printHelp("JSync\n", getCommandOptions(), true);

        StringBuilder footer = new StringBuilder();
        footer.append("\n@Thomas Freese");

        try (PrintWriter pw = new PrintWriter(printStream, true, StandardCharsets.UTF_8))
        {
            formatter.printHelp(pw, 120, "jsync [OPTIONS]\n", "\nParameter:", this.options, 3, 5, footer.toString(), true);
        }
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#receiver()
     */
    @Override
    public String receiver()
    {
        return this.line.getOptionValue("receiver");
    }

    /**
     * @see de.freese.jsync.arguments.ArgumentParser#sender()
     */
    @Override
    public String sender()
    {
        return this.line.getOptionValue("sender");
    }
}
