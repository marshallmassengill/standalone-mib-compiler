package org.opennms.mibcompiler.cli;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.opennms.mibcompiler.model.Events;
import org.opennms.mibcompiler.parser.MibToEventsParser;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(name = "mib-compiler", 
         mixinStandardHelpOptions = true, 
         version = "MIB to Events Compiler 1.0.0",
         description = "Convert SNMP MIB files into OpenNMS event definitions")
public class MibCompilerCLI implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(MibCompilerCLI.class);

    @Parameters(index = "0", 
                description = "Input MIB file or directory containing MIB files")
    private File input;

    @Option(names = {"-d", "--mib-directory"}, 
            description = "Directory containing MIB dependencies (defaults to input directory if input is a file)")
    private File mibDirectory;

    @Option(names = {"-o", "--output"}, 
            description = "Output file for events XML (defaults to stdout)")
    private File outputFile;

    @Option(names = {"-u", "--uei-base"}, 
            description = "Base UEI for generated events", 
            defaultValue = "uei.opennms.org/mib")
    private String ueiBase;

    @Option(names = {"-v", "--verbose"}, 
            description = "Enable verbose logging")
    private boolean verbose;

    @Option(names = {"--process-all"}, 
            description = "Process all MIB files in directory (when input is a directory)")
    private boolean processAll;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MibCompilerCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (verbose) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        }

        if (!input.exists()) {
            System.err.println("Error: Input file or directory does not exist: " + input);
            return 1;
        }

        // Set default MIB directory
        if (mibDirectory == null) {
            if (input.isDirectory()) {
                mibDirectory = input;
            } else {
                mibDirectory = input.getParentFile();
                if (mibDirectory == null) {
                    mibDirectory = new File(".");
                }
            }
        }

        if (!mibDirectory.exists() || !mibDirectory.isDirectory()) {
            System.err.println("Error: MIB directory does not exist or is not a directory: " + mibDirectory);
            return 1;
        }

        try {
            Events allEvents = new Events();
            int processedCount = 0;
            int errorCount = 0;

            if (input.isDirectory()) {
                if (processAll) {
                    // Process all MIB files in directory
                    try (Stream<Path> paths = Files.walk(input.toPath())) {
                        for (Path path : paths.filter(Files::isRegularFile)
                                              .filter(this::isMibFile)
                                              .toArray(Path[]::new)) {
                            File mibFile = path.toFile();
                            System.out.println("Processing MIB file: " + mibFile.getName());
                            
                            Events events = processMibFile(mibFile);
                            if (events != null && !events.getEvents().isEmpty()) {
                                allEvents.getEvents().addAll(events.getEvents());
                                processedCount++;
                                System.out.println("  Generated " + events.getEvents().size() + " events");
                            } else if (events != null && events.getEvents().isEmpty()) {
                                System.out.println("  No trap or notification definitions found (0 events generated)");
                            } else {
                                errorCount++;
                                System.err.println("  Failed to parse MIB file");
                            }
                        }
                    }
                } else {
                    System.err.println("Error: Input is a directory. Use --process-all to process all MIB files, or specify a single MIB file.");
                    return 1;
                }
            } else {
                // Process single MIB file
                System.out.println("Processing MIB file: " + input.getName());
                Events events = processMibFile(input);
                if (events != null && !events.getEvents().isEmpty()) {
                    allEvents = events;
                    processedCount = 1;
                    System.out.println("Generated " + events.getEvents().size() + " events");
                } else if (events != null && events.getEvents().isEmpty()) {
                    System.out.println("No trap or notification definitions found (0 events generated)");
                    System.out.println("\nNote: This MIB does not contain any TRAP-TYPE or NOTIFICATION-TYPE definitions.");
                    System.out.println("The MIB may only define data structures for monitoring/polling purposes.");
                } else {
                    errorCount = 1;
                    System.err.println("Failed to parse MIB file");
                }
            }

            if (processedCount == 0) {
                System.err.println("No MIB files were successfully processed.");
                return 1;
            }

            // Output results
            String xmlOutput = marshalEvents(allEvents);
            
            if (outputFile != null) {
                try (FileWriter writer = new FileWriter(outputFile)) {
                    writer.write(xmlOutput);
                }
                System.out.println("Events written to: " + outputFile.getAbsolutePath());
            } else {
                System.out.println("\n" + xmlOutput);
            }

            System.out.println("\nSummary:");
            System.out.println("  Processed: " + processedCount + " MIB files");
            System.out.println("  Errors: " + errorCount + " MIB files");
            System.out.println("  Total events generated: " + allEvents.getEvents().size());

            return errorCount > 0 ? 2 : 0; // Exit with 2 if there were errors, 0 if all successful

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private Events processMibFile(File mibFile) {
        MibToEventsParser parser = new MibToEventsParser();
        parser.setMibDirectory(mibDirectory);
        
        if (!parser.parseMib(mibFile)) {
            String errors = parser.getFormattedErrors();
            if (errors != null) {
                System.err.println("Parsing errors:");
                System.err.println(errors);
            }
            
            if (!parser.getMissingDependencies().isEmpty()) {
                System.err.println("Missing dependencies: " + parser.getMissingDependencies());
            }
            return null;
        }

        Events events = parser.getEvents(ueiBase);
        if (events == null) {
            String errors = parser.getFormattedErrors();
            if (errors != null) {
                System.err.println("Event generation errors:");
                System.err.println(errors);
            }
        }
        
        return events;
    }

    private boolean isMibFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".mib") || 
               fileName.endsWith(".txt") || 
               fileName.endsWith(".my") ||
               (!fileName.contains(".") && Files.isRegularFile(path)); // Files without extension
    }

    private String marshalEvents(Events events) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Events.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        
        StringWriter writer = new StringWriter();
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        marshaller.marshal(events, writer);
        
        return writer.toString();
    }
}