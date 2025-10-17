package org.opennms.mibcompiler.parser;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsmiparser.parser.SmiDefaultParser;
import org.jsmiparser.smi.*;
import org.opennms.mibcompiler.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MibToEventsParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(MibToEventsParser.class);
    
    private static final String[] MIB_SUFFIXES = new String[] { "", ".txt", ".mib", ".my" };
    private static final Pattern TRAP_OID_PATTERN = Pattern.compile("(.*)\\.(\\d+)$");
    
    private File mibDirectory;
    private SmiDefaultParser parser;
    private SmiModule module;
    private ProblemEventHandler errorHandler;
    private List<String> missingDependencies = new ArrayList<>();

    public MibToEventsParser() {
        parser = new SmiDefaultParser();
        errorHandler = new ProblemEventHandler(parser);
    }

    public void setMibDirectory(File mibDirectory) {
        this.mibDirectory = mibDirectory;
    }

    public boolean parseMib(File mibFile) {
        if (mibDirectory == null || !mibDirectory.isDirectory()) {
            errorHandler.addError("MIB directory has not been set.");
            return false;
        }

        missingDependencies.clear();

        List<URL> queue = new ArrayList<>();
        parser.getFileParserPhase().setInputUrls(queue);

        final Map<String, File> mibDirectoryFiles = new HashMap<>();
        for (final File file : mibDirectory.listFiles()) {
            mibDirectoryFiles.put(file.getName().toLowerCase(), file);
        }

        LOG.debug("Parsing {}", mibFile.getAbsolutePath());
        SmiMib mib = null;
        addFileToQueue(queue, mibFile);
        
        while (true) {
            errorHandler.reset();
            try {
                mib = parser.parse();
            } catch (Exception e) {
                LOG.error("Can't compile {}", mibFile, e);
                errorHandler.addError(e.getMessage());
            }
            
            if (errorHandler.isOk()) {
                break;
            } else {
                List<String> dependencies = errorHandler.getDependencies();
                if (dependencies.isEmpty()) {
                    break;
                }
                missingDependencies.addAll(dependencies);
                if (!addDependencyToQueue(queue, mibDirectoryFiles)) {
                    break;
                }
            }
        }
        
        if (errorHandler.isNotOk() || mib == null) {
            return false;
        }

        LOG.info("The MIB {} has been parsed successfully.", mibFile.getAbsolutePath());
        module = getModule(mib, mibFile);
        return module != null;
    }

    public Events getEvents(String ueibase) {
        if (module == null) {
            return null;
        }
        
        LOG.info("Generating events for {} using the following UEI Base: {}", module.getId(), ueibase);
        try {
            return convertMibToEvents(module, ueibase);
        } catch (Throwable e) {
            String errors = e.getMessage();
            if (errors == null || errors.trim().equals("")) {
                errors = "An unknown error occurred when generating events objects from the MIB " + module.getId();
            }
            LOG.error("Event parsing error: {}", errors, e);
            errorHandler.addError(errors);
            return null;
        }
    }

    public String getMibName() {
        return module != null ? module.getId() : null;
    }

    public String getFormattedErrors() {
        return errorHandler.getMessages();
    }

    public List<String> getMissingDependencies() {
        return missingDependencies;
    }

    private Events convertMibToEvents(SmiModule module, String ueibase) {
        Events events = new Events();
        
        for (SmiNotificationType trap : module.getNotificationTypes()) {
            events.addEvent(getTrapEvent(trap, ueibase));
        }
        
        for (SmiTrapType trap : module.getTrapTypes()) {
            events.addEvent(getTrapEvent(trap, ueibase));
        }
        
        return events;
    }

    private Event getTrapEvent(Notification trap, String ueibase) {
        String severity = "Indeterminate";
        
        Event evt = new Event();
        evt.setUei(getTrapEventUEI(trap, ueibase));
        evt.setEventLabel(getTrapEventLabel(trap));
        evt.setLogmsg(getTrapEventLogmsg(trap));
        evt.setSeverity(severity);
        evt.setDescr(getTrapEventDescr(trap));
        
        List<Varbindsdecode> decode = getTrapVarbindsDecode(trap);
        if (!decode.isEmpty()) {
            evt.setVarbindsdecodes(decode);
        }
        
        evt.setMask(new Mask());
        addMaskElement(evt, "id", getTrapEnterprise(trap));
        addMaskElement(evt, "generic", "6");
        addMaskElement(evt, "specific", getTrapSpecificType(trap));
        
        return evt;
    }

    private String getTrapEventUEI(Notification trap, String ueibase) {
        final StringBuilder buf = new StringBuilder(ueibase);
        if (!ueibase.endsWith("/")) {
            buf.append("/");
        }
        buf.append(trap.getId());
        return buf.toString();
    }

    private String getTrapEventLabel(Notification trap) {
        final StringBuilder buf = new StringBuilder();
        buf.append(trap.getModule().getId());
        buf.append(" defined trap event: ");
        buf.append(trap.getId());
        return buf.toString();
    }

    private Logmsg getTrapEventLogmsg(Notification trap) {
        final StringBuilder dbuf = new StringBuilder();
        dbuf.append("<p>");
        dbuf.append("\n");
        dbuf.append("\t").append(trap.getId()).append(" trap received\n");
        
        int vbNum = 1;
        for (SmiVariable var : trap.getObjects()) {
            dbuf.append("\t").append(var.getId()).append("=%parm[#").append(vbNum).append("]%\n");
            vbNum++;
        }
        
        if (dbuf.charAt(dbuf.length() - 1) == '\n') {
            dbuf.deleteCharAt(dbuf.length() - 1);
        }
        dbuf.append("</p>\n\t");
        
        return new Logmsg("logndisplay", dbuf.toString());
    }

    private String getTrapEventDescr(Notification trap) {
        String description = trap.getDescription();
        if (description == null) {
            LOG.warn("The trap {} doesn't have a description field", trap.getOidStr());
        }
        
        final String descrEndingNewlines = description == null ? "No Description." : 
            description.replaceAll("^", "\n<p>").replaceAll("$", "</p>\n");
        final StringBuffer dbuf = new StringBuffer(descrEndingNewlines);
        
        if (dbuf.charAt(dbuf.length() - 1) == '\n') {
            dbuf.deleteCharAt(dbuf.length() - 1);
        }
        
        dbuf.append("<table>");
        dbuf.append("\n");
        
        int vbNum = 1;
        for (SmiVariable var : trap.getObjects()) {
            dbuf.append("\t<tr><td><b>\n\n\t").append(var.getId());
            dbuf.append("</b></td><td>\n\t%parm[#").append(vbNum).append("]%;</td><td><p>");
            
            SmiPrimitiveType type = var.getType().getPrimitiveType();
            if (type.equals(SmiPrimitiveType.ENUM)) {
                SortedMap<BigInteger, String> map = new TreeMap<>();
                SmiType t = var.getType();
                while (t.getEnumValues() == null) {
                    t = t.getBaseType();
                }
                List<SmiNamedNumber> enumValues = t.getEnumValues();
                if (enumValues != null) {
                    for (SmiNamedNumber v : enumValues) {
                        map.put(v.getValue(), v.getId());
                    }
                } else {
                    map.put(new BigInteger("0"), "Unable to derive list of possible values.");
                }
                
                dbuf.append("\n");
                for (Entry<BigInteger, String> entry : map.entrySet()) {
                    dbuf.append("\t\t").append(entry.getValue()).append("(").append(entry.getKey()).append(")\n");
                }
                dbuf.append("\t");
            }
            dbuf.append("</p></td></tr>\n");
            vbNum++;
        }
        
        if (dbuf.charAt(dbuf.length() - 1) == '\n') {
            dbuf.deleteCharAt(dbuf.length() - 1);
        }
        dbuf.append("</table>\n\t");
        
        return dbuf.toString();
    }

    private List<Varbindsdecode> getTrapVarbindsDecode(Notification trap) {
        Map<String, Varbindsdecode> decode = new LinkedHashMap<>();
        int vbNum = 1;
        
        for (SmiVariable var : trap.getObjects()) {
            String parmName = "parm[#" + vbNum + "]";
            SmiPrimitiveType type = var.getType().getPrimitiveType();
            
            if (type.equals(SmiPrimitiveType.ENUM)) {
                SortedMap<BigInteger, String> map = new TreeMap<>();
                SmiType t = var.getType();
                while (t.getEnumValues() == null) {
                    t = t.getBaseType();
                }
                List<SmiNamedNumber> enumValues = t.getEnumValues();
                if (enumValues != null) {
                    for (SmiNamedNumber v : enumValues) {
                        map.put(v.getValue(), v.getId());
                    }
                    
                    for (Entry<BigInteger, String> entry : map.entrySet()) {
                        if (!decode.containsKey(parmName)) {
                            Varbindsdecode newVarbind = new Varbindsdecode();
                            newVarbind.setParmid(parmName);
                            decode.put(newVarbind.getParmid(), newVarbind);
                        }
                        
                        Decode d = new Decode();
                        d.setVarbinddecodedstring(entry.getValue());
                        d.setVarbindvalue(entry.getKey().toString());
                        decode.get(parmName).addDecode(d);
                    }
                }
            }
            vbNum++;
        }
        
        return new ArrayList<>(decode.values());
    }

    private String getTrapEnterprise(Notification trap) {
        String trapOid = getMatcherForOid(getTrapOid(trap)).group(1);

        if (trapOid.endsWith(".0")) {
            trapOid = trapOid.substring(0, trapOid.length() - 2);
        }
        return trapOid;
    }

    private String getTrapSpecificType(Notification trap) {
        return getMatcherForOid(getTrapOid(trap)).group(2);
    }

    private Matcher getMatcherForOid(String trapOid) {
        Matcher m = TRAP_OID_PATTERN.matcher(trapOid);
        if (!m.matches()) {
            throw new IllegalStateException("Could not match the trap OID '" + trapOid + "' against '" + m.pattern().pattern() + "'");
        }
        return m;
    }

    private String getTrapOid(Notification trap) {
        return '.' + trap.getOidStr();
    }

    private void addMaskElement(Event event, String name, String value) {
        if (event.getMask() == null) {
            throw new IllegalStateException("Event mask is not present, must have been set before this method was called");
        }
        Maskelement me = new Maskelement();
        me.setMename(name);
        me.addMevalue(value);
        event.getMask().addMaskelement(me);
    }

    private void addFileToQueue(List<URL> queue, File mibFile) {
        try {
            URL url = mibFile.toURI().toURL();
            if (!queue.contains(url)) {
                LOG.debug("Adding {} to queue ", url);
                queue.add(url);
            }
        } catch (Exception e) {
            LOG.warn("Can't generate URL from {}", mibFile.getAbsolutePath());
        }
    }

    private boolean addDependencyToQueue(final List<URL> queue, final Map<String, File> mibDirectoryFiles) {
        final List<String> dependencies = new ArrayList<>(missingDependencies);
        boolean ok = true;
        
        for (String dependency : dependencies) {
            boolean found = false;
            for (String suffix : MIB_SUFFIXES) {
                final String fileName = (dependency + suffix).toLowerCase();
                if (mibDirectoryFiles.containsKey(fileName)) {
                    File f = mibDirectoryFiles.get(fileName);
                    LOG.debug("Checking dependency file {}", f.getAbsolutePath());
                    if (f.exists()) {
                        LOG.info("Adding dependency file {}", f.getAbsolutePath());
                        addFileToQueue(queue, f);
                        missingDependencies.remove(dependency);
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                LOG.warn("Couldn't find dependency {} on {}", dependency, mibDirectory);
                ok = false;
            }
        }
        return ok;
    }

    private SmiModule getModule(SmiMib mibObject, File mibFile) {
        for (SmiModule m : mibObject.getModules()) {
            URL source = null;
            try {
                source = new URL(m.getIdToken().getLocation().getSource());
            } catch (Exception e) {
                // Ignore
            }
            if (source != null) {
                try {
                    File srcFile = new File(source.toURI());
                    if (srcFile.getAbsolutePath().equals(mibFile.getAbsolutePath())) {
                        return m;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        LOG.error("Can't find the MIB module for " + mibFile);
        errorHandler.addError("Can't find the MIB module for " + mibFile);
        return null;
    }
}