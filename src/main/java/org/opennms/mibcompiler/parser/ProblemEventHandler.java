package org.opennms.mibcompiler.parser;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsmiparser.parser.SmiDefaultParser;
import org.jsmiparser.util.problem.DefaultProblemReporterFactory;
import org.jsmiparser.util.problem.ProblemEvent;
import org.jsmiparser.util.problem.ProblemReporterFactory;
import org.jsmiparser.util.problem.annotations.ProblemSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProblemEventHandler implements org.jsmiparser.util.problem.ProblemEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemEventHandler.class);
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("Cannot find module ([^,]+)", Pattern.MULTILINE);

    private int[] severityCounters = new int[ProblemSeverity.values().length];
    private int totalCounter;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private PrintStream out;

    public ProblemEventHandler(SmiDefaultParser parser) {
        out = new PrintStream(outputStream);
        ProblemReporterFactory problemReporterFactory = new DefaultProblemReporterFactory(getClass().getClassLoader(), this);
        parser.setProblemReporterFactory(problemReporterFactory);
    }

    @Override
    public void handle(ProblemEvent event) {
        severityCounters[event.getSeverity().ordinal()]++;
        totalCounter++;
        String message = event.getSeverity().toString() + ": " + event.getLocalizedMessage();
        if (event.getLocation() != null) {
            message += " (Location: " + event.getLocation() + ")";
        }
        LOG.debug(message);
        out.println(message);
    }

    @Override
    public boolean isOk() {
        for (int i = 0; i < severityCounters.length; i++) {
            if (i >= ProblemSeverity.ERROR.ordinal()) {
                int severityCounter = severityCounters[i];
                if (severityCounter > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isNotOk() {
        return !isOk();
    }

    @Override
    public int getSeverityCount(ProblemSeverity severity) {
        return severityCounters[severity.ordinal()];
    }

    @Override
    public int getTotalCount() {
        return totalCounter;
    }

    public void reset() {
        outputStream.reset();
        severityCounters = new int[ProblemSeverity.values().length];
        totalCounter = 0;
    }

    public List<String> getDependencies() {
        List<String> dependencies = new ArrayList<>();
        if (outputStream.size() > 0) {
            Matcher m = DEPENDENCY_PATTERN.matcher(outputStream.toString());
            while (m.find()) {
                final String dep = m.group(1);
                if (!dependencies.contains(dep)) {
                    dependencies.add(dep);
                }
            }
        }
        return dependencies;
    }

    public String getMessages() {
        return outputStream.size() > 0 ? outputStream.toString() : null;
    }

    public void addError(String errorMessage) {
        out.println(errorMessage);
    }
}